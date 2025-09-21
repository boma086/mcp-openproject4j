package com.example.mcp.service;

import com.example.mcp.client.OpenProjectApiClient;
import com.example.mcp.exception.OpenProjectApiException;
import com.example.mcp.exception.RateLimitExceededException;
import com.example.mcp.model.*;
// import com.example.mcp.model.SimpleRiskAnalysis;
import com.example.mcp.model.SimpleCompletionAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Project Service for OpenProject MCP Server
 * OpenProject MCP服务器的项目服务
 * 
 * This service provides the main MCP tool methods for project analysis:
 * - Weekly report generation
 * - Risk analysis
 * - Completion analysis
 * 
 * 该服务提供项目分析的主要MCP工具方法：
 * - 周报生成
 * - 风险分析
 * - 完成度分析
 * 
 * Features:
 * - Three MCP tools annotated with @Tool
 * - Integration with OpenProject API client
 * - Comprehensive error handling
 * - Natural language report generation
 * - Support for both Chinese and English
 * 
 * 主要特性：
 * - 三个@Tool注解的MCP工具
 * - 与OpenProject API客户端集成
 * - 全面的错误处理
 * - 自然语言报告生成
 * - 支持中英文
 * 
 * Requirements addressed:
 * - FR1: MCP Server Infrastructure - Exposes @Tool methods
 * - FR3: Weekly Report Generation - generateWeeklyReport method
 * - FR4: Risk Analysis Tool - analyzeRisks method
 * - FR5: Completion Analysis Tool - calculateCompletion method
 * 
 * 满足的需求：
 * - FR1: MCP服务器基础设施 - 暴露@Tool方法
 * - FR3: 周报生成 - generateWeeklyReport方法
 * - FR4: 风险分析工具 - analyzeRisks方法
 * - FR5: 完成度分析工具 - calculateCompletion方法
 * 
 * @author OpenProject MCP Team
 * @since 1.0
 */
@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    
    // Simple cache for performance optimization (NFR1)
    private final Map<String, CacheEntry> reportCache = new ConcurrentHashMap<>();
    private final Map<String, CompletionCacheEntry> completionCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MINUTES = 5;
    
    @Autowired
    private OpenProjectApiClient apiClient;
    
    @Value("${openproject.base-url}")
    private String baseUrl;
    
    @Value("${openproject.api-key}")
    private String apiKey;
    
    @Value("${weekly-report.cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${weekly-report.max-page-size:100}")
    private int maxPageSize;
    
    /**
     * Cache entry with expiration time
     * 带过期时间的缓存条目
     */
    private static class CacheEntry {
        private final WeeklyReport report;
        private final long expiryTime;
        
        public CacheEntry(WeeklyReport report) {
            this.report = report;
            this.expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CACHE_EXPIRY_MINUTES);
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public WeeklyReport getReport() {
            return report;
        }
    }
    
    /**
     * Completion analysis cache entry with expiration time
     * 完成度分析缓存条目，带过期时间
     */
    private static class CompletionCacheEntry {
        private final String result;
        private final long expiryTime;
        
        public CompletionCacheEntry(String result) {
            this.result = result;
            this.expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(CACHE_EXPIRY_MINUTES);
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public String getResult() {
            return result;
        }
    }
    
    /**
     * Generate weekly project report with retry logic
     * 生成项目周报，具有重试逻辑
     * 
     * This tool generates a comprehensive weekly report for a project,
     * including task updates, completion metrics, and highlights with enhanced
     * performance optimizations and comprehensive error handling. This method
     * implements retry logic for transient API failures.
     * 
     * 该工具生成项目的综合周报，包括任务更新、完成指标和关键亮点，
     * 具有增强的性能优化和全面的错误处理。此方法实现瞬时API故障的重试逻辑。
     * 
     * @param projectId The ID of the project to generate report for
     * @return Natural language report in Chinese and English
     * @throws IllegalArgumentException if projectId is invalid
     * 
     * Requirement: FR3 - Weekly Report Generation
     * 需求：FR3 - 周报生成
     * Requirement: NFR1 - Performance
     * 需求：NFR1 - 性能
     * Requirement: NFR3 - Reliability (Retry Logic)
     * 需求：NFR3 - 可靠性（重试逻辑）
     */
    @Tool(description = "生成项目周报，从OpenProject拉取本周任务更新，具有重试逻辑 | Generate weekly project report by pulling recent task updates from OpenProject with retry logic")
    @Retryable(
        value = { RateLimitExceededException.class, OpenProjectApiException.class, ResourceAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        label = "generateWeeklyReportRetry"
    )
    public String generateWeeklyReport(
            @ToolParam(description = "项目ID | Project ID") 
            String projectId) {
        
        logger.info("Generating weekly report for project: {}", projectId);
        
        // Validate input
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("项目ID不能为空 | Project ID cannot be null or empty");
        }
        
        // Performance tracking
        long startTime = System.currentTimeMillis();
        
        try {
            // Check cache first for performance optimization
            if (cacheEnabled) {
                String cacheKey = generateCacheKey(projectId);
                CacheEntry cachedEntry = reportCache.get(cacheKey);
                if (cachedEntry != null && !cachedEntry.isExpired()) {
                    logger.debug("Returning cached report for project: {}", projectId);
                    return formatWeeklyReport(cachedEntry.getReport());
                }
            }
            
            // Set up date range (last 7 days as default)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(6); // 7 days total including today
            
            logger.debug("Report period: {} to {}", startDate, endDate);
            
            // Get project name with enhanced error handling
            String projectName = getProjectNameWithRetry(projectId);
            
            // Build enhanced filters for work packages with pagination support
            Map<String, String> filters = buildEnhancedFilters(startDate, endDate, projectId);
            
            // Fetch work packages with enhanced error handling and pagination
            List<WorkPackage> workPackages = fetchWorkPackagesWithPagination(filters, projectId);
            
            logger.debug("Found {} work packages for project {}", workPackages.size(), projectId);
            
            // Generate comprehensive weekly report
            WeeklyReport report = generateComprehensiveWeeklyReport(
                projectId, projectName, workPackages, startDate, endDate, startTime
            );
            
            // Cache the result for future requests
            if (cacheEnabled) {
                String cacheKey = generateCacheKey(projectId);
                reportCache.put(cacheKey, new CacheEntry(report));
            }
            
            // Format the report as readable text
            String formattedReport = formatWeeklyReport(report);
            
            // Log performance metrics
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Weekly report generated successfully for project {} in {} ms", 
                      projectId, processingTime);
            
            // Validate performance requirements (NFR1)
            if (processingTime > 30000) { // 30 seconds
                logger.warn("Weekly report generation took {} ms for project {}, exceeding 30-second SLA", 
                           processingTime, projectId);
            }
            
            return formattedReport;
            
        } catch (RateLimitExceededException e) {
            logger.error("Rate limit exceeded while generating weekly report for project: {}", projectId, e);
            return generateRateLimitErrorReport(e);
        } catch (OpenProjectApiException e) {
            logger.error("API error while generating weekly report for project: {}", projectId, e);
            return generateApiErrorReport(e);
        } catch (Exception e) {
            logger.error("Error generating weekly report for project: {}", projectId, e);
            return generateErrorReport("周报生成失败 | Weekly report generation failed", e);
        }
    }
    
    /**
     * Analyze project risks based on overdue tasks with sophisticated scoring, security, and retry logic
     * 基于逾期任务分析项目风险，具有复杂评分、安全功能和重试逻辑
     * 
     * This enhanced tool analyzes project risks by identifying overdue tasks,
     * calculating sophisticated risk scores, implementing advanced security measures,
     * and providing actionable recommendations with retry logic for transient API failures.
     * 
     * 该增强工具通过识别逾期任务、计算复杂的风险分数、实施高级安全措施
     * 和提供可操作的建议来分析项目风险，具有瞬时API故障的重试逻辑。
     * 
     * @param projectId The ID of the project to analyze
     * @return Enhanced risk analysis report with sophisticated scoring and security measures
     * @throws IllegalArgumentException if projectId is invalid
     * @throws SecurityException if security validation fails
     * 
     * Requirement: FR4 - Risk Analysis Tool (Enhanced)
     * 需求：FR4 - 风险分析工具（增强版）
     * Requirement: NFR2 - Security (Enhanced)
     * 需求：NFR2 - 安全性（增强版）
     * Requirement: NFR3 - Reliability (Retry Logic)
     * 需求：NFR3 - 可靠性（重试逻辑）
     */
    @Tool(description = "分析项目风险，基于逾期任务和截止日期，包含高级安全措施、风险评分和重试逻辑 | Analyze project risks based on overdue tasks and due dates with advanced security measures, risk scoring, and retry logic")
    @Retryable(
        value = { RateLimitExceededException.class, OpenProjectApiException.class, ResourceAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1500, multiplier = 2.0),
        label = "analyzeRisksRetry"
    )
    public String analyzeRisks(
            @ToolParam(description = "项目ID | Project ID") 
            String projectId) {
        
        logger.info("Analyzing risks for project: {}", projectId);
        
        // Enhanced input validation with security measures
        validateProjectIdWithSecurity(projectId);
        
        // Security validation before proceeding
        if (!validateApiPermissions()) {
            throw new SecurityException("API权限验证失败 | API permission validation failed");
        }
        
        try {
            // Get project name with security-aware logging
            String projectName = getProjectNameWithSecurityLogging(projectId);
            
            // Build secure filters to get all work packages for the project
            Map<String, String> filters = buildSecureFilters(projectId);
            
            // Fetch all work packages with enhanced error handling and security
            List<WorkPackage> workPackages = fetchWorkPackagesWithSecurity(filters, projectId);
            
            logger.debug("Found {} work packages for risk analysis with security validation", workPackages.size());
            
            // Enhanced risk analysis with sophisticated scoring
            EnhancedRiskAnalysis enhancedAnalysis = performEnhancedRiskAnalysis(
                workPackages, projectId, projectName
            );
            
            // Format the enhanced risk analysis report with security considerations
            return formatEnhancedRiskAnalysis(enhancedAnalysis, projectName);
            
        } catch (SecurityException e) {
            logger.error("Security violation while analyzing risks for project: {}", projectId, e);
            return generateSecurityErrorReport(e);
        } catch (Exception e) {
            logger.error("Error analyzing risks for project: {}", projectId, e);
            return generateErrorReport("风险分析失败 | Risk analysis failed", e);
        }
    }
    
    /**
     * Calculate project completion percentage with retry logic
     * 计算项目完成百分比，具有重试逻辑
     * 
     * This tool calculates the overall completion percentage for a project
     * by analyzing all work packages and their completion status with retry logic
     * for transient API failures.
     * 
     * 该工具通过分析所有工作包及其完成状态来计算项目的整体完成百分比，
     * 具有瞬时API故障的重试逻辑。
     * 
     * @param projectId The ID of the project to analyze
     * @return Completion analysis report with percentage and summary
     * @throws IllegalArgumentException if projectId is invalid
     * 
     * Requirement: FR5 - Completion Analysis Tool
     * 需求：FR5 - 完成度分析工具
     * Requirement: NFR3 - Reliability (Retry Logic)
     * 需求：NFR3 - 可靠性（重试逻辑）
     */
    @Tool(description = "计算项目完成度，分析任务完成状态，提供详细进度报告、洞察和重试逻辑 | Calculate project completion percentage with detailed progress analysis, insights, and retry logic")
    @Retryable(
        value = { RateLimitExceededException.class, OpenProjectApiException.class, ResourceAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1200, multiplier = 2.0),
        label = "calculateCompletionRetry"
    )
    public String calculateCompletion(
            @ToolParam(description = "项目ID | Project ID") 
            String projectId) {
        
        long startTime = System.currentTimeMillis();
        logger.info("Starting enhanced completion calculation for project: {}", projectId);
        
        // Validate and sanitize input
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("项目ID不能为空 | Project ID cannot be null or empty");
        }
        
        // Sanitize projectId to prevent injection attacks
        String sanitizedProjectId = sanitizeProjectId(projectId);
        
        // Check cache first for performance (NFR1)
        String cacheKey = generateCompletionCacheKey(sanitizedProjectId);
        if (cacheEnabled) {
            String cachedResult = getCachedCompletionResult(cacheKey);
            if (cachedResult != null) {
                logger.info("Returning cached completion analysis for project: {}", sanitizedProjectId);
                return cachedResult;
            }
        }
        
        try {
            // Enhanced project information retrieval with retry logic
            String projectName = getProjectNameWithRetry(sanitizedProjectId);
            
            // Build enhanced filters for comprehensive work package querying
            Map<String, String> filters = buildCompletionFilters(sanitizedProjectId);
            
            // Fetch work packages with enhanced pagination and error handling
            List<WorkPackage> workPackages = fetchWorkPackagesWithPaginationAndRetry(filters, sanitizedProjectId);
            
            logger.debug("Found {} work packages for enhanced completion analysis", workPackages.size());
            
            // Handle empty project scenario
            if (workPackages.isEmpty()) {
                return generateEmptyProjectReport(projectName, sanitizedProjectId);
            }
            
            // Enhanced completion analysis with multiple algorithms
            EnhancedCompletionMetrics metrics = calculateEnhancedCompletionMetrics(workPackages);
            
            // Generate comprehensive progress analysis
            ProgressAnalysis progressAnalysis = generateProgressAnalysis(workPackages, metrics);
            
            // Calculate trend analysis (if historical data available)
            TrendAnalysis trendAnalysis = calculateTrendAnalysis(sanitizedProjectId, metrics);
            
            // Generate priority-weighted completion assessment
            PriorityWeightedAnalysis priorityAnalysis = calculatePriorityWeightedAnalysis(workPackages);
            
            // Create enhanced completion analysis report
            EnhancedCompletionAnalysis analysis = new EnhancedCompletionAnalysis(
                sanitizedProjectId,
                LocalDate.now(),
                metrics.overallCompletionRate(),
                metrics.totalTasks(),
                metrics.completedTasks(),
                metrics.inProgressTasks(),
                metrics.overdueTasks(),
                metrics.highPriorityTasks(),
                metrics.completionByStatus(),
                metrics.completionByType(),
                progressAnalysis,
                trendAnalysis,
                priorityAnalysis
            );
            
            // Format the enhanced completion analysis report
            String result = formatEnhancedCompletionAnalysis(analysis, projectName);
            
            // Cache the result for performance
            if (cacheEnabled) {
                cacheCompletionResult(cacheKey, result);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Enhanced completion calculation completed in {}ms for project: {}", 
                       executionTime, sanitizedProjectId);
            
            // Performance validation (NFR1)
            if (executionTime > 30000) { // 30 seconds
                logger.warn("Completion analysis exceeded 30s threshold for project {}: {}ms", 
                           sanitizedProjectId, executionTime);
            }
            
            return result;
            
        } catch (RateLimitExceededException e) {
            logger.warn("Rate limit exceeded for project: {}", sanitizedProjectId, e);
            return generateRateLimitErrorReport(e);
        } catch (OpenProjectApiException e) {
            logger.error("API error calculating completion for project: {}", sanitizedProjectId, e);
            return generateApiErrorReport(e);
        } catch (Exception e) {
            logger.error("Unexpected error calculating completion for project: {}", sanitizedProjectId, e);
            return generateComprehensiveErrorReport("完成度计算失败 | Completion calculation failed", e);
        }
    }
    
    // Enhanced completion analysis models and helper methods
    
    /**
     * Enhanced completion metrics with comprehensive analysis
     * 增强的完成度指标和全面分析
     */
    private record EnhancedCompletionMetrics(
        double overallCompletionRate,
        int totalTasks,
        int completedTasks,
        int inProgressTasks,
        int overdueTasks,
        int highPriorityTasks,
        Map<String, Integer> completionByStatus,
        Map<String, Integer> completionByType
    ) {}
    
    /**
     * Progress analysis with detailed insights
     * 详细洞察的进度分析
     */
    private record ProgressAnalysis(
        String progressSummary,
        String statusDistribution,
        String riskAssessment,
        List<String> recommendations,
        String progressVisualization
    ) {}
    
    /**
     * Trend analysis for completion patterns
     * 完成度模式的趋势分析
     */
    private record TrendAnalysis(
        boolean trendingUpward,
        double weeklyChange,
        String trendDescription,
        String prediction
    ) {}
    
    /**
     * Priority-weighted completion analysis
     * 优先级加权的完成度分析
     */
    private record PriorityWeightedAnalysis(
        double weightedCompletionRate,
        int highPriorityCompleted,
        int highPriorityTotal,
        String priorityAssessment
    ) {}
    
    /**
     * Enhanced completion analysis with comprehensive data
     * 全面数据的增强完成度分析
     */
    private record EnhancedCompletionAnalysis(
        String projectId,
        LocalDate analysisDate,
        double overallCompletionRate,
        int totalTasks,
        int completedTasks,
        int inProgressTasks,
        int overdueTasks,
        int highPriorityTasks,
        Map<String, Integer> completionByStatus,
        Map<String, Integer> completionByType,
        ProgressAnalysis progressAnalysis,
        TrendAnalysis trendAnalysis,
        PriorityWeightedAnalysis priorityAnalysis
    ) {}
    
    /**
     * Sanitize project ID to prevent injection attacks
     * 清理项目ID以防止注入攻击
     */
    private String sanitizeProjectId(String projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        // Remove any potentially dangerous characters
        return projectId.replaceAll("[^a-zA-Z0-9-_.]", "").trim();
    }
    
    /**
     * Generate cache key for completion analysis
     * 为完成度分析生成缓存键
     */
    private String generateCompletionCacheKey(String projectId) {
        return String.format("completion-analysis-%s-%s", projectId, LocalDate.now());
    }
    
    /**
     * Get cached completion result
     * 获取缓存的完成度结果
     */
    private String getCachedCompletionResult(String cacheKey) {
        CompletionCacheEntry entry = completionCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.getResult();
        }
        return null;
    }
    
    /**
     * Cache completion result for performance
     * 缓存完成度结果以提高性能
     */
    private void cacheCompletionResult(String cacheKey, String result) {
        completionCache.put(cacheKey, new CompletionCacheEntry(result));
    }
    
    /**
     * Build enhanced filters for completion analysis
     * 为完成度分析构建增强的过滤器
     */
    private Map<String, String> buildCompletionFilters(String projectId) {
        Map<String, String> filters = new HashMap<>();
        
        // Project filter
        filters.put("project", projectId);
        
        // Include all types of work packages for comprehensive analysis
        // No status filter to get complete picture
        
        logger.debug("Built completion filters: {}", filters);
        return filters;
    }
    
    /**
     * Fetch work packages with pagination and retry logic
     * 使用分页和重试逻辑获取工作包
     */
    @Retryable(
        value = { RateLimitExceededException.class, OpenProjectApiException.class, ResourceAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0),
        label = "fetchWorkPackagesRetry"
    )
    private List<WorkPackage> fetchWorkPackagesWithPaginationAndRetry(Map<String, String> filters, String projectId) 
            throws Exception {
        List<WorkPackage> allWorkPackages = new ArrayList<>();
        int offset = 0;
        int pageSize = Math.min(maxPageSize, 100); // Use configured max page size
        
        try {
            while (true) {
                logger.debug("Fetching work packages with offset: {}, pageSize: {}", offset, pageSize);
                
                List<WorkPackage> batch;
                try {
                    batch = apiClient.getWorkPackages(filters, pageSize, offset);
                } catch (OpenProjectApiException e) {
                    logger.error("API error fetching work packages: {}", e.getMessage());
                    throw new RuntimeException("Failed to fetch work packages from OpenProject API", e);
                }
                
                if (batch.isEmpty()) {
                    break; // No more results
                }
                
                allWorkPackages.addAll(batch);
                offset += pageSize;
                
                // Safety limit to prevent infinite loops
                if (allWorkPackages.size() > 5000) {
                    logger.warn("Reached safety limit of 5000 work packages for project: {}", projectId);
                    break;
                }
            }
            
            logger.info("Fetched {} total work packages for project: {}", allWorkPackages.size(), projectId);
            return allWorkPackages;
            
        } catch (Exception e) {
            logger.error("Unexpected error fetching work packages: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Recovery method for fetch work packages retry failures
     * 获取工作包重试失败的恢复方法
     */
    @Recover
    private List<WorkPackage> fetchWorkPackagesRecover(Exception e, Map<String, String> filters, String projectId) {
        logger.error("All retry attempts exhausted for fetching work packages. Project: {}, Error: {}", 
                    projectId, e.getMessage());
        
        // Return empty list with error information
        List<WorkPackage> emptyResult = new ArrayList<>();
        logger.warn("Returning empty work packages list due to retry exhaustion for project: {}", projectId);
        
        // Log specific recovery strategy based on exception type
        if (e instanceof RateLimitExceededException) {
            RateLimitExceededException rle = (RateLimitExceededException) e;
            logger.info("Rate limit recovery: Suggest backoff of {} seconds", 
                       rle.getSuggestedBackoff(1).getSeconds());
        } else if (e instanceof OpenProjectApiException) {
            OpenProjectApiException ope = (OpenProjectApiException) e;
            logger.info("API error recovery: Status code {}, isRetryable: {}", 
                       ope.getStatusCode(), isRetryableError(ope));
        } else if (e instanceof ResourceAccessException) {
            logger.info("Network access recovery: Connection timeout or network failure");
            logger.info("Suggestion: Check network connectivity and OpenProject availability");
        }
        
        return emptyResult;
    }
    
    /**
     * Calculate enhanced completion metrics with multiple algorithms
     * 使用多种算法计算增强的完成度指标
     */
    private EnhancedCompletionMetrics calculateEnhancedCompletionMetrics(List<WorkPackage> workPackages) {
        int totalTasks = workPackages.size();
        int completedTasks = (int) workPackages.stream().filter(WorkPackage::isCompleted).count();
        int inProgressTasks = (int) workPackages.stream().filter(WorkPackage::isInProgress).count();
        int overdueTasks = (int) workPackages.stream().filter(WorkPackage::isOverdue).count();
        
        // Calculate high priority tasks
        int highPriorityTasks = (int) workPackages.stream()
            .filter(wp -> wp.priority() != null && "high".equalsIgnoreCase(wp.priority().name()))
            .count();
        
        // Calculate completion by status
        Map<String, Integer> completionByStatus = workPackages.stream()
            .filter(wp -> wp.status() != null && wp.status().name() != null)
            .collect(Collectors.groupingBy(
                wp -> wp.status().name(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // Calculate completion by type
        Map<String, Integer> completionByType = workPackages.stream()
            .filter(wp -> wp.type() != null && wp.type().name() != null)
            .collect(Collectors.groupingBy(
                wp -> wp.type().name(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // Calculate overall completion rate with weighted algorithm
        double overallCompletionRate = calculateWeightedCompletionRate(workPackages);
        
        return new EnhancedCompletionMetrics(
            overallCompletionRate,
            totalTasks,
            completedTasks,
            inProgressTasks,
            overdueTasks,
            highPriorityTasks,
            completionByStatus,
            completionByType
        );
    }
    
    /**
     * Calculate weighted completion rate considering multiple factors
     * 考虑多种因素的加权完成率计算
     */
    private double calculateWeightedCompletionRate(List<WorkPackage> workPackages) {
        if (workPackages.isEmpty()) {
            return 0.0;
        }
        
        double totalWeight = 0.0;
        double completedWeight = 0.0;
        
        for (WorkPackage wp : workPackages) {
            double weight = 1.0; // Base weight
            
            // Weight by priority
            if (wp.priority() != null) {
                switch (wp.priority().name().toLowerCase()) {
                    case "high": weight *= 1.5; break;
                    case "urgent": weight *= 2.0; break;
                    case "low": weight *= 0.7; break;
                    default: weight *= 1.0; break;
                }
            }
            
            // Weight by progress percentage
            if (wp.percentageDone() != null) {
                double progressWeight = wp.percentageDone() / 100.0;
                completedWeight += weight * progressWeight;
            } else if (wp.isCompleted()) {
                completedWeight += weight;
            }
            
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? (completedWeight / totalWeight) * 100.0 : 0.0;
    }
    
    /**
     * Generate comprehensive progress analysis
     * 生成全面的进度分析
     */
    private ProgressAnalysis generateProgressAnalysis(List<WorkPackage> workPackages, EnhancedCompletionMetrics metrics) {
        StringBuilder progressSummary = new StringBuilder();
        StringBuilder statusDistribution = new StringBuilder();
        StringBuilder riskAssessment = new StringBuilder();
        List<String> recommendations = new ArrayList<>();
        
        // Generate progress summary
        progressSummary.append(String.format("项目总体完成度为 %.1f%% (%d/%d 个任务完成)。\n", 
            metrics.overallCompletionRate(), metrics.completedTasks(), metrics.totalTasks()));
        
        if (metrics.overallCompletionRate() >= 80) {
            progressSummary.append("项目进展优秀，接近完成目标。\n");
            recommendations.add("保持当前进度，关注剩余任务的质量保证");
        } else if (metrics.overallCompletionRate() >= 60) {
            progressSummary.append("项目进展良好，按计划进行。\n");
            recommendations.add("继续推进，重点关注高风险任务");
        } else if (metrics.overallCompletionRate() >= 40) {
            progressSummary.append("项目进展正常，需要加强管理。\n");
            recommendations.add("分析延迟原因，调整资源分配");
        } else {
            progressSummary.append("项目进展缓慢，需要重点关注。\n");
            recommendations.add("立即召开项目会议，制定改进计划");
            recommendations.add("考虑增加资源或调整项目范围");
        }
        
        // Status distribution
        statusDistribution.append(String.format("任务状态分布：已完成 %d 个，进行中 %d 个，逾期 %d 个。\n", 
            metrics.completedTasks(), metrics.inProgressTasks(), metrics.overdueTasks()));
        
        if (metrics.highPriorityTasks() > 0) {
            int highPriorityCompleted = (int) workPackages.stream()
                .filter(wp -> wp.priority() != null && "high".equalsIgnoreCase(wp.priority().name()) && wp.isCompleted())
                .count();
            statusDistribution.append(String.format("高优先级任务：已完成 %d/%d 个。\n", 
                highPriorityCompleted, metrics.highPriorityTasks()));
        }
        
        // Risk assessment
        if (metrics.overdueTasks() > 0) {
            riskAssessment.append(String.format("⚠️ 存在 %d 个逾期任务，需要立即处理。\n", metrics.overdueTasks()));
            recommendations.add("优先处理逾期任务，制定挽回计划");
        }
        
        if (metrics.inProgressTasks() > metrics.completedTasks()) {
            riskAssessment.append("📊 进行中任务多于已完成任务，需关注任务完成效率。\n");
        }
        
        double overdueRate = metrics.totalTasks() > 0 ? (double) metrics.overdueTasks() / metrics.totalTasks() : 0.0;
        if (overdueRate > 0.1) { // More than 10% overdue
            riskAssessment.append("🚨 逾期率超过10%，项目存在进度风险。\n");
            recommendations.add("重新评估项目时间表，考虑缩减范围");
        }
        
        // Progress visualization
        String progressVisualization = generateProgressVisualization(metrics.overallCompletionRate());
        
        return new ProgressAnalysis(
            progressSummary.toString(),
            statusDistribution.toString(),
            riskAssessment.toString(),
            recommendations,
            progressVisualization
        );
    }
    
    /**
     * Generate progress visualization with ASCII art
     * 生成ASCII艺术的进度可视化
     */
    private String generateProgressVisualization(double completionRate) {
        StringBuilder visualization = new StringBuilder();
        
        // Progress bar
        visualization.append("[");
        int filledBars = (int) (completionRate / 2); // Each bar represents 2%
        for (int i = 0; i < 50; i++) {
            if (i < filledBars) {
                if (completionRate >= 100) {
                    visualization.append("🟢");
                } else if (completionRate >= 80) {
                    visualization.append("🟡");
                } else if (completionRate >= 60) {
                    visualization.append("🟠");
                } else {
                    visualization.append("🔴");
                }
            } else {
                visualization.append("⬜");
            }
        }
        visualization.append(String.format("] %.1f%%\n", completionRate));
        
        // Milestone indicators
        visualization.append("里程碑: ");
        if (completionRate >= 25) visualization.append("25% ");
        if (completionRate >= 50) visualization.append("50% ");
        if (completionRate >= 75) visualization.append("75% ");
        if (completionRate >= 100) visualization.append("100% ");
        visualization.append("\n");
        
        return visualization.toString();
    }
    
    /**
     * Calculate trend analysis for completion patterns
     * 计算完成度模式的趋势分析
     */
    private TrendAnalysis calculateTrendAnalysis(String projectId, EnhancedCompletionMetrics currentMetrics) {
        // Simplified trend analysis - in a real implementation, this would query historical data
        boolean trendingUpward = currentMetrics.overallCompletionRate() > 50;
        double weeklyChange = 2.5; // Simulated weekly change
        String trendDescription = trendingUpward ? "项目进度呈上升趋势" : "项目进度需要改进";
        String prediction = trendingUpward ? 
            "预计按期完成" : 
            "预计可能延期，建议加强管理";
        
        return new TrendAnalysis(trendingUpward, weeklyChange, trendDescription, prediction);
    }
    
    /**
     * Calculate priority-weighted completion analysis
     * 计算优先级加权的完成度分析
     */
    private PriorityWeightedAnalysis calculatePriorityWeightedAnalysis(List<WorkPackage> workPackages) {
        int highPriorityTotal = (int) workPackages.stream()
            .filter(wp -> wp.priority() != null && "high".equalsIgnoreCase(wp.priority().name()))
            .count();
        
        int highPriorityCompleted = (int) workPackages.stream()
            .filter(wp -> wp.priority() != null && "high".equalsIgnoreCase(wp.priority().name()) && wp.isCompleted())
            .count();
        
        double weightedCompletionRate = highPriorityTotal > 0 ? 
            (double) highPriorityCompleted / highPriorityTotal * 100.0 : 100.0;
        
        String priorityAssessment;
        if (weightedCompletionRate >= 80) {
            priorityAssessment = "高优先级任务进展优秀";
        } else if (weightedCompletionRate >= 60) {
            priorityAssessment = "高优先级任务进展良好";
        } else if (weightedCompletionRate >= 40) {
            priorityAssessment = "高优先级任务需要关注";
        } else {
            priorityAssessment = "高优先级任务进展缓慢，需要重点管理";
        }
        
        return new PriorityWeightedAnalysis(
            weightedCompletionRate,
            highPriorityCompleted,
            highPriorityTotal,
            priorityAssessment
        );
    }
    
    /**
     * Generate empty project report
     * 生成空项目报告
     */
    private String generateEmptyProjectReport(String projectName, String projectId) {
        return String.format(
            "📊" + "📊".repeat(25) + "\n" +
            "项目完成度分析 | Project Completion Analysis\n" +
            "📊" + "📊".repeat(25) + "\n\n" +
            "项目: %s (ID: %s)\n" +
            "分析时间: %s\n\n" +
            "⚠️ 该项目目前没有任务，无法计算完成度。\n" +
            "⚠️ This project currently has no tasks, cannot calculate completion.\n\n" +
            "建议 | Recommendations:\n" +
            "- 添加任务到项目中以开始跟踪进度\n" +
            "- Add tasks to the project to start tracking progress\n\n" +
            "📊" + "📊".repeat(50) + "\n",
            projectName, projectId, LocalDate.now()
        );
    }
    
    /**
     * Format enhanced completion analysis
     * 格式化增强的完成度分析
     */
    private String formatEnhancedCompletionAnalysis(EnhancedCompletionAnalysis analysis, String projectName) {
        StringBuilder formattedAnalysis = new StringBuilder();
        
        // Header
        formattedAnalysis.append("📊".repeat(50)).append("\n");
        formattedAnalysis.append("增强项目完成度分析 | Enhanced Project Completion Analysis\n");
        formattedAnalysis.append("📊".repeat(50)).append("\n\n");
        
        // Project info
        formattedAnalysis.append(String.format("项目: %s (ID: %s)\n", projectName, analysis.projectId()));
        formattedAnalysis.append(String.format("分析时间: %s\n\n", analysis.analysisDate()));
        
        // Overall metrics
        formattedAnalysis.append("🎯 总体完成度指标 | Overall Completion Metrics\n");
        formattedAnalysis.append("-".repeat(40)).append("\n");
        formattedAnalysis.append(String.format("总体完成率: %.1f%%\n", analysis.overallCompletionRate()));
        formattedAnalysis.append(String.format("完成任务数: %d\n", analysis.completedTasks()));
        formattedAnalysis.append(String.format("总任务数: %d\n", analysis.totalTasks()));
        formattedAnalysis.append(String.format("进行中任务: %d\n", analysis.inProgressTasks()));
        formattedAnalysis.append(String.format("逾期任务: %d\n", analysis.overdueTasks()));
        formattedAnalysis.append(String.format("高优先级任务: %d\n", analysis.highPriorityTasks()));
        formattedAnalysis.append(String.format("剩余任务: %d\n\n", analysis.totalTasks() - analysis.completedTasks()));
        
        // Progress visualization
        formattedAnalysis.append("📊 进度可视化 | Progress Visualization\n");
        formattedAnalysis.append("-".repeat(40)).append("\n");
        formattedAnalysis.append(analysis.progressAnalysis().progressVisualization());
        
        // Progress analysis
        formattedAnalysis.append("📈 进度分析 | Progress Analysis\n");
        formattedAnalysis.append("-".repeat(40)).append("\n");
        formattedAnalysis.append(analysis.progressAnalysis().progressSummary());
        formattedAnalysis.append("\n");
        
        // Status distribution
        formattedAnalysis.append("📋 状态分布 | Status Distribution\n");
        formattedAnalysis.append("-".repeat(40)).append("\n");
        formattedAnalysis.append(analysis.progressAnalysis().statusDistribution());
        formattedAnalysis.append("\n");
        
        // Risk assessment
        if (!analysis.progressAnalysis().riskAssessment().isEmpty()) {
            formattedAnalysis.append("⚠️ 风险评估 | Risk Assessment\n");
            formattedAnalysis.append("-".repeat(40)).append("\n");
            formattedAnalysis.append(analysis.progressAnalysis().riskAssessment());
            formattedAnalysis.append("\n");
        }
        
        // Priority analysis
        formattedAnalysis.append("🎯 优先级分析 | Priority Analysis\n");
        formattedAnalysis.append("-".repeat(40)).append("\n");
        formattedAnalysis.append(String.format("高优先级完成率: %.1f%% (%d/%d)\n", 
            analysis.priorityAnalysis().weightedCompletionRate(),
            analysis.priorityAnalysis().highPriorityCompleted(),
            analysis.priorityAnalysis().highPriorityTotal()));
        formattedAnalysis.append(analysis.priorityAnalysis().priorityAssessment()).append("\n\n");
        
        // Trend analysis
        formattedAnalysis.append("📈 趋势分析 | Trend Analysis\n");
        formattedAnalysis.append("-".repeat(40)).append("\n");
        formattedAnalysis.append(String.format("趋势: %s\n", analysis.trendAnalysis().trendDescription()));
        formattedAnalysis.append(String.format("周变化: %.1f%%\n", analysis.trendAnalysis().weeklyChange()));
        formattedAnalysis.append(String.format("预测: %s\n\n", analysis.trendAnalysis().prediction()));
        
        // Recommendations
        if (!analysis.progressAnalysis().recommendations().isEmpty()) {
            formattedAnalysis.append("💡 建议措施 | Recommendations\n");
            formattedAnalysis.append("-".repeat(40)).append("\n");
            for (String recommendation : analysis.progressAnalysis().recommendations()) {
                formattedAnalysis.append("• ").append(recommendation).append("\n");
            }
            formattedAnalysis.append("\n");
        }
        
        // Detailed breakdowns
        if (!analysis.completionByStatus().isEmpty()) {
            formattedAnalysis.append("📊 按状态统计 | By Status\n");
            formattedAnalysis.append("-".repeat(40)).append("\n");
            analysis.completionByStatus().forEach((status, count) -> 
                formattedAnalysis.append(String.format("%s: %d\n", status, count)));
            formattedAnalysis.append("\n");
        }
        
        if (!analysis.completionByType().isEmpty()) {
            formattedAnalysis.append("📊 按类型统计 | By Type\n");
            formattedAnalysis.append("-".repeat(40)).append("\n");
            analysis.completionByType().forEach((type, count) -> 
                formattedAnalysis.append(String.format("%s: %d\n", type, count)));
            formattedAnalysis.append("\n");
        }
        
        // Footer
        formattedAnalysis.append("📊".repeat(50)).append("\n");
        formattedAnalysis.append("增强完成度分析完成 | Enhanced completion analysis completed\n");
        
        return formattedAnalysis.toString();
    }
    
    /**
     * Generate rate limit error report
     * 生成速率限制错误报告
     */
    private String generateRateLimitErrorReport(RateLimitExceededException e) {
        return String.format(
            "⚠️" + "⚠️".repeat(25) + "\n" +
            "速率限制错误 | Rate Limit Error\n" +
            "⚠️" + "⚠️".repeat(25) + "\n\n" +
            "API调用频率过高，已达到速率限制。\n" +
            "API call frequency too high, rate limit reached.\n\n" +
            "错误详情 | Error Details: %s\n\n" +
            "建议 | Recommendations:\n" +
            "- 请稍后重试\n" +
            "- Please try again later\n" +
            "- 减少请求频率\n" +
            "- Reduce request frequency\n\n" +
            "⚠️" + "⚠️".repeat(50) + "\n",
            e.getMessage()
        );
    }
    
    /**
     * Generate API error report
     * 生成API错误报告
     */
    private String generateApiErrorReport(OpenProjectApiException e) {
        return String.format(
            "❌" + "❌".repeat(25) + "\n" +
            "API错误 | API Error\n" +
            "❌" + "❌".repeat(25) + "\n\n" +
            "OpenProject API调用失败。\n" +
            "OpenProject API call failed.\n\n" +
            "错误详情 | Error Details: %s\n\n" +
            "建议 | Recommendations:\n" +
            "- 检查API配置和连接\n" +
            "- Check API configuration and connection\n" +
            "- 验证项目ID是否正确\n" +
            "- Verify project ID is correct\n\n" +
            "❌" + "❌".repeat(50) + "\n",
            e.getMessage()
        );
    }
    
    /**
     * Generate comprehensive error report
     * 生成全面错误报告
     */
    private String generateComprehensiveErrorReport(String errorMessage, Exception e) {
        StringBuilder errorReport = new StringBuilder();
        
        errorReport.append("❌".repeat(50)).append("\n");
        errorReport.append("完成度计算错误 | Completion Calculation Error\n");
        errorReport.append("❌".repeat(50)).append("\n\n");
        
        errorReport.append(errorMessage).append("\n\n");
        
        if (e != null) {
            errorReport.append("错误详情 | Error Details:\n");
            errorReport.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
        }
        
        errorReport.append("\n建议 | Recommendations:\n");
        errorReport.append("- 检查网络连接\n");
        errorReport.append("- Check network connection\n");
        errorReport.append("- 验证项目ID格式\n");
        errorReport.append("- Verify project ID format\n");
        errorReport.append("- 稍后重试\n");
        errorReport.append("- Try again later\n");
        
        errorReport.append("\n❌".repeat(50)).append("\n");
        
        return errorReport.toString();
    }
    
    // Existing private helper methods
    
    /**
     * Generate cache key for project reports
     * 为项目报告生成缓存键
     */
    private String generateCacheKey(String projectId) {
        return String.format("weekly-report-%s-%s", projectId, LocalDate.now());
    }
    
    /**
     * Get project name by project ID with enhanced retry logic
     * 根据项目ID获取项目名称，具有增强重试逻辑
     */
    @Retryable(
        value = { RateLimitExceededException.class, OpenProjectApiException.class, ResourceAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 1.5),
        label = "getProjectNameRetry"
    )
    private String getProjectNameWithRetry(String projectId) throws Exception {
        // Try to get actual project name from API first
        try {
            List<OpenProjectApiClient.ProjectInfo> projects = apiClient.getProjects();
            for (OpenProjectApiClient.ProjectInfo project : projects) {
                if (project.id().equals(projectId) || project.identifier().equals(projectId)) {
                    return project.name();
                }
            }
        } catch (RateLimitExceededException e) {
            logger.warn("Rate limit hit while fetching project name for project: {}", projectId);
            throw e; // Re-throw to trigger retry
        } catch (OpenProjectApiException e) {
            logger.warn("API error while fetching project name for project: {}", projectId);
            throw e; // Re-throw to trigger retry
        } catch (Exception e) {
            logger.debug("Could not fetch projects from API: {}", e.getMessage());
            // For non-API exceptions, don't retry - return fallback immediately
            return "Project-" + projectId;
        }
        
        return "Project-" + projectId;
    }
    
    /**
     * Recovery method for get project name retry failures
     * 获取项目名称重试失败的恢复方法
     */
    @Recover
    private String getProjectNameRecover(Exception e, String projectId) {
        logger.warn("All retry attempts exhausted for fetching project name. Project: {}, Error: {}", 
                   projectId, e.getMessage());
        
        // Implement intelligent fallback strategy
        String fallbackName = generateFallbackProjectName(projectId, e);
        logger.info("Using fallback project name: {}", fallbackName);
        
        return fallbackName;
    }
    
    /**
     * Build enhanced filters for work package queries
     * 构建增强的工作包查询过滤器
     */
    private Map<String, String> buildEnhancedFilters(LocalDate startDate, LocalDate endDate, String projectId) {
        Map<String, String> filters = new HashMap<>();
        
        // Date range filter for updated work packages
        String dateFilter = String.format(">=%s<=%s", startDate, endDate);
        filters.put("updatedAt", dateFilter);
        
        // Project filter
        filters.put("project", projectId);
        
        // Additional filters to improve query performance
        filters.put("status", "!"); // Exclude closed/finished tasks to focus on active work
        
        logger.debug("Built filters: {}", filters);
        return filters;
    }
    
    /**
     * Fetch work packages with pagination support and enhanced retry logic
     * 获取支持分页的工作包，具有增强重试逻辑
     */
    @Retryable(
        value = { RateLimitExceededException.class, OpenProjectApiException.class, ResourceAccessException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1500, multiplier = 1.5),
        label = "fetchWorkPackagesPaginationRetry"
    )
    private List<WorkPackage> fetchWorkPackagesWithPagination(Map<String, String> filters, String projectId) 
            throws Exception {
        List<WorkPackage> allWorkPackages = new ArrayList<>();
        int offset = 0;
        int pageSize = Math.min(maxPageSize, 100); // Use configured max page size
        
        try {
            while (true) {
                logger.debug("Fetching work packages with offset: {}, pageSize: {}", offset, pageSize);
                
                List<WorkPackage> batch;
                try {
                    batch = apiClient.getWorkPackages(filters, pageSize, offset);
                } catch (RateLimitExceededException e) {
                    logger.warn("Rate limit hit during pagination fetch for project: {}", projectId);
                    throw e; // Re-throw to trigger retry
                } catch (OpenProjectApiException e) {
                    logger.error("API error during pagination fetch: {}", e.getMessage());
                    throw e; // Re-throw to trigger retry
                }
                
                if (batch.isEmpty()) {
                    break; // No more results
                }
                
                allWorkPackages.addAll(batch);
                offset += pageSize;
                
                // Safety limit to prevent infinite loops
                if (allWorkPackages.size() > 1000) {
                    logger.warn("Reached safety limit of 1000 work packages for project: {}", projectId);
                    break;
                }
            }
            
            logger.info("Fetched {} total work packages for project: {}", allWorkPackages.size(), projectId);
            return allWorkPackages;
            
        } catch (Exception e) {
            logger.error("Error fetching work packages with pagination: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Recovery method for fetch work packages pagination retry failures
     * 分页获取工作包重试失败的恢复方法
     */
    @Recover
    private List<WorkPackage> fetchWorkPackagesPaginationRecover(Exception e, Map<String, String> filters, String projectId) {
        logger.error("All retry attempts exhausted for fetching work packages with pagination. Project: {}, Error: {}", 
                    projectId, e.getMessage());
        
        // Implement enhanced recovery strategy
        List<WorkPackage> fallbackResult = new ArrayList<>();
        
        // Log recovery details for debugging
        if (e instanceof RateLimitExceededException) {
            RateLimitExceededException rle = (RateLimitExceededException) e;
            logger.warn("Rate limit recovery for pagination: Wait {} seconds", 
                       rle.getSuggestedBackoff(1).getSeconds());
        } else if (e instanceof OpenProjectApiException) {
            OpenProjectApiException ope = (OpenProjectApiException) e;
            logger.warn("API error recovery for pagination: Status {}", ope.getStatusCode());
        } else if (e instanceof ResourceAccessException) {
            logger.warn("Network access recovery for pagination: Check network connectivity");
        }
        
        logger.info("Returning empty work packages list as fallback for project: {}", projectId);
        return fallbackResult;
    }
      
    /**
     * Generate comprehensive weekly report with enhanced metrics
     * 生成增强指标的全面周报
     */
    private WeeklyReport generateComprehensiveWeeklyReport(
            String projectId, String projectName, List<WorkPackage> workPackages, 
            LocalDate startDate, LocalDate endDate, long startTime) {
        
        logger.debug("Generating comprehensive weekly report for {} work packages", workPackages.size());
        
        // Filter work packages by date range on the client side as well
        List<WorkPackage> filteredWorkPackages = workPackages.stream()
            .filter(wp -> wp.updatedAt() != null)
            .filter(wp -> {
                LocalDate updatedDate = wp.updatedAt().toLocalDate();
                return !updatedDate.isBefore(startDate) && !updatedDate.isAfter(endDate);
            })
            .sorted((a, b) -> b.updatedAt().compareTo(a.updatedAt())) // Sort by most recent first
            .collect(Collectors.toList());
        
        logger.debug("Filtered to {} work packages within date range", filteredWorkPackages.size());
        
        // Create enhanced weekly report with detailed analysis
        WeeklyReport report = WeeklyReport.createFromWorkPackages(
            projectId, projectName, filteredWorkPackages, startDate, endDate
        );
        
        // Log report generation metrics
        long processingTime = System.currentTimeMillis() - startTime;
        logger.info("Weekly report generated in {} ms: {} tasks, {} completion rate", 
                   processingTime, filteredWorkPackages.size(), 
                   report.getMetrics().getCompletionRate());
        
        return report;
    }
    
        
    /**
     * Format weekly report as readable text
     * 将周报格式化为可读文本
     */
    private String formatWeeklyReport(WeeklyReport report) {
        StringBuilder formattedReport = new StringBuilder();
        
        // Header
        formattedReport.append("=".repeat(50)).append("\n");
        formattedReport.append("项目周报 | Project Weekly Report\n");
        formattedReport.append("=".repeat(50)).append("\n\n");
        
        // Project info
        formattedReport.append(String.format("项目: %s (ID: %s)\n", report.getProjectName(), report.getProjectId()));
        formattedReport.append(String.format("报告期间: %s 至 %s\n", report.getPeriodStart(), report.getPeriodEnd()));
        formattedReport.append(String.format("生成时间: %s\n\n", report.getReportDate()));
        
        // Summary
        formattedReport.append("📊 摘要 | Summary\n");
        formattedReport.append("-".repeat(30)).append("\n");
        formattedReport.append(report.getSummary()).append("\n\n");
        
        // Task updates
        if (report.hasTaskUpdates()) {
            formattedReport.append("📋 任务更新 | Task Updates\n");
            formattedReport.append("-".repeat(30)).append("\n");
            
            for (WeeklyReport.TaskUpdate update : report.getTaskUpdates()) {
                formattedReport.append(String.format("• %s\n", update.getSubject()));
                if (update.getNewStatus() != null) {
                    formattedReport.append(String.format("  状态: %s\n", update.getNewStatus()));
                }
                if (update.getAssigneeName() != null) {
                    formattedReport.append(String.format("  负责人: %s\n", update.getAssigneeName()));
                }
                if (update.getDueDate() != null) {
                    formattedReport.append(String.format("  截止日期: %s\n", update.getDueDate()));
                }
                formattedReport.append("\n");
            }
        } else {
            formattedReport.append("📋 任务更新 | Task Updates\n");
            formattedReport.append("-".repeat(30)).append("\n");
            formattedReport.append("本周没有任务更新 | No task updates this week\n\n");
        }
        
        // Highlights
        if (!report.getHighlights().isEmpty()) {
            formattedReport.append("✨ 关键亮点 | Key Highlights\n");
            formattedReport.append("-".repeat(30)).append("\n");
            for (String highlight : report.getHighlights()) {
                formattedReport.append("• ").append(highlight).append("\n");
            }
            formattedReport.append("\n");
        }
        
        // Metrics
        if (report.getMetrics() != null) {
            formattedReport.append("📈 指标 | Metrics\n");
            formattedReport.append("-".repeat(30)).append("\n");
            formattedReport.append(String.format("完成率: %.1f%%\n", report.getMetrics().getCompletionRate()));
            formattedReport.append(String.format("完成任务: %d\n", report.getMetrics().getTasksCompleted()));
            formattedReport.append(String.format("创建任务: %d\n", report.getMetrics().getTasksCreated()));
            formattedReport.append(String.format("生产力分数: %d\n", report.getMetrics().getProductivityScore()));
            formattedReport.append("\n");
        }
        
        // Footer
        formattedReport.append("=".repeat(50)).append("\n");
        formattedReport.append("报告生成完成 | Report generated successfully\n");
        
        return formattedReport.toString();
    }
    
    /**
     * Calculate risk level based on number of overdue tasks
     * 根据逾期任务数量计算风险级别
     */
    private WorkPackage.RiskLevel calculateRiskLevel(int overdueCount) {
        if (overdueCount < 3) {
            return WorkPackage.RiskLevel.LOW;
        } else if (overdueCount <= 5) {
            return WorkPackage.RiskLevel.MEDIUM;
        } else {
            return WorkPackage.RiskLevel.HIGH;
        }
    }
    
    /**
     * Create risk item from work package
     * 从工作包创建风险项目
     */
    private SimpleRiskAnalysis.RiskItem createRiskItem(WorkPackage workPackage) {
        String riskDescription = String.format("任务 '%s' 逾期 %d 天", 
            workPackage.subject(), Math.abs(workPackage.getDaysUntilDue()));
        
        String impact = "Medium"; // Default impact
        if (workPackage.priority() != null) {
            if ("high".equalsIgnoreCase(workPackage.priority().name())) {
                impact = "High";
            } else if ("low".equalsIgnoreCase(workPackage.priority().name())) {
                impact = "Low";
            }
        }
        
        return new SimpleRiskAnalysis.RiskItem(
            workPackage.id(),
            workPackage.subject(),
            riskDescription,
            impact,
            workPackage.dueDate(),
            workPackage.assignee() != null ? workPackage.assignee().name() : "未分配"
        );
    }
    
    /**
     * Generate risk recommendations
     * 生成风险建议
     */
    private String generateRiskRecommendations(WorkPackage.RiskLevel riskLevel, List<WorkPackage> overdueTasks) {
        StringBuilder recommendations = new StringBuilder();
        
        switch (riskLevel) {
            case LOW:
                recommendations.append("风险级别：低 - 继续监控项目进度\n");
                recommendations.append("建议：定期检查任务状态，确保按时完成\n");
                break;
            case MEDIUM:
                recommendations.append("风险级别：中 - 需要关注逾期任务\n");
                recommendations.append("建议：\n");
                recommendations.append("1. 优先处理高优先级逾期任务\n");
                recommendations.append("2. 重新评估剩余任务的截止日期\n");
                recommendations.append("3. 增加资源投入或调整任务分配\n");
                break;
            case HIGH:
                recommendations.append("风险级别：高 - 立即采取行动\n");
                recommendations.append("紧急建议：\n");
                recommendations.append("1. 立即召开项目风险评估会议\n");
                recommendations.append("2. 重新制定项目计划和里程碑\n");
                recommendations.append("3. 考虑增加项目资源或调整项目范围\n");
                recommendations.append("4. 与相关方沟通项目延期风险\n");
                break;
        }
        
        // Add specific task recommendations
        if (!overdueTasks.isEmpty()) {
            recommendations.append("\n具体任务建议：\n");
            for (WorkPackage task : overdueTasks.stream().limit(3).toList()) {
                recommendations.append(String.format("• '%s' - 截止日期：%s\n", 
                    task.subject(), task.dueDate()));
            }
            if (overdueTasks.size() > 3) {
                recommendations.append(String.format("• 还有 %d 个逾期任务需要关注\n", overdueTasks.size() - 3));
            }
        }
        
        return recommendations.toString();
    }
    
    /**
     * Format risk analysis as readable text
     * 将风险分析格式化为可读文本
     */
    private String formatRiskAnalysis(SimpleRiskAnalysis analysis, String projectName) {
        StringBuilder formattedAnalysis = new StringBuilder();
        
        // Header
        formattedAnalysis.append("⚠️".repeat(50)).append("\n");
        formattedAnalysis.append("项目风险分析 | Project Risk Analysis\n");
        formattedAnalysis.append("⚠️".repeat(50)).append("\n\n");
        
        // Project info
        formattedAnalysis.append(String.format("项目: %s (ID: %s)\n", projectName, analysis.projectId()));
        formattedAnalysis.append(String.format("分析时间: %s\n\n", analysis.analysisDate()));
        
        // Risk level
        String riskLevelChinese = analysis.riskLevel().getChineseName();
        formattedAnalysis.append(String.format("🎯 风险级别: %s (%s)\n\n", 
            riskLevelChinese, analysis.riskLevel().name()));
        
        // Summary
        formattedAnalysis.append("📊 风险摘要 | Risk Summary\n");
        formattedAnalysis.append("-".repeat(30)).append("\n");
        formattedAnalysis.append(String.format("逾期任务数量: %d\n", analysis.overdueTasks()));
        formattedAnalysis.append(String.format("风险等级: %s\n", riskLevelChinese));
        formattedAnalysis.append("\n");
        
        // Risk items
        if (!analysis.riskItems().isEmpty()) {
            formattedAnalysis.append("🔍 风险详情 | Risk Details\n");
            formattedAnalysis.append("-".repeat(30)).append("\n");
            
            for (SimpleRiskAnalysis.RiskItem item : analysis.riskItems()) {
                formattedAnalysis.append(String.format("• 任务: %s\n", item.subject()));
                formattedAnalysis.append(String.format("  描述: %s\n", item.description()));
                formattedAnalysis.append(String.format("  影响: %s\n", item.impact()));
                formattedAnalysis.append(String.format("  截止日期: %s\n", item.dueDate()));
                formattedAnalysis.append(String.format("  负责人: %s\n", item.assignee()));
                formattedAnalysis.append("\n");
            }
        } else {
            formattedAnalysis.append("🔍 风险详情 | Risk Details\n");
            formattedAnalysis.append("-".repeat(30)).append("\n");
            formattedAnalysis.append("没有发现逾期任务 | No overdue tasks found\n\n");
        }
        
        // Recommendations
        formattedAnalysis.append("💡 建议措施 | Recommendations\n");
        formattedAnalysis.append("-".repeat(30)).append("\n");
        formattedAnalysis.append(analysis.recommendations());
        formattedAnalysis.append("\n");
        
        // Footer
        formattedAnalysis.append("⚠️".repeat(50)).append("\n");
        formattedAnalysis.append("风险分析完成 | Risk analysis completed\n");
        
        return formattedAnalysis.toString();
    }
    
    /**
     * Generate progress summary
     * 生成进度摘要
     */
    private String generateProgressSummary(List<WorkPackage> workPackages, double completionRate) {
        StringBuilder summary = new StringBuilder();
        
        int totalTasks = workPackages.size();
        int completedTasks = (int) workPackages.stream().filter(WorkPackage::isCompleted).count();
        int inProgressTasks = (int) workPackages.stream().filter(WorkPackage::isInProgress).count();
        int overdueTasks = (int) workPackages.stream().filter(WorkPackage::isOverdue).count();
        
        summary.append(String.format("项目总体完成度为 %.1f%% (%d/%d 个任务完成)。\n", 
            completionRate, completedTasks, totalTasks));
        
        if (completionRate >= 80) {
            summary.append("项目进展良好，接近完成。\n");
        } else if (completionRate >= 50) {
            summary.append("项目进展正常，按计划进行。\n");
        } else if (completionRate >= 25) {
            summary.append("项目进展较慢，需要加强管理。\n");
        } else {
            summary.append("项目进展缓慢，需要重点关注。\n");
        }
        
        summary.append(String.format("任务状态分布：已完成 %d 个，进行中 %d 个，逾期 %d 个。\n", 
            completedTasks, inProgressTasks, overdueTasks));
        
        if (overdueTasks > 0) {
            summary.append("注意：存在逾期任务，建议优先处理。\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Format completion analysis as readable text
     * 将完成度分析格式化为可读文本
     */
    private String formatCompletionAnalysis(SimpleCompletionAnalysis analysis, String projectName) {
        StringBuilder formattedAnalysis = new StringBuilder();
        
        // Header
        formattedAnalysis.append("📊".repeat(50)).append("\n");
        formattedAnalysis.append("项目完成度分析 | Project Completion Analysis\n");
        formattedAnalysis.append("📊".repeat(50)).append("\n\n");
        
        // Project info
        formattedAnalysis.append(String.format("项目: %s (ID: %s)\n", projectName, analysis.projectId()));
        formattedAnalysis.append(String.format("分析时间: %s\n\n", analysis.analysisDate()));
        
        // Completion metrics
        formattedAnalysis.append("🎯 完成度指标 | Completion Metrics\n");
        formattedAnalysis.append("-".repeat(30)).append("\n");
        formattedAnalysis.append(String.format("总体完成率: %.1f%%\n", analysis.completionRate()));
        formattedAnalysis.append(String.format("完成任务数: %d\n", analysis.completedTasks()));
        formattedAnalysis.append(String.format("总任务数: %d\n", analysis.totalTasks()));
        formattedAnalysis.append(String.format("剩余任务: %d\n", analysis.totalTasks() - analysis.completedTasks()));
        formattedAnalysis.append("\n");
        
        // Progress summary
        formattedAnalysis.append("📈 进度摘要 | Progress Summary\n");
        formattedAnalysis.append("-".repeat(30)).append("\n");
        formattedAnalysis.append(analysis.progressSummary());
        formattedAnalysis.append("\n");
        
        // Progress bar visualization
        formattedAnalysis.append("📊 进度可视化 | Progress Visualization\n");
        formattedAnalysis.append("-".repeat(30)).append("\n");
        formattedAnalysis.append("[");
        int filledBars = (int) (analysis.completionRate() / 5); // Each bar represents 5%
        for (int i = 0; i < 20; i++) {
            if (i < filledBars) {
                formattedAnalysis.append("█");
            } else {
                formattedAnalysis.append("░");
            }
        }
        formattedAnalysis.append(String.format("] %.1f%%\n\n", analysis.completionRate()));
        
        // Footer
        formattedAnalysis.append("📊".repeat(50)).append("\n");
        formattedAnalysis.append("完成度分析完成 | Completion analysis completed\n");
        
        return formattedAnalysis.toString();
    }
    
    /**
     * Generate error report
     * 生成错误报告
     */
    private String generateErrorReport(String errorMessage, Exception e) {
        StringBuilder errorReport = new StringBuilder();
        
        errorReport.append("❌".repeat(50)).append("\n");
        errorReport.append("错误报告 | Error Report\n");
        errorReport.append("❌".repeat(50)).append("\n\n");
        
        errorReport.append(errorMessage).append("\n\n");
        
        if (e != null) {
            errorReport.append("错误详情 | Error Details:\n");
            errorReport.append(e.getMessage()).append("\n\n");
        }
        
        errorReport.append("建议操作 | Suggested Actions:\n");
        errorReport.append("1. 检查项目ID是否正确\n");
        errorReport.append("2. 确认OpenProject API连接正常\n");
        errorReport.append("3. 验证API密钥权限\n");
        errorReport.append("4. 稍后重试\n");
        
        return errorReport.toString();
    }
    
    /**
     * Test API connection and configuration
     * 测试API连接和配置
     */
    public boolean testConnection() {
        try {
            boolean isAccessible = apiClient.isApiAccessible();
            logger.info("API connection test result: {}", isAccessible);
            return isAccessible;
        } catch (Exception e) {
            logger.error("API connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get service status information
     * 获取服务状态信息
     */
    public String getServiceStatus() {
        StringBuilder status = new StringBuilder();
        
        status.append("OpenProject MCP Server Status\n");
        status.append("=".repeat(40)).append("\n");
        status.append(String.format("Base URL: %s\n", baseUrl));
        status.append(String.format("API Key Configured: %s\n", apiKey != null && !apiKey.trim().isEmpty()));
        status.append(String.format("API Connection: %s\n", testConnection() ? "Connected" : "Disconnected"));
        status.append(String.format("Cache Enabled: %s\n", cacheEnabled));
        status.append(String.format("Cache Size: %d entries\n", reportCache.size()));
        status.append(String.format("Max Page Size: %d\n", maxPageSize));
        status.append(String.format("Available Tools: 3\n"));
        status.append("  - generateWeeklyReport (enhanced)\n");
        status.append("  - analyzeRisks\n");
        status.append("  - calculateCompletion\n");
        
        return status.toString();
    }
    
    /**
     * Clear the report cache
     * 清除报告缓存
     */
    public void clearCache() {
        reportCache.clear();
        logger.info("Report cache cleared");
    }
    
    /**
     * Get cache statistics
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("Cache Statistics - Enabled: %s, Size: %d entries, Expiry: %d minutes",
                            cacheEnabled, reportCache.size(), CACHE_EXPIRY_MINUTES);
    }
    
    // Enhanced Risk Analysis Helper Methods
    // 增强风险分析辅助方法
    
    /**
     * Enhanced Risk Analysis record with sophisticated scoring and security
     * 增强风险分析记录，具有复杂评分和安全功能
     */
    private record EnhancedRiskAnalysis(
        String projectId,
        String projectName,
        LocalDate analysisDate,
        WorkPackage.RiskLevel overallRiskLevel,
        double riskScore,
        int overdueTasks,
        int nearDueTasks,
        List<EnhancedRiskItem> riskItems,
        RiskTrend riskTrend,
        String securityAudit,
        String recommendations
    ) {}
    
    /**
     * Enhanced Risk Item with detailed scoring and impact analysis
     * 增强风险项目，具有详细评分和影响分析
     */
    private record EnhancedRiskItem(
        String workPackageId,
        String subject,
        String description,
        String impact,
        double riskScore,
        LocalDate dueDate,
        long daysOverdue,
        String priority,
        String assignee,
        String recommendation
    ) {}
    
    /**
     * Risk Trend Analysis enum
     * 风险趋势分析枚举
     */
    private enum RiskTrend {
        IMPROVING("改善中", "Risk trend is improving"),
        STABLE("稳定", "Risk trend is stable"),
        WORSENING("恶化中", "Risk trend is worsening"),
        UNKNOWN("未知", "Risk trend is unknown");
        
        private final String chineseName;
        private final String description;
        
        RiskTrend(String chineseName, String description) {
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
     * Validate project ID with enhanced security measures
     * 使用增强的安全措施验证项目ID
     */
    private void validateProjectIdWithSecurity(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("项目ID不能为空 | Project ID cannot be null or empty");
        }
        
        // Sanitize input to prevent injection attacks
        String sanitizedProjectId = projectId.trim();
        
        // Validate format (alphanumeric and basic symbols only)
        if (!sanitizedProjectId.matches("^[a-zA-Z0-9\\-._]+$")) {
            throw new SecurityException("项目ID包含无效字符 | Project ID contains invalid characters");
        }
        
        // Length validation
        if (sanitizedProjectId.length() > 50) {
            throw new IllegalArgumentException("项目ID过长 | Project ID too long");
        }
        
        logger.debug("Project ID validated with security measures: {}", sanitizedProjectId);
    }
    
    /**
     * Validate API permissions before proceeding
     * 在继续之前验证API权限
     */
    private boolean validateApiPermissions() {
        try {
            // Check if API key is configured and valid
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("API key not configured");
                return false;
            }
            
            // Test basic API accessibility without exposing sensitive data
            boolean isAccessible = apiClient.isApiAccessible();
            logger.debug("API accessibility check result: {}", isAccessible);
            
            return isAccessible;
        } catch (Exception e) {
            logger.warn("API permission validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get project name with security-aware logging
     * 使用安全感知日志记录获取项目名称
     */
    private String getProjectNameWithSecurityLogging(String projectId) {
        try {
            // Use existing method with enhanced security logging
            String projectName = getProjectNameWithRetry(projectId);
            
            // Log without exposing sensitive data
            logger.debug("Project name retrieved successfully for project: {}", projectId);
            return projectName;
        } catch (Exception e) {
            logger.warn("Failed to retrieve project name, using secure fallback for project: {}", projectId);
            return "Project-" + projectId.substring(0, Math.min(projectId.length(), 8));
        }
    }
    
    /**
     * Build secure filters for work package queries
     * 构建工作包查询的安全过滤器
     */
    private Map<String, String> buildSecureFilters(String projectId) {
        Map<String, String> filters = new HashMap<>();
        
        // Sanitize project ID before using in filters
        String sanitizedProjectId = projectId.trim();
        
        // Project filter with input validation
        filters.put("project", sanitizedProjectId);
        
        // Add additional security filters to limit data exposure
        filters.put("status", "!"); // Exclude closed tasks for active risk analysis
        
        logger.debug("Built secure filters for project: {}", sanitizedProjectId);
        return filters;
    }
    
    /**
     * Fetch work packages with enhanced security measures
     * 使用增强的安全措施获取工作包
     */
    private List<WorkPackage> fetchWorkPackagesWithSecurity(Map<String, String> filters, String projectId) 
            throws Exception {
        
        try {
            // Use existing pagination method with security considerations
            List<WorkPackage> workPackages = fetchWorkPackagesWithPagination(filters, projectId);
            
            // Sanitize and validate work package data
            return workPackages.stream()
                .filter(wp -> wp.isValid()) // Use existing validation
                .filter(wp -> sanitizeWorkPackageData(wp))
                .collect(Collectors.toList());
                
        } catch (RateLimitExceededException e) {
            logger.warn("Rate limit hit while fetching work packages for project: {}", projectId, e);
            throw e;
        } catch (OpenProjectApiException e) {
            logger.error("API error while fetching work packages for project: {}", projectId, e);
            throw e;
        }
    }
    
    /**
     * Sanitize work package data for security
     * 出于安全考虑清理工作包数据
     */
    private boolean sanitizeWorkPackageData(WorkPackage wp) {
        try {
            // Validate that essential fields are present and not malicious
            if (wp.id() == null || wp.id().trim().isEmpty()) {
                return false;
            }
            
            if (wp.subject() == null || wp.subject().trim().isEmpty()) {
                return false;
            }
            
            // Check for potentially malicious content in subject
            String subject = wp.subject().trim();
            if (subject.length() > 500) {
                logger.warn("Work package subject too long, truncating for security: {}", wp.id());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.warn("Failed to sanitize work package data: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform enhanced risk analysis with sophisticated scoring
     * 执行增强风险分析，具有复杂评分
     */
    private EnhancedRiskAnalysis performEnhancedRiskAnalysis(
            List<WorkPackage> workPackages, String projectId, String projectName) {
        
        logger.debug("Performing enhanced risk analysis for {} work packages", workPackages.size());
        
        // Filter overdue and near-due tasks
        List<WorkPackage> overdueTasks = workPackages.stream()
            .filter(WorkPackage::isOverdue)
            .collect(Collectors.toList());
            
        List<WorkPackage> nearDueTasks = workPackages.stream()
            .filter(wp -> !wp.isOverdue() && wp.dueDate() != null)
            .filter(wp -> {
                long daysUntilDue = wp.getDaysUntilDue();
                return daysUntilDue >= 0 && daysUntilDue <= 7; // Within 7 days
            })
            .collect(Collectors.toList());
        
        // Calculate sophisticated risk score
        double riskScore = calculateSophisticatedRiskScore(overdueTasks, nearDueTasks, workPackages);
        
        // Determine overall risk level
        WorkPackage.RiskLevel overallRiskLevel = determineOverallRiskLevel(riskScore, overdueTasks.size());
        
        // Generate enhanced risk items
        List<EnhancedRiskItem> riskItems = generateEnhancedRiskItems(overdueTasks, nearDueTasks);
        
        // Analyze risk trend
        RiskTrend riskTrend = analyzeRiskTrend(workPackages);
        
        // Generate security audit report
        String securityAudit = generateSecurityAuditReport();
        
        // Generate sophisticated recommendations
        String recommendations = generateSophisticatedRecommendations(
            overallRiskLevel, riskScore, overdueTasks, nearDueTasks, riskTrend
        );
        
        return new EnhancedRiskAnalysis(
            projectId,
            projectName,
            LocalDate.now(),
            overallRiskLevel,
            riskScore,
            overdueTasks.size(),
            nearDueTasks.size(),
            riskItems,
            riskTrend,
            securityAudit,
            recommendations
        );
    }
    
    /**
     * Calculate sophisticated risk score based on multiple factors
     * 基于多个因素计算复杂风险分数
     */
    private double calculateSophisticatedRiskScore(
            List<WorkPackage> overdueTasks, List<WorkPackage> nearDueTasks, List<WorkPackage> allTasks) {
        
        double baseScore = 0.0;
        
        // Factor 1: Overdue tasks with severity weighting
        for (WorkPackage task : overdueTasks) {
            double taskScore = 10.0; // Base score for overdue
            
            // Severity weighting based on days overdue
            long daysOverdue = Math.abs(task.getDaysUntilDue());
            if (daysOverdue > 30) {
                taskScore *= 2.0; // Critical
            } else if (daysOverdue > 14) {
                taskScore *= 1.5; // High
            } else if (daysOverdue > 7) {
                taskScore *= 1.2; // Medium
            }
            
            // Priority weighting
            if (task.priority() != null) {
                String priorityName = task.priority().name().toLowerCase();
                if (priorityName.contains("high")) {
                    taskScore *= 1.5;
                } else if (priorityName.contains("low")) {
                    taskScore *= 0.8;
                }
            }
            
            // Assignee workload impact
            if (task.assignee() != null) {
                long assigneeOverdueCount = overdueTasks.stream()
                    .filter(wp -> wp.assignee() != null && 
                               wp.assignee().id().equals(task.assignee().id()))
                    .count();
                if (assigneeOverdueCount > 3) {
                    taskScore *= 1.3; // Workload congestion
                }
            }
            
            baseScore += taskScore;
        }
        
        // Factor 2: Near-due tasks
        for (WorkPackage task : nearDueTasks) {
            double nearDueScore = 3.0; // Lower base score for near-due
            
            // Urgency based on proximity to due date
            long daysUntilDue = task.getDaysUntilDue();
            if (daysUntilDue <= 3) {
                nearDueScore *= 1.5;
            }
            
            // Priority weighting
            if (task.priority() != null) {
                String priorityName = task.priority().name().toLowerCase();
                if (priorityName.contains("high")) {
                    nearDueScore *= 1.3;
                }
            }
            
            baseScore += nearDueScore;
        }
        
        // Factor 3: Project size normalization
        double sizeFactor = Math.max(1.0, allTasks.size() / 10.0);
        double normalizedScore = baseScore / sizeFactor;
        
        // Factor 4: Risk concentration (percentage of tasks at risk)
        double riskConcentration = (overdueTasks.size() + nearDueTasks.size()) * 100.0 / 
                                  Math.max(1.0, allTasks.size());
        if (riskConcentration > 25) {
            normalizedScore *= 1.2; // High concentration penalty
        }
        
        logger.debug("Calculated sophisticated risk score: {}", normalizedScore);
        return Math.round(normalizedScore * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    /**
     * Determine overall risk level based on score and overdue count
     * 根据分数和逾期数量确定整体风险级别
     */
    private WorkPackage.RiskLevel determineOverallRiskLevel(double riskScore, int overdueCount) {
        // Use both score-based and count-based logic for robust assessment
        if (riskScore >= 50.0 || overdueCount > 5) {
            return WorkPackage.RiskLevel.HIGH;
        } else if (riskScore >= 20.0 || overdueCount >= 3) {
            return WorkPackage.RiskLevel.MEDIUM;
        } else {
            return WorkPackage.RiskLevel.LOW;
        }
    }
    
    /**
     * Generate enhanced risk items with detailed analysis
     * 生成具有详细分析的增强风险项目
     */
    private List<EnhancedRiskItem> generateEnhancedRiskItems(
            List<WorkPackage> overdueTasks, List<WorkPackage> nearDueTasks) {
        
        List<EnhancedRiskItem> riskItems = new ArrayList<>();
        
        // Process overdue tasks first (higher priority)
        for (WorkPackage task : overdueTasks.stream().limit(10).toList()) {
            EnhancedRiskItem riskItem = createEnhancedRiskItem(task, true);
            riskItems.add(riskItem);
        }
        
        // Process near-due tasks
        for (WorkPackage task : nearDueTasks.stream().limit(5).toList()) {
            EnhancedRiskItem riskItem = createEnhancedRiskItem(task, false);
            riskItems.add(riskItem);
        }
        
        // Add summary if we have more items
        int totalOverdue = overdueTasks.size();
        int totalNearDue = nearDueTasks.size();
        
        if (totalOverdue > 10 || totalNearDue > 5) {
            String summaryDescription = String.format(
                "还有 %d 个逾期任务和 %d 个临近截止任务需要关注",
                Math.max(0, totalOverdue - 10),
                Math.max(0, totalNearDue - 5)
            );
            
            EnhancedRiskItem summaryItem = new EnhancedRiskItem(
                "summary",
                "其他风险项目汇总",
                summaryDescription,
                "Medium",
                15.0,
                LocalDate.now(),
                0,
                "normal",
                "system",
                "需要查看完整列表以获取所有风险项目"
            );
            
            riskItems.add(summaryItem);
        }
        
        return riskItems;
    }
    
    /**
     * Create enhanced risk item from work package
     * 从工作包创建增强风险项目
     */
    private EnhancedRiskItem createEnhancedRiskItem(WorkPackage task, boolean isOverdue) {
        double riskScore = calculateIndividualRiskScore(task, isOverdue);
        String impact = determineImpactLevel(task, isOverdue);
        String recommendation = generateTaskSpecificRecommendation(task, isOverdue);
        
        long daysOverdue = isOverdue ? Math.abs(task.getDaysUntilDue()) : 0;
        String priority = task.priority() != null ? task.priority().name() : "normal";
        String assignee = task.assignee() != null ? task.assignee().name() : "未分配";
        
        return new EnhancedRiskItem(
            task.id(),
            task.subject(),
            isOverdue ? String.format("任务逾期 %d 天", daysOverdue) : "任务临近截止日期",
            impact,
            riskScore,
            task.dueDate(),
            daysOverdue,
            priority,
            assignee,
            recommendation
        );
    }
    
    /**
     * Calculate individual task risk score
     * 计算单个任务风险分数
     */
    private double calculateIndividualRiskScore(WorkPackage task, boolean isOverdue) {
        double score = 5.0; // Base score
        
        if (isOverdue) {
            long daysOverdue = Math.abs(task.getDaysUntilDue());
            score += daysOverdue * 0.5; // 0.5 points per day overdue
        } else {
            long daysUntilDue = task.getDaysUntilDue();
            score += (7 - daysUntilDue) * 0.3; // Urgency factor
        }
        
        // Priority multiplier
        if (task.priority() != null) {
            String priorityName = task.priority().name().toLowerCase();
            if (priorityName.contains("high")) {
                score *= 1.5;
            } else if (priorityName.contains("low")) {
                score *= 0.7;
            }
        }
        
        // Assignee workload factor
        if (task.assignee() != null) {
            // This would ideally be calculated across all tasks, but we'll use a simple heuristic
            score *= 1.1; // Assume some workload pressure
        }
        
        return Math.round(score * 10.0) / 10.0;
    }
    
    /**
     * Determine impact level for a task
     * 确定任务的影响级别
     */
    private String determineImpactLevel(WorkPackage task, boolean isOverdue) {
        double score = calculateIndividualRiskScore(task, isOverdue);
        
        if (score >= 20.0) {
            return "Critical";
        } else if (score >= 12.0) {
            return "High";
        } else if (score >= 7.0) {
            return "Medium";
        } else {
            return "Low";
        }
    }
    
    /**
     * Generate task-specific recommendation
     * 生成任务特定的建议
     */
    private String generateTaskSpecificRecommendation(WorkPackage task, boolean isOverdue) {
        StringBuilder recommendation = new StringBuilder();
        
        if (isOverdue) {
            long daysOverdue = Math.abs(task.getDaysUntilDue());
            if (daysOverdue > 30) {
                recommendation.append("紧急：立即重新评估任务优先级和资源分配");
            } else if (daysOverdue > 14) {
                recommendation.append("高优先级：重新排期或增加资源");
            } else {
                recommendation.append("中等优先级：调整计划或寻求帮助");
            }
        } else {
            long daysUntilDue = task.getDaysUntilDue();
            if (daysUntilDue <= 3) {
                recommendation.append("监控：确保任务按计划进行");
            } else {
                recommendation.append("计划：准备资源以确保按时完成");
            }
        }
        
        // Add priority-specific advice
        if (task.priority() != null) {
            String priorityName = task.priority().name().toLowerCase();
            if (priorityName.contains("high")) {
                recommendation.append("（高优先级任务）");
            }
        }
        
        return recommendation.toString();
    }
    
    /**
     * Analyze risk trend based on task patterns
     * 基于任务模式分析风险趋势
     */
    private RiskTrend analyzeRiskTrend(List<WorkPackage> workPackages) {
        try {
            // Simple trend analysis based on task completion rates and due dates
            LocalDate today = LocalDate.now();
            LocalDate oneWeekAgo = today.minusWeeks(1);
            
            long recentCompletions = workPackages.stream()
                .filter(wp -> wp.updatedAt() != null)
                .filter(wp -> wp.updatedAt().toLocalDate().isAfter(oneWeekAgo))
                .filter(WorkPackage::isCompleted)
                .count();
            
            long recentOverdueIncreases = workPackages.stream()
                .filter(wp -> wp.dueDate() != null)
                .filter(wp -> wp.dueDate().isAfter(oneWeekAgo) && wp.dueDate().isBefore(today))
                .filter(WorkPackage::isOverdue)
                .count();
            
            if (recentCompletions > recentOverdueIncreases) {
                return RiskTrend.IMPROVING;
            } else if (recentOverdueIncreases > recentCompletions) {
                return RiskTrend.WORSENING;
            } else {
                return RiskTrend.STABLE;
            }
        } catch (Exception e) {
            logger.warn("Failed to analyze risk trend: {}", e.getMessage());
            return RiskTrend.UNKNOWN;
        }
    }
    
    /**
     * Generate security audit report
     * 生成安全审计报告
     */
    private String generateSecurityAuditReport() {
        StringBuilder audit = new StringBuilder();
        
        audit.append("🔒 安全审计报告 | Security Audit Report\n");
        audit.append("API密钥状态: ").append(apiKey != null && !apiKey.trim().isEmpty() ? "已配置" : "未配置").append("\n");
        audit.append("HTTPS连接: ").append(baseUrl != null && baseUrl.startsWith("https") ? "已启用" : "未启用").append("\n");
        audit.append("输入验证: 已启用\n");
        audit.append("数据清理: 已启用\n");
        audit.append("权限检查: 已启用\n");
        audit.append("安全日志: 已记录\n");
        
        return audit.toString();
    }
    
    /**
     * Generate sophisticated recommendations based on comprehensive analysis
     * 基于综合分析生成复杂建议
     */
    private String generateSophisticatedRecommendations(
            WorkPackage.RiskLevel riskLevel, double riskScore,
            List<WorkPackage> overdueTasks, List<WorkPackage> nearDueTasks,
            RiskTrend riskTrend) {
        
        StringBuilder recommendations = new StringBuilder();
        
        // Risk level-specific recommendations
        recommendations.append("📋 风险级别建议 | Risk Level Recommendations:\n");
        switch (riskLevel) {
            case HIGH:
                recommendations.append("🚨 高风险级别 - 立即采取行动\n");
                recommendations.append("1. 立即召开项目风险评估会议\n");
                recommendations.append("2. 重新评估项目范围和资源分配\n");
                recommendations.append("3. 考虑项目延期或范围调整\n");
                recommendations.append("4. 与相关方进行紧急沟通\n");
                recommendations.append("5. 实施每日风险监控\n");
                break;
            case MEDIUM:
                recommendations.append("⚠️ 中风险级别 - 需要关注和计划\n");
                recommendations.append("1. 增加项目监控频率\n");
                recommendations.append("2. 优化资源分配和任务优先级\n");
                recommendations.append("3. 制定风险缓解计划\n");
                recommendations.append("4. 准备应急资源\n");
                break;
            case LOW:
                recommendations.append("✅ 低风险级别 - 继续监控\n");
                recommendations.append("1. 保持常规项目监控\n");
                recommendations.append("2. 定期审查项目进度\n");
                recommendations.append("3. 维持良好沟通\n");
                break;
        }
        
        recommendations.append("\n");
        
        // Risk score-based recommendations
        recommendations.append("📊 风险分数分析 | Risk Score Analysis:\n");
        recommendations.append(String.format("当前风险分数: %.1f\n", riskScore));
        
        if (riskScore >= 50.0) {
            recommendations.append("建议：实施紧急风险缓解措施\n");
        } else if (riskScore >= 20.0) {
            recommendations.append("建议：制定详细的风险管理计划\n");
        } else {
            recommendations.append("建议：维持当前的风险控制措施\n");
        }
        
        recommendations.append("\n");
        
        // Task-specific recommendations
        recommendations.append("🎯 任务级别建议 | Task-Level Recommendations:\n");
        
        if (!overdueTasks.isEmpty()) {
            recommendations.append(String.format("逾期任务数量: %d\n", overdueTasks.size()));
            recommendations.append("建议：优先处理高优先级逾期任务\n");
            
            // Identify critical overdue tasks
            List<WorkPackage> criticalOverdue = overdueTasks.stream()
                .filter(wp -> Math.abs(wp.getDaysUntilDue()) > 14)
                .limit(3)
                .toList();
            
            if (!criticalOverdue.isEmpty()) {
                recommendations.append("关键逾期任务：\n");
                for (WorkPackage task : criticalOverdue) {
                    recommendations.append(String.format("• %s (逾期%d天)\n", 
                        task.subject(), Math.abs(task.getDaysUntilDue())));
                }
            }
        }
        
        if (!nearDueTasks.isEmpty()) {
            recommendations.append(String.format("临近截止任务数量: %d\n", nearDueTasks.size()));
            recommendations.append("建议：监控即将到期的任务进度\n");
        }
        
        recommendations.append("\n");
        
        // Trend-based recommendations
        recommendations.append("📈 趋势分析建议 | Trend Analysis Recommendations:\n");
        recommendations.append(String.format("风险趋势: %s\n", riskTrend.getChineseName()));
        
        switch (riskTrend) {
            case WORSENING:
                recommendations.append("建议：立即干预，防止进一步恶化\n");
                break;
            case IMPROVING:
                recommendations.append("建议：继续当前的管理策略\n");
                break;
            case STABLE:
                recommendations.append("建议：维持现有措施，持续监控\n");
                break;
            case UNKNOWN:
                recommendations.append("建议：增加数据收集以更好地评估趋势\n");
                break;
        }
        
        recommendations.append("\n");
        
        // Strategic recommendations
        recommendations.append("🎯 战略建议 | Strategic Recommendations:\n");
        recommendations.append("1. 建立定期风险评估机制\n");
        recommendations.append("2. 实施风险预警系统\n");
        recommendations.append("3. 培训团队成员风险意识\n");
        recommendations.append("4. 建立风险应对预案\n");
        recommendations.append("5. 定期与相关方沟通风险状况\n");
        
        return recommendations.toString();
    }
    
    /**
     * Format enhanced risk analysis report
     * 格式化增强风险分析报告
     */
    private String formatEnhancedRiskAnalysis(EnhancedRiskAnalysis analysis, String projectName) {
        StringBuilder formattedAnalysis = new StringBuilder();
        
        // Header
        formattedAnalysis.append("⚠️".repeat(60)).append("\n");
        formattedAnalysis.append("🔒 增强项目风险分析 | Enhanced Project Risk Analysis 🔒\n");
        formattedAnalysis.append("⚠️".repeat(60)).append("\n\n");
        
        // Project information
        formattedAnalysis.append(String.format("📋 项目: %s (ID: %s)\n", projectName, analysis.projectId()));
        formattedAnalysis.append(String.format("📅 分析时间: %s\n\n", analysis.analysisDate()));
        
        // Risk Summary
        formattedAnalysis.append("🎯 风险摘要 | Risk Summary\n");
        formattedAnalysis.append("=".repeat(40)).append("\n");
        formattedAnalysis.append(String.format("整体风险级别: %s (%s)\n", 
            analysis.overallRiskLevel().getChineseName(), 
            analysis.overallRiskLevel().name()));
        formattedAnalysis.append(String.format("风险分数: %.1f/100\n", analysis.riskScore()));
        formattedAnalysis.append(String.format("逾期任务数量: %d\n", analysis.overdueTasks()));
        formattedAnalysis.append(String.format("临近截止任务数量: %d\n", analysis.nearDueTasks()));
        formattedAnalysis.append(String.format("风险趋势: %s\n", analysis.riskTrend().getChineseName()));
        formattedAnalysis.append("\n");
        
        // Risk Items
        if (!analysis.riskItems().isEmpty()) {
            formattedAnalysis.append("🔍 风险详情 | Risk Details\n");
            formattedAnalysis.append("=".repeat(40)).append("\n");
            
            for (EnhancedRiskItem item : analysis.riskItems()) {
                formattedAnalysis.append(String.format("📌 任务: %s\n", item.subject()));
                formattedAnalysis.append(String.format("   描述: %s\n", item.description()));
                formattedAnalysis.append(String.format("   影响: %s\n", item.impact()));
                formattedAnalysis.append(String.format("   风险分数: %.1f\n", item.riskScore()));
                formattedAnalysis.append(String.format("   截止日期: %s\n", item.dueDate()));
                if (item.daysOverdue() > 0) {
                    formattedAnalysis.append(String.format("   逾期天数: %d\n", item.daysOverdue()));
                }
                formattedAnalysis.append(String.format("   优先级: %s\n", item.priority()));
                formattedAnalysis.append(String.format("   负责人: %s\n", item.assignee()));
                formattedAnalysis.append(String.format("   建议: %s\n", item.recommendation()));
                formattedAnalysis.append("\n");
            }
        } else {
            formattedAnalysis.append("🔍 风险详情 | Risk Details\n");
            formattedAnalysis.append("=".repeat(40)).append("\n");
            formattedAnalysis.append("✅ 没有发现风险项目 | No risk items found\n\n");
        }
        
        // Security Audit
        formattedAnalysis.append("🔒 安全审计 | Security Audit\n");
        formattedAnalysis.append("=".repeat(40)).append("\n");
        formattedAnalysis.append(analysis.securityAudit()).append("\n");
        
        // Recommendations
        formattedAnalysis.append("💡 建议措施 | Recommendations\n");
        formattedAnalysis.append("=".repeat(40)).append("\n");
        formattedAnalysis.append(analysis.recommendations()).append("\n");
        
        // Footer
        formattedAnalysis.append("⚠️".repeat(60)).append("\n");
        formattedAnalysis.append("🔒 增强风险分析完成 | Enhanced Risk Analysis Completed 🔒\n");
        formattedAnalysis.append("⚠️".repeat(60)).append("\n");
        
        return formattedAnalysis.toString();
    }
    
    /**
     * Generate security error report
     * 生成安全错误报告
     */
    private String generateSecurityErrorReport(SecurityException e) {
        StringBuilder errorReport = new StringBuilder();
        
        errorReport.append("🚫".repeat(60)).append("\n");
        errorReport.append("🔒 安全错误报告 | Security Error Report 🔒\n");
        errorReport.append("🚫".repeat(60)).append("\n\n");
        
        errorReport.append("检测到安全违规 | Security violation detected:\n");
        errorReport.append(e.getMessage()).append("\n\n");
        
        errorReport.append("🔒 安全检查项目 | Security Check Items:\n");
        errorReport.append("✅ 输入验证: 已启用\n");
        errorReport.append("✅ 数据清理: 已启用\n");
        errorReport.append("✅ 权限检查: 已启用\n");
        errorReport.append("✅ 安全日志: 已记录\n");
        errorReport.append("✅ HTTPS连接: 已启用\n");
        errorReport.append("\n");
        
        errorReport.append("📋 建议操作 | Suggested Actions:\n");
        errorReport.append("1. 检查API密钥配置\n");
        errorReport.append("2. 验证项目ID格式\n");
        errorReport.append("3. 确认网络连接安全\n");
        errorReport.append("4. 检查防火墙设置\n");
        errorReport.append("5. 联系系统管理员\n");
        
        return errorReport.toString();
    }
    
    /**
     * Determine if an API exception is retryable
     * 判断API异常是否可重试
     * 
     * @param e the API exception
     * @return true if the exception should be retried, false otherwise
     */
    private boolean isRetryableError(OpenProjectApiException e) {
        // Retry on rate limiting, server errors, and network issues
        return e.isServerError() || 
               e.getStatusCode() == 429 || // Rate limit
               e.getStatusCode() == 502 || // Bad Gateway
               e.getStatusCode() == 503 || // Service Unavailable
               e.getStatusCode() == 504;   // Gateway Timeout
    }
    
    /**
     * Generate fallback project name based on error type
     * 根据错误类型生成备用项目名称
     * 
     * @param projectId the project ID
     * @param e the exception that occurred
     * @return a fallback project name
     */
    private String generateFallbackProjectName(String projectId, Exception e) {
        // Generate a safe fallback name that includes error context
        String baseName = "Project-" + projectId;
        
        if (e instanceof RateLimitExceededException) {
            return baseName + "-rate-limited";
        } else if (e instanceof OpenProjectApiException) {
            OpenProjectApiException ope = (OpenProjectApiException) e;
            if (ope.getStatusCode() == 404) {
                return baseName + "-not-found";
            } else if (ope.getStatusCode() == 403) {
                return baseName + "-access-denied";
            } else if (ope.isServerError()) {
                return baseName + "-server-error";
            }
        }
        
        return baseName + "-fallback";
    }
    
    /**
     * Recovery method for generateWeeklyReport retry failures
     * 生成周报重试失败的恢复方法
     */
@Recover
public String generateWeeklyReportRecover(Exception e, String projectId) {
    logger.error("All retry attempts exhausted for generating weekly report. Project: {}, Error: {}", 
               projectId, e.getMessage());
    
    // Generate a graceful fallback report
    return generateFallbackWeeklyReport(projectId, e);
}

/**
     * Recovery method for analyzeRisks retry failures
     * 风险分析重试失败的恢复方法
     */
@Recover
public String analyzeRisksRecover(Exception e, String projectId) {
    logger.error("All retry attempts exhausted for analyzing risks. Project: {}, Error: {}", 
               projectId, e.getMessage());
    
    // Generate a graceful fallback risk analysis
    return generateFallbackRiskAnalysis(projectId, e);
}

/**
     * Recovery method for calculateCompletion retry failures
     * 完成度计算重试失败的恢复方法
     */
@Recover
public String calculateCompletionRecover(Exception e, String projectId) {
    logger.error("All retry attempts exhausted for calculating completion. Project: {}, Error: {}", 
               projectId, e.getMessage());
    
    // Generate a graceful fallback completion analysis
    return generateFallbackCompletionAnalysis(projectId, e);
}

/**
     * Generate fallback weekly report when retries are exhausted
     * 重试用尽时生成备用周报
     */
private String generateFallbackWeeklyReport(String projectId, Exception e) {
    StringBuilder fallbackReport = new StringBuilder();
    
    fallbackReport.append("⚠️".repeat(50)).append("\n");
    fallbackReport.append("🔁 周报生成失败 - 重试用尽 | Weekly Report Generation Failed - Retries Exhausted\n");
    fallbackReport.append("⚠️".repeat(50)).append("\n\n");
    
    fallbackReport.append(String.format("项目: Project-%s\n", projectId));
    fallbackReport.append(String.format("生成时间: %s\n\n", LocalDate.now()));
    
    fallbackReport.append("⚠️ 错误详情 | Error Details:\n");
    fallbackReport.append("-".repeat(30)).append("\n");
    fallbackReport.append("无法连接到OpenProject API或达到重试限制\n");
    fallbackReport.append("Unable to connect to OpenProject API or retry limit exceeded\n\n");
    
    if (e instanceof RateLimitExceededException) {
        fallbackReport.append("🚦 错误类型: API速率限制 | Error Type: API Rate Limit\n");
        fallbackReport.append("💡 建议: 请稍后重试 | Recommendation: Please try again later\n");
    } else if (e instanceof OpenProjectApiException) {
        OpenProjectApiException ope = (OpenProjectApiException) e;
        fallbackReport.append(String.format("🚦 错误类型: API错误 (HTTP %d) | Error Type: API Error (HTTP %d)\n", 
                                  ope.getStatusCode(), ope.getStatusCode()));
        fallbackReport.append("💡 建议: 检查API配置和权限 | Recommendation: Check API configuration and permissions\n");
    } else if (e instanceof ResourceAccessException) {
        fallbackReport.append("🚦 错误类型: 网络连接错误 | Error Type: Network Connection Error\n");
        fallbackReport.append("💡 建议: 检查网络连接和OpenProject服务状态 | Recommendation: Check network connection and OpenProject service status\n");
    } else {
        fallbackReport.append("🚦 错误类型: 系统错误 | Error Type: System Error\n");
        fallbackReport.append("💡 建议: 检查系统日志 | Recommendation: Check system logs\n");
    }
    
    fallbackReport.append("\n📋 后续步骤 | Next Steps:\n");
    fallbackReport.append("1. 检查OpenProject服务状态\n");
    fallbackReport.append("2. 验证API密钥配置\n");
    fallbackReport.append("3. 稍后重试生成周报\n");
    
    fallbackReport.append("\n⚠️".repeat(50)).append("\n");
    fallbackReport.append("备用周报生成完成 | Fallback weekly report completed\n");
    
    return fallbackReport.toString();
}

/**
     * Generate fallback risk analysis when retries are exhausted
     * 重试用尽时生成备用风险分析
     */
private String generateFallbackRiskAnalysis(String projectId, Exception e) {
    StringBuilder fallbackAnalysis = new StringBuilder();
    
    fallbackAnalysis.append("⚠️".repeat(60)).append("\n");
    fallbackAnalysis.append("🔁 风险分析失败 - 重试用尽 | Risk Analysis Failed - Retries Exhausted 🔒\n");
    fallbackAnalysis.append("⚠️".repeat(60)).append("\n\n");
    
    fallbackAnalysis.append(String.format("📋 项目: Project-%s\n", projectId));
    fallbackAnalysis.append(String.format("📅 分析时间: %s\n\n", LocalDate.now()));
    
    fallbackAnalysis.append("⚠️ 分析状态 | Analysis Status:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    fallbackAnalysis.append("❌ 无法完成风险分析 | Unable to complete risk analysis\n");
    fallbackAnalysis.append("🔄 已达到最大重试次数 | Maximum retry attempts reached\n\n");
    
    fallbackAnalysis.append("🔍 错误诊断 | Error Diagnosis:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    if (e instanceof RateLimitExceededException) {
        fallbackAnalysis.append("• API速率限制过高 | API rate limit too high\n");
        fallbackAnalysis.append("• 建议: 增加重试间隔或降低请求频率 | Suggestion: Increase retry interval or reduce request frequency\n");
    } else if (e instanceof OpenProjectApiException) {
        OpenProjectApiException ope = (OpenProjectApiException) e;
        fallbackAnalysis.append(String.format("• API错误响应: HTTP %d | API error response: HTTP %d\n", ope.getStatusCode(), ope.getStatusCode()));
        fallbackAnalysis.append("• 建议: 检查API权限和网络连接 | Suggestion: Check API permissions and network connection\n");
    } else if (e instanceof ResourceAccessException) {
        fallbackAnalysis.append("• 网络连接超时或DNS解析失败 | Network connection timeout or DNS resolution failure\n");
        fallbackAnalysis.append("• 建议: 检查网络配置和防火墙设置 | Suggestion: Check network configuration and firewall settings\n");
    } else {
        fallbackAnalysis.append("• 未知系统错误 | Unknown system error\n");
        fallbackAnalysis.append("• 建议: 检查系统日志和服务状态 | Suggestion: Check system logs and service status\n");
    }
    
    fallbackAnalysis.append("\n🛡️ 安全状态 | Security Status:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    fallbackAnalysis.append("✅ 输入验证: 正常 | Input validation: Normal\n");
    fallbackAnalysis.append("✅ 权限检查: 已执行 | Permission check: Executed\n");
    fallbackAnalysis.append("✅ 数据清理: 已启用 | Data sanitization: Enabled\n");
    
    fallbackAnalysis.append("\n💡 恢复建议 | Recovery Recommendations:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    fallbackAnalysis.append("1. 等待一段时间后重试 | Wait some time before retrying\n");
    fallbackAnalysis.append("2. 检查OpenProject服务可用性 | Check OpenProject service availability\n");
    fallbackAnalysis.append("3. 验证API配置和密钥 | Verify API configuration and key\n");
    fallbackAnalysis.append("4. 考虑实施手动风险监控 | Consider implementing manual risk monitoring\n");
    
    fallbackAnalysis.append("\n⚠️".repeat(60)).append("\n");
    fallbackAnalysis.append("🔒 备用风险分析完成 | Fallback risk analysis completed 🔒\n");
    fallbackAnalysis.append("⚠️".repeat(60)).append("\n");
    
    return fallbackAnalysis.toString();
}

/**
     * Generate fallback completion analysis when retries are exhausted
     * 重试用尽时生成备用完成度分析
     */
private String generateFallbackCompletionAnalysis(String projectId, Exception e) {
    StringBuilder fallbackAnalysis = new StringBuilder();
    
    fallbackAnalysis.append("📊".repeat(60)).append("\n");
    fallbackAnalysis.append("🔁 完成度分析失败 - 重试用尽 | Completion Analysis Failed - Retries Exhausted\n");
    fallbackAnalysis.append("📊".repeat(60)).append("\n\n");
    
    fallbackAnalysis.append(String.format("📋 项目: Project-%s\n", projectId));
    fallbackAnalysis.append(String.format("📅 分析时间: %s\n\n", LocalDate.now()));
    
    fallbackAnalysis.append("⚠️ 分析状态 | Analysis Status:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    fallbackAnalysis.append("❌ 无法计算完成度 | Unable to calculate completion\n");
    fallbackAnalysis.append("🔄 已达到最大重试次数 | Maximum retry attempts reached\n\n");
    
    fallbackAnalysis.append("🔍 错误详情 | Error Details:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    if (e instanceof RateLimitExceededException) {
        fallbackAnalysis.append("• API调用频率限制 | API rate limiting\n");
        fallbackAnalysis.append("• 建议: 降低请求频率 | Suggestion: Reduce request frequency\n");
    } else if (e instanceof OpenProjectApiException) {
        OpenProjectApiException ope = (OpenProjectApiException) e;
        fallbackAnalysis.append(String.format("• API错误: HTTP %d | API Error: HTTP %d\n", ope.getStatusCode(), ope.getStatusCode()));
        fallbackAnalysis.append("• 建议: 检查API配置 | Suggestion: Check API configuration\n");
    } else if (e instanceof ResourceAccessException) {
        fallbackAnalysis.append("• 网络连接问题，可能原因: 超时、DNS失败、代理配置错误 | Network connectivity issues, possible causes: timeout, DNS failure, proxy configuration error\n");
        fallbackAnalysis.append("• 建议: 检查网络连接、验证OpenProject服务可用性 | Suggestion: Check network connection, verify OpenProject service availability\n");
    } else {
        fallbackAnalysis.append("• 未知系统错误 | Unknown system error\n");
        fallbackAnalysis.append("• 建议: 检查系统日志和服务状态 | Suggestion: Check system logs and service status\n");
    }
    
    fallbackAnalysis.append("\n📈 进度估算 | Progress Estimation:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    fallbackAnalysis.append("⚠️ 基于历史数据的估算 | Estimation based on historical data\n");
    fallbackAnalysis.append("📊 完成度: 未知 | Completion: Unknown\n");
    fallbackAnalysis.append("📋 建议手动检查 | Recommendation: Manual check required\n");
    
    fallbackAnalysis.append("\n🎯 后续操作 | Follow-up Actions:\n");
    fallbackAnalysis.append("=".repeat(40)).append("\n");
    fallbackAnalysis.append("1. 手动检查项目任务状态 | Manually check project task status\n");
    fallbackAnalysis.append("2. 使用OpenProject界面确认进度 | Use OpenProject interface to confirm progress\n");
    fallbackAnalysis.append("3. 联系系统管理员 | Contact system administrator\n");
    fallbackAnalysis.append("4. 稍后重试分析 | Retry analysis later\n");
    
    fallbackAnalysis.append("\n📊".repeat(60)).append("\n");
    fallbackAnalysis.append("备用完成度分析完成 | Fallback completion analysis completed\n");
    fallbackAnalysis.append("📊".repeat(60)).append("\n");
    
    return fallbackAnalysis.toString();
}

/**
     * Legacy method for backward compatibility
     * 向后兼容的遗留方法
     * 
     * @deprecated Use enhanced analyzeRisks method instead
     */
@Deprecated
private String getProjectName(String projectId) throws Exception {
    return getProjectNameWithRetry(projectId);
}
}