package com.fairing.fairplay.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API
            .allowedOrigins(
                "http://localhost:3000", // 프론트 개발 주소
                "http://localhost:5173", // Vite(React)
                "https://fair-play.ink", // 배포용 도메인
                "https://service.iamport.kr", // 아임포트 서비스
                "https://pg.uplus.co.kr", // LG U+ PG
                "https://mobile.uplus.co.kr", // LG U+ 모바일
                "https://m.uplus.co.kr", // LG U+ 모바일 단축
                "https://payment.uplus.co.kr", // LG U+ 결제
                "https://webapp.uplus.co.kr", // LG U+ 웹앱
                "https://pg.lguplus.co.kr", // LG U+ PG 구버전
                "https://mobile-pay.uplus.co.kr" // LG U+ 모바일 결제
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true) // 인증(쿠키/헤더) 허용
            .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드 파일 정적 서빙 (S3 대체)
        String uploadPath = System.getProperty("app.upload.path", System.getProperty("user.home") + "/fairplay-uploads");
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadPath + "/")
            .setCachePeriod(3600) // 1시간 캐시
            .resourceChain(false); // 캐시 체인 비활성화로 실시간 파일 변경 반영
        
        // SPA를 위한 정적 리소스 핸들링
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource requestedResource = location.createRelative(resourcePath);

                    // API 요청과 업로드 파일 요청은 처리하지 않음
                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("uploads/")) {
                        return null;
                    }

                    // 실제 파일이 존재하면 반환 (JS, CSS, 이미지 등)
                    if (requestedResource.exists() && requestedResource.isReadable()) {
                        return requestedResource;
                    }

                    // SPA 라우팅: 존재하지 않는 경로는 index.html로 폴백
                    return new ClassPathResource("/static/index.html");
                }
            });
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 루트 경로를 index.html로 포워딩
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}