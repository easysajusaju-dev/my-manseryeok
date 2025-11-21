package com.saju.engine

import com.saju.manse_api.service.SajuService
import com.saju.manse_api.service.SajuResult

// ==============================
// 프론트에서 바로 쓰기 위한 통합 결과 구조
// ==============================
data class DaewoonInfo(
    val direction: String,   // "forward" | "reverse"
    val startAge: Int        // 대운수 (전통나이 시작)
)

data class Relation(
    val from: String,        // "year" | "month" | "day" | "hour"
    val to: String,
    val kind: String         // "형" | "충" | "파" | "합" 등
)

data class RelationGroup(
    val hyung: List<Relation> = emptyList(),
    val chung: List<Relation> = emptyList(),
    val pa: List<Relation> = emptyList(),
    val hap: List<Relation> = emptyList()
)

data class SajuFullResult(
    val ganji: Map<String, String>,         // { year, month, day, hour }
    val sibsung: Map<String, String>,       // { year, month, day, hour } (십신 - 천간 기준)
    val branchSibsung: Map<String, String>, // 지지 십신 (지금은 비워둠)
    val twelve: Map<String, String>,        // 12운성 (지금은 비워둠)
    val daewoon: DaewoonInfo,               // 방향 + 대운수
    val relations: RelationGroup            // 형·충·파·합 (지금은 빈 값)
)

/**
 * 기존 SajuService.getSaju() 를 감싸서
 * 프론트엔드에서 쓰기 좋은 JSON 형태로 변환해 주는 통합 엔진.
 *
 * 👉 지금 단계에서는:
 *  - 사주 4주 간지
 *  - 천간 기준 십신(년/월/일/시)
 *  - 대운 방향/대운수
 * 만 정확히 채우고,
 *  - 지지 십신, 12운성, 형충파합은 일단 빈값으로 둔다.
 *    (프론트는 값이 없으면 그냥 공백으로 표시)
 */
object SajuFullEngine {

    fun run(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        isLunar: Boolean,
        leap: Boolean,
        isMale: Boolean
    ): SajuFullResult {

        // 기존 만세력 + 대운 계산 엔진 그대로 사용
        val base: SajuResult = SajuService.getSaju(
            year = year,
            month = month,
            day = day,
            hour = hour,
            minute = minute,
            isLunar = isLunar,
            leap = leap,
            isMale = isMale,
            pivotMin = 30,
            tzAdjust = -30,
            seasonAdjust = 0,
            daeRound = "floor"
        )

        // 4주 간지
        val ganji = mapOf(
            "year" to base.yearGanji,
            "month" to base.monthGanji,
            "day" to base.dayGanji,
            "hour" to base.hourGanji
        )

        // 천간 기준 십신
        val sibsung = mapOf(
            "year" to base.yearGod,
            "month" to base.monthGod,
            "day" to base.dayGod,
            "hour" to base.hourGod
        )

        // 아직 서버에서 계산 안 하는 값들은 일단 빈 맵으로 (프론트에서 null/undefined 취급)
        val branchSibsung: Map<String, String> = emptyMap()
        val twelve: Map<String, String> = emptyMap()

        // 대운 정보 → 방향 + 대운수
        val daewoon = DaewoonInfo(
            direction = if (base.daeDir == "정사") "forward" else "reverse",
            startAge = base.daeNum
        )

        // 형·충·파·합은 나중에 서버로 옮길 예정 → 지금은 빈값
        val relations = RelationGroup()

        return SajuFullResult(
            ganji = ganji,
            sibsung = sibsung,
            branchSibsung = branchSibsung,
            twelve = twelve,
            daewoon = daewoon,
            relations = relations
        )
    }
}
