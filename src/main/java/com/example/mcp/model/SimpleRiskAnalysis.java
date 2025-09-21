package com.example.mcp.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Simple Risk Analysis Model for MCP Tools
 * MCP工具的简单风险分析模型
 * 
 * This is a simplified version of the RiskAnalysis model specifically designed
 * for the MCP tools to work with the existing codebase structure.
 * 
 * 这是RiskAnalysis模型的简化版本，专门设计用于MCP工具与现有代码库结构配合使用。
 */
public record SimpleRiskAnalysis(
    String projectId,
    LocalDate analysisDate,
    WorkPackage.RiskLevel riskLevel,
    int overdueTasks,
    List<RiskItem> riskItems,
    String recommendations
) {
    
    /**
     * Simple Risk Item record
     * 简单风险项目记录
     */
    public record RiskItem(
        String workPackageId,
        String subject,
        String description,
        String impact,
        LocalDate dueDate,
        String assignee
    ) {}
}