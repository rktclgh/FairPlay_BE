package com.fairing.fairplay.core.config;

import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HashidsConfig {

    @Value("${hashids.salt}")
    private String salt;

    @Bean
    public Hashids hashids() {
        return new Hashids(salt, 8); // 최소 길이 8
    }
    
}
