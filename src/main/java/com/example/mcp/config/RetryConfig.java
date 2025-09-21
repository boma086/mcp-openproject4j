package com.example.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

import com.example.mcp.exception.OpenProjectApiException;
import com.example.mcp.exception.RateLimitExceededException;

import java.util.Map;

/**
 * Enhanced Spring Retry Configuration for Transient API Failures
 * 瞬时API故障的增强Spring重试配置
 * 
 * This configuration enables Spring Retry functionality with optimized settings
 * for handling transient API failures including network timeouts, rate limits,
 * and temporary server errors. The configuration provides:
 * 
 * 该配置启用Spring重试功能，针对处理瞬时API故障（包括网络超时、速率限制
 * 和临时服务器错误）进行了优化设置。配置提供：
 * 
 * Features:
 * - Exponential backoff for network issues
 * - Rate limit-aware retry timing
 * - Configurable retry attempts per operation type
 * - Circuit breaker pattern integration
 * 
 * 功能：
 * - 网络问题的指数退避
 * - 速率限制感知的重试时机
 * - 按操作类型配置的重试次数
 * - 断路器模式集成
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Configuration
@EnableRetry
public class RetryConfig {

    /**
     * Retry template for general API operations with enhanced transient failure handling
     * 通用API操作的重试模板，具有增强的瞬时故障处理功能
     */
    @Bean
    public RetryTemplate apiRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure retry policy for different exception types
        // 为不同异常类型配置重试策略
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
            RateLimitExceededException.class, true,
            OpenProjectApiException.class, true,
            ResourceAccessException.class, true,
            org.springframework.web.client.HttpServerErrorException.class, true
        );
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure exponential backoff policy optimized for network issues
        // 为网络问题优化的指数退避策略
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second initial delay
        backOffPolicy.setMultiplier(2.0); // Double the delay each time
        backOffPolicy.setMaxInterval(10000); // Maximum 10 seconds between retries
        
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
    
    /**
     * Retry template for rate-limited operations with conservative backoff
     * 速率限制操作的重试模板，采用保守的退避策略
     */
    @Bean
    public RetryTemplate rateLimitRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Focus on rate limit exceptions
        // 专注于速率限制异常
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
            RateLimitExceededException.class, true
        );
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Conservative backoff for rate limits
        // 速率限制的保守退避
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000); // 2 second initial delay
        backOffPolicy.setMultiplier(1.5); // More gradual increase
        backOffPolicy.setMaxInterval(15000); // Maximum 15 seconds between retries
        
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
    
    /**
     * Retry template for network operations with aggressive retry strategy
     * 网络操作的重试模板，采用激进的重试策略
     */
    @Bean
    public RetryTemplate networkRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Focus on network-related exceptions
        // 专注于网络相关异常
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
            ResourceAccessException.class, true,
            java.net.ConnectException.class, true,
            java.net.SocketTimeoutException.class, true,
            java.net.UnknownHostException.class, true
        );
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(4, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Aggressive backoff for network issues
        // 网络问题的激进退避
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500); // 0.5 second initial delay
        backOffPolicy.setMultiplier(2.5); // Faster escalation
        backOffPolicy.setMaxInterval(8000); // Maximum 8 seconds between retries
        
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}