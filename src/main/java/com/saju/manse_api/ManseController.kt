package com.saju.manse_api

import com.saju.engine.SajuFullEngine
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/saju")
class ManseController {

    // ------------------------------------------------------
    // 🔥 통합 사주 API — 프론트는 이것만 호출하면 됨
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
            mapOf("ok" to false, "error" to e.message)
        }
    }


    // ------------------------------------------------------
    // 🔥 Ping — 서버 깨우기용
    // ------------------------------------------------------
    @GetMapping("/ping")
    fun ping() = "pong"
}
