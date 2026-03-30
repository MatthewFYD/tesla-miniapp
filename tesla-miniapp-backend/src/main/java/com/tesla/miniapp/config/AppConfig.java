package com.tesla.miniapp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

/**
 * 应用配置
 */
@Configuration
public class AppConfig {
    
    /**
     * Caffeine缓存配置（内存缓存，无需Redis）
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 10分钟过期
                .maximumSize(1000)                        // 最多1000条缓存
                .recordStats());                          // 记录统计信息
        return cacheManager;
    }
    
    /**
     * WebClient配置
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("User-Agent", "Tesla-MiniApp-Backend/1.0.0")
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)); // 10MB
    }
}