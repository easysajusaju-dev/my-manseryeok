package com.saju.engine

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/saju")
class SajuController(
    private val service: SajuService
) {

    @PostMapping("/full")
    fun calculateFull(@RequestBody req: SajuRequest): SajuResponse {
        return service.calculate(req)
    }
}
