package com.saju.manse_api;

import com.saju.manse_api.service.SajuResult;
import com.saju.manse_api.service.SajuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManseController {

    @GetMapping("/ping")
    public String ping() { return "pong"; }

    @GetMapping("/saju")
    public SajuResult getSaju(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day,
            @RequestParam int hour,
            @RequestParam(defaultValue = "0") int min,
            @RequestParam(defaultValue = "false") boolean isLunar,
            @RequestParam(defaultValue = "false") boolean leap,
            @RequestParam(defaultValue = "true") boolean isMale,
            @RequestParam(defaultValue = "30") int pivotMin
    ) {
        return SajuService.INSTANCE.getSaju(year, month, day, hour, min, isLunar, leap, isMale, pivotMin);
    }
}