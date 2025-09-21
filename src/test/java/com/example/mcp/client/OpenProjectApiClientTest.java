package com.example.mcp.client;

import com.example.mcp.exception.OpenProjectApiException;
import com.example.mcp.exception.RateLimitExceededException;
import com.example.mcp.model.WorkPackage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for OpenProjectApiClient.
 * OpenProjectApiClient的测试类。
 */
@ExtendWith(MockitoExtension.class)
class OpenProjectApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private OpenProjectApiClient apiClient;

    @BeforeEach
    void setUp() {
        // Create a simple ObjectMapper for testing
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.example.mcp.client.RateLimiter rateLimiter = new com.example.mcp.client.RateLimiter();
        apiClient = new OpenProjectApiClient(restTemplate, objectMapper, rateLimiter);
        
        // Set configuration values using reflection
        ReflectionTestUtils.setField(apiClient, "baseUrl", "http://test.openproject.com");
        ReflectionTestUtils.setField(apiClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(apiClient, "requestsPerMinute", 100);
    }

    @Test
    void testIsApiAccessible_Success() {
        // Arrange
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
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // Act
        boolean result = apiClient.isApiAccessible();

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetRateLimitStatus() {
        // Act
        OpenProjectApiClient.RateLimitStatus status = apiClient.getRateLimitStatus();

        // Assert
        assertNotNull(status);
        assertEquals(100, status.requestLimit());
        assertEquals(100, status.remainingRequests());
        assertFalse(status.isRateLimited());
    }

    @Test
    void testGetWorkPackages_Success() throws Exception {
        // Arrange
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
    void testGetWorkPackages_RateLimitExceeded() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"));

        // Act & Assert
        assertThrows(RateLimitExceededException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
    }

    @Test
    void testGetWorkPackages_AuthenticationError() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new org.springframework.web.client.HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
        
        assertEquals(403, exception.getStatusCode());
        assertTrue(exception.isAuthenticationError());
    }

    @Test
    void testGetWorkPackages_ConnectionError() {
        // Arrange
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));

        // Act & Assert
        OpenProjectApiException exception = assertThrows(OpenProjectApiException.class, () -> {
            apiClient.getWorkPackages(Map.of(), 20, 0);
        });
        
        assertEquals(503, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Connection error"));
    }

    @Test
    void testBuildWorkPackagesUrl_WithFilters() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
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
        assertTrue(url.contains("filters%5B%5D=status=open"));
        assertTrue(url.contains("filters%5B%5D=assignee=1"));
        assertTrue(url.contains("sort%5B%5D=-createdAt"));
    }

    @Test
    void testGetProjects_Success() throws Exception {
        // Arrange
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
        assertEquals(1, projects.size());
        assertEquals("1", projects.get(0).id());
        assertEquals("TEST", projects.get(0).identifier());
        assertEquals("Test Project", projects.get(0).name());
    }
}