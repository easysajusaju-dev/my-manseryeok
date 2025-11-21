package com.saju.engine

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.floor

// ---------------------------------------------------------------
// 🔥 1) 응답 구조
// ---------------------------------------------------------------
data class SajuFullResponse(
    val ok: Boolean = true,
    val saju: SajuPart,
    val fortune: FortunePart,
    val meta: MetaPart
)

data class SajuPart(
    val ganji: Map<String, String>,
    val sibsung: Map<String, String>,
    val branchSibsung: Map<String, String>,
    val twelve: Map<String, String>,
    val jijanggan: Map<String, List<String>>,
    val nabeum: Map<String, String>,
    val relations: Map<String, List<RelationItem>>
)

data class RelationItem(
    val kind: String,
    val from: String,
    val to: String
)

data class FortunePart(
    val daeun: DaeunPart,
    val seun: List<SeunPart>
)

data class DaeunPart(
    val direction: String,
    val startAge: Int,
    val list: List<DaeunItem>
)

data class DaeunItem(
    val age: Int,
    val ganji: String,
    val year: Int
)

data class SeunPart(
    val year: Int,
    val ganji: String
)

data class MetaPart(
    val solar: String,
    val lunar: String,
    val termName: String,
    val termDate: String
)


// ---------------------------------------------------------------
// 🔥 2) 엔진 본체
// ---------------------------------------------------------------
object SajuFullEngine {

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

    private fun isYang(g: String) = g in YANG
    private fun elem(s: String) = STEM_ELEM[s]!!
    private fun idx(e: String) = CYCLE.indexOf(e)

    // ---------------------------------------------------------------
    // 🔥 십신 공식 — 교정 버전
    // ---------------------------------------------------------------
    private fun tenGod(dayStem: String, target: String): String {
        val me = elem(dayStem)
        val tg = elem(target)
        val same = isYang(dayStem) == isYang(target)

        return when {
            tg == me -> if (same) "비견" else "겁재"
            tg == CYCLE[(idx(me) + 1) % 5] -> if (same) "식신" else "상관"
            tg == CONTROL[me] -> if (same) "편재" else "정재"
            CONTROL[tg] == me -> if (same) "편관" else "정관"
            tg == CYCLE[(idx(me) + 4) % 5] -> if (same) "편인" else "정인"
            else -> "-"
        }
    }

    // ---------------------------------------------------------------
    // 🔥 지장간
    // ---------------------------------------------------------------
    private val JIJANG = mapOf(
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

    // ---------------------------------------------------------------
    // 🔥 12운성
    // ---------------------------------------------------------------
    private val TWELVE = listOf("장생","목욕","관대","임관","제왕","쇠","병","사","묘","절","태","양")

    private fun twelve(dayStem: String, branch: String): String {
        val seq = mapOf(
            "甲" to 1, "乙" to 1,
            "丙" to 3, "丁" to 3,
            "戊" to 5,
            "己" to 7,
            "庚" to 9, "辛" to 9,
            "壬" to 11, "癸" to 11
        )
        val start = seq[dayStem]!!
        val idx = (start + JI.indexOf(branch)) % 12
        return TWELVE[idx]
    }

    // ---------------------------------------------------------------
    // 🔥 납음
    // ---------------------------------------------------------------
    private val NABEUM = mapOf(
        "甲子" to "해중금","乙丑" to "해중금",
        "丙寅" to "노중화","丁卯" to "노중화",
        "戊辰" to "대림목","己巳" to "대림목",
        "庚午" to "노방토","辛未" to "노방토",
        "壬申" to "검금","癸酉" to "검금",
        "甲戌" to "산두화","乙亥" to "산두화",
        "丙子" to "천하수","丁丑" to "천하수",
        "戊寅" to "성두화","己卯" to "성두화",
        "庚辰" to "백랍금","辛巳" to "백랍금",
        "壬午" to "양중수","癸未" to "양중수"
    )

    // ---------------------------------------------------------------
    // 🔥 메인 엔진
    // ---------------------------------------------------------------
    fun run(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        isLunar: Boolean,
        leap: Boolean,
        isMale: Boolean
    ): SajuFullResponse {

        // ---------------------------
        // 날짜 정보 DB에서 획득
        // ---------------------------
        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)
            ?: throw IllegalArgumentException("날짜 없음")

        val yearGanji = info.hy
        val monthGanji = info.hm
        val dayGanji = info.hd

        val originalBirth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val birth = originalBirth.minusMinutes(30) // 동경시 보정

        // ---------------------------
        // 시주
        // ---------------------------
        val ds = dayGanji[0].toString()
        val hIdx = hourToBranchIndex(birth.hour, birth.minute)
        val hourBranch = JI[hIdx]
        val hourStem = GAN[(GAN.indexOf(ds) % 5 * 2 + hIdx) % 10]
        val hourGanji = hourStem + hourBranch

        // ---------------------------
        // 십신
        // ---------------------------
        val sibsung = mapOf(
            "year" to tenGod(ds, yearGanji[0].toString()),
            "month" to tenGod(ds, monthGanji[0].toString()),
            "day" to "일간",
            "hour" to tenGod(ds, hourStem)
        )

        // ---------------------------
        // 지장간
        // ---------------------------
        val jg = mapOf(
            "year" to JIJANG[yearGanji[1].toString()]!!,
            "month" to JIJANG[monthGanji[1].toString()]!!,
            "day" to JIJANG[dayGanji[1].toString()]!!,
            "hour" to JIJANG[hourBranch]!!
        )

        // ---------------------------
        // 12운성
        // ---------------------------
        val tw = mapOf(
            "year" to twelve(ds, yearGanji[1].toString()),
            "month" to twelve(ds, monthGanji[1].toString()),
            "day" to twelve(ds, dayGanji[1].toString()),
            "hour" to twelve(ds, hourBranch)
        )

        // ---------------------------
        // 납음
        // ---------------------------
        val nb = mapOf(
            "year" to (NABEUM[yearGanji] ?: ""),
            "month" to (NABEUM[monthGanji] ?: ""),
            "day" to (NABEUM[dayGanji] ?: ""),
            "hour" to (NABEUM[hourGanji] ?: "")
        )

        // ---------------------------
        // 대운
        // ---------------------------
        val forward = (isMale && isYang(yearGanji[0].toString())) ||
                (!isMale && !isYang(yearGanji[0].toString()))
        val dirLabel = if (forward) "forward" else "reverse"

        val daeTerm = nextOrPrevTerm(info, birth.hour, forward)
        val diff = abs(Duration.between(
            birth.toLocalDate().atStartOfDay(),
            daeTerm.dt.toLocalDate().atStartOfDay()
        ).toDays())
        val daeNum = floor(diff / 3.0).toInt().coerceAtLeast(1)
        val startAge = daeNum

        val msIdx = GAN.indexOf(monthGanji[0].toString())
        val mbIdx = JI.indexOf(monthGanji[1].toString())

        val daeList = (1..10).map {
            val s = if (forward) (msIdx + it) % 10 else (msIdx - it + 100) % 10
            val b = if (forward) (mbIdx + it) % 12 else (mbIdx - it + 120) % 12
            DaeunItem(
                age = startAge + (it - 1) * 10,
                ganji = GAN[s] + JI[b],
                year = info.sy + startAge - 1 + (it - 1) * 10
            )
        }

        // ---------------------------
        // 세운
        // ---------------------------
        val seun = (0 until 10).map {
            val y = info.sy + startAge - 1 + it
            val gan = GAN[(y + 6) % 10] + JI[(y + 8) % 12]
            SeunPart(y, gan)
        }

        // ---------------------------
        // 최종 응답 생성
        // ---------------------------
        return SajuFullResponse(
            saju = SajuPart(
                ganji = mapOf(
                    "year" to yearGanji,
                    "month" to monthGanji,
                    "day" to dayGanji,
                    "hour" to hourGanji
                ),
                sibsung = sibsung,
                branchSibsung = mapOf(), // 필요시 확장
                twelve = tw,
                jijanggan = jg,
                nabeum = nb,
                relations = mapOf(
                    "hyung" to listOf(),
                    "chung" to listOf(),
                    "pa" to listOf(),
                    "hap" to listOf()
                )
            ),
            fortune = FortunePart(
                daeun = DaeunPart(
                    direction = dirLabel,
                    startAge = startAge,
                    list = daeList
                ),
                seun = seun
            ),
            meta = MetaPart(
                solar = "${info.sy}-${info.sm}-${info.sd} ${birth.hour}:${birth.minute}",
                lunar = "음력 ${info.lm}월 ${info.ld}일",
                termName = daeTerm.name,
                termDate = daeTerm.dt.toString()
            )
        )
    }

    private fun hourToBranchIndex(h: Int, m: Int): Int {
        val t = h * 60 + m
        val base = 23 * 60 - 30
        var d = (t - base) % (12 * 120)
        if (d < 0) d += 12 * 120
        return d / 120
    }

    private fun nextOrPrevTerm(
        info: com.saju.manse_api.repo.ManseryeokPoint,
        hour: Int,
        forward: Boolean
    ): com.saju.manse_api.repo.SeasonPoint {
        var t = if (forward)
            SeasonRepo.nextAfter(info.sy, info.sm, info.sd, hour)
        else
            SeasonRepo.prevBefore(info.sy, info.sm, info.sd, hour)

        val principal = setOf(
            "입춘","경칩","청명","입하","망종","소서",
            "입추","백로","한로","입동","대설","소한"
        )

        while (t.name !in principal) {
            val d = t.dt
            t = if (forward)
                SeasonRepo.nextAfter(d.year, d.monthValue, d.dayOfMonth, d.hour)
            else
                SeasonRepo.prevBefore(d.year, d.monthValue, d.dayOfMonth, d.hour)
        }
        return t
    }
}
