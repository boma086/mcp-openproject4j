package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Risk Analysis Model for Project Risk Assessment
 * 项目风险评估的风险分析模型
 * 
 * This model represents a comprehensive risk analysis report generated from OpenProject data.
 * It includes risk identification, assessment, mitigation strategies, and trend analysis.
 * 
 * 该模型表示从OpenProject数据生成的综合风险评估报告。它包括风险识别、评估、缓解策略和趋势分析。
 * 
 * Key features:
 * - Comprehensive risk assessment with multiple risk dimensions
 * - Risk level categorization with quantitative scoring
 * - Mitigation strategy tracking and effectiveness monitoring
 * - Trend analysis for risk evolution over time
 * - Integration with WorkPackage model for data consistency
 * 
 * 主要特性：
 * - 多维度的综合风险评估
 * - 风险级别分类和量化评分
 * - 缓解策略跟踪和效果监控
 * - 随时间变化的风险趋势分析
 * - 与WorkPackage模型集成以确保数据一致性
 */
public class RiskAnalysis {
    
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
     * Date when the risk analysis was generated
     * 风险分析生成日期
     */
    @JsonProperty("analysisDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate analysisDate;
    
    /**
     * Overall risk score for the project (0-100)
     * 项目的整体风险评分（0-100）
     */
    @JsonProperty("overallRiskScore")
    private final int overallRiskScore;
    
    /**
     * Overall risk level classification
     * 整体风险级别分类
     */
    @JsonProperty("overallRiskLevel")
    private final RiskLevel overallRiskLevel;
    
    /**
     * List of identified risks with detailed information
     * 识别风险的详细信息列表
     */
    @JsonProperty("identifiedRisks")
    private final List<RiskItem> identifiedRisks;
    
    /**
     * Risk distribution by category
     * 按类别分类的风险分布
     */
    @JsonProperty("riskDistribution")
    private final RiskDistribution riskDistribution;
    
    /**
     * Risk trends and changes over time
     * 风险趋势和随时间的变化
     */
    @JsonProperty("riskTrends")
    private final RiskTrends riskTrends;
    
    /**
     * Recommended mitigation strategies
     * 推荐的缓解策略
     */
    @JsonProperty("mitigationStrategies")
    private final List<MitigationStrategy> mitigationStrategies;
    
    /**
     * Key risk indicators and metrics
     * 关键风险指标和度量
     */
    @JsonProperty("keyRiskIndicators")
    private final KeyRiskIndicators keyRiskIndicators;
    
    /**
     * Analysis metadata for tracking and validation
     * 用于跟踪和验证的分析元数据
     */
    @JsonProperty("metadata")
    private final AnalysisMetadata metadata;
    
    /**
     * Constructor for RiskAnalysis
     * 风险分析构造函数
     */
    public RiskAnalysis(
            String projectId,
            String projectName,
            LocalDate analysisDate,
            int overallRiskScore,
            RiskLevel overallRiskLevel,
            List<RiskItem> identifiedRisks,
            RiskDistribution riskDistribution,
            RiskTrends riskTrends,
            List<MitigationStrategy> mitigationStrategies,
            KeyRiskIndicators keyRiskIndicators,
            AnalysisMetadata metadata) {
        
        this.projectId = Objects.requireNonNull(projectId, "Project ID cannot be null");
        this.projectName = Objects.requireNonNull(projectName, "Project name cannot be null");
        this.analysisDate = Objects.requireNonNull(analysisDate, "Analysis date cannot be null");
        this.overallRiskScore = overallRiskScore;
        this.overallRiskLevel = Objects.requireNonNull(overallRiskLevel, "Overall risk level cannot be null");
        this.identifiedRisks = Objects.requireNonNull(identifiedRisks, "Identified risks list cannot be null");
        this.riskDistribution = Objects.requireNonNull(riskDistribution, "Risk distribution cannot be null");
        this.riskTrends = Objects.requireNonNull(riskTrends, "Risk trends cannot be null");
        this.mitigationStrategies = Objects.requireNonNull(mitigationStrategies, "Mitigation strategies list cannot be null");
        this.keyRiskIndicators = Objects.requireNonNull(keyRiskIndicators, "Key risk indicators cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
    }
    
    // Getters
    public String getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public LocalDate getAnalysisDate() { return analysisDate; }
    public int getOverallRiskScore() { return overallRiskScore; }
    public RiskLevel getOverallRiskLevel() { return overallRiskLevel; }
    public List<RiskItem> getIdentifiedRisks() { return identifiedRisks; }
    public RiskDistribution getRiskDistribution() { return riskDistribution; }
    public RiskTrends getRiskTrends() { return riskTrends; }
    public List<MitigationStrategy> getMitigationStrategies() { return mitigationStrategies; }
    public KeyRiskIndicators getKeyRiskIndicators() { return keyRiskIndicators; }
    public AnalysisMetadata getMetadata() { return metadata; }
    
    /**
     * Gets the count of high-risk items
     * 获取高风险项目的数量
     */
    public long getHighRiskCount() {
        return identifiedRisks.stream()
            .filter(risk -> risk.getRiskLevel() == RiskLevel.HIGH)
            .count();
    }
    
    /**
     * Gets the count of medium-risk items
     * 获取中等风险项目的数量
     */
    public long getMediumRiskCount() {
        return identifiedRisks.stream()
            .filter(risk -> risk.getRiskLevel() == RiskLevel.MEDIUM)
            .count();
    }
    
    /**
     * Gets the count of low-risk items
     * 获取低风险项目的数量
     */
    public long getLowRiskCount() {
        return identifiedRisks.stream()
            .filter(risk -> risk.getRiskLevel() == RiskLevel.LOW)
            .count();
    }
    
    /**
     * Checks if the analysis has any critical risks
     * 检查分析是否有任何关键风险
     */
    public boolean hasCriticalRisks() {
        return identifiedRisks.stream()
            .anyMatch(RiskItem::isCritical);
    }
    
    /**
     * Gets the most urgent risks requiring immediate attention
     * 获取需要立即关注的最紧急风险
     */
    public List<RiskItem> getUrgentRisks() {
        return identifiedRisks.stream()
            .filter(risk -> risk.getRiskLevel() == RiskLevel.HIGH && risk.isCritical())
            .toList();
    }
    
    /**
     * Nested class for individual risk items
     * 单个风险项目的嵌套类
     */
    public static class RiskItem {
        
        @JsonProperty("id")
        private final String id;
        
        @JsonProperty("workPackageId")
        private final String workPackageId;
        
        @JsonProperty("workPackageSubject")
        private final String workPackageSubject;
        
        @JsonProperty("riskType")
        private final RiskType riskType;
        
        @JsonProperty("riskLevel")
        private final RiskLevel riskLevel;
        
        @JsonProperty("riskScore")
        private final int riskScore;
        
        @JsonProperty("description")
        private final String description;
        
        @JsonProperty("probability")
        private final Probability probability;
        
        @JsonProperty("impact")
        private final Impact impact;
        
        @JsonProperty("identifiedDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate identifiedDate;
        
        @JsonProperty("dueDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate dueDate;
        
        @JsonProperty("assignee")
        private final String assignee;
        
        @JsonProperty("isCritical")
        private final boolean isCritical;
        
        @JsonProperty("mitigationStatus")
        private final MitigationStatus mitigationStatus;
        
        @JsonProperty("lastUpdated")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private final String lastUpdated;
        
        public RiskItem(
                String id,
                String workPackageId,
                String workPackageSubject,
                RiskType riskType,
                RiskLevel riskLevel,
                int riskScore,
                String description,
                Probability probability,
                Impact impact,
                LocalDate identifiedDate,
                LocalDate dueDate,
                String assignee,
                boolean isCritical,
                MitigationStatus mitigationStatus,
                String lastUpdated) {
            
            this.id = Objects.requireNonNull(id, "Risk ID cannot be null");
            this.workPackageId = Objects.requireNonNull(workPackageId, "Work package ID cannot be null");
            this.workPackageSubject = Objects.requireNonNull(workPackageSubject, "Work package subject cannot be null");
            this.riskType = Objects.requireNonNull(riskType, "Risk type cannot be null");
            this.riskLevel = Objects.requireNonNull(riskLevel, "Risk level cannot be null");
            this.riskScore = riskScore;
            this.description = Objects.requireNonNull(description, "Description cannot be null");
            this.probability = Objects.requireNonNull(probability, "Probability cannot be null");
            this.impact = Objects.requireNonNull(impact, "Impact cannot be null");
            this.identifiedDate = Objects.requireNonNull(identifiedDate, "Identified date cannot be null");
            this.dueDate = dueDate;
            this.assignee = assignee;
            this.isCritical = isCritical;
            this.mitigationStatus = Objects.requireNonNull(mitigationStatus, "Mitigation status cannot be null");
            this.lastUpdated = Objects.requireNonNull(lastUpdated, "Last updated cannot be null");
        }
        
        // Getters
        public String getId() { return id; }
        public String getWorkPackageId() { return workPackageId; }
        public String getWorkPackageSubject() { return workPackageSubject; }
        public RiskType getRiskType() { return riskType; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public int getRiskScore() { return riskScore; }
        public String getDescription() { return description; }
        public Probability getProbability() { return probability; }
        public Impact getImpact() { return impact; }
        public LocalDate getIdentifiedDate() { return identifiedDate; }
        public LocalDate getDueDate() { return dueDate; }
        public String getAssignee() { return assignee; }
        public boolean isCritical() { return isCritical; }
        public MitigationStatus getMitigationStatus() { return mitigationStatus; }
        public String getLastUpdated() { return lastUpdated; }
        
        // Business logic methods
        public boolean isOverdue() {
            return dueDate != null && dueDate.isBefore(LocalDate.now());
        }
        
        public boolean requiresImmediateAttention() {
            return isCritical || (riskLevel == RiskLevel.HIGH && isOverdue());
        }
        
        public int getDaysUntilDue() {
            if (dueDate == null) {
                return Integer.MAX_VALUE;
            }
            return (int) LocalDate.now().until(dueDate).getDays();
        }
    }
    
    /**
     * Risk type enumeration
     * 风险类型枚举
     */
    public enum RiskType {
        SCHEDULE("进度风险", "Schedule delays and timeline risks"),
        COST("成本风险", "Budget overruns and cost escalation"),
        TECHNICAL("技术风险", "Technical challenges and feasibility issues"),
        RESOURCE("资源风险", "Resource availability and skill gaps"),
        SCOPE("范围风险", "Scope creep and requirement changes"),
        QUALITY("质量风险", "Quality defects and standards non-compliance"),
        EXTERNAL("外部风险", "External factors and market changes"),
        DEPENDENCY("依赖风险", "Third-party dependencies and integration issues");
        
        private final String chineseName;
        private final String description;
        
        RiskType(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Risk level enumeration
     * 风险级别枚举
     */
    public enum RiskLevel {
        LOW("低", "Low risk - Monitor and review periodically"),
        MEDIUM("中", "Medium risk - Active monitoring and mitigation planning"),
        HIGH("高", "High risk - Immediate attention and active mitigation"),
        CRITICAL("关键", "Critical risk - Immediate action required, potential project failure");
        
        private final String chineseName;
        private final String description;
        
        RiskLevel(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Probability enumeration
     * 概率枚举
     */
    public enum Probability {
        RARE("罕见", "0-10% chance of occurrence"),
        UNLIKELY("不太可能", "11-30% chance of occurrence"),
        POSSIBLE("可能", "31-60% chance of occurrence"),
        LIKELY("很可能", "61-90% chance of occurrence"),
        ALMOST_CERTAIN("几乎确定", "91-100% chance of occurrence");
        
        private final String chineseName;
        private final String description;
        
        Probability(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Impact enumeration
     * 影响枚举
     */
    public enum Impact {
        MINIMAL("最小", "Minimal impact on project objectives"),
        MINOR("较小", "Minor impact on project objectives"),
        MODERATE("中等", "Moderate impact on project objectives"),
        MAJOR("较大", "Major impact on project objectives"),
        SEVERE("严重", "Severe impact on project objectives");
        
        private final String chineseName;
        private final String description;
        
        Impact(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Mitigation status enumeration
     * 缓解状态枚举
     */
    public enum MitigationStatus {
        NOT_STARTED("未开始", "No mitigation actions initiated"),
        IN_PROGRESS("进行中", "Mitigation actions in progress"),
        COMPLETED("已完成", "Mitigation actions completed"),
        MONITORING("监控中", "Mitigation completed and monitoring"),
        DEFERRED("已推迟", "Mitigation deferred to later date"),
        CANCELLED("已取消", "Mitigation cancelled - risk accepted");
        
        private final String chineseName;
        private final String description;
        
        MitigationStatus(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Risk distribution class
     * 风险分布类
     */
    public static class RiskDistribution {
        
        @JsonProperty("byRiskLevel")
        private final RiskLevelDistribution byRiskLevel;
        
        @JsonProperty("byRiskType")
        private final RiskTypeDistribution byRiskType;
        
        @JsonProperty("byAssignee")
        private final AssigneeDistribution byAssignee;
        
        public RiskDistribution(
                RiskLevelDistribution byRiskLevel,
                RiskTypeDistribution byRiskType,
                AssigneeDistribution byAssignee) {
            
            this.byRiskLevel = Objects.requireNonNull(byRiskLevel, "Risk level distribution cannot be null");
            this.byRiskType = Objects.requireNonNull(byRiskType, "Risk type distribution cannot be null");
            this.byAssignee = Objects.requireNonNull(byAssignee, "Assignee distribution cannot be null");
        }
        
        // Getters
        public RiskLevelDistribution getByRiskLevel() { return byRiskLevel; }
        public RiskTypeDistribution getByRiskType() { return byRiskType; }
        public AssigneeDistribution getByAssignee() { return byAssignee; }
    }
    
    /**
     * Risk level distribution class
     * 风险级别分布类
     */
    public static class RiskLevelDistribution {
        
        @JsonProperty("low")
        private final int low;
        
        @JsonProperty("medium")
        private final int medium;
        
        @JsonProperty("high")
        private final int high;
        
        @JsonProperty("critical")
        private final int critical;
        
        public RiskLevelDistribution(int low, int medium, int high, int critical) {
            this.low = low;
            this.medium = medium;
            this.high = high;
            this.critical = critical;
        }
        
        // Getters
        public int getLow() { return low; }
        public int getMedium() { return medium; }
        public int getHigh() { return high; }
        public int getCritical() { return critical; }
        
        public int getTotal() {
            return low + medium + high + critical;
        }
    }
    
    /**
     * Risk type distribution class
     * 风险类型分布类
     */
    public static class RiskTypeDistribution {
        
        @JsonProperty("schedule")
        private final int schedule;
        
        @JsonProperty("cost")
        private final int cost;
        
        @JsonProperty("technical")
        private final int technical;
        
        @JsonProperty("resource")
        private final int resource;
        
        @JsonProperty("scope")
        private final int scope;
        
        @JsonProperty("quality")
        private final int quality;
        
        @JsonProperty("external")
        private final int external;
        
        @JsonProperty("dependency")
        private final int dependency;
        
        public RiskTypeDistribution(int schedule, int cost, int technical, int resource,
                                   int scope, int quality, int external, int dependency) {
            this.schedule = schedule;
            this.cost = cost;
            this.technical = technical;
            this.resource = resource;
            this.scope = scope;
            this.quality = quality;
            this.external = external;
            this.dependency = dependency;
        }
        
        // Getters
        public int getSchedule() { return schedule; }
        public int getCost() { return cost; }
        public int getTechnical() { return technical; }
        public int getResource() { return resource; }
        public int getScope() { return scope; }
        public int getQuality() { return quality; }
        public int getExternal() { return external; }
        public int getDependency() { return dependency; }
        
        public int getTotal() {
            return schedule + cost + technical + resource + scope + quality + external + dependency;
        }
    }
    
    /**
     * Assignee distribution class
     * 分配者分布类
     */
    public static class AssigneeDistribution {
        
        @JsonProperty("unassigned")
        private final int unassigned;
        
        @JsonProperty("assigned")
        private final List<AssigneeItem> assigned;
        
        public AssigneeDistribution(int unassigned, List<AssigneeItem> assigned) {
            this.unassigned = unassigned;
            this.assigned = Objects.requireNonNull(assigned, "Assigned list cannot be null");
        }
        
        // Getters
        public int getUnassigned() { return unassigned; }
        public List<AssigneeItem> getAssigned() { return assigned; }
    }
    
    /**
     * Assignee item class
     * 分配者项目类
     */
    public static class AssigneeItem {
        
        @JsonProperty("assigneeName")
        private final String assigneeName;
        
        @JsonProperty("riskCount")
        private final int riskCount;
        
        @JsonProperty("highRiskCount")
        private final int highRiskCount;
        
        public AssigneeItem(String assigneeName, int riskCount, int highRiskCount) {
            this.assigneeName = Objects.requireNonNull(assigneeName, "Assignee name cannot be null");
            this.riskCount = riskCount;
            this.highRiskCount = highRiskCount;
        }
        
        // Getters
        public String getAssigneeName() { return assigneeName; }
        public int getRiskCount() { return riskCount; }
        public int getHighRiskCount() { return highRiskCount; }
    }
    
    /**
     * Risk trends class
     * 风险趋势类
     */
    public static class RiskTrends {
        
        @JsonProperty("overallTrend")
        private final TrendDirection overallTrend;
        
        @JsonProperty("riskScoreHistory")
        private final List<RiskScorePoint> riskScoreHistory;
        
        @JsonProperty("riskCountTrend")
        private final RiskCountTrend riskCountTrend;
        
        @JsonProperty("mitigationEffectiveness")
        private final double mitigationEffectiveness;
        
        public RiskTrends(
                TrendDirection overallTrend,
                List<RiskScorePoint> riskScoreHistory,
                RiskCountTrend riskCountTrend,
                double mitigationEffectiveness) {
            
            this.overallTrend = Objects.requireNonNull(overallTrend, "Overall trend cannot be null");
            this.riskScoreHistory = Objects.requireNonNull(riskScoreHistory, "Risk score history cannot be null");
            this.riskCountTrend = Objects.requireNonNull(riskCountTrend, "Risk count trend cannot be null");
            this.mitigationEffectiveness = mitigationEffectiveness;
        }
        
        // Getters
        public TrendDirection getOverallTrend() { return overallTrend; }
        public List<RiskScorePoint> getRiskScoreHistory() { return riskScoreHistory; }
        public RiskCountTrend getRiskCountTrend() { return riskCountTrend; }
        public double getMitigationEffectiveness() { return mitigationEffectiveness; }
    }
    
    /**
     * Trend direction enumeration
     * 趋势方向枚举
     */
    public enum TrendDirection {
        IMPROVING("改善", "Risk profile is improving"),
        STABLE("稳定", "Risk profile is stable"),
        DETERIORATING("恶化", "Risk profile is deteriorating"),
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
     * Risk score point class
     * 风险评分点类
     */
    public static class RiskScorePoint {
        
        @JsonProperty("date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate date;
        
        @JsonProperty("riskScore")
        private final int riskScore;
        
        @JsonProperty("highRiskCount")
        private final int highRiskCount;
        
        public RiskScorePoint(LocalDate date, int riskScore, int highRiskCount) {
            this.date = Objects.requireNonNull(date, "Date cannot be null");
            this.riskScore = riskScore;
            this.highRiskCount = highRiskCount;
        }
        
        // Getters
        public LocalDate getDate() { return date; }
        public int getRiskScore() { return riskScore; }
        public int getHighRiskCount() { return highRiskCount; }
    }
    
    /**
     * Risk count trend class
     * 风险数量趋势类
     */
    public static class RiskCountTrend {
        
        @JsonProperty("newRisks")
        private final int newRisks;
        
        @JsonProperty("mitigatedRisks")
        private final int mitigatedRisks;
        
        @JsonProperty("escalatedRisks")
        private final int escalatedRisks;
        
        @JsonProperty("netChange")
        private final int netChange;
        
        public RiskCountTrend(int newRisks, int mitigatedRisks, int escalatedRisks, int netChange) {
            this.newRisks = newRisks;
            this.mitigatedRisks = mitigatedRisks;
            this.escalatedRisks = escalatedRisks;
            this.netChange = netChange;
        }
        
        // Getters
        public int getNewRisks() { return newRisks; }
        public int getMitigatedRisks() { return mitigatedRisks; }
        public int getEscalatedRisks() { return escalatedRisks; }
        public int getNetChange() { return netChange; }
    }
    
    /**
     * Mitigation strategy class
     * 缓解策略类
     */
    public static class MitigationStrategy {
        
        @JsonProperty("id")
        private final String id;
        
        @JsonProperty("strategyType")
        private final StrategyType strategyType;
        
        @JsonProperty("description")
        private final String description;
        
        @JsonProperty("targetRiskTypes")
        private final List<RiskType> targetRiskTypes;
        
        @JsonProperty("implementationCost")
        private final CostLevel implementationCost;
        
        @JsonProperty("implementationTime")
        private final TimeLevel implementationTime;
        
        @JsonProperty("effectiveness")
        private final EffectivenessLevel effectiveness;
        
        @JsonProperty("status")
        private final ImplementationStatus status;
        
        @JsonProperty("assignedTo")
        private final String assignedTo;
        
        @JsonProperty("expectedCompletion")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private final LocalDate expectedCompletion;
        
        public MitigationStrategy(
                String id,
                StrategyType strategyType,
                String description,
                List<RiskType> targetRiskTypes,
                CostLevel implementationCost,
                TimeLevel implementationTime,
                EffectivenessLevel effectiveness,
                ImplementationStatus status,
                String assignedTo,
                LocalDate expectedCompletion) {
            
            this.id = Objects.requireNonNull(id, "Strategy ID cannot be null");
            this.strategyType = Objects.requireNonNull(strategyType, "Strategy type cannot be null");
            this.description = Objects.requireNonNull(description, "Description cannot be null");
            this.targetRiskTypes = Objects.requireNonNull(targetRiskTypes, "Target risk types cannot be null");
            this.implementationCost = Objects.requireNonNull(implementationCost, "Implementation cost cannot be null");
            this.implementationTime = Objects.requireNonNull(implementationTime, "Implementation time cannot be null");
            this.effectiveness = Objects.requireNonNull(effectiveness, "Effectiveness cannot be null");
            this.status = Objects.requireNonNull(status, "Status cannot be null");
            this.assignedTo = assignedTo;
            this.expectedCompletion = expectedCompletion;
        }
        
        // Getters
        public String getId() { return id; }
        public StrategyType getStrategyType() { return strategyType; }
        public String getDescription() { return description; }
        public List<RiskType> getTargetRiskTypes() { return targetRiskTypes; }
        public CostLevel getImplementationCost() { return implementationCost; }
        public TimeLevel getImplementationTime() { return implementationTime; }
        public EffectivenessLevel getEffectiveness() { return effectiveness; }
        public ImplementationStatus getStatus() { return status; }
        public String getAssignedTo() { return assignedTo; }
        public LocalDate getExpectedCompletion() { return expectedCompletion; }
    }
    
    /**
     * Strategy type enumeration
     * 策略类型枚举
     */
    public enum StrategyType {
        PREVENTIVE("预防性", "Prevent risk occurrence"),
        MITIGATIVE("缓解性", "Reduce risk impact or probability"),
        CONTINGENT("应急性", "Plan for risk response when it occurs"),
        TRANSFER("转移性", "Transfer risk to third party"),
        ACCEPTANCE("接受性", "Accept risk without action"),
        AVOIDANCE("避免性", "Avoid risk by changing project plan");
        
        private final String chineseName;
        private final String description;
        
        StrategyType(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Cost level enumeration
     * 成本级别枚举
     */
    public enum CostLevel {
        LOW("低", "Low cost implementation"),
        MEDIUM("中", "Moderate cost implementation"),
        HIGH("高", "High cost implementation"),
        VERY_HIGH("非常高", "Very high cost implementation");
        
        private final String chineseName;
        private final String description;
        
        CostLevel(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Time level enumeration
     * 时间级别枚举
     */
    public enum TimeLevel {
        SHORT("短期", "Short implementation time (< 1 week)"),
        MEDIUM("中期", "Medium implementation time (1-4 weeks)"),
        LONG("长期", "Long implementation time (> 4 weeks)"),
        ONGOING("持续", "Ongoing implementation effort");
        
        private final String chineseName;
        private final String description;
        
        TimeLevel(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Effectiveness level enumeration
     * 有效性级别枚举
     */
    public enum EffectivenessLevel {
        LOW("低", "Low effectiveness"),
        MEDIUM("中", "Medium effectiveness"),
        HIGH("高", "High effectiveness"),
        VERY_HIGH("非常高", "Very high effectiveness");
        
        private final String chineseName;
        private final String description;
        
        EffectivenessLevel(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Implementation status enumeration
     * 实施状态枚举
     */
    public enum ImplementationStatus {
        NOT_STARTED("未开始", "Implementation not started"),
        IN_PROGRESS("进行中", "Implementation in progress"),
        COMPLETED("已完成", "Implementation completed"),
        CANCELLED("已取消", "Implementation cancelled"),
        ON_HOLD("暂停", "Implementation on hold");
        
        private final String chineseName;
        private final String description;
        
        ImplementationStatus(String chineseName, String description) {
            this.chineseName = chineseName;
            this.description = description;
        }
        
        public String getChineseName() { return chineseName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Key risk indicators class
     * 关键风险指标类
     */
    public static class KeyRiskIndicators {
        
        @JsonProperty("riskVelocity")
        private final double riskVelocity;
        
        @JsonProperty("riskExposure")
        private final double riskExposure;
        
        @JsonProperty("riskConcentration")
        private final double riskConcentration;
        
        @JsonProperty("mitigationBurden")
        private final double mitigationBurden;
        
        @JsonProperty("earlyWarningScore")
        private final int earlyWarningScore;
        
        @JsonProperty("riskMaturityIndex")
        private final int riskMaturityIndex;
        
        public KeyRiskIndicators(
                double riskVelocity,
                double riskExposure,
                double riskConcentration,
                double mitigationBurden,
                int earlyWarningScore,
                int riskMaturityIndex) {
            
            this.riskVelocity = riskVelocity;
            this.riskExposure = riskExposure;
            this.riskConcentration = riskConcentration;
            this.mitigationBurden = mitigationBurden;
            this.earlyWarningScore = earlyWarningScore;
            this.riskMaturityIndex = riskMaturityIndex;
        }
        
        // Getters
        public double getRiskVelocity() { return riskVelocity; }
        public double getRiskExposure() { return riskExposure; }
        public double getRiskConcentration() { return riskConcentration; }
        public double getMitigationBurden() { return mitigationBurden; }
        public int getEarlyWarningScore() { return earlyWarningScore; }
        public int getRiskMaturityIndex() { return riskMaturityIndex; }
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
     * Builder class for RiskAnalysis
     * 风险分析构建器类
     */
    public static class RiskAnalysisBuilder {
        
        private String projectId;
        private String projectName;
        private LocalDate analysisDate;
        private int overallRiskScore;
        private RiskLevel overallRiskLevel;
        private List<RiskItem> identifiedRisks = new ArrayList<>();
        private RiskDistribution riskDistribution;
        private RiskTrends riskTrends;
        private List<MitigationStrategy> mitigationStrategies = new ArrayList<>();
        private KeyRiskIndicators keyRiskIndicators;
        private AnalysisMetadata metadata;
        
        public RiskAnalysisBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }
        
        public RiskAnalysisBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }
        
        public RiskAnalysisBuilder analysisDate(LocalDate analysisDate) {
            this.analysisDate = analysisDate;
            return this;
        }
        
        public RiskAnalysisBuilder overallRiskScore(int overallRiskScore) {
            this.overallRiskScore = overallRiskScore;
            return this;
        }
        
        public RiskAnalysisBuilder overallRiskLevel(RiskLevel overallRiskLevel) {
            this.overallRiskLevel = overallRiskLevel;
            return this;
        }
        
        public RiskAnalysisBuilder identifiedRisks(List<RiskItem> identifiedRisks) {
            this.identifiedRisks = identifiedRisks;
            return this;
        }
        
        public RiskAnalysisBuilder addRiskItem(RiskItem riskItem) {
            this.identifiedRisks.add(riskItem);
            return this;
        }
        
        public RiskAnalysisBuilder riskDistribution(RiskDistribution riskDistribution) {
            this.riskDistribution = riskDistribution;
            return this;
        }
        
        public RiskAnalysisBuilder riskTrends(RiskTrends riskTrends) {
            this.riskTrends = riskTrends;
            return this;
        }
        
        public RiskAnalysisBuilder mitigationStrategies(List<MitigationStrategy> mitigationStrategies) {
            this.mitigationStrategies = mitigationStrategies;
            return this;
        }
        
        public RiskAnalysisBuilder addMitigationStrategy(MitigationStrategy strategy) {
            this.mitigationStrategies.add(strategy);
            return this;
        }
        
        public RiskAnalysisBuilder keyRiskIndicators(KeyRiskIndicators keyRiskIndicators) {
            this.keyRiskIndicators = keyRiskIndicators;
            return this;
        }
        
        public RiskAnalysisBuilder metadata(AnalysisMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public RiskAnalysis build() {
            return new RiskAnalysis(
                projectId,
                projectName,
                analysisDate,
                overallRiskScore,
                overallRiskLevel,
                identifiedRisks,
                riskDistribution,
                riskTrends,
                mitigationStrategies,
                keyRiskIndicators,
                metadata
            );
        }
    }
    
    /**
     * Static factory method to generate RiskAnalysis from WorkPackage data
     * 从WorkPackage数据生成风险分析的静态工厂方法
     */
    public static RiskAnalysis generateFromWorkPackages(
            String projectId,
            String projectName,
            List<WorkPackage> workPackages) {
        
        Objects.requireNonNull(projectId, "Project ID cannot be null");
        Objects.requireNonNull(projectName, "Project name cannot be null");
        Objects.requireNonNull(workPackages, "Work packages list cannot be null");
        
        // Auto-identify risks from work packages
        // 从工作包自动识别风险
        List<RiskItem> identifiedRisks = autoIdentifyRisks(workPackages);
        
        // Calculate overall risk score and level
        // 计算整体风险评分和级别
        RiskAssessment assessment = calculateOverallRiskAssessment(identifiedRisks);
        
        // Generate risk distribution
        // 生成风险分布
        RiskDistribution riskDistribution = generateRiskDistribution(identifiedRisks);
        
        // Generate risk trends
        // 生成风险趋势
        RiskTrends riskTrends = generateRiskTrends(identifiedRisks);
        
        // Generate mitigation strategies
        // 生成缓解策略
        List<MitigationStrategy> mitigationStrategies = suggestMitigationStrategies(identifiedRisks);
        
        // Calculate key risk indicators
        // 计算关键风险指标
        KeyRiskIndicators keyRiskIndicators = calculateKeyRiskIndicators(identifiedRisks);
        
        // Create metadata
        // 创建元数据
        AnalysisMetadata metadata = new AnalysisMetadata(
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            "OpenProject MCP Server",
            "1.0.0",
            List.of("OpenProject API v3"),
            "Automated Risk Assessment Algorithm",
            0.85,
            System.currentTimeMillis()
        );
        
        return new RiskAnalysis(
            projectId,
            projectName,
            LocalDate.now(),
            assessment.overallRiskScore(),
            assessment.overallRiskLevel(),
            identifiedRisks,
            riskDistribution,
            riskTrends,
            mitigationStrategies,
            keyRiskIndicators,
            metadata
        );
    }
    
    /**
     * Automatically identify risks from work packages
     * 从工作包自动识别风险
     */
    private static List<RiskItem> autoIdentifyRisks(List<WorkPackage> workPackages) {
        List<RiskItem> risks = new ArrayList<>();
        
        for (WorkPackage wp : workPackages) {
            // Skip completed work packages
            // 跳过已完成的工作包
            if (wp.isCompleted()) {
                continue;
            }
            
            // Check for overdue risk
            // 检查逾期风险
            if (wp.isOverdue()) {
                risks.add(createOverdueRisk(wp));
            }
            
            // Check for high-priority risk
            // 检查高优先级风险
            if (isHighPriorityRisk(wp)) {
                risks.add(createHighPriorityRisk(wp));
            }
            
            // Check for resource risk
            // 检查资源风险
            if (isResourceRisk(wp)) {
                risks.add(createResourceRisk(wp));
            }
            
            // Check for schedule risk
            // 检查进度风险
            if (isScheduleRisk(wp)) {
                risks.add(createScheduleRisk(wp));
            }
        }
        
        return risks;
    }
    
    /**
     * Create overdue risk item
     * 创建逾期风险项目
     */
    private static RiskItem createOverdueRisk(WorkPackage wp) {
        return new RiskItem(
            "risk-" + wp.id() + "-overdue",
            wp.id(),
            wp.subject(),
            RiskType.SCHEDULE,
            calculateRiskLevel(wp, RiskType.SCHEDULE),
            calculateRiskScore(wp, RiskType.SCHEDULE),
            "任务已逾期 " + Math.abs(wp.getDaysUntilDue()) + " 天 / Task is overdue by " + Math.abs(wp.getDaysUntilDue()) + " days",
            Probability.LIKELY,
            Impact.MODERATE,
            LocalDate.now(),
            wp.dueDate(),
            wp.assignee() != null ? wp.assignee().name() : null,
            true,
            MitigationStatus.NOT_STARTED,
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }
    
    /**
     * Create high-priority risk item
     * 创建高优先级风险项目
     */
    private static RiskItem createHighPriorityRisk(WorkPackage wp) {
        return new RiskItem(
            "risk-" + wp.id() + "-priority",
            wp.id(),
            wp.subject(),
            RiskType.RESOURCE,
            calculateRiskLevel(wp, RiskType.RESOURCE),
            calculateRiskScore(wp, RiskType.RESOURCE),
            "高优先级任务需要特别关注 / High-priority task requires special attention",
            Probability.POSSIBLE,
            Impact.MODERATE,
            LocalDate.now(),
            wp.dueDate(),
            wp.assignee() != null ? wp.assignee().name() : null,
            false,
            MitigationStatus.NOT_STARTED,
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }
    
    /**
     * Create resource risk item
     * 创建资源风险项目
     */
    private static RiskItem createResourceRisk(WorkPackage wp) {
        return new RiskItem(
            "risk-" + wp.id() + "-resource",
            wp.id(),
            wp.subject(),
            RiskType.RESOURCE,
            calculateRiskLevel(wp, RiskType.RESOURCE),
            calculateRiskScore(wp, RiskType.RESOURCE),
            "任务缺少分配者或资源不足 / Task lacks assignee or has insufficient resources",
            Probability.POSSIBLE,
            Impact.MINOR,
            LocalDate.now(),
            wp.dueDate(),
            wp.assignee() != null ? wp.assignee().name() : null,
            false,
            MitigationStatus.NOT_STARTED,
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }
    
    /**
     * Create schedule risk item
     * 创建进度风险项目
     */
    private static RiskItem createScheduleRisk(WorkPackage wp) {
        return new RiskItem(
            "risk-" + wp.id() + "-schedule",
            wp.id(),
            wp.subject(),
            RiskType.SCHEDULE,
            calculateRiskLevel(wp, RiskType.SCHEDULE),
            calculateRiskScore(wp, RiskType.SCHEDULE),
            "任务可能无法按计划完成 / Task may not be completed on schedule",
            Probability.POSSIBLE,
            Impact.MODERATE,
            LocalDate.now(),
            wp.dueDate(),
            wp.assignee() != null ? wp.assignee().name() : null,
            false,
            MitigationStatus.NOT_STARTED,
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }
    
    /**
     * Check if work package represents a high-priority risk
     * 检查工作包是否代表高优先级风险
     */
    private static boolean isHighPriorityRisk(WorkPackage wp) {
        return wp.priority() != null && 
               ("high".equalsIgnoreCase(wp.priority().name()) || "urgent".equalsIgnoreCase(wp.priority().name()));
    }
    
    /**
     * Check if work package represents a resource risk
     * 检查工作包是否代表资源风险
     */
    private static boolean isResourceRisk(WorkPackage wp) {
        return wp.assignee() == null || 
               (wp.estimatedTime() != null && wp.estimatedTime() > 40); // More than 40 hours
    }
    
    /**
     * Check if work package represents a schedule risk
     * 检查工作包是否代表进度风险
     */
    private static boolean isScheduleRisk(WorkPackage wp) {
        if (wp.dueDate() == null) {
            return false;
        }
        long daysUntilDue = wp.getDaysUntilDue();
        return daysUntilDue <= 7 && daysUntilDue > 0; // Due within 7 days but not overdue
    }
    
    /**
     * Calculate risk level for work package
     * 计算工作包的风险级别
     */
    private static RiskLevel calculateRiskLevel(WorkPackage wp, RiskType riskType) {
        if (wp.isOverdue()) {
            return RiskLevel.HIGH;
        }
        
        if (isHighPriorityRisk(wp)) {
            return RiskLevel.MEDIUM;
        }
        
        long daysUntilDue = wp.getDaysUntilDue();
        if (daysUntilDue <= 3 && daysUntilDue > 0) {
            return RiskLevel.MEDIUM;
        }
        
        return RiskLevel.LOW;
    }
    
    /**
     * Calculate risk score for work package
     * 计算工作包的风险评分
     */
    private static int calculateRiskScore(WorkPackage wp, RiskType riskType) {
        int baseScore = 50;
        
        // Add score for overdue
        // 逾期加分
        if (wp.isOverdue()) {
            baseScore += Math.min(30, Math.abs(wp.getDaysUntilDue()) * 2);
        }
        
        // Add score for high priority
        // 高优先级加分
        if (isHighPriorityRisk(wp)) {
            baseScore += 20;
        }
        
        // Add score for schedule pressure
        // 进度压力加分
        long daysUntilDue = wp.getDaysUntilDue();
        if (daysUntilDue <= 3 && daysUntilDue > 0) {
            baseScore += 15;
        } else if (daysUntilDue <= 7 && daysUntilDue > 0) {
            baseScore += 10;
        }
        
        return Math.min(100, baseScore);
    }
    
    /**
     * Calculate overall risk assessment
     * 计算整体风险评估
     */
    private static RiskAssessment calculateOverallRiskAssessment(List<RiskItem> risks) {
        if (risks.isEmpty()) {
            return new RiskAssessment(0, RiskLevel.LOW);
        }
        
        double averageScore = risks.stream()
            .mapToInt(RiskItem::getRiskScore)
            .average()
            .orElse(0.0);
        
        int overallScore = (int) Math.round(averageScore);
        RiskLevel overallLevel = determineOverallRiskLevel(risks, overallScore);
        
        return new RiskAssessment(overallScore, overallLevel);
    }
    
    /**
     * Determine overall risk level
     * 确定整体风险级别
     */
    private static RiskLevel determineOverallRiskLevel(List<RiskItem> risks, int overallScore) {
        // Check for critical risks
        // 检查关键风险
        boolean hasCritical = risks.stream().anyMatch(RiskItem::isCritical);
        if (hasCritical) {
            return RiskLevel.CRITICAL;
        }
        
        // Check for high risks
        // 检查高风险
        long highRiskCount = risks.stream()
            .filter(risk -> risk.getRiskLevel() == RiskLevel.HIGH)
            .count();
        if (highRiskCount >= 2 || overallScore >= 70) {
            return RiskLevel.HIGH;
        }
        
        // Check for medium risks
        // 检查中等风险
        long mediumRiskCount = risks.stream()
            .filter(risk -> risk.getRiskLevel() == RiskLevel.MEDIUM)
            .count();
        if (mediumRiskCount >= 3 || overallScore >= 40) {
            return RiskLevel.MEDIUM;
        }
        
        return RiskLevel.LOW;
    }
    
    /**
     * Generate risk distribution
     * 生成风险分布
     */
    private static RiskDistribution generateRiskDistribution(List<RiskItem> risks) {
        RiskLevelDistribution levelDistribution = new RiskLevelDistribution(
            (int) risks.stream().filter(r -> r.getRiskLevel() == RiskLevel.LOW).count(),
            (int) risks.stream().filter(r -> r.getRiskLevel() == RiskLevel.MEDIUM).count(),
            (int) risks.stream().filter(r -> r.getRiskLevel() == RiskLevel.HIGH).count(),
            (int) risks.stream().filter(r -> r.getRiskLevel() == RiskLevel.CRITICAL).count()
        );
        
        RiskTypeDistribution typeDistribution = new RiskTypeDistribution(
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.SCHEDULE).count(),
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.COST).count(),
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.TECHNICAL).count(),
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.RESOURCE).count(),
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.SCOPE).count(),
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.QUALITY).count(),
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.EXTERNAL).count(),
            (int) risks.stream().filter(r -> r.getRiskType() == RiskType.DEPENDENCY).count()
        );
        
        AssigneeDistribution assigneeDistribution = generateAssigneeDistribution(risks);
        
        return new RiskDistribution(levelDistribution, typeDistribution, assigneeDistribution);
    }
    
    /**
     * Generate assignee distribution
     * 生成分配者分布
     */
    private static AssigneeDistribution generateAssigneeDistribution(List<RiskItem> risks) {
        Map<String, Long> assigneeCounts = risks.stream()
            .filter(r -> r.getAssignee() != null)
            .collect(Collectors.groupingBy(RiskItem::getAssignee, Collectors.counting()));
        
        List<AssigneeItem> assigneeItems = assigneeCounts.entrySet().stream()
            .map(entry -> {
                String assignee = entry.getKey();
                long totalRisks = entry.getValue();
                long highRisks = risks.stream()
                    .filter(r -> assignee.equals(r.getAssignee()) && r.getRiskLevel() == RiskLevel.HIGH)
                    .count();
                return new AssigneeItem(assignee, (int) totalRisks, (int) highRisks);
            })
            .collect(Collectors.toList());
        
        long unassigned = risks.stream()
            .filter(r -> r.getAssignee() == null)
            .count();
        
        return new AssigneeDistribution((int) unassigned, assigneeItems);
    }
    
    /**
     * Generate risk trends
     * 生成风险趋势
     */
    private static RiskTrends generateRiskTrends(List<RiskItem> risks) {
        // Simplified trend analysis
        // 简化的趋势分析
        List<RiskScorePoint> riskScoreHistory = new ArrayList<>();
        riskScoreHistory.add(new RiskScorePoint(
            LocalDate.now().minusDays(7),
            calculateHistoricalRiskScore(risks, 7),
            (int) risks.stream().filter(r -> r.getRiskLevel() == RiskLevel.HIGH).count()
        ));
        riskScoreHistory.add(new RiskScorePoint(
            LocalDate.now(),
            (int) risks.stream().mapToInt(RiskItem::getRiskScore).average().orElse(0),
            (int) risks.stream().filter(r -> r.getRiskLevel() == RiskLevel.HIGH).count()
        ));
        
        // VelocityTrend velocityTrend = new VelocityTrend(
        //     calculateCurrentVelocity(risks),
        //     calculateAverageVelocity(risks),
        //     determineVelocityTrend(risks),
        //     calculateVelocityStability(risks)
        // );
        
        return new RiskTrends(
            determineOverallTrend(risks),
            riskScoreHistory,
            null, // riskCountTrend - would need proper implementation
            0.0 // calculateMitigationEffectiveness(risks) - placeholder
        );
    }
    
    /**
     * Suggest mitigation strategies
     * 建议缓解策略
     */
    private static List<MitigationStrategy> suggestMitigationStrategies(List<RiskItem> risks) {
        List<MitigationStrategy> strategies = new ArrayList<>();
        
        for (RiskItem risk : risks) {
            switch (risk.getRiskType()) {
                case SCHEDULE:
                    strategies.add(createScheduleMitigation(risk));
                    break;
                case RESOURCE:
                    strategies.add(createResourceMitigation(risk));
                    break;
                case TECHNICAL:
                    strategies.add(createTechnicalMitigation(risk));
                    break;
                default:
                    strategies.add(createGeneralMitigation(risk));
            }
        }
        
        return strategies;
    }
    
    /**
     * Create schedule mitigation strategy
     * 创建进度缓解策略
     */
    private static MitigationStrategy createScheduleMitigation(RiskItem risk) {
        return new MitigationStrategy(
            "mitigation-" + risk.getId(),
            StrategyType.MITIGATIVE,
            "重新安排任务优先级并分配额外资源 / Reprioritize tasks and allocate additional resources",
            List.of(RiskType.SCHEDULE),
            CostLevel.MEDIUM,
            TimeLevel.SHORT,
            EffectivenessLevel.HIGH,
            ImplementationStatus.NOT_STARTED,
            risk.getAssignee(),
            LocalDate.now().plusDays(7)
        );
    }
    
    /**
     * Create resource mitigation strategy
     * 创建资源缓解策略
     */
    private static MitigationStrategy createResourceMitigation(RiskItem risk) {
        return new MitigationStrategy(
            "mitigation-" + risk.getId(),
            StrategyType.PREVENTIVE,
            "为任务分配适当的资源和人员 / Assign appropriate resources and personnel to tasks",
            List.of(RiskType.RESOURCE),
            CostLevel.MEDIUM,
            TimeLevel.MEDIUM,
            EffectivenessLevel.HIGH,
            ImplementationStatus.NOT_STARTED,
            risk.getAssignee(),
            LocalDate.now().plusDays(14)
        );
    }
    
    /**
     * Create technical mitigation strategy
     * 创建技术缓解策略
     */
    private static MitigationStrategy createTechnicalMitigation(RiskItem risk) {
        return new MitigationStrategy(
            "mitigation-" + risk.getId(),
            StrategyType.PREVENTIVE,
            "进行技术评估和制定备用方案 / Conduct technical assessment and develop contingency plans",
            List.of(RiskType.TECHNICAL),
            CostLevel.HIGH,
            TimeLevel.LONG,
            EffectivenessLevel.MEDIUM,
            ImplementationStatus.NOT_STARTED,
            risk.getAssignee(),
            LocalDate.now().plusDays(21)
        );
    }
    
    /**
     * Create general mitigation strategy
     * 创建通用缓解策略
     */
    private static MitigationStrategy createGeneralMitigation(RiskItem risk) {
        return new MitigationStrategy(
            "mitigation-" + risk.getId(),
            StrategyType.MITIGATIVE,
            "定期监控风险状态并采取必要的纠正措施 / Regularly monitor risk status and take corrective action",
            List.of(risk.getRiskType()),
            CostLevel.LOW,
            TimeLevel.SHORT,
            EffectivenessLevel.MEDIUM,
            ImplementationStatus.NOT_STARTED,
            risk.getAssignee(),
            LocalDate.now().plusDays(3)
        );
    }
    
    /**
     * Calculate key risk indicators
     * 计算关键风险指标
     */
    private static KeyRiskIndicators calculateKeyRiskIndicators(List<RiskItem> risks) {
        double riskVelocity = calculateRiskVelocity(risks);
        double riskExposure = calculateRiskExposure(risks);
        double riskConcentration = calculateRiskConcentration(risks);
        double mitigationBurden = calculateMitigationBurden(risks);
        int earlyWarningScore = calculateEarlyWarningScore(risks);
        int riskMaturityIndex = calculateRiskMaturityIndex(risks);
        
        return new KeyRiskIndicators(
            riskVelocity,
            riskExposure,
            riskConcentration,
            mitigationBurden,
            earlyWarningScore,
            riskMaturityIndex
        );
    }
    
    // Helper methods for calculations
    // 计算的辅助方法
    
    private static int calculateHistoricalRiskScore(List<RiskItem> risks, int daysAgo) {
        // Simplified historical calculation
        // 简化的历史计算
        return (int) risks.stream()
            .mapToInt(RiskItem::getRiskScore)
            .average()
            .orElse(0);
    }
    
    private static double calculateCurrentVelocity(List<RiskItem> risks) {
        // Simplified velocity calculation
        // 简化的速度计算
        return risks.stream()
            .mapToInt(RiskItem::getRiskScore)
            .average()
            .orElse(0.0);
    }
    
    private static double calculateAverageVelocity(List<RiskItem> risks) {
        // Simplified average velocity
        // 简化的平均速度
        return risks.stream()
            .mapToInt(RiskItem::getRiskScore)
            .average()
            .orElse(0.0);
    }
    
    private static TrendDirection determineVelocityTrend(List<RiskItem> risks) {
        // Simplified trend determination
        // 简化的趋势确定
        return risks.size() > 5 ? TrendDirection.STABLE : TrendDirection.UNKNOWN;
    }
    
    private static double calculateVelocityStability(List<RiskItem> risks) {
        // Simplified stability calculation
        // 简化的稳定性计算
        return 0.75; // Placeholder value
    }
    
    private static TrendDirection determineOverallTrend(List<RiskItem> risks) {
        // Simplified overall trend
        // 简化的整体趋势
        long highRisks = risks.stream()
            .filter(r -> r.getRiskLevel() == RiskLevel.HIGH || r.getRiskLevel() == RiskLevel.CRITICAL)
            .count();
        
        if (highRisks == 0) return TrendDirection.IMPROVING;
        if (highRisks <= 2) return TrendDirection.STABLE;
        return TrendDirection.DETERIORATING;
    }
    
    private static double calculateBurndownRate(List<RiskItem> risks) {
        // Simplified burndown rate
        // 简化的燃尽率
        return 0.1; // Placeholder value
    }
    
    private static double calculateAccelerationFactor(List<RiskItem> risks) {
        // Simplified acceleration factor
        // 简化的加速因子
        return 0.0; // Placeholder value
    }
    
    private static double calculateRiskVelocity(List<RiskItem> risks) {
        // Simplified risk velocity
        // 简化的风险速度
        return risks.stream()
            .mapToInt(RiskItem::getRiskScore)
            .average()
            .orElse(0.0) / 10.0;
    }
    
    private static double calculateRiskExposure(List<RiskItem> risks) {
        // Simplified risk exposure
        // 简化的风险暴露
        return risks.stream()
            .mapToDouble(r -> r.getRiskScore() * getImpactMultiplier(r.getImpact()))
            .average()
            .orElse(0.0);
    }
    
    private static double getImpactMultiplier(Impact impact) {
        switch (impact) {
            case MINIMAL: return 0.2;
            case MINOR: return 0.5;
            case MODERATE: return 1.0;
            case MAJOR: return 2.0;
            case SEVERE: return 5.0;
            default: return 1.0;
        }
    }
    
    private static double calculateRiskConcentration(List<RiskItem> risks) {
        // Simplified risk concentration
        // 简化的风险集中度
        if (risks.isEmpty()) return 0.0;
        
        Map<RiskType, Long> typeCounts = risks.stream()
            .collect(Collectors.groupingBy(RiskItem::getRiskType, Collectors.counting()));
        
        double maxConcentration = typeCounts.values().stream()
            .mapToDouble(Long::doubleValue)
            .max()
            .orElse(0.0);
        
        return maxConcentration / risks.size();
    }
    
    private static double calculateMitigationBurden(List<RiskItem> risks) {
        // Simplified mitigation burden
        // 简化的缓解负担
        return risks.stream()
            .filter(r -> r.getMitigationStatus() != MitigationStatus.COMPLETED)
            .count() * 0.1;
    }
    
    private static int calculateEarlyWarningScore(List<RiskItem> risks) {
        // Simplified early warning score
        // 简化的预警分数
        int score = 0;
        score += risks.stream().filter(r -> r.isCritical()).count() * 25;
        score += risks.stream().filter(r -> r.getRiskLevel() == RiskLevel.HIGH).count() * 10;
        score += risks.stream().filter(r -> r.isOverdue()).count() * 15;
        return Math.min(100, score);
    }
    
    private static int calculateRiskMaturityIndex(List<RiskItem> risks) {
        // Simplified risk maturity index
        // 简化的风险成熟度指数
        if (risks.isEmpty()) return 50;
        
        long mitigated = risks.stream()
            .filter(r -> r.getMitigationStatus() == MitigationStatus.COMPLETED)
            .count();
        
        return (int) ((mitigated * 100.0) / risks.size());
    }
    
    /**
     * Record for risk assessment results
     * 风险评估结果的记录
     */
    private record RiskAssessment(int overallRiskScore, RiskLevel overallRiskLevel) {}
    
    /**
     * Get a new builder instance
     * 获取新的构建器实例
     */
    public static RiskAnalysisBuilder builder() {
        return new RiskAnalysisBuilder();
    }
    
    /**
     * Validate the risk analysis data
     * 验证风险分析数据
     */
    public boolean isValid() {
        return projectId != null && !projectId.trim().isEmpty() &&
               projectName != null && !projectName.trim().isEmpty() &&
               analysisDate != null &&
               identifiedRisks != null &&
               riskDistribution != null &&
               riskTrends != null &&
               mitigationStrategies != null &&
               keyRiskIndicators != null &&
               metadata != null;
    }
    
    /**
     * Get risk summary in Chinese
     * 获取中文风险摘要
     */
    public String getChineseSummary() {
        return String.format("项目 %s 风险分析: 识别出 %d 个风险，整体风险级别 %s，风险评分 %d",
            projectName, identifiedRisks.size(), overallRiskLevel.getChineseName(), overallRiskScore);
    }
    
    /**
     * Get risk summary in English
     * 获取英文风险摘要
     */
    public String getEnglishSummary() {
        return String.format("Project %s Risk Analysis: %d risks identified, overall risk level %s, risk score %d",
            projectName, identifiedRisks.size(), overallRiskLevel.name(), overallRiskScore);
    }
}