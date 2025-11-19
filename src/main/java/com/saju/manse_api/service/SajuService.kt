package com.saju.manse_api.service

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// ================================
//  최종 결과 데이터 구조
// ================================
data class SajuResult(
    val yearGanji: String,
    val monthGanji: String,
    val dayGanji: String,
    val hourGanji: String,
    val yearGod: String,
    val monthGod: String,
    val dayGod: String,
    val hourGod: String,
    val daeNum: Int,
    val daeDir: String,
    val daeWoon: List<String>,
    val daeWoonGanji: List<String>,
    val daeWoonYear: List<Int>,
    val seunYear: List<Int>,
    val seunGanji: List<String>,
    val solarText: String?,
    val lunarText: String?,
    val termName: String?,
    val termDate: String?
)


// ================================
//  사주 계산 핵심 서비스
// ================================
object SajuService {

    // 천간/지지
    private val GAN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
    private val JI  = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")

    private val STEM_ELEM = mapOf(
        "甲" to "wood","乙" to "wood","丙" to "fire","丁" to "fire",
        "戊" to "earth","己" to "earth","庚" to "metal","辛" to "metal",
        "壬" to "water","癸" to "water"
    )
    private val CYCLE = listOf("wood","fire","earth","metal","water")
    private val CONTROL = mapOf(
        "wood" to "earth","fire" to "metal","earth" to "water",
        "metal" to "wood","water" to "fire"
    )
    private val YANG = setOf("甲","丙","戊","庚","壬")

    private fun isYang(s: String) = s in YANG
    private fun elemOfStem(s: String) = STEM_ELEM[s] ?: "earth"
    private fun idx(d: String) = CYCLE.indexOf(d)

    private fun z2(n: Int) = n.toString().padStart(2, '0')

    // 십신 계산
    private fun tenGod(dayStem: String, stem: String): String {
        val ed = elemOfStem(dayStem)
        val eo = elemOfStem(stem)
        val same = isYang(dayStem) == isYang(stem)

        return when {
            eo == ed -> if (same) "비견" else "겁재"
            eo == CYCLE[(idx(ed) + 1) % 5] -> if (same) "식신" else "상관"
            eo == CYCLE[(idx(ed) + 4) % 5] -> if (same) "정인" else "편인"
            eo == CONTROL[ed] -> if (same) "정재" else "편재"
            CONTROL[eo] == ed -> if (same) "정관" else "편관"
            else -> "-"
        }
    }

    /** 시지 계산 (앱 그대로) */
    private fun hourBranchIndex(hour: Int, minute: Int, pivot: Int): Int {
        val total = hour * 60 + minute
        val start = 23 * 60 - pivot   // 기준점: 23:00
        val cycle = 12 * 120          // 2시간 × 12지
        var t = (total - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }


    // ======================================================
    //  ⭐ 기본 사주 API — 네 /saju 엔드포인트가 사용하는 함수
    // ======================================================
    fun getSaju(
        year: Int, month: Int, day: Int,
        hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean,
        isMale: Boolean,
        pivotMin: Int = 30,
        tzAdjust: Int = -30,
        seasonAdjust: Int = 0,
        daeRound: String = "floor"
    ): SajuResult {

        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)

        require(info != null) { "해당 날짜 데이터 없음" }

        val yearGanji = info!!.hy
        val monthGanji = info.hm
        val dayGanji = info.hd

        // 출생시간 (동경시 보정 적용)
        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
            .plusMinutes(tzAdjust.toLong())

        // 시주 계산
        val dayStem = dayGanji.substring(0, 1)
        val hIdx = hourBranchIndex(birth.hour, birth.minute, pivotMin)
        val hourJi = JI[hIdx]
        val dayStemIdx = GAN.indexOf(dayStem)
        val hourGan = GAN[(dayStemIdx % 5 * 2 + hIdx) % 10]
        val hourGanji = hourGan + hourJi

        // 방향(정사/역사)
        val yearStem = yearGanji.substring(0, 1)
        val forward = (isMale && isYang(yearStem)) || (!isMale && !isYang(yearStem))
        val dirLabel = if (forward) "정사" else "역사"

        // 절기 시점
        val rawTerm = if (forward)
            SeasonRepo.nextAfter(info.sy, info.sm, info.sd, birth.hour)
        else
            SeasonRepo.prevBefore(info.sy, info.sm, info.sd, birth.hour)

        val term = rawTerm.dt.plusMinutes(seasonAdjust.toLong())

        // 대운수 계산
        val diffHours = abs(Duration.between(birth, term).toHours().toDouble())
        val daiRaw = (diffHours / 24.0) / 3.0

        val daeNum = when (daeRound.lowercase()) {
            "round" -> kotlin.math.round(daiRaw)
            "ceil" -> ceil(daiRaw)
            else -> floor(daiRaw)
        }.toInt().coerceAtLeast(1)

        val startYear = info.sy + daeNum - 1


        // =========================
        // 대운
        // =========================
        val mStem = monthGanji.substring(0, 1)
        val mBr = monthGanji.substring(1, 2)
        val msIdx = GAN.indexOf(mStem)
        val mbIdx = JI.indexOf(mBr)

        val daeWoon = mutableListOf<String>()
        val daeWoonGanji = mutableListOf<String>()
        val daeWoonYear = mutableListOf<Int>()

        for (i in 1..10) {
            val gi = if (forward) (msIdx + i) % 10 else (msIdx - i + 100) % 10
            val bi = if (forward) (mbIdx + i) % 12 else (mbIdx - i + 120) % 12

            daeWoon.add("${i * 10}대운 (${if (forward) "순행" else "역사"})")
            daeWoonGanji.add(GAN[gi] + JI[bi])
            daeWoonYear.add(startYear + (i - 1) * 10)
        }

        // =========================
        // 연운
        // =========================
        val seunYear = mutableListOf<Int>()
        val seunGanji = mutableListOf<String>()

        for (i in 0 until 10) {
            val y2 = startYear + i
            seunYear.add(y2)
            seunGanji.add(GAN[(y2 + 6) % 10] + JI[(y2 + 8) % 12])
        }

        return SajuResult(
            yearGanji = yearGanji,
            monthGanji = monthGanji,
            dayGanji = dayGanji,
            hourGanji = hourGanji,

            yearGod = tenGod(dayStem, yearStem),
            monthGod = tenGod(dayStem, mStem),
            dayGod = "일간",
            hourGod = tenGod(dayStem, hourGan),

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



    // ======================================================
    //  ⭐ 앱 호환 모드 — /saju/compat 엔드포인트
    // ======================================================
    fun getSajuCompat(
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean, isMale: Boolean,
        pivotMin: Int = 0, tzAdjust: Int = -30,
        seasonAdjust: Int = 0, daeRound: String = "floor"
    ): SajuResult {

        return getSaju(
            year, month, day, hour, minute,
            isLunar, leap, isMale,
            pivotMin, tzAdjust, seasonAdjust, daeRound
        )
    }
}
