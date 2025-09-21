package com.example.mcp.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenProject Health Indicator that monitors connectivity and service health
 * for OpenProject API integration as part of the reliability requirements (NFR3).
 * 
 * <p>This health indicator provides comprehensive monitoring of OpenProject connectivity,
 * including API availability, authentication status, response times, and service reliability.
 * It integrates with Spring Boot Actuator to expose health metrics through standard endpoints.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Real-time connectivity testing to OpenProject API</li>
 *   <li>Authentication and authorization validation</li>
 *   <li>Response time monitoring and performance metrics</li>
 *   <li>Graceful degradation during service interruptions</li>
 *   <li>Detailed health status logging for troubleshooting</li>
 *   <li>Integration with existing OpenProject client infrastructure</li>
 * </ul>
 * 
 * <p>This implementation addresses the following requirements:</p>
 * <ul>
 *   <li><strong>Success Criteria</strong> - Health monitoring functionality</li>
 *   <li><strong>NFR3: Reliability</strong> - Graceful handling of API failures</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Slf4j
@Component
public class OpenProjectHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    
    @Value("${openproject.base-url}")
    private String baseUrl;
    
    @Value("${openproject.api-key}")
    private String apiKey;
    
    @Value("${openproject.health.timeout:10000}")
    private int healthTimeout;
    
    @Value("${openproject.health.max-response-time:5000}")
    private int maxResponseTime;
    
    @Value("${openproject.health.retries:2}")
    private int maxRetries;

    public OpenProjectHealthIndicator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            Instant startTime = Instant.now();
            
            // Test basic API connectivity
            boolean connectivityHealthy = testBasicConnectivity(details);
            
            // Test authentication
            boolean authenticationHealthy = testAuthentication(details);
            
            // Test core API functionality
            boolean functionalityHealthy = testCoreFunctionality(details);
            
            // Test performance metrics
            boolean performanceHealthy = testPerformanceMetrics(details);
            
            // Calculate total response time
            long totalResponseTime = Duration.between(startTime, Instant.now()).toMillis();
            details.put("totalResponseTime", totalResponseTime + "ms");
            details.put("totalResponseTimeMs", totalResponseTime);
            
            // Determine overall health status
            Status status = determineOverallHealth(connectivityHealthy, authenticationHealthy, 
                                                  functionalityHealthy, performanceHealthy, totalResponseTime);
            
            // Add service metadata
            addServiceMetadata(details);
            
            return Health.status(status)
                    .withDetails(details)
                    .build();
                    
        } catch (Exception e) {
            log.error("Unexpected error during OpenProject health check", e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", "Health check failed: " + e.getMessage());
            errorDetails.put("service", "OpenProject API");
            errorDetails.put("baseUrl", baseUrl);
            errorDetails.put("timestamp", System.currentTimeMillis());
            errorDetails.put("exception", e.getClass().getSimpleName());
            errorDetails.put("exceptionMessage", e.getMessage());
            
            return Health.down()
                    .withException(e)
                    .withDetails(errorDetails)
                    .build();
        }
    }

    /**
     * Tests basic connectivity to the OpenProject API endpoint.
     * 
     * @param details the map to add connectivity details to
     * @return true if basic connectivity is healthy
     */
    private boolean testBasicConnectivity(Map<String, Object> details) {
        try {
            String testUrl = baseUrl + "/api/v3";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "OpenProject-MCP-Health-Check/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Instant requestStart = Instant.now();
            ResponseEntity<String> response = restTemplate.exchange(
                testUrl, HttpMethod.GET, entity, String.class);
            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
            
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            
            details.put("connectivity", isHealthy ? "HEALTHY" : "UNHEALTHY");
            details.put("connectivityResponseTime", responseTime + "ms");
            details.put("connectivityResponseTimeMs", responseTime);
            details.put("connectivityStatusCode", response.getStatusCode().value());
            details.put("apiVersion", response.getHeaders().getFirst("X-OpenProject-Version"));
            
            if (isHealthy) {
                log.debug("OpenProject connectivity test successful - Status: {}, Response time: {}ms", 
                         response.getStatusCode(), responseTime);
            } else {
                log.warn("OpenProject connectivity test failed - Status: {}", response.getStatusCode());
            }
            
            return isHealthy;
            
        } catch (ResourceAccessException e) {
            log.warn("OpenProject connectivity test failed - Connection error: {}", e.getMessage());
            details.put("connectivity", "UNREACHABLE");
            details.put("connectivityError", "Connection failed: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("OpenProject connectivity test failed", e);
            details.put("connectivity", "UNHEALTHY");
            details.put("connectivityError", e.getMessage());
            return false;
        }
    }

    /**
     * Tests authentication with the OpenProject API.
     * 
     * @param details the map to add authentication details to
     * @return true if authentication is healthy
     */
    private boolean testAuthentication(Map<String, Object> details) {
        try {
            String testUrl = baseUrl + "/api/v3/projects";
            
            HttpHeaders headers = new HttpHeaders();
            String authHeader = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes());
            headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "OpenProject-MCP-Health-Check/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Instant requestStart = Instant.now();
            ResponseEntity<String> response = restTemplate.exchange(
                testUrl, HttpMethod.GET, entity, String.class);
            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
            
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            
            details.put("authentication", isHealthy ? "HEALTHY" : "UNHEALTHY");
            details.put("authenticationResponseTime", responseTime + "ms");
            details.put("authenticationResponseTimeMs", responseTime);
            details.put("authenticationStatusCode", response.getStatusCode().value());
            
            // Check API permissions
            if (isHealthy) {
                String responseBody = response.getBody();
                if (responseBody != null && responseBody.contains("_embedded")) {
                    details.put("projectAccess", "GRANTED");
                    details.put("apiPermissions", "VALID");
                    details.put("accessibleProjects", "YES");
                } else {
                    details.put("projectAccess", "RESTRICTED");
                    details.put("apiPermissions", "LIMITED");
                    details.put("accessibleProjects", "NO");
                }
            } else {
                details.put("projectAccess", "DENIED");
                details.put("apiPermissions", "INVALID");
                details.put("accessibleProjects", "NO");
            }
            
            if (isHealthy) {
                log.debug("OpenProject authentication test successful - Status: {}, Response time: {}ms", 
                         response.getStatusCode(), responseTime);
            } else {
                log.warn("OpenProject authentication test failed - Status: {}", response.getStatusCode());
            }
            
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("OpenProject authentication test failed", e);
            details.put("authentication", "UNHEALTHY");
            details.put("authenticationError", e.getMessage());
            details.put("projectAccess", "DENIED");
            details.put("apiPermissions", "INVALID");
            details.put("accessibleProjects", "NO");
            return false;
        }
    }

    /**
     * Tests core API functionality through essential endpoints.
     * 
     * @param details the map to add functionality details to
     * @return true if core functionality is healthy
     */
    private boolean testCoreFunctionality(Map<String, Object> details) {
        try {
            Map<String, Object> endpointStatus = new HashMap<>();
            boolean allCriticalHealthy = true;
            
            // Test critical endpoints
            boolean projectsHealthy = testEndpoint("/api/v3/projects?pageSize=1", "projects", endpointStatus);
            allCriticalHealthy = allCriticalHealthy && projectsHealthy;
            
            boolean workPackagesHealthy = testEndpoint("/api/v3/work_packages?pageSize=1", "workPackages", endpointStatus);
            allCriticalHealthy = allCriticalHealthy && workPackagesHealthy;
            
            // Test schema endpoint (less critical but important)
            boolean schemaHealthy = testEndpoint("/api/v3/schema", "schema", endpointStatus);
            // Don't fail overall health if schema is down but critical endpoints work
            if (!schemaHealthy) {
                log.debug("Schema endpoint unavailable, but critical endpoints are functional");
            }
            
            // Test users endpoint (may be restricted by permissions)
            boolean usersHealthy = testEndpoint("/api/v3/users?pageSize=1", "users", endpointStatus);
            // Don't fail overall health if users endpoint is restricted
            if (!usersHealthy) {
                log.debug("Users endpoint restricted (expected in some configurations)");
            }
            
            details.put("endpoints", endpointStatus);
            details.put("criticalEndpointsHealthy", allCriticalHealthy);
            details.put("totalEndpointsTested", endpointStatus.size());
            
            // Count healthy endpoints
            long healthyCount = endpointStatus.values().stream()
                .map(endpoint -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> endpointData = (Map<String, Object>) endpoint;
                    return endpointData;
                })
                .filter(endpoint -> Boolean.TRUE.equals(endpoint.get("healthy")))
                .count();
            details.put("healthyEndpointsCount", healthyCount);
            
            return allCriticalHealthy;
            
        } catch (Exception e) {
            log.warn("OpenProject core functionality test failed", e);
            details.put("functionalityError", e.getMessage());
            details.put("criticalEndpointsHealthy", false);
            return false;
        }
    }

    /**
     * Tests a specific API endpoint for health.
     * 
     * @param endpoint the API endpoint path
     * @param name the name of the endpoint
     * @param endpointStatus the map to add endpoint status to
     * @return true if the endpoint is healthy
     */
    private boolean testEndpoint(String endpoint, String name, Map<String, Object> endpointStatus) {
        try {
            String url = baseUrl + endpoint;
            
            HttpHeaders headers = new HttpHeaders();
            String authHeader = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes());
            headers.set("Authorization", authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "OpenProject-MCP-Health-Check/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Instant requestStart = Instant.now();
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
            
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            boolean isRateLimited = response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
            
            Map<String, Object> status = new HashMap<>();
            status.put("status", isHealthy ? "HEALTHY" : (isRateLimited ? "RATE_LIMITED" : "UNHEALTHY"));
            status.put("statusCode", response.getStatusCode().value());
            status.put("responseTime", responseTime + "ms");
            status.put("responseTimeMs", responseTime);
            status.put("healthy", isHealthy);
            status.put("rateLimited", isRateLimited);
            
            // Handle rate limiting information
            if (isRateLimited) {
                String retryAfter = response.getHeaders().getFirst("Retry-After");
                if (retryAfter != null) {
                    status.put("retryAfter", retryAfter);
                }
                String rateLimitRemaining = response.getHeaders().getFirst("X-RateLimit-Remaining");
                if (rateLimitRemaining != null) {
                    status.put("rateLimitRemaining", rateLimitRemaining);
                }
            }
            
            endpointStatus.put(name, status);
            
            if (isHealthy) {
                log.debug("Endpoint {} test successful - Status: {}, Response time: {}ms", 
                         name, response.getStatusCode(), responseTime);
            } else if (isRateLimited) {
                log.warn("Endpoint {} rate limited - Status: {}", name, response.getStatusCode());
            } else {
                log.warn("Endpoint {} test failed - Status: {}", name, response.getStatusCode());
            }
            
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("Endpoint {} test failed", name, e);
            Map<String, Object> status = new HashMap<>();
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
            status.put("healthy", false);
            status.put("rateLimited", false);
            endpointStatus.put(name, status);
            return false;
        }
    }

    /**
     * Tests performance metrics against defined thresholds.
     * 
     * @param details the map to add performance details to
     * @return true if performance is within acceptable limits
     */
    private boolean testPerformanceMetrics(Map<String, Object> details) {
        try {
            // Test API response time for a lightweight request
            String testUrl = baseUrl + "/api/v3";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "OpenProject-MCP-Health-Check/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Instant requestStart = Instant.now();
            restTemplate.exchange(
                testUrl, HttpMethod.GET, entity, String.class);
            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
            
            boolean isPerformanceHealthy = responseTime <= maxResponseTime;
            
            details.put("performance", isPerformanceHealthy ? "HEALTHY" : "SLOW");
            details.put("performanceResponseTime", responseTime + "ms");
            details.put("performanceResponseTimeMs", responseTime);
            details.put("performanceThreshold", maxResponseTime + "ms");
            
            if (!isPerformanceHealthy) {
                log.warn("OpenProject API performance degraded - Response time: {}ms (threshold: {}ms)", 
                         responseTime, maxResponseTime);
            } else {
                log.debug("OpenProject API performance acceptable - Response time: {}ms", responseTime);
            }
            
            return isPerformanceHealthy;
            
        } catch (Exception e) {
            log.warn("OpenProject performance test failed", e);
            details.put("performance", "UNHEALTHY");
            details.put("performanceError", e.getMessage());
            return false;
        }
    }

    /**
     * Determines the overall health status based on all test results.
     * 
     * @param connectivityHealthy connectivity test result
     * @param authenticationHealthy authentication test result
     * @param functionalityHealthy functionality test result
     * @param performanceHealthy performance test result
     * @param totalResponseTime total response time
     * @return the determined health status
     */
    private Status determineOverallHealth(boolean connectivityHealthy, boolean authenticationHealthy,
                                          boolean functionalityHealthy, boolean performanceHealthy,
                                          long totalResponseTime) {
        
        // If basic connectivity fails, service is DOWN
        if (!connectivityHealthy) {
            return Status.DOWN;
        }
        
        // If authentication fails, service is DOWN
        if (!authenticationHealthy) {
            return Status.DOWN;
        }
        
        // If core functionality fails, service is OUT_OF_SERVICE
        if (!functionalityHealthy) {
            return Status.OUT_OF_SERVICE;
        }
        
        // Check for performance degradation
        if (!performanceHealthy || totalResponseTime > healthTimeout) {
            log.warn("OpenProject API performance degradation detected - Total time: {}ms", totalResponseTime);
            return Status.OUT_OF_SERVICE;
        }
        
        // All checks passed - service is UP
        return Status.UP;
    }

    /**
     * Adds service metadata to the health details.
     * 
     * @param details the map to add metadata to
     */
    private void addServiceMetadata(Map<String, Object> details) {
        details.put("service", "OpenProject API");
        details.put("version", "1.0");
        details.put("baseUrl", baseUrl);
        details.put("timestamp", System.currentTimeMillis());
        details.put("healthCheckTimeout", healthTimeout + "ms");
        details.put("maxResponseTimeThreshold", maxResponseTime + "ms");
        details.put("maxRetries", maxRetries);
    }

    /**
     * Gets a simplified health summary for quick status checks.
     * 
     * @return map with essential health information
     */
    public Map<String, Object> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            Health health = health();
            Map<String, Object> details = health.getDetails();
            
            summary.put("status", health.getStatus().getCode());
            summary.put("service", "OpenProject API");
            summary.put("timestamp", System.currentTimeMillis());
            summary.put("baseUrl", baseUrl);
            
            // Extract key health indicators
            if (details.containsKey("connectivity")) {
                summary.put("connectivity", details.get("connectivity"));
            }
            
            if (details.containsKey("authentication")) {
                summary.put("authentication", details.get("authentication"));
            }
            
            if (details.containsKey("projectAccess")) {
                summary.put("projectAccess", details.get("projectAccess"));
            }
            
            if (details.containsKey("totalResponseTimeMs")) {
                summary.put("responseTimeMs", details.get("totalResponseTimeMs"));
            }
            
            if (details.containsKey("criticalEndpointsHealthy")) {
                summary.put("endpointsHealthy", details.get("criticalEndpointsHealthy"));
            }
            
            // Check for any rate limiting
            if (details.containsKey("endpoints")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> endpoints = (Map<String, Object>) details.get("endpoints");
                boolean rateLimited = endpoints.values().stream()
                    .anyMatch(endpoint -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> endpointData = (Map<String, Object>) endpoint;
                        return Boolean.TRUE.equals(endpointData.get("rateLimited"));
                    });
                summary.put("rateLimited", rateLimited);
            }
            
        } catch (Exception e) {
            log.error("Error generating health summary", e);
            summary.put("status", Status.DOWN.getCode());
            summary.put("error", "Failed to generate health summary: " + e.getMessage());
        }
        
        return summary;
    }
}