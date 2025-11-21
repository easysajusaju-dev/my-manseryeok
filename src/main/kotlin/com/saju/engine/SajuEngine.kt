package com.saju.engine

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// ===========================
//  ⭐ 최종 결과 구조
// ===========================
data class EngineResult(
    val ganji: Map<String, String>,         // year/month/day/hour 간지
    val sibsung: Map<String, String>,       // 천간 십성
    val branchSibsung: Map<String, String>, // 지지 십성
    val twelve: Map<String, String>,        // 12운성
    val daewoon: Map<String, Any>,          // 대운수 + 방향
    val daewoonList: List<String>,
    val daewoonGanji: List<String>,
    val daewoonYear: List<Int>,
    val relations: Map<String, List<Any>>   // 형충파해삼합 등
)

// ===========================
//  ⭐ 단일 사주 엔진
// ===========================
object SajuEngine {

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

    // ============================
    //  십성 계산
    // ============================
    private fun isYang(s: String) = s in YANG
    private fun elemOf(s: String) = STEM_ELEM[s] ?: "earth"
    private fun idx(e: String) = CYCLE.indexOf(e)

    private fun tenGod(dayStem: String, target: String): String {
        val me = elemOf(dayStem)
        val tg = elemOf(target)
        val same = isYang(dayStem) == isYang(target)

        return when {
            tg == me ->
                if (same) "비견" else "겁재"

            tg == CYCLE[(idx(me) + 1) % 5] ->
                if (same) "식신" else "상관"

            tg == CONTROL[me] ->
                if (same) "편재" else "정재"

            CONTROL[tg] == me ->
                if (same) "편관" else "정관"

            tg == CYCLE[(idx(me) + 4) % 5] ->
                if (same) "편인" else "정인"

            else -> "-"
        }
    }

    // ============================
    // 지지방의 십성 (지장간 기반)
    // ============================
    private val JIJANGGAN = mapOf(
        "子" to listOf("壬","癸"),
        "丑" to listOf("癸","辛","己"),
        "寅" to listOf("戊","丙","甲"),
        "卯" to listOf("甲","乙"),
        "辰" to listOf("乙","癸","戊"),
        "巳" to listOf("戊","庚","丙"),
        "午" to listOf("丙","己","丁"),
        "未" to listOf("丁","乙","己"),
        "申" to listOf("戊","壬","庚"),
        "酉" to listOf("庚","辛"),
        "戌" to listOf("辛","丁","戊"),
        "亥" to listOf("戊","甲","壬")
    )

    private fun branchGod(dayStem: String, branch: String): String {
        val gods = JIJANGGAN[branch] ?: return "-"
        val primary = gods.last()   // 본기 기준
        return tenGod(dayStem, primary)
    }

    // ============================
    // 12운성
    // ============================
    private val TWELVE = listOf(
        "장생","목욕","관대","임관","제왕","쇠","병","사","묘","절","태","양"
    )

    private fun twelveYun(dayStem: String, branch: String): String {
        val start = when(dayStem){
            "甲" -> 2; "乙" -> 8
            "丙" -> 5; "丁" -> 11
            "戊" -> 5; "己" -> 11
            "庚" -> 8; "辛" -> 2
            "壬" -> 11; "癸" -> 5
            else -> 0
        }
        val bi = JI.indexOf(branch)
        return TWELVE[(bi - start + 120) % 12]
    }

    // ============================
    // 시주 계산
    // ============================
    private fun hourGan(dayStem: String, hIdx: Int): String {
        val dsIdx = GAN.indexOf(dayStem)
        return GAN[(dsIdx % 5 * 2 + hIdx) % 10]
    }

    // ============================
    // 대운 수 계산
    // ============================
    private val PRINCIPAL = setOf("입춘","경칩","청명","입하","망종","소서","입추","백로","한로","입동","대설","소한")

    private fun nextPrincipal(info: ManseryeokRepo.Info, birthH: Int) =
        SeasonRepo.nextAfter(info.sy, info.sm, info.sd, birthH).let { t ->
            if (t.name in PRINCIPAL) t
            else SeasonRepo.nextPrincipalOnly(t)
        }

    private fun prevPrincipal(info: ManseryeokRepo.Info, birthH: Int) =
        SeasonRepo.prevBefore(info.sy, info.sm, info.sd, birthH).let { t ->
            if (t.name in PRINCIPAL) t
            else SeasonRepo.prevPrincipalOnly(t)
        }


    // ============================
    //  ⭐ 단일 API
    // ============================
    fun calculate(
        year: Int, month: Int, day: Int,
        hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean,
        isMale: Boolean,
        pivotMin: Int = 30,
        tzAdjust: Int = -30
    ): EngineResult {

        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else ManseryeokRepo.findBySolar(year, month, day)
            ?: throw IllegalArgumentException("Invalid date")

        val yearG = info.hy
        val monthG = info.hm
        val dayG = info.hd

        val dayStem = dayG.substring(0, 1)

        // 시간 보정(-30)
        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
            .plusMinutes(tzAdjust.toLong())

        // 시지
        val total = birth.hour * 60 + birth.minute
        val pivot = 23 * 60 - pivotMin
        var t = (total - pivot) % (12 * 120)
        if (t < 0) t += 12 * 120
        val hIdx = t / 120

        val hJi = JI[hIdx]
        val hGan = hourGan(dayStem, hIdx)
        val hourGanji = hGan + hJi

        // 대운 방향
        val yearStem = yearG.substring(0, 1)
        val forward = (isMale && isYang(yearStem)) || (!isMale && !isYang(yearStem))

        val daeTerm = if (forward) nextPrincipal(info, birth.hour) else prevPrincipal(info, birth.hour)
        val birth0 = birth.toLocalDate().atStartOfDay()
        val term0 = daeTerm.dt.toLocalDate().atStartOfDay()

        val diffDays = abs(Duration.between(birth0, term0).toDays().toDouble())
        val daeNum = floor(diffDays / 3.0).toInt().coerceAtLeast(1)

        val startYear = info.sy + daeNum - 1

        val ms = monthG.substring(0, 1)
        val mb = monthG.substring(1, 2)
        val msIdx = GAN.indexOf(ms)
        val mbIdx = JI.indexOf(mb)

        val dList = mutableListOf<String>()
        val dGList = mutableListOf<String>()
        val dYList = mutableListOf<Int>()

        for (i in 1..10) {
            val gi = if (forward) (msIdx + i) % 10 else (msIdx - i + 100) % 10
            val bi = if (forward) (mbIdx + i) % 12 else (mbIdx - i + 120) % 12

            dList.add("${i * 10}대운")
            dGList.add(GAN[gi] + JI[bi])
            dYList.add(startYear + (i - 1) * 10)
        }

        // ============================
        //  결과 반환
        // ============================
        return EngineResult(
            ganji = mapOf(
                "year" to yearG,
                "month" to monthG,
                "day" to dayG,
                "hour" to hourGanji
            ),
            sibsung = mapOf(
                "year" to tenGod(dayStem, yearG.substring(0, 1)),
                "month" to tenGod(dayStem, monthG.substring(0, 1)),
                "day" to "일간",
                "hour" to tenGod(dayStem, hGan)
            ),
            branchSibsung = mapOf(
                "year" to branchGod(dayStem, yearG.substring(1)),
                "month" to branchGod(dayStem, monthG.substring(1)),
                "day" to branchGod(dayStem, dayG.substring(1)),
                "hour" to branchGod(dayStem, hJi)
            ),
            twelve = mapOf(
                "year" to twelveYun(dayStem, yearG.substring(1)),
                "month" to twelveYun(dayStem, monthG.substring(1)),
                "day" to twelveYun(dayStem, dayG.substring(1)),
                "hour" to twelveYun(dayStem, hJi)
            ),
            daewoon = mapOf(
                "direction" to (if (forward) "forward" else "reverse"),
                "startAge" to daeNum
            ),
            daewoonList = dList,
            daewoonGanji = dGList,
            daewoonYear = dYList,
            relations = emptyMap() // 형충파해 삼합은 필요하면 붙일 수 있음
        )
    }
}
