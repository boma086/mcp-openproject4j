package com.example.mcp.client;

import com.example.mcp.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Rate Limiter for OpenProject API calls using Token Bucket algorithm.
 * 使用令牌桶算法的OpenProject API调用速率限制器。
 * 
 * <p>This rate limiter implements the Token Bucket algorithm to control the rate of API requests
 * to OpenProject, preventing rate limit violations and providing graceful handling when limits
 * are approached or exceeded.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Token Bucket algorithm for smooth rate limiting</li>
 *   <li>Multiple rate limit contexts (global, per-project, per-user)</li>
 *   <li>Thread-safe concurrent access</li>
 *   <li>Dynamic rate limit adjustment</li>
 *   <li>Wait time calculation for rate-limited requests</li>
 *   <li>Integration with RateLimitExceededException</li>
 *   <li>Comprehensive monitoring and logging</li>
 * </ul>
 * 
 * <p>This implementation addresses the following requirements:</p>
 * <ul>
 *   <li><strong>NFR1: Performance</strong> - Handle API rate limiting gracefully, complete within 30 seconds</li>
 *   <li><strong>FR2: OpenProject API Integration</strong> - Handle API rate limits (approximately 100 req/min)</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Component
@EnableConfigurationProperties
public class RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    
    // Default configuration values
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final int DEFAULT_BURST_CAPACITY = 10;
    private static final int DEFAULT_WAIT_TIMEOUT_SECONDS = 30;
    
    @Value("${openproject.rate-limit.requests-per-minute:100}")
    private int configuredRequestsPerMinute;
    
    @Value("${openproject.rate-limit.burst-capacity:10}")
    private int burstCapacity;
    
    @Value("${openproject.rate-limit.wait-timeout-seconds:30}")
    private int waitTimeoutSeconds;
    
    // Token bucket for global rate limiting
    private final TokenBucket globalBucket;
    
    // Context-specific buckets (per-project, per-user, etc.)
    private final ConcurrentHashMap<String, TokenBucket> contextBuckets;
    
    // Monitoring metrics
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong rateLimitedRequests = new AtomicLong(0);
    private final AtomicLong waitTimeTotal = new AtomicLong(0);
    
    // Configuration lock for dynamic updates
    private final ReentrantLock configLock = new ReentrantLock();
    
    /**
     * Constructs a new RateLimiter with default configuration.
     */
    public RateLimiter() {
        this.globalBucket = new TokenBucket(configuredRequestsPerMinute, burstCapacity);
        this.contextBuckets = new ConcurrentHashMap<>();
        logger.info("RateLimiter initialized with {} requests per minute, burst capacity {}", 
                   configuredRequestsPerMinute, burstCapacity);
    }
    
    /**
     * Attempts to acquire a permit for an API request.
     * 尝试获取API请求的许可。
     * 
     * @param context The rate limit context (e.g., "global", "project:123", "user:456")
     * @return true if the permit was acquired, false if rate limited
     * @throws RateLimitExceededException if the request would exceed rate limits
     */
    public boolean tryAcquire(String context) throws RateLimitExceededException {
        return tryAcquire(context, 1);
    }
    
    /**
     * Attempts to acquire multiple permits for an API request.
     * 尝试获取多个API请求许可。
     * 
     * @param context The rate limit context (e.g., "global", "project:123", "user:456")
     * @param permits The number of permits to acquire
     * @return true if the permits were acquired, false if rate limited
     * @throws RateLimitExceededException if the request would exceed rate limits
     */
    public boolean tryAcquire(String context, int permits) throws RateLimitExceededException {
        totalRequests.incrementAndGet();
        
        Instant startTime = Instant.now();
        
        try {
            // Check global rate limit first
            if (!globalBucket.tryAcquire(permits)) {
                Duration globalWaitTime = globalBucket.estimateWaitTime(permits);
                handleRateLimitExceeded(context, permits, globalWaitTime, "global");
                return false;
            }
            
            // Check context-specific rate limit if not global
            if (!"global".equals(context)) {
                TokenBucket contextBucket = contextBuckets.computeIfAbsent(context, 
                    k -> new TokenBucket(configuredRequestsPerMinute, burstCapacity));
                
                if (!contextBucket.tryAcquire(permits)) {
                    // Release global permit since context limit was exceeded
                    globalBucket.release(permits);
                    
                    Duration contextWaitTime = contextBucket.estimateWaitTime(permits);
                    handleRateLimitExceeded(context, permits, contextWaitTime, "context");
                    return false;
                }
            }
            
            Duration waitTime = Duration.between(startTime, Instant.now());
            waitTimeTotal.addAndGet(waitTime.toMillis());
            
            logger.debug("Rate limit permit acquired for context: {}, permits: {}, wait time: {}ms", 
                        context, permits, waitTime.toMillis());
            
            return true;
            
        } catch (RateLimitExceededException e) {
            rateLimitedRequests.incrementAndGet();
            throw e;
        }
    }
    
    /**
     * Acquires a permit for an API request, blocking if necessary.
     * 获取API请求的许可，如果需要则阻塞。
     * 
     * @param context The rate limit context (e.g., "global", "project:123", "user:456")
     * @throws RateLimitExceededException if the wait timeout is exceeded
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void acquire(String context) throws RateLimitExceededException, InterruptedException {
        acquire(context, 1);
    }
    
    /**
     * Acquires multiple permits for an API request, blocking if necessary.
     * 获取多个API请求的许可，如果需要则阻塞。
     * 
     * @param context The rate limit context (e.g., "global", "project:123", "user:456")
     * @param permits The number of permits to acquire
     * @throws RateLimitExceededException if the wait timeout is exceeded
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void acquire(String context, int permits) throws RateLimitExceededException, InterruptedException {
        totalRequests.incrementAndGet();
        
        Instant startTime = Instant.now();
        Instant deadline = startTime.plusSeconds(waitTimeoutSeconds);
        
        try {
            // Try to acquire without waiting first
            if (tryAcquire(context, permits)) {
                return;
            }
            
            // Calculate required wait time
            Duration globalWaitTime = globalBucket.estimateWaitTime(permits);
            Duration contextWaitTime = Duration.ZERO;
            
            if (!"global".equals(context)) {
                TokenBucket contextBucket = contextBuckets.computeIfAbsent(context, 
                    k -> new TokenBucket(configuredRequestsPerMinute, burstCapacity));
                contextWaitTime = contextBucket.estimateWaitTime(permits);
            }
            
            Duration totalWaitTime = globalWaitTime.plus(contextWaitTime);
            Instant readyTime = Instant.now().plus(totalWaitTime);
            
            // Check if wait time exceeds timeout
            if (readyTime.isAfter(deadline)) {
                throw new RateLimitExceededException(
                    String.format("Rate limit wait time %ds exceeds timeout %ds for context: %s", 
                                totalWaitTime.getSeconds(), waitTimeoutSeconds, context),
                    totalWaitTime, readyTime, 0, configuredRequestsPerMinute
                );
            }
            
            // Wait and retry
            logger.debug("Waiting {}ms for rate limit permits for context: {}, permits: {}", 
                        totalWaitTime.toMillis(), context, permits);
            
            Thread.sleep(totalWaitTime.toMillis());
            
            // Try again after waiting
            if (!tryAcquire(context, permits)) {
                throw new RateLimitExceededException(
                    String.format("Rate limit still exceeded after waiting for context: %s", context),
                    Duration.ofSeconds(1), Instant.now().plusSeconds(1), 0, configuredRequestsPerMinute
                );
            }
            
        } catch (RateLimitExceededException e) {
            rateLimitedRequests.incrementAndGet();
            throw e;
        }
    }
    
    /**
     * Gets the current rate limit status for a context.
     * 获取指定上下文的当前速率限制状态。
     * 
     * @param context The rate limit context
     * @return RateLimitStatus object with current rate limit information
     */
    public RateLimitStatus getStatus(String context) {
        TokenBucket bucket = "global".equals(context) ? globalBucket : contextBuckets.get(context);
        
        if (bucket == null) {
            return new RateLimitStatus(configuredRequestsPerMinute, configuredRequestsPerMinute, 
                                     Instant.now(), Duration.ZERO);
        }
        
        return new RateLimitStatus(
            bucket.getAvailablePermits(),
            bucket.getMaxPermits(),
            bucket.getLastRefillTime(),
            bucket.estimateWaitTime(1)
        );
    }
    
    /**
     * Updates the rate limit configuration dynamically.
     * 动态更新速率限制配置。
     * 
     * @param requestsPerMinute New requests per minute limit
     * @param burstCapacity New burst capacity
     */
    public void updateConfiguration(int requestsPerMinute, int burstCapacity) {
        configLock.lock();
        try {
            logger.info("Updating rate limiter configuration: {} requests per minute, burst capacity {}", 
                       requestsPerMinute, burstCapacity);
            
            this.configuredRequestsPerMinute = requestsPerMinute;
            this.burstCapacity = burstCapacity;
            
            // Update global bucket
            globalBucket.updateConfiguration(requestsPerMinute, burstCapacity);
            
            // Update all context buckets
            contextBuckets.forEach((context, bucket) -> 
                bucket.updateConfiguration(requestsPerMinute, burstCapacity));
            
        } finally {
            configLock.unlock();
        }
    }
    
    /**
     * Adjusts rate limits based on server response headers.
     * 根据服务器响应头调整速率限制。
     * 
     * @param remainingRequests Remaining requests from server response
     * @param resetTime Reset time from server response
     * @param context The context to adjust (null for global)
     */
    public void adjustFromServerResponse(Integer remainingRequests, Instant resetTime, String context) {
        if (remainingRequests == null || resetTime == null) {
            return;
        }
        
        Instant now = Instant.now();
        Duration windowDuration = Duration.between(now, resetTime);
        
        if (windowDuration.isNegative() || windowDuration.isZero()) {
            return;
        }
        
        // Calculate new rate limit based on server response
        double minutes = windowDuration.toMillis() / 60000.0;
        int newRequestsPerMinute = (int) Math.max(1, Math.round(remainingRequests / minutes));
        
        logger.debug("Adjusting rate limit for context {} based on server response: {} requests per minute", 
                    context, newRequestsPerMinute);
        
        updateConfiguration(newRequestsPerMinute, Math.max(1, newRequestsPerMinute / 10));
    }
    
    /**
     * Resets the rate limiter for testing purposes.
     * 重置速率限制器用于测试目的。
     */
    public void reset() {
        globalBucket.reset();
        contextBuckets.clear();
        totalRequests.set(0);
        rateLimitedRequests.set(0);
        waitTimeTotal.set(0);
        logger.info("Rate limiter reset");
    }
    
    /**
     * Gets monitoring metrics for the rate limiter.
     * 获取速率限制器的监控指标。
     * 
     * @return RateLimitMetrics object with performance metrics
     */
    public RateLimitMetrics getMetrics() {
        return new RateLimitMetrics(
            totalRequests.get(),
            rateLimitedRequests.get(),
            waitTimeTotal.get(),
            contextBuckets.size()
        );
    }
    
    /**
     * Handles rate limit exceeded scenarios.
     * 处理速率限制超出场景。
     */
    private void handleRateLimitExceeded(String context, int permits, Duration waitTime, String limitType) 
            throws RateLimitExceededException {
        
        String message = String.format("%s rate limit exceeded for context: %s, permits: %d. Wait time: %ds", 
                                     limitType, context, permits, waitTime.getSeconds());
        
        logger.warn(message);
        
        throw new RateLimitExceededException(
            message,
            waitTime,
            Instant.now().plus(waitTime),
            0,
            configuredRequestsPerMinute
        );
    }
    
    /**
     * Token Bucket implementation for rate limiting.
     * 速率限制的令牌桶实现。
     */
    private static class TokenBucket {
        private final AtomicInteger availablePermits;
        private final int maxPermits;
        private int refillRate;
        private volatile Instant lastRefillTime;
        private final ReentrantLock refillLock = new ReentrantLock();
        
        public TokenBucket(int requestsPerMinute, int burstCapacity) {
            this.maxPermits = burstCapacity;
            this.availablePermits = new AtomicInteger(burstCapacity);
            this.refillRate = requestsPerMinute;
            this.lastRefillTime = Instant.now();
        }
        
        public boolean tryAcquire(int permits) {
            refill();
            
            while (true) {
                int current = availablePermits.get();
                if (current < permits) {
                    return false;
                }
                
                if (availablePermits.compareAndSet(current, current - permits)) {
                    return true;
                }
            }
        }
        
        public void release(int permits) {
            availablePermits.updateAndGet(current -> 
                Math.min(maxPermits, current + permits));
        }
        
        public Duration estimateWaitTime(int permits) {
            int current = availablePermits.get();
            if (current >= permits) {
                return Duration.ZERO;
            }
            
            int needed = permits - current;
            double permitsPerSecond = refillRate / 60.0;
            long waitMillis = (long) Math.ceil(needed / permitsPerSecond * 1000);
            
            return Duration.ofMillis(waitMillis);
        }
        
        public int getAvailablePermits() {
            refill();
            return availablePermits.get();
        }
        
        public int getMaxPermits() {
            return maxPermits;
        }
        
        public Instant getLastRefillTime() {
            return lastRefillTime;
        }
        
        public void updateConfiguration(int requestsPerMinute, int burstCapacity) {
            refillLock.lock();
            try {
                this.refillRate = requestsPerMinute;
                // Scale available permits proportionally
                double scale = (double) burstCapacity / maxPermits;
                int newAvailable = (int) Math.min(burstCapacity, availablePermits.get() * scale);
                availablePermits.set(newAvailable);
            } finally {
                refillLock.unlock();
            }
        }
        
        public void reset() {
            availablePermits.set(maxPermits);
            lastRefillTime = Instant.now();
        }
        
        private void refill() {
            Instant now = Instant.now();
            Duration timeSinceLastRefill = Duration.between(lastRefillTime, now);
            
            if (timeSinceLastRefill.getSeconds() >= 60) {
                refillLock.lock();
                try {
                    // Re-check inside lock to prevent double refills
                    Duration actualTimeSinceLastRefill = Duration.between(lastRefillTime, now);
                    if (actualTimeSinceLastRefill.getSeconds() >= 60) {
                        int minutesElapsed = (int) (actualTimeSinceLastRefill.getSeconds() / 60);
                        int permitsToAdd = minutesElapsed * refillRate;
                        
                        availablePermits.updateAndGet(current -> 
                            Math.min(maxPermits, current + permitsToAdd));
                        
                        lastRefillTime = now;
                    }
                } finally {
                    refillLock.unlock();
                }
            }
        }
    }
    
    /**
     * Rate limit status information.
     * 速率限制状态信息。
     */
    public record RateLimitStatus(
        int availablePermits,
        int maxPermits,
        Instant lastRefillTime,
        Duration estimatedWaitTime
    ) {
        public boolean isRateLimited() {
            return availablePermits <= 0;
        }
        
        public double getUtilizationPercentage() {
            return ((double) (maxPermits - availablePermits) / maxPermits) * 100;
        }
    }
    
    /**
     * Rate limiting performance metrics.
     * 速率限制性能指标。
     */
    public record RateLimitMetrics(
        long totalRequests,
        long rateLimitedRequests,
        long totalWaitTimeMillis,
        int activeContexts
    ) {
        public double getRateLimitPercentage() {
            return totalRequests > 0 ? ((double) rateLimitedRequests / totalRequests) * 100 : 0;
        }
        
        public double getAverageWaitTimeMillis() {
            return totalRequests > 0 ? (double) totalWaitTimeMillis / totalRequests : 0;
        }
    }
}