package com.example.mcp.client;

import com.example.mcp.exception.OpenProjectApiException;
import com.example.mcp.exception.RateLimitExceededException;
import com.example.mcp.model.WorkPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for OpenProjectApiClient.
 * OpenProjectApiClient的综合测试类。
 * 
 * <p>This test class covers all public methods of the OpenProjectApiClient,
 * including successful scenarios, error handling, rate limiting, and edge cases.</p>
 * 
 * <p>Test Coverage:</p>
 * <ul>
 *   <li>FR2: OpenProject API Integration - Basic Auth, HATEOAS, filtering/pagination, rate limit handling</li>
 *   <li>NFR3: Reliability - Graceful error handling, retry logic for transient failures</li>
 *   <li>All public API methods</li>
 *   <li>Error scenarios and exception handling</li>
 *   <li>Rate limiting functionality</li>
 *   <li>Circuit breaker fallback methods</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@ExtendWith(MockitoExtension.class)
class OpenProjectApiClientTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RateLimiter rateLimiter;
    
    private OpenProjectApiClient apiClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() {
        // Create a mock RateLimiter for testing
        rateLimiter = mock(RateLimiter.class);
        
        apiClient = new OpenProjectApiClient(restTemplate, objectMapper, rateLimiter);
        
        // Set configuration values using reflection
        ReflectionTestUtils.setField(apiClient, "baseUrl", "http://test.openproject.com");
        ReflectionTestUtils.setField(apiClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(apiClient, "requestsPerMinute", 100);
        ReflectionTestUtils.setField(apiClient, "remainingRequests", 100);
        ReflectionTestUtils.setField(apiClient, "rateLimitResetTime", Instant.now().plus(1, ChronoUnit.MINUTES));
        
        // Mock default rate limiter status for legacy rate limiting
        RateLimiter.RateLimitStatus mockStatus = new RateLimiter.RateLimitStatus(
            100, 100, Instant.now(), Duration.ZERO
        );
        lenient().when(rateLimiter.getStatus(anyString())).thenReturn(mockStatus);
        
        // Note: Don't set global rate limiter behavior here - each test will configure it specifically
    }

    // ===== API Accessibility Tests =====

    @Test
    void testIsApiAccessible_Success() {
        // Arrange
        lenient().when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        ResponseEntity<String> response = ResponseEntity.ok("{\"_embedded\":{\"elements\":[]}}");
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        boolean result = apiClient.isApiAccessible();

        // Assert
        assertTrue(result);
        verify(restTemplate).exchange(contains("/projects"), any(), any(), eq(String.class));
    }

    @Test
    void testIsApiAccessible_Failure() {
        // Arrange
        lenient().when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // Act
        boolean result = apiClient.isApiAccessible();

        // Assert
        assertFalse(result);
    }

    // ===== Work Package Tests =====

    @Test
    void testGetWorkPackages_Success() throws Exception {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String mockResponse = """
            {
                "_embedded": {
                    "elements": [
                        {
                            "id": "1",
                            "subject": "Test Work Package",
                            "description": "Test description",
                            "status": {
                                "id": "1",
                                "name": "New",
                                "isClosed": false,
                                "isDefault": true,
                                "position": "1"
                            },
                            "type": {
                                "id": "1",
                                "name": "Task",
                                "isDefault": true,
                                "color": "#FF0000",
                                "position": "1"
                            },
                            "priority": {
                                "id": "8",
                                "name": "Normal",
                                "isDefault": true,
                                "position": "8"
                            },
                            "createdAt": "2023-01-01T00:00:00Z",
                            "updatedAt": "2023-01-01T00:00:00Z",
                            "_links": {
                                "project": {
                                    "href": "/api/v3/projects/1"
                                }
                            }
                        }
                    ]
                },
                "count": 1,
                "pageSize": 20,
                "offset": 0
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        List<WorkPackage> workPackages = apiClient.getWorkPackages(Map.of(), 20, 0);

        // Assert
        assertNotNull(workPackages);
        assertEquals(1, workPackages.size());
        assertEquals("1", workPackages.get(0).id());
        assertEquals("Test Work Package", workPackages.get(0).subject());
    }

    @Test
    void testGetWorkPackages_WithFilters() throws Exception {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String mockResponse = """
            {
                "_embedded": {
                    "elements": [
                        {
                            "id": "2",
                            "subject": "Filtered Work Package",
                            "description": "Filtered description",
                            "status": {
                                "id": "2",
                                "name": "In Progress",
                                "isClosed": false,
                                "isDefault": false,
                                "position": "2"
                            },
                            "type": {
                                "id": "1",
                                "name": "Task",
                                "isDefault": true,
                                "color": "#FF0000",
                                "position": "1"
                            },
                            "priority": {
                                "id": "8",
                                "name": "Normal",
                                "isDefault": true,
                                "position": "8"
                            },
                            "createdAt": "2023-01-02T00:00:00Z",
                            "updatedAt": "2023-01-02T00:00:00Z",
                            "_links": {
                                "project": {
                                    "href": "/api/v3/projects/2"
                                }
                            }
                        }
                    ]
                },
                "count": 1,
                "pageSize": 50,
                "offset": 10
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        List<WorkPackage> workPackages = apiClient.getWorkPackages(
            Map.of("status", "open", "assignee", "1"), 50, 10);

        // Assert
        assertNotNull(workPackages);
        assertEquals(1, workPackages.size());
        assertEquals("2", workPackages.get(0).id());
        assertEquals("Filtered Work Package", workPackages.get(0).subject());
        
        // Verify URL contains filters
        verify(restTemplate).exchange(contains("filters[]=status=open"), any(), any(), eq(String.class));
        verify(restTemplate).exchange(contains("filters[]=assignee=1"), any(), any(), eq(String.class));
    }

    @Test
    void testGetWorkPackages_EmptyResponse() throws Exception {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String mockResponse = """
            {
                "_embedded": {
                    "elements": []
                },
                "count": 0,
                "pageSize": 20,
                "offset": 0
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        List<WorkPackage> workPackages = apiClient.getWorkPackages(Map.of(), 20, 0);

        // Assert
        assertNotNull(workPackages);
        assertTrue(workPackages.isEmpty());
    }

    @Test
    void testGetWorkPackages_RateLimitExceeded() {
        // Arrange - mock rate limiter to throw exception
        when(rateLimiter.tryAcquire(anyString(), anyInt()))
            .thenThrow(new RateLimitExceededException("Rate limit exceeded", 
                                                 Duration.ofSeconds(60), Instant.now().plusSeconds(60), 0, 100));

        // Act & Assert
        assertThrows(RateLimitExceededException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
    }

    @Test
    void testGetWorkPackages_AuthenticationError() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
        
        assertEquals(403, exception.getStatusCode());
        assertTrue(exception.isAuthenticationError());
    }

    @Test
    void testGetWorkPackages_NotFound() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
        
        assertEquals(404, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testGetWorkPackages_ConnectionError() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new ResourceAccessException("Connection refused"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
        
        assertEquals(503, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Connection error"));
    }

    @Test
    void testGetWorkPackages_ServerError() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
        
        assertEquals(500, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Server error"));
    }

    // ===== Get Work Package By ID Tests =====

    @Test
    void testGetWorkPackageById_Success() throws Exception {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String mockResponse = """
            {
                "_embedded": {
                    "elements": [
                        {
                            "id": "123",
                            "subject": "Specific Work Package",
                            "description": "Specific description",
                            "status": {
                                "id": "1",
                                "name": "New",
                                "isClosed": false,
                                "isDefault": true,
                                "position": "1"
                            },
                            "type": {
                                "id": "1",
                                "name": "Task",
                                "isDefault": true,
                                "color": "#FF0000",
                                "position": "1"
                            },
                            "priority": {
                                "id": "8",
                                "name": "Normal",
                                "isDefault": true,
                                "position": "8"
                            },
                            "createdAt": "2023-01-01T00:00:00Z",
                            "updatedAt": "2023-01-01T00:00:00Z",
                            "_links": {
                                "project": {
                                    "href": "/api/v3/projects/1"
                                }
                            }
                        }
                    ]
                }
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        WorkPackage workPackage = apiClient.getWorkPackageById("123");

        // Assert
        assertNotNull(workPackage);
        assertEquals("123", workPackage.id());
        assertEquals("Specific Work Package", workPackage.subject());
        
        // Verify correct URL was called
        verify(restTemplate).exchange(contains("/work_packages/123"), any(), any(), eq(String.class));
    }

    @Test
    void testGetWorkPackageById_NotFound() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackageById("999");
        });
        
        assertEquals(404, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Requested resource not found"));
    }

    @Test
    void testGetWorkPackageById_RateLimitExceeded() {
        // Arrange - mock rate limiter to throw exception
        when(rateLimiter.tryAcquire(anyString(), anyInt()))
            .thenThrow(new RateLimitExceededException("Rate limit exceeded", 
                                                 Duration.ofSeconds(60), Instant.now().plusSeconds(60), 0, 100));

        // Act & Assert
        assertThrows(RateLimitExceededException.class, () -> {
            apiClient.getWorkPackageById("123");
        });
    }

    // ===== Projects Tests =====

    @Test
    void testGetProjects_Success() throws Exception {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String mockResponse = """
            {
                "_embedded": {
                    "elements": [
                        {
                            "id": "1",
                            "identifier": "TEST",
                            "name": "Test Project",
                            "description": "A test project",
                            "createdAt": "2023-01-01T00:00:00Z",
                            "updatedAt": "2023-01-01T00:00:00Z"
                        },
                        {
                            "id": "2",
                            "identifier": "DEMO",
                            "name": "Demo Project",
                            "description": "A demo project",
                            "createdAt": "2023-01-02T00:00:00Z",
                            "updatedAt": "2023-01-02T00:00:00Z"
                        }
                    ]
                }
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        List<OpenProjectApiClient.ProjectInfo> projects = apiClient.getProjects();

        // Assert
        assertNotNull(projects);
        assertEquals(2, projects.size());
        
        // Verify first project
        assertEquals("1", projects.get(0).id());
        assertEquals("TEST", projects.get(0).identifier());
        assertEquals("Test Project", projects.get(0).name());
        
        // Verify second project
        assertEquals("2", projects.get(1).id());
        assertEquals("DEMO", projects.get(1).identifier());
        assertEquals("Demo Project", projects.get(1).name());
    }

    @Test
    void testGetProjects_EmptyResponse() throws Exception {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String mockResponse = """
            {
                "_embedded": {
                    "elements": []
                }
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        List<OpenProjectApiClient.ProjectInfo> projects = apiClient.getProjects();

        // Assert
        assertNotNull(projects);
        assertTrue(projects.isEmpty());
    }

    @Test
    void testGetProjects_RateLimitExceeded() {
        // Arrange - mock rate limiter to throw exception
        when(rateLimiter.tryAcquire(anyString(), anyInt()))
            .thenThrow(new RateLimitExceededException("Rate limit exceeded", 
                                                 Duration.ofSeconds(60), Instant.now().plusSeconds(60), 0, 100));

        // Act & Assert
        assertThrows(RateLimitExceededException.class, () -> {
            apiClient.getProjects();
        });
    }

    @Test
    void testGetProjects_AuthenticationError() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getProjects();
        });
        
        assertEquals(403, exception.getStatusCode());
        assertTrue(exception.isAuthenticationError());
    }

    // ===== Rate Limiting Tests =====

    @Test
    void testGetRateLimitStatus() {
        // Arrange
        lenient().when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        
        // Act
        OpenProjectApiClient.RateLimitStatus status = apiClient.getRateLimitStatus();

        // Assert
        assertNotNull(status);
        assertEquals(100, status.requestLimit());
        assertEquals(100, status.remainingRequests());
        assertFalse(status.isRateLimited());
    }

    @Test
    void testGetAdvancedRateLimitStatus() {
        // Arrange
        RateLimiter.RateLimitStatus mockStatus = new RateLimiter.RateLimitStatus(
            50, 100, Instant.now(), Duration.ofSeconds(30)
        );
        when(rateLimiter.getStatus(anyString())).thenReturn(mockStatus);

        // Act
        RateLimiter.RateLimitStatus status = apiClient.getAdvancedRateLimitStatus();

        // Assert
        assertNotNull(status);
        assertEquals(50, status.availablePermits());
        assertEquals(100, status.maxPermits());
        assertEquals(Duration.ofSeconds(30), status.estimatedWaitTime());
        
        verify(rateLimiter).getStatus("openproject-api");
    }

    @Test
    void testGetRateLimitMetrics() {
        // Arrange
        RateLimiter.RateLimitMetrics mockMetrics = new RateLimiter.RateLimitMetrics(
            1000, 50, 5000, 5
        );
        when(rateLimiter.getMetrics()).thenReturn(mockMetrics);

        // Act
        RateLimiter.RateLimitMetrics metrics = apiClient.getRateLimitMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals(1000, metrics.totalRequests());
        assertEquals(50, metrics.rateLimitedRequests());
        assertEquals(5000, metrics.totalWaitTimeMillis());
        assertEquals(5, metrics.activeContexts());
    }

    @Test
    void testAdjustRateLimitFromResponse() {
        // Arrange
        Instant resetTime = Instant.now().plusSeconds(60);
        
        // Act
        apiClient.adjustRateLimitFromResponse(50, resetTime);

        // Assert
        verify(rateLimiter).adjustFromServerResponse(50, resetTime, "openproject-api");
    }

    @Test
    void testUpdateRateLimitConfiguration() {
        // Arrange
        lenient().when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        
        // Act
        apiClient.updateRateLimitConfiguration(200, 20);

        // Assert
        verify(rateLimiter).updateConfiguration(200, 20);
    }

    // ===== Fallback Method Tests =====

    @Test
    void testGetWorkPackagesFallback() {
        // Arrange - use reflection to access private fallback method
        Exception testException = new RuntimeException("Test exception");
        
        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            invokePrivateMethod("getWorkPackagesFallback", 
                new Object[]{Map.of(), 20, 0, testException}, 
                new Class[]{Map.class, int.class, int.class, Exception.class});
        });
        
        assertEquals(503, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("currently unavailable"));
    }

    @Test
    void testGetWorkPackageByIdFallback() {
        // Arrange
        Exception testException = new RuntimeException("Test exception");
        
        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            invokePrivateMethod("getWorkPackageByIdFallback", 
                new Object[]{"123", testException}, 
                new Class[]{String.class, Exception.class});
        });
        
        assertEquals(503, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("currently unavailable"));
    }

    @Test
    void testGetProjectsFallback() {
        // Arrange
        Exception testException = new RuntimeException("Test exception");
        
        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            invokePrivateMethod("getProjectsFallback", 
                new Object[]{testException}, 
                new Class[]{Exception.class});
        });
        
        assertEquals(503, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("currently unavailable"));
    }

    // ===== Helper Method Tests =====

    @Test
    void testBuildWorkPackagesUrl_WithFilters() throws Exception {
        // Arrange - use reflection to access private method
        java.lang.reflect.Method method = OpenProjectApiClient.class.getDeclaredMethod(
            "buildWorkPackagesUrl", Map.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        String url = (String) method.invoke(apiClient, 
            Map.of("status", "open", "assignee", "1"), 50, 10);

        // Assert
        assertTrue(url.contains("pageSize=50"));
        assertTrue(url.contains("offset=10"));
        assertTrue(url.contains("filters[]=status=open"));
        assertTrue(url.contains("filters[]=assignee=1"));
        assertTrue(url.contains("sort[]=-createdAt"));
    }

    @Test
    void testBuildWorkPackagesUrl_NoFilters() throws Exception {
        // Arrange - use reflection to access private method
        java.lang.reflect.Method method = OpenProjectApiClient.class.getDeclaredMethod(
            "buildWorkPackagesUrl", Map.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        String url = (String) method.invoke(apiClient, 
            Map.of(), 20, 0);

        // Assert
        assertTrue(url.contains("pageSize=20"));
        assertTrue(url.contains("offset=0"));
        assertTrue(url.contains("sort[]=-createdAt"));
        assertFalse(url.contains("filters"));
    }

    // ===== Edge Cases and Error Handling Tests =====

    @Test
    void testInvalidJsonResponse() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String invalidJson = "{ invalid json }";
        ResponseEntity<String> response = ResponseEntity.ok(invalidJson);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act & Assert
        // Should throw exception for invalid JSON
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
        
        // Should indicate JSON parsing error
        assertTrue(exception.getMessage().contains("Unexpected error"));
        assertEquals(500, exception.getStatusCode());
    }

    @Test
    void testMalformedWorkPackageData() {
        // Arrange
        when(rateLimiter.tryAcquire(anyString(), anyInt())).thenReturn(true);
        String mockResponse = """
            {
                "_embedded": {
                    "elements": [
                        {
                            "id": "1",
                            "subject": "Test Work Package",
                            "description": "Test description",
                            "createdAt": "2023-01-01T00:00:00Z",
                            "updatedAt": "2023-01-01T00:00:00Z"
                        }
                    ]
                }
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(response);

        // Act
        List<WorkPackage> workPackages = apiClient.getWorkPackages(Map.of(), 20, 0);

        // Assert - should skip malformed work packages
        assertNotNull(workPackages);
        assertTrue(workPackages.isEmpty());
    }

    // ===== Private Helper Methods =====

    /**
     * Helper method to invoke private methods for testing.
     */
    private Object invokePrivateMethod(String methodName, Object[] args, Class<?>[] parameterTypes) 
            throws Exception {
        java.lang.reflect.Method method = OpenProjectApiClient.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        try {
            return method.invoke(apiClient, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the target exception
            throw (Exception) e.getTargetException();
        }
    }
}