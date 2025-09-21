package com.example.mcp.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified test class for ReportCacheService
 * ReportCacheService的简化测试类
 */
@SpringBootTest(classes = ReportCacheService.class)
@EnableCaching
@TestPropertySource(properties = {
    "spring.cache.cache-ttl=3600000",
    "cache.weekly-report-ttl=1800000",
    "cache.risk-analysis-ttl=900000",
    "cache.completion-analysis-ttl=600000",
    "cache.work-package-ttl=300000"
})
class ReportCacheServiceSimpleTest {

    @Autowired
    private ReportCacheService reportCacheService;

    @Test
    void testServiceIsAvailable() {
        // Test that the service is properly injected and available
        assertNotNull(reportCacheService);
    }

    @Test
    void testGetCacheStatistics() {
        // Test cache statistics retrieval
        Map<String, Object> stats = reportCacheService.getCacheStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalCacheHits"));
        assertTrue(stats.containsKey("totalCacheMisses"));
        assertTrue(stats.containsKey("totalCacheEvictions"));
        assertTrue(stats.containsKey("overallHitRate"));
        assertTrue(stats.containsKey("perCacheStats"));
        assertTrue(stats.containsKey("lastUpdated"));
    }

    @Test
    void testGetCacheHealth() {
        // Test cache health check
        Map<String, Object> health = reportCacheService.getCacheHealth();
        
        assertNotNull(health);
        assertTrue(health.containsKey("status"));
        assertTrue(health.containsKey("overallHitRate"));
        assertTrue(health.containsKey("totalRequests"));
        assertTrue(health.containsKey("activeCaches"));
        assertTrue(health.containsKey("defaultTtlMs"));
        assertTrue(health.containsKey("lastHealthCheck"));
        assertTrue(health.containsKey("cacheHealth"));
    }

    @Test
    void testCacheKeyGenerator() {
        // Test that the cache key generator bean is properly configured
        Object keyGenerator = reportCacheService.reportCacheKeyGenerator();
        assertNotNull(keyGenerator);
        assertTrue(keyGenerator instanceof org.springframework.cache.interceptor.KeyGenerator);
    }

    @Test
    void testCacheEvictionMethods() {
        // Test cache eviction methods (they should not throw exceptions)
        assertDoesNotThrow(() -> reportCacheService.evictWeeklyReports("test-project"));
        assertDoesNotThrow(() -> reportCacheService.evictRiskAnalyses("test-project"));
        assertDoesNotThrow(() -> reportCacheService.evictCompletionAnalyses("test-project"));
        assertDoesNotThrow(() -> reportCacheService.evictWorkPackages("test-project"));
        assertDoesNotThrow(() -> reportCacheService.evictAllProjectCaches("test-project"));
        assertDoesNotThrow(() -> reportCacheService.clearAllCaches());
    }
}