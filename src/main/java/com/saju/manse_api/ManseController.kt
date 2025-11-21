package com.saju.manse_api

import com.saju.engine.SajuFullEngine
import com.saju.manse_api.service.SajuService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/saju")
class ManseController {

    // ------------------------------------------------------
    // 1) 기존 디버그용 API (프론트에서 이미 사용 중)
    //    예: /saju/debug?year=1978&month=2&...
    // ------------------------------------------------------
    @GetMapping("/debug")
    fun debugSaju(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
        @RequestParam hour: Int,
        @RequestParam min: Int,
        @RequestParam isLunar: Boolean,
        @RequestParam leap: Boolean,
        @RequestParam isMale: Boolean,
        @RequestParam(defaultValue = "30") pivotMin: Int,
        @RequestParam(defaultValue = "-30") tzAdjust: Int,
        @RequestParam(defaultValue = "0") seasonAdjust: Int
    ): Any {
        return SajuService.debugSaju(
            year = year,
            month = month,
            day = day,
            hour = hour,
            minute = min,
            isLunar = isLunar,
            leap = leap,
            isMale = isMale,
            pivotMin = pivotMin,
            tzAdjust = tzAdjust,
            seasonAdjust = seasonAdjust
        )
    }

    // ------------------------------------------------------
    // 2) 기본 사주 API (필요하면 사용)  /saju
    // ------------------------------------------------------
    @GetMapping
    fun basicSaju(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
        @RequestParam hour: Int,
        @RequestParam min: Int,
        @RequestParam isLunar: Boolean,
        @RequestParam leap: Boolean,
        @RequestParam isMale: Boolean
    ): Any {
        return SajuService.getSaju(
            year = year,
            month = month,
            day = day,
            hour = hour,
            minute = min,
            isLunar = isLunar,
            leap = leap,
            isMale = isMale
        )
    }

    // ------------------------------------------------------
    // 3) 앱 호환 모드 (정시/동경시-30/절기0)  /saju/compat
    // ------------------------------------------------------
    @GetMapping("/compat")
    fun compatSaju(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
        @RequestParam hour: Int,
        @RequestParam min: Int,
        @RequestParam isLunar: Boolean,
        @RequestParam leap: Boolean,
        @RequestParam isMale: Boolean
    ): Any {
        return SajuService.getSajuCompat(
            year = year,
            month = month,
            day = day,
            hour = hour,
            minute = min,
            isLunar = isLunar,
            leap = leap,
            isMale = isMale
        )
    }

    // ------------------------------------------------------
    // 4) 🔥 통합 엔진 API — 프론트에서 최종적으로 이것만 쓰게 만들 예정
    //    /saju/full
    // ------------------------------------------------------
    @GetMapping("/full")
    fun fullSaju(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam day: Int,
        @RequestParam hour: Int,
        @RequestParam min: Int,
        @RequestParam isLunar: Boolean,
        @RequestParam leap: Boolean,
        @RequestParam isMale: Boolean
    ): Any {
        return try {
            val res = SajuFullEngine.run(
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = min,
                isLunar = isLunar,
                leap = leap,
                isMale = isMale
            )
            mapOf("ok" to true, "result" to res)
        } catch (e: Exception) {
            mapOf("ok" to false, "error" to (e.message ?: "unknown error"))
        }
    }

    // ------------------------------------------------------
    // 5) Ping — 서버 깨우기용  /saju/ping
    // ------------------------------------------------------
    @GetMapping("/ping")
    fun ping() = "pong"
}
