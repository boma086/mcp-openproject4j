package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Weekly Report Model for Project Analysis
 * 项目分析周报模型
 * 
 * This model represents a weekly project report generated from OpenProject data.
 * It includes task updates, summary information, and metadata for tracking.
 * 
 * 该模型表示从OpenProject数据生成的项目周报。它包括任务更新、摘要信息和跟踪元数据。
 * 
 * Key features:
 * - Comprehensive weekly report structure with Chinese/English support
 * - Task update tracking with status changes and timestamps
 * - Summary generation with key highlights and trends
 * - Metadata for report tracking and caching
 * - Integration with WorkPackage model for data consistency
 * 
 * 主要特性：
 * - 支持中英文的全面周报结构
 * - 任务更新跟踪，包含状态变更和时间戳
 * - 摘要生成，包含关键亮点和趋势
 * - 用于报告跟踪和缓存的元数据
 * - 与WorkPackage模型集成以确保数据一致性
 */
public class WeeklyReport {
    
    /**
     * Unique identifier for the project this report belongs to
     * 此报告所属项目的唯一标识符
     */
    @JsonProperty("projectId")
    private final String projectId;
    
    /**
     * Project name for display purposes
     * 用于显示目的的项目名称
     */
    @JsonProperty("projectName")
    private final String projectName;
    
    /**
     * Date when the report was generated
     * 报告生成日期
     */
    @JsonProperty("reportDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate reportDate;
    
    /**
     * Start date of the reporting period (typically 7 days ago)
     * 报告期的开始日期（通常为7天前）
     */
    @JsonProperty("periodStart")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate periodStart;
    
    /**
     * End date of the reporting period (typically today)
     * 报告期的结束日期（通常为今天）
     */
    @JsonProperty("periodEnd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate periodEnd;
    
    /**
     * Total number of work packages updated during the reporting period
     * 报告期内更新的工作包总数
     */
    @JsonProperty("totalTasks")
    private final int totalTasks;
    
    /**
     * List of task updates with detailed information
     * 详细信息任务更新列表
     */
    @JsonProperty("taskUpdates")
    private final List<TaskUpdate> taskUpdates;
    
    /**
     * Natural language summary of the weekly changes
     * 本周变更的自然语言摘要
     */
    @JsonProperty("summary")
    private final String summary;
    
    /**
     * Key highlights or important changes identified
     * 识别的关键亮点或重要变更
     */
    @JsonProperty("highlights")
    private final List<String> highlights;
    
    /**
     * Performance metrics and trends
     * 性能指标和趋势
     */
    @JsonProperty("metrics")
    private final WeeklyMetrics metrics;
    
    /**
     * Report metadata for tracking and caching
     * 用于跟踪和缓存的报告元数据
     */
    @JsonProperty("metadata")
    private final ReportMetadata metadata;
    
    /**
     * Constructor for WeeklyReport
     * 周报构造函数
     */
    public WeeklyReport(
            String projectId,
            String projectName,
            LocalDate reportDate,
            LocalDate periodStart,
            LocalDate periodEnd,
            int totalTasks,
            List<TaskUpdate> taskUpdates,
            String summary,
            List<String> highlights,
            WeeklyMetrics metrics,
            ReportMetadata metadata) {
        
        this.projectId = Objects.requireNonNull(projectId, "Project ID cannot be null");
        this.projectName = Objects.requireNonNull(projectName, "Project name cannot be null");
        this.reportDate = Objects.requireNonNull(reportDate, "Report date cannot be null");
        this.periodStart = Objects.requireNonNull(periodStart, "Period start date cannot be null");
        this.periodEnd = Objects.requireNonNull(periodEnd, "Period end date cannot be null");
        this.totalTasks = totalTasks;
        this.taskUpdates = Objects.requireNonNull(taskUpdates, "Task updates list cannot be null");
        this.summary = Objects.requireNonNull(summary, "Summary cannot be null");
        this.highlights = Objects.requireNonNull(highlights, "Highlights list cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "Metrics cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
    }
    
    /**
     * Gets the project ID
     * 获取项目ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Gets the project name
     * 获取项目名称
     */
    public String getProjectName() {
        return projectName;
    }
    
    /**
     * Gets the report date
     * 获取报告日期
     */
    public LocalDate getReportDate() {
        return reportDate;
    }
    
    /**
     * Gets the period start date
     * 获取期间开始日期
     */
    public LocalDate getPeriodStart() {
        return periodStart;
    }
    
    /**
     * Gets the period end date
     * 获取期间结束日期
     */
    public LocalDate getPeriodEnd() {
        return periodEnd;
    }
    
    /**
     * Gets the total number of tasks
     * 获取任务总数
     */
    public int getTotalTasks() {
        return totalTasks;
    }
    
    /**
     * Gets the task updates list
     * 获取任务更新列表
     */
    public List<TaskUpdate> getTaskUpdates() {
        return taskUpdates;
    }
    
    /**
     * Gets the summary
     * 获取摘要
     */
    public String getSummary() {
        return summary;
    }
    
    /**
     * Gets the highlights
     * 获取关键亮点
     */
    public List<String> getHighlights() {
        return highlights;
    }
    
    /**
     * Gets the metrics
     * 获取指标
     */
    public WeeklyMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Gets the metadata
     * 获取元数据
     */
    public ReportMetadata getMetadata() {
        return metadata;
    }
    
    /**
     * Gets the report period description
     * 获取报告期描述
     */
    public String getPeriodDescription() {
        return String.format("%s 至 %s", periodStart, periodEnd);
    }
    
    /**
     * Checks if the report has any task updates
     * 检查报告是否有任何任务更新
     */
    public boolean hasTaskUpdates() {
        return !taskUpdates.isEmpty();
    }
    
    /**
     * Gets the count of completed tasks in this report
     * 获取此报告中已完成任务的数量
     */
    public long getCompletedTasksCount() {
        return taskUpdates.stream()
            .filter(update -> update.getNewStatus() != null && 
                           update.getNewStatus().toLowerCase().contains("complete"))
            .count();
    }
    
    /**
     * Gets the count of high-priority tasks
     * 获取高优先级任务的数量
     */
    public long getHighPriorityTasksCount() {
        return taskUpdates.stream()
            .filter(TaskUpdate::isHighPriority)
            .count();
    }
    
    /**
     * Gets the count of overdue tasks
     * 获取逾期任务的数量
     */
    public long getOverdueTasksCount() {
        return taskUpdates.stream()
            .filter(TaskUpdate::isOverdue)
            .count();
    }
    
    /**
     * Nested class for task update information
     * 任务更新信息的嵌套类
     */
    public static class TaskUpdate {
        
        @JsonProperty("workPackageId")
        private final String workPackageId;
        
        @JsonProperty("subject")
        private final String subject;
        
        @JsonProperty("previousStatus")
        private final String previousStatus;
        
        @JsonProperty("newStatus")
        private final String newStatus;
        
        @JsonProperty("updatedAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private final String updatedAt;
        
        @JsonProperty("assigneeName")
        private final String assigneeName;
        
        @JsonProperty("priority")
        private final String priority;
        
        @JsonProperty("dueDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate dueDate;
        
        @JsonProperty("updateType")
        private final UpdateType updateType;
        
        @JsonProperty("description")
        private final String description;
        
        public TaskUpdate(
                String workPackageId,
                String subject,
                String previousStatus,
                String newStatus,
                String updatedAt,
                String assigneeName,
                String priority,
                LocalDate dueDate,
                UpdateType updateType,
                String description) {
            
            this.workPackageId = Objects.requireNonNull(workPackageId, "Work package ID cannot be null");
            this.subject = Objects.requireNonNull(subject, "Subject cannot be null");
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
            this.assigneeName = assigneeName;
            this.priority = priority;
            this.dueDate = dueDate;
            this.updateType = Objects.requireNonNull(updateType, "Update type cannot be null");
            this.description = description;
        }
        
        // Getters
        public String getWorkPackageId() { return workPackageId; }
        public String getSubject() { return subject; }
        public String getPreviousStatus() { return previousStatus; }
        public String getNewStatus() { return newStatus; }
        public String getUpdatedAt() { return updatedAt; }
        public String getAssigneeName() { return assigneeName; }
        public String getPriority() { return priority; }
        public LocalDate getDueDate() { return dueDate; }
        public UpdateType getUpdateType() { return updateType; }
        public String getDescription() { return description; }
        
        // Business logic methods
        public boolean isHighPriority() {
            return "high".equalsIgnoreCase(priority) || "urgent".equalsIgnoreCase(priority);
        }
        
        public boolean isOverdue() {
            return dueDate != null && dueDate.isBefore(LocalDate.now());
        }
        
        public boolean hasStatusChange() {
            return previousStatus != null && !previousStatus.equals(newStatus);
        }
    }
    
    /**
     * Enum for update types
     * 更新类型的枚举
     */
    public enum UpdateType {
        STATUS_CHANGE("状态变更", "Status changed"),
        NEW_TASK("新任务", "New task created"),
        ASSIGNMENT_CHANGE("分配变更", "Assignment changed"),
        DUE_DATE_CHANGE("截止日期变更", "Due date changed"),
        PRIORITY_CHANGE("优先级变更", "Priority changed"),
        PROGRESS_UPDATE("进度更新", "Progress updated"),
        COMMENT_ADDED("评论添加", "Comment added");
        
        private final String chineseName;
        private final String description;
        
        UpdateType(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Weekly metrics class
     * 周度指标类
     */
    public static class WeeklyMetrics {
        
        @JsonProperty("completionRate")
        private final double completionRate;
        
        @JsonProperty("tasksCompleted")
        private final int tasksCompleted;
        
        @JsonProperty("tasksCreated")
        private final int tasksCreated;
        
        @JsonProperty("averageCompletionTime")
        private final double averageCompletionTime;
        
        @JsonProperty("productivityScore")
        private final int productivityScore;
        
        public WeeklyMetrics(
                double completionRate,
                int tasksCompleted,
                int tasksCreated,
                double averageCompletionTime,
                int productivityScore) {
            
            this.completionRate = completionRate;
            this.tasksCompleted = tasksCompleted;
            this.tasksCreated = tasksCreated;
            this.averageCompletionTime = averageCompletionTime;
            this.productivityScore = productivityScore;
        }
        
        // Getters
        public double getCompletionRate() { return completionRate; }
        public int getTasksCompleted() { return tasksCompleted; }
        public int getTasksCreated() { return tasksCreated; }
        public double getAverageCompletionTime() { return averageCompletionTime; }
        public int getProductivityScore() { return productivityScore; }
    }
    
    /**
     * Report metadata class
     * 报告元数据类
     */
    public static class ReportMetadata {
        
        @JsonProperty("generatedAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private final String generatedAt;
        
        @JsonProperty("generatedBy")
        private final String generatedBy;
        
        @JsonProperty("version")
        private final String version;
        
        @JsonProperty("dataSources")
        private final List<String> dataSources;
        
        @JsonProperty("processingTime")
        private final long processingTime;
        
        public ReportMetadata(
                String generatedAt,
                String generatedBy,
                String version,
                List<String> dataSources,
                long processingTime) {
            
            this.generatedAt = Objects.requireNonNull(generatedAt, "Generated at cannot be null");
            this.generatedBy = Objects.requireNonNull(generatedBy, "Generated by cannot be null");
            this.version = Objects.requireNonNull(version, "Version cannot be null");
            this.dataSources = Objects.requireNonNull(dataSources, "Data sources cannot be null");
            this.processingTime = processingTime;
        }
        
        // Getters
        public String getGeneratedAt() { return generatedAt; }
        public String getGeneratedBy() { return generatedBy; }
        public String getVersion() { return version; }
        public List<String> getDataSources() { return dataSources; }
        public long getProcessingTime() { return processingTime; }
    }
    
    /**
     * Builder class for WeeklyReport
     * 周报构建器类
     */
    public static class WeeklyReportBuilder {
        
        private String projectId;
        private String projectName;
        private LocalDate reportDate;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private int totalTasks;
        private List<TaskUpdate> taskUpdates = new ArrayList<>();
        private String summary;
        private List<String> highlights = new ArrayList<>();
        private WeeklyMetrics metrics;
        private ReportMetadata metadata;
        
        public WeeklyReportBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }
        
        public WeeklyReportBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }
        
        public WeeklyReportBuilder reportDate(LocalDate reportDate) {
            this.reportDate = reportDate;
            return this;
        }
        
        public WeeklyReportBuilder periodStart(LocalDate periodStart) {
            this.periodStart = periodStart;
            return this;
        }
        
        public WeeklyReportBuilder periodEnd(LocalDate periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }
        
        public WeeklyReportBuilder totalTasks(int totalTasks) {
            this.totalTasks = totalTasks;
            return this;
        }
        
        public WeeklyReportBuilder taskUpdates(List<TaskUpdate> taskUpdates) {
            this.taskUpdates = taskUpdates;
            return this;
        }
        
        public WeeklyReportBuilder addTaskUpdate(TaskUpdate taskUpdate) {
            this.taskUpdates.add(taskUpdate);
            return this;
        }
        
        public WeeklyReportBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public WeeklyReportBuilder highlights(List<String> highlights) {
            this.highlights = highlights;
            return this;
        }
        
        public WeeklyReportBuilder addHighlight(String highlight) {
            this.highlights.add(highlight);
            return this;
        }
        
        public WeeklyReportBuilder metrics(WeeklyMetrics metrics) {
            this.metrics = metrics;
            return this;
        }
        
        public WeeklyReportBuilder metadata(ReportMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public WeeklyReport build() {
            return new WeeklyReport(
                projectId,
                projectName,
                reportDate,
                periodStart,
                periodEnd,
                totalTasks,
                taskUpdates,
                summary,
                highlights,
                metrics,
                metadata
            );
        }
    }
    
    /**
     * Static factory method to create WeeklyReport from WorkPackage data
     * 从WorkPackage数据创建周报的静态工厂方法
     */
    public static WeeklyReport createFromWorkPackages(
            String projectId,
            String projectName,
            List<WorkPackage> workPackages,
            LocalDate periodStart,
            LocalDate periodEnd) {
        
        Objects.requireNonNull(projectId, "Project ID cannot be null");
        Objects.requireNonNull(projectName, "Project name cannot be null");
        Objects.requireNonNull(workPackages, "Work packages list cannot be null");
        Objects.requireNonNull(periodStart, "Period start date cannot be null");
        Objects.requireNonNull(periodEnd, "Period end date cannot be null");
        
        // Filter work packages by date range
        // 按日期范围过滤工作包
        List<WorkPackage> filteredWorkPackages = filterWorkPackagesByDateRange(workPackages, periodStart, periodEnd);
        
        // Generate task updates
        // 生成任务更新
        List<TaskUpdate> taskUpdates = generateTaskUpdates(filteredWorkPackages);
        
        // Calculate metrics
        // 计算指标
        WeeklyMetrics metrics = calculateMetricsFromWorkPackages(filteredWorkPackages);
        
        // Generate summary
        // 生成摘要
        String summary = generateSummary(filteredWorkPackages, periodStart, periodEnd);
        
        // Generate highlights
        // 生成关键亮点
        List<String> highlights = generateHighlights(filteredWorkPackages);
        
        // Create metadata
        // 创建元数据
        ReportMetadata metadata = new ReportMetadata(
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            "OpenProject MCP Server",
            "1.0.0",
            List.of("OpenProject API v3"),
            System.currentTimeMillis()
        );
        
        return new WeeklyReport(
            projectId,
            projectName,
            LocalDate.now(),
            periodStart,
            periodEnd,
            filteredWorkPackages.size(),
            taskUpdates,
            summary,
            highlights,
            metrics,
            metadata
        );
    }
    
    /**
     * Filter work packages by date range
     * 按日期范围过滤工作包
     */
    private static List<WorkPackage> filterWorkPackagesByDateRange(
            List<WorkPackage> workPackages, LocalDate periodStart, LocalDate periodEnd) {
        
        return workPackages.stream()
            .filter(wp -> wp.updatedAt() != null)
            .filter(wp -> {
                LocalDateTime updatedDateTime = wp.updatedAt();
                LocalDate updatedDate = updatedDateTime.toLocalDate();
                return !updatedDate.isBefore(periodStart) && !updatedDate.isAfter(periodEnd);
            })
            .toList();
    }
    
    /**
     * Generate task updates from work packages
     * 从工作包生成任务更新
     */
    private static List<TaskUpdate> generateTaskUpdates(List<WorkPackage> workPackages) {
        return workPackages.stream()
            .map(wp -> {
                UpdateType updateType = determineUpdateType(wp);
                return new TaskUpdate(
                    wp.id(),
                    wp.subject(),
                    wp.status() != null ? wp.status().name() : null,
                    wp.status() != null ? wp.status().name() : null,
                    wp.updatedAt() != null ? wp.updatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null,
                    wp.assignee() != null ? wp.assignee().name() : null,
                    wp.priority() != null ? wp.priority().name() : null,
                    wp.dueDate(),
                    updateType,
                    generateUpdateDescription(wp, updateType)
                );
            })
            .toList();
    }
    
    /**
     * Determine update type based on work package changes
     * 根据工作包变更确定更新类型
     */
    private static UpdateType determineUpdateType(WorkPackage wp) {
        if (wp.isCompleted()) {
            return UpdateType.PROGRESS_UPDATE;
        }
        if (wp.isInProgress()) {
            return UpdateType.PROGRESS_UPDATE;
        }
        if (wp.isOverdue()) {
            return UpdateType.DUE_DATE_CHANGE;
        }
        return UpdateType.PROGRESS_UPDATE;
    }
    
    /**
     * Generate update description
     * 生成更新描述
     */
    private static String generateUpdateDescription(WorkPackage wp, UpdateType updateType) {
        StringBuilder description = new StringBuilder();
        description.append(wp.subject());
        
        if (wp.percentageDone() != null) {
            description.append(" - 进度: ").append(wp.percentageDone()).append("%");
        }
        
        if (wp.isOverdue()) {
            description.append(" - 逾期");
        }
        
        if (wp.assignee() != null) {
            description.append(" - 负责人: ").append(wp.assignee().name());
        }
        
        return description.toString();
    }
    
    /**
     * Calculate metrics from work packages
     * 从工作包计算指标
     */
    private static WeeklyMetrics calculateMetricsFromWorkPackages(List<WorkPackage> workPackages) {
        int totalTasks = workPackages.size();
        int tasksCompleted = (int) workPackages.stream().filter(WorkPackage::isCompleted).count();
        int tasksCreated = totalTasks; // All filtered tasks are considered "created" in this period
        double completionRate = totalTasks > 0 ? (tasksCompleted * 100.0) / totalTasks : 0.0;
        
        // Calculate average completion time (simplified)
        // 计算平均完成时间（简化版）
        double averageCompletionTime = calculateAverageCompletionTime(workPackages);
        
        // Calculate productivity score (simplified)
        // 计算生产力分数（简化版）
        int productivityScore = calculateProductivityScore(workPackages, completionRate);
        
        return new WeeklyMetrics(
            completionRate,
            tasksCompleted,
            tasksCreated,
            averageCompletionTime,
            productivityScore
        );
    }
    
    /**
     * Calculate average completion time
     * 计算平均完成时间
     */
    private static double calculateAverageCompletionTime(List<WorkPackage> workPackages) {
        // Simplified calculation - in real implementation, this would consider actual time spent
        // 简化计算 - 在实际实现中，这会考虑实际花费的时间
        return workPackages.stream()
            .filter(WorkPackage::isCompleted)
            .mapToDouble(wp -> wp.estimatedTime() != null ? wp.estimatedTime() : 8.0)
            .average()
            .orElse(8.0);
    }
    
    /**
     * Calculate productivity score
     * 计算生产力分数
     */
    private static int calculateProductivityScore(List<WorkPackage> workPackages, double completionRate) {
        int baseScore = (int) completionRate;
        
        // Bonus for on-time completion
        // 按时完成奖励
        long onTimeCount = workPackages.stream()
            .filter(wp -> !wp.isOverdue())
            .count();
        double onTimeBonus = workPackages.size() > 0 ? (onTimeCount * 10.0) / workPackages.size() : 0;
        
        // Penalty for overdue tasks
        // 逾期任务惩罚
        long overdueCount = workPackages.stream()
            .filter(WorkPackage::isOverdue)
            .count();
        double overduePenalty = workPackages.size() > 0 ? (overdueCount * 15.0) / workPackages.size() : 0;
        
        int finalScore = baseScore + (int) onTimeBonus - (int) overduePenalty;
        return Math.max(0, Math.min(100, finalScore));
    }
    
    /**
     * Generate summary for the weekly report
     * 生成周报摘要
     */
    private static String generateSummary(List<WorkPackage> workPackages, LocalDate periodStart, LocalDate periodEnd) {
        if (workPackages.isEmpty()) {
            return String.format("在 %s 至 %s 期间没有任务更新。", periodStart, periodEnd);
        }
        
        int totalTasks = workPackages.size();
        int completedTasks = (int) workPackages.stream().filter(WorkPackage::isCompleted).count();
        int overdueTasks = (int) workPackages.stream().filter(WorkPackage::isOverdue).count();
        double completionRate = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;
        
        return String.format(
            "在 %s 至 %s 期间，共更新了 %d 个任务，其中 %d 个已完成（%.1f%%），%d 个逾期。整体进度良好。%n" +
            "During the period %s to %s, %d tasks were updated, with %d completed (%.1f%%) and %d overdue. Overall progress is good.",
            periodStart, periodEnd, totalTasks, completedTasks, completionRate, overdueTasks,
            periodStart, periodEnd, totalTasks, completedTasks, completionRate, overdueTasks
        );
    }
    
    /**
     * Generate highlights for the weekly report
     * 生成周报关键亮点
     */
    private static List<String> generateHighlights(List<WorkPackage> workPackages) {
        List<String> highlights = new ArrayList<>();
        
        // Completed tasks highlight
        // 已完成任务亮点
        long completedCount = workPackages.stream().filter(WorkPackage::isCompleted).count();
        if (completedCount > 0) {
            highlights.add(String.format("本周完成了 %d 个任务 / Completed %d tasks this week", completedCount, completedCount));
        }
        
        // Overdue tasks highlight
        // 逾期任务亮点
        long overdueCount = workPackages.stream().filter(WorkPackage::isOverdue).count();
        if (overdueCount > 0) {
            highlights.add(String.format("有 %d 个任务逾期 / %d tasks are overdue", overdueCount, overdueCount));
        }
        
        // High priority tasks highlight
        // 高优先级任务亮点
        long highPriorityCount = workPackages.stream()
            .filter(wp -> wp.priority() != null && "high".equalsIgnoreCase(wp.priority().name()))
            .count();
        if (highPriorityCount > 0) {
            highlights.add(String.format("有 %d 个高优先级任务 / %d high-priority tasks", highPriorityCount, highPriorityCount));
        }
        
        // Tasks with assignees highlight
        // 有分配者的任务亮点
        long assignedCount = workPackages.stream()
            .filter(wp -> wp.assignee() != null)
            .count();
        if (assignedCount > 0) {
            highlights.add(String.format("有 %d 个任务已分配负责人 / %d tasks have assignees", assignedCount, assignedCount));
        }
        
        return highlights;
    }
    
    /**
     * Get a new builder instance
     * 获取新的构建器实例
     */
    public static WeeklyReportBuilder builder() {
        return new WeeklyReportBuilder();
    }
    
    /**
     * Validate the report data
     * 验证报告数据
     */
    public boolean isValid() {
        return projectId != null && !projectId.trim().isEmpty() &&
               projectName != null && !projectName.trim().isEmpty() &&
               reportDate != null &&
               periodStart != null && periodEnd != null &&
               !periodStart.isAfter(periodEnd) &&
               taskUpdates != null &&
               summary != null && !summary.trim().isEmpty() &&
               highlights != null &&
               metrics != null &&
               metadata != null;
    }
    
    /**
     * Get report summary in Chinese
     * 获取中文报告摘要
     */
    public String getChineseSummary() {
        return String.format("项目 %s 周报 (%s 至 %s): %d 个任务更新，完成率 %.1f%%",
            projectName, periodStart, periodEnd, totalTasks, 
            metrics != null ? metrics.getCompletionRate() : 0.0);
    }
    
    /**
     * Get report summary in English
     * 获取英文报告摘要
     */
    public String getEnglishSummary() {
        return String.format("Project %s Weekly Report (%s to %s): %d task updates, completion rate %.1f%%",
            projectName, periodStart, periodEnd, totalTasks,
            metrics != null ? metrics.getCompletionRate() : 0.0);
    }
}