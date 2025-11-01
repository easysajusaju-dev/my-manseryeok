package com.saju.manse_api

import com.saju.manse_api.service.FullResult
import com.saju.manse_api.service.SajuResult
import com.saju.manse_api.service.SajuService
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
class ManseController {

    @GetMapping("/ping")
    fun ping() = "pong"

    // 기존 간단 결과
    @GetMapping("/saju")
    fun saju(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
        @RequestParam hour: Int,
        @RequestParam(name = "min", defaultValue = "0") min: Int,
        @RequestParam(name = "isLunar", defaultValue = "false") isLunar: Boolean,
        @RequestParam(name = "leap", defaultValue = "false") leap: Boolean,
        @RequestParam(name = "isMale", defaultValue = "true") isMale: Boolean,
        @RequestParam(name = "pivotMin", defaultValue = "30") pivotMin: Int
    ): SajuResult {
        return SajuService.getSaju(year, month, day, hour, min, isLunar, leap, isMale, pivotMin)
    }

    // 모든 것을 한 번에
    @GetMapping("/saju/full")
    fun sajuFull(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
        @RequestParam hour: Int,
        @RequestParam(name = "min", defaultValue = "0") min: Int,
        @RequestParam(name = "isLunar", defaultValue = "false") isLunar: Boolean,
        @RequestParam(name = "leap", defaultValue = "false") leap: Boolean,
        @RequestParam(name = "isMale", defaultValue = "true") isMale: Boolean,
        @RequestParam(name = "pivotMin", defaultValue = "30") pivotMin: Int,
        // 연운 범위(예: "1975-2095"), 없으면 출생~출생+120
        @RequestParam(name = "years", required = false) years: String?,
        // 월운 연도(없으면 현재년도)
        @RequestParam(name = "monthsYear", required = false) monthsYear: Int?
    ): FullResult {
        val (ys, ye) = parseYears(years, year)
        val my = monthsYear ?: java.time.LocalDate.now().year
        return SajuService.getSajuFull(year, month, day, hour, min, isLunar, leap, isMale, pivotMin, ys, ye, my)
    }

    private fun parseYears(text: String?, birthYear: Int): Pair<Int, Int> {
        if (text.isNullOrBlank()) return birthYear to birthYear + 120
        val parts = text.split("-").mapNotNull { it.trim().toIntOrNull() }
        return if (parts.size == 2 && parts[0] <= parts[1]) parts[0] to parts[1] else birthYear to birthYear + 120
    }
}
