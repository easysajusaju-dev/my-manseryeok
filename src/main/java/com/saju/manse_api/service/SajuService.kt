package com.saju.manse_api.service

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

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

object SajuService {

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

    /** 비결앱과 100% 동일한 시지 계산 */
    private fun hourBranchIndex(hour: Int, minute: Int, pivot: Int): Int {
        val total = hour * 60 + minute
        val start = 23 * 60 - pivot   // 23:00 - pivot
        val cycle = 12 * 120
        var t = (total - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }

    /**
     * ================================
     *   🚨 비결앱 기본 로직 그대로 적용
     * ================================
     */
    fun getSaju(
        year: Int, month: Int, day: Int,
        hour: Int, minute: Int,
        isLunar: Boolean, leap: Boolean,
        isMale: Boolean,
        pivotMin: Int = 30,       // 정시=0, 반시=30
        tzAdjust: Int = -30,      // 동경시 기본 -30분
        seasonAdjust: Int = 0,    // 기본 보정 없음 (앱도 기본 모드)
        daeRound: String = "floor"
    ): SajuResult {

        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)

        require(info != null)

        val yearGanji = info!!.hy
        val monthGanji = info.hm
        val dayGanji = info.hd

        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
            .plusMinutes(tzAdjust.toLong())

        // 시주 (지)
        val ds = dayGanji.substring(0, 1)
        val hIdx = hourBranchIndex(birth.hour, birth.minute, pivotMin)
        val hourJi = JI[hIdx]

        // 시주 (간)
        val dsIdx = GAN.indexOf(ds)
        val hourGan = GAN[(dsIdx % 5 * 2 + hIdx) % 10]
        val hourGanji = hourGan + hourJi

        // 방향 (정사/역사)
        val ys = yearGanji.substring(0, 1)
        val forward = (isMale && isYang(ys)) || (!isMale && !isYang(ys))
        val dirLabel = if (forward) "정사" else "역사"

        // 절기 기준 찾기
        val rawTerm =
            if (forward)
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

        // 대운 리스트
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

        // 연운
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
            yearGod = tenGod(ds, yearGanji.substring(0, 1)),
            monthGod = tenGod(ds, mStem),
            dayGod = "일간",
            hourGod = tenGod(ds, hourGan),
            daeNum = daeNum,
            daeDir = dirLabel,
            daeWoon = daeWoon,
            daeWoonGanji = daeWoonGanji,
            daeWoonYear = daeWoonYear,
            seunYear = seunYear,
            seunGanji = seunGanji,
            solarText = "${info.sy}년 %02d월 %02d일 %02d:%02d".format(info.sm, info.sd, birth.hour, birth.minute),
            lunarText = "음력 ${info.lm}월 ${info.ld}일" + if (info.leap) " (윤달)" else "",
            termName = rawTerm.name,
            termDate = term.toString().replace('T',' ')
        )
    }
}
