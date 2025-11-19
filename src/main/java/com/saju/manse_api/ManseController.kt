package com.saju.manse_api

import com.saju.manse_api.service.SajuResult
import com.saju.manse_api.service.SajuService
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
class ManseController {

    @GetMapping("/ping")
    fun ping() = "pong"

    // ========================================
    // 기본 사주 API
    // ========================================
    @GetMapping("/saju")
    fun saju(
        @RequestParam year:Int,
        @RequestParam month:Int,
        @RequestParam day:Int,
        @RequestParam hour:Int,
        @RequestParam(name="min", defaultValue="0") min:Int,
        @RequestParam(name="isLunar", defaultValue="false") isLunar:Boolean,
        @RequestParam(name="leap", defaultValue="false") leap:Boolean,
        @RequestParam(name="isMale", defaultValue="true") isMale:Boolean,
        @RequestParam(name="pivotMin", defaultValue="30") pivotMin:Int,
        @RequestParam(name="tzAdjust", defaultValue="-30") tzAdjust:Int,
        @RequestParam(name="seasonAdjust", defaultValue="0") seasonAdjust:Int
    ): SajuResult {

        return SajuService.getSaju(
            year, month, day, hour, min,
            isLunar, leap, isMale,
            pivotMin = pivotMin,
            tzAdjust = tzAdjust,
            seasonAdjust = seasonAdjust
        )
    }

    // ========================================
    // 앱 호환 모드
    // ========================================
    @GetMapping("/saju/compat")
    fun sajuCompat(
        @RequestParam year:Int,
        @RequestParam month:Int,
        @RequestParam day:Int,
        @RequestParam hour:Int,
        @RequestParam(name="min", defaultValue="0") min:Int,
        @RequestParam(name="isLunar", defaultValue="false") isLunar:Boolean,
        @RequestParam(name="leap", defaultValue="false") leap:Boolean,
        @RequestParam(name="isMale", defaultValue="true") isMale:Boolean,
        @RequestParam(name="tzAdjust", defaultValue="-30") tzAdjust:Int,
        @RequestParam(name="seasonAdjust", defaultValue="0") seasonAdjust:Int
    ): SajuResult {

        return SajuService.getSajuCompat(
            year, month, day, hour, min,
            isLunar, leap, isMale,
            pivotMin = 0,
            tzAdjust = tzAdjust,
            seasonAdjust = seasonAdjust,
            daeRound = "floor"
        )
    }

    // ========================================
    // DEBUG API — 내부 계산 전체 출력
    // ========================================
    @GetMapping("/saju/debug")
    fun sajuDebug(
        @RequestParam year:Int,
        @RequestParam month:Int,
        @RequestParam day:Int,
        @RequestParam hour:Int,
        @RequestParam(name="min", defaultValue="0") min:Int,
        @RequestParam(name="isLunar", defaultValue="false") isLunar:Boolean,
        @RequestParam(name="leap", defaultValue="false") leap:Boolean,
        @RequestParam(name="isMale", defaultValue="true") isMale:Boolean,
        @RequestParam(name="pivotMin", defaultValue="30") pivotMin:Int,
        @RequestParam(name="tzAdjust", defaultValue="-30") tzAdjust:Int,
        @RequestParam(name="seasonAdjust", defaultValue="0") seasonAdjust:Int
    ): Map<String, Any?> {

        return SajuService.debugSaju(
            year, month, day, hour, min,
            isLunar, leap, isMale,
            pivotMin, tzAdjust, seasonAdjust
        )
    }
}
