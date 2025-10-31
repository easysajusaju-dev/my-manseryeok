package com.saju.manse_api

import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.time.temporal.ChronoUnit
// 데이터 그릇 (결과를 담는 역할)
data class SajuData(
    val yearGanji: String,
    val monthGanji: String,
    val dayGanji: String,
    val hourGanji: String,
    val daeWoon: List<String>,
    val daeWoonGanji: List<String>,
    val daeWoonYear: List<Int>
)

// 만세력 계산기 (핵심 로직)
object Manse {
    private val GAN = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val JI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val DAEWOON_DIRECTION = listOf("순행", "역행")

    // 24절기 계산 함수 (천문 계산식 사용)
    private fun getSolarterm(year: Int, month: Int): Int {
        val term = arrayOf(
            doubleArrayOf(20.32, 4.47, 5.22, 20.94, 6.38, 21.42, 6.95, 22.83, 8.23, 23.55, 8.35, 23.65),
            doubleArrayOf(20.42, 4.62, 5.35, 21.04, 6.50, 21.51, 7.07, 22.95, 8.35, 23.65, 8.42, 23.73),
            doubleArrayOf(20.51, 4.76, 5.48, 21.13, 6.62, 21.60, 7.20, 23.08, 8.48, 23.75, 8.51, 23.82),
            doubleArrayOf(20.59, 4.89, 5.60, 21.22, 6.74, 21.68, 7.32, 23.20, 8.60, 23.84, 8.61, 23.90),
            doubleArrayOf(20.68, 5.01, 5.71, 21.31, 6.85, 21.76, 7.43, 23.32, 8.72, 23.93, 8.70, 23.98)
        )
        val n = if (year < 2000) 0 else if (year < 2100) 1 else if (year < 2200) 2 else if (year < 2300) 3 else 4
        var day = 0.0
        when (month) {
            1 -> day = term[n][0] // 소한
            2 -> day = term[n][1] // 입춘
            3 -> day = term[n][2] // 경칩
            4 -> day = term[n][3] // 청명
            5 -> day = term[n][4] // 입하
            6 -> day = term[n][5] // 망종
            7 -> day = term[n][6] // 소서
            8 -> day = term[n][7] // 입추
            9 -> day = term[n][8] // 백로
            10 -> day = term[n][9] // 한로
            11 -> day = term[n][10] // 입동
            12 -> day = term[n][11] // 대설
        }
        if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0) { // 윤년 보정
            if (month == 3 || month == 4) day -= 1.0
        }
        return day.toInt()
    }

    fun getSaju(year: Int, month: Int, day: Int, hour: Int, isMale: Boolean, isLunar: Boolean): SajuData {
        var solYear = year
        var solMonth = month
        var solDay = day
        if (isLunar) {
            // 음력->양력 변환은 생략(추후 필요 시 별도 라이브러리 필요)
        }

        // 년주
        val termDayYear = getSolarterm(solYear, 2)
        val baseYear = if (solMonth == 1 || (solMonth == 2 && solDay < termDayYear)) solYear - 1 else solYear
        val yearGan = (baseYear + 6) % 10
        val yearJi = (baseYear + 8) % 12
        val yearGanji = GAN[yearGan] + JI[yearJi]

        // 월주
        val termDayMonth = getSolarterm(solYear, solMonth)
        val baseMonth = if (solDay < termDayMonth) solMonth - 1 else solMonth
        val monthGan = ((baseYear - 1) * 2 + (baseMonth + 1)) % 10
        val monthJi = (baseMonth + 3) % 12
        val monthGanji = GAN[monthGan] + JI[monthJi]

        // 일주
        val m = if (solMonth <= 2) solMonth + 12 else solMonth
        val yAdj = if (solMonth <= 2) solYear - 1 else solYear
        val totalDays = (yAdj + yAdj / 4 - yAdj / 100 + yAdj / 400 + (13 * m + 8) / 5 + solDay) % 60
        val dayGan = totalDays % 10
        val dayJi = totalDays % 12
        val dayGanji = GAN[dayGan] + JI[dayJi]

        // 시주
        val hourJi = ((hour + 1) / 2) % 12
        val hourGan = (dayGan % 5 * 2 + hourJi) % 10
        val hourGanji = GAN[hourGan] + JI[hourJi]

        // 대운
        val direction = if ((yearGan % 2 == 0 && isMale) || (yearGan % 2 != 0 && !isMale)) 0 else 1 // 0: 순행, 1: 역행
        val startNum = getDaewoonNum(solYear, solMonth, solDay, direction)

        val daeWoon = mutableListOf<String>()
        val daeWoonGanji = mutableListOf<String>()
        val daeWoonYear = mutableListOf<Int>()

        for (i in 0 until 10) {
            val num = startNum + (i * 10)
            daeWoon.add("${num}대운 (${if (direction == 0) "순행" else "역행"})")
            daeWoonYear.add(solYear + num - 1)

            val ganIndex = if (direction == 0) (monthGan + i + 1) % 10 else (monthGan - (i + 1) + 100) % 10
            val jiIndex = if (direction == 0) (monthJi + i + 1) % 12 else (monthJi - (i + 1) + 120) % 12
            daeWoonGanji.add(GAN[ganIndex] + JI[jiIndex])
        }

        return SajuData(yearGanji, monthGanji, dayGanji, hourGanji, daeWoon, daeWoonGanji, daeWoonYear)
    }

    private fun getDaewoonNum(year: Int, month: Int, day: Int, direction: Int): Int {
        val cal = LocalDate.of(year, month, day)
        var termCal: LocalDate
        var diff = 0

        if (direction == 0) { // 순행
            var termMonth = month
            var termDay = getSolarterm(year, month)
            if (day > termDay) {
                termMonth = if (month == 12) 1 else month + 1
                val termYear = if (month == 12) year + 1 else year
                termDay = getSolarterm(termYear, termMonth)
                termCal = LocalDate.of(termYear, termMonth, termDay)
            } else {
                termCal = LocalDate.of(year, termMonth, termDay)
            }
            diff = (termCal.toEpochDay() - cal.toEpochDay()).toInt()
        } else { // 역행
            var termMonth = month
            var termDay = getSolarterm(year, month)
            if (day < termDay) {
                termMonth = if (month == 1) 12 else month - 1
                val termYear = if (month == 1) year - 1 else year
                termDay = getSolarterm(termYear, termMonth)
                termCal = LocalDate.of(termYear, termMonth, termDay)
            } else {
                termCal = LocalDate.of(year, termMonth, termDay)
            }
            diff = (cal.toEpochDay() - termCal.toEpochDay()).toInt()
        }

        val result = (diff.toDouble() / 3.0).let { if (it < 1.0) 1.0 else it }.toInt()
        return if (result > 10) 10 else result
    }
}