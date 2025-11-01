package com.saju.manse_api.service

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

// 서버가 내려줄 응답 모델(십성 필드 포함)
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

    // 대운/세운
    val daeWoon: List<String> = emptyList(),
    val daeWoonGanji: List<String> = emptyList(),
    val daeWoonGod: List<String> = emptyList(),
    val daeWoonYear: List<Int> = emptyList(),

    val seunYear: List<Int> = emptyList(),
    val seunGanji: List<String> = emptyList(),
    val seunGod: List<String> = emptyList(),

    // (옵션) 요약 표기를 위해
    val solarText: String? = null,
    val lunarText: String? = null,
    val termName: String? = null,
    val termDate: String? = null
)

object SajuService {

    // 오행 매핑(천간 기준)
    private val STEM_ELEM = mapOf(
        "甲" to "wood", "乙" to "wood",
        "丙" to "fire", "丁" to "fire",
        "戊" to "earth","己" to "earth",
        "庚" to "metal","辛" to "metal",
        "壬" to "water","癸" to "water"
    )
    // 지지 오행(표시용)
    private val BRANCH_ELEM = mapOf(
        "子" to "water","丑" to "earth","寅" to "wood","卯" to "wood",
        "辰" to "earth","巳" to "fire","午" to "fire","未" to "earth",
        "申" to "metal","酉" to "metal","戌" to "earth","亥" to "water"
    )
    private val CYCLE = listOf("wood","fire","earth","metal","water")
    private val CONTROL = mapOf("wood" to "earth","fire" to "metal","earth" to "water","metal" to "wood","water" to "fire")
    private val YANG = setOf("甲","丙","戊","庚","壬")

    private fun elemOfStem(stem: String): String = STEM_ELEM[stem] ?: "earth"
    private fun isYang(stem: String) = stem in YANG

    // 십성 계산(일간 vs 상대 간) — 간지의 '간'만 사용
    private fun tenGod(dayStem: String, otherStem: String): String {
        if (dayStem.isBlank() || otherStem.isBlank()) return "-"
        val ed = elemOfStem(dayStem)
        val eo = elemOfStem(otherStem)
        val same = isYang(dayStem) == isYang(otherStem)
        val idx = { k: String -> CYCLE.indexOf(k) }
        return when {
            eo == ed -> if (same) "비견" else "겁재"
            eo == CYCLE[(idx(ed) + 1) % 5] -> if (same) "식신" else "상관"
            eo == CYCLE[(idx(ed) + 4) % 5] -> if (same) "정인" else "편인"
            eo == CONTROL[ed]             -> if (same) "정재" else "편재"
            CONTROL[eo] == ed             -> if (same) "정관" else "편관"
            else -> "-"
        }
    }

    // 시지 인덱스(0=子, 1=丑, ...) — pivotMinute=30이면 23:30부터 子시
    private fun hourBranchIndex(hour: Int, minute: Int, pivotMinute: Int): Int {
        val totalMin = hour * 60 + minute
        val start = 23 * 60 - pivotMinute
        val cycle = 12 * 120
        var t = (totalMin - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }

    // === 메인 진입 ===
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

        // DB에서 간지(년/월/일) 읽기
        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)

        require(info != null) {
            "해당 날짜 데이터가 없습니다: year=$year month=$month day=$day isLunar=$isLunar leap=$leap"
        }

        val yearGanji  = info!!.hy
        val monthGanji = info.hm
        val dayGanji   = info.hd

        // 시주
        val dayStemChar = dayGanji.substring(0, 1) // 일간
        val hIdx = hourBranchIndex(hour, minute, pivotMinute)
        val JI  = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
        val GAN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
        val hourJi = JI[hIdx]
        val dayStemIdx = GAN.indexOf(dayStemChar)
        val hourGanIdx = (dayStemIdx % 5) * 2 + hIdx
        val hourGan = GAN[hourGanIdx % 10]
        val hourGanji = hourGan + hourJi

        // 대운 방향(정사/역사)
        val yearStem = yearGanji.substring(0, 1)
        val isYearYang = isYang(yearStem)
        val forward = (isMale && isYearYang) || (!isMale && !isYearYang)
        val dirLabel = if (forward) "순행" else "역행"

        // 절기까지 차이 → 대운 시작나이(3일=1년, 올림)
        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val term  = if (forward) SeasonRepo.nextAfter(info.sy, info.sm, info.sd, hour)
                    else          SeasonRepo.prevBefore(info.sy, info.sm, info.sd, hour)
        val hoursDiff = abs(Duration.between(birth, term.dt).toHours().toDouble())
        val startAge  = max(1, ceil((hoursDiff / 24.0) / 3.0).toInt())
        val startYear = info.sy + startAge - 1

        // 대운 간지(월간/월지에서 1씩 전진/후진)
        val mStem = monthGanji.substring(0, 1)
        val mBr   = monthGanji.substring(1, 2)
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
            daeWoon.add("${i * 10}대운 ($dirLabel)")
            daeWoonGanji.add(g)
            daeWoonGod.add(tenGod(dayStemChar, GAN[gi])) // 일간 vs 대운 '간'
            daeWoonYear.add(startYear + (i - 1) * 10)
        }

        // 세운(첫 10년)
        val seunYear = mutableListOf<Int>()
        val seunGanji = mutableListOf<String>()
        val seunGod = mutableListOf<String>()
        for (i in 0 until 10) {
            val y = startYear + i
            val yg = GAN[(y + 6) % 10]
            val yb = JI[(y + 8) % 12]
            seunYear.add(y)
            seunGanji.add(yg + yb)
            seunGod.add(tenGod(dayStemChar, yg)) // 일간 vs 연운 '간'
        }

        // 각 기둥 십성
        val yearGod  = tenGod(dayStemChar, yearGanji.substring(0,1))
        val monthGod = tenGod(dayStemChar, monthGanji.substring(0,1))
        val dayGod   = "일간"
        val hourGod  = tenGod(dayStemChar, hourGanji.substring(0,1))

        // 요약 텍스트(옵션)
        val solarText = "${info.sy}년 ${zero2(info.sm)}월 ${zero2(info.sd)}일 ${zero2(hour)}:${zero2(minute)}"
        val lunarText = if (isLunar) "음력 ${month}월 ${day}일${if (leap) " (윤달)" else ""}" else null

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
            termName = null,   // 절기 이름/일시는 필요 시 SeasonRepo에서 채워 넣을 수 있음
            termDate = null
        )
    }

    private fun zero2(n: Int) = if (n < 10) "0$n" else "$n"
}
