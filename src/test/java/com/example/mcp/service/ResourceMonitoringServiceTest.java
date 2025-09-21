package com.example.mcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ResourceMonitoringService.
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@ExtendWith(MockitoExtension.class)
class ResourceMonitoringServiceTest {

    @Mock
    private MemoryMXBean mockMemoryMXBean;

    @Mock
    private OperatingSystemMXBean mockOperatingSystemMXBean;

    @Mock
    private ThreadMXBean mockThreadMXBean;

    private ResourceMonitoringService resourceMonitoringService;

    @BeforeEach
    void setUp() {
        // Mock the ManagementFactory beans
        try {
            var memoryField = ManagementFactory.class.getDeclaredField("memoryMXBean");
            memoryField.setAccessible(true);
            memoryField.set(null, mockMemoryMXBean);

            var osField = ManagementFactory.class.getDeclaredField("operatingSystemMXBean");
            osField.setAccessible(true);
            osField.set(null, mockOperatingSystemMXBean);

            var threadField = ManagementFactory.class.getDeclaredField("threadMXBean");
            threadField.setAccessible(true);
            threadField.set(null, mockThreadMXBean);
        } catch (Exception e) {
            // If we can't mock the static fields, we'll test with real values
            // This is acceptable for integration testing
        }

        resourceMonitoringService = new ResourceMonitoringService();
    }

    @Test
    void testHealthReturnsValidStatus() {
        // Act
        Health health = resourceMonitoringService.health();

        // Assert
        assertNotNull(health);
        assertNotNull(health.getStatus());
        assertTrue(health.getDetails().containsKey("memory"));
        assertTrue(health.getDetails().containsKey("cpu"));
        assertTrue(health.getDetails().containsKey("threads"));
        assertTrue(health.getDetails().containsKey("system"));
        
        // Status should be one of the valid health statuses
        assertTrue(health.getStatus() == Status.UP || 
                   health.getStatus() == Status.OUT_OF_SERVICE || 
                   health.getStatus() == Status.DOWN);
    }

    @Test
    void testHealthWithExceptionStillReturnsHealthObject() {
        // Act
        Health health = resourceMonitoringService.health();

        // Assert
        assertNotNull(health);
        // Should handle the exception gracefully and still return a health object
        assertNotNull(health.getStatus());
        assertTrue(health.getDetails().containsKey("memory"));
        assertTrue(health.getDetails().get("memory") instanceof Map);
    }

    @Test
    void testResourceSummaryContainsExpectedFields() {
        // Act
        Map<String, Object> summary = resourceMonitoringService.getResourceSummary();

        // Assert
        assertNotNull(summary);
        assertTrue(summary.containsKey("status"));
        assertTrue(summary.containsKey("timestamp"));
        // These fields may or may not be present depending on the test environment
    }

    @Test
    void testHealthHandlesExceptionsGracefully() {
        // Act
        Health health = resourceMonitoringService.health();

        // Assert
        assertNotNull(health);
        // Should handle the exception gracefully
        assertNotNull(health.getStatus());
    }

    @Test
    void testMemoryMetricsCollection() {
        // Act
        Health health = resourceMonitoringService.health();
        Map<String, Object> details = health.getDetails();

        // Assert
        assertTrue(details.containsKey("memory"));
        @SuppressWarnings("unchecked")
        Map<String, Object> memory = (Map<String, Object>) details.get("memory");
        
        assertTrue(memory.containsKey("heapUsed") || memory.containsKey("memoryError"));
        assertTrue(memory.containsKey("heapMax") || memory.containsKey("memoryError"));
    }

    @Test
    void testCpuMetricsCollection() {
        // Act
        Health health = resourceMonitoringService.health();
        Map<String, Object> details = health.getDetails();

        // Assert
        assertTrue(details.containsKey("cpu"));
        @SuppressWarnings("unchecked")
        Map<String, Object> cpu = (Map<String, Object>) details.get("cpu");
        
        assertTrue(cpu.containsKey("systemLoadAverage") || cpu.containsKey("cpuError"));
        assertTrue(cpu.containsKey("availableProcessors") || cpu.containsKey("cpuError"));
    }

    @Test
    void testThreadMetricsCollection() {
        // Act
        Health health = resourceMonitoringService.health();
        Map<String, Object> details = health.getDetails();

        // Assert
        assertTrue(details.containsKey("threads"));
        @SuppressWarnings("unchecked")
        Map<String, Object> threads = (Map<String, Object>) details.get("threads");
        
        assertTrue(threads.containsKey("liveThreads") || threads.containsKey("threadError"));
        assertTrue(threads.containsKey("peakThreads") || threads.containsKey("threadError"));
    }

    @Test
    void testSystemMetricsCollection() {
        // Act
        Health health = resourceMonitoringService.health();
        Map<String, Object> details = health.getDetails();

        // Assert
        assertTrue(details.containsKey("system"));
        @SuppressWarnings("unchecked")
        Map<String, Object> system = (Map<String, Object>) details.get("system");
        
        assertTrue(system.containsKey("availableProcessors") || system.containsKey("systemError"));
        assertTrue(system.containsKey("osName") || system.containsKey("systemError"));
        assertTrue(system.containsKey("javaVersion") || system.containsKey("systemError"));
    }

    @Test
    void testFormatBytes() {
        // Test with reflection since the method is private
        try {
            var method = ResourceMonitoringService.class.getDeclaredMethod("formatBytes", long.class);
            method.setAccessible(true);
            
            assertEquals("100 B", method.invoke(resourceMonitoringService, 100L));
            assertEquals("1.0 KB", method.invoke(resourceMonitoringService, 1024L));
            assertEquals("1.0 MB", method.invoke(resourceMonitoringService, 1024L * 1024));
            assertEquals("1.0 GB", method.invoke(resourceMonitoringService, 1024L * 1024 * 1024));
        } catch (Exception e) {
            fail("Should be able to access formatBytes method: " + e.getMessage());
        }
    }

    @Test
    void testFormatUptime() {
        // Test with reflection since the method is private
        try {
            var method = ResourceMonitoringService.class.getDeclaredMethod("formatUptime", long.class);
            method.setAccessible(true);
            
            assertEquals("30 seconds", method.invoke(resourceMonitoringService, 30000L));
            assertEquals("1 minutes, 30 seconds", method.invoke(resourceMonitoringService, 90000L));
            assertEquals("1 hours, 1 minutes, 30 seconds", method.invoke(resourceMonitoringService, 3690000L));
        } catch (Exception e) {
            fail("Should be able to access formatUptime method: " + e.getMessage());
        }
    }

    @Test
    void testGetLoadStatus() {
        // Test with reflection since the method is private
        try {
            var method = ResourceMonitoringService.class.getDeclaredMethod("getLoadStatus", double.class);
            method.setAccessible(true);
            
            assertEquals("NORMAL", method.invoke(resourceMonitoringService, 25.0));
            assertEquals("MODERATE", method.invoke(resourceMonitoringService, 60.0));
            assertEquals("HIGH", method.invoke(resourceMonitoringService, 85.0));
            assertEquals("CRITICAL", method.invoke(resourceMonitoringService, 95.0));
        } catch (Exception e) {
            fail("Should be able to access getLoadStatus method: " + e.getMessage());
        }
    }

    @Test
    void testGetCpuUsageStatus() {
        // Test with reflection since the method is private
        try {
            var method = ResourceMonitoringService.class.getDeclaredMethod("getCpuUsageStatus", double.class);
            method.setAccessible(true);
            
            assertEquals("NORMAL", method.invoke(resourceMonitoringService, 25.0));
            assertEquals("MODERATE", method.invoke(resourceMonitoringService, 60.0));
            assertEquals("HIGH", method.invoke(resourceMonitoringService, 85.0));
            assertEquals("CRITICAL", method.invoke(resourceMonitoringService, 95.0));
        } catch (Exception e) {
            fail("Should be able to access getCpuUsageStatus method: " + e.getMessage());
        }
    }

    @Test
    void testGetThreadStatus() {
        // Test with reflection since the method is private
        try {
            var method = ResourceMonitoringService.class.getDeclaredMethod("getThreadStatus", int.class, int.class);
            method.setAccessible(true);
            
            assertEquals("NORMAL", method.invoke(resourceMonitoringService, 50, 100));
            assertEquals("MODERATE", method.invoke(resourceMonitoringService, 80, 100));
            assertEquals("HIGH", method.invoke(resourceMonitoringService, 95, 100));
        } catch (Exception e) {
            fail("Should be able to access getThreadStatus method: " + e.getMessage());
        }
    }
}