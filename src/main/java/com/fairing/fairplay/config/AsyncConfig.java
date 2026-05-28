package com.fairing.fairplay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리를 위한 ThreadPool 설정
 * RAG 임베딩 병렬 처리에 사용
 */
@Configuration
public class AsyncConfig {

    /**
     * RAG 임베딩 처리용 ThreadPoolTaskExecutor
     * - Core Pool Size: 4 (기본 스레드 수)
     * - Max Pool Size: 10 (최대 스레드 수)
     * - Queue Capacity: 50 (대기열 크기)
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수 (항상 유지)
        executor.setCorePoolSize(4);
        
        // 최대 스레드 수 (부하 시 확장)
        executor.setMaxPoolSize(10);
        
        // 대기열 크기
        executor.setQueueCapacity(50);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("rag-async-");
        
        // 스레드 풀 이름
        executor.setBeanName("ragTaskExecutor");
        
        // 애플리케이션 종료 시 스레드 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        // 초기화
        executor.initialize();
        
        return executor;
    }

    @Bean(name = "aiChatTaskExecutor")
    public ThreadPoolTaskExecutor aiChatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-chat-response-");
        executor.setBeanName("aiChatTaskExecutor");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    @Bean(name = "chatCacheTaskExecutor")
    public ThreadPoolTaskExecutor chatCacheTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("chat-cache-");
        executor.setBeanName("chatCacheTaskExecutor");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    @Bean(name = "ragAdminTaskExecutor")
    public ThreadPoolTaskExecutor ragAdminTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("rag-admin-");
        executor.setBeanName("ragAdminTaskExecutor");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
