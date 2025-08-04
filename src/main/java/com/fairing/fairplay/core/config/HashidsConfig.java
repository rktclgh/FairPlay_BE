package com.fairing.fairplay.core.config;

import lombok.Setter;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hashids")
@Setter
public class HashidsConfig {

//    @Value("${hashids.salt}")
    /*
    Bean 생성 타이밍 맞게
    Spring Environment 준비 후에 주입하도록 변경했어요! 기능고장나면 수정해주세요!
    제꺼에서는 됐다 안됐다 해서 아예 순서를 조정했습니다!
    */
    private String salt;

    @Bean
    public Hashids hashids() {
        return new Hashids(salt, 8); // 최소 길이 8
    }
    
}
