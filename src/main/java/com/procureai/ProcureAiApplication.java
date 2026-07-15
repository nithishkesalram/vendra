package com.procureai;

import com.procureai.auth.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ProcureAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcureAiApplication.class, args);
    }
}
