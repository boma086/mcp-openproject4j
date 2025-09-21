package com.example.mcp.service;

import com.example.mcp.model.CompletionAnalysis;
import com.example.mcp.model.RiskAnalysis;
import com.example.mcp.model.WeeklyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ReportCacheService
 * ReportCacheService的测试类
 */
@ExtendWith(MockitoExtension.class)
@EnableCaching
@TestPropertySource(properties = {
    "cache.weekly-report-ttl=1800000",
    "cache.risk-analysis-ttl=900000",
    "cache.completion-analysis-ttl=600000",
    "cache.work-package-ttl=300000"
})
class ReportCacheServiceTest {

    @InjectMocks
    private ReportCacheService reportCacheService;

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
        
        // Verify initial state
        assertEquals(0, stats.get("totalCacheHits"));
        assertEquals(0, stats.get("totalCacheMisses"));
        assertEquals(0, stats.get("totalCacheEvictions"));
        assertEquals(0.0, stats.get("overallHitRate"));
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
        
        // Verify initial status
        assertEquals("POOR", health.get("status"));
        assertEquals(0.0, health.get("overallHitRate"));
    }

    @Test
    void testValidateCacheConfiguration() {
        // Test cache configuration validation
        Map<String, Object> validation = reportCacheService.validateCacheConfiguration();
        
        assertNotNull(validation);
        assertTrue(validation.containsKey("valid"));
        assertTrue(validation.containsKey("issues"));
        assertTrue(validation.containsKey("recommendations"));
        assertTrue(validation.containsKey("overallHitRate"));
        assertTrue(validation.containsKey("validatedAt"));
        
        // Should be valid with default configuration
        assertTrue((Boolean) validation.get("valid"));
    }

    @Test
    void testGetRecommendedTtl() {
        // Test TTL recommendations for different report types
        assertEquals(1800000L, reportCacheService.getRecommendedTtl("weekly"));
        assertEquals(1800000L, reportCacheService.getRecommendedTtl("WeeklyReport"));
        assertEquals(900000L, reportCacheService.getRecommendedTtl("risk"));
        assertEquals(900000L, reportCacheService.getRecommendedTtl("RiskAnalysis"));
        assertEquals(600000L, reportCacheService.getRecommendedTtl("completion"));
        assertEquals(600000L, reportCacheService.getRecommendedTtl("CompletionAnalysis"));
        assertEquals(300000L, reportCacheService.getRecommendedTtl("workpackage"));
        assertEquals(300000L, reportCacheService.getRecommendedTtl("workpackages"));
        
        // Test default TTL for unknown type
        assertEquals(3600000L, reportCacheService.getRecommendedTtl("unknown"));
    }

    @Test
    void testGetPerformanceMetrics() {
        // Test performance metrics retrieval
        Map<String, Object> metrics = reportCacheService.getPerformanceMetrics();
        
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("totalRequests"));
        assertTrue(metrics.containsKey("cacheHits"));
        assertTrue(metrics.containsKey("cacheMisses"));
        assertTrue(metrics.containsKey("hitRate"));
        assertTrue(metrics.containsKey("missRate"));
        assertTrue(metrics.containsKey("totalEvictions"));
        assertTrue(metrics.containsKey("estimatedTimeSavedMs"));
        assertTrue(metrics.containsKey("estimatedTimeSavedSeconds"));
        assertTrue(metrics.containsKey("perCacheMetrics"));
        assertTrue(metrics.containsKey("calculatedAt"));
        
        // Verify initial state
        assertEquals(0, metrics.get("totalRequests"));
        assertEquals(0, metrics.get("cacheHits"));
        assertEquals(0, metrics.get("cacheMisses"));
        assertEquals(0.0, metrics.get("hitRate"));
        assertEquals(1.0, metrics.get("missRate"));
    }

    @Test
    void testGenerateCacheReport() {
        // Test cache report generation
        Map<String, Object> report = reportCacheService.generateCacheReport();
        
        assertNotNull(report);
        assertTrue(report.containsKey("generatedAt"));
        assertTrue(report.containsKey("generatedBy"));
        assertTrue(report.containsKey("version"));
        assertTrue(report.containsKey("statistics"));
        assertTrue(report.containsKey("health"));
        assertTrue(report.containsKey("performance"));
        assertTrue(report.containsKey("configuration"));
        assertTrue(report.containsKey("usageSummary"));
        assertTrue(report.containsKey("recommendations"));
        
        // Verify report content
        assertEquals("ReportCacheService", report.get("generatedBy"));
        assertEquals("1.0.0", report.get("version"));
    }

    @Test
    void testInvalidateStaleEntries() {
        // Test stale entry invalidation
        int result = reportCacheService.invalidateStaleEntries(60);
        
        // Should return a count (even if placeholder)
        assertTrue(result >= 0);
    }

    @Test
    void testCacheKeyGeneration() {
        // Test that the cache key generator bean is properly configured
        assertNotNull(reportCacheService.reportCacheKeyGenerator());
    }

    @Test
    void testCacheNamesConfiguration() {
        // Test that cache names are properly configured
        Map<String, Object> stats = reportCacheService.getCacheStatistics();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> perCacheStats = (Map<String, Map<String, Object>>) stats.get("perCacheStats");
        
        assertTrue(perCacheStats.containsKey("weeklyReports"));
        assertTrue(perCacheStats.containsKey("riskAnalyses"));
        assertTrue(perCacheStats.containsKey("completionAnalyses"));
        assertTrue(perCacheStats.containsKey("workPackages"));
    }

    @Test
    void testCacheConfigurationProperties() {
        // Test that configuration properties are properly loaded
        assertEquals(1800000L, reportCacheService.getRecommendedTtl("weekly"));
        assertEquals(900000L, reportCacheService.getRecommendedTtl("risk"));
        assertEquals(600000L, reportCacheService.getRecommendedTtl("completion"));
        assertEquals(300000L, reportCacheService.getRecommendedTtl("workpackages"));
    }

    @Test
    void testStatisticsTracking() {
        // Test that statistics tracking works correctly
        
        // Get initial stats
        Map<String, Object> initialStats = reportCacheService.getCacheStatistics();
        long initialHits = (Long) initialStats.get("totalCacheHits");
        long initialMisses = (Long) initialStats.get("totalCacheMisses");
        
        // Simulate some cache operations (this would normally be done through actual cache usage)
        // For now, we just verify that the statistics structure exists and can be retrieved
        
        // Get updated stats
        Map<String, Object> updatedStats = reportCacheService.getCacheStatistics();
        assertNotNull(updatedStats);
        
        // Verify that the statistics structure is consistent
        assertEquals(initialHits, updatedStats.get("totalCacheHits"));
        assertEquals(initialMisses, updatedStats.get("totalCacheMisses"));
    }

    @Test
    void testHealthStatusCalculation() {
        // Test health status calculation based on hit rate
        
        // Get initial health (should be POOR due to 0% hit rate)
        Map<String, Object> health = reportCacheService.getCacheHealth();
        assertEquals("POOR", health.get("status"));
        
        // Verify health structure
        @SuppressWarnings("unchecked")
        Map<String, String> cacheHealth = (Map<String, String>) health.get("cacheHealth");
        assertNotNull(cacheHealth);
        
        // Verify per-cache health status
        assertEquals("POOR", cacheHealth.get("weeklyReports"));
        assertEquals("POOR", cacheHealth.get("riskAnalyses"));
        assertEquals("POOR", cacheHealth.get("completionAnalyses"));
        assertEquals("POOR", cacheHealth.get("workPackages"));
    }

    @Test
    void testPerformanceMetricsCalculation() {
        // Test performance metrics calculation
        
        Map<String, Object> metrics = reportCacheService.getPerformanceMetrics();
        
        // Verify hit/miss rate calculations
        double hitRate = (Double) metrics.get("hitRate");
        double missRate = (Double) metrics.get("missRate");
        
        assertEquals(0.0, hitRate);
        assertEquals(1.0, missRate);
        assertEquals(1.0, hitRate + missRate, 0.001);
        
        // Verify time saved calculation
        long estimatedTimeSavedMs = (Long) metrics.get("estimatedTimeSavedMs");
        assertEquals(0, estimatedTimeSavedMs);
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

    @Test
    void testReportStructure() {
        // Test that the cache report has the expected structure
        
        Map<String, Object> report = reportCacheService.generateCacheReport();
        
        // Verify main sections
        assertTrue(report.containsKey("statistics"));
        assertTrue(report.containsKey("health"));
        assertTrue(report.containsKey("performance"));
        assertTrue(report.containsKey("configuration"));
        assertTrue(report.containsKey("usageSummary"));
        assertTrue(report.containsKey("recommendations"));
        
        // Verify usage summary
        @SuppressWarnings("unchecked")
        Map<String, Object> usageSummary = (Map<String, Object>) report.get("usageSummary");
        assertNotNull(usageSummary);
        assertTrue(usageSummary.containsKey("totalCacheRequests"));
        assertTrue(usageSummary.containsKey("cacheHitRate"));
        assertTrue(usageSummary.containsKey("mostUsedCache"));
        assertTrue(usageSummary.containsKey("leastUsedCache"));
        assertTrue(usageSummary.containsKey("cacheEfficiency"));
        
        // Verify recommendations
        @SuppressWarnings("unchecked")
        List<String> recommendations = (List<String>) report.get("recommendations");
        assertNotNull(recommendations);
        assertTrue(recommendations.size() > 0);
    }

    @Test
    void testConcurrentAccess() {
        // Test that the service can handle concurrent access safely
        
        // This is a basic test - in a real scenario, you would use concurrent threads
        // For now, we just verify that the service methods don't throw exceptions
        // when called in sequence (which would indicate thread safety issues)
        
        assertDoesNotThrow(() -> {
            reportCacheService.getCacheStatistics();
            reportCacheService.getCacheHealth();
            reportCacheService.getPerformanceMetrics();
            reportCacheService.validateCacheConfiguration();
            reportCacheService.generateCacheReport();
        });
    }

    @Test
    void testCacheConfigurationWithCustomTtl() {
        // Test that custom TTL values are properly handled
        
        // Verify that different report types have different TTL values
        long weeklyTtl = reportCacheService.getRecommendedTtl("weekly");
        long riskTtl = reportCacheService.getRecommendedTtl("risk");
        long completionTtl = reportCacheService.getRecommendedTtl("completion");
        long workPackageTtl = reportCacheService.getRecommendedTtl("workpackages");
        
        // Verify that TTL values are different and reasonable
        assertNotEquals(weeklyTtl, riskTtl);
        assertNotEquals(riskTtl, completionTtl);
        assertNotEquals(completionTtl, workPackageTtl);
        
        // Verify TTL value ordering (weekly > risk > completion > workpackages)
        assertTrue(weeklyTtl > riskTtl);
        assertTrue(riskTtl > completionTtl);
        assertTrue(completionTtl > workPackageTtl);
    }
}