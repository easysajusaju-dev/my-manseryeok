package com.saju.manse_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class ManseApiApplication {
@PostConstruct
public void init() {
TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul")); // 한국 표준시
}
public static void main(String[] args) {
SpringApplication.run(ManseApiApplication.class, args);
}
}
