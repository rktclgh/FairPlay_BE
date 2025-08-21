package com.fairing.fairplay.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 API
                        .allowedOrigins(
                                "http://localhost:3000", // 프론트 개발 주소
                                "http://localhost:5173", // Vite(React)
                                "https://fair-play.ink" // 배포용 도메인
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true) // 인증(쿠키/헤더) 허용
                        .maxAge(3600);
            }
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                // React Router fallback
                registry.addViewController("/{spring:[a-zA-Z0-9-_]+}")
                    .setViewName("forward:/index.html");

                // /eventDetail/1/extra 같은 다중 경로
                registry.addViewController("/{spring:[a-zA-Z0-9-_]+}/**")
                    .setViewName("forward:/index.html");
            }
        };
    }

}
