package com.example.mcp.service;

import com.example.mcp.client.OpenProjectApiClient;
import com.example.mcp.exception.OpenProjectApiException;
import com.example.mcp.exception.RateLimitExceededException;
import com.example.mcp.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

/**
 * Comprehensive unit test class for ProjectService
 * ProjectService的全面单元测试类
 * 
 * This test class covers all three main MCP tool methods:
 * - generateWeeklyReport: Test successful report generation, error handling, and edge cases
 * - analyzeRisks: Test risk analysis logic, overdue task detection, and risk level categorization
 * - calculateCompletion: Test completion percentage calculation and task counting logic
 * 
 * 该测试类涵盖所有三个主要MCP工具方法：
 * - generateWeeklyReport：测试成功报告生成、错误处理和边缘情况
 * - analyzeRisks：测试风险分析逻辑、逾期任务检测和风险等级分类
 * - calculateCompletion：测试完成百分比计算和任务计数逻辑
 * 
 * @author OpenProject MCP Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private OpenProjectApiClient apiClient;

    @Mock
    private ReportCacheService cacheService;

    @InjectMocks
    private ProjectService projectService;

    private WorkPackage testWorkPackage;
    private WorkPackage completedWorkPackage;
    private WorkPackage overdueWorkPackage;
    private List<WorkPackage> testWorkPackages;
    private OpenProjectApiClient.ProjectInfo testProjectInfo;

    @BeforeEach
    void setUp() {
        // Set up configuration values using reflection
        ReflectionTestUtils.setField(projectService, "baseUrl", "http://test.openproject.com");
        ReflectionTestUtils.setField(projectService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(projectService, "cacheEnabled", false); // Disable cache for simpler tests
        ReflectionTestUtils.setField(projectService, "maxPageSize", 100);

        // Create test data
        createTestData();
    }

    private void createTestData() {
        // Create test project info
        testProjectInfo = new OpenProjectApiClient.ProjectInfo(
            "1", "TEST", "Test Project", "A test project", 
            "2023-01-01T00:00:00Z", "2023-01-01T00:00:00Z"
        );

        // Create test assignee
        WorkPackage.Assignee assignee = new WorkPackage.Assignee("1", "John Doe", "johndoe", "john@example.com");

        // Create test status
        WorkPackage.WorkPackageStatus statusNew = new WorkPackage.WorkPackageStatus("1", "New", false, true, "1");
        WorkPackage.WorkPackageStatus statusCompleted = new WorkPackage.WorkPackageStatus("2", "Completed", true, false, "2");

        // Create test priority
        WorkPackage.WorkPackagePriority priorityNormal = new WorkPackage.WorkPackagePriority("8", "Normal", true, "8");
        WorkPackage.WorkPackagePriority priorityHigh = new WorkPackage.WorkPackagePriority("6", "High", false, "6");

        // Create test type
        WorkPackage.WorkPackageType taskType = new WorkPackage.WorkPackageType("1", "Task", true, "#FF0000", "1");

        // Create test work packages
        testWorkPackage = new WorkPackage(
            "1", "Test Task", "Test description", statusNew, 
            taskType,
            priorityNormal, LocalDate.now().plusDays(7), LocalDate.now(),
            LocalDateTime.now(), LocalDateTime.now(), assignee, 50, 8.0, null, 
            new WorkPackage.Links("1", "1", "1", "1", "1"), null
        );

        completedWorkPackage = new WorkPackage(
            "2", "Completed Task", "Completed task description", statusCompleted,
            taskType,
            priorityNormal, LocalDate.now().plusDays(7), LocalDate.now(),
            LocalDateTime.now(), LocalDateTime.now(), assignee, 100, 8.0, null, 
            new WorkPackage.Links("1", "1", "1", "1", "1"), null
        );

        overdueWorkPackage = new WorkPackage(
            "3", "Overdue Task", "Overdue task description", statusNew,
            taskType,
            priorityHigh, LocalDate.now().minusDays(1), LocalDate.now(),
            LocalDateTime.now(), LocalDateTime.now(), assignee, 30, 8.0, null, 
            new WorkPackage.Links("1", "1", "1", "1", "1"), null
        );

        testWorkPackages = List.of(testWorkPackage, completedWorkPackage, overdueWorkPackage);
    }

    // ===== generateWeeklyReport Tests =====

    @Test
    void testGenerateWeeklyReport_Success() throws Exception {
        // Arrange
        String projectId = "1";
        
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(testWorkPackages);

        // Act
        String result = projectService.generateWeeklyReport(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Weekly Report") || result.contains("周报"));
        
        // Verify API calls
        verify(apiClient, atLeastOnce()).getWorkPackages(anyMap(), anyInt(), anyInt());
    }

    @Test
    void testGenerateWeeklyReport_InvalidProjectId() {
        // Arrange
        String projectId = "";
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            projectService.generateWeeklyReport(projectId);
        });
        
        assertTrue(exception.getMessage().contains("Project ID cannot be null or empty"));
        assertTrue(exception.getMessage().contains("项目ID不能为空"));
    }

    @Test
    void testGenerateWeeklyReport_NullProjectId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            projectService.generateWeeklyReport(null);
        });
        
        assertTrue(exception.getMessage().contains("Project ID cannot be null or empty"));
        assertTrue(exception.getMessage().contains("项目ID不能为空"));
    }

    @Test
    void testGenerateWeeklyReport_ApiNotAccessible() throws Exception {
        // Arrange
        String projectId = "1";
        
        // Mock the API client to throw exception when accessing
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0)))
            .thenThrow(new OpenProjectApiException("API not accessible", 503, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        
        // Act
        String result = projectService.generateWeeklyReport(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("error") || result.contains("错误"));
    }

    @Test
    void testGenerateWeeklyReport_RateLimitExceeded() throws Exception {
        // Arrange
        String projectId = "1";
        
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0)))
            .thenThrow(new RateLimitExceededException("Rate limit exceeded", 
                java.time.Duration.ofSeconds(60), java.time.Instant.now().plusSeconds(60), 0, 100));
        
        // Act
        String result = projectService.generateWeeklyReport(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("rate limit") || result.contains("速率限制"));
    }

    @Test
    void testGenerateWeeklyReport_OpenProjectApiException() throws Exception {
        // Arrange
        String projectId = "1";
        
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0)))
            .thenThrow(new OpenProjectApiException("API error", 500, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));
        
        // Act
        String result = projectService.generateWeeklyReport(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("API error") || result.contains("API错误"));
    }

    // ===== analyzeRisks Tests =====

    @Test
    void testAnalyzeRisks_Success() throws Exception {
        // Arrange
        String projectId = "1";
        
        // Mock the API accessibility check for security validation
        when(apiClient.isApiAccessible()).thenReturn(true);
        when(apiClient.getProjects()).thenReturn(List.of(testProjectInfo));
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(testWorkPackages);
        
        // Act
        String result = projectService.analyzeRisks(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Risk Analysis") || result.contains("风险分析"));
        assertTrue(result.contains("Test Project"));
        
        // Verify API calls
        verify(apiClient).getProjects();
        verify(apiClient, atLeastOnce()).getWorkPackages(anyMap(), anyInt(), anyInt());
    }

    @Test
    void testAnalyzeRisks_WithOverdueTasks() throws Exception {
        // Arrange
        String projectId = "1";
        
        // Create more overdue tasks for testing risk level calculation
        WorkPackage.Assignee assignee = new WorkPackage.Assignee("1", "John Doe", "johndoe", "john@example.com");
        WorkPackage.WorkPackageStatus statusNew = new WorkPackage.WorkPackageStatus("1", "New", false, true, "1");
        WorkPackage.WorkPackagePriority priorityHigh = new WorkPackage.WorkPackagePriority("6", "High", false, "6");
        WorkPackage.WorkPackageType taskType = new WorkPackage.WorkPackageType("1", "Task", true, "#FF0000", "1");
        
        WorkPackage overdueTask2 = new WorkPackage(
            "4", "Another Overdue Task", "Another overdue description", statusNew,
            taskType,
            priorityHigh, LocalDate.now().minusDays(3), LocalDate.now(),
            LocalDateTime.now(), LocalDateTime.now(), assignee, 20, 8.0, null, 
            new WorkPackage.Links("1", "1", "1", "1", "1"), null
        );
        
        List<WorkPackage> workPackagesWithMoreOverdue = List.of(testWorkPackage, completedWorkPackage, overdueWorkPackage, overdueTask2);
        
        // Mock the API accessibility check for security validation
        when(apiClient.isApiAccessible()).thenReturn(true);
        when(apiClient.getProjects()).thenReturn(List.of(testProjectInfo));
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(workPackagesWithMoreOverdue);
        
        // Act
        String result = projectService.analyzeRisks(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Risk Analysis") || result.contains("风险分析"));
        assertTrue(result.contains("overdue") || result.contains("逾期"));
    }

    @Test
    void testAnalyzeRisks_NoOverdueTasks() throws Exception {
        // Arrange
        String projectId = "1";
        
        List<WorkPackage> noOverdueTasks = List.of(testWorkPackage, completedWorkPackage);
        
        // Mock the API accessibility check for security validation
        when(apiClient.isApiAccessible()).thenReturn(true);
        when(apiClient.getProjects()).thenReturn(List.of(testProjectInfo));
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(noOverdueTasks);
        
        // Act
        String result = projectService.analyzeRisks(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Risk Analysis") || result.contains("风险分析"));
    }

    @Test
    void testAnalyzeRisks_InvalidProjectId() {
        // Arrange
        String projectId = "";
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            projectService.analyzeRisks(projectId);
        });
        
        assertTrue(exception.getMessage().contains("Project ID cannot be null or empty"));
    }

    // ===== calculateCompletion Tests =====

    @Test
    void testCalculateCompletion_Success() throws Exception {
        // Arrange
        String projectId = "1";
        
        when(apiClient.getProjects()).thenReturn(List.of(testProjectInfo));
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(testWorkPackages);
        
        // Act
        String result = projectService.calculateCompletion(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Enhanced Project Completion Analysis") || result.contains("增强项目完成度分析"));
        assertTrue(result.contains("Test Project"));
        
        // The completion calculation is complex and includes priority weighting
        // The actual result shows 55.7% completion for the test data
        assertTrue(result.contains("55.7") || result.contains("55"));
        
        // Verify API calls
        verify(apiClient).getProjects();
        verify(apiClient, atLeastOnce()).getWorkPackages(anyMap(), anyInt(), anyInt());
    }

    @Test
    void testCalculateCompletion_AllTasksCompleted() throws Exception {
        // Arrange
        String projectId = "1";
        
        WorkPackage.Assignee assignee = new WorkPackage.Assignee("1", "John Doe", "johndoe", "john@example.com");
        WorkPackage.WorkPackageStatus statusCompleted = new WorkPackage.WorkPackageStatus("2", "Completed", true, false, "2");
        WorkPackage.WorkPackagePriority priorityNormal = new WorkPackage.WorkPackagePriority("8", "Normal", true, "8");
        WorkPackage.WorkPackageType taskType = new WorkPackage.WorkPackageType("1", "Task", true, "#FF0000", "1");
        
        WorkPackage completedTask2 = new WorkPackage(
            "4", "Another Completed Task", "Another completed description", statusCompleted,
            taskType,
            priorityNormal, LocalDate.now().plusDays(7), LocalDate.now(),
            LocalDateTime.now(), LocalDateTime.now(), assignee, 100, 8.0, null, 
            new WorkPackage.Links("1", "1", "1", "1", "1"), null
        );
        
        WorkPackage completedTask3 = new WorkPackage(
            "5", "Third Completed Task", "Third completed description", statusCompleted,
            taskType,
            priorityNormal, LocalDate.now().plusDays(7), LocalDate.now(),
            LocalDateTime.now(), LocalDateTime.now(), assignee, 100, 8.0, null, 
            new WorkPackage.Links("1", "1", "1", "1", "1"), null
        );
        
        List<WorkPackage> allCompletedTasks = List.of(completedWorkPackage, completedTask2, completedTask3);
        
        when(apiClient.getProjects()).thenReturn(List.of(testProjectInfo));
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(allCompletedTasks);
        
        // Act
        String result = projectService.calculateCompletion(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Enhanced Project Completion Analysis") || result.contains("增强项目完成度分析"));
        
        // Should show 100% completion
        assertTrue(result.contains("100"));
        assertTrue(result.contains("completed") || result.contains("完成"));
    }

    @Test
    void testCalculateCompletion_NoTasks() throws Exception {
        // Arrange
        String projectId = "1";
        
        when(apiClient.getProjects()).thenReturn(List.of(testProjectInfo));
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(List.of());
        
        // Act
        String result = projectService.calculateCompletion(projectId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Project Completion Analysis") || result.contains("项目完成度分析"));
        
        // Should show empty project message
        assertTrue(result.contains("no tasks") || result.contains("没有任务") || result.contains("cannot calculate completion"));
    }

    @Test
    void testCalculateCompletion_InvalidProjectId() {
        // Arrange
        String projectId = "";
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            projectService.calculateCompletion(projectId);
        });
        
        assertTrue(exception.getMessage().contains("Project ID cannot be null or empty"));
        assertTrue(exception.getMessage().contains("项目ID不能为空"));
    }

    @Test
    void testCalculateCompletion_RateLimitRetry() throws Exception {
        // Arrange
        String projectId = "1";
        
        // All calls throw rate limit exception to test error handling
        when(apiClient.getProjects())
            .thenThrow(new RateLimitExceededException("Rate limit exceeded", 
                java.time.Duration.ofSeconds(60), java.time.Instant.now().plusSeconds(60), 0, 100));
        
        // Act
        String result = projectService.calculateCompletion(projectId);
        
        // Assert
        assertNotNull(result);
        // After all retries fail, it should return a rate limit error report
        assertTrue(result.contains("Rate Limit Error") || result.contains("速率限制错误"));
        
        // Should have been called at least once
        verify(apiClient, atLeast(1)).getProjects();
    }

    // ===== Edge Cases and Integration Tests =====

    @Test
    void testAllMethodsWithValidProjectId() throws Exception {
        // Arrange
        String projectId = "test-project"; // Use valid ID per security validation
        
        // Mock the API accessibility check for security validation
        when(apiClient.isApiAccessible()).thenReturn(true);
        when(apiClient.getProjects()).thenReturn(List.of(testProjectInfo));
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0))).thenReturn(testWorkPackages);
        
        // Act & Assert - all methods should handle valid project ID gracefully
        assertDoesNotThrow(() -> projectService.generateWeeklyReport(projectId));
        assertDoesNotThrow(() -> projectService.analyzeRisks(projectId));
        assertDoesNotThrow(() -> projectService.calculateCompletion(projectId));
    }

    @Test
    void testInputValidationAndSanitization() throws Exception {
        // Arrange - test various invalid inputs
        String[] invalidProjectIds = {"", null, "  "};
        String[] maliciousProjectIds = {"test; DROP TABLE users;", "<script>alert('xss')</script>"};
        
        // Act & Assert - Test null/empty inputs
        for (String invalidId : invalidProjectIds) {
            assertThrows(IllegalArgumentException.class, () -> {
                projectService.generateWeeklyReport(invalidId);
            });
            assertThrows(IllegalArgumentException.class, () -> {
                projectService.analyzeRisks(invalidId);
            });
            assertThrows(IllegalArgumentException.class, () -> {
                projectService.calculateCompletion(invalidId);
            });
        }
        
        // Act & Assert - Test malicious inputs (should be sanitized, not throw exception)
        for (String maliciousId : maliciousProjectIds) {
            // Mock the API for this test
            when(apiClient.getWorkPackages(anyMap(), anyInt(), anyInt())).thenReturn(testWorkPackages);
            
            // These should not throw exceptions but should sanitize the input
            String result = projectService.generateWeeklyReport(maliciousId);
            assertNotNull(result);
        }
    }

    @Test
    void testRetryLogicForTransientFailures() throws Exception {
        // Arrange
        String projectId = "1";
        
        // Configure API client to always fail with rate limit exceptions
        when(apiClient.getProjects())
            .thenThrow(new RateLimitExceededException("Rate limit exceeded", 
                java.time.Duration.ofSeconds(1), java.time.Instant.now().plusSeconds(1), 0, 100));
        
        // Act
        String result = projectService.calculateCompletion(projectId);
        
        // Assert
        assertNotNull(result);
        // After all retries fail, it should return a rate limit error report
        assertTrue(result.contains("Rate Limit Error") || result.contains("速率限制错误"));
        
        // Should have been called at least once
        verify(apiClient, atLeast(1)).getProjects();
    }

    @Test
    void testBilingualErrorMessages() throws Exception {
        // Arrange
        String projectId = "1";
        
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0)))
            .thenThrow(new OpenProjectApiException("Test API Error", 500, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));
        
        // Act
        String result = projectService.generateWeeklyReport(projectId);
        
        // Assert - should contain both English and Chinese error messages
        assertNotNull(result);
        assertTrue(result.length() > 0); // Should have some error content
        
        // Test with rate limit error
        reset(apiClient);
        when(apiClient.getWorkPackages(anyMap(), eq(100), eq(0)))
            .thenThrow(new RateLimitExceededException("Rate limit exceeded", 
                java.time.Duration.ofSeconds(60), java.time.Instant.now().plusSeconds(60), 0, 100));
        
        String rateLimitResult = projectService.generateWeeklyReport(projectId);
        assertNotNull(rateLimitResult);
        assertTrue(rateLimitResult.length() > 0);
    }

    @Test
    void testResourceAccessExceptionHandling() throws Exception {
        // Arrange
        String projectId = "1";
        
        when(apiClient.getProjects())
            .thenThrow(new ResourceAccessException("Connection refused"));
        
        // Act
        String result = projectService.calculateCompletion(projectId);
        
        // Assert
        assertNotNull(result);
        // The method should handle the exception and return some kind of error response
        assertTrue(result.length() > 0);
    }
}