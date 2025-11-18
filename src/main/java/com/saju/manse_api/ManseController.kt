package com.saju.manse_api

import com.saju.manse_api.service.SajuResult
import com.saju.manse_api.service.SajuService
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
class ManseController {

    @GetMapping("/ping")
    fun ping() = "pong"

    /**
     * 기본 사주 API
     * 프론트에서 pivotMin / tzAdjust / seasonAdjust 등을 모두 조절해서 보내기에
     * 그대로 반영해서 계산하도록 수정함.
     */
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

        // ⭐ 추가된 옵션들 — 프론트에서 직접 제어 가능하도록
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

    /**
     * 앱 호환 모드 (M 앱과 완전히 동일한 계산 규칙)
     * — 정시 0분
     * — 동경시 -30분
     * — 절기 보정 없음
     * — 대운 floor
     *
     * 프론트에서 tzAdjust 를 선택할 수 있도록 옵션 추가
     */
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

        // ⭐ compat도 tzAdjust 받도록 수정 (기본값: 앱과 동일하게 -30)
        @RequestParam(name="tzAdjust", defaultValue="-30") tzAdjust:Int,

        // seasonAdjust도 필요하면 받을 수 있게
        @RequestParam(name="seasonAdjust", defaultValue="0") seasonAdjust:Int
    ): SajuResult {

        return SajuService.getSajuCompat(
            year, month, day, hour, min,
            isLunar, leap, isMale,

            pivotMin = 0,          // 고정 — 정시(0분)
            tzAdjust = tzAdjust,    // ⭐ 프론트·앱 동경시 설정 지원
            seasonAdjust = seasonAdjust,
            daeRound = "floor"      // 대운 내려서 계산
        )
    }
}
