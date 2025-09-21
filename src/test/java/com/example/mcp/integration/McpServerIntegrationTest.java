package com.example.mcp.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify MCP server endpoints are working
 * 验证MCP服务器端点是否正常工作的集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class McpServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testMcpServerEndpoint_Accessible() {
        // Test MCP server endpoint is accessible
        // 测试MCP服务器端点可访问
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/mcp", String.class);
        
        // Should be accessible (may return 405 if GET not allowed, but not 404)
        // 应该可访问（如果GET方法不允许可能返回405，但不应该是404）
        assertNotEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testHealthEndpoint_Accessible() {
        // Test health endpoint
        // 测试健康检查端点
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health", String.class);
        
        // Health endpoint should be accessible
        // 健康检查端点应该可访问
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testInfoEndpoint_Accessible() {
        // Test info endpoint
        // 测试信息端点
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/info", String.class);
        
        // Info endpoint should be accessible
        // 信息端点应该可访问
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }
}