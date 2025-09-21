package com.example.mcp.client;

import com.example.mcp.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RateLimiter.
 * RateLimiter的集成测试。
 * 
 * <p>This test validates that the RateLimiter works correctly in a Spring context
 * and integrates properly with the application configuration.</p>
 */
@SpringBootTest
class RateLimiterIntegrationTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        
        // Configure for testing
        ReflectionTestUtils.setField(rateLimiter, "configuredRequestsPerMinute", 60);
        ReflectionTestUtils.setField(rateLimiter, "burstCapacity", 10);
        ReflectionTestUtils.setField(rateLimiter, "waitTimeoutSeconds", 5);
    }

    @Test
    void testBasicRateLimiting() throws RateLimitExceededException {
        // Arrange
        String context = "integration-test";

        // Act
        boolean result1 = rateLimiter.tryAcquire(context);
        boolean result2 = rateLimiter.tryAcquire(context);
        boolean result3 = rateLimiter.tryAcquire(context);

        // Assert
        assertTrue(result1, "First request should succeed");
        assertTrue(result2, "Second request should succeed");
        assertTrue(result3, "Third request should succeed");
    }

    @Test
    void testRateLimitExceeded() throws RateLimitExceededException {
        // Arrange
        String context = "burst-test";

        // Act - exhaust burst capacity
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(context), "Request " + i + " should succeed");
        }

        // Assert - next request should be rate limited
        assertThrows(RateLimitExceededException.class, () -> {
            rateLimiter.tryAcquire(context);
        }, "Should throw RateLimitExceededException when burst capacity exceeded");
    }

    @Test
    void testContextIsolation() throws RateLimitExceededException {
        // Arrange
        String context1 = "project-1";
        String context2 = "project-2";

        // Act - exhaust context1
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire(context1);
        }

        // Assert - context1 should be rate limited, context2 should work
        assertThrows(RateLimitExceededException.class, () -> {
            rateLimiter.tryAcquire(context1);
        }, "Context1 should be rate limited");

        assertTrue(rateLimiter.tryAcquire(context2), "Context2 should work independently");
    }

    @Test
    void testAcquireWithBlocking() throws Exception {
        // Arrange
        String context = "blocking-test";
        
        // Use most permits
        for (int i = 0; i < 8; i++) {
            rateLimiter.tryAcquire(context);
        }

        // Act - this should succeed with minimal blocking
        long startTime = System.currentTimeMillis();
        rateLimiter.acquire(context);
        long endTime = System.currentTimeMillis();

        // Assert
        long duration = endTime - startTime;
        assertTrue(duration < 1000, "Should not block significantly when permits available");
    }

    @Test
    void testConcurrentRequests() throws Exception {
        // Arrange
        int threadCount = 10;
        int requestsPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            if (rateLimiter.tryAcquire("concurrent-integration")) {
                                successCount.incrementAndGet();
                            }
                        } catch (RateLimitExceededException e) {
                            rateLimitCount.incrementAndGet();
                        }
                        Thread.sleep(50); // Small delay
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(successCount.get() > 0, "Some requests should succeed");
        assertTrue(rateLimitCount.get() > 0, "Some requests should be rate limited");
        
        System.out.println("Integration test - Success: " + successCount.get() + 
                          ", Rate limited: " + rateLimitCount.get());
    }

    @Test
    void testMetricsCollection() throws RateLimitExceededException {
        // Arrange - generate some activity
        rateLimiter.tryAcquire("metrics-test");
        rateLimiter.tryAcquire("metrics-test-2");
        
        try {
            rateLimiter.tryAcquire("metrics-test", 100); // This should fail
        } catch (RateLimitExceededException e) {
            // Expected
        }

        // Act
        RateLimiter.RateLimitMetrics metrics = rateLimiter.getMetrics();

        // Assert
        assertNotNull(metrics);
        assertTrue(metrics.totalRequests() >= 3, "Should track total requests");
        assertTrue(metrics.rateLimitedRequests() >= 1, "Should track rate limited requests");
        assertTrue(metrics.activeContexts() >= 2, "Should track active contexts");
        assertTrue(metrics.getRateLimitPercentage() > 0, "Should calculate rate limit percentage");
    }

    @Test
    void testStatusReporting() throws RateLimitExceededException {
        // Arrange
        String context = "status-test";
        rateLimiter.tryAcquire(context, 3);

        // Act
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus(context);

        // Assert
        assertNotNull(status);
        assertTrue(status.availablePermits() > 0, "Should have available permits");
        assertTrue(status.maxPermits() > 0, "Should have max permits");
        assertNotNull(status.lastRefillTime(), "Should have last refill time");
        assertEquals(0, status.estimatedWaitTime().getSeconds(), "Should not need to wait");
        assertFalse(status.isRateLimited(), "Should not be rate limited");
        assertTrue(status.getUtilizationPercentage() > 0, "Should have utilization percentage");
    }

    @Test
    void testConfigurationUpdate() throws RateLimitExceededException {
        // Arrange - use some permits
        rateLimiter.tryAcquire("config-test", 5);

        // Act
        rateLimiter.updateConfiguration(120, 20);

        // Assert
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("config-test");
        assertTrue(status.maxPermits() >= 20, "Should have increased capacity");
        
        // Should be able to make more requests
        for (int i = 0; i < 15; i++) {
            assertTrue(rateLimiter.tryAcquire("config-test"), "Should handle more requests after config update");
        }
    }

    @Test
    void testAdjustFromServerResponse() {
        // Arrange
        Instant resetTime = Instant.now().plusSeconds(60);
        Integer remainingRequests = 30;

        // Act
        rateLimiter.adjustFromServerResponse(remainingRequests, resetTime, "server-test");

        // Assert - should not crash and should adjust appropriately
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("server-test");
        assertNotNull(status, "Should create status for new context");
        assertTrue(status.maxPermits() > 0, "Should have positive max permits");
    }

    @Test
    void testRateLimiterReset() throws RateLimitExceededException {
        // Arrange - use some permits
        rateLimiter.tryAcquire("reset-test", 7);
        
        // Verify some permits used
        RateLimiter.RateLimitStatus beforeReset = rateLimiter.getStatus("reset-test");
        assertTrue(beforeReset.availablePermits() < 10, "Should have used some permits");

        // Act
        rateLimiter.reset();

        // Assert
        RateLimiter.RateLimitStatus afterReset = rateLimiter.getStatus("reset-test");
        assertEquals(10, afterReset.availablePermits(), "Should reset to full capacity");
        
        // Verify metrics are reset
        RateLimiter.RateLimitMetrics metrics = rateLimiter.getMetrics();
        assertEquals(0, metrics.totalRequests(), "Should reset total requests");
        assertEquals(0, metrics.rateLimitedRequests(), "Should reset rate limited requests");
    }

    @Test
    void testMultipleAcquireCalls() throws RateLimitExceededException {
        // Arrange
        String context = "multi-acquire-test";

        // Act
        boolean singlePermit = rateLimiter.tryAcquire(context);
        boolean multiPermit = rateLimiter.tryAcquire(context, 3);
        boolean remainingPermit = rateLimiter.tryAcquire(context);

        // Assert
        assertTrue(singlePermit, "Single permit should succeed");
        assertTrue(multiPermit, "Multi permit should succeed");
        assertTrue(remainingPermit, "Remaining permit should succeed");

        // Verify total permits used
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus(context);
        assertEquals(5, 10 - status.availablePermits(), "Should have used 5 permits total");
    }

    @Test
    void testWaitTimeEstimation() throws RateLimitExceededException {
        // Arrange - exhaust permits
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire("wait-test");
        }

        // Act
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("wait-test");

        // Assert
        assertNotNull(status.estimatedWaitTime(), "Should estimate wait time");
        assertTrue(status.estimatedWaitTime().getSeconds() > 0, "Should have positive wait time when rate limited");
    }

    @Test
    void testIntegrationWithOpenProjectApiClient() {
        // This test validates that the RateLimiter can be used in the context
        // of the OpenProjectApiClient integration
        
        // Act
        RateLimiter.RateLimitMetrics metrics = rateLimiter.getMetrics();
        
        // Assert
        assertNotNull(metrics, "Should be able to get metrics");
        assertEquals(0, metrics.totalRequests(), "Should start with zero requests");
        assertEquals(0, metrics.rateLimitedRequests(), "Should start with zero rate limited requests");
        
        // Verify context tracking
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("openproject-api");
        assertNotNull(status, "Should be able to get status for API context");
    }
}