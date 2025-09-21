package com.example.mcp.exception;

import java.time.Duration;
import java.time.Instant;

/**
 * Exception thrown when the OpenProject API rate limit is exceeded.
 * This exception provides information about rate limiting including retry-after duration
 * and suggestions for handling rate limit scenarios.
 * 
 * <p>This exception addresses the following requirements:</p>
 * <ul>
 *   <li><strong>NFR3: Reliability</strong> - Graceful handling of rate limiting to prevent API abuse</li>
 *   <li><strong>FR2: OpenProject API Integration</strong> - Handles HTTP 429 Too Many Requests responses</li>
 * </ul>
 * 
 * <p>This exception should be used when the OpenProject API returns HTTP 429 status code,
 * indicating that the client has exceeded the allowed number of requests in a given time period.</p>
 * 
 * <p>Best practices for handling rate limits:</p>
 * <ul>
 *   <li>Implement exponential backoff retry logic</li>
 *   <li>Respect the Retry-After header if provided</li>
 *   <li>Consider implementing request queuing or throttling</li>
 *   <li>Monitor rate limit usage to proactively avoid hitting limits</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
public class RateLimitExceededException extends OpenProjectApiException {
    
    private final Instant resetTime;
    private final Duration retryAfter;
    private final int remainingRequests;
    private final int requestLimit;
    
    /**
     * Constructs a new RateLimitExceededException with the specified detail message.
     * 
     * @param message the detail message
     */
    public RateLimitExceededException(String message) {
        this(message, null, null, 0, 0);
    }
    
    /**
     * Constructs a new RateLimitExceededException with the specified detail message and retry-after duration.
     * 
     * @param message the detail message
     * @param retryAfter the duration to wait before retrying
     */
    public RateLimitExceededException(String message, Duration retryAfter) {
        this(message, retryAfter, null, 0, 0);
    }
    
    /**
     * Constructs a new RateLimitExceededException with detailed rate limit information.
     * 
     * @param message the detail message
     * @param retryAfter the duration to wait before retrying
     * @param resetTime the time when the rate limit window resets
     * @param remainingRequests the number of requests remaining in the current window
     * @param requestLimit the total request limit for the time window
     */
    public RateLimitExceededException(String message, Duration retryAfter, Instant resetTime, 
                                     int remainingRequests, int requestLimit) {
        super(message, null, 429, org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
        this.resetTime = resetTime;
        this.remainingRequests = remainingRequests;
        this.requestLimit = requestLimit;
    }
    
    /**
     * Factory method to create an exception from a Retry-After header value.
     * 
     * @param retryAfterSeconds the number of seconds to wait before retrying
     * @return a RateLimitExceededException with the specified retry duration
     */
    public static RateLimitExceededException withRetryAfter(int retryAfterSeconds) {
        Duration retryAfter = Duration.ofSeconds(retryAfterSeconds);
        Instant resetTime = Instant.now().plus(retryAfter);
        return new RateLimitExceededException(
            "Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds.",
            retryAfter, resetTime, 0, 0
        );
    }
    
    /**
     * Factory method to create an exception with detailed rate limit information.
     * 
     * @param remaining the number of requests remaining
     * @param limit the total request limit
     * @param resetTime the time when the rate limit resets
     * @return a RateLimitExceededException with detailed rate limit information
     */
    public static RateLimitExceededException withDetails(int remaining, int limit, Instant resetTime) {
        Duration retryAfter = Duration.between(Instant.now(), resetTime);
        return new RateLimitExceededException(
            String.format("Rate limit exceeded. %d/%d requests used. Reset at %s", 
                         remaining, limit, resetTime),
            retryAfter, resetTime, remaining, limit
        );
    }
    
    /**
     * Gets the duration to wait before retrying the request.
     * 
     * @return the retry-after duration, or null if not specified
     */
    public Duration getRetryAfter() {
        return retryAfter;
    }
    
    /**
     * Gets the time when the rate limit window resets.
     * 
     * @return the reset time, or null if not specified
     */
    public Instant getResetTime() {
        return resetTime;
    }
    
    /**
     * Gets the number of requests remaining in the current rate limit window.
     * 
     * @return the remaining requests, or 0 if not specified
     */
    public int getRemainingRequests() {
        return remainingRequests;
    }
    
    /**
     * Gets the total request limit for the time window.
     * 
     * @return the request limit, or 0 if not specified
     */
    public int getRequestLimit() {
        return requestLimit;
    }
    
    /**
     * Calculates the time in seconds until the rate limit resets.
     * 
     * @return the number of seconds to wait, or 0 if reset time is not available
     */
    public long getSecondsUntilReset() {
        if (resetTime == null) {
            return 0;
        }
        return Math.max(0, Duration.between(Instant.now(), resetTime).getSeconds());
    }
    
    /**
     * Checks if the rate limit window has been reset (i.e., if it's safe to retry).
     * 
     * @return true if the rate limit window has reset, false otherwise
     */
    public boolean isReset() {
        return resetTime != null && !Instant.now().isBefore(resetTime);
    }
    
    /**
     * Gets a suggested backoff duration for retrying the request.
     * This implements exponential backoff with jitter for better distribution of retries.
     * 
     * @param attemptNumber the current attempt number (1-based)
     * @return the suggested backoff duration
     */
    public Duration getSuggestedBackoff(int attemptNumber) {
        if (retryAfter != null) {
            // If server provided Retry-After, respect it
            return retryAfter;
        }
        
        // Otherwise use exponential backoff with jitter
        long baseDelay = (long) Math.pow(2, attemptNumber) * 1000; // Convert to milliseconds
        long jitter = (long) (Math.random() * 1000); // Add up to 1 second of jitter
        long totalDelay = Math.min(baseDelay + jitter, 60000); // Cap at 1 minute
        
        return Duration.ofMillis(totalDelay);
    }
    
    /**
     * Gets a user-friendly message with retry instructions.
     * 
     * @return a user-friendly error message
     */
    @Override
    public String getUserFriendlyMessage() {
        StringBuilder message = new StringBuilder("OpenProject API rate limit exceeded. ");
        
        if (retryAfter != null) {
            message.append("Please wait ").append(retryAfter.getSeconds()).append(" seconds before trying again.");
        } else if (resetTime != null) {
            long secondsUntilReset = getSecondsUntilReset();
            message.append("Please wait ").append(secondsUntilReset).append(" seconds before trying again.");
        } else {
            message.append("Please wait a few moments before retrying your request.");
        }
        
        if (requestLimit > 0) {
            message.append(" (Limit: ").append(requestLimit).append(" requests per time window)");
        }
        
        return message.toString();
    }
    
    /**
     * Gets a detailed error message suitable for logging.
     * 
     * @return a detailed error message for logging purposes
     */
    public String getDetailedMessage() {
        StringBuilder message = new StringBuilder(getMessage());
        
        if (retryAfter != null) {
            message.append(" [Retry-After: ").append(retryAfter.getSeconds()).append("s]");
        }
        
        if (resetTime != null) {
            message.append(" [Reset-Time: ").append(resetTime).append("]");
        }
        
        if (remainingRequests > 0 || requestLimit > 0) {
            message.append(" [Requests: ").append(remainingRequests)
                   .append("/").append(requestLimit).append("]");
        }
        
        return message.toString();
    }
}