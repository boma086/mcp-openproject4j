package com.example.mcp.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the OpenProject API exception classes.
 * These tests verify that the exception classes handle various error scenarios
 * correctly and provide appropriate information for error handling.
 */
class OpenProjectApiExceptionTest {

    @Test
    void openProjectApiException_BasicConstruction_ShouldSetPropertiesCorrectly() {
        String message = "API request failed";
        OpenProjectApiException exception = new OpenProjectApiException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(500, exception.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        assertNull(exception.getCause());
        assertFalse(exception.isClientError());
        assertTrue(exception.isServerError());
    }

    @Test
    void openProjectApiException_WithStatusCode_ShouldSetPropertiesCorrectly() {
        String message = "Resource not found";
        int statusCode = 404;
        HttpStatus httpStatus = HttpStatus.NOT_FOUND;
        
        OpenProjectApiException exception = new OpenProjectApiException(message, statusCode, httpStatus);
        
        assertEquals(message, exception.getMessage());
        assertEquals(statusCode, exception.getStatusCode());
        assertEquals(httpStatus, exception.getHttpStatus());
        assertTrue(exception.isClientError());
        assertFalse(exception.isServerError());
    }

    @Test
    void openProjectApiException_WithCause_ShouldSetPropertiesCorrectly() {
        String message = "Server error";
        Throwable cause = new RuntimeException("Connection failed");
        int statusCode = 503;
        HttpStatus httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        
        OpenProjectApiException exception = new OpenProjectApiException(message, cause, statusCode, httpStatus);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(statusCode, exception.getStatusCode());
        assertEquals(httpStatus, exception.getHttpStatus());
    }

    @Test
    void openProjectApiException_FactoryMethods_ShouldCreateCorrectExceptions() {
        // Test forbidden
        OpenProjectApiException forbidden = OpenProjectApiException.forbidden("Access denied");
        assertEquals("Access denied", forbidden.getMessage());
        assertEquals(403, forbidden.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getHttpStatus());
        assertTrue(forbidden.isAuthenticationError());

        // Test not found
        OpenProjectApiException notFound = OpenProjectApiException.notFound("work-package-123");
        assertEquals("Resource not found: work-package-123", notFound.getMessage());
        assertEquals(404, notFound.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, notFound.getHttpStatus());

        // Test server error
        OpenProjectApiException serverError = OpenProjectApiException.serverError("Database failure", 503);
        assertEquals("Database failure", serverError.getMessage());
        assertEquals(503, serverError.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, serverError.getHttpStatus());
    }

    @Test
    void openProjectApiException_UserFriendlyMessage_ShouldReturnAppropriateMessages() {
        assertEquals("Authentication failed. Please check your API key.", 
                    OpenProjectApiException.forbidden("Auth failed").getUserFriendlyMessage());
        
        assertEquals("Access denied. You don't have permission to perform this action.", 
                    OpenProjectApiException.notFound("resource").getUserFriendlyMessage());
        
        assertEquals("The requested resource was not found in OpenProject.", 
                    OpenProjectApiException.notFound("resource").getUserFriendlyMessage());
        
        assertEquals("OpenProject server is temporarily unavailable. Please try again later.", 
                    OpenProjectApiException.serverError("Server down", 503).getUserFriendlyMessage());
    }

    @Test
    void rateLimitExceededException_BasicConstruction_ShouldSetPropertiesCorrectly() {
        String message = "Too many requests";
        RateLimitExceededException exception = new RateLimitExceededException(message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(429, exception.getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
        assertNull(exception.getRetryAfter());
        assertEquals(0, exception.getSecondsUntilReset());
    }

    @Test
    void rateLimitExceededException_WithRetryAfter_ShouldSetPropertiesCorrectly() {
        String message = "Rate limit exceeded";
        Duration retryAfter = Duration.ofSeconds(30);
        
        RateLimitExceededException exception = new RateLimitExceededException(message, retryAfter);
        
        assertEquals(message, exception.getMessage());
        assertEquals(retryAfter, exception.getRetryAfter());
        assertEquals(30, exception.getSecondsUntilReset());
        assertFalse(exception.isReset());
    }

    @Test
    void rateLimitExceededException_WithDetails_ShouldSetPropertiesCorrectly() {
        String message = "Rate limit exceeded";
        Duration retryAfter = Duration.ofSeconds(60);
        Instant resetTime = Instant.now().plusSeconds(60);
        int remaining = 5;
        int limit = 100;
        
        RateLimitExceededException exception = new RateLimitExceededException(
            message, retryAfter, resetTime, remaining, limit);
        
        assertEquals(message, exception.getMessage());
        assertEquals(retryAfter, exception.getRetryAfter());
        assertEquals(resetTime, exception.getResetTime());
        assertEquals(remaining, exception.getRemainingRequests());
        assertEquals(limit, exception.getRequestLimit());
        assertEquals(60, exception.getSecondsUntilReset());
        assertFalse(exception.isReset());
    }

    @Test
    void rateLimitExceededException_FactoryMethods_ShouldCreateCorrectExceptions() {
        // Test withRetryAfter
        RateLimitExceededException withRetryAfter = RateLimitExceededException.withRetryAfter(45);
        assertEquals("Rate limit exceeded. Retry after 45 seconds.", withRetryAfter.getMessage());
        assertEquals(Duration.ofSeconds(45), withRetryAfter.getRetryAfter());
        assertEquals(45, withRetryAfter.getSecondsUntilReset());

        // Test withDetails
        Instant futureTime = Instant.now().plusSeconds(120);
        RateLimitExceededException withDetails = RateLimitExceededException.withDetails(10, 50, futureTime);
        assertTrue(withDetails.getMessage().contains("10/50 requests used"));
        assertEquals(10, withDetails.getRemainingRequests());
        assertEquals(50, withDetails.getRequestLimit());
    }

    @Test
    void rateLimitExceededException_SuggestedBackoff_ShouldCalculateCorrectly() {
        RateLimitExceededException exception = new RateLimitExceededException("Rate limited");
        
        // Test exponential backoff
        Duration backoff1 = exception.getSuggestedBackoff(1);
        Duration backoff2 = exception.getSuggestedBackoff(2);
        Duration backoff3 = exception.getSuggestedBackoff(3);
        
        assertTrue(backoff2.toMillis() > backoff1.toMillis());
        assertTrue(backoff3.toMillis() > backoff2.toMillis());
        
        // Test capping at 1 minute
        Duration backoff10 = exception.getSuggestedBackoff(10);
        assertEquals(60000, backoff10.toMillis());
    }

    @Test
    void rateLimitExceededException_UserFriendlyMessage_ShouldIncludeRetryInformation() {
        Duration retryAfter = Duration.ofSeconds(30);
        RateLimitExceededException exception = new RateLimitExceededException("Rate limited", retryAfter);
        
        String message = exception.getUserFriendlyMessage();
        assertTrue(message.contains("30 seconds"));
        assertTrue(message.contains("OpenProject API rate limit exceeded"));
    }

    @Test
    void rateLimitExceededException_DetailedMessage_ShouldIncludeAllInformation() {
        Duration retryAfter = Duration.ofSeconds(60);
        Instant resetTime = Instant.now().plusSeconds(120);
        RateLimitExceededException exception = new RateLimitExceededException(
            "Rate limited", retryAfter, resetTime, 25, 100);
        
        String detailedMessage = exception.getDetailedMessage();
        assertTrue(detailedMessage.contains("Rate limited"));
        assertTrue(detailedMessage.contains("Retry-After: 60s"));
        assertTrue(detailedMessage.contains("Requests: 25/100"));
    }
}