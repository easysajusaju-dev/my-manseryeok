package com.saju.manse_api;

import com.saju.manse_api.service.SajuResult;
import com.saju.manse_api.service.SajuService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"https://easysajusaju-dev.github.io"}) // GitHub Pages에서 호출 허용
public class ManseController {

text

@GetMapping("/ping")
public String ping() { return "pong"; }

@GetMapping("/saju")
public SajuResult saju(
        @RequestParam int year,
        @RequestParam int month,
        @RequestParam int day,
        @RequestParam int hour,
        @RequestParam(name = "min", defaultValue = "0") int min,
        @RequestParam(name = "isLunar", defaultValue = "false") boolean isLunar,
        @RequestParam(name = "leap", defaultValue = "false") boolean leap,
        @RequestParam(name = "isMale", defaultValue = "true") boolean isMale,
        @RequestParam(name = "pivotMin", defaultValue = "30") int pivotMin
) {
    return SajuService.INSTANCE.getSaju(year, month, day, hour, min, isLunar, leap, isMale, pivotMin);
}
}
