package com.example.mcp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource monitoring service that provides comprehensive health metrics
 * for the MCP server including memory, CPU, threads, and system resources.
 * 
 * This service addresses the following requirements:
 * <ul>
 *   <li><strong>NFR3: Reliability</strong> - Resource monitoring to ensure system stability</li>
 *   <li><strong>NFR4: Scalability</strong> - Monitoring resource usage patterns for scaling decisions</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Slf4j
@Component
public class ResourceMonitoringService implements HealthIndicator {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            
            // Memory metrics
            addMemoryMetrics(details);
            
            // CPU metrics
            addCpuMetrics(details);
            
            // Thread metrics
            addThreadMetrics(details);
            
            // System metrics
            addSystemMetrics(details);
            
            // Determine overall health status
            Status status = determineHealthStatus(details);
            
            return Health.status(status)
                    .withDetails(details)
                    .build();
            
        } catch (Exception e) {
            log.error("Error collecting resource metrics", e);
            return Health.down()
                    .withException(e)
                    .withDetail("error", "Failed to collect resource metrics: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Adds memory-related metrics to the health details.
     * 
     * @param details the map to add memory metrics to
     */
    private void addMemoryMetrics(Map<String, Object> details) {
        try {
            // Heap memory
            long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
            double heapUsagePercent = (heapUsed * 100.0) / heapMax;
            
            // Non-heap memory
            long nonHeapUsed = memoryMXBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
            double nonHeapUsagePercent = nonHeapMax > 0 ? (nonHeapUsed * 100.0) / nonHeapMax : 0.0;
            
            // JVM memory
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            double jvmMemoryUsagePercent = (usedMemory * 100.0) / maxMemory;
            
            Map<String, Object> memoryMetrics = new HashMap<>();
            memoryMetrics.put("heapUsed", formatBytes(heapUsed));
            memoryMetrics.put("heapMax", formatBytes(heapMax));
            memoryMetrics.put("heapUsagePercent", String.format("%.2f%%", heapUsagePercent));
            memoryMetrics.put("nonHeapUsed", formatBytes(nonHeapUsed));
            memoryMetrics.put("nonHeapMax", formatBytes(nonHeapMax));
            memoryMetrics.put("nonHeapUsagePercent", String.format("%.2f%%", nonHeapUsagePercent));
            memoryMetrics.put("jvmUsed", formatBytes(usedMemory));
            memoryMetrics.put("jvmMax", formatBytes(maxMemory));
            memoryMetrics.put("jvmUsagePercent", String.format("%.2f%%", jvmMemoryUsagePercent));
            memoryMetrics.put("freeMemory", formatBytes(freeMemory));
            memoryMetrics.put("totalMemory", formatBytes(totalMemory));
            
            details.put("memory", memoryMetrics);
            
            log.debug("Memory metrics collected - Heap: {:.2f}%, JVM: {:.2f}%", heapUsagePercent, jvmMemoryUsagePercent);
            
        } catch (Exception e) {
            log.warn("Error collecting memory metrics", e);
            details.put("memoryError", "Failed to collect memory metrics: " + e.getMessage());
        }
    }

    /**
     * Adds CPU-related metrics to the health details.
     * 
     * @param details the map to add CPU metrics to
     */
    private void addCpuMetrics(Map<String, Object> details) {
        try {
            // Get system load average
            double systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();
            int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
            double loadAveragePercent = (systemLoadAverage / availableProcessors) * 100.0;
            
            // Try to get CPU usage if available
            double cpuUsage = 0.0;
            try {
                if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunOsBean = 
                        (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
                    cpuUsage = sunOsBean.getProcessCpuLoad() * 100.0;
                }
            } catch (Exception e) {
                log.debug("Detailed CPU usage not available on this platform");
            }
            
            Map<String, Object> cpuMetrics = new HashMap<>();
            cpuMetrics.put("systemLoadAverage", String.format("%.2f", systemLoadAverage));
            cpuMetrics.put("availableProcessors", availableProcessors);
            cpuMetrics.put("loadAveragePercent", String.format("%.2f%%", loadAveragePercent));
            cpuMetrics.put("processCpuUsage", String.format("%.2f%%", cpuUsage));
            cpuMetrics.put("systemLoadStatus", getLoadStatus(loadAveragePercent));
            cpuMetrics.put("cpuUsageStatus", getCpuUsageStatus(cpuUsage));
            
            details.put("cpu", cpuMetrics);
            
            log.debug("CPU metrics collected - Load: {:.2f}%, Usage: {:.2f}%", loadAveragePercent, cpuUsage);
            
        } catch (Exception e) {
            log.warn("Error collecting CPU metrics", e);
            details.put("cpuError", "Failed to collect CPU metrics: " + e.getMessage());
        }
    }

    /**
     * Adds thread-related metrics to the health details.
     * 
     * @param details the map to add thread metrics to
     */
    private void addThreadMetrics(Map<String, Object> details) {
        try {
            int threadCount = threadMXBean.getThreadCount();
            int peakThreadCount = threadMXBean.getPeakThreadCount();
            int daemonThreadCount = threadMXBean.getDaemonThreadCount();
            long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();
            
            // Thread states (Note: Thread state counting requires ThreadInfo array)
            Map<String, Object> threadStates = new HashMap<>();
            try {
                long[] allThreadIds = threadMXBean.getAllThreadIds();
                Map<String, Long> stateCounts = new HashMap<>();
                
                for (long threadId : allThreadIds) {
                    try {
                        Thread.State state = threadMXBean.getThreadInfo(threadId, 0).getThreadState();
                        if (state != null) {
                            stateCounts.put(state.name(), stateCounts.getOrDefault(state.name(), 0L) + 1);
                        }
                    } catch (Exception e) {
                        // Thread may have terminated
                        continue;
                    }
                }
                
                threadStates.putAll(stateCounts);
            } catch (Exception e) {
                log.debug("Cannot collect detailed thread state information", e);
                threadStates.put("error", "Thread state collection not available");
            }
            
            Map<String, Object> threadMetrics = new HashMap<>();
            threadMetrics.put("liveThreads", threadCount);
            threadMetrics.put("peakThreads", peakThreadCount);
            threadMetrics.put("daemonThreads", daemonThreadCount);
            threadMetrics.put("totalStartedThreads", totalStartedThreadCount);
            threadMetrics.put("threadStates", threadStates);
            threadMetrics.put("threadStatus", getThreadStatus(threadCount, peakThreadCount));
            
            details.put("threads", threadMetrics);
            
            log.debug("Thread metrics collected - Live: {}, Peak: {}", threadCount, peakThreadCount);
            
        } catch (Exception e) {
            log.warn("Error collecting thread metrics", e);
            details.put("threadError", "Failed to collect thread metrics: " + e.getMessage());
        }
    }

    /**
     * Adds system-related metrics to the health details.
     * 
     * @param details the map to add system metrics to
     */
    private void addSystemMetrics(Map<String, Object> details) {
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // System information
            Map<String, Object> systemMetrics = new HashMap<>();
            systemMetrics.put("availableProcessors", runtime.availableProcessors());
            systemMetrics.put("osName", System.getProperty("os.name"));
            systemMetrics.put("osVersion", System.getProperty("os.version"));
            systemMetrics.put("osArch", System.getProperty("os.arch"));
            systemMetrics.put("javaVersion", System.getProperty("java.version"));
            systemMetrics.put("javaVendor", System.getProperty("java.vendor"));
            systemMetrics.put("jvmName", System.getProperty("java.vm.name"));
            systemMetrics.put("uptime", formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()));
            
            // Environment info
            systemMetrics.put("workingDirectory", System.getProperty("user.dir"));
            systemMetrics.put("tempDirectory", System.getProperty("java.io.tmpdir"));
            systemMetrics.put("userTimezone", System.getProperty("user.timezone"));
            
            details.put("system", systemMetrics);
            
            log.debug("System metrics collected - OS: {}, Java: {}", 
                     System.getProperty("os.name"), System.getProperty("java.version"));
            
        } catch (Exception e) {
            log.warn("Error collecting system metrics", e);
            details.put("systemError", "Failed to collect system metrics: " + e.getMessage());
        }
    }

    /**
     * Determines the overall health status based on collected metrics.
     * 
     * @param details the collected metrics
     * @return the health status
     */
    private Status determineHealthStatus(Map<String, Object> details) {
        try {
            // Check memory usage
            if (details.containsKey("memory")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memory = (Map<String, Object>) details.get("memory");
                String heapUsageStr = (String) memory.get("heapUsagePercent");
                double heapUsage = Double.parseDouble(heapUsageStr.replace("%", ""));
                
                if (heapUsage > 90.0) {
                    log.warn("High memory usage detected: {:.2f}%", heapUsage);
                    return Status.DOWN;
                } else if (heapUsage > 80.0) {
                    log.warn("Elevated memory usage: {:.2f}%", heapUsage);
                    return Status.OUT_OF_SERVICE;
                }
            }
            
            // Check CPU usage
            if (details.containsKey("cpu")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cpu = (Map<String, Object>) details.get("cpu");
                String cpuUsageStr = (String) cpu.get("processCpuUsage");
                double cpuUsage = Double.parseDouble(cpuUsageStr.replace("%", ""));
                
                if (cpuUsage > 90.0) {
                    log.warn("High CPU usage detected: {:.2f}%", cpuUsage);
                    return Status.DOWN;
                } else if (cpuUsage > 80.0) {
                    log.warn("Elevated CPU usage: {:.2f}%", cpuUsage);
                    return Status.OUT_OF_SERVICE;
                }
            }
            
            // Check thread usage
            if (details.containsKey("threads")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> threads = (Map<String, Object>) details.get("threads");
                Integer liveThreads = (Integer) threads.get("liveThreads");
                Integer peakThreads = (Integer) threads.get("peakThreads");
                
                if (liveThreads != null && peakThreads != null && liveThreads > peakThreads * 0.9) {
                    log.warn("High thread usage detected: {} live threads", liveThreads);
                    return Status.OUT_OF_SERVICE;
                }
            }
            
            return Status.UP;
            
        } catch (Exception e) {
            log.error("Error determining health status", e);
            return Status.DOWN;
        }
    }

    /**
     * Formats bytes to a human-readable string.
     * 
     * @param bytes the number of bytes
     * @return formatted string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Formats uptime to a human-readable string.
     * 
     * @param uptimeMs uptime in milliseconds
     * @return formatted string
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes, %d seconds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }

    /**
     * Gets the load status based on load average percentage.
     * 
     * @param loadAveragePercent the load average percentage
     * @return status description
     */
    private String getLoadStatus(double loadAveragePercent) {
        if (loadAveragePercent > 90.0) {
            return "CRITICAL";
        } else if (loadAveragePercent > 70.0) {
            return "HIGH";
        } else if (loadAveragePercent > 50.0) {
            return "MODERATE";
        } else {
            return "NORMAL";
        }
    }

    /**
     * Gets the CPU usage status based on CPU usage percentage.
     * 
     * @param cpuUsage the CPU usage percentage
     * @return status description
     */
    private String getCpuUsageStatus(double cpuUsage) {
        if (cpuUsage > 90.0) {
            return "CRITICAL";
        } else if (cpuUsage > 70.0) {
            return "HIGH";
        } else if (cpuUsage > 50.0) {
            return "MODERATE";
        } else {
            return "NORMAL";
        }
    }

    /**
     * Gets the thread status based on thread count.
     * 
     * @param threadCount the current thread count
     * @param peakThreadCount the peak thread count
     * @return status description
     */
    private String getThreadStatus(int threadCount, int peakThreadCount) {
        double ratio = (double) threadCount / peakThreadCount;
        if (ratio > 0.9) {
            return "HIGH";
        } else if (ratio > 0.7) {
            return "MODERATE";
        } else {
            return "NORMAL";
        }
    }

    /**
     * Gets a summary of current resource usage.
     * 
     * @return summary map with key metrics
     */
    public Map<String, Object> getResourceSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            Health health = health();
            Map<String, Object> details = health.getDetails();
            
            summary.put("status", health.getStatus().getCode());
            summary.put("timestamp", System.currentTimeMillis());
            
            if (details.containsKey("memory")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memory = (Map<String, Object>) details.get("memory");
                summary.put("memoryUsage", memory.get("heapUsagePercent"));
            }
            
            if (details.containsKey("cpu")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cpu = (Map<String, Object>) details.get("cpu");
                summary.put("cpuUsage", cpu.get("processCpuUsage"));
                summary.put("systemLoad", cpu.get("systemLoadAverage"));
            }
            
            if (details.containsKey("threads")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> threads = (Map<String, Object>) details.get("threads");
                summary.put("threadCount", threads.get("liveThreads"));
            }
            
            if (details.containsKey("system")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> system = (Map<String, Object>) details.get("system");
                summary.put("uptime", system.get("uptime"));
                summary.put("availableProcessors", system.get("availableProcessors"));
            }
            
        } catch (Exception e) {
            log.error("Error generating resource summary", e);
            summary.put("error", "Failed to generate resource summary: " + e.getMessage());
        }
        
        return summary;
    }
}