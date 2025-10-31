package com.saju.manse_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.saju"})  // 스캔 범위 확장!
public class ManseApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManseApiApplication.class, args);
    }
}
