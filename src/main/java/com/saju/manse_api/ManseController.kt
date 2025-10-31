package com.saju.manse_api

import com.saju.manse_api.service.SajuResult
import com.saju.manse_api.service.SajuService
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["https://easysajusaju-dev.github.io"]) // GitHub Pages 허용
@RestController
class ManseController {

@GetMapping("/ping")
fun ping() = "pong"

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
}
