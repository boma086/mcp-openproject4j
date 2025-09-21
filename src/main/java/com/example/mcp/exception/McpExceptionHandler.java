package com.example.mcp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for MCP Server
 * MCP服务器的全局异常处理器
 * 
 * This global exception handler provides comprehensive error handling for the MCP server,
 * ensuring graceful handling of API failures, validation errors, and system exceptions.
 * It addresses reliability requirements (NFR3) and OpenProject API integration requirements (FR2).
 * 
 * 该全局异常处理器为MCP服务器提供全面的错误处理，确保优雅地处理API故障、验证错误和系统异常。
 * 它解决了可靠性要求（NFR3）和OpenProject API集成要求（FR2）。
 * 
 * <p>This implementation addresses the following requirements:</p>
 * <ul>
 *   <li><strong>NFR3: Reliability</strong> - Graceful handling of API failures with meaningful error messages</li>
 *   <li><strong>FR2: OpenProject API Integration</strong> - Comprehensive error handling for API communication issues</li>
 * </ul>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Custom exception handling for OpenProject API errors</li>
 *   <li>Rate limiting exception handling with retry information</li>
 *   <li>Validation error handling for MCP tool parameters</li>
 *   <li>Generic exception handling for unexpected errors</li>
 *   <li>Bilingual error messages (English/Chinese)</li>
 *   <li>Structured error response format</li>
 *   <li>Request correlation ID for debugging</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@RestControllerAdvice
public class McpExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(McpExceptionHandler.class);
    
    /**
     * Handles OpenProject API exceptions with appropriate HTTP status codes.
     * 处理OpenProject API异常，使用适当的HTTP状态码。
     * 
     * @param ex the OpenProjectApiException
     * @param request the current web request
     * @return ResponseEntity with error details
     */
    @ExceptionHandler(OpenProjectApiException.class)
    public ResponseEntity<ErrorResponse> handleOpenProjectApiException(
            OpenProjectApiException ex, WebRequest request) {
        
        logger.warn("OpenProject API error: {} | Status: {} | Path: {}", 
                   ex.getMessage(), ex.getStatusCode(), request.getDescription(false));
        
        ErrorResponse errorResponse = createErrorResponse(
            ex.getHttpStatus(),
            "OPENPROJECT_API_ERROR",
            ex.getUserFriendlyMessage(),
            request
        );
        
        // Add additional error details if available
        if (ex.getErrorDetails() != null) {
            errorResponse.addDetail("api_error_details", ex.getErrorDetails());
        }
        
        // Add error classification
        errorResponse.addDetail("error_classification", classifyApiError(ex));
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    
    /**
     * Handles rate limit exceeded exceptions with retry information.
     * 处理速率限制超出异常，包含重试信息。
     * 
     * @param ex the RateLimitExceededException
     * @param request the current web request
     * @return ResponseEntity with rate limit details
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex, WebRequest request) {
        
        logger.warn("Rate limit exceeded: {} | Reset: {} | Path: {}", 
                   ex.getDetailedMessage(), ex.getResetTime(), request.getDescription(false));
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS,
            "RATE_LIMIT_EXCEEDED",
            ex.getUserFriendlyMessage(),
            request
        );
        
        // Add rate limit specific details
        if (ex.getRetryAfter() != null) {
            errorResponse.addDetail("retry_after_seconds", ex.getRetryAfter().getSeconds());
        }
        
        if (ex.getResetTime() != null) {
            errorResponse.addDetail("reset_time", ex.getResetTime().toString());
            errorResponse.addDetail("seconds_until_reset", ex.getSecondsUntilReset());
        }
        
        if (ex.getRequestLimit() > 0) {
            errorResponse.addDetail("request_limit", ex.getRequestLimit());
            errorResponse.addDetail("remaining_requests", ex.getRemainingRequests());
        }
        
        // Add retry suggestion
        errorResponse.addDetail("suggested_backoff", 
                              ex.getSuggestedBackoff(1).getSeconds() + "s");
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * Handles validation exceptions for MCP tool parameters.
     * 处理MCP工具参数的验证异常。
     * 
     * @param ex the MethodArgumentNotValidException
     * @param request the current web request
     * @return ResponseEntity with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        logger.warn("Validation error: {} | Path: {}", 
                   ex.getMessage(), request.getDescription(false));
        
        // Extract validation error details
        Map<String, String> validationErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                fieldError -> fieldError.getField(),
                fieldError -> fieldError.getDefaultMessage() != null ? 
                              fieldError.getDefaultMessage() : "Invalid value"
            ));
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            "Invalid request parameters. Please check your input data.",
            request
        );
        
        errorResponse.addDetail("validation_errors", validationErrors);
        errorResponse.addDetail("error_count", validationErrors.size());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles constraint violation exceptions.
     * 处理约束违反异常。
     * 
     * @param ex the ConstraintViolationException
     * @param request the current web request
     * @return ResponseEntity with constraint violation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        logger.warn("Constraint violation: {} | Path: {}", 
                   ex.getMessage(), request.getDescription(false));
        
        // Extract constraint violation details
        Map<String, String> violations = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                violation -> violation.getMessage()
            ));
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "CONSTRAINT_VIOLATION",
            "Request violates one or more constraints. Please check your input data.",
            request
        );
        
        errorResponse.addDetail("constraint_violations", violations);
        errorResponse.addDetail("violation_count", violations.size());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles method argument type mismatch exceptions.
     * 处理方法参数类型不匹配异常。
     * 
     * @param ex the MethodArgumentTypeMismatchException
     * @param request the current web request
     * @return ResponseEntity with type mismatch details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        logger.warn("Type mismatch for parameter '{}': Expected {}, got {} | Path: {}", 
                   ex.getName(), ex.getRequiredType().getSimpleName(), 
                   ex.getValue(), request.getDescription(false));
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "TYPE_MISMATCH",
            String.format("Invalid parameter type for '%s'. Expected %s.", 
                         ex.getName(), ex.getRequiredType().getSimpleName()),
            request
        );
        
        errorResponse.addDetail("parameter_name", ex.getName());
        errorResponse.addDetail("expected_type", ex.getRequiredType().getSimpleName());
        errorResponse.addDetail("actual_value", ex.getValue() != null ? ex.getValue().toString() : "null");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles 404 Not Found exceptions.
     * 处理404未找到异常。
     * 
     * @param ex the NoHandlerFoundException
     * @param request the current web request
     * @return ResponseEntity with 404 error details
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoHandlerFoundException ex, WebRequest request) {
        
        logger.warn("Resource not found: {} {} | Path: {}", 
                   ex.getHttpMethod(), ex.getRequestURL(), request.getDescription(false));
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "The requested resource was not found.",
            request
        );
        
        errorResponse.addDetail("requested_url", ex.getRequestURL());
        errorResponse.addDetail("http_method", ex.getHttpMethod());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handles illegal argument exceptions.
     * 处理非法参数异常。
     * 
     * @param ex the IllegalArgumentException
     * @param request the current web request
     * @return ResponseEntity with illegal argument details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.warn("Illegal argument: {} | Path: {}", 
                   ex.getMessage(), request.getDescription(false));
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "ILLEGAL_ARGUMENT",
            "Invalid argument provided: " + ex.getMessage(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handles illegal state exceptions.
     * 处理非法状态异常。
     * 
     * @param ex the IllegalStateException
     * @param request the current web request
     * @return ResponseEntity with illegal state details
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        logger.error("Illegal state: {} | Path: {}", 
                    ex.getMessage(), request.getDescription(false));
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.CONFLICT,
            "ILLEGAL_STATE",
            "Operation cannot be performed due to current system state: " + ex.getMessage(),
            request
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handles generic exceptions as a fallback.
     * 处理通用异常作为后备。
     * 
     * @param ex the Exception
     * @param request the current web request
     * @return ResponseEntity with generic error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected error: {} | Path: {}", 
                    ex.getMessage(), request.getDescription(false), ex);
        
        ErrorResponse errorResponse = createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred while processing your request.",
            request
        );
        
        // Add debug information (avoid sensitive data)
        errorResponse.addDetail("error_type", ex.getClass().getSimpleName());
        if (ex.getCause() != null) {
            errorResponse.addDetail("cause_type", ex.getCause().getClass().getSimpleName());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Creates a standardized error response.
     * 创建标准化的错误响应。
     * 
     * @param status the HTTP status
     * @param errorCode the error code
     * @param message the error message
     * @param request the current web request
     * @return ErrorResponse object
     */
    private ErrorResponse createErrorResponse(HttpStatus status, String errorCode, 
                                          String message, WebRequest request) {
        return new ErrorResponse(
            status.value(),
            errorCode,
            message,
            request.getDescription(false),
            LocalDateTime.now()
        );
    }
    
    /**
     * Classifies API errors for better error handling and reporting.
     * 分类API错误以便更好地错误处理和报告。
     * 
     * @param ex the OpenProjectApiException
     * @return error classification string
     */
    private String classifyApiError(OpenProjectApiException ex) {
        if (ex.isAuthenticationError()) {
            return "AUTHENTICATION_ERROR";
        } else if (ex.isClientError()) {
            return "CLIENT_ERROR";
        } else if (ex.isServerError()) {
            return "SERVER_ERROR";
        } else {
            return "UNKNOWN_ERROR";
        }
    }
    
    /**
     * Standardized error response structure.
     * 标准化错误响应结构。
     * 
     * This inner class provides a consistent format for all error responses,
     * including bilingual messages and detailed error information.
     * 
     * 该内部类为所有错误响应提供一致的格式，包括双语消息和详细的错误信息。
     */
    public static class ErrorResponse {
        private final int status;
        private final String errorCode;
        private final String message;
        private final String chineseMessage;
        private final String path;
        private final LocalDateTime timestamp;
        private final String correlationId;
        private final Map<String, Object> details = new HashMap<>();
        
        public ErrorResponse(int status, String errorCode, String message, String path, LocalDateTime timestamp) {
            this.status = status;
            this.errorCode = errorCode;
            this.message = message;
            this.chineseMessage = generateChineseMessage(errorCode, message);
            this.path = path;
            this.timestamp = timestamp;
            this.correlationId = java.util.UUID.randomUUID().toString();
        }
        
        /**
         * Generates Chinese error messages based on error codes.
         * 根据错误码生成中文错误消息。
         * 
         * @param errorCode the error code
         * @param englishMessage the English message
         * @return Chinese error message
         */
        private String generateChineseMessage(String errorCode, String englishMessage) {
            return switch (errorCode) {
                case "OPENPROJECT_API_ERROR" -> "OpenProject API错误：" + englishMessage;
                case "RATE_LIMIT_EXCEEDED" -> "请求频率超限：" + englishMessage;
                case "VALIDATION_ERROR" -> "验证错误：请检查您的输入数据。";
                case "CONSTRAINT_VIOLATION" -> "约束违反：请求违反了一个或多个约束条件。";
                case "TYPE_MISMATCH" -> "类型不匹配：参数类型不正确。";
                case "RESOURCE_NOT_FOUND" -> "资源未找到：请求的资源不存在。";
                case "ILLEGAL_ARGUMENT" -> "非法参数：" + englishMessage;
                case "ILLEGAL_STATE" -> "非法状态：由于当前系统状态无法执行操作。";
                case "INTERNAL_SERVER_ERROR" -> "内部服务器错误：处理请求时发生意外错误。";
                default -> "系统错误：" + englishMessage;
            };
        }
        
        /**
         * Adds additional detail information to the error response.
         * 向错误响应添加详细信息。
         * 
         * @param key the detail key
         * @param value the detail value
         */
        public void addDetail(String key, Object value) {
            this.details.put(key, value);
        }
        
        // Getters
        public int getStatus() { return status; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public String getChineseMessage() { return chineseMessage; }
        public String getPath() { return path; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getCorrelationId() { return correlationId; }
        public Map<String, Object> getDetails() { return details; }
    }
}