# Java 21 경량 런타임 이미지 사용 (성능+보안 최적)
FROM eclipse-temurin:21-jre

# 빌드된 JAR 복사 (build/libs/*.jar → app.jar)
COPY build/libs/*.jar app.jar

# (필요시) 환경변수 기본값 설정 예시 (주석 해제해서 사용)
# ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
