# 1. Java 21 기반 경량 이미지를 사용
FROM openjdk:21-jdk-slim

# 2. 빌드 결과 JAR 파일을 복사
# (빌드 시 build/libs/ 폴더에 jar가 생성됨. 이름이 바뀌어도 *로 복사)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 3. 환경변수/프로파일 추가 (필요 시)
# ENV SPRING_PROFILES_ACTIVE=prod

# 4. JAR 파일 실행 (컨테이너 스타트 시)
ENTRYPOINT ["java", "-jar", "app.jar"]
