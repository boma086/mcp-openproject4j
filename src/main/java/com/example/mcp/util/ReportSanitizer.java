package com.example.mcp.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Report Sanitizer Utility for Data Processing and Sanitization
 * 报告清理工具类，用于数据处理和清理
 * 
 * This utility class provides methods for sanitizing report data to ensure security
 * and privacy compliance. It removes sensitive information, validates input formats,
 * and standardizes output for consistent reporting.
 * 
 * 该工具类提供清理报告数据的方法，以确保安全和隐私合规性。它会移除敏感信息、
 * 验证输入格式，并标准化输出以实现一致的报告。
 * 
 * <p>This implementation addresses the following requirements:</p>
 * <ul>
 *   <li><strong>NFR2: Security</strong> - Secure handling of sensitive data in reports</li>
 *   <li><strong>FR3: Weekly Report Generation</strong> - Clean report output formatting</li>
 *   <li><strong>FR4: Risk Analysis Tool</strong> - Sanitized risk data presentation</li>
 *   <li><strong>FR5: Completion Analysis Tool</strong> - Safe completion metrics display</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
public class ReportSanitizer {
    
    // Patterns for detecting sensitive information
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(api[_-]?key|token|secret)[\\s]*[:=][\\s]*([\"']?)[a-zA-Z0-9+/=]{10,}\\1", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(password|pwd)[\\s]*[:=][\\s]*([\"']?)[^\\s\"']{4,}\\1", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?1[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?|ftp)://[^\\s/$.?#].[^\\s]*", Pattern.CASE_INSENSITIVE);
    
    // Patterns for HTML/Script injection prevention
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:[^\\s]*", Pattern.CASE_INSENSITIVE);
    
    // Patterns for data validation
    private static final Pattern PROJECT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,50}$");
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,50}$");
    
    /**
     * Sanitizes report content by removing sensitive information and malicious content
     * 清理报告内容，移除敏感信息和恶意内容
     * 
     * @param content The raw report content to sanitize
     * @return Sanitized content safe for display
     */
    public static String sanitizeReportContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        String sanitized = content;
        
        // Remove sensitive authentication information
        sanitized = removeSensitiveAuthenticationData(sanitized);
        
        // Remove personal identifiable information (PII)
        sanitized = removePersonalInformation(sanitized);
        
        // Prevent XSS and script injection
        sanitized = preventScriptInjection(sanitized);
        
        // Clean up excessive whitespace
        sanitized = normalizeWhitespace(sanitized);
        
        // Remove potentially dangerous URLs
        sanitized = sanitizeUrls(sanitized);
        
        return sanitized;
    }
    
    /**
     * Sanitizes a project ID to ensure it meets security requirements
     * 清理项目ID以确保其符合安全要求
     * 
     * @param projectId The project ID to validate and sanitize
     * @return Sanitized project ID or null if invalid
     */
    public static String sanitizeProjectId(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return null;
        }
        
        String sanitized = projectId.trim();
        
        // Remove any potentially dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'&;]", "");
        
        // Validate against allowed pattern
        if (!PROJECT_ID_PATTERN.matcher(sanitized).matches()) {
            return null;
        }
        
        return sanitized;
    }
    
    /**
     * Sanitizes task IDs for use in reports
     * 清理任务ID以便在报告中使用
     * 
     * @param taskId The task ID to sanitize
     * @return Sanitized task ID or null if invalid
     */
    public static String sanitizeTaskId(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return null;
        }
        
        String sanitized = taskId.trim();
        
        // Remove any potentially dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'&;]", "");
        
        // Validate against allowed pattern
        if (!TASK_ID_PATTERN.matcher(sanitized).matches()) {
            return null;
        }
        
        return sanitized;
    }
    
    /**
     * Sanitizes numeric values to prevent injection attacks
     * 清理数值以防止注入攻击
     * 
     * @param numericString The numeric string to sanitize
     * @return Sanitized numeric string or null if invalid
     */
    public static String sanitizeNumericValue(String numericString) {
        if (numericString == null || numericString.trim().isEmpty()) {
            return null;
        }
        
        String sanitized = numericString.trim();
        
        // Remove any non-numeric characters except decimal point and minus sign
        sanitized = sanitized.replaceAll("[^0-9.-]", "");
        
        // Validate that it's a proper numeric format
        if (!sanitized.matches("^-?\\d+(\\.\\d+)?$") && !sanitized.matches("^\\d*\\.\\d+$")) {
            return null;
        }
        
        return sanitized;
    }
    
    /**
     * Sanitizes user-generated text for safe display in reports
     * 清理用户生成的文本以便在报告中安全显示
     * 
     * @param userText The user-generated text to sanitize
     * @param maxLength Maximum allowed length for the text
     * @return Sanitized text safe for display
     */
    public static String sanitizeUserText(String userText, int maxLength) {
        if (userText == null) {
            return "";
        }
        
        String sanitized = userText.trim();
        
        // Truncate to maximum length
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength) + "...";
        }
        
        // Remove HTML tags and scripts
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Escape special characters
        sanitized = sanitized.replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#39;")
                           .replace("&", "&amp;");
        
        return sanitized;
    }
    
    /**
     * Validates and sanitizes API response data for safe processing
     * 验证并清理API响应数据以便安全处理
     * 
     * @param responseData The raw API response data
     * @return Sanitized response data
     */
    public static String sanitizeApiResponse(String responseData) {
        if (responseData == null) {
            return "";
        }
        
        String sanitized = responseData;
        
        // Remove sensitive headers or tokens that might be in the response
        sanitized = sanitized.replaceAll("(Authorization|Bearer|Token)[\\s]*:[\\s]*[^\\n\\r]*", "***REDACTED***");
        
        // Remove internal server paths that could expose system information
        sanitized = sanitized.replaceAll("/(?:home|usr|opt|var|tmp)/[^\\s]*", "***PATH***");
        
        // Remove stack traces and error details that could expose system internals
        sanitized = sanitized.replaceAll("(Exception|Error|StackTrace)[^\\n\\r]*", "***ERROR***");
        
        return sanitized;
    }
    
    /**
     * Removes sensitive authentication data from content
     * 从内容中移除敏感认证数据
     */
    private static String removeSensitiveAuthenticationData(String content) {
        String sanitized = content;
        
        // Remove API keys and tokens
        Matcher apiKeyMatcher = API_KEY_PATTERN.matcher(sanitized);
        sanitized = apiKeyMatcher.replaceAll("$1=***REDACTED***");
        
        // Remove passwords
        Matcher passwordMatcher = PASSWORD_PATTERN.matcher(sanitized);
        sanitized = passwordMatcher.replaceAll("$1=***REDACTED***");
        
        return sanitized;
    }
    
    /**
     * Removes personal identifiable information (PII) from content
     * 从内容中移除个人身份信息（PII）
     */
    private static String removePersonalInformation(String content) {
        String sanitized = content;
        
        // Remove email addresses (partially mask them)
        Matcher emailMatcher = EMAIL_PATTERN.matcher(sanitized);
        sanitized = emailMatcher.replaceAll("***@***.***");
        
        // Remove phone numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(sanitized);
        sanitized = phoneMatcher.replaceAll("***-***-****");
        
        return sanitized;
    }
    
    /**
     * Prevents script injection and XSS attacks
     * 防止脚本注入和XSS攻击
     */
    private static String preventScriptInjection(String content) {
        String sanitized = content;
        
        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove script blocks
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove javascript: protocol links
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("***REDACTED***");
        
        return sanitized;
    }
    
    /**
     * Normalizes whitespace in content for consistent formatting
     * 标准化内容中的空白以实现一致的格式
     */
    private static String normalizeWhitespace(String content) {
        // Replace multiple whitespace characters with a single space
        String normalized = content.replaceAll("\\s+", " ");
        
        // Trim leading and trailing whitespace
        normalized = normalized.trim();
        
        return normalized;
    }
    
    /**
     * Sanitizes URLs to prevent malicious links
     * 清理URL以防止恶意链接
     */
    private static String sanitizeUrls(String content) {
        Matcher urlMatcher = URL_PATTERN.matcher(content);
        
        return urlMatcher.replaceAll("***URL***");
    }
    
    /**
     * Validates that a string contains only safe characters for report generation
     * 验证字符串只包含用于报告生成的安全字符
     * 
     * @param input The string to validate
     * @return true if the string is safe, false otherwise
     */
    public static boolean isSafeForReportGeneration(String input) {
        if (input == null) {
            return false;
        }
        
        // Check for potentially dangerous characters
        return !input.matches(".*[<>\"'&;].*") && 
               !input.matches(".*(?i)(javascript|script|iframe|object|embed).*") &&
               !input.matches(".*(?i)(api[_-]?key|token|secret|password).*");
    }
    
    /**
     * Generates a safe report title by sanitizing the input
     * 通过清理输入生成安全的报告标题
     * 
     * @param title The raw title input
     * @return Safe and sanitized title
     */
    public static String generateSafeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "Untitled Report";
        }
        
        String sanitized = title.trim();
        
        // Remove HTML and script tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove special characters that could cause issues
        sanitized = sanitized.replaceAll("[<>\"'&;]", "");
        
        // Limit length
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100) + "...";
        }
        
        return sanitized.isEmpty() ? "Untitled Report" : sanitized;
    }
}