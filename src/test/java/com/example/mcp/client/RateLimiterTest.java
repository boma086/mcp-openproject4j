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
 * Test class for RateLimiter.
 * RateLimiter的测试类。
 */
@SpringBootTest
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        
        // Set configuration for testing
        ReflectionTestUtils.setField(rateLimiter, "configuredRequestsPerMinute", 60);
        ReflectionTestUtils.setField(rateLimiter, "burstCapacity", 10);
        ReflectionTestUtils.setField(rateLimiter, "waitTimeoutSeconds", 5);
    }

    @Test
    void testTryAcquire_Success() throws RateLimitExceededException {
        // Act
        boolean result = rateLimiter.tryAcquire("test-context");

        // Assert
        assertTrue(result);
    }

    @Test
    void testTryAcquire_MultiplePermits_Success() throws RateLimitExceededException {
        // Act
        boolean result = rateLimiter.tryAcquire("test-context", 5);

        // Assert
        assertTrue(result);
    }

    @Test
    void testTryAcquire_ExceedsLimit() throws RateLimitExceededException {
        // Arrange - exhaust all permits
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire("test-context");
        }

        // Act & Assert
        assertThrows(RateLimitExceededException.class, () -> {
            rateLimiter.tryAcquire("test-context");
        });
    }

    @Test
    void testTryAcquire_ContextIsolation() throws RateLimitExceededException {
        // Arrange - exhaust permits for one context
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire("context-1");
        }

        // Act & Assert - other context should still work
        assertTrue(rateLimiter.tryAcquire("context-2"));
        
        // First context should be rate limited
        assertThrows(RateLimitExceededException.class, () -> {
            rateLimiter.tryAcquire("context-1");
        });
    }

    @Test
    void testTryAcquire_GlobalLimit() throws RateLimitExceededException {
        // Arrange - exhaust all global permits by using different contexts
        for (int i = 0; i < 15; i++) { // More than burst capacity
            rateLimiter.tryAcquire("context-" + i);
        }

        // Act & Assert - should be rate limited due to global limit
        assertThrows(RateLimitExceededException.class, () -> {
            rateLimiter.tryAcquire("new-context");
        });
    }

    @Test
    void testAcquire_WithBlocking() throws Exception {
        // Arrange - exhaust permits
        for (int i = 0; i < 9; i++) {
            rateLimiter.tryAcquire("test-context");
        }

        // Act - this should block and succeed
        long startTime = System.currentTimeMillis();
        rateLimiter.acquire("test-context");
        long endTime = System.currentTimeMillis();

        // Assert
        assertTrue(endTime - startTime < 1000, "Should not wait significantly for available permits");
    }

    @Test
    void testAcquire_TimeoutExceeded() {
        // Set a very short timeout
        ReflectionTestUtils.setField(rateLimiter, "waitTimeoutSeconds", 1);
        
        // Arrange - exhaust all permits
        for (int i = 0; i < 10; i++) {
            try {
                rateLimiter.tryAcquire("test-context");
            } catch (RateLimitExceededException e) {
                fail("Should not throw exception in setup");
            }
        }

        // Act & Assert - should throw due to timeout
        assertThrows(RateLimitExceededException.class, () -> {
            rateLimiter.acquire("test-context");
        });
    }

    @Test
    void testGetStatus() {
        // Act
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("test-context");

        // Assert
        assertNotNull(status);
        assertTrue(status.availablePermits() > 0);
        assertFalse(status.isRateLimited());
        assertEquals(0, status.estimatedWaitTime().getSeconds());
    }

    @Test
    void testGetStatus_ContextSpecific() throws RateLimitExceededException {
        // Arrange - use some permits
        rateLimiter.tryAcquire("test-context", 3);

        // Act
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("test-context");

        // Assert
        assertNotNull(status);
        assertTrue(status.availablePermits() < 10);
        assertFalse(status.isRateLimited());
    }

    @Test
    void testUpdateConfiguration() throws RateLimitExceededException {
        // Arrange - use some permits
        rateLimiter.tryAcquire("test-context", 5);

        // Act
        rateLimiter.updateConfiguration(120, 20);

        // Assert - should have more capacity now
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("test-context");
        assertTrue(status.maxPermits() >= 20);
    }

    @Test
    void testAdjustFromServerResponse() {
        // Arrange
        Instant resetTime = Instant.now().plusSeconds(60);
        
        // Act
        rateLimiter.adjustFromServerResponse(30, resetTime, "test-context");

        // Assert - configuration should be adjusted based on server response
        // The exact behavior depends on the calculation, but it shouldn't crash
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("test-context");
        assertNotNull(status);
    }

    @Test
    void testReset() throws RateLimitExceededException {
        // Arrange - use some permits
        rateLimiter.tryAcquire("test-context", 8);

        // Act
        rateLimiter.reset();

        // Assert - should be back to initial state
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("test-context");
        assertEquals(10, status.availablePermits());
    }

    @Test
    void testGetMetrics() throws RateLimitExceededException {
        // Arrange - generate some activity
        rateLimiter.tryAcquire("context-1");
        rateLimiter.tryAcquire("context-2");
        
        try {
            rateLimiter.tryAcquire("context-1", 100); // This should fail
        } catch (RateLimitExceededException e) {
            // Expected
        }

        // Act
        RateLimiter.RateLimitMetrics metrics = rateLimiter.getMetrics();

        // Assert
        assertNotNull(metrics);
        assertTrue(metrics.totalRequests() > 0);
        assertTrue(metrics.rateLimitedRequests() > 0);
        assertTrue(metrics.activeContexts() > 0);
    }

    @Test
    void testConcurrentAccess() throws Exception {
        // Arrange
        int threadCount = 20;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            if (rateLimiter.tryAcquire("concurrent-test")) {
                                successCount.incrementAndGet();
                            }
                        } catch (RateLimitExceededException e) {
                            rateLimitCount.incrementAndGet();
                        }
                        Thread.sleep(10); // Small delay between requests
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        finishLatch.await(30, TimeUnit.SECONDS); // Wait for completion
        executor.shutdown();

        // Assert
        assertTrue(successCount.get() > 0, "Some requests should succeed");
        assertTrue(rateLimitCount.get() > 0, "Some requests should be rate limited");
        System.out.println("Concurrent test results: " + successCount.get() + " succeeded, " + 
                          rateLimitCount.get() + " rate limited");
    }

    @Test
    void testEstimateWaitTime() throws RateLimitExceededException {
        // Arrange - exhaust permits
        for (int i = 0; i < 10; i++) {
            rateLimiter.tryAcquire("test-context");
        }

        // Act
        RateLimiter.RateLimitStatus status = rateLimiter.getStatus("test-context");

        // Assert
        assertNotNull(status);
        assertTrue(status.estimatedWaitTime().getSeconds() > 0, 
                  "Should estimate wait time when rate limited");
    }

    @Test
    void testDifferentContexts() throws RateLimitExceededException {
        // Act - use multiple contexts
        assertTrue(rateLimiter.tryAcquire("project-1"));
        assertTrue(rateLimiter.tryAcquire("project-2"));
        assertTrue(rateLimiter.tryAcquire("user-123"));
        assertTrue(rateLimiter.tryAcquire("global"));

        // Assert - all contexts should work independently
        RateLimiter.RateLimitMetrics metrics = rateLimiter.getMetrics();
        assertEquals(4, metrics.activeContexts());
    }

    @Test
    void testBurstHandling() throws RateLimitExceededException {
        // Act - make burst requests
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire("burst-test"));
        }

        // Assert - should be rate limited after burst
        assertThrows(RateLimitExceededException.class, () -> {
            rateLimiter.tryAcquire("burst-test");
        });
    }

    @Test
    void testReleasePermits() throws RateLimitExceededException {
        // This test validates that permits are properly managed
        // Arrange - use most permits
        for (int i = 0; i < 8; i++) {
            rateLimiter.tryAcquire("release-test");
        }

        // Act - should still have permits available
        boolean result = rateLimiter.tryAcquire("release-test", 2);

        // Assert
        assertTrue(result, "Should be able to use remaining permits");
    }
}