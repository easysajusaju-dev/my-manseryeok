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

    // 🔥 대운수에 사용하는 "정절기" 목록
    // (월의 분기점이 되는 절기만 사용, 가운데 낀 중기는 제외)
    private val PRINCIPAL_TERMS = setOf(
        "입춘", "경칩", "청명",
        "입하", "망종", "소서",
        "입추", "백로", "한로",
        "입동", "대설", "소한"
    )

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

    /** 정절기인지 여부 */
    private fun isPrincipalTermName(name: String): Boolean =
        name in PRINCIPAL_TERMS

    /** 기준 시각 이후의 "다음 정절기" */
    private fun nextPrincipalAfter(y: Int, m: Int, d: Int, h: Int) : com.saju.manse_api.repo.SeasonPoint {
        var t = SeasonRepo.nextAfter(y, m, d, h)
        while (!isPrincipalTermName(t.name)) {
            val dt = t.dt
            t = SeasonRepo.nextAfter(dt.year, dt.monthValue, dt.dayOfMonth, dt.hour)
        }
        return t
    }

    /** 기준 시각 이전의 "직전 정절기" */
    private fun prevPrincipalBefore(y: Int, m: Int, d: Int, h: Int) : com.saju.manse_api.repo.SeasonPoint {
        var t = SeasonRepo.prevBefore(y, m, d, h)
        while (!isPrincipalTermName(t.name)) {
            val dt = t.dt
            t = SeasonRepo.prevBefore(dt.year, dt.monthValue, dt.dayOfMonth, dt.hour)
        }
        return t
    }

    // ======================================================
    //  ⭐ 기본 사주 API — /saju가 사용하는 함수
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
        val originalBirth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val birth = originalBirth.plusMinutes(tzAdjust.toLong())

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

        // 🔥 대운수에 사용할 "정절기" 선택
        // 순행: 앞으로 다가오는 정절기
        // 역행: 이미 지나간 정절기
        val daeTerm = if (forward)
            nextPrincipalAfter(info.sy, info.sm, info.sd, birth.hour)
        else
            prevPrincipalBefore(info.sy, info.sm, info.sd, birth.hour)

        val term = daeTerm.dt.plusMinutes(seasonAdjust.toLong())

        // 🔥 대운수 계산 (일수 기준)
        // 글 설명처럼 "며칠 남았/지났는지" 를 3으로 나눈다.
        val birth0 = birth.toLocalDate().atStartOfDay()
        val term0 = daeTerm.dt.toLocalDate().atStartOfDay()
        val diffDays = abs(Duration.between(birth0, term0).toDays().toDouble())
        val diffHours = diffDays * 24.0                       // debug용 유지
        val daeRaw = diffDays / 3.0

        val daeNum = when (daeRound.lowercase()) {
            "round" -> kotlin.math.round(daeRaw)
            "ceil" -> ceil(daeRaw)
            else -> floor(daeRaw)
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
            termName = daeTerm.name,
            termDate = term.toString().replace('T', ' ')
        )
    }



    // ======================================================
    //  ⭐ 앱 호환 모드 — /saju/compat (정시, 동경시 -30, 절기 0)
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


    // ======================================================
    //  ⭐ DEBUG API — 내부 모든 계산 추적
    // ======================================================
    fun debugSaju(
        year: Int, month: Int, day: Int,
        hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean,
        isMale: Boolean,
        pivotMin: Int,
        tzAdjust: Int,
        seasonAdjust: Int
    ): Map<String, Any?> {

        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)

        require(info != null)

        val originalBirth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val birthAdjusted = originalBirth.plusMinutes(tzAdjust.toLong())

        val ds = info.hd.substring(0, 1)
        val hIdx = hourBranchIndex(birthAdjusted.hour, birthAdjusted.minute, pivotMin)
        val hourJi = JI[hIdx]

        val dsIdx = GAN.indexOf(ds)
        val hourGan = GAN[(dsIdx % 5 * 2 + hIdx) % 10]
        val hourGanji = hourGan + hourJi

        val ys = info.hy.substring(0, 1)
        val forward = (isMale && isYang(ys)) || (!isMale && !isYang(ys))
        val dirLabel = if (forward) "정사" else "역사"

        // 🔥 debug에서도 대운수 기준 정절기를 동일하게 사용
        val daeTerm = if (forward)
            nextPrincipalAfter(info.sy, info.sm, info.sd, birthAdjusted.hour)
        else
            prevPrincipalBefore(info.sy, info.sm, info.sd, birthAdjusted.hour)

        val termAdjusted = daeTerm.dt.plusMinutes(seasonAdjust.toLong())

        val birth0 = birthAdjusted.toLocalDate().atStartOfDay()
        val term0 = daeTerm.dt.toLocalDate().atStartOfDay()
        val diffDays = abs(Duration.between(birth0, term0).toDays().toDouble())
        val diffHours = diffDays * 24.0
        val daeRaw = diffDays / 3.0
        val daeNum = floor(daeRaw).toInt().coerceAtLeast(1)

        val startYear = info.sy + daeNum - 1

        return mapOf(
            "input" to mapOf(
                "year" to year,
                "month" to month,
                "day" to day,
                "hour" to hour,
                "minute" to minute,
                "isLunar" to isLunar,
                "leap" to leap,
                "isMale" to isMale,
                "pivotMin" to pivotMin,
                "tzAdjust" to tzAdjust,
                "seasonAdjust" to seasonAdjust
            ),
            "dbInfo" to mapOf(
                "solarYMD" to "${info.sy}-${info.sm}-${info.sd}",
                "lunar" to "${info.lm}-${info.ld}",
                "hy" to info.hy,
                "hm" to info.hm,
                "hd" to info.hd
            ),
            "timeCalc" to mapOf(
                "originalBirth" to originalBirth.toString(),
                "birthAdjusted" to birthAdjusted.toString(),
                "hourBranchIndex" to hIdx,
                "hourJi" to hourJi,
                "hourGan" to hourGan,
                "hourGanji" to hourGanji
            ),
            "seasonCalc" to mapOf(
                "rawTermName" to daeTerm.name,
                "rawTermDate" to daeTerm.dt.toString(),
                "termAdjusted" to termAdjusted.toString()
            ),
            "daeCalc" to mapOf(
                "diffHours" to diffHours,   // 실제로는 일수×24
                "daeRaw" to daeRaw,
                "daeNum" to daeNum,
                "startYear" to startYear,
                "dir" to dirLabel
            ),
            "finalResult" to getSaju(
                year, month, day, hour, minute,
                isLunar, leap, isMale,
                pivotMin, tzAdjust, seasonAdjust
            )
        )
    }
}
