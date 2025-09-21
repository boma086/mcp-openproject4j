package com.example.mcp.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for McpExceptionHandler
 * McpExceptionHandler的测试类
 */
class McpExceptionHandlerTest {
    
    private final McpExceptionHandler exceptionHandler = new McpExceptionHandler();
    
    @Test
    void testHandleOpenProjectApiException() {
        // Arrange
        OpenProjectApiException ex = OpenProjectApiException.forbidden("Access denied");
        MockHttpServletRequest request = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(request);
        
        // Act
        ResponseEntity<McpExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleOpenProjectApiException(ex, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("OPENPROJECT_API_ERROR", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("Access denied"));
        assertNotNull(response.getBody().getCorrelationId());
    }
    
    @Test
    void testHandleRateLimitExceededException() {
        // Arrange
        RateLimitExceededException ex = RateLimitExceededException.withRetryAfter(60);
        MockHttpServletRequest request = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(request);
        
        // Act
        ResponseEntity<McpExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleRateLimitExceededException(ex, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RATE_LIMIT_EXCEEDED", response.getBody().getErrorCode());
        assertEquals(60, response.getBody().getDetails().get("retry_after_seconds"));
    }
    
    @Test
    void testHandleIllegalArgumentException() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid project ID");
        MockHttpServletRequest request = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(request);
        
        // Act
        ResponseEntity<McpExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleIllegalArgumentException(ex, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ILLEGAL_ARGUMENT", response.getBody().getErrorCode());
        assertTrue(response.getBody().getMessage().contains("Invalid project ID"));
    }
    
    @Test
    void testHandleGenericException() {
        // Arrange
        Exception ex = new Exception("Unexpected error");
        MockHttpServletRequest request = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(request);
        
        // Act
        ResponseEntity<McpExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleGenericException(ex, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
        assertEquals("Exception", response.getBody().getDetails().get("error_type"));
    }
    
    @Test
    void testErrorResponseStructure() {
        // Arrange
        OpenProjectApiException ex = OpenProjectApiException.notFound("test-resource");
        MockHttpServletRequest request = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(request);
        
        // Act
        ResponseEntity<McpExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleOpenProjectApiException(ex, webRequest);
        McpExceptionHandler.ErrorResponse errorResponse = response.getBody();
        
        // Assert
        assertNotNull(errorResponse);
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus());
        assertEquals("OPENPROJECT_API_ERROR", errorResponse.getErrorCode());
        assertNotNull(errorResponse.getMessage());
        assertNotNull(errorResponse.getChineseMessage());
        assertNotNull(errorResponse.getTimestamp());
        assertNotNull(errorResponse.getCorrelationId());
        assertNotNull(errorResponse.getDetails());
        assertTrue(errorResponse.getDetails().containsKey("error_classification"));
    }
    
    @Test
    void testBilingualErrorMessages() {
        // Arrange
        OpenProjectApiException ex = OpenProjectApiException.forbidden("Test message");
        MockHttpServletRequest request = new MockHttpServletRequest();
        WebRequest webRequest = new ServletWebRequest(request);
        
        // Act
        ResponseEntity<McpExceptionHandler.ErrorResponse> response = 
            exceptionHandler.handleOpenProjectApiException(ex, webRequest);
        McpExceptionHandler.ErrorResponse errorResponse = response.getBody();
        
        // Assert
        assertNotNull(errorResponse);
        assertTrue(errorResponse.getMessage().contains("Test message"));
        assertTrue(errorResponse.getChineseMessage().contains("OpenProject API错误"));
    }
}