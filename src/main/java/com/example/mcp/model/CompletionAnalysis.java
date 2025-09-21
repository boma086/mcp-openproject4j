package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Completion Analysis Model for Project Progress Assessment
 * 项目进度评估的完成度分析模型
 * 
 * This model represents a comprehensive completion analysis report generated from OpenProject data.
 * It includes progress tracking, completion rates, performance metrics, and predictive analytics.
 * 
 * 该模型表示从OpenProject数据生成的综合完成度分析报告。它包括进度跟踪、完成率、性能指标和预测分析。
 * 
 * Key features:
 * - Comprehensive completion analysis with multiple progress dimensions
 * - Performance metrics and trend analysis
 * - Predictive analytics for project completion forecasting
 * - Resource utilization and productivity analysis
 * - Integration with WorkPackage model for data consistency
 * 
 * 主要特性：
 * - 多维度的综合完成度分析
 * - 性能指标和趋势分析
 * - 项目完成情况的预测分析
 * - 资源利用率和生产力分析
 * - 与WorkPackage模型集成以确保数据一致性
 */
public class CompletionAnalysis {
    
    /**
     * Unique identifier for the project this analysis belongs to
     * 此分析所属项目的唯一标识符
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
     * Date when the completion analysis was generated
     * 完成度分析生成日期
     */
    @JsonProperty("analysisDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate analysisDate;
    
    /**
     * Overall completion percentage for the project (0-100)
     * 项目的整体完成百分比（0-100）
     */
    @JsonProperty("overallCompletion")
    private final double overallCompletion;
    
    /**
     * Project status classification
     * 项目状态分类
     */
    @JsonProperty("projectStatus")
    private final ProjectStatus projectStatus;
    
    /**
     * List of work packages with completion details
     * 工作包的完成详细信息列表
     */
    @JsonProperty("workPackages")
    private final List<WorkPackageCompletion> workPackages;
    
    /**
     * Progress metrics and performance indicators
     * 进度指标和性能指标
     */
    @JsonProperty("progressMetrics")
    private final ProgressMetrics progressMetrics;
    
    /**
     * Completion trends and patterns
     * 完成趋势和模式
     */
    @JsonProperty("completionTrends")
    private final CompletionTrends completionTrends;
    
    /**
     * Resource utilization analysis
     * 资源利用分析
     */
    @JsonProperty("resourceUtilization")
    private final ResourceUtilization resourceUtilization;
    
    /**
     * Predictive analytics and forecasts
     * 预测分析和预测
     */
    @JsonProperty("predictions")
    private final PredictiveAnalytics predictions;
    
    /**
     * Performance benchmarks and comparisons
     * 性能基准和比较
     */
    @JsonProperty("benchmarks")
    private final PerformanceBenchmarks benchmarks;
    
    /**
     * Analysis metadata for tracking and validation
     * 用于跟踪和验证的分析元数据
     */
    @JsonProperty("metadata")
    private final AnalysisMetadata metadata;
    
    /**
     * Constructor for CompletionAnalysis
     * 完成度分析构造函数
     */
    public CompletionAnalysis(
            String projectId,
            String projectName,
            LocalDate analysisDate,
            double overallCompletion,
            ProjectStatus projectStatus,
            List<WorkPackageCompletion> workPackages,
            ProgressMetrics progressMetrics,
            CompletionTrends completionTrends,
            ResourceUtilization resourceUtilization,
            PredictiveAnalytics predictions,
            PerformanceBenchmarks benchmarks,
            AnalysisMetadata metadata) {
        
        this.projectId = Objects.requireNonNull(projectId, "Project ID cannot be null");
        this.projectName = Objects.requireNonNull(projectName, "Project name cannot be null");
        this.analysisDate = Objects.requireNonNull(analysisDate, "Analysis date cannot be null");
        this.overallCompletion = overallCompletion;
        this.projectStatus = Objects.requireNonNull(projectStatus, "Project status cannot be null");
        this.workPackages = Objects.requireNonNull(workPackages, "Work packages list cannot be null");
        this.progressMetrics = Objects.requireNonNull(progressMetrics, "Progress metrics cannot be null");
        this.completionTrends = Objects.requireNonNull(completionTrends, "Completion trends cannot be null");
        this.resourceUtilization = Objects.requireNonNull(resourceUtilization, "Resource utilization cannot be null");
        this.predictions = Objects.requireNonNull(predictions, "Predictions cannot be null");
        this.benchmarks = Objects.requireNonNull(benchmarks, "Benchmarks cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
    }
    
    // Getters
    public String getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public LocalDate getAnalysisDate() { return analysisDate; }
    public double getOverallCompletion() { return overallCompletion; }
    public ProjectStatus getProjectStatus() { return projectStatus; }
    public List<WorkPackageCompletion> getWorkPackages() { return workPackages; }
    public ProgressMetrics getProgressMetrics() { return progressMetrics; }
    public CompletionTrends getCompletionTrends() { return completionTrends; }
    public ResourceUtilization getResourceUtilization() { return resourceUtilization; }
    public PredictiveAnalytics getPredictions() { return predictions; }
    public PerformanceBenchmarks getBenchmarks() { return benchmarks; }
    public AnalysisMetadata getMetadata() { return metadata; }
    
    /**
     * Gets the count of completed work packages
     * 获取已完成工作包的数量
     */
    public long getCompletedWorkPackagesCount() {
        return workPackages.stream()
            .filter(wp -> wp.getCompletionPercentage() >= 100)
            .count();
    }
    
    /**
     * Gets the count of work packages in progress
     * 获取进行中工作包的数量
     */
    public long getInProgressWorkPackagesCount() {
        return workPackages.stream()
            .filter(wp -> wp.getCompletionPercentage() > 0 && wp.getCompletionPercentage() < 100)
            .count();
    }
    
    /**
     * Gets the count of work packages not started
     * 获取未开始工作包的数量
     */
    public long getNotStartedWorkPackagesCount() {
        return workPackages.stream()
            .filter(wp -> wp.getCompletionPercentage() == 0)
            .count();
    }
    
    /**
     * Gets the count of overdue work packages
     * 获取逾期工作包的数量
     */
    public long getOverdueWorkPackagesCount() {
        return workPackages.stream()
            .filter(WorkPackageCompletion::isOverdue)
            .count();
    }
    
    /**
     * Gets the average completion percentage
     * 获取平均完成百分比
     */
    public double getAverageCompletionPercentage() {
        return workPackages.stream()
            .mapToInt(WorkPackageCompletion::getCompletionPercentage)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Checks if the project is on track
     * 检查项目是否按计划进行
     */
    public boolean isOnTrack() {
        return projectStatus == ProjectStatus.ON_TRACK || 
               projectStatus == ProjectStatus.AHEAD_OF_SCHEDULE;
    }
    
    /**
     * Checks if the project has risks
     * 检查项目是否有风险
     */
    public boolean hasRisks() {
        return projectStatus == ProjectStatus.AT_RISK || 
               projectStatus == ProjectStatus.DELAYED ||
               projectStatus == ProjectStatus.CRITICAL;
    }
    
    /**
     * Nested class for work package completion details
     * 工作包完成详情的嵌套类
     */
    public static class WorkPackageCompletion {
        
        @JsonProperty("workPackageId")
        private final String workPackageId;
        
        @JsonProperty("subject")
        private final String subject;
        
        @JsonProperty("type")
        private final String type;
        
        @JsonProperty("status")
        private final String status;
        
        @JsonProperty("completionPercentage")
        private final int completionPercentage;
        
        @JsonProperty("progressDescription")
        private final String progressDescription;
        
        @JsonProperty("assignee")
        private final String assignee;
        
        @JsonProperty("dueDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate dueDate;
        
        @JsonProperty("startDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate startDate;
        
        @JsonProperty("actualCompletionDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate actualCompletionDate;
        
        @JsonProperty("estimatedTime")
        private final Double estimatedTime;
        
        @JsonProperty("spentTime")
        private final Double spentTime;
        
        @JsonProperty("timeEfficiency")
        private final Double timeEfficiency;
        
        @JsonProperty("isOverdue")
        private final boolean isOverdue;
        
        @JsonProperty("daysOverdue")
        private final int daysOverdue;
        
        @JsonProperty("daysUntilDue")
        private final int daysUntilDue;
        
        @JsonProperty("priority")
        private final String priority;
        
        @JsonProperty("progressTrend")
        private final ProgressTrend progressTrend;
        
        @JsonProperty("lastUpdated")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private final String lastUpdated;
        
        public WorkPackageCompletion(
                String workPackageId,
                String subject,
                String type,
                String status,
                int completionPercentage,
                String progressDescription,
                String assignee,
                LocalDate dueDate,
                LocalDate startDate,
                LocalDate actualCompletionDate,
                Double estimatedTime,
                Double spentTime,
                Double timeEfficiency,
                boolean isOverdue,
                int daysOverdue,
                int daysUntilDue,
                String priority,
                ProgressTrend progressTrend,
                String lastUpdated) {
            
            this.workPackageId = Objects.requireNonNull(workPackageId, "Work package ID cannot be null");
            this.subject = Objects.requireNonNull(subject, "Subject cannot be null");
            this.type = Objects.requireNonNull(type, "Type cannot be null");
            this.status = Objects.requireNonNull(status, "Status cannot be null");
            this.completionPercentage = completionPercentage;
            this.progressDescription = progressDescription;
            this.assignee = assignee;
            this.dueDate = dueDate;
            this.startDate = startDate;
            this.actualCompletionDate = actualCompletionDate;
            this.estimatedTime = estimatedTime;
            this.spentTime = spentTime;
            this.timeEfficiency = timeEfficiency;
            this.isOverdue = isOverdue;
            this.daysOverdue = daysOverdue;
            this.daysUntilDue = daysUntilDue;
            this.priority = Objects.requireNonNull(priority, "Priority cannot be null");
            this.progressTrend = Objects.requireNonNull(progressTrend, "Progress trend cannot be null");
            this.lastUpdated = Objects.requireNonNull(lastUpdated, "Last updated cannot be null");
        }
        
        // Getters
        public String getWorkPackageId() { return workPackageId; }
        public String getSubject() { return subject; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public int getCompletionPercentage() { return completionPercentage; }
        public String getProgressDescription() { return progressDescription; }
        public String getAssignee() { return assignee; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getActualCompletionDate() { return actualCompletionDate; }
        public Double getEstimatedTime() { return estimatedTime; }
        public Double getSpentTime() { return spentTime; }
        public Double getTimeEfficiency() { return timeEfficiency; }
        public boolean isOverdue() { return isOverdue; }
        public int getDaysOverdue() { return daysOverdue; }
        public int getDaysUntilDue() { return daysUntilDue; }
        public String getPriority() { return priority; }
        public ProgressTrend getProgressTrend() { return progressTrend; }
        public String getLastUpdated() { return lastUpdated; }
        
        // Business logic methods
        public boolean isCompleted() {
            return completionPercentage >= 100;
        }
        
        public boolean isInProgress() {
            return completionPercentage > 0 && completionPercentage < 100;
        }
        
        public boolean isNotStarted() {
            return completionPercentage == 0;
        }
        
        public boolean isHighPriority() {
            return "high".equalsIgnoreCase(priority) || "urgent".equalsIgnoreCase(priority);
        }
        
        public boolean isOnSchedule() {
            return !isOverdue && daysUntilDue >= 0;
        }
        
        public double getCompletionRate() {
            if (startDate == null) {
                return 0.0;
            }
            
            LocalDate currentDate = LocalDate.now();
            if (currentDate.isBefore(startDate)) {
                return 0.0;
            }
            
            if (dueDate == null) {
                return completionPercentage / 100.0;
            }
            
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, dueDate);
            long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, currentDate);
            
            if (totalDays <= 0) {
                return 1.0;
            }
            
            double expectedProgress = Math.min(1.0, (double) elapsedDays / totalDays);
            double actualProgress = completionPercentage / 100.0;
            
            return actualProgress / expectedProgress;
        }
    }
    
    /**
     * Progress trend enumeration
     * 进度趋势枚举
     */
    public enum ProgressTrend {
        ACCELERATING("加速", "Progress is accelerating"),
        STEADY("稳定", "Progress is steady and consistent"),
        DECELERATING("减速", "Progress is decelerating"),
        STALLED("停滞", "Progress has stalled"),
        REVERSED("倒退", "Progress has reversed"),
        UNKNOWN("未知", "Insufficient data to determine trend");
        
        private final String chineseName;
        private final String description;
        
        ProgressTrend(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Project status enumeration
     * 项目状态枚举
     */
    public enum ProjectStatus {
        NOT_STARTED("未开始", "Project has not started"),
        IN_PROGRESS("进行中", "Project is in progress"),
        AHEAD_OF_SCHEDULE("提前", "Project is ahead of schedule"),
        ON_TRACK("按计划", "Project is on track"),
        AT_RISK("有风险", "Project is at risk"),
        DELAYED("延迟", "Project is delayed"),
        CRITICAL("关键", "Project is in critical state"),
        COMPLETED("已完成", "Project is completed"),
        CANCELLED("已取消", "Project is cancelled"),
        ON_HOLD("暂停", "Project is on hold");
        
        private final String chineseName;
        private final String description;
        
        ProjectStatus(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Progress metrics class
     * 进度指标类
     */
    public static class ProgressMetrics {
        
        @JsonProperty("totalWorkPackages")
        private final int totalWorkPackages;
        
        @JsonProperty("completedWorkPackages")
        private final int completedWorkPackages;
        
        @JsonProperty("inProgressWorkPackages")
        private final int inProgressWorkPackages;
        
        @JsonProperty("notStartedWorkPackages")
        private final int notStartedWorkPackages;
        
        @JsonProperty("overdueWorkPackages")
        private final int overdueWorkPackages;
        
        @JsonProperty("averageCompletionRate")
        private final double averageCompletionRate;
        
        @JsonProperty("weightedCompletionRate")
        private final double weightedCompletionRate;
        
        @JsonProperty("scheduleVariance")
        private final double scheduleVariance;
        
        @JsonProperty("costVariance")
        private final double costVariance;
        
        @JsonProperty("productivityIndex")
        private final double productivityIndex;
        
        @JsonProperty("qualityScore")
        private final double qualityScore;
        
        public ProgressMetrics(
                int totalWorkPackages,
                int completedWorkPackages,
                int inProgressWorkPackages,
                int notStartedWorkPackages,
                int overdueWorkPackages,
                double averageCompletionRate,
                double weightedCompletionRate,
                double scheduleVariance,
                double costVariance,
                double productivityIndex,
                double qualityScore) {
            
            this.totalWorkPackages = totalWorkPackages;
            this.completedWorkPackages = completedWorkPackages;
            this.inProgressWorkPackages = inProgressWorkPackages;
            this.notStartedWorkPackages = notStartedWorkPackages;
            this.overdueWorkPackages = overdueWorkPackages;
            this.averageCompletionRate = averageCompletionRate;
            this.weightedCompletionRate = weightedCompletionRate;
            this.scheduleVariance = scheduleVariance;
            this.costVariance = costVariance;
            this.productivityIndex = productivityIndex;
            this.qualityScore = qualityScore;
        }
        
        // Getters
        public int getTotalWorkPackages() { return totalWorkPackages; }
        public int getCompletedWorkPackages() { return completedWorkPackages; }
        public int getInProgressWorkPackages() { return inProgressWorkPackages; }
        public int getNotStartedWorkPackages() { return notStartedWorkPackages; }
        public int getOverdueWorkPackages() { return overdueWorkPackages; }
        public double getAverageCompletionRate() { return averageCompletionRate; }
        public double getWeightedCompletionRate() { return weightedCompletionRate; }
        public double getScheduleVariance() { return scheduleVariance; }
        public double getCostVariance() { return costVariance; }
        public double getProductivityIndex() { return productivityIndex; }
        public double getQualityScore() { return qualityScore; }
        
        // Business logic methods
        public double getCompletionPercentage() {
            if (totalWorkPackages == 0) {
                return 0.0;
            }
            return (completedWorkPackages * 100.0) / totalWorkPackages;
        }
        
        public double getOnSchedulePercentage() {
            if (totalWorkPackages == 0) {
                return 0.0;
            }
            int onSchedule = totalWorkPackages - overdueWorkPackages;
            return (onSchedule * 100.0) / totalWorkPackages;
        }
    }
    
    /**
     * Completion trends class
     * 完成趋势类
     */
    public static class CompletionTrends {
        
        @JsonProperty("overallTrend")
        private final TrendDirection overallTrend;
        
        @JsonProperty("completionHistory")
        private final List<CompletionHistoryPoint> completionHistory;
        
        @JsonProperty("velocityTrend")
        private final VelocityTrend velocityTrend;
        
        @JsonProperty("burndownRate")
        private final double burndownRate;
        
        @JsonProperty("accelerationFactor")
        private final double accelerationFactor;
        
        public CompletionTrends(
                TrendDirection overallTrend,
                List<CompletionHistoryPoint> completionHistory,
                VelocityTrend velocityTrend,
                double burndownRate,
                double accelerationFactor) {
            
            this.overallTrend = Objects.requireNonNull(overallTrend, "Overall trend cannot be null");
            this.completionHistory = Objects.requireNonNull(completionHistory, "Completion history cannot be null");
            this.velocityTrend = Objects.requireNonNull(velocityTrend, "Velocity trend cannot be null");
            this.burndownRate = burndownRate;
            this.accelerationFactor = accelerationFactor;
        }
        
        // Getters
        public TrendDirection getOverallTrend() { return overallTrend; }
        public List<CompletionHistoryPoint> getCompletionHistory() { return completionHistory; }
        public VelocityTrend getVelocityTrend() { return velocityTrend; }
        public double getBurndownRate() { return burndownRate; }
        public double getAccelerationFactor() { return accelerationFactor; }
    }
    
    /**
     * Trend direction enumeration
     * 趋势方向枚举
     */
    public enum TrendDirection {
        IMPROVING("改善", "Trend is improving"),
        STABLE("稳定", "Trend is stable"),
        DECLINING("下降", "Trend is declining"),
        VOLATILE("波动", "Trend is volatile"),
        UNKNOWN("未知", "Insufficient data to determine trend");
        
        private final String chineseName;
        private final String description;
        
        TrendDirection(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Completion history point class
     * 完成历史点类
     */
    public static class CompletionHistoryPoint {
        
        @JsonProperty("date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate date;
        
        @JsonProperty("completionPercentage")
        private final double completionPercentage;
        
        @JsonProperty("completedWorkPackages")
        private final int completedWorkPackages;
        
        @JsonProperty("velocity")
        private final double velocity;
        
        public CompletionHistoryPoint(LocalDate date, double completionPercentage, int completedWorkPackages, double velocity) {
            this.date = Objects.requireNonNull(date, "Date cannot be null");
            this.completionPercentage = completionPercentage;
            this.completedWorkPackages = completedWorkPackages;
            this.velocity = velocity;
        }
        
        // Getters
        public LocalDate getDate() { return date; }
        public double getCompletionPercentage() { return completionPercentage; }
        public int getCompletedWorkPackages() { return completedWorkPackages; }
        public double getVelocity() { return velocity; }
    }
    
    /**
     * Velocity trend class
     * 速度趋势类
     */
    public static class VelocityTrend {
        
        @JsonProperty("currentVelocity")
        private final double currentVelocity;
        
        @JsonProperty("averageVelocity")
        private final double averageVelocity;
        
        @JsonProperty("velocityTrend")
        private final TrendDirection velocityTrend;
        
        @JsonProperty("velocityStability")
        private final double velocityStability;
        
        public VelocityTrend(double currentVelocity, double averageVelocity, TrendDirection velocityTrend, double velocityStability) {
            this.currentVelocity = currentVelocity;
            this.averageVelocity = averageVelocity;
            this.velocityTrend = Objects.requireNonNull(velocityTrend, "Velocity trend cannot be null");
            this.velocityStability = velocityStability;
        }
        
        // Getters
        public double getCurrentVelocity() { return currentVelocity; }
        public double getAverageVelocity() { return averageVelocity; }
        public TrendDirection getVelocityTrend() { return velocityTrend; }
        public double getVelocityStability() { return velocityStability; }
    }
    
    /**
     * Resource utilization class
     * 资源利用类
     */
    public static class ResourceUtilization {
        
        @JsonProperty("teamUtilization")
        private final double teamUtilization;
        
        @JsonProperty("resourceEfficiency")
        private final double resourceEfficiency;
        
        @JsonProperty("workloadDistribution")
        private final WorkloadDistribution workloadDistribution;
        
        @JsonProperty("timeUtilization")
        private final TimeUtilization timeUtilization;
        
        @JsonProperty("capacityUtilization")
        private final CapacityUtilization capacityUtilization;
        
        public ResourceUtilization(
                double teamUtilization,
                double resourceEfficiency,
                WorkloadDistribution workloadDistribution,
                TimeUtilization timeUtilization,
                CapacityUtilization capacityUtilization) {
            
            this.teamUtilization = teamUtilization;
            this.resourceEfficiency = resourceEfficiency;
            this.workloadDistribution = Objects.requireNonNull(workloadDistribution, "Workload distribution cannot be null");
            this.timeUtilization = Objects.requireNonNull(timeUtilization, "Time utilization cannot be null");
            this.capacityUtilization = Objects.requireNonNull(capacityUtilization, "Capacity utilization cannot be null");
        }
        
        // Getters
        public double getTeamUtilization() { return teamUtilization; }
        public double getResourceEfficiency() { return resourceEfficiency; }
        public WorkloadDistribution getWorkloadDistribution() { return workloadDistribution; }
        public TimeUtilization getTimeUtilization() { return timeUtilization; }
        public CapacityUtilization getCapacityUtilization() { return capacityUtilization; }
    }
    
    /**
     * Workload distribution class
     * 工作负载分布类
     */
    public static class WorkloadDistribution {
        
        @JsonProperty("byAssignee")
        private final List<AssigneeWorkload> byAssignee;
        
        @JsonProperty("byPriority")
        private final PriorityWorkload byPriority;
        
        @JsonProperty("byType")
        private final TypeWorkload byType;
        
        public WorkloadDistribution(
                List<AssigneeWorkload> byAssignee,
                PriorityWorkload byPriority,
                TypeWorkload byType) {
            
            this.byAssignee = Objects.requireNonNull(byAssignee, "By assignee cannot be null");
            this.byPriority = Objects.requireNonNull(byPriority, "By priority cannot be null");
            this.byType = Objects.requireNonNull(byType, "By type cannot be null");
        }
        
        // Getters
        public List<AssigneeWorkload> getByAssignee() { return byAssignee; }
        public PriorityWorkload getByPriority() { return byPriority; }
        public TypeWorkload getByType() { return byType; }
    }
    
    /**
     * Assignee workload class
     * 分配者工作负载类
     */
    public static class AssigneeWorkload {
        
        @JsonProperty("assigneeName")
        private final String assigneeName;
        
        @JsonProperty("totalWorkPackages")
        private final int totalWorkPackages;
        
        @JsonProperty("completedWorkPackages")
        private final int completedWorkPackages;
        
        @JsonProperty("inProgressWorkPackages")
        private final int inProgressWorkPackages;
        
        @JsonProperty("workloadPercentage")
        private final double workloadPercentage;
        
        @JsonProperty("averageCompletionRate")
        private final double averageCompletionRate;
        
        public AssigneeWorkload(
                String assigneeName,
                int totalWorkPackages,
                int completedWorkPackages,
                int inProgressWorkPackages,
                double workloadPercentage,
                double averageCompletionRate) {
            
            this.assigneeName = Objects.requireNonNull(assigneeName, "Assignee name cannot be null");
            this.totalWorkPackages = totalWorkPackages;
            this.completedWorkPackages = completedWorkPackages;
            this.inProgressWorkPackages = inProgressWorkPackages;
            this.workloadPercentage = workloadPercentage;
            this.averageCompletionRate = averageCompletionRate;
        }
        
        // Getters
        public String getAssigneeName() { return assigneeName; }
        public int getTotalWorkPackages() { return totalWorkPackages; }
        public int getCompletedWorkPackages() { return completedWorkPackages; }
        public int getInProgressWorkPackages() { return inProgressWorkPackages; }
        public double getWorkloadPercentage() { return workloadPercentage; }
        public double getAverageCompletionRate() { return averageCompletionRate; }
    }
    
    /**
     * Priority workload class
     * 优先级工作负载类
     */
    public static class PriorityWorkload {
        
        @JsonProperty("high")
        private final int high;
        
        @JsonProperty("medium")
        private final int medium;
        
        @JsonProperty("low")
        private final int low;
        
        public PriorityWorkload(int high, int medium, int low) {
            this.high = high;
            this.medium = medium;
            this.low = low;
        }
        
        // Getters
        public int getHigh() { return high; }
        public int getMedium() { return medium; }
        public int getLow() { return low; }
        
        public int getTotal() {
            return high + medium + low;
        }
    }
    
    /**
     * Type workload class
     * 类型工作负载类
     */
    public static class TypeWorkload {
        
        @JsonProperty("byType")
        private final List<TypeWorkloadItem> byType;
        
        public TypeWorkload(List<TypeWorkloadItem> byType) {
            this.byType = Objects.requireNonNull(byType, "By type cannot be null");
        }
        
        // Getters
        public List<TypeWorkloadItem> getByType() { return byType; }
    }
    
    /**
     * Type workload item class
     * 类型工作负载项目类
     */
    public static class TypeWorkloadItem {
        
        @JsonProperty("type")
        private final String type;
        
        @JsonProperty("count")
        private final int count;
        
        @JsonProperty("completionRate")
        private final double completionRate;
        
        public TypeWorkloadItem(String type, int count, double completionRate) {
            this.type = Objects.requireNonNull(type, "Type cannot be null");
            this.count = count;
            this.completionRate = completionRate;
        }
        
        // Getters
        public String getType() { return type; }
        public int getCount() { return count; }
        public double getCompletionRate() { return completionRate; }
    }
    
    /**
     * Time utilization class
     * 时间利用类
     */
    public static class TimeUtilization {
        
        @JsonProperty("estimatedTotalHours")
        private final double estimatedTotalHours;
        
        @JsonProperty("spentTotalHours")
        private final double spentTotalHours;
        
        @JsonProperty("remainingEstimatedHours")
        private final double remainingEstimatedHours;
        
        @JsonProperty("efficiencyRatio")
        private final double efficiencyRatio;
        
        public TimeUtilization(
                double estimatedTotalHours,
                double spentTotalHours,
                double remainingEstimatedHours,
                double efficiencyRatio) {
            
            this.estimatedTotalHours = estimatedTotalHours;
            this.spentTotalHours = spentTotalHours;
            this.remainingEstimatedHours = remainingEstimatedHours;
            this.efficiencyRatio = efficiencyRatio;
        }
        
        // Getters
        public double getEstimatedTotalHours() { return estimatedTotalHours; }
        public double getSpentTotalHours() { return spentTotalHours; }
        public double getRemainingEstimatedHours() { return remainingEstimatedHours; }
        public double getEfficiencyRatio() { return efficiencyRatio; }
    }
    
    /**
     * Capacity utilization class
     * 容量利用类
     */
    public static class CapacityUtilization {
        
        @JsonProperty("plannedCapacity")
        private final double plannedCapacity;
        
        @JsonProperty("actualUtilization")
        private final double actualUtilization;
        
        @JsonProperty("utilizationRate")
        private final double utilizationRate;
        
        @JsonProperty("capacityVariance")
        private final double capacityVariance;
        
        public CapacityUtilization(
                double plannedCapacity,
                double actualUtilization,
                double utilizationRate,
                double capacityVariance) {
            
            this.plannedCapacity = plannedCapacity;
            this.actualUtilization = actualUtilization;
            this.utilizationRate = utilizationRate;
            this.capacityVariance = capacityVariance;
        }
        
        // Getters
        public double getPlannedCapacity() { return plannedCapacity; }
        public double getActualUtilization() { return actualUtilization; }
        public double getUtilizationRate() { return utilizationRate; }
        public double getCapacityVariance() { return capacityVariance; }
    }
    
    /**
     * Predictive analytics class
     * 预测分析类
     */
    public static class PredictiveAnalytics {
        
        @JsonProperty("projectedCompletionDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate projectedCompletionDate;
        
        @JsonProperty("projectedCompletionPercentage")
        private final double projectedCompletionPercentage;
        
        @JsonProperty("confidenceLevel")
        private final double confidenceLevel;
        
        @JsonProperty("riskOfDelay")
        private final double riskOfDelay;
        
        @JsonProperty("estimatedRemainingDuration")
        private final int estimatedRemainingDuration;
        
        @JsonProperty("completionProbability")
        private final CompletionProbability completionProbability;
        
        @JsonProperty("criticalPathAnalysis")
        private final CriticalPathAnalysis criticalPathAnalysis;
        
        public PredictiveAnalytics(
                LocalDate projectedCompletionDate,
                double projectedCompletionPercentage,
                double confidenceLevel,
                double riskOfDelay,
                int estimatedRemainingDuration,
                CompletionProbability completionProbability,
                CriticalPathAnalysis criticalPathAnalysis) {
            
            this.projectedCompletionDate = projectedCompletionDate;
            this.projectedCompletionPercentage = projectedCompletionPercentage;
            this.confidenceLevel = confidenceLevel;
            this.riskOfDelay = riskOfDelay;
            this.estimatedRemainingDuration = estimatedRemainingDuration;
            this.completionProbability = Objects.requireNonNull(completionProbability, "Completion probability cannot be null");
            this.criticalPathAnalysis = Objects.requireNonNull(criticalPathAnalysis, "Critical path analysis cannot be null");
        }
        
        // Getters
        public LocalDate getProjectedCompletionDate() { return projectedCompletionDate; }
        public double getProjectedCompletionPercentage() { return projectedCompletionPercentage; }
        public double getConfidenceLevel() { return confidenceLevel; }
        public double getRiskOfDelay() { return riskOfDelay; }
        public int getEstimatedRemainingDuration() { return estimatedRemainingDuration; }
        public CompletionProbability getCompletionProbability() { return completionProbability; }
        public CriticalPathAnalysis getCriticalPathAnalysis() { return criticalPathAnalysis; }
    }
    
    /**
     * Completion probability class
     * 完成概率类
     */
    public static class CompletionProbability {
        
        @JsonProperty("onTimeCompletion")
        private final double onTimeCompletion;
        
        @JsonProperty("within10PercentDelay")
        private final double within10PercentDelay;
        
        @JsonProperty("within20PercentDelay")
        private final double within20PercentDelay;
        
        @JsonProperty("significantDelay")
        private final double significantDelay;
        
        public CompletionProbability(
                double onTimeCompletion,
                double within10PercentDelay,
                double within20PercentDelay,
                double significantDelay) {
            
            this.onTimeCompletion = onTimeCompletion;
            this.within10PercentDelay = within10PercentDelay;
            this.within20PercentDelay = within20PercentDelay;
            this.significantDelay = significantDelay;
        }
        
        // Getters
        public double getOnTimeCompletion() { return onTimeCompletion; }
        public double getWithin10PercentDelay() { return within10PercentDelay; }
        public double getWithin20PercentDelay() { return within20PercentDelay; }
        public double getSignificantDelay() { return significantDelay; }
    }
    
    /**
     * Critical path analysis class
     * 关键路径分析类
     */
    public static class CriticalPathAnalysis {
        
        @JsonProperty("criticalPathItems")
        private final List<CriticalPathItem> criticalPathItems;
        
        @JsonProperty("totalFloat")
        private final int totalFloat;
        
        @JsonProperty("criticalPathDuration")
        private final int criticalPathDuration;
        
        @JsonProperty("criticalityIndex")
        private final double criticalityIndex;
        
        public CriticalPathAnalysis(
                List<CriticalPathItem> criticalPathItems,
                int totalFloat,
                int criticalPathDuration,
                double criticalityIndex) {
            
            this.criticalPathItems = Objects.requireNonNull(criticalPathItems, "Critical path items cannot be null");
            this.totalFloat = totalFloat;
            this.criticalPathDuration = criticalPathDuration;
            this.criticalityIndex = criticalityIndex;
        }
        
        // Getters
        public List<CriticalPathItem> getCriticalPathItems() { return criticalPathItems; }
        public int getTotalFloat() { return totalFloat; }
        public int getCriticalPathDuration() { return criticalPathDuration; }
        public double getCriticalityIndex() { return criticalityIndex; }
    }
    
    /**
     * Critical path item class
     * 关键路径项目类
     */
    public static class CriticalPathItem {
        
        @JsonProperty("workPackageId")
        private final String workPackageId;
        
        @JsonProperty("subject")
        private final String subject;
        
        @JsonProperty("duration")
        private final int duration;
        
        @JsonProperty("float")
        private final int floatTime;
        
        @JsonProperty("isCritical")
        private final boolean isCritical;
        
        public CriticalPathItem(String workPackageId, String subject, int duration, int floatTime, boolean isCritical) {
            this.workPackageId = Objects.requireNonNull(workPackageId, "Work package ID cannot be null");
            this.subject = Objects.requireNonNull(subject, "Subject cannot be null");
            this.duration = duration;
            this.floatTime = floatTime;
            this.isCritical = isCritical;
        }
        
        // Getters
        public String getWorkPackageId() { return workPackageId; }
        public String getSubject() { return subject; }
        public int getDuration() { return duration; }
        public int getFloat() { return floatTime; }
        public boolean isCritical() { return isCritical; }
    }
    
    /**
     * Performance benchmarks class
     * 性能基准类
     */
    public static class PerformanceBenchmarks {
        
        @JsonProperty("industryBenchmarks")
        private final IndustryBenchmarks industryBenchmarks;
        
        @JsonProperty("historicalPerformance")
        private final HistoricalPerformance historicalPerformance;
        
        @JsonProperty("comparativeAnalysis")
        private final ComparativeAnalysis comparativeAnalysis;
        
        public PerformanceBenchmarks(
                IndustryBenchmarks industryBenchmarks,
                HistoricalPerformance historicalPerformance,
                ComparativeAnalysis comparativeAnalysis) {
            
            this.industryBenchmarks = Objects.requireNonNull(industryBenchmarks, "Industry benchmarks cannot be null");
            this.historicalPerformance = Objects.requireNonNull(historicalPerformance, "Historical performance cannot be null");
            this.comparativeAnalysis = Objects.requireNonNull(comparativeAnalysis, "Comparative analysis cannot be null");
        }
        
        // Getters
        public IndustryBenchmarks getIndustryBenchmarks() { return industryBenchmarks; }
        public HistoricalPerformance getHistoricalPerformance() { return historicalPerformance; }
        public ComparativeAnalysis getComparativeAnalysis() { return comparativeAnalysis; }
    }
    
    /**
     * Industry benchmarks class
     * 行业基准类
     */
    public static class IndustryBenchmarks {
        
        @JsonProperty("averageCompletionRate")
        private final double averageCompletionRate;
        
        @JsonProperty("averageTimeEfficiency")
        private final double averageTimeEfficiency;
        
        @JsonProperty("averageResourceUtilization")
        private final double averageResourceUtilization;
        
        @JsonProperty("industryStandard")
        private final String industryStandard;
        
        public IndustryBenchmarks(
                double averageCompletionRate,
                double averageTimeEfficiency,
                double averageResourceUtilization,
                String industryStandard) {
            
            this.averageCompletionRate = averageCompletionRate;
            this.averageTimeEfficiency = averageTimeEfficiency;
            this.averageResourceUtilization = averageResourceUtilization;
            this.industryStandard = Objects.requireNonNull(industryStandard, "Industry standard cannot be null");
        }
        
        // Getters
        public double getAverageCompletionRate() { return averageCompletionRate; }
        public double getAverageTimeEfficiency() { return averageTimeEfficiency; }
        public double getAverageResourceUtilization() { return averageResourceUtilization; }
        public String getIndustryStandard() { return industryStandard; }
    }
    
    /**
     * Historical performance class
     * 历史性能类
     */
    public static class HistoricalPerformance {
        
        @JsonProperty("previousProjects")
        private final List<HistoricalProject> previousProjects;
        
        @JsonProperty("averagePerformance")
        private final double averagePerformance;
        
        @JsonProperty("performanceTrend")
        private final TrendDirection performanceTrend;
        
        public HistoricalPerformance(
                List<HistoricalProject> previousProjects,
                double averagePerformance,
                TrendDirection performanceTrend) {
            
            this.previousProjects = Objects.requireNonNull(previousProjects, "Previous projects cannot be null");
            this.averagePerformance = averagePerformance;
            this.performanceTrend = Objects.requireNonNull(performanceTrend, "Performance trend cannot be null");
        }
        
        // Getters
        public List<HistoricalProject> getPreviousProjects() { return previousProjects; }
        public double getAveragePerformance() { return averagePerformance; }
        public TrendDirection getPerformanceTrend() { return performanceTrend; }
    }
    
    /**
     * Historical project class
     * 历史项目类
     */
    public static class HistoricalProject {
        
        @JsonProperty("projectName")
        private final String projectName;
        
        @JsonProperty("completionDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate completionDate;
        
        @JsonProperty("finalCompletionRate")
        private final double finalCompletionRate;
        
        @JsonProperty("timeEfficiency")
        private final double timeEfficiency;
        
        @JsonProperty("budgetEfficiency")
        private final double budgetEfficiency;
        
        public HistoricalProject(
                String projectName,
                LocalDate completionDate,
                double finalCompletionRate,
                double timeEfficiency,
                double budgetEfficiency) {
            
            this.projectName = Objects.requireNonNull(projectName, "Project name cannot be null");
            this.completionDate = completionDate;
            this.finalCompletionRate = finalCompletionRate;
            this.timeEfficiency = timeEfficiency;
            this.budgetEfficiency = budgetEfficiency;
        }
        
        // Getters
        public String getProjectName() { return projectName; }
        public LocalDate getCompletionDate() { return completionDate; }
        public double getFinalCompletionRate() { return finalCompletionRate; }
        public double getTimeEfficiency() { return timeEfficiency; }
        public double getBudgetEfficiency() { return budgetEfficiency; }
    }
    
    /**
     * Comparative analysis class
     * 比较分析类
     */
    public static class ComparativeAnalysis {
        
        @JsonProperty("currentVsIndustry")
        private final PerformanceComparison currentVsIndustry;
        
        @JsonProperty("currentVsHistorical")
        private final PerformanceComparison currentVsHistorical;
        
        @JsonProperty("performanceRating")
        private final PerformanceRating performanceRating;
        
        public ComparativeAnalysis(
                PerformanceComparison currentVsIndustry,
                PerformanceComparison currentVsHistorical,
                PerformanceRating performanceRating) {
            
            this.currentVsIndustry = Objects.requireNonNull(currentVsIndustry, "Current vs industry cannot be null");
            this.currentVsHistorical = Objects.requireNonNull(currentVsHistorical, "Current vs historical cannot be null");
            this.performanceRating = Objects.requireNonNull(performanceRating, "Performance rating cannot be null");
        }
        
        // Getters
        public PerformanceComparison getCurrentVsIndustry() { return currentVsIndustry; }
        public PerformanceComparison getCurrentVsHistorical() { return currentVsHistorical; }
        public PerformanceRating getPerformanceRating() { return performanceRating; }
    }
    
    /**
     * Performance comparison class
     * 性能比较类
     */
    public static class PerformanceComparison {
        
        @JsonProperty("completionRate")
        private final double completionRate;
        
        @JsonProperty("timeEfficiency")
        private final double timeEfficiency;
        
        @JsonProperty("resourceUtilization")
        private final double resourceUtilization;
        
        @JsonProperty("overallPerformance")
        private final double overallPerformance;
        
        public PerformanceComparison(
                double completionRate,
                double timeEfficiency,
                double resourceUtilization,
                double overallPerformance) {
            
            this.completionRate = completionRate;
            this.timeEfficiency = timeEfficiency;
            this.resourceUtilization = resourceUtilization;
            this.overallPerformance = overallPerformance;
        }
        
        // Getters
        public double getCompletionRate() { return completionRate; }
        public double getTimeEfficiency() { return timeEfficiency; }
        public double getResourceUtilization() { return resourceUtilization; }
        public double getOverallPerformance() { return overallPerformance; }
    }
    
    /**
     * Performance rating enumeration
     * 性能评级枚举
     */
    public enum PerformanceRating {
        EXCELLENT("优秀", "Performance is excellent"),
        GOOD("良好", "Performance is good"),
        AVERAGE("一般", "Performance is average"),
        BELOW_AVERAGE("低于平均", "Performance is below average"),
        POOR("较差", "Performance is poor");
        
        private final String chineseName;
        private final String description;
        
        PerformanceRating(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Analysis metadata class
     * 分析元数据类
     */
    public static class AnalysisMetadata {
        
        @JsonProperty("generatedAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private final String generatedAt;
        
        @JsonProperty("generatedBy")
        private final String generatedBy;
        
        @JsonProperty("version")
        private final String version;
        
        @JsonProperty("dataSources")
        private final List<String> dataSources;
        
        @JsonProperty("analysisMethodology")
        private final String analysisMethodology;
        
        @JsonProperty("confidenceLevel")
        private final double confidenceLevel;
        
        @JsonProperty("processingTime")
        private final long processingTime;
        
        public AnalysisMetadata(
                String generatedAt,
                String generatedBy,
                String version,
                List<String> dataSources,
                String analysisMethodology,
                double confidenceLevel,
                long processingTime) {
            
            this.generatedAt = Objects.requireNonNull(generatedAt, "Generated at cannot be null");
            this.generatedBy = Objects.requireNonNull(generatedBy, "Generated by cannot be null");
            this.version = Objects.requireNonNull(version, "Version cannot be null");
            this.dataSources = Objects.requireNonNull(dataSources, "Data sources cannot be null");
            this.analysisMethodology = Objects.requireNonNull(analysisMethodology, "Analysis methodology cannot be null");
            this.confidenceLevel = confidenceLevel;
            this.processingTime = processingTime;
        }
        
        // Getters
        public String getGeneratedAt() { return generatedAt; }
        public String getGeneratedBy() { return generatedBy; }
        public String getVersion() { return version; }
        public List<String> getDataSources() { return dataSources; }
        public String getAnalysisMethodology() { return analysisMethodology; }
        public double getConfidenceLevel() { return confidenceLevel; }
        public long getProcessingTime() { return processingTime; }
    }
    
    /**
     * Builder class for CompletionAnalysis
     * 完成度分析的构建器类
     * 
     * Provides a fluent API for constructing CompletionAnalysis instances
     * 提供流式API用于构建CompletionAnalysis实例
     */
    public static class CompletionAnalysisBuilder {
        private String projectId;
        private String projectName;
        private LocalDate analysisDate;
        private Double overallCompletion;
        private ProjectStatus projectStatus;
        private List<WorkPackageCompletion> workPackages = new ArrayList<>();
        private ProgressMetrics progressMetrics;
        private CompletionTrends completionTrends;
        private ResourceUtilization resourceUtilization;
        private PredictiveAnalytics predictions;
        private PerformanceBenchmarks benchmarks;
        private AnalysisMetadata metadata;
        
        /**
         * Sets the project identifier
         * 设置项目标识符
         */
        public CompletionAnalysisBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }
        
        /**
         * Sets the project name
         * 设置项目名称
         */
        public CompletionAnalysisBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }
        
        /**
         * Sets the analysis date
         * 设置分析日期
         */
        public CompletionAnalysisBuilder analysisDate(LocalDate analysisDate) {
            this.analysisDate = analysisDate;
            return this;
        }
        
        /**
         * Sets the overall completion percentage
         * 设置整体完成百分比
         */
        public CompletionAnalysisBuilder overallCompletion(double overallCompletion) {
            this.overallCompletion = overallCompletion;
            return this;
        }
        
        /**
         * Sets the project status
         * 设置项目状态
         */
        public CompletionAnalysisBuilder projectStatus(ProjectStatus projectStatus) {
            this.projectStatus = projectStatus;
            return this;
        }
        
        /**
         * Sets the work packages completion list
         * 设置工作包完成列表
         */
        public CompletionAnalysisBuilder workPackages(List<WorkPackageCompletion> workPackages) {
            this.workPackages = workPackages;
            return this;
        }
        
        /**
         * Adds a single work package completion
         * 添加单个工作包完成信息
         */
        public CompletionAnalysisBuilder addWorkPackage(WorkPackageCompletion workPackage) {
            this.workPackages.add(workPackage);
            return this;
        }
        
        /**
         * Sets the progress metrics
         * 设置进度指标
         */
        public CompletionAnalysisBuilder progressMetrics(ProgressMetrics progressMetrics) {
            this.progressMetrics = progressMetrics;
            return this;
        }
        
        /**
         * Sets the completion trends
         * 设置完成趋势
         */
        public CompletionAnalysisBuilder completionTrends(CompletionTrends completionTrends) {
            this.completionTrends = completionTrends;
            return this;
        }
        
        /**
         * Sets the resource utilization
         * 设置资源利用
         */
        public CompletionAnalysisBuilder resourceUtilization(ResourceUtilization resourceUtilization) {
            this.resourceUtilization = resourceUtilization;
            return this;
        }
        
        /**
         * Sets the predictive analytics
         * 设置预测分析
         */
        public CompletionAnalysisBuilder predictions(PredictiveAnalytics predictions) {
            this.predictions = predictions;
            return this;
        }
        
        /**
         * Sets the performance benchmarks
         * 设置性能基准
         */
        public CompletionAnalysisBuilder benchmarks(PerformanceBenchmarks benchmarks) {
            this.benchmarks = benchmarks;
            return this;
        }
        
        /**
         * Sets the analysis metadata
         * 设置分析元数据
         */
        public CompletionAnalysisBuilder metadata(AnalysisMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        /**
         * Builds the CompletionAnalysis instance
         * 构建CompletionAnalysis实例
         * 
         * @return CompletionAnalysis instance with all configured properties
         */
        public CompletionAnalysis build() {
            // Set defaults for required fields if not provided
            if (analysisDate == null) {
                analysisDate = LocalDate.now();
            }
            
            if (overallCompletion == null) {
                overallCompletion = calculateOverallCompletion(workPackages);
            }
            
            if (projectStatus == null) {
                projectStatus = determineProjectStatus(overallCompletion, workPackages);
            }
            
            if (progressMetrics == null) {
                progressMetrics = generateProgressMetrics(workPackages);
            }
            
            if (completionTrends == null) {
                completionTrends = generateCompletionTrends(workPackages);
            }
            
            if (resourceUtilization == null) {
                resourceUtilization = generateResourceUtilization(workPackages);
            }
            
            if (predictions == null) {
                predictions = generatePredictiveAnalytics(workPackages, overallCompletion);
            }
            
            if (benchmarks == null) {
                benchmarks = generatePerformanceBenchmarks(workPackages, overallCompletion);
            }
            
            if (metadata == null) {
                metadata = generateMetadata();
            }
            
            return new CompletionAnalysis(
                projectId,
                projectName,
                analysisDate,
                overallCompletion,
                projectStatus,
                workPackages,
                progressMetrics,
                completionTrends,
                resourceUtilization,
                predictions,
                benchmarks,
                metadata
            );
        }
        
        private double calculateOverallCompletion(List<WorkPackageCompletion> workPackages) {
            if (workPackages.isEmpty()) {
                return 0.0;
            }
            return workPackages.stream()
                .mapToInt(WorkPackageCompletion::getCompletionPercentage)
                .average()
                .orElse(0.0);
        }
        
        private ProjectStatus determineProjectStatus(double overallCompletion, List<WorkPackageCompletion> workPackages) {
            long overdueCount = workPackages.stream()
                .filter(WorkPackageCompletion::isOverdue)
                .count();
            
            if (overallCompletion >= 100) {
                return ProjectStatus.COMPLETED;
            } else if (overallCompletion >= 90) {
                return ProjectStatus.AHEAD_OF_SCHEDULE;
            } else if (overdueCount == 0) {
                return ProjectStatus.ON_TRACK;
            } else if (overdueCount <= workPackages.size() * 0.1) {
                return ProjectStatus.AT_RISK;
            } else {
                return ProjectStatus.DELAYED;
            }
        }
        
        private ProgressMetrics generateProgressMetrics(List<WorkPackageCompletion> workPackages) {
            int total = workPackages.size();
            int completed = (int) workPackages.stream()
                .filter(wp -> wp.getCompletionPercentage() >= 100)
                .count();
            int inProgress = (int) workPackages.stream()
                .filter(wp -> wp.getCompletionPercentage() > 0 && wp.getCompletionPercentage() < 100)
                .count();
            int notStarted = (int) workPackages.stream()
                .filter(wp -> wp.getCompletionPercentage() == 0)
                .count();
            int overdue = (int) workPackages.stream()
                .filter(WorkPackageCompletion::isOverdue)
                .count();
            
            double avgCompletion = workPackages.isEmpty() ? 0.0 :
                workPackages.stream()
                    .mapToInt(WorkPackageCompletion::getCompletionPercentage)
                    .average()
                    .orElse(0.0);
            
            return new ProgressMetrics(
                total, completed, inProgress, notStarted, overdue,
                avgCompletion, avgCompletion, 0.0, 0.0, 1.0, 0.9
            );
        }
        
        private CompletionTrends generateCompletionTrends(List<WorkPackageCompletion> workPackages) {
            List<CompletionHistoryPoint> history = new ArrayList<>();
            history.add(new CompletionHistoryPoint(
                LocalDate.now(), 
                calculateOverallCompletion(workPackages),
                (int) workPackages.stream().filter(wp -> wp.getCompletionPercentage() >= 100).count(),
                1.0
            ));
            
            return new CompletionTrends(
                TrendDirection.STABLE,
                history,
                new VelocityTrend(1.0, 1.0, TrendDirection.STABLE, 1.0),
                1.0,
                0.0
            );
        }
        
        private ResourceUtilization generateResourceUtilization(List<WorkPackageCompletion> workPackages) {
            Map<String, List<WorkPackageCompletion>> byAssignee = workPackages.stream()
                .filter(wp -> wp.getAssignee() != null)
                .collect(Collectors.groupingBy(WorkPackageCompletion::getAssignee));
            
            List<AssigneeWorkload> assigneeWorkloads = byAssignee.entrySet().stream()
                .map(entry -> {
                    List<WorkPackageCompletion> assigneeWps = entry.getValue();
                    int total = assigneeWps.size();
                    int completed = (int) assigneeWps.stream()
                        .filter(wp -> wp.getCompletionPercentage() >= 100)
                        .count();
                    int inProgress = (int) assigneeWps.stream()
                        .filter(wp -> wp.getCompletionPercentage() > 0 && wp.getCompletionPercentage() < 100)
                        .count();
                    
                    return new AssigneeWorkload(
                        entry.getKey(),
                        total,
                        completed,
                        inProgress,
                        (double) total / workPackages.size() * 100,
                        assigneeWps.stream()
                            .mapToInt(WorkPackageCompletion::getCompletionPercentage)
                            .average()
                            .orElse(0.0)
                    );
                })
                .collect(Collectors.toList());
            
            return new ResourceUtilization(
                0.85,
                0.9,
                new WorkloadDistribution(
                    assigneeWorkloads,
                    new PriorityWorkload(0, 0, 0),
                    new TypeWorkload(new ArrayList<>())
                ),
                new TimeUtilization(0, 0, 0, 1.0),
                new CapacityUtilization(100, 85, 0.85, 0.15)
            );
        }
        
        private PredictiveAnalytics generatePredictiveAnalytics(List<WorkPackageCompletion> workPackages, double overallCompletion) {
            return new PredictiveAnalytics(
                LocalDate.now().plusDays((int) ((100 - overallCompletion) * 2)),
                overallCompletion + 5,
                0.8,
                0.2,
                (int) ((100 - overallCompletion) * 2),
                new CompletionProbability(0.7, 0.85, 0.95, 0.05),
                new CriticalPathAnalysis(new ArrayList<>(), 10, 30, 0.8)
            );
        }
        
        private PerformanceBenchmarks generatePerformanceBenchmarks(List<WorkPackageCompletion> workPackages, double overallCompletion) {
            return new PerformanceBenchmarks(
                new IndustryBenchmarks(0.75, 0.85, 0.8, "Standard"),
                new HistoricalPerformance(new ArrayList<>(), 0.8, TrendDirection.STABLE),
                new ComparativeAnalysis(
                    new PerformanceComparison(overallCompletion - 0.75, 0.05, 0.05, 0.05),
                    new PerformanceComparison(0.0, 0.0, 0.0, 0.0),
                    PerformanceRating.GOOD
                )
            );
        }
        
        private AnalysisMetadata generateMetadata() {
            return new AnalysisMetadata(
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                "OpenProject MCP Server",
                "1.0.0",
                List.of("OpenProject API v3"),
                "Automated completion analysis",
                0.9,
                System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Static factory method to create CompletionAnalysis from WorkPackage data
     * 从WorkPackage数据创建CompletionAnalysis的静态工厂方法
     * 
     * @param projectId Project identifier
     * @param projectName Project name
     * @param workPackages List of work packages to analyze
     * @return CompletionAnalysis instance with auto-generated analysis
     */
    public static CompletionAnalysis createFromWorkPackages(
            String projectId, 
            String projectName, 
            List<WorkPackage> workPackages) {
        
        if (workPackages == null || workPackages.isEmpty()) {
            return new CompletionAnalysisBuilder()
                .projectId(projectId)
                .projectName(projectName)
                .workPackages(new ArrayList<>())
                .build();
        }
        
        List<WorkPackageCompletion> completions = workPackages.stream()
            .map(CompletionAnalysis::convertToWorkPackageCompletion)
            .collect(Collectors.toList());
        
        return new CompletionAnalysisBuilder()
            .projectId(projectId)
            .projectName(projectName)
            .workPackages(completions)
            .build();
    }
    
    /**
     * Converts a WorkPackage to WorkPackageCompletion
     * 将WorkPackage转换为WorkPackageCompletion
     * 
     * @param workPackage The work package to convert
     * @return WorkPackageCompletion instance
     */
    private static WorkPackageCompletion convertToWorkPackageCompletion(WorkPackage workPackage) {
        LocalDate today = LocalDate.now();
        boolean isOverdue = workPackage.isOverdue();
        int daysOverdue = isOverdue ? (int) Math.abs(today.until(workPackage.dueDate()).getDays()) : 0;
        int daysUntilDue = !isOverdue && workPackage.dueDate() != null ? 
            (int) today.until(workPackage.dueDate()).getDays() : 0;
        
        // Calculate time efficiency
        Double timeEfficiency = null;
        if (workPackage.estimatedTime() != null && workPackage.spentTime() != null && workPackage.estimatedTime() > 0) {
            timeEfficiency = workPackage.estimatedTime() / workPackage.spentTime();
        }
        
        // Determine progress trend based on completion percentage and due date
        ProgressTrend progressTrend = determineProgressTrend(workPackage);
        
        return new WorkPackageCompletion(
            workPackage.id(),
            workPackage.subject(),
            workPackage.type() != null ? workPackage.type().name() : "Unknown",
            workPackage.status() != null ? workPackage.status().name() : "Unknown",
            workPackage.percentageDone() != null ? workPackage.percentageDone() : 0,
            workPackage.getProgressDescription(),
            workPackage.assignee() != null ? workPackage.assignee().name() : null,
            workPackage.dueDate(),
            workPackage.startDate(),
            workPackage.isCompleted() ? today : null,
            workPackage.estimatedTime(),
            workPackage.spentTime(),
            timeEfficiency,
            isOverdue,
            daysOverdue,
            daysUntilDue,
            workPackage.priority() != null ? workPackage.priority().name() : "Medium",
            progressTrend,
            workPackage.updatedAt() != null ? workPackage.updatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : 
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }
    
    /**
     * Determines the progress trend for a work package
     * 确定工作包的进度趋势
     * 
     * @param workPackage The work package to analyze
     * @return ProgressTrend enum value
     */
    private static ProgressTrend determineProgressTrend(WorkPackage workPackage) {
        if (workPackage.percentageDone() == null) {
            return ProgressTrend.UNKNOWN;
        }
        
        int completion = workPackage.percentageDone();
        LocalDate today = LocalDate.now();
        
        if (workPackage.dueDate() == null) {
            return completion > 50 ? ProgressTrend.STEADY : ProgressTrend.STALLED;
        }
        
        long daysUntilDue = today.until(workPackage.dueDate()).getDays();
        
        if (completion >= 100) {
            return ProgressTrend.ACCELERATING;
        } else if (daysUntilDue < 0) {
            return completion > 0 ? ProgressTrend.DECELERATING : ProgressTrend.STALLED;
        } else if (daysUntilDue <= 3 && completion < 75) {
            return ProgressTrend.DECELERATING;
        } else if (completion >= 80) {
            return ProgressTrend.ACCELERATING;
        } else if (completion >= 50) {
            return ProgressTrend.STEADY;
        } else {
            return ProgressTrend.STALLED;
        }
    }
    
    /**
     * Creates a basic CompletionAnalysis with minimal configuration
     * 创建基本配置的CompletionAnalysis
     * 
     * @param projectId Project identifier
     * @param projectName Project name
     * @return CompletionAnalysis instance with default values
     */
    public static CompletionAnalysis createBasic(String projectId, String projectName) {
        return new CompletionAnalysisBuilder()
            .projectId(projectId)
            .projectName(projectName)
            .build();
    }
    
    /**
     * Business logic method to get completion summary
     * 获取完成摘要的业务逻辑方法
     * 
     * @return Summary string with key completion metrics
     */
    public String getCompletionSummary() {
        return String.format(
            "项目完成度分析: %s (%s) - 整体完成率: %.1f%%, 状态: %s, 已完成: %d/%d, 按计划: %s",
            projectName,
            projectId,
            overallCompletion,
            projectStatus.getChineseName(),
            getCompletedWorkPackagesCount(),
            workPackages.size(),
            isOnTrack() ? "是" : "否"
        );
    }
    
    /**
     * Business logic method to identify critical issues
     * 识别关键问题的业务逻辑方法
     * 
     * @return List of critical issues requiring attention
     */
    public List<String> identifyCriticalIssues() {
        List<String> issues = new ArrayList<>();
        
        if (hasRisks()) {
            issues.add(String.format("项目状态风险: %s", projectStatus.getChineseName()));
        }
        
        long overdueCount = getOverdueWorkPackagesCount();
        if (overdueCount > 0) {
            issues.add(String.format("逾期工作包: %d个", overdueCount));
        }
        
        long highPriorityInProgress = workPackages.stream()
            .filter(wp -> wp.isHighPriority() && wp.isInProgress())
            .count();
        if (highPriorityInProgress > 0) {
            issues.add(String.format("高优先级进行中: %d个", highPriorityInProgress));
        }
        
        if (overallCompletion < 50 && !workPackages.isEmpty()) {
            issues.add(String.format("完成率偏低: %.1f%%", overallCompletion));
        }
        
        return issues;
    }
    
    /**
     * Business logic method to generate performance insights
     * 生成性能洞察的业务逻辑方法
     * 
     * @return List of performance insights and recommendations
     */
    public List<String> generatePerformanceInsights() {
        List<String> insights = new ArrayList<>();
        
        // Resource utilization insights
        if (resourceUtilization.getTeamUtilization() < 0.7) {
            insights.add("团队利用率较低，考虑重新分配任务");
        } else if (resourceUtilization.getTeamUtilization() > 0.9) {
            insights.add("团队利用率较高，注意资源过载风险");
        }
        
        // Progress insights
        double avgCompletion = getAverageCompletionPercentage();
        if (avgCompletion > 80) {
            insights.add("项目进展良好，继续保持当前节奏");
        } else if (avgCompletion < 30) {
            insights.add("项目进展较慢，建议检查瓶颈和障碍");
        }
        
        // Time efficiency insights
        double timeEfficiency = progressMetrics.getProductivityIndex();
        if (timeEfficiency > 1.2) {
            insights.add("时间效率优秀，团队生产力高");
        } else if (timeEfficiency < 0.8) {
            insights.add("时间效率有待提升，建议优化工作流程");
        }
        
        // Risk insights
        if (predictions.getRiskOfDelay() > 0.3) {
            insights.add("项目延期风险较高，建议制定风险缓解计划");
        }
        
        return insights;
    }
    
    /**
     * Business logic method to check if completion analysis is valid
     * 检查完成度分析是否有效的业务逻辑方法
     * 
     * @return true if the analysis contains valid data
     */
    public boolean isValid() {
        return projectId != null && !projectId.trim().isEmpty() &&
               projectName != null && !projectName.trim().isEmpty() &&
               analysisDate != null &&
               projectStatus != null &&
               workPackages != null &&
               progressMetrics != null &&
               completionTrends != null &&
               resourceUtilization != null &&
               predictions != null &&
               benchmarks != null &&
               metadata != null;
    }
    
    /**
     * Business logic method to export analysis as JSON
     * 将分析导出为JSON的业务逻辑方法
     * 
     * @return JSON representation of the completion analysis
     */
    public String toJson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\": \"Failed to convert to JSON: " + e.getMessage() + "\"}";
        }
    }
}