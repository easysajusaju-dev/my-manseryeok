package com.saju.manse_api.controller

import com.saju.engine.SajuEngine
import org.springframework.web.bind.annotation.*

data class SajuRequest(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val isLunar: Boolean,
    val leap: Boolean,
    val isMale: Boolean
)

@RestController
@RequestMapping("/saju")
class ManseController {

    @PostMapping("/engine")
    fun engine(@RequestBody req: SajuRequest) =
        SajuEngine.calculate(
            year = req.year,
            month = req.month,
            day = req.day,
            hour = req.hour,
            minute = req.minute,
            isLunar = req.isLunar,
            leap = req.leap,
            isMale = req.isMale
        )
}
