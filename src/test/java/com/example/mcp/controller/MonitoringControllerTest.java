package com.example.mcp.controller;

import com.example.mcp.service.OpenProjectApiHealthIndicator;
import com.example.mcp.service.ResourceMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for MonitoringController.
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {

    @Mock
    private ResourceMonitoringService mockResourceMonitoringService;

    @Mock
    private OpenProjectApiHealthIndicator mockApiHealthIndicator;

    private MonitoringController monitoringController;

    @BeforeEach
    void setUp() {
        monitoringController = new MonitoringController();
        
        // Use reflection to set the service dependencies
        try {
            var resourceField = MonitoringController.class.getDeclaredField("resourceMonitoringService");
            resourceField.setAccessible(true);
            resourceField.set(monitoringController, mockResourceMonitoringService);
            
            var apiField = MonitoringController.class.getDeclaredField("apiHealthIndicator");
            apiField.setAccessible(true);
            apiField.set(monitoringController, mockApiHealthIndicator);
        } catch (Exception e) {
            fail("Failed to set dependencies: " + e.getMessage());
        }
    }

    @Test
    void testGetResourceMetricsReturnsSuccess() {
        // Arrange
        Health mockHealth = Health.up().withDetail("memory", Map.of("heapUsed", "100MB")).build();
        when(mockResourceMonitoringService.health()).thenReturn(mockHealth);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getResourceMetrics();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("metrics"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }

    @Test
    void testGetResourceMetricsHandlesException() {
        // Arrange
        when(mockResourceMonitoringService.health()).thenThrow(new RuntimeException("Test exception"));

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getResourceMetrics();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ERROR", response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    void testGetResourceSummaryReturnsSuccess() {
        // Arrange
        Map<String, Object> mockSummary = Map.of(
            "status", "UP",
            "memoryUsage", "50%",
            "cpuUsage", "25%"
        );
        when(mockResourceMonitoringService.getResourceSummary()).thenReturn(mockSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getResourceSummary();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockSummary, response.getBody());
    }

    @Test
    void testGetApiMetricsReturnsSuccess() {
        // Arrange
        Health mockHealth = Health.up().withDetail("connectivity", "HEALTHY").build();
        when(mockApiHealthIndicator.health()).thenReturn(mockHealth);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getApiMetrics();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("metrics"));
    }

    @Test
    void testGetApiSummaryReturnsSuccess() {
        // Arrange
        Map<String, Object> mockSummary = Map.of(
            "status", "UP",
            "connectivity", "HEALTHY",
            "responseTimeMs", 150L
        );
        when(mockApiHealthIndicator.getApiHealthSummary()).thenReturn(mockSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getApiSummary();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockSummary, response.getBody());
    }

    @Test
    void testGetHealthDashboardReturnsSuccess() {
        // Arrange
        Map<String, Object> resourceSummary = Map.of("status", "UP", "memoryUsage", "50%");
        Map<String, Object> apiSummary = Map.of("status", "UP", "connectivity", "HEALTHY");
        
        when(mockResourceMonitoringService.getResourceSummary()).thenReturn(resourceSummary);
        when(mockApiHealthIndicator.getApiHealthSummary()).thenReturn(apiSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthDashboard();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertTrue(body.containsKey("resources"));
        assertTrue(body.containsKey("api"));
        assertTrue(body.containsKey("overallStatus"));
        assertTrue(body.containsKey("healthChecks"));
        assertTrue(body.containsKey("recommendations"));
        assertTrue(body.containsKey("timestamp"));
        
        // Check overall status calculation
        assertEquals("UP", body.get("overallStatus"));
        
        // Check health checks
        @SuppressWarnings("unchecked")
        Map<String, Object> healthChecks = (Map<String, Object>) body.get("healthChecks");
        assertEquals("UP", healthChecks.get("resourceHealth"));
        assertEquals("UP", healthChecks.get("apiHealth"));
        assertEquals("UP", healthChecks.get("overallHealth"));
    }

    @Test
    void testGetSpecificMetricReturnsSuccess() {
        // Arrange
        Health mockHealth = Health.up()
            .withDetail("memory", Map.of("heapUsed", "100MB", "heapUsagePercent", "50%"))
            .build();
        when(mockResourceMonitoringService.health()).thenReturn(mockHealth);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getSpecificMetric("memory", true);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("memory", body.get("metricType"));
        assertEquals("UP", body.get("status"));
        assertTrue(body.containsKey("data"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertEquals("100MB", data.get("heapUsed"));
        assertEquals("50%", data.get("heapUsagePercent"));
    }

    @Test
    void testGetSpecificMetricReturnsNotFound() {
        // Arrange
        Health mockHealth = Health.up().withDetail("memory", Map.of()).build();
        when(mockResourceMonitoringService.health()).thenReturn(mockHealth);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getSpecificMetric("nonexistent", true);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertTrue(body.containsKey("error"));
        assertTrue(body.get("error").toString().contains("not found"));
    }

    @Test
    void testGetSpecificMetricReturnsSummaryWhenDetailedFalse() {
        // Arrange
        Health mockHealth = Health.up()
            .withDetail("memory", Map.of("heapUsed", "100MB", "heapUsagePercent", "50%"))
            .build();
        when(mockResourceMonitoringService.health()).thenReturn(mockHealth);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getSpecificMetric("memory", false);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("memory", body.get("metricType"));
        assertTrue(body.containsKey("summary"));
        assertFalse(body.containsKey("data"));
    }

    @Test
    void testHealthDashboardOverallStatusCalculation() {
        // Test various combinations for overall status
        
        // Both UP -> UP
        testOverallStatusCombination("UP", "UP", "UP");
        
        // One DOWN -> DOWN
        testOverallStatusCombination("DOWN", "UP", "DOWN");
        testOverallStatusCombination("UP", "DOWN", "DOWN");
        
        // One OUT_OF_SERVICE -> OUT_OF_SERVICE
        testOverallStatusCombination("OUT_OF_SERVICE", "UP", "OUT_OF_SERVICE");
        testOverallStatusCombination("UP", "OUT_OF_SERVICE", "OUT_OF_SERVICE");
        
        // Both DOWN -> DOWN
        testOverallStatusCombination("DOWN", "DOWN", "DOWN");
    }

    private void testOverallStatusCombination(String resourceStatus, String apiStatus, String expectedOverall) {
        // Arrange
        Map<String, Object> resourceSummary = Map.of("status", resourceStatus);
        Map<String, Object> apiSummary = Map.of("status", apiStatus);
        
        when(mockResourceMonitoringService.getResourceSummary()).thenReturn(resourceSummary);
        when(mockApiHealthIndicator.getApiHealthSummary()).thenReturn(apiSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthDashboard();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedOverall, response.getBody().get("overallStatus"));
    }

    @Test
    void testHealthDashboardGeneratesRecommendations() {
        // Arrange
        Map<String, Object> resourceSummary = Map.of(
            "status", "UP",
            "memoryUsage", "95%",  // Should trigger alert
            "cpuUsage", "85%"     // Should trigger warning
        );
        Map<String, Object> apiSummary = Map.of(
            "status", "UP",
            "responseTimeMs", 12000L,  // Should trigger warning
            "rateLimited", true           // Should trigger warning
        );
        
        when(mockResourceMonitoringService.getResourceSummary()).thenReturn(resourceSummary);
        when(mockApiHealthIndicator.getApiHealthSummary()).thenReturn(apiSummary);

        // Act
        ResponseEntity<Map<String, Object>> response = monitoringController.getHealthDashboard();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> recommendations = (Map<String, Object>) response.getBody().get("recommendations");
        
        assertNotNull(recommendations);
        assertTrue(recommendations.containsKey("alerts"));
        assertTrue(recommendations.containsKey("warnings"));
        assertTrue(recommendations.containsKey("suggestions"));
        assertTrue(recommendations.containsKey("totalIssues"));
        
        // Check that we have the expected number of issues
        int totalIssues = (Integer) recommendations.get("totalIssues");
        assertTrue(totalIssues > 0);
    }
}