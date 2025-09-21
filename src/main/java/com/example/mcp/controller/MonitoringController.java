package com.example.mcp.controller;

import com.example.mcp.service.OpenProjectApiHealthIndicator;
import com.example.mcp.service.ResourceMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP Server monitoring controller that provides detailed resource metrics
 * and health information beyond the standard Actuator endpoints.
 * 
 * This controller addresses the following requirements:
 * <ul>
 *   <li><strong>NFR3: Reliability</strong> - Comprehensive monitoring for system stability</li>
 *   <li><strong>NFR4: Scalability</strong> - Resource usage tracking for scaling decisions</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@RestController
@RequestMapping("/api/v1/monitoring")
@Slf4j
public class MonitoringController {

    @Autowired
    private ResourceMonitoringService resourceMonitoringService;

    @Autowired
    private OpenProjectApiHealthIndicator apiHealthIndicator;

    /**
     * Get comprehensive resource monitoring information.
     * 
     * @return detailed resource metrics including memory, CPU, threads, and system info
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> getResourceMetrics() {
        try {
            Health health = resourceMonitoringService.health();
            Map<String, Object> response = new HashMap<>();
            
            response.put("status", health.getStatus().getCode());
            response.put("timestamp", System.currentTimeMillis());
            response.put("metrics", health.getDetails());
            
            log.debug("Resource metrics requested and provided");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting resource metrics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get resource metrics: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get resource usage summary.
     * 
     * @return summary of key resource usage metrics
     */
    @GetMapping("/resources/summary")
    public ResponseEntity<Map<String, Object>> getResourceSummary() {
        try {
            Map<String, Object> summary = resourceMonitoringService.getResourceSummary();
            
            log.debug("Resource summary requested and provided");
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Error getting resource summary", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get resource summary: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get OpenProject API health information.
     * 
     * @return detailed API health metrics including connectivity, authentication, and endpoint status
     */
    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> getApiMetrics() {
        try {
            Health health = apiHealthIndicator.health();
            Map<String, Object> response = new HashMap<>();
            
            response.put("status", health.getStatus().getCode());
            response.put("timestamp", System.currentTimeMillis());
            response.put("metrics", health.getDetails());
            
            log.debug("API metrics requested and provided");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting API metrics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get API metrics: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get API health summary.
     * 
     * @return summary of API health status
     */
    @GetMapping("/api/summary")
    public ResponseEntity<Map<String, Object>> getApiSummary() {
        try {
            Map<String, Object> summary = apiHealthIndicator.getApiHealthSummary();
            
            log.debug("API summary requested and provided");
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Error getting API summary", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get API summary: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get overall system health dashboard.
     * 
     * @return comprehensive health dashboard combining all metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getHealthDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();
            
            // Resource metrics
            Map<String, Object> resourceSummary = resourceMonitoringService.getResourceSummary();
            dashboard.put("resources", resourceSummary);
            
            // API metrics
            Map<String, Object> apiSummary = apiHealthIndicator.getApiHealthSummary();
            dashboard.put("api", apiSummary);
            
            // Overall health determination
            String overallStatus = determineOverallHealth(resourceSummary, apiSummary);
            dashboard.put("overallStatus", overallStatus);
            dashboard.put("timestamp", System.currentTimeMillis());
            
            // Health checks
            dashboard.put("healthChecks", Map.of(
                "resourceHealth", resourceSummary.get("status"),
                "apiHealth", apiSummary.get("status"),
                "overallHealth", overallStatus
            ));
            
            // Recommendations
            dashboard.put("recommendations", generateRecommendations(resourceSummary, apiSummary));
            
            log.debug("Health dashboard requested and provided");
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            log.error("Error generating health dashboard", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate health dashboard: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get specific metric type.
     * 
     * @param metricType the type of metric to retrieve (memory, cpu, threads, system)
     * @return specific metric data
     */
    @GetMapping("/resources/{metricType}")
    public ResponseEntity<Map<String, Object>> getSpecificMetric(
            @PathVariable String metricType,
            @RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
        
        try {
            Health health = resourceMonitoringService.health();
            Map<String, Object> details = health.getDetails();
            
            if (!details.containsKey(metricType)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Metric type '" + metricType + "' not found");
                errorResponse.put("availableMetrics", details.keySet());
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("metricType", metricType);
            response.put("status", health.getStatus().getCode());
            response.put("timestamp", System.currentTimeMillis());
            
            Object metricData = details.get(metricType);
            if (detailed) {
                response.put("data", metricData);
            } else {
                response.put("summary", summarizeMetric(metricType, metricData));
            }
            
            log.debug("Specific metric {} requested and provided", metricType);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting specific metric: {}", metricType, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get metric '" + metricType + "': " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Determines overall system health status.
     * 
     * @param resourceSummary resource health summary
     * @param apiSummary API health summary
     * @return overall health status
     */
    private String determineOverallHealth(Map<String, Object> resourceSummary, Map<String, Object> apiSummary) {
        String resourceStatus = (String) resourceSummary.getOrDefault("status", "UNKNOWN");
        String apiStatus = (String) apiSummary.getOrDefault("status", "UNKNOWN");
        
        // If either is DOWN, overall is DOWN
        if ("DOWN".equals(resourceStatus) || "DOWN".equals(apiStatus)) {
            return "DOWN";
        }
        
        // If either is OUT_OF_SERVICE, overall is OUT_OF_SERVICE
        if ("OUT_OF_SERVICE".equals(resourceStatus) || "OUT_OF_SERVICE".equals(apiStatus)) {
            return "OUT_OF_SERVICE";
        }
        
        // Both must be UP for overall UP
        if ("UP".equals(resourceStatus) && "UP".equals(apiStatus)) {
            return "UP";
        }
        
        return "UNKNOWN";
    }

    /**
     * Generates recommendations based on current metrics.
     * 
     * @param resourceSummary resource health summary
     * @param apiSummary API health summary
     * @return list of recommendations
     */
    private Map<String, Object> generateRecommendations(Map<String, Object> resourceSummary, Map<String, Object> apiSummary) {
        Map<String, Object> recommendations = new HashMap<>();
        java.util.List<String> alerts = new java.util.ArrayList<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        
        // Resource-based recommendations
        if (resourceSummary.containsKey("memoryUsage")) {
            String memoryUsage = (String) resourceSummary.get("memoryUsage");
            double memoryPercent = Double.parseDouble(memoryUsage.replace("%", ""));
            
            if (memoryPercent > 90) {
                alerts.add("Critical: Memory usage above 90%. Consider scaling or memory optimization.");
            } else if (memoryPercent > 80) {
                warnings.add("Warning: Memory usage above 80%. Monitor closely.");
            } else if (memoryPercent > 70) {
                suggestions.add("Memory usage is elevated. Consider monitoring trends.");
            }
        }
        
        if (resourceSummary.containsKey("cpuUsage")) {
            String cpuUsage = (String) resourceSummary.get("cpuUsage");
            double cpuPercent = Double.parseDouble(cpuUsage.replace("%", ""));
            
            if (cpuPercent > 90) {
                alerts.add("Critical: CPU usage above 90%. Immediate attention required.");
            } else if (cpuPercent > 80) {
                warnings.add("Warning: CPU usage above 80%. Investigate CPU-intensive processes.");
            }
        }
        
        // API-based recommendations
        if (apiSummary.containsKey("responseTimeMs")) {
            Long responseTime = (Long) apiSummary.get("responseTimeMs");
            if (responseTime > 10000) {
                warnings.add("API response time is slow (>10s). Check network and OpenProject performance.");
            } else if (responseTime > 5000) {
                suggestions.add("API response time is elevated. Monitor for performance degradation.");
            }
        }
        
        if (Boolean.TRUE.equals(apiSummary.get("rateLimited"))) {
            warnings.add("API rate limiting detected. Review API usage patterns.");
        }
        
        recommendations.put("alerts", alerts);
        recommendations.put("warnings", warnings);
        recommendations.put("suggestions", suggestions);
        recommendations.put("totalIssues", alerts.size() + warnings.size());
        
        return recommendations;
    }

    /**
     * Summarizes metric data for simple view.
     * 
     * @param metricType the type of metric
     * @param metricData the full metric data
     * @return summarized metric data
     */
    private Map<String, Object> summarizeMetric(String metricType, Object metricData) {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) metricData;
            
            switch (metricType) {
                case "memory":
                    summary.put("heapUsage", data.get("heapUsagePercent"));
                    summary.put("jvmUsage", data.get("jvmUsagePercent"));
                    summary.put("freeMemory", data.get("freeMemory"));
                    break;
                    
                case "cpu":
                    summary.put("processCpuUsage", data.get("processCpuUsage"));
                    summary.put("systemLoadAverage", data.get("systemLoadAverage"));
                    summary.put("loadStatus", data.get("systemLoadStatus"));
                    break;
                    
                case "threads":
                    summary.put("liveThreads", data.get("liveThreads"));
                    summary.put("peakThreads", data.get("peakThreads"));
                    summary.put("threadStatus", data.get("threadStatus"));
                    break;
                    
                case "system":
                    summary.put("availableProcessors", data.get("availableProcessors"));
                    summary.put("osName", data.get("osName"));
                    summary.put("uptime", data.get("uptime"));
                    break;
                    
                default:
                    summary.put("data", data);
            }
            
        } catch (Exception e) {
            log.warn("Error summarizing metric: {}", metricType, e);
            summary.put("error", "Failed to summarize metric data");
            summary.put("rawData", metricData);
        }
        
        return summary;
    }
}