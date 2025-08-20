package com.fairing.fairplay;

import com.fairing.fairplay.core.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling//오래된 이메일 삭제 스케쥴러 실행
@EnableJpaAuditing    // 생성/수정 시각 자동 반영
public class FairplayApplication {

	@PostConstruct
	public void init() {
		// JVM 기본 시간대를 Asia/Seoul로 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		EnvLoader.loadEnv();
		SpringApplication.run(FairplayApplication.class, args);
	}

}
