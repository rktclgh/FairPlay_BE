package com.fairing.fairplay.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 기본 시간대를 Asia/Seoul로 설정
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return mapper;
    }

    @Bean
    public ZoneId defaultZoneId() {
        return ZoneId.of("Asia/Seoul");
    }
}