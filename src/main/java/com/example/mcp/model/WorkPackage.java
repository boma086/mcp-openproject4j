package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Work Package Model for OpenProject Data Representation
 * OpenProject数据表示的工作包模型
 * 
 * This record represents a work package from OpenProject API v3 with all necessary fields
 * for the three reporting tools: weekly reports, risk analysis, and completion analysis.
 * 
 * 该记录表示来自OpenProject API v3的工作包，包含三个报告工具所需的所有字段：
 * 周报、风险分析和完成度分析。
 * 
 * Key features:
 * - Immutable record implementation for thread safety
 * - Comprehensive field coverage for OpenProject API v3
 * - Static factory methods for JSON parsing
 * - Utility methods for business logic calculations
 * - Support for both basic and nested OpenProject API responses
 * 
 * 主要特性：
 * - 不可变记录实现，确保线程安全
 * - 全面覆盖OpenProject API v3字段
 * - 用于JSON解析的静态工厂方法
 * - 业务逻辑计算的实用方法
 * - 支持基本和嵌套的OpenProject API响应
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkPackage(
    
    /**
     * Unique identifier for the work package
     * 工作包的唯一标识符
     */
    @JsonProperty("id")
    String id,
    
    /**
     * Subject/title of the work package
     * 工作包的主题/标题
     */
    @JsonProperty("subject")
    String subject,
    
    /**
     * Detailed description of the work package
     * 工作包的详细描述
     */
    @JsonProperty("description")
    String description,
    
    /**
     * Current status of the work package
     * 工作包的当前状态
     */
    @JsonProperty("status")
    WorkPackageStatus status,
    
    /**
     * Type/category of the work package
     * 工作包的类型/类别
     */
    @JsonProperty("type")
    WorkPackageType type,
    
    /**
     * Priority level of the work package
     * 工作包的优先级
     */
    @JsonProperty("priority")
    WorkPackagePriority priority,
    
    /**
     * Due date for the work package
     * 工作包的截止日期
     */
    @JsonProperty("dueDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dueDate,
    
    /**
     * Start date for the work package
     * 工作包的开始日期
     */
    @JsonProperty("startDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,
    
    /**
     * Date when the work package was created
     * 工作包创建日期
     */
    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime createdAt,
    
    /**
     * Date when the work package was last updated
     * 工作包最后更新日期
     */
    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    LocalDateTime updatedAt,
    
    /**
     * Assignee information for the work package
     * 工作包的分配者信息
     */
    @JsonProperty("assignee")
    Assignee assignee,
    
    /**
     * Percentage complete (0-100)
     * 完成百分比（0-100）
     */
    @JsonProperty("percentageDone")
    Integer percentageDone,
    
    /**
     * Estimated time in hours
     * 预估时间（小时）
     */
    @JsonProperty("estimatedTime")
    Double estimatedTime,
    
    /**
     * Spent time in hours
     * 已用时间（小时）
     */
    @JsonProperty("spentTime")
    Double spentTime,
    
    /**
     * Project identifier this work package belongs to
     * 此工作包所属的项目标识符
     */
    @JsonProperty("_links")
    Links links,
    
    /**
     * Additional custom fields
     * 额外的自定义字段
     */
    @JsonProperty("customFields")
    List<CustomField> customFields
    
) {
    
    /**
     * Static factory method to create WorkPackage from JSON node
     * 从JSON节点创建WorkPackage的静态工厂方法
     * 
     * @param node JSON node from OpenProject API response
     * @return WorkPackage instance
     */
    public static WorkPackage fromJson(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            throw new IllegalArgumentException("JSON node cannot be null or missing");
        }
        
        ObjectMapper mapper = new ObjectMapper();
        
        // Extract nested status information
        // 提取嵌套的状态信息
        WorkPackageStatus status = extractStatus(node.path("status"));
        
        // Extract nested type information
        // 提取嵌套的类型信息
        WorkPackageType type = extractType(node.path("type"));
        
        // Extract nested priority information
        // 提取嵌套的优先级信息
        WorkPackagePriority priority = extractPriority(node.path("priority"));
        
        // Extract assignee information
        // 提取分配者信息
        Assignee assignee = extractAssignee(node.path("assignee"));
        
        // Extract links information
        // 提取链接信息
        Links links = extractLinks(node.path("_links"));
        
        // Extract custom fields
        // 提取自定义字段
        List<CustomField> customFields = extractCustomFields(node.path("customFields"));
        
        return new WorkPackage(
            extractString(node, "id"),
            extractString(node, "subject"),
            extractString(node, "description"),
            status,
            type,
            priority,
            parseLocalDate(node, "dueDate"),
            parseLocalDate(node, "startDate"),
            parseLocalDateTime(node, "createdAt"),
            parseLocalDateTime(node, "updatedAt"),
            assignee,
            extractInteger(node, "percentageDone"),
            extractDouble(node, "estimatedTime"),
            extractDouble(node, "spentTime"),
            links,
            customFields
        );
    }
    
    /**
     * Business logic method to check if work package is overdue
     * 业务逻辑方法：检查工作包是否逾期
     * 
     * @return true if due date is in the past and status is not closed/completed
     */
    public boolean isOverdue() {
        if (dueDate == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        boolean isDueInPast = dueDate.isBefore(today);
        boolean isNotClosed = status != null && !status.isClosedStatus();
        
        return isDueInPast && isNotClosed;
    }
    
    /**
     * Business logic method to check if work package is completed
     * 业务逻辑方法：检查工作包是否已完成
     * 
     * @return true if percentageDone is 100 or status indicates completion
     */
    public boolean isCompleted() {
        boolean isPercentageComplete = percentageDone != null && percentageDone >= 100;
        boolean isStatusComplete = status != null && status.isCompleted();
        
        return isPercentageComplete || isStatusComplete;
    }
    
    /**
     * Business logic method to check if work package was recently updated
     * 业务逻辑方法：检查工作包是否最近更新过
     * 
     * @param days number of days to consider as "recent"
     * @return true if updated within the specified number of days
     */
    public boolean isRecentlyUpdated(int days) {
        if (updatedAt == null) {
            return false;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return updatedAt.isAfter(cutoff);
    }
    
    /**
     * Business logic method to calculate risk level based on due date and status
     * 业务逻辑方法：基于截止日期和状态计算风险级别
     * 
     * @return RiskLevel enum indicating the risk level
     */
    public RiskLevel calculateRiskLevel() {
        if (isOverdue()) {
            return RiskLevel.HIGH;
        }
        
        if (dueDate == null) {
            return RiskLevel.MEDIUM;
        }
        
        LocalDate today = LocalDate.now();
        long daysUntilDue = today.until(dueDate).getDays();
        
        if (daysUntilDue <= 3) {
            return RiskLevel.HIGH;
        } else if (daysUntilDue <= 7) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }
    
    /**
     * Business logic method to get days until due date
     * 业务逻辑方法：获取距离截止日期的天数
     * 
     * @return number of days until due (negative if overdue)
     */
    public long getDaysUntilDue() {
        if (dueDate == null) {
            return Long.MAX_VALUE;
        }
        
        LocalDate today = LocalDate.now();
        return today.until(dueDate).getDays();
    }
    
    /**
     * Business logic method to check if work package is in progress
     * 业务逻辑方法：检查工作包是否正在进行中
     * 
     * @return true if status indicates work is in progress
     */
    public boolean isInProgress() {
        return status != null && status.isInProgress();
    }
    
    /**
     * Business logic method to get progress description
     * 业务逻辑方法：获取进度描述
     * 
     * @return Human-readable progress description
     */
    public String getProgressDescription() {
        if (percentageDone == null) {
            return status != null ? status.name() : "Unknown";
        }
        
        return String.format("%d%% %s", percentageDone, 
            isCompleted() ? "Completed" : "In Progress");
    }
    
    // Private helper methods for JSON parsing
    // JSON解析的私有辅助方法
    
    private static WorkPackageStatus extractStatus(JsonNode statusNode) {
        if (statusNode.isMissingNode() || statusNode.isNull()) {
            return null;
        }
        return new WorkPackageStatus(
            extractString(statusNode, "id"),
            extractString(statusNode, "name"),
            extractBoolean(statusNode, "isClosed"),
            extractBoolean(statusNode, "isDefault"),
            extractString(statusNode, "position")
        );
    }
    
    private static WorkPackageType extractType(JsonNode typeNode) {
        if (typeNode.isMissingNode() || typeNode.isNull()) {
            return null;
        }
        return new WorkPackageType(
            extractString(typeNode, "id"),
            extractString(typeNode, "name"),
            extractBoolean(typeNode, "isDefault"),
            extractString(typeNode, "color"),
            extractString(typeNode, "position")
        );
    }
    
    private static WorkPackagePriority extractPriority(JsonNode priorityNode) {
        if (priorityNode.isMissingNode() || priorityNode.isNull()) {
            return null;
        }
        return new WorkPackagePriority(
            extractString(priorityNode, "id"),
            extractString(priorityNode, "name"),
            extractBoolean(priorityNode, "isDefault"),
            extractString(priorityNode, "position")
        );
    }
    
    private static Assignee extractAssignee(JsonNode assigneeNode) {
        if (assigneeNode.isMissingNode() || assigneeNode.isNull()) {
            return null;
        }
        return new Assignee(
            extractString(assigneeNode, "id"),
            extractString(assigneeNode, "name"),
            extractString(assigneeNode, "login"),
            extractString(assigneeNode, "email")
        );
    }
    
    private static Links extractLinks(JsonNode linksNode) {
        if (linksNode.isMissingNode() || linksNode.isNull()) {
            return null;
        }
        return new Links(
            extractString(linksNode.path("project"), "href"),
            extractString(linksNode.path("assignee"), "href"),
            extractString(linksNode.path("status"), "href"),
            extractString(linksNode.path("type"), "href"),
            extractString(linksNode.path("priority"), "href")
        );
    }
    
    private static List<CustomField> extractCustomFields(JsonNode customFieldsNode) {
        if (customFieldsNode.isMissingNode() || !customFieldsNode.isArray()) {
            return List.of();
        }
        
        return new ObjectMapper().convertValue(customFieldsNode, 
            new com.fasterxml.jackson.core.type.TypeReference<List<CustomField>>() {});
    }
    
    private static String extractString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asText();
    }
    
    private static Integer extractInteger(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asInt();
    }
    
    private static Double extractDouble(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asDouble();
    }
    
    private static Boolean extractBoolean(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asBoolean();
    }
    
    private static LocalDate parseLocalDate(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        
        try {
            return LocalDate.parse(fieldNode.asText());
        } catch (Exception e) {
            return null;
        }
    }
    
    private static LocalDateTime parseLocalDateTime(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(fieldNode.asText(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
    
    // Nested record classes for complex fields
    // 复杂字段的嵌套记录类
    
    /**
     * Status information for work package
     * 工作包状态信息
     */
    public record WorkPackageStatus(
        String id,
        String name,
        Boolean isClosed,
        Boolean isDefault,
        String position
    ) {
        public boolean isClosedStatus() {
            return Boolean.TRUE.equals(isClosed);
        }
        
        public boolean isCompleted() {
            return isClosedStatus() || "closed".equalsIgnoreCase(name);
        }
        
        public boolean isInProgress() {
            return !isClosedStatus() && ("in progress".equalsIgnoreCase(name) || 
                                  "working".equalsIgnoreCase(name));
        }
    }
    
    /**
     * Type information for work package
     * 工作包类型信息
     */
    public record WorkPackageType(
        String id,
        String name,
        Boolean isDefault,
        String color,
        String position
    ) {}
    
    /**
     * Priority information for work package
     * 工作包优先级信息
     */
    public record WorkPackagePriority(
        String id,
        String name,
        Boolean isDefault,
        String position
    ) {}
    
    /**
     * Assignee information for work package
     * 工作包分配者信息
     */
    public record Assignee(
        String id,
        String name,
        String login,
        String email
    ) {}
    
    /**
     * Links information for work package
     * 工作包链接信息
     */
    public record Links(
        String projectHref,
        String assigneeHref,
        String statusHref,
        String typeHref,
        String priorityHref
    ) {}
    
    /**
     * Custom field information
     * 自定义字段信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomField(
        String id,
        String name,
        String value,
        String type
    ) {}
    
    /**
     * Risk level enumeration
     * 风险级别枚举
     */
    public enum RiskLevel {
        LOW("低", "Low risk - No immediate action required"),
        MEDIUM("中", "Medium risk - Monitor and plan mitigation"),
        HIGH("高", "High risk - Immediate action required");
        
        private final String chineseName;
        private final String description;
        
        RiskLevel(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() {
            return chineseName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Utility method to validate work package data
     * 验证工作包数据的实用方法
     * 
     * @return true if the work package contains valid data
     */
    public boolean isValid() {
        return id != null && !id.trim().isEmpty() &&
               subject != null && !subject.trim().isEmpty() &&
               status != null;
    }
    
    /**
     * Utility method to get a summary of the work package
     * 获取工作包摘要的实用方法
     * 
     * @return Summary string with key information
     */
    public String getSummary() {
        return String.format("WorkPackage[%s]: %s - %s (Due: %s, Progress: %s)",
            id, subject, status != null ? status.name() : "Unknown",
            dueDate != null ? dueDate : "No due date",
            getProgressDescription());
    }
}