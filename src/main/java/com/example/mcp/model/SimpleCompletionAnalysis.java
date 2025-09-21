package com.example.mcp.model;

import java.time.LocalDate;

/**
 * Simple Completion Analysis Model for MCP Tools
 * MCP工具的简单完成度分析模型
 * 
 * This is a simplified version of the CompletionAnalysis model specifically designed
 * for the MCP tools to work with the existing codebase structure.
 * 
 * 这是CompletionAnalysis模型的简化版本，专门设计用于MCP工具与现有代码库结构配合使用。
 */
public record SimpleCompletionAnalysis(
    String projectId,
    LocalDate analysisDate,
    double completionRate,
    int totalTasks,
    int completedTasks,
    String progressSummary
) {}