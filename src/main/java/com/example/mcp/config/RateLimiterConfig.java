package com.example.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Configuration class for Rate Limiter settings.
 * 速率限制器设置的配置类。
 * 
 * <p>This class provides configuration properties for the rate limiter,
 * allowing customization of rate limiting behavior through application properties.</p>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Configuration
@Validated
public class RateLimiterConfig {

    /**
     * Configuration properties for rate limiting.
     */
    @Bean
    @ConfigurationProperties(prefix = "openproject.rate-limit")
    public RateLimitProperties rateLimitProperties() {
        return new RateLimitProperties();
    }

    /**
     * Rate limiting configuration properties.
     * 速率限制配置属性。
     */
    public static class RateLimitProperties {
        
        /**
         * Maximum number of requests allowed per minute.
         * 每分钟允许的最大请求数。
         */
        @Min(1)
        @Max(10000)
        private int requestsPerMinute = 100;
        
        /**
         * Burst capacity for handling sudden spikes in requests.
         * 处理请求突增的突发容量。
         */
        @Min(1)
        @Max(1000)
        private int burstCapacity = 10;
        
        /**
         * Maximum wait time in seconds for acquiring permits.
         * 获取许可的最大等待时间（秒）。
         */
        @Min(1)
        @Max(300)
        private int waitTimeoutSeconds = 30;
        
        /**
         * Whether to enable adaptive rate limiting based on server responses.
         * 是否基于服务器响应启用自适应速率限制。
         */
        private boolean adaptiveRateLimiting = true;
        
        /**
         * Factor for reducing rate limits when server indicates throttling.
         * 服务器指示限制时减少速率限制的系数。
         */
        private double throttlingFactor = 0.8;
        
        /**
         * Window size in seconds for calculating request rates.
         * 计算请求速率的窗口大小（秒）。
         */
        @Min(1)
        @Max(300)
        private int windowSizeSeconds = 60;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getWaitTimeoutSeconds() {
            return waitTimeoutSeconds;
        }

        public void setWaitTimeoutSeconds(int waitTimeoutSeconds) {
            this.waitTimeoutSeconds = waitTimeoutSeconds;
        }

        public boolean isAdaptiveRateLimiting() {
            return adaptiveRateLimiting;
        }

        public void setAdaptiveRateLimiting(boolean adaptiveRateLimiting) {
            this.adaptiveRateLimiting = adaptiveRateLimiting;
        }

        public double getThrottlingFactor() {
            return throttlingFactor;
        }

        public void setThrottlingFactor(double throttlingFactor) {
            this.throttlingFactor = throttlingFactor;
        }

        public int getWindowSizeSeconds() {
            return windowSizeSeconds;
        }

        public void setWindowSizeSeconds(int windowSizeSeconds) {
            this.windowSizeSeconds = windowSizeSeconds;
        }

        @Override
        public String toString() {
            return "RateLimitProperties{" +
                    "requestsPerMinute=" + requestsPerMinute +
                    ", burstCapacity=" + burstCapacity +
                    ", waitTimeoutSeconds=" + waitTimeoutSeconds +
                    ", adaptiveRateLimiting=" + adaptiveRateLimiting +
                    ", throttlingFactor=" + throttlingFactor +
                    ", windowSizeSeconds=" + windowSizeSeconds +
                    '}';
        }
    }
}