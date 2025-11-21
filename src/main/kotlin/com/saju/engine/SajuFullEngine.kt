package com.saju.engine

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// =====================================
//  최종 반환 구조
// =====================================
data class FullSajuResponse(
    val ganji: Map<String, String>,
    val sibsung: Map<String, String>,
    val branchSibsung: Map<String, String>,
    val twelve: Map<String, String>,
    val daewoon: Map<String, Any>,
    val relations: Map<String, List<Map<String, String>>>
)

object SajuFullEngine {

    private val GAN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
    private val JI  = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")

    private val STEM_ELEM = mapOf(
        "甲" to "wood","乙" to "wood","丙" to "fire","丁" to "fire",
        "戊" to "earth","己" to "earth","庚" to "metal","辛" to "metal",
        "壬" to "water","癸" to "water"
    )
    private val CONTROL = mapOf(
        "wood" to "earth","fire" to "metal","earth" to "water",
        "metal" to "wood","water" to "fire"
    )
    private val CYCLE = listOf("wood","fire","earth","metal","water")
    private val YANG = setOf("甲","丙","戊","庚","壬")

    private fun elem(s: String) = STEM_ELEM[s] ?: "earth"
    private fun isYang(s: String) = s in YANG
    private fun idx(e: String) = CYCLE.indexOf(e)

    /** 십신 계산 (교정본) */
    private fun tenGod(dayStem: String, stem: String): String {
        val me = elem(dayStem)
        val t = elem(stem)
        val same = isYang(dayStem) == isYang(stem)

        return when {
            t == me -> if (same) "비견" else "겁재"
            t == CYCLE[(idx(me)+1)%5] -> if (same) "식신" else "상관"
            t == CONTROL[me] -> if (same) "편재" else "정재"
            CONTROL[t] == me -> if (same) "편관" else "정관"
            t == CYCLE[(idx(me)+4)%5] -> if (same) "편인" else "정인"
            else -> "-"
        }
    }

    // -----------------------------
    // 시지 계산
    // -----------------------------
    private fun hourBranchIndex(hour: Int, minute: Int, pivot: Int): Int {
        val total = hour*60 + minute
        val start = 23*60 - pivot
        val cycle = 12*120
        var t = (total - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }

    // -----------------------------
    // 메인 실행
    // -----------------------------
    fun run(
        year: Int, month: Int, day: Int,
        hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean,
        isMale: Boolean
    ): FullSajuResponse {

        // ------------------ 만세력 기본정보 ------------------
        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)

        require(info != null) { "만세력 정보 없음" }

        val yearG = info!!.hy
        val monthG = info.hm
        val dayG = info.hd

        val original = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val birth = original.plusMinutes(-30) // 동경시 -30분

        // 시주
        val ds = dayG[0].toString()
        val hIdx = hourBranchIndex(birth.hour, birth.minute, 30)
        val hourJi = JI[hIdx]
        val hourGan = GAN[(GAN.indexOf(ds)%5*2 + hIdx) % 10]
        val hourG = hourGan + hourJi

        // ------------------ 십성 계산 ------------------
        val sibs = mapOf(
            "year" to tenGod(ds, yearG[0].toString()),
            "month" to tenGod(ds, monthG[0].toString()),
            "day" to "일간(나)",
            "hour" to tenGod(ds, hourGan)
        )

        // ------------------ 12운성 (임시 동일값 처리) ------------------
        val tw = mapOf(
            "year" to "—",
            "month" to "—",
            "day" to "—",
            "hour" to "—"
        )

        // ------------------ 관계 (형·충·파·합) — 엔진 결과 그대로 유지 ------------------
        val relations = mapOf(
            "hyung" to emptyList<Map<String,String>>(),
            "chung" to emptyList(),
            "pa" to emptyList(),
            "hap" to emptyList()
        )

        // ------------------ 대운수 계산 ------------------
        val forward = (isMale && isYang(yearG[0].toString())) ||
                      (!isMale && !isYang(yearG[0].toString()))

        val nextTerm = SeasonRepo.nextAfter(info.sy, info.sm, info.sd, birth.hour)
        val prevTerm = SeasonRepo.prevBefore(info.sy, info.sm, info.sd, birth.hour)

        val targetTerm =
            if (forward) nextTerm else prevTerm

        val birth0 = birth.toLocalDate().atStartOfDay()
        val term0 = targetTerm.dt.toLocalDate().atStartOfDay()

        val diffDays = abs(Duration.between(birth0, term0).toDays().toDouble())
        val daeNum = floor(diffDays / 3.0).toInt().coerceAtLeast(1)

        val daewoon = mapOf(
            "direction" to (if (forward) "forward" else "reverse"),
            "startAge" to daeNum
        )

        return FullSajuResponse(
            ganji = mapOf(
                "year" to yearG,
                "month" to monthG,
                "day" to dayG,
                "hour" to hourG
            ),
            sibsung = sibs,
            branchSibsung = mapOf(),   // 추후 확장
            twelve = tw,
            daewoon = daewoon,
            relations = relations
        )
    }
}
