package com.saju.manse_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct; // ← 여기만 jakarta로 교체
import java.util.TimeZone;

@SpringBootApplication
public class ManseApiApplication {
@PostConstruct
public void init() {
TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
}
public static void main(String[] args) {
SpringApplication.run(ManseApiApplication.class, args);
}
}
