package com.saju.manse_api.service

import com.saju.manse_api.repo.ManseryeokRepo
import com.saju.manse_api.repo.SeasonRepo
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

data class SajuResult(
    val yearGanji: String,
    val monthGanji: String,
    val dayGanji: String,
    val hourGanji: String,
    val daeWoon: List<String> = emptyList(),
    val daeWoonGanji: List<String> = emptyList(),
    val daeWoonYear: List<Int> = emptyList(),
    val seunYear: List<Int> = emptyList(),
    val seunGanji: List<String> = emptyList()
)

object SajuService {
    private val GAN = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
    private val JI = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
    private val GAN_INDEX = GAN.withIndex().associate { it.value to it.index }
    private val JI_INDEX = JI.withIndex().associate { it.value to it.index }


    // 호환(분 없이 호출 시): minute=0, pivot=30, 양력/남자 기본
    fun getSaju(year: Int, month: Int, day: Int, hour: Int): SajuResult =
        getSaju(year, month, day, hour, 0, false, false, true, 30)

    // 시간지 인덱스 계산: 23시를 기준으로 2시간 단위. pivotMinute=30이면
// 경계가 23:30, 1:30, 3:30, ... 식으로 반시 기준이 됩니다.
    private fun hourBranchIndex(hour: Int, minute: Int, pivotMinute: Int): Int {
        val totalMin = hour * 60 + minute
        val start = 23 * 60 - pivotMinute           // pivot=0 => 23:00, pivot=30 => 22:30 기준 이동
        val cycle = 12 * 120                        // 12지 × 120분
        var t = (totalMin - start) % cycle
        if (t < 0) t += cycle
        return t / 120
    }

    // 본 함수: 분과 경계 정책까지 반영
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

        val info = if (isLunar)
            ManseryeokRepo.findByLunar(year, month, day, leap)
        else
            ManseryeokRepo.findBySolar(year, month, day)
        require(info != null) { "해당 날짜 데이터가 없습니다: year=$year month=$month day=$day isLunar=$isLunar leap=$leap" }

        val yearGanji  = info!!.hy
        val monthGanji = info.hm
        val dayGanji   = info.hd

        // 시주: 일간 + 분/경계정책 반영한 시간지
        val dayStem = dayGanji.substring(0, 1)
        val dayStemIdx = GAN_INDEX[dayStem] ?: error("알 수 없는 일간: $dayGanji")
        val hIdx = hourBranchIndex(hour, minute, pivotMinute)
        val hourGanIdx = (dayStemIdx % 5) * 2 + hIdx
        val hourGanji = GAN[hourGanIdx % 10] + JI[hIdx]

        // 대운 방향: (남자 ∧ 연간 양) 또는 (여자 ∧ 연간 음) ⇒ 순행
        val yearStem = yearGanji.substring(0, 1)
        val yearStemIdx = GAN_INDEX[yearStem] ?: 0
        val isYearYang = (yearStemIdx % 2 == 0)
        val forward = (isMale && isYearYang) || (!isMale && !isYearYang)

        // 절기까지의 차이로 시작나이(3일=1년, 올림). 분 반영
        val birth = LocalDateTime.of(info.sy, info.sm, info.sd, hour, minute)
        val term  = if (forward)
            SeasonRepo.nextAfter(info.sy, info.sm, info.sd, hour)
        else
            SeasonRepo.prevBefore(info.sy, info.sm, info.sd, hour)
        val hoursDiff = abs(Duration.between(birth, term.dt).toHours().toDouble())
        val startAge  = max(1, ceil((hoursDiff / 24.0) / 3.0).toInt())
        val startYear = info.sy + startAge - 1

        // 대운: 월주 기준 전진/후진
        val mStemIdx = GAN_INDEX[monthGanji.substring(0, 1)] ?: 0
        val mBrIdx   = JI_INDEX[monthGanji.substring(1, 2)] ?: 0

        val daeWoon = mutableListOf<String>()
        val daeWoonGanji = mutableListOf<String>()
        val daeWoonYear = mutableListOf<Int>()
        for (i in 1..10) {
            val gi = if (forward) (mStemIdx + i) % 10 else (mStemIdx - i + 100) % 10
            val bi = if (forward) (mBrIdx   + i) % 12 else (mBrIdx   - i + 120) % 12
            daeWoon.add("${i * 10}대운 (${if (forward) "순행" else "역행"})")
            daeWoonGanji.add(GAN[gi] + JI[bi])
            daeWoonYear.add(startYear + (i - 1) * 10)
        }

        // 세운(첫 10년)
        val seunYear = mutableListOf<Int>()
        val seunGanji = mutableListOf<String>()
        for (i in 0 until 10) {
            val y = startYear + i
            val yg = (y + 6) % 10
            val yb = (y + 8) % 12
            seunYear.add(y)
            seunGanji.add(GAN[yg] + JI[yb])
        }

        return SajuResult(
            yearGanji, monthGanji, dayGanji, hourGanji,
            daeWoon, daeWoonGanji, daeWoonYear, seunYear, seunGanji
        )
    }
}