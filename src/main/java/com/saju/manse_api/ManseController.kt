package com.saju.manse_api

import com.saju.manse_api.service.SajuResult
import com.saju.manse_api.service.SajuService
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"])
@RestController
class ManseController {

@GetMapping("/ping")
fun ping() = "pong"

// 기존 간단 결과(서버가 daeNum/daeDir도 내려줌)
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
    @RequestParam(name="pivotMin", defaultValue="30") pivotMin:Int
): SajuResult {
    return SajuService.getSaju(year, month, day, hour, min, isLunar, leap, isMale, pivotMin)
}

// 앱 호환 규칙(정시/동경시-30/절기+10/대운수 floor) 강제
@GetMapping("/saju/compat")
fun sajuCompat(
    @RequestParam year:Int,
    @RequestParam month:Int,
    @RequestParam day:Int,
    @RequestParam hour:Int,
    @RequestParam(name="min", defaultValue="0") min:Int,
    @RequestParam(name="isLunar", defaultValue="false") isLunar:Boolean,
    @RequestParam(name="leap", defaultValue="false") leap:Boolean,
    @RequestParam(name="isMale", defaultValue="true") isMale:Boolean
): SajuResult {
    return SajuService.getSajuCompat(
        year, month, day, hour, min,
        isLunar, leap, isMale,
        pivotMin = 0,      // 정시(0분)
        tzAdjust = -30,    // 동경시 보정(-30분)
        seasonAdjust = 10, // 절기 +10분 보정
        daeRound = "floor" // 대운수 내림
    )
}
}
