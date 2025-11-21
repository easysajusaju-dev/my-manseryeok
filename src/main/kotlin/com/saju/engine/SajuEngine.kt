package com.saju.engine

import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// -------------------------------
// 1) 기본 테이블
// -------------------------------
private val STEMS = listOf("갑","을","병","정","무","기","경","신","임","계")
private val BRANCH = listOf("자","축","인","묘","진","사","오","미","신","유","술","해")

// 오행
private val STEM_ELEM = mapOf(
    "갑" to "목","을" to "목",
    "병" to "화","정" to "화",
    "무" to "토","기" to "토",
    "경" to "금","신" to "금",
    "임" to "수","계" to "수"
)

private val BRANCH_ELEM = mapOf(
    "자" to "수","축" to "토","인" to "목","묘" to "목",
    "진" to "토","사" to "화","오" to "화","미" to "토",
    "신" to "금","유" to "금","술" to "토","해" to "수"
)

// 음양
private val STEM_YINYANG = mapOf(
    "갑" to "양","을" to "음","병" to "양","정" to "음","무" to "양",
    "기" to "음","경" to "양","신" to "음","임" to "양","계" to "음"
)

// 생(生) cycle
private val CYCLE = listOf("목","화","토","금","수") // 목→화→토→금→수→목

// 극(剋)
private val CONTROL = mapOf(
    "목" to "토",
    "화" to "금",
    "토" to "수",
    "금" to "목",
    "수" to "화"
)

private fun idx(elem: String) = CYCLE.indexOf(elem)
private fun isYang(stem: String) = STEM_YINYANG[stem] == "양"
private fun elemOfStem(s: String) = STEM_ELEM[s]!!

// -------------------------------
// 2) 십신 계산
// -------------------------------
private fun getTenGod(dayStem: String, targetStem: String): String {
    val ed = elemOfStem(dayStem)
    val eo = elemOfStem(targetStem)
    val same = isYang(dayStem) == isYang(targetStem)

    return when {
        // ① 비견/겁재(같은 오행)
        eo == ed -> if (same) "비견" else "겁재"

        // ② 식신/상관 (내가 생 → +1)
        eo == CYCLE[(idx(ed) + 1) % 5] -> if (same) "식신" else "상관"

        // ③ 재성 (내가 극)
        CONTROL[ed] == eo -> if (same) "정재" else "편재"

        // ④ 관성 (나를 극)
        CONTROL[eo] == ed -> if (same) "정관" else "편관"

        // ⑤ 인성 (나를 생 → -1)
        eo == CYCLE[(idx(ed) + 4) % 5] -> if (same) "정인" else "편인"

        else -> "-"
    }
}

// -------------------------------
// 3) 십이운성 (일간 기준)
// -------------------------------
private val TWELVE = listOf(
    "장생","목욕","관대","임관","제왕","쇠","병","사","묘","절","태","양"
)

private val TWELVE_MAP: Map<String, List<String>> = mapOf(
    "갑" to listOf("해","자","축","인","묘","진","사","오","미","신","유","술"),
    "을" to listOf("오","미","신","유","술","해","자","축","인","묘","진","사"),
    "병" to listOf("인","묘","진","사","오","미","신","유","술","해","자","축"),
    "정" to listOf("유","술","해","자","축","인","묘","진","사","오","미","신"),
    "무" to listOf("사","오","미","신","유","술","해","자","축","인","묘","진"),
    "기" to listOf("자","축","인","묘","진","사","오","미","신","유","술","해"),
    "경" to listOf("진","사","오","미","신","유","술","해","자","축","인","묘"),
    "신" to listOf("해","자","축","인","묘","진","사","오","미","신","유","술"),
    "임" to listOf("술","해","자","축","인","묘","진","사","오","미","신","유"),
    "계" to listOf("묘","진","사","오","미","신","유","술","해","자","축","인")
)

private fun getTwelve(dayStem: String, branch: String): String {
    val arr = TWELVE_MAP[dayStem] ?: return "-"
    val idx = arr.indexOf(branch)
    return if (idx >= 0) TWELVE[idx] else "-"
}

// -------------------------------
// 4) 합·충·형·파 계산 (간단버전)
// -------------------------------
data class Relation(val kind: String, val from: String, val to: String)

private val CHUNG = listOf(
    "자-오","축-미","인-신","묘-유","진-술","사-해"
)

private fun getRelations(): List<Relation> {
    val rel = mutableListOf<Relation>()

    fun add(kind: String, a: String, b: String) {
        rel += Relation(kind, a, b)
        rel += Relation(kind, b, a)
    }

    CHUNG.forEach {
        val (a, b) = it.split("-")
        add("충", a, b)
    }

    return rel
}

private val REL_LIST = getRelations()

// -------------------------------
// 5) 대운 계산
// -------------------------------
private fun calcDaeun(birth: LocalDateTime, term: LocalDateTime, isMale: Boolean): Pair<Int, String> {
    val goForward = isMale // 남 = 순행
    val diffHours = abs(Duration.between(birth, term).toHours().toDouble())
    val daeNum = (diffHours / 3).toInt()

    return Pair(daeNum, if (goForward) "순행" else "역행")
}

// -------------------------------
// 6) 메인 계산 함수
// -------------------------------
data class SajuResult(
    val ganji: Map<String, String>,
    val sibsung: Map<String, String>,
    val branchSibsung: Map<String, String>,
    val twelve: Map<String, String>,
    val relations: List<Relation>,
    val daewoon: Map<String, Any>
)

class SajuEngine {

    fun calculate(
        yearStem: String, yearBranch: String,
        monthStem: String, monthBranch: String,
        dayStem: String, dayBranch: String,
        hourStem: String, hourBranch: String,
        gender: String,
        birth: String,
        solarTermName: String,
        solarTermDate: String
    ): SajuResult {

        val ganji = mapOf(
            "year" to "$yearStem$yearBranch",
            "month" to "$monthStem$monthBranch",
            "day" to "$dayStem$dayBranch",
            "hour" to "$hourStem$hourBranch"
        )

        // 십신
        val sibsung = mapOf(
            "year" to getTenGod(dayStem, yearStem),
            "month" to getTenGod(dayStem, monthStem),
            "day" to "일간(나)",
            "hour" to getTenGod(dayStem, hourStem)
        )

        // 지지 십신
        val branchSibsung = mapOf(
            "year" to getTenGod(dayStem, branchToStem(yearBranch)),
            "month" to getTenGod(dayStem, branchToStem(monthBranch)),
            "day" to getTenGod(dayStem, branchToStem(dayBranch)),
            "hour" to getTenGod(dayStem, branchToStem(hourBranch))
        )

        // 십이운성
        val tw = mapOf(
            "year" to getTwelve(dayStem, yearBranch),
            "month" to getTwelve(dayStem, monthBranch),
            "day" to getTwelve(dayStem, dayBranch),
            "hour" to getTwelve(dayStem, hourBranch)
        )

        // 관계
        val relations = REL_LIST.filter {
            it.from == yearBranch || it.to == yearBranch ||
                    it.from == monthBranch || it.to == monthBranch ||
                    it.from == dayBranch || it.to == dayBranch ||
                    it.from == hourBranch || it.to == hourBranch
        }

        // 대운
        val birthDt = LocalDateTime.parse(birth)
        val termDt = LocalDateTime.parse(solarTermDate)

        val (daeAge, dir) = calcDaeun(birthDt, termDt, gender == "M")

        val daewoon = mapOf(
            "startAge" to daeAge,
            "direction" to dir
        )

        return SajuResult(
            ganji = ganji,
            sibsung = sibsung,
            branchSibsung = branchSibsung,
            twelve = tw,
            relations = relations,
            daewoon = daewoon
        )
    }
}

// -------------------------------
// 7) 지지를 천간으로 환산(간단 매핑)
// -------------------------------
private fun branchToStem(branch: String): String {
    val map = mapOf(
        "자" to "임","축" to "기","인" to "갑","묘" to "을","진" to "무",
        "사" to "병","오" to "정","미" to "기","신" to "경","유" to "신",
        "술" to "무","해" to "임"
    )
    return map[branch]!!
}
