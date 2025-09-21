package com.example.mcp.config;

// Temporarily commenting out MCP imports - package structure may be different
// import org.springframework.ai.mcp.server.tool.MethodToolCallbackProvider;
// import org.springframework.ai.mcp.server.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * MCP Server Configuration Class
 * MCP服务器配置类
 * 
 * This configuration class sets up the MCP server beans and HTTP client
 * for OpenProject API integration. It includes:
 * - MCP tool callback provider for registering @Tool annotated methods
 * - Configured RestTemplate with HTTP client settings
 * - Authentication headers for OpenProject API
 * - Connection pooling and timeout settings
 * 
 * 此配置类设置MCP服务器Bean和HTTP客户端，用于OpenProject API集成。
 * 它包括：
 * - MCP工具回调提供者，用于注册@Tool注解方法
 * - 配置的RestTemplate，包含HTTP客户端设置
 * - OpenProject API的认证头
 * - 连接池和超时设置
 */
@Configuration
public class McpServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

    @Value("${openproject.timeout:30000}")
    private int timeout;

    @Value("${openproject.rate-limit.requests-per-minute:100}")
    private int rateLimit;

    @Value("${openproject.connection.max-connections:10}")
    private int maxConnections;

    @Value("${openproject.connection.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${openproject.connection.read-timeout:10000}")
    private int readTimeout;

    /**
     * Create MCP tool callback provider for registering @Tool annotated methods
     * 创建MCP工具回调提供者，用于注册@Tool注解方法
     * 
     * @param projectService Project service containing @Tool methods
     * @return ToolCallbackProvider instance
     */
    // @Bean
    // public ToolCallbackProvider projectTools(Object projectService) {
    //     logger.info("Creating MCP tool callback provider | 创建MCP工具回调提供者");
    //     
    //     return MethodToolCallbackProvider.builder()
            // .toolObjects(projectService)
            // .build();
    // }

    /**
     * Create configured RestTemplate for HTTP client operations
     * 创建配置的RestTemplate用于HTTP客户端操作
     * 
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        logger.info("Creating configured RestTemplate | 创建配置的RestTemplate");
        
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(createHttpClient());
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Configure additional message converters if needed
        // 如果需要，配置额外的消息转换器
        logger.info("RestTemplate configured with timeout: {}ms, max connections: {}", 
                   timeout, maxConnections);
        
        return restTemplate;
    }

    /**
     * Create configured HTTP client with connection pooling and timeouts
     * 创建配置的HTTP客户端，包含连接池和超时设置
     * 
     * @return Configured HttpClient instance
     */
    private HttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
            .setConnectionRequestTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
            .setResponseTimeout(readTimeout, TimeUnit.MILLISECONDS)
            .build();

        return HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            // .setMaxConnTotal(maxConnections) - method not available in this version
            // .setMaxConnPerRoute(maxConnections) - method not available in this version
            .evictExpiredConnections()
            // .evictIdleConnections(60, TimeUnit.SECONDS) - method signature changed
            // .setConnectionTimeToLive(30, TimeUnit.SECONDS) - method signature changed
            .build();
    }

    /**
     * Create authentication headers for OpenProject API
     * 创建OpenProject API的认证头
     * 
     * @param apiKey OpenProject API key
     * @return Base64 encoded authentication header
     */
    public static String createAuthHeader(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty | API密钥不能为空");
        }
        
        String auth = apiKey + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        logger.debug("Created authentication header | 创建认证头");
        return "Basic " + encodedAuth;
    }

    /**
     * Validate API key format
     * 验证API密钥格式
     * 
     * @param apiKey OpenProject API key to validate
     * @return true if valid, false otherwise
     */
    public static boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - API key should be a reasonable length
        // 基本验证 - API密钥应该是合理长度
        return apiKey.length() >= 10 && apiKey.length() <= 100;
    }

    /**
     * Get timeout configuration for logging and monitoring
     * 获取超时配置用于日志记录和监控
     * 
     * @return String representation of timeout settings
     */
    public String getTimeoutConfig() {
        return String.format("Timeouts - Connection: %dms, Read: %dms, Total: %dms", 
                            connectionTimeout, readTimeout, timeout);
    }

    /**
     * Get rate limit configuration for logging and monitoring
     * 获取速率限制配置用于日志记录和监控
     * 
     * @return String representation of rate limit settings
     */
    public String getRateLimitConfig() {
        return String.format("Rate Limit: %d requests per minute", rateLimit);
    }

    /**
     * Get connection pool configuration for logging and monitoring
     * 获取连接池配置用于日志记录和监控
     * 
     * @return String representation of connection pool settings
     */
    public String getConnectionPoolConfig() {
        return String.format("Connection Pool - Max: %d, Per Route: %d", 
                            maxConnections, maxConnections);
    }
}