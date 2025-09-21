package com.example.mcp.exception.example;

import com.example.mcp.exception.OpenProjectApiException;
import com.example.mcp.exception.RateLimitExceededException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

/**
 * Example controller demonstrating how to use the OpenProject API exception classes
 * for proper error handling in API integration scenarios.
 * 
 * <p>This example shows best practices for:</p>
 * <ul>
 *   <li>Handling different HTTP error responses from OpenProject API</li>
 *   <li>Implementing rate limiting with retry logic</li>
 *   <li>Providing user-friendly error messages</li>
 *   <li>Logging detailed error information</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/work-packages")
public class WorkPackageControllerExample {

    private final RestTemplate restTemplate;
    private static final String OPENPROJECT_API_BASE = "https://api.openproject.org/api/v3";

    public WorkPackageControllerExample(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Example method demonstrating how to handle OpenProject API errors
     * when fetching a work package.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkPackage(@PathVariable String id) {
        try {
            String url = OPENPROJECT_API_BASE + "/work_packages/" + id;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            return ResponseEntity.ok(response.getBody());
            
        } catch (HttpClientErrorException e) {
            // Handle HTTP error responses
            return handleOpenProjectApiError(e);
            
        } catch (RestClientException e) {
            // Handle other client errors (network issues, etc.)
            throw new OpenProjectApiException(
                "Failed to communicate with OpenProject API: " + e.getMessage(), 
                e, 
                500, 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Example method showing rate limiting handling with retry logic.
     */
    @GetMapping("/{id}/with-retry")
    public ResponseEntity<?> getWorkPackageWithRetry(@PathVariable String id) {
        int maxRetries = 3;
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                String url = OPENPROJECT_API_BASE + "/work_packages/" + id;
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                
                return ResponseEntity.ok(response.getBody());
                
            } catch (HttpClientErrorException.TooManyRequests e) {
                // Handle rate limiting
                attempt++;
                if (attempt >= maxRetries) {
                    throw handleRateLimitError(e);
                }
                
                // Extract retry-after header and wait
                Duration retryAfter = extractRetryAfter(e);
                waitForRetry(retryAfter, attempt);
                
            } catch (HttpClientErrorException e) {
                // Handle other HTTP errors
                return handleOpenProjectApiError(e);
            }
        }
        
        throw new OpenProjectApiException("Failed after " + maxRetries + " attempts");
    }

    /**
     * Handles OpenProject API errors by converting Spring WebClient exceptions
     * to our custom exception classes.
     */
    private ResponseEntity<?> handleOpenProjectApiError(HttpClientErrorException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String responseBody = e.getResponseBodyAsString();
        
        OpenProjectApiException exception;
        
        switch (status) {
            case UNAUTHORIZED:
                exception = OpenProjectApiException.forbidden("Authentication failed");
                break;
                
            case FORBIDDEN:
                exception = OpenProjectApiException.forbidden("Access denied: " + responseBody);
                break;
                
            case NOT_FOUND:
                exception = OpenProjectApiException.notFound("Work package");
                break;
                
            case TOO_MANY_REQUESTS:
                exception = handleRateLimitError(e);
                break;
                
            default:
                exception = new OpenProjectApiException(
                    "OpenProject API error: " + responseBody, 
                    e, 
                    status.value(), 
                    status,
                    responseBody
                );
        }
        
        // Log detailed error information
        logError(exception);
        
        // Return user-friendly response
        return ResponseEntity.status(status)
                .body(new ErrorResponse(exception.getUserFriendlyMessage()));
    }

    /**
     * Handles rate limit errors by extracting rate limit information from headers.
     */
    private RateLimitExceededException handleRateLimitError(HttpClientErrorException e) {
        HttpHeaders headers = e.getResponseHeaders();
        
        // Extract rate limit headers
        String retryAfterHeader = headers.getFirst("Retry-After");
        String remainingHeader = headers.getFirst("X-RateLimit-Remaining");
        String limitHeader = headers.getFirst("X-RateLimit-Limit");
        String resetHeader = headers.getFirst("X-RateLimit-Reset");
        
        Duration retryAfter = null;
        Instant resetTime = null;
        int remaining = 0;
        int limit = 0;
        
        if (retryAfterHeader != null) {
            retryAfter = Duration.ofSeconds(Long.parseLong(retryAfterHeader));
        }
        
        if (remainingHeader != null) {
            remaining = Integer.parseInt(remainingHeader);
        }
        
        if (limitHeader != null) {
            limit = Integer.parseInt(limitHeader);
        }
        
        if (resetHeader != null) {
            resetTime = Instant.ofEpochSecond(Long.parseLong(resetHeader));
        }
        
        return new RateLimitExceededException(
            "OpenProject API rate limit exceeded",
            retryAfter,
            resetTime,
            remaining,
            limit
        );
    }

    /**
     * Extracts retry-after duration from HTTP headers.
     */
    private Duration extractRetryAfter(HttpClientErrorException e) {
        String retryAfterHeader = e.getResponseHeaders().getFirst("Retry-After");
        if (retryAfterHeader != null) {
            return Duration.ofSeconds(Long.parseLong(retryAfterHeader));
        }
        
        // Use exponential backoff if no retry-after header
        return Duration.ofSeconds((long) Math.pow(2, 1)); // 2 seconds for first retry
    }

    /**
     * Waits for the specified duration before retrying.
     */
    private void waitForRetry(Duration retryAfter, int attempt) {
        try {
            Duration backoff = retryAfter != null ? retryAfter : 
                Duration.ofSeconds((long) Math.pow(2, attempt));
            
            Thread.sleep(backoff.toMillis());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenProjectApiException("Retry interrupted", e, 500, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Logs error information for debugging purposes.
     */
    private void logError(OpenProjectApiException exception) {
        System.err.println("OpenProject API Error:");
        System.err.println("  Message: " + exception.getMessage());
        System.err.println("  Status: " + exception.getStatusCode());
        System.err.println("  User Friendly: " + exception.getUserFriendlyMessage());
        
        if (exception instanceof RateLimitExceededException) {
            RateLimitExceededException rateLimitEx = (RateLimitExceededException) exception;
            System.err.println("  Rate Limit Details:");
            System.err.println("    Retry After: " + rateLimitEx.getRetryAfter());
            System.err.println("    Reset Time: " + rateLimitEx.getResetTime());
            System.err.println("    Requests: " + rateLimitEx.getRemainingRequests() + 
                             "/" + rateLimitEx.getRequestLimit());
        }
    }

    /**
     * Simple error response DTO for API responses.
     */
    public static class ErrorResponse {
        private final String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
}