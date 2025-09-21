package com.example.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Temporarily commenting out MCP imports - package structure may be different
// import org.springframework.ai.mcp.server.McpServerProperties;
// import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import com.example.mcp.config.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenProject MCP Server Main Application Class
 * OpenProject MCP服务器主应用类
 * 
 * This is the main entry point for the Spring Boot application that implements
 * the MCP server for OpenProject integration. The application exposes three main tools:
 * - generateWeeklyReport: Generate weekly project reports
 * - analyzeRisks: Analyze project risks based on overdue tasks
 * - calculateCompletion: Calculate project completion percentage
 * 
 * 这是Spring Boot应用程序的主入口点，实现了OpenProject集成的MCP服务器。
 * 应用程序暴露三个主要工具：
 * - generateWeeklyReport: 生成项目周报
 * - analyzeRisks: 基于逾期任务分析项目风险
 * - calculateCompletion: 计算项目完成百分比
 */
@SpringBootApplication
// @Import(McpServerAutoConfiguration.class)
@Import(RetryConfig.class)
public class OpenProjectMcpApplication {

    private static final Logger logger = LoggerFactory.getLogger(OpenProjectMcpApplication.class);

    // @Autowired
    // private McpServerProperties mcpServerProperties;

    /**
     * Main entry point for the Spring Boot application
     * Spring Boot应用程序的主入口点
     * 
     * @param args Command line arguments | 命令行参数
     */
    public static void main(String[] args) {
        logger.info("Starting OpenProject MCP Server | 启动OpenProject MCP服务器");
        SpringApplication.run(OpenProjectMcpApplication.class, args);
    }

    /**
     * Application runner for MCP server initialization and validation
     * MCP服务器初始化和验证的应用程序运行器
     * 
     * @return ApplicationRunner | 应用程序运行器
     */
    // @Bean
    // public ApplicationRunner mcpServerInitializer() {
    //     return args -> {
    //         logger.info("OpenProject MCP Server initialized successfully | OpenProject MCP服务器初始化成功");
    //         logger.info("MCP Server Configuration | MCP服务器配置:");
    //         logger.info("  - Name: {}", mcpServerProperties.getName());
    //         logger.info("  - Version: {}", mcpServerProperties.getVersion());
    //         logger.info("  - Type: {}", mcpServerProperties.getType());
    //         logger.info("  - Base URL: {}", mcpServerProperties.getBaseUrl());
    //         logger.info("  - Enabled: {}", mcpServerProperties.isEnabled());
    //         logger.info("  - Capabilities: {}", mcpServerProperties.getCapabilities());
    //         
    //         // Validate MCP server configuration
    //         validateMcpServerConfiguration();
    //         
    //         logger.info("MCP Server is ready to accept connections | MCP服务器已准备好接受连接");
    //         logger.info("Available tools | 可用工具:");
    //         logger.info("  - generateWeeklyReport: Generate weekly project reports | 生成项目周报");
    //         logger.info("  - analyzeRisks: Analyze project risks | 分析项目风险");
    //         logger.info("  - calculateCompletion: Calculate project completion | 计算项目完成度");
    //     };
    // }

    /**
     * Validate MCP server configuration
     * 验证MCP服务器配置
     */
    // private void validateMcpServerConfiguration() {
    //     if (!mcpServerProperties.isEnabled()) {
    //         logger.warn("MCP Server is disabled | MCP服务器已禁用");
    //         return;
    //     }

    //     if (mcpServerProperties.getBaseUrl() == null || mcpServerProperties.getBaseUrl().isBlank()) {
    //         throw new IllegalStateException("MCP Server base URL is not configured | MCP服务器基础URL未配置");
    //     }

    //     if (!mcpServerProperties.getCapabilities().isTool()) {
    //         throw new IllegalStateException("MCP Server tool capability is not enabled | MCP服务器工具功能未启用");
    //     }

    //     logger.info("MCP Server configuration validation passed | MCP服务器配置验证通过");
    // }

    /**
     * Shutdown hook for graceful shutdown
     * 优雅关闭的关闭钩子
     */
    @Bean
    public ApplicationRunner shutdownHook() {
        return args -> {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("OpenProject MCP Server shutting down gracefully | OpenProject MCP服务器正在优雅关闭");
            }));
        };
    }
}