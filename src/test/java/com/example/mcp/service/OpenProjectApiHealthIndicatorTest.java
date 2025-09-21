package com.example.mcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for OpenProjectApiHealthIndicator.
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@ExtendWith(MockitoExtension.class)
class OpenProjectApiHealthIndicatorTest {

    @Mock
    private RestTemplate mockRestTemplate;

    private OpenProjectApiHealthIndicator apiHealthIndicator;

    @BeforeEach
    void setUp() {
        apiHealthIndicator = new OpenProjectApiHealthIndicator();
        
        // Use reflection to set the RestTemplate (since it's private final)
        try {
            var restTemplateField = OpenProjectApiHealthIndicator.class.getDeclaredField("restTemplate");
            restTemplateField.setAccessible(true);
            restTemplateField.set(apiHealthIndicator, mockRestTemplate);
        } catch (Exception e) {
            fail("Failed to set RestTemplate: " + e.getMessage());
        }
    }

    @Test
    void testHealthReturnsUpStatusWhenAllChecksPass() {
        // Arrange
        ResponseEntity<String> connectivityResponse = new ResponseEntity<>("{\"_links\":{}}", HttpStatus.OK);
        ResponseEntity<String> authResponse = new ResponseEntity<>("{\"_embedded\":{\"projects\":[]}}", HttpStatus.OK);
        ResponseEntity<String> endpointResponse = new ResponseEntity<>("{\"_embedded\":{\"workPackages\":[]}}", HttpStatus.OK);

        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenReturn(connectivityResponse)
            .thenReturn(authResponse)
            .thenReturn(endpointResponse);

        // Act
        Health health = apiHealthIndicator.health();

        // Assert
        assertNotNull(health);
        assertEquals(Status.UP, health.getStatus());
        assertTrue(health.getDetails().containsKey("connectivity"));
        assertTrue(health.getDetails().containsKey("authentication"));
        assertTrue(health.getDetails().containsKey("endpoints"));
        assertEquals("HEALTHY", health.getDetails().get("connectivity"));
        assertEquals("HEALTHY", health.getDetails().get("authentication"));
    }

    @Test
    void testHealthReturnsDownWhenConnectivityFails() {
        // Arrange
        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // Act
        Health health = apiHealthIndicator.health();

        // Assert
        assertNotNull(health);
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("connectivity"));
        assertEquals("UNHEALTHY", health.getDetails().get("connectivity"));
        assertTrue(health.getDetails().containsKey("connectivityError"));
    }

    @Test
    void testHealthReturnsDownWhenAuthenticationFails() {
        // Arrange
        ResponseEntity<String> connectivityResponse = new ResponseEntity<>("{\"_links\":{}}", HttpStatus.OK);
        
        // Simulate authentication failure with exception
        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenReturn(connectivityResponse)
            .thenThrow(new RuntimeException("Authentication failed: 401 Unauthorized"));

        // Act
        Health health = apiHealthIndicator.health();

        // Assert
        assertNotNull(health);
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().containsKey("connectivity"));
        assertEquals("HEALTHY", health.getDetails().get("connectivity"));
        assertTrue(health.getDetails().containsKey("authentication"));
        assertEquals("UNHEALTHY", health.getDetails().get("authentication"));
        assertEquals("DENIED", health.getDetails().get("projectAccess"));
    }

    @Test
    void testHealthReturnsOutOfServiceWhenEndpointsFail() {
        // Arrange
        ResponseEntity<String> connectivityResponse = new ResponseEntity<>("{\"_links\":{}}", HttpStatus.OK);
        ResponseEntity<String> authResponse = new ResponseEntity<>("{\"_embedded\":{\"projects\":[]}}", HttpStatus.OK);
        ResponseEntity<String> endpointResponse = new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);

        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenReturn(connectivityResponse)
            .thenReturn(authResponse)
            .thenReturn(endpointResponse);

        // Act
        Health health = apiHealthIndicator.health();

        // Assert
        assertNotNull(health);
        assertEquals(Status.OUT_OF_SERVICE, health.getStatus());
        assertTrue(health.getDetails().containsKey("endpoints"));
        assertFalse((Boolean) health.getDetails().get("endpointsHealthy"));
    }

    @Test
    void testApiHealthSummaryContainsExpectedFields() {
        // Arrange
        ResponseEntity<String> response = new ResponseEntity<>("{\"_embedded\":{\"projects\":[]}}", HttpStatus.OK);

        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        Map<String, Object> summary = apiHealthIndicator.getApiHealthSummary();

        // Assert
        assertNotNull(summary);
        assertTrue(summary.containsKey("status"));
        assertTrue(summary.containsKey("timestamp"));
        assertTrue(summary.containsKey("baseUrl"));
        // These may not be present depending on the mock responses
    }

    @Test
    void testHealthHandlesRateLimiting() {
        // Arrange
        ResponseEntity<String> connectivityResponse = new ResponseEntity<>("{\"_links\":{}}", HttpStatus.OK);
        ResponseEntity<String> authResponse = new ResponseEntity<>("{\"_embedded\":{\"projects\":[]}}", HttpStatus.OK);
        HttpHeaders rateLimitHeaders = new HttpHeaders();
        rateLimitHeaders.set("Retry-After", "60");
        ResponseEntity<String> rateLimitedResponse = new ResponseEntity<>("Too Many Requests", rateLimitHeaders, HttpStatus.TOO_MANY_REQUESTS);

        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenReturn(connectivityResponse)
            .thenReturn(authResponse)
            .thenReturn(rateLimitedResponse);

        // Act
        Health health = apiHealthIndicator.health();

        // Assert
        assertNotNull(health);
        // Should still be UP if other endpoints work, but with rate limiting detected
        assertTrue(health.getDetails().containsKey("endpoints"));
    }

    @Test
    void testHealthMeasuresResponseTime() {
        // Arrange
        ResponseEntity<String> response = new ResponseEntity<>("{\"_embedded\":{\"projects\":[]}}", HttpStatus.OK);

        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        Health health = apiHealthIndicator.health();

        // Assert
        assertNotNull(health);
        assertTrue(health.getDetails().containsKey("responseTime"));
        assertNotNull(health.getDetails().get("responseTimeMs"));
    }

    @Test
    void testHealthHandlesSlowResponseTime() {
        // Arrange
        // Create a response that will take time to process
        ResponseEntity<String> slowResponse = new ResponseEntity<>("{\"_embedded\":{\"projects\":[]}}", HttpStatus.OK);

        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenAnswer(invocation -> {
                Thread.sleep(15); // Simulate slow response
                return slowResponse;
            });

        // Act
        Health health = apiHealthIndicator.health();

        // Assert
        assertNotNull(health);
        // Should still work but might have elevated status due to slow response
        assertTrue(health.getDetails().containsKey("responseTime"));
        Long responseTime = (Long) health.getDetails().get("responseTimeMs");
        assertNotNull(responseTime);
        assertTrue(responseTime >= 15); // At least 15ms from our sleep
    }

    @Test
    void testApiHealthSummaryHandlesException() {
        // Arrange
        when(mockRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Test exception"));

        // Act
        Map<String, Object> summary = apiHealthIndicator.getApiHealthSummary();

        // Assert
        assertNotNull(summary);
        assertEquals("DOWN", summary.get("status"));
        assertTrue(summary.containsKey("error"));
    }
}