package com.fairing.fairplay.core.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.lang.reflect.Field;
import java.util.Map;

public class EnvLoader {
    public static void loadEnv() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")   // .env 파일이 루트에 있을 때
                .ignoreIfMissing()
                .load();

        // .env의 모든 key를 System 환경변수로 set (반영)
        dotenv.entries().forEach(entry -> setEnv(entry.getKey(), entry.getValue()));
    }

    // OS 환경변수에도 주입 (주의: 일부 OS/JVM에서는 제한될 수 있음)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            // ignore
        }
        // 추가: System.setProperty도 같이 등록
        System.setProperty(key, value);
    }
}
