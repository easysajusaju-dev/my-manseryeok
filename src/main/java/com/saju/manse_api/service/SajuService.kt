package com.saju.manse_api.service

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

// 서버가 내려주는 결과(간단형)
data class SajuResult(
    val yearGanji: String,
    val monthGanji: String,
    val dayGanji: String,
    val hourGanji: String,

    // 4주 십성(일간 기준, 서버 계산)
    val yearGod: String,
    val monthGod: String,
    val dayGod: String,
    val hourGod: String,

    // 대운수/방향(서버 계산)
    val daeNum: Int,
    val daeDir: String,

    // 대운/연운 라인(표시용)
    val daeWoon: List<String> = emptyList(),
    val daeWoonGanji: List<String> = emptyList(),
    val daeWoonYear: List<Int> = emptyList(),
    val seunYear: List<Int> = emptyList(),
    val seunGanji: List<String> = emptyList(),

    // 요약(옵션)
    val solarText: String? = null,
    val lunarText: String? = null,
    val termName: String? = null,
    val termDate: String? = null
)

object SajuService {

    private val GAN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
    private val JI  = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")

    private val STEM_ELEM = mapOf(
        "甲" to "wood","乙" to "wood","丙" to "fire","丁" to "fire",
        "戊" to "earth","己" to "earth","庚" to "metal","辛" to "metal",
        "壬" to "water","癸" to "water"
    )
    private val CYCLE = listOf("wood","fire","earth","metal","water")
    private val CONTROL = mapOf("wood" to "earth","fire" to "metal","earth" to "water","metal" to "wood","water" to "fire")
    private val YANG = setOf("甲","丙","戊","庚","壬")

    private fun isYang(s: String) = s in YANG
    private fun elemOfStem(s: String) = STEM_ELEM[s] ?: "earth"
    private fun z2(n: Int) = if (n < 10) "0$n" else "$n"

    // 시지 인덱스(0=子) — pivotMinute=30이면 23:30부터 子
    private fun hourBranchIndex(hour: Int, minute: Int, pivotMinute: Int): Int {
        val total = hour * 60 + minute
        val start = 23 * 60 - pivotMinute
        val cycle = 12 * 120
        var t = (total - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }

    // 십성(일간 vs 상대 '간')
    private fun tenGod(dayStem: String, otherStem: String): String {
        if (dayStem.isBlank() || otherStem.isBlank()) return "-"
        val ed = elemOfStem(dayStem)
        val eo = elemOfStem(otherStem)
        val same = isYang(dayStem) == isYang(otherStem)
        val idx = { k: String -> CYCLE.indexOf(k) }
        return when {
            eo == ed                       -> if (same) "비견" else "겁재"
            eo == CYCLE[(idx(ed) + 1) % 5] -> if (same) "식신" else "상관"
            eo == CYCLE[(idx(ed) + 4) % 5] -> if (same) "정인" else "편인"
            eo == CONTROL[ed]              -> if (same) "정재" else "편재"
            CONTROL[eo] == ed              -> if (same) "정관" else "편관"
            else -> "-"
        }
    }

    // 기본 엔드포인트(/saju) — daeNum/daeDir 포함
    fun getSaju(
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean, isMale: Boolean, pivotMinute: Int = 30
    ): SajuResult {

        val info = if (isLunar) ManseryeokRepo.findByLunar(year, month, day, leap)
                   else         ManseryeokRepo.findBySolar(year, month, day)
        require(info != null) { "해당 날짜 데이터가 없습니다." }

        val yearGanji  = info!!.hy
        val monthGanji = info.hm
        val dayGanji   = info.hd

        // 시주
        val dayStem = dayGanji.substring(0, 1)
        val hIdx = hourBranchIndex(hour, minute, pivotMinute)
        val hourJi = JI[hIdx]
        val dayStemIdx = GAN.indexOf(dayStem)
        val hourGan = GAN[(dayStemIdx % 5 * 2 + hIdx) % 10]
        val hourGanji = hourGan + hourJi

        // 방향(연간 기준)
        val yearStem = yearGanji.substring(0, 1)
        val forward = (isMale && isYang(yearStem)) || (!isMale && !isYang(yearStem))
        val dirLabel = if (forward) "정사" else "역사"

        // 대운수(절기까지의 시간차 / 24 / 3 → 올림)
        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val termPoint = if (forward) SeasonRepo.nextAfter(info.sy, info.sm, info.sd, hour)
                        else         SeasonRepo.prevBefore(info.sy, info.sm, info.sd, hour)
        val hoursDiff = abs(Duration.between(birth, termPoint.dt).toHours().toDouble())
        val daeNum    = max(1, ceil((hoursDiff / 24.0) / 3.0).toInt())
        val startYear = info.sy + daeNum - 1

        // 라인들
        val mStem = monthGanji.substring(0, 1)
        val mBr   = monthGanji.substring(1, 2)
        val mStemIdx = GAN.indexOf(mStem)
        val mBrIdx   = JI.indexOf(mBr)

        val daeWoon = mutableListOf<String>()
        val daeWoonGanji = mutableListOf<String>()
        val daeWoonYear = mutableListOf<Int>()
        for (i in 1..10) {
            val gi = if (forward) (mStemIdx + i) % 10 else (mStemIdx - i + 100) % 10
            val bi = if (forward) (mBrIdx   + i) % 12 else (mBrIdx   - i + 120) % 12
            daeWoon.add("${i * 10}대운 (${if (forward) "순행" else "역행"})")
            daeWoonGanji.add(GAN[gi] + JI[bi])
            daeWoonYear.add(startYear + (i - 1) * 10)
        }

        val seunYear = mutableListOf<Int>()
        val seunGanji = mutableListOf<String>()
        for (i in 0 until 10) {
            val y2 = startYear + i
            seunYear.add(y2)
            seunGanji.add(GAN[(y2 + 6) % 10] + JI[(y2 + 8) % 12])
        }

        // 십성
        val yearGod  = tenGod(dayStem, yearStem)
        val monthGod = tenGod(dayStem, mStem)
        val dayGod   = "일간"
        val hourGod  = tenGod(dayStem, hourGan)

        return SajuResult(
            yearGanji = yearGanji,
            monthGanji = monthGanji,
            dayGanji = dayGanji,
            hourGanji = hourGanji,
            yearGod = yearGod,
            monthGod = monthGod,
            dayGod = dayGod,
            hourGod = hourGod,
            daeNum = daeNum,
            daeDir = dirLabel,
            daeWoon = daeWoon,
            daeWoonGanji = daeWoonGanji,
            daeWoonYear = daeWoonYear,
            seunYear = seunYear,
            seunGanji = seunGanji,
            solarText = "${info.sy}년 ${z2(info.sm)}월 ${z2(info.sd)}일 ${z2(hour)}:${z2(minute)}",
            lunarText = "음력 ${info.lm}월 ${info.ld}일" + if (info.leap) " (윤달)" else "",
            termName = termPoint.name,
            termDate = termPoint.dt.toString().replace('T', ' ')
        )
    }

    // 앱 호환 강제(/saju/compat) — 정시/동경시 -30/절기 +10/대운 floor 등
    fun getSajuCompat(
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean, isMale: Boolean,
        pivotMin: Int = 0, tzAdjust: Int = -30, seasonAdjust: Int = 10, daeRound: String = "floor"
    ): SajuResult {

        val info = if (isLunar) ManseryeokRepo.findByLunar(year, month, day, leap)
                   else         ManseryeokRepo.findBySolar(year, month, day)
        require(info != null) { "해당 날짜 데이터가 없습니다." }

        val yearGanji  = info!!.hy
        val monthGanji = info.hm
        val dayGanji   = info.hd

        // 보정된 출생시각
        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute).plusMinutes(tzAdjust.toLong())

        // 시주(보정/정시)
        val dayStem = dayGanji.substring(0, 1)
        val hIdx = hourBranchIndex(birth.hour, birth.minute, pivotMin)
        val hourJi = JI[hIdx]
        val dayStemIdx = GAN.indexOf(dayStem)
        val hourGan = GAN[(dayStemIdx % 5 * 2 + hIdx) % 10]
        val hourGanji = hourGan + hourJi

        // 방향
        val yearStem = yearGanji.substring(0, 1)
        val forward = (isMale && isYang(yearStem)) || (!isMale && !isYang(yearStem))
        val dirLabel = if (forward) "정사" else "역사"

        // 절기(+10분 보정)
        val rawTerm = if (forward) SeasonRepo.nextAfter(info.sy, info.sm, info.sd, birth.hour)
                      else         SeasonRepo.prevBefore(info.sy, info.sm, info.sd, birth.hour)
        val term = rawTerm.dt.plusMinutes(seasonAdjust.toLong())

        // 대운수(반올림 방식 선택)
        val rawYears = (abs(Duration.between(birth, term).toHours()).toDouble() / 24.0) / 3.0
        val daeNum = when (daeRound.lowercase()) {
            "floor" -> kotlin.math.floor(rawYears).toInt().coerceAtLeast(1)
            "round" -> round(rawYears).toInt().coerceAtLeast(1)
            else    -> ceil(rawYears).toInt().coerceAtLeast(1)
        }
        val startYear = info.sy + daeNum - 1

        // 라인들
        val mStem = monthGanji.substring(0, 1)
        val mBr   = monthGanji.substring(1, 2)
        val mStemIdx = GAN.indexOf(mStem)
        val mBrIdx   = JI.indexOf(mBr)

        val daeWoon = mutableListOf<String>()
        val daeWoonGanji = mutableListOf<String>()
        val daeWoonYear = mutableListOf<Int>()
        for (i in 1..10) {
            val gi = if (forward) (mStemIdx + i) % 10 else (mStemIdx - i + 100) % 10
            val bi = if (forward) (mBrIdx   + i) % 12 else (mBrIdx   - i + 120) % 12
            daeWoon.add("${i * 10}대운 (${if (forward) "순행" else "역사"})")
            daeWoonGanji.add(GAN[gi] + JI[bi])
            daeWoonYear.add(startYear + (i - 1) * 10)
        }

        val seunYear = mutableListOf<Int>()
        val seunGanji = mutableListOf<String>()
        for (i in 0 until 10) {
            val y2 = startYear + i
            seunYear.add(y2)
            seunGanji.add(GAN[(y2 + 6) % 10] + JI[(y2 + 8) % 12])
        }

        val yearGod  = tenGod(dayStem, yearStem)
        val monthGod = tenGod(dayStem, mStem)
        val dayGod   = "일간"
        val hourGod  = tenGod(dayStem, hourGan)

        return SajuResult(
            yearGanji = yearGanji,
            monthGanji = monthGanji,
            dayGanji = dayGanji,
            hourGanji = hourGanji,
            yearGod = yearGod,
            monthGod = monthGod,
            dayGod = dayGod,
            hourGod = hourGod,
            daeNum = daeNum,
            daeDir = dirLabel,
            daeWoon = daeWoon,
            daeWoonGanji = daeWoonGanji,
            daeWoonYear = daeWoonYear,
            seunYear = seunYear,
            seunGanji = seunGanji,
            solarText = "${info.sy}년 ${z2(info.sm)}월 ${z2(info.sd)}일 ${z2(birth.hour)}:${z2(birth.minute)}",
            lunarText = "음력 ${info.lm}월 ${info.ld}일" + if (info.leap) " (윤달)" else "",
            termName = rawTerm.name,
            termDate = term.toString().replace('T', ' ')
        )
    }
}
