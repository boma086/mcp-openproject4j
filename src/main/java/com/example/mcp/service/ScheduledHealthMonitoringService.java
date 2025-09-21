package com.example.mcp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduled health monitoring service that periodically checks system resources
 * and API connectivity, maintaining a history of health metrics for trend analysis.
 * 
 * This service addresses the following requirements:
 * <ul>
 *   <li><strong>NFR3: Reliability</strong> - Continuous monitoring for proactive issue detection</li>
 *   <li><strong>NFR4: Scalability</strong> - Historical data for capacity planning</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Slf4j
@Service
public class ScheduledHealthMonitoringService {

    @Autowired
    private ResourceMonitoringService resourceMonitoringService;

    @Autowired
    private OpenProjectApiHealthIndicator apiHealthIndicator;

    private final Map<String, Object> healthHistory = new ConcurrentHashMap<>();
    private final AtomicLong lastHealthCheck = new AtomicLong(0);
    private final int MAX_HISTORY_SIZE = 100;
    private final long HEALTH_CHECK_INTERVAL_MS = 30000; // 30 seconds

    private final Map<String, Object> alerts = new ConcurrentHashMap<>();
    private final AtomicLong alertCount = new AtomicLong(0);

    /**
     * Scheduled health check that runs every 30 seconds.
     * This method monitors both system resources and OpenProject API health.
     */
    @Scheduled(fixedRate = HEALTH_CHECK_INTERVAL_MS)
    public void performScheduledHealthCheck() {
        try {
            log.debug("Performing scheduled health check");
            
            Map<String, Object> healthSnapshot = createHealthSnapshot();
            storeHealthSnapshot(healthSnapshot);
            
            // Analyze health data for potential issues
            analyzeHealthTrends(healthSnapshot);
            
            // Update last check timestamp
            lastHealthCheck.set(System.currentTimeMillis());
            
            log.debug("Scheduled health check completed successfully");
            
        } catch (Exception e) {
            log.error("Error during scheduled health check", e);
            recordHealthCheckError(e);
        }
    }

    /**
     * Creates a comprehensive health snapshot.
     * 
     * @return health snapshot containing all metrics
     */
    private Map<String, Object> createHealthSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        long timestamp = System.currentTimeMillis();
        
        snapshot.put("timestamp", timestamp);
        snapshot.put("checkNumber", healthHistory.size() + 1);
        
        // Resource health
        try {
            Health resourceHealth = resourceMonitoringService.health();
            snapshot.put("resourceHealth", resourceHealth.getStatus().getCode());
            snapshot.put("resourceDetails", resourceHealth.getDetails());
            
            // Add resource summary
            Map<String, Object> resourceSummary = resourceMonitoringService.getResourceSummary();
            snapshot.put("resourceSummary", resourceSummary);
            
        } catch (Exception e) {
            log.warn("Error collecting resource health data", e);
            snapshot.put("resourceHealth", "ERROR");
            snapshot.put("resourceError", e.getMessage());
        }
        
        // API health
        try {
            Health apiHealth = apiHealthIndicator.health();
            snapshot.put("apiHealth", apiHealth.getStatus().getCode());
            snapshot.put("apiDetails", apiHealth.getDetails());
            
            // Add API summary
            Map<String, Object> apiSummary = apiHealthIndicator.getApiHealthSummary();
            snapshot.put("apiSummary", apiSummary);
            
        } catch (Exception e) {
            log.warn("Error collecting API health data", e);
            snapshot.put("apiHealth", "ERROR");
            snapshot.put("apiError", e.getMessage());
        }
        
        // Calculate overall health
        String resourceHealth = (String) snapshot.getOrDefault("resourceHealth", "UNKNOWN");
        String apiHealth = (String) snapshot.getOrDefault("apiHealth", "UNKNOWN");
        String overallHealth = calculateOverallHealth(resourceHealth, apiHealth);
        snapshot.put("overallHealth", overallHealth);
        
        return snapshot;
    }

    /**
     * Stores health snapshot in history with size management.
     * 
     * @param snapshot the health snapshot to store
     */
    private void storeHealthSnapshot(Map<String, Object> snapshot) {
        String key = "check_" + System.currentTimeMillis();
        
        // Manage history size
        if (healthHistory.size() >= MAX_HISTORY_SIZE) {
            // Remove oldest entry
            String oldestKey = healthHistory.keySet().stream()
                .sorted()
                .findFirst()
                .orElse(null);
            if (oldestKey != null) {
                healthHistory.remove(oldestKey);
                log.debug("Removed oldest health snapshot: {}", oldestKey);
            }
        }
        
        healthHistory.put(key, snapshot);
        log.debug("Stored health snapshot: {} (total: {})", key, healthHistory.size());
    }

    /**
     * Analyzes health trends and detects potential issues.
     * 
     * @param currentSnapshot the current health snapshot
     */
    private void analyzeHealthTrends(Map<String, Object> currentSnapshot) {
        try {
            // Check for critical issues
            String overallHealth = (String) currentSnapshot.get("overallHealth");
            if ("DOWN".equals(overallHealth)) {
                createAlert("CRITICAL", "System health is DOWN", currentSnapshot);
            } else if ("OUT_OF_SERVICE".equals(overallHealth)) {
                createAlert("WARNING", "System health is DEGRADED", currentSnapshot);
            }
            
            // Analyze resource trends
            analyzeResourceTrends(currentSnapshot);
            
            // Analyze API trends
            analyzeApiTrends(currentSnapshot);
            
        } catch (Exception e) {
            log.error("Error analyzing health trends", e);
        }
    }

    /**
     * Analyzes resource usage trends.
     * 
     * @param currentSnapshot the current health snapshot
     */
    private void analyzeResourceTrends(Map<String, Object> currentSnapshot) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resourceSummary = (Map<String, Object>) currentSnapshot.get("resourceSummary");
            
            if (resourceSummary.containsKey("memoryUsage")) {
                String memoryUsage = (String) resourceSummary.get("memoryUsage");
                double memoryPercent = Double.parseDouble(memoryUsage.replace("%", ""));
                
                if (memoryPercent > 90) {
                    createAlert("CRITICAL", "High memory usage: " + memoryUsage, currentSnapshot);
                } else if (memoryPercent > 80) {
                    createAlert("WARNING", "Elevated memory usage: " + memoryUsage, currentSnapshot);
                }
            }
            
            if (resourceSummary.containsKey("cpuUsage")) {
                String cpuUsage = (String) resourceSummary.get("cpuUsage");
                double cpuPercent = Double.parseDouble(cpuUsage.replace("%", ""));
                
                if (cpuPercent > 90) {
                    createAlert("CRITICAL", "High CPU usage: " + cpuUsage, currentSnapshot);
                } else if (cpuPercent > 80) {
                    createAlert("WARNING", "Elevated CPU usage: " + cpuUsage, currentSnapshot);
                }
            }
            
            // Check for memory leak patterns (consistently increasing memory)
            analyzeMemoryLeakPattern();
            
        } catch (Exception e) {
            log.warn("Error analyzing resource trends", e);
        }
    }

    /**
     * Analyzes API health trends.
     * 
     * @param currentSnapshot the current health snapshot
     */
    private void analyzeApiTrends(Map<String, Object> currentSnapshot) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> apiSummary = (Map<String, Object>) currentSnapshot.get("apiSummary");
            
            if (apiSummary.containsKey("responseTimeMs")) {
                Long responseTime = (Long) apiSummary.get("responseTimeMs");
                
                if (responseTime > 10000) {
                    createAlert("WARNING", "Slow API response time: " + responseTime + "ms", currentSnapshot);
                }
            }
            
            if (Boolean.TRUE.equals(apiSummary.get("rateLimited"))) {
                createAlert("WARNING", "API rate limiting detected", currentSnapshot);
            }
            
            // Check for API connectivity issues
            String apiHealth = (String) currentSnapshot.get("apiHealth");
            if (!"UP".equals(apiHealth)) {
                createAlert("CRITICAL", "API connectivity issues detected: " + apiHealth, currentSnapshot);
            }
            
        } catch (Exception e) {
            log.warn("Error analyzing API trends", e);
        }
    }

    /**
     * Analyzes memory usage patterns for potential leaks.
     */
    private void analyzeMemoryLeakPattern() {
        try {
            if (healthHistory.size() < 5) {
                return; // Not enough data for trend analysis
            }
            
            // Get last 5 snapshots for memory analysis
            java.util.List<Map<String, Object>> recentSnapshots = new java.util.ArrayList<>();
            healthHistory.values().stream()
                .sorted((a, b) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> snapshotA = (Map<String, Object>) a;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> snapshotB = (Map<String, Object>) b;
                    return Long.compare((Long) snapshotB.get("timestamp"), (Long) snapshotA.get("timestamp"));
                })
                .limit(5)
                .forEach(snapshot -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedSnapshot = (Map<String, Object>) snapshot;
                    recentSnapshots.add(typedSnapshot);
                });
            
            java.util.List<Double> memoryUsages = new java.util.ArrayList<>();
            for (Map<String, Object> snapshot : recentSnapshots) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resourceSummary = (Map<String, Object>) snapshot.get("resourceSummary");
                    if (resourceSummary.containsKey("memoryUsage")) {
                        String memoryUsage = (String) resourceSummary.get("memoryUsage");
                        double memoryPercent = Double.parseDouble(memoryUsage.replace("%", ""));
                        memoryUsages.add(memoryPercent);
                    }
                } catch (Exception e) {
                    continue; // Skip invalid entries
                }
            }
            
            if (memoryUsages.size() >= 3) {
                // Check for consistently increasing memory
                boolean increasingTrend = true;
                for (int i = 1; i < memoryUsages.size(); i++) {
                    if (memoryUsages.get(i) <= memoryUsages.get(i - 1)) {
                        increasingTrend = false;
                        break;
                    }
                }
                
                if (increasingTrend && memoryUsages.get(memoryUsages.size() - 1) > 70) {
                    createAlert("WARNING", "Potential memory leak detected - consistently increasing memory usage", 
                              recentSnapshots.get(recentSnapshots.size() - 1));
                }
            }
            
        } catch (Exception e) {
            log.warn("Error analyzing memory leak patterns", e);
        }
    }

    /**
     * Creates an alert for potential issues.
     * 
     * @param level the alert level (CRITICAL, WARNING, INFO)
     * @param message the alert message
     * @param snapshot the health snapshot that triggered the alert
     */
    private void createAlert(String level, String message, Map<String, Object> snapshot) {
        try {
            String alertId = "alert_" + System.currentTimeMillis() + "_" + alertCount.incrementAndGet();
            
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", alertId);
            alert.put("level", level);
            alert.put("message", message);
            alert.put("timestamp", System.currentTimeMillis());
            alert.put("snapshot", snapshot);
            
            alerts.put(alertId, alert);
            
            // Log alert
            if ("CRITICAL".equals(level)) {
                log.error("ALERT [{}]: {}", level, message);
            } else if ("WARNING".equals(level)) {
                log.warn("ALERT [{}]: {}", level, message);
            } else {
                log.info("ALERT [{}]: {}", level, message);
            }
            
            // Keep only recent alerts (limit to 50)
            if (alerts.size() > 50) {
                String oldestAlertId = alerts.keySet().stream()
                    .sorted()
                    .findFirst()
                    .orElse(null);
                if (oldestAlertId != null) {
                    alerts.remove(oldestAlertId);
                }
            }
            
        } catch (Exception e) {
            log.error("Error creating alert", e);
        }
    }

    /**
     * Records health check errors.
     * 
     * @param e the exception that occurred
     */
    private void recordHealthCheckError(Exception e) {
        createAlert("ERROR", "Health check failed: " + e.getMessage(), null);
    }

    /**
     * Calculates overall health status.
     * 
     * @param resourceHealth resource health status
     * @param apiHealth API health status
     * @return overall health status
     */
    private String calculateOverallHealth(String resourceHealth, String apiHealth) {
        if ("DOWN".equals(resourceHealth) || "DOWN".equals(apiHealth)) {
            return "DOWN";
        }
        if ("OUT_OF_SERVICE".equals(resourceHealth) || "OUT_OF_SERVICE".equals(apiHealth)) {
            return "OUT_OF_SERVICE";
        }
        if ("UP".equals(resourceHealth) && "UP".equals(apiHealth)) {
            return "UP";
        }
        return "UNKNOWN";
    }

    /**
     * Gets the current health status.
     * 
     * @return current health status
     */
    public Map<String, Object> getCurrentHealth() {
        if (healthHistory.isEmpty()) {
            return Map.of("status", "NO_DATA");
        }
        
        // Get the most recent snapshot
        Map<String, Object> latestSnapshot = healthHistory.values().stream()
            .sorted((a, b) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> snapshotA = (Map<String, Object>) a;
                @SuppressWarnings("unchecked")
                Map<String, Object> snapshotB = (Map<String, Object>) b;
                return Long.compare((Long) snapshotB.get("timestamp"), (Long) snapshotA.get("timestamp"));
            })
            .findFirst()
            .map(snapshot -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedSnapshot = (Map<String, Object>) snapshot;
                return typedSnapshot;
            })
            .orElse(null);
        
        return latestSnapshot != null ? latestSnapshot : Map.of("status", "NO_DATA");
    }

    /**
     * Gets health history for the specified time range.
     * 
     * @param sinceTime start time in milliseconds
     * @return list of health snapshots within the time range
     */
    public java.util.List<Map<String, Object>> getHealthHistory(long sinceTime) {
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        healthHistory.values().stream()
            .filter(snapshot -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedSnapshot = (Map<String, Object>) snapshot;
                return (Long) typedSnapshot.get("timestamp") >= sinceTime;
            })
            .sorted((a, b) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> snapshotA = (Map<String, Object>) a;
                @SuppressWarnings("unchecked")
                Map<String, Object> snapshotB = (Map<String, Object>) b;
                return Long.compare((Long) snapshotA.get("timestamp"), (Long) snapshotB.get("timestamp"));
            })
            .forEach(snapshot -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedSnapshot = (Map<String, Object>) snapshot;
                result.add(typedSnapshot);
            });
        return result;
    }

    /**
     * Gets current alerts.
     * 
     * @return current alerts
     */
    public Map<String, Object> getAlerts() {
        return new HashMap<>(alerts);
    }

    /**
     * Gets monitoring statistics.
     * 
     * @return monitoring statistics
     */
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHealthChecks", healthHistory.size());
        stats.put("lastHealthCheck", lastHealthCheck.get());
        stats.put("alertCount", alertCount.get());
        stats.put("activeAlerts", alerts.size());
        stats.put("checkInterval", HEALTH_CHECK_INTERVAL_MS);
        stats.put("maxHistorySize", MAX_HISTORY_SIZE);
        
        // Calculate uptime percentage
        if (!healthHistory.isEmpty()) {
            long healthyChecks = healthHistory.values().stream()
                .filter(snapshot -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedSnapshot = (Map<String, Object>) snapshot;
                    return "UP".equals(typedSnapshot.get("overallHealth"));
                })
                .count();
            double uptimePercentage = (healthyChecks * 100.0) / healthHistory.size();
            stats.put("uptimePercentage", String.format("%.2f%%", uptimePercentage));
        }
        
        return stats;
    }

    /**
     * Clears monitoring history and alerts.
     */
    public void clearMonitoringData() {
        healthHistory.clear();
        alerts.clear();
        lastHealthCheck.set(0);
        alertCount.set(0);
        log.info("Monitoring data cleared");
    }
}