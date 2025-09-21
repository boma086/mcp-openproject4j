package com.example.mcp.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an error occurs while communicating with the OpenProject API.
 * This exception handles various HTTP error responses from the OpenProject API including
 * authentication failures, resource not found, and server errors.
 * 
 * <p>This exception addresses the following requirements:</p>
 * <ul>
 *   <li><strong>NFR3: Reliability</strong> - Graceful handling of API failures</li>
 *   <li><strong>FR2: OpenProject API Integration</strong> - Handles API error responses (403, 404, 429)</li>
 * </ul>
 * 
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>HTTP 403 Forbidden - Invalid API key or insufficient permissions</li>
 *   <li>HTTP 404 Not Found - Resource does not exist</li>
 *   <li>HTTP 429 Too Many Requests - Rate limiting (use {@link RateLimitExceededException} instead)</li>
 *   <li>HTTP 5xx Server Errors - OpenProject server issues</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
public class OpenProjectApiException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final int statusCode;
    private final String errorDetails;
    
    /**
     * Constructs a new OpenProjectApiException with the specified detail message.
     * 
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public OpenProjectApiException(String message) {
        this(message, null, 500, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Constructs a new OpenProjectApiException with the specified detail message and HTTP status.
     * 
     * @param message the detail message
     * @param statusCode the HTTP status code
     * @param httpStatus the HTTP status enum
     */
    public OpenProjectApiException(String message, int statusCode, HttpStatus httpStatus) {
        this(message, null, statusCode, httpStatus);
    }
    
    /**
     * Constructs a new OpenProjectApiException with the specified detail message, cause, HTTP status.
     * 
     * @param message the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     * @param statusCode the HTTP status code
     * @param httpStatus the HTTP status enum
     */
    public OpenProjectApiException(String message, Throwable cause, int statusCode, HttpStatus httpStatus) {
        super(message, cause);
        this.statusCode = statusCode;
        this.httpStatus = httpStatus;
        this.errorDetails = null;
    }
    
    /**
     * Constructs a new OpenProjectApiException with the specified detail message, cause, HTTP status, and error details.
     * 
     * @param message the detail message
     * @param cause the cause
     * @param statusCode the HTTP status code
     * @param httpStatus the HTTP status enum
     * @param errorDetails additional error details from the API response
     */
    public OpenProjectApiException(String message, Throwable cause, int statusCode, HttpStatus httpStatus, String errorDetails) {
        super(message, cause);
        this.statusCode = statusCode;
        this.httpStatus = httpStatus;
        this.errorDetails = errorDetails;
    }
    
    /**
     * Factory method to create an exception for 403 Forbidden errors.
     * 
     * @param message the error message
     * @return an OpenProjectApiException for 403 Forbidden
     */
    public static OpenProjectApiException forbidden(String message) {
        return new OpenProjectApiException(message, 403, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Factory method to create an exception for 404 Not Found errors.
     * 
     * @param resource the resource that was not found
     * @return an OpenProjectApiException for 404 Not Found
     */
    public static OpenProjectApiException notFound(String resource) {
        return new OpenProjectApiException("Resource not found: " + resource, 404, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Factory method to create an exception for 5xx Server Error responses.
     * 
     * @param message the error message
     * @param statusCode the specific server error code
     * @return an OpenProjectApiException for server errors
     */
    public static OpenProjectApiException serverError(String message, int statusCode) {
        return new OpenProjectApiException(message, statusCode, HttpStatus.resolve(statusCode));
    }
    
    /**
     * Gets the HTTP status code associated with this API error.
     * 
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Gets the HTTP status enum associated with this API error.
     * 
     * @return the HTTP status
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * Gets additional error details from the API response, if available.
     * 
     * @return additional error details, or null if not available
     */
    public String getErrorDetails() {
        return errorDetails;
    }
    
    /**
     * Checks if this exception represents a client error (4xx status codes).
     * 
     * @return true if this is a client error (4xx), false otherwise
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Checks if this exception represents a server error (5xx status codes).
     * 
     * @return true if this is a server error (5xx), false otherwise
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    /**
     * Checks if this exception represents an authentication/authorization error.
     * 
     * @return true if this is a 401 or 403 error, false otherwise
     */
    public boolean isAuthenticationError() {
        return statusCode == 401 || statusCode == 403;
    }
    
    /**
     * Returns a user-friendly error message based on the HTTP status code.
     * 
     * @return a user-friendly error message
     */
    public String getUserFriendlyMessage() {
        if (httpStatus == null) {
            return "An unexpected error occurred while communicating with OpenProject API";
        }
        
        switch (httpStatus) {
            case UNAUTHORIZED:
                return "Authentication failed. Please check your API key.";
            case FORBIDDEN:
                return "Access denied. You don't have permission to perform this action.";
            case NOT_FOUND:
                return "The requested resource was not found in OpenProject.";
            case BAD_REQUEST:
                return "Invalid request. Please check your input data.";
            case TOO_MANY_REQUESTS:
                return "Too many requests. Please wait before trying again.";
            default:
                if (httpStatus.is5xxServerError()) {
                    return "OpenProject server is temporarily unavailable. Please try again later.";
                } else {
                    return "An error occurred while communicating with OpenProject API.";
                }
        }
    }
}