package com.saju.manse_api.service

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonPoint
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// ================================
//  추가 데이터 구조 (프론트 엔진 대체용)
// ================================
data class FourCols(
    val year: String,
    val month: String,
    val day: String,
    val hour: String
)

data class BranchRelationItem(
    val from: String,   // "year" / "month" / "day" / "hour"
    val to: String,
    val branches: String,
    val kind: String    // "형" / "충" / "파" / "합"
)

data class BranchRelations(
    val hyung: List<BranchRelationItem>,
    val chung: List<BranchRelationItem>,
    val pa: List<BranchRelationItem>,
    val hap: List<BranchRelationItem>
)

data class SeunInfo(
    val years: List<Int>,
    val ganji: List<String>
)

data class WolunInfo(
    val year: Int,
    val months: List<Int>,
    val ganji: List<String>
)

// ================================
//  최종 결과 데이터 구조
//  (기존 + 프론트에서 하던 것 모두 포함)
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
    val termDate: String?,

    // 🔥 프론트 사주엔진이 계산하던 부분들
    val sibsung: FourCols?,          // 천간 십성
    val branchSibsung: FourCols?,    // 지지 십성
    val twelve: FourCols?,           // 12운성
    val relations: BranchRelations?, // 형충파합
    val seun: SeunInfo?,             // 연운(세운)
    val wolun: WolunInfo?            // 월운
)


// ================================
//  사주 계산 핵심 서비스
// ================================
object SajuService {

    // 천간/지지
    private val GAN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
    private val JI  = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")

    // 천간 오행
    private val STEM_ELEM = mapOf(
        "甲" to "wood","乙" to "wood",
        "丙" to "fire","丁" to "fire",
        "戊" to "earth","己" to "earth",
        "庚" to "metal","辛" to "metal",
        "壬" to "water","癸" to "water"
    )

    private val CYCLE = listOf("wood","fire","earth","metal","water")

    private val CONTROL = mapOf(
        "wood" to "earth",
        "fire" to "metal",
        "earth" to "water",
        "metal" to "wood",
        "water" to "fire"
    )

    private val YANG_STEM = setOf("甲","丙","戊","庚","壬")

    private fun isYangStem(s: String) = s in YANG_STEM
    private fun elemOfStem(s: String) = STEM_ELEM[s] ?: "earth"
    private fun idx(d: String) = CYCLE.indexOf(d)
    private fun z2(n: Int) = n.toString().padStart(2, '0')

    // ---------- 지지 음양/오행 ----------
    private val BRANCH_YINYANG = mapOf(
        "子" to "음", "丑" to "음", "寅" to "양", "卯" to "음",
        "辰" to "양", "巳" to "양", "午" to "음", "未" to "음",
        "申" to "양", "酉" to "음", "戌" to "양", "亥" to "양"
    )
    private val BRANCH_ELEM = mapOf(
        "子" to "water","丑" to "earth","寅" to "wood","卯" to "wood",
        "辰" to "earth","巳" to "fire","午" to "fire","未" to "earth",
        "申" to "metal","酉" to "metal","戌" to "earth","亥" to "water"
    )
    private fun elemOfBranch(b: String) = BRANCH_ELEM[b] ?: "earth"
    private fun isYangBranch(b: String) = BRANCH_YINYANG[b] == "양"

    // ============ 십성 계산 (정/편 기준) ============

    private fun tenGod(dayStem: String, stem: String): String {
        val ed = elemOfStem(dayStem)
        val eo = elemOfStem(stem)
        val same = isYangStem(dayStem) == isYangStem(stem)

        return when {
            eo == ed ->
                if (same) "비견" else "겁재"

            eo == CYCLE[(idx(ed) + 1) % 5] ->
                if (same) "식신" else "상관"

            eo == CYCLE[(idx(ed) + 4) % 5] ->
                if (same) "편인" else "정인"

            eo == CONTROL[ed] ->
                if (same) "편재" else "정재"

            CONTROL[eo] == ed ->
                if (same) "편관" else "정관"

            else -> "-"
        }
    }

    // 지지 기준 십성
    private fun tenGodBranch(dayStem: String, branch: String): String {
        val ed = elemOfStem(dayStem)
        val eo = elemOfBranch(branch)
        val same = isYangStem(dayStem) == isYangBranch(branch)

        return when {
            eo == ed ->
                if (same) "비견" else "겁재"

            eo == CYCLE[(idx(ed) + 1) % 5] ->
                if (same) "식신" else "상관"

            eo == CYCLE[(idx(ed) + 4) % 5] ->
                if (same) "편인" else "정인"

            eo == CONTROL[ed] ->
                if (same) "편재" else "정재"

            CONTROL[eo] == ed ->
                if (same) "편관" else "정관"

            else -> "미정"
        }
    }

    /** 시지 계산 (앱 로직 그대로) */
    private fun hourBranchIndex(hour: Int, minute: Int, pivot: Int): Int {
        val total = hour * 60 + minute
        val start = 23 * 60 - pivot
        val cycle = 12 * 120
        var t = (total - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }

    // 🔥 대운수에 사용하는 "정절기"
    private val PRINCIPAL_TERMS = setOf(
        "입춘", "경칩", "청명",
        "입하", "망종", "소서",
        "입추", "백로", "한로",
        "입동", "대설", "소한"
    )

    private fun isPrincipalTermName(name: String): Boolean =
        name in PRINCIPAL_TERMS

    /** 기준 시각 이후의 "다음 정절기" */
    private fun nextPrincipalAfter(y: Int, m: Int, d: Int, h: Int) : SeasonPoint {
        var t = SeasonRepo.nextAfter(y, m, d, h)
        while (!isPrincipalTermName(t.name)) {
            val dt = t.dt
            t = SeasonRepo.nextAfter(dt.year, dt.monthValue, dt.dayOfMonth, dt.hour)
        }
        return t
    }

    /** 기준 시각 이전의 "직전 정절기" */
    private fun prevPrincipalBefore(y: Int, m: Int, d: Int, h: Int) : SeasonPoint {
        var t = SeasonRepo.prevBefore(y, m, d, h)
        while (!isPrincipalTermName(t.name)) {
            val dt = t.dt
            t = SeasonRepo.prevBefore(dt.year, dt.monthValue, dt.dayOfMonth, dt.hour)
        }
        return t
    }

    // ================= 12운성 표 =================

    private val twelveUnseongTable: Map<String, Map<String, String>> = mapOf(
        "寅" to mapOf("甲" to "건록","乙" to "제왕","丙" to "장생","丁" to "사지","戊" to "장생","己" to "사지","庚" to "절지","辛" to "태지","壬" to "병지","癸" to "목욕"),
        "卯" to mapOf("甲" to "제왕","乙" to "건록","丙" to "목욕","丁" to "병지","戊" to "목욕","己" to "병지","庚" to "태지","辛" to "절지","壬" to "사지","癸" to "장생"),
        "辰" to mapOf("甲" to "쇠지","乙" to "관대","丙" to "관대","丁" to "쇠지","戊" to "관대","己" to "쇠지","庚" to "양지","辛" to "묘지","壬" to "묘지","癸" to "양지"),
        "巳" to mapOf("甲" to "병지","乙" to "목욕","丙" to "건록","丁" to "제왕","戊" to "건록","己" to "제왕","庚" to "장생","辛" to "사지","壬" to "절지","癸" to "태지"),
        "午" to mapOf("甲" to "사지","乙" to "장생","丙" to "제왕","丁" to "건록","戊" to "제왕","己" to "건록","庚" to "목욕","辛" to "병지","壬" to "태지","癸" to "절지"),
        "未" to mapOf("甲" to "묘지","乙" to "양지","丙" to "쇠지","丁" to "관대","戊" to "쇠지","己" to "관대","庚" to "쇠지","辛" to "양지","壬" to "묘지","癸" to "묘지"),
        "申" to mapOf("甲" to "절지","乙" to "태지","丙" to "병지","丁" to "목욕","戊" to "병지","己" to "목욕","庚" to "건록","辛" to "제왕","壬" to "장생","癸" to "사지"),
        "酉" to mapOf("甲" to "태지","乙" to "절지","丙" to "사지","丁" to "장생","戊" to "사지","己" to "장생","庚" to "제왕","辛" to "건록","壬" to "목욕","癸" to "병지"),
        "戌" to mapOf("甲" to "양지","乙" to "묘지","丙" to "묘지","丁" to "양지","戊" to "묘지","己" to "양지","庚" to "쇠지","辛" to "관대","壬" to "관대","癸" to "쇠지"),
        "亥" to mapOf("甲" to "장생","乙" to "사지","丙" to "절지","丁" to "태지","戊" to "절지","己" to "태지","庚" to "병지","辛" to "목욕","壬" to "건록","癸" to "제왕"),
        "子" to mapOf("甲" to "목욕","乙" to "병지","丙" to "태지","丁" to "절지","戊" to "태지","己" to "절지","庚" to "사지","辛" to "장생","壬" to "제왕","癸" to "건록"),
        "丑" to mapOf("甲" to "관대","乙" to "쇠지","丙" to "양지","丁" to "묘지","戊" to "양지","己" to "묘지","庚" to "양지","辛" to "쇠지","壬" to "절지","癸" to "관대")
    )

    private fun getTwelve(dayStem: String, branch: String): String {
        val row = twelveUnseongTable[branch] ?: return "미정"
        return row[dayStem] ?: "미정"
    }

    // ================= 형·충·파·합 =================

    private val HYUNG_SET = setOf(
        "寅巳","巳寅","寅申","申寅","巳申","申巳",
        "丑戌","戌丑","丑未","未丑","戌未","未戌",
        "子卯","卯子",
        "辰辰","午午","酉酉","亥亥"
    )

    private val CHUNG_SET = setOf(
        "子午","午자",
        "丑未","未丑",
        "寅申","申寅",
        "卯酉","酉卯",
        "辰戌","戌辰",
        "巳亥","亥巳"
    )

    private val PA_SET = setOf(
        "子酉","酉子",
        "丑辰","辰丑",
        "寅亥","亥寅",
        "巳申","申巳",
        "午卯","卯午",
        "戌未","未戌"
    )

    private val HAP_SET = setOf(
        "子丑","丑자",
        "寅亥","亥寅",
        "卯戌","戌卯",
        "辰酉","酉辰",
        "巳申","申巳",
        "午未","未午"
    )

    private fun getBranchRelations(
        yearBr: String,
        monthBr: String,
        dayBr: String,
        hourBr: String
    ): BranchRelations {
        val branches = mapOf(
            "year" to yearBr,
            "month" to monthBr,
            "day" to dayBr,
            "hour" to hourBr
        )
        val keys = listOf("year","month","day","hour")

        val hyung = mutableListOf<BranchRelationItem>()
        val chung = mutableListOf<BranchRelationItem>()
        val pa = mutableListOf<BranchRelationItem>()
        val hap = mutableListOf<BranchRelationItem>()

        fun push(list: MutableList<BranchRelationItem>, kind: String, a: String, b: String) {
            val ba = branches[a] ?: ""
            val bb = branches[b] ?: ""
            list.add(
                BranchRelationItem(
                    from = a,
                    to = b,
                    branches = ba + bb,
                    kind = kind
                )
            )
        }

        for (i in keys.indices) {
            for (j in i + 1 until keys.size) {
                val a = keys[i]
                val b = keys[j]
                val pair = (branches[a] ?: "") + (branches[b] ?: "")

                if (HYUNG_SET.contains(pair)) push(hyung, "형", a, b)
                if (CHUNG_SET.contains(pair)) push(chung, "충", a, b)
                if (PA_SET.contains(pair)) push(pa, "파", a, b)
                if (HAP_SET.contains(pair)) push(hap, "합", a, b)
            }
        }

        return BranchRelations(
            hyung = hyung,
            chung = chung,
            pa = pa,
            hap = hap
        )
    }

    // =============== 월운 계산 ===============

    private val MONTH_BRANCH_ORDER =
        listOf("寅","卯","辰","巳","午","未","申","酉","戌","亥","子","丑")

    private val MONTH_STEM_START_BY_YEAR_STEM = mapOf(
        "甲" to "丙","己" to "丙",
        "乙" to "戊","庚" to "戊",
        "丙" to "庚","辛" to "庚",
        "丁" to "壬","壬" to "壬",
        "戊" to "甲","癸" to "甲"
    )

    private fun stepStem(stem: String, step: Int): String {
        val idx = GAN.indexOf(stem)
        if (idx == -1) return stem
        val next = (idx + step + 10) % 10
        return GAN[next]
    }

    private fun calcWolun(birthYear: Int, yearStem: String): WolunInfo? {
        val firstStem = MONTH_STEM_START_BY_YEAR_STEM[yearStem] ?: return null

        val months = mutableListOf<Int>()
        val ganji = mutableListOf<String>()

        var stem = firstStem
        for (i in 0 until 12) {
            val month = i + 1
            val branch = MONTH_BRANCH_ORDER[i]
            months.add(month)
            ganji.add(stem + branch)
            stem = stepStem(stem, 1)
        }
        return WolunInfo(year = birthYear, months = months, ganji = ganji)
    }

    // ======================================================
    //  ⭐ 기본 사주 계산
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
        val originalBirth =
            LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
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
        val forward = (isMale && isYangStem(yearStem)) ||
                (!isMale && !isYangStem(yearStem))
        val dirLabel = if (forward) "정사" else "역사"

        // 🔥 대운수용 정절기
        val daeTerm = if (forward)
            nextPrincipalAfter(info.sy, info.sm, info.sd, birth.hour)
        else
            prevPrincipalBefore(info.sy, info.sm, info.sd, birth.hour)

        val term = daeTerm.dt.plusMinutes(seasonAdjust.toLong())

        // 🔥 대운수 계산 (일수 기준)
        val birth0 = birth.toLocalDate().atStartOfDay()
        val term0 = daeTerm.dt.toLocalDate().atStartOfDay()
        val diffDays =
            abs(Duration.between(birth0, term0).toDays().toDouble())
        val daeRaw = diffDays / 3.0

        val daeNum = when (daeRound.lowercase()) {
            "round" -> kotlin.math.round(daeRaw)
            "ceil"  -> ceil(daeRaw)
            else    -> floor(daeRaw)
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
        // 연운(세운)
        // =========================
        val seunYear = mutableListOf<Int>()
        val seunGanji = mutableListOf<String>()

        for (i in 0 until 10) {
            val y2 = startYear + i
            seunYear.add(y2)
            seunGanji.add(
                GAN[(y2 + 6) % 10] +
                        JI[(y2 + 8) % 12]
            )
        }
        val seunInfo = SeunInfo(seunYear, seunGanji)

        // =========================
        // 십성/지지십성/12운성/형충파합/월운
        // =========================
        val yearBranch = yearGanji.substring(1, 2)
        val monthBranch = monthGanji.substring(1, 2)
        val dayBranch = dayGanji.substring(1, 2)
        val hourBranch = hourJi

        val yearGod = tenGod(dayStem, yearStem)
        val monthGod = tenGod(dayStem, mStem)
        val hourGod = tenGod(dayStem, hourGan)

        val sibsung = FourCols(
            year = yearGod,
            month = monthGod,
            day = "비견",          // 일간 기준
            hour = hourGod
        )

        val branchSibsung = FourCols(
            year = tenGodBranch(dayStem, yearBranch),
            month = tenGodBranch(dayStem, monthBranch),
            day = tenGodBranch(dayStem, dayBranch),
            hour = tenGodBranch(dayStem, hourBranch)
        )

        val twelve = FourCols(
            year = getTwelve(dayStem, yearBranch),
            month = getTwelve(dayStem, monthBranch),
            day = getTwelve(dayStem, dayBranch),
            hour = getTwelve(dayStem, hourBranch)
        )

        val relations = getBranchRelations(
            yearBranch,
            monthBranch,
            dayBranch,
            hourBranch
        )

        val wolun = calcWolun(info.sy, yearStem)

        return SajuResult(
            yearGanji = yearGanji,
            monthGanji = monthGanji,
            dayGanji = dayGanji,
            hourGanji = hourGanji,

            yearGod = yearGod,
            monthGod = monthGod,
            dayGod = "일간",
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
            termName = daeTerm.name,
            termDate = term.toString().replace('T', ' '),

            sibsung = sibsung,
            branchSibsung = branchSibsung,
            twelve = twelve,
            relations = relations,
            seun = seunInfo,
            wolun = wolun
        )
    }

    // ======================================================
    //  ⭐ 앱 호환 모드 — /saju/compat
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
    //  ⭐ DEBUG API — /saju/debug 에서 사용
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
        val forward = (isMale && isYangStem(ys)) || (!isMale && !isYangStem(ys))
        val dirLabel = if (forward) "정사" else "역사"

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
                "diffHours" to diffHours,
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
