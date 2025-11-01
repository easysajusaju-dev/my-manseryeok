package com.saju.manse_api.service

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

// 십성/절기/음력 텍스트 포함 응답
data class SajuResult(
    val yearGanji: String,
    val monthGanji: String,
    val dayGanji: String,
    val hourGanji: String,

    // 십성(일간 기준)
    val yearGod: String,
    val monthGod: String,
    val dayGod: String,
    val hourGod: String,

    // 대운/연운
    val daeWoon: List<String> = emptyList(),
    val daeWoonGanji: List<String> = emptyList(),
    val daeWoonGod: List<String> = emptyList(),
    val daeWoonYear: List<Int> = emptyList(),

    val seunYear: List<Int> = emptyList(),
    val seunGanji: List<String> = emptyList(),
    val seunGod: List<String> = emptyList(),

    // 요약 표시용
    val solarText: String? = null,
    val lunarText: String? = null,
    val termName: String? = null,
    val termDate: String? = null
)

object SajuService {

    private val STEM_ELEM = mapOf(
        "甲" to "wood","乙" to "wood",
        "丙" to "fire","丁" to "fire",
        "戊" to "earth","己" to "earth",
        "庚" to "metal","辛" to "metal",
        "壬" to "water","癸" to "water"
    )
    private val CYCLE = listOf("wood","fire","earth","metal","water")
    private val CONTROL = mapOf("wood" to "earth","fire" to "metal","earth" to "water","metal" to "wood","water" to "fire")
    private val YANG = setOf("甲","丙","戊","庚","壬")

    private fun elemOfStem(stem: String) = STEM_ELEM[stem] ?: "earth"
    private fun isYang(stem: String) = stem in YANG
    private fun idx(k: String) = CYCLE.indexOf(k)

    // 십성(일간 vs 상대 '간')
    private fun tenGod(dayStem: String, otherStem: String): String {
        if (dayStem.isBlank() || otherStem.isBlank()) return "-"
        val ed = elemOfStem(dayStem)
        val eo = elemOfStem(otherStem)
        val same = isYang(dayStem) == isYang(otherStem)
        return when {
            eo == ed                       -> if (same) "비견" else "겁재"
            eo == CYCLE[(idx(ed) + 1) % 5] -> if (same) "식신" else "상관"
            eo == CYCLE[(idx(ed) + 4) % 5] -> if (same) "정인" else "편인"
            eo == CONTROL[ed]              -> if (same) "정재" else "편재"
            CONTROL[eo] == ed              -> if (same) "정관" else "편관"
            else -> "-"
        }
    }

    private fun hourBranchIndex(hour: Int, minute: Int, pivotMinute: Int): Int {
        val totalMin = hour * 60 + minute
        val start = 23 * 60 - pivotMinute
        val cycle = 12 * 120
        var t = (totalMin - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }

    fun getSaju(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        isLunar: Boolean,
        leap: Boolean,
        isMale: Boolean,
        pivotMinute: Int = 30
    ): SajuResult {

        // DB 조회
        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)

        require(info != null) {
            "해당 날짜 데이터가 없습니다: year=$year month=$month day=$day isLunar=$isLunar leap=$leap"
        }

        val yearGanji = info!!.hy
        val monthGanji = info.hm
        val dayGanji = info.hd

        // 시주 계산
        val GAN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
        val JI  = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")

        val dayStem = dayGanji.substring(0,1)
        val hIdx = hourBranchIndex(hour, minute, pivotMinute)
        val hourJi = JI[hIdx]
        val dayStemIdx = GAN.indexOf(dayStem)
        val hourGan = GAN[(dayStemIdx % 5 * 2 + hIdx) % 10]
        val hourGanji = hourGan + hourJi

        // 대운 방향
        val yearStem = yearGanji.substring(0,1)
        val forward = (isMale && isYang(yearStem)) || (!isMale && !isYang(yearStem))
        val dirLabel = if (forward) "순행" else "역행"

        // 절기 차이 → 대운수
        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val term  = if (forward) SeasonRepo.nextAfter(info.sy, info.sm, info.sd, hour)
                    else          SeasonRepo.prevBefore(info.sy, info.sm, info.sd, hour)
        val hoursDiff = abs(Duration.between(birth, term.dt).toHours().toDouble())
        val startAge  = max(1, ceil((hoursDiff / 24.0) / 3.0).toInt())
        val startYear = info.sy + startAge - 1

        // 대운
        val mStem = monthGanji.substring(0,1)
        val mBr   = monthGanji.substring(1,1+1)
        val mStemIdx = GAN.indexOf(mStem)
        val mBrIdx   = JI.indexOf(mBr)

        val daeWoon = mutableListOf<String>()
        val daeWoonGanji = mutableListOf<String>()
        val daeWoonGod = mutableListOf<String>()
        val daeWoonYear = mutableListOf<Int>()

        for (i in 1..10) {
            val gi = if (forward) (mStemIdx + i) % 10 else (mStemIdx - i + 100) % 10
            val bi = if (forward) (mBrIdx   + i) % 12 else (mBrIdx   - i + 120) % 12
            val g  = GAN[gi] + JI[bi]
            daeWoon.add("${i*10}대운 ($dirLabel)")
            daeWoonGanji.add(g)
            daeWoonGod.add(tenGod(dayStem, GAN[gi]))
            daeWoonYear.add(startYear + (i-1)*10)
        }

        // 연운(첫 10년)
        val seunYear = mutableListOf<Int>()
        val seunGanji = mutableListOf<String>()
        val seunGod = mutableListOf<String>()
        for (i in 0 until 10) {
            val y = startYear + i
            val yg = GAN[(y + 6) % 10]
            val yb = JI[(y + 8) % 12]
            seunYear.add(y)
            seunGanji.add(yg + yb)
            seunGod.add(tenGod(dayStem, yg))
        }

        // 각 기둥 십성
        val yearGod  = tenGod(dayStem, yearGanji.substring(0,1))
        val monthGod = tenGod(dayStem, monthGanji.substring(0,1))
        val dayGod   = "일간"
        val hourGod  = tenGod(dayStem, hourGanji.substring(0,1))

        // 절기(현재 절기 = 기준 시각 이전 마지막 절기)
        val curTerm = SeasonRepo.currentAt(info.sy, info.sm, info.sd, hour)
        val tfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        // 텍스트
        val solarText = "${info.sy}년 ${z2(info.sm)}월 ${z2(info.sd)}일 ${z2(hour)}:${z2(minute)}"
        val lunarText = "음력 ${info.lm}월 ${info.ld}일" + if (info.leap) " (윤달)" else ""

        return SajuResult(
            yearGanji = yearGanji,
            monthGanji = monthGanji,
            dayGanji = dayGanji,
            hourGanji = hourGanji,

            yearGod = yearGod,
            monthGod = monthGod,
            dayGod = dayGod,
            hourGod = hourGod,

            daeWoon = daeWoon,
            daeWoonGanji = daeWoonGanji,
            daeWoonGod = daeWoonGod,
            daeWoonYear = daeWoonYear,

            seunYear = seunYear,
            seunGanji = seunGanji,
            seunGod = seunGod,

            solarText = solarText,
            lunarText = lunarText,
            termName  = curTerm.name,
            termDate  = curTerm.dt.format(tfmt)
        )
    }

    private fun z2(n: Int) = if (n < 10) "0$n" else "$n"
}
