package com.fairing.fairplay.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] STATIC_ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:5173",
        "https://fair-play.ink",
        "https://fairplay.rktclgh.site",
        "https://service.iamport.kr",
        "https://pg.uplus.co.kr",
        "https://mobile.uplus.co.kr",
        "https://m.uplus.co.kr",
        "https://payment.uplus.co.kr",
        "https://webapp.uplus.co.kr",
        "https://pg.lguplus.co.kr",
        "https://mobile-pay.uplus.co.kr"
    };

    private final String frontendBaseUrl;
    private final String appBaseUrl;
    private final String extraAllowedOrigins;

    public WebConfig(
        @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
        @Value("${app.base-url:https://fair-play.ink}") String appBaseUrl,
        @Value("${app.cors.allowed-origins:}") String extraAllowedOrigins
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
        this.appBaseUrl = appBaseUrl;
        this.extraAllowedOrigins = extraAllowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API
            .allowedOrigins(allowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true) // 인증(쿠키/헤더) 허용
            .maxAge(3600);
    }

    String[] allowedOrigins() {
        return Stream.of(
                Arrays.stream(STATIC_ALLOWED_ORIGINS),
                Stream.of(frontendBaseUrl, appBaseUrl),
                Arrays.stream(extraAllowedOrigins.split(","))
            )
            .flatMap(origin -> origin)
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .distinct()
            .toArray(String[]::new);
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
