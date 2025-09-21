package com.example.mcp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenProject API health indicator that monitors connectivity and response times
 * to ensure the MCP server can properly communicate with OpenProject instances.
 * 
 * This indicator addresses the following requirements:
 * <ul>
 *   <li><strong>FR2: OpenProject API Integration</strong> - Health monitoring for API connectivity</li>
 *   <li><strong>NFR3: Reliability</strong> - Monitoring external service availability</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Slf4j
@Component
public class OpenProjectApiHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;
    
    @Value("${openproject.base-url}")
    private String baseUrl;
    
    @Value("${openproject.api-key}")
    private String apiKey;
    
    @Value("${openproject.timeout:30000}")
    private int timeout;
    
    @Value("${openproject.connection.max-connections:10}")
    private int maxConnections;

    public OpenProjectApiHealthIndicator() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();
            Instant startTime = Instant.now();
            
            // Test basic API connectivity
            boolean connectivityHealthy = testConnectivity(details);
            
            // Test authentication
            boolean authHealthy = testAuthentication(details);
            
            // Test API endpoints
            boolean endpointsHealthy = testApiEndpoints(details);
            
            // Calculate response time
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            details.put("responseTime", responseTime + "ms");
            details.put("responseTimeMs", responseTime);
            
            // Determine overall health status
            Status status = determineApiHealthStatus(connectivityHealthy, authHealthy, endpointsHealthy, responseTime);
            
            return Health.status(status)
                    .withDetails(details)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error checking OpenProject API health", e);
            return Health.down()
                    .withException(e)
                    .withDetail("error", "Failed to check API health: " + e.getMessage())
                    .withDetail("baseUrl", baseUrl)
                    .build();
        }
    }

    /**
     * Tests basic connectivity to the OpenProject API.
     * 
     * @param details the map to add connectivity details to
     * @return true if connectivity is healthy
     */
    private boolean testConnectivity(Map<String, Object> details) {
        try {
            String testUrl = baseUrl + "/api/v3";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Instant requestStart = Instant.now();
            ResponseEntity<String> response = restTemplate.exchange(
                testUrl, HttpMethod.GET, entity, String.class);
            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
            
            details.put("connectivity", "HEALTHY");
            details.put("connectivityResponseTime", responseTime + "ms");
            details.put("connectivityStatusCode", response.getStatusCode().value());
            details.put("apiVersion", response.getHeaders().getFirst("X-OpenProject-Version"));
            
            log.debug("OpenProject connectivity test successful - Status: {}, Response time: {}ms", 
                     response.getStatusCode(), responseTime);
            
            return response.getStatusCode().is2xxSuccessful();
            
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
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Instant requestStart = Instant.now();
            ResponseEntity<String> response = restTemplate.exchange(
                testUrl, HttpMethod.GET, entity, String.class);
            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
            
            details.put("authentication", "HEALTHY");
            details.put("authenticationResponseTime", responseTime + "ms");
            details.put("authenticationStatusCode", response.getStatusCode().value());
            
            // Check if we can access projects (basic permission check)
            String responseBody = response.getBody();
            if (responseBody != null && responseBody.contains("_embedded")) {
                details.put("projectAccess", "GRANTED");
                details.put("apiPermissions", "VALID");
            } else {
                details.put("projectAccess", "LIMITED");
                details.put("apiPermissions", "RESTRICTED");
            }
            
            log.debug("OpenProject authentication test successful - Status: {}, Response time: {}ms", 
                     response.getStatusCode(), responseTime);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.warn("OpenProject authentication test failed", e);
            details.put("authentication", "UNHEALTHY");
            details.put("authenticationError", e.getMessage());
            details.put("projectAccess", "DENIED");
            details.put("apiPermissions", "INVALID");
            return false;
        }
    }

    /**
     * Tests specific API endpoints for functionality.
     * 
     * @param details the map to add endpoint details to
     * @return true if endpoints are healthy
     */
    private boolean testApiEndpoints(Map<String, Object> details) {
        try {
            Map<String, Object> endpointStatus = new HashMap<>();
            boolean allHealthy = true;
            
            // Test work packages endpoint
            boolean workPackagesHealthy = testEndpoint("/api/v3/work_packages", "workPackages", endpointStatus);
            allHealthy = allHealthy && workPackagesHealthy;
            
            // Test projects endpoint with basic filters
            boolean projectsHealthy = testEndpoint("/api/v3/projects?pageSize=1", "projects", endpointStatus);
            allHealthy = allHealthy && projectsHealthy;
            
            // Test users endpoint (if accessible)
            boolean usersHealthy = testEndpoint("/api/v3/users?pageSize=1", "users", endpointStatus);
            // Don't fail overall health if users endpoint is restricted
            if (!usersHealthy) {
                log.debug("Users endpoint is restricted or unavailable (this is expected in some configurations)");
            }
            
            // Test API schema endpoint
            boolean schemaHealthy = testEndpoint("/api/v3/schema", "schema", endpointStatus);
            allHealthy = allHealthy && schemaHealthy;
            
            details.put("endpoints", endpointStatus);
            details.put("endpointsHealthy", allHealthy);
            
            return allHealthy;
            
        } catch (Exception e) {
            log.warn("OpenProject API endpoints test failed", e);
            details.put("endpointsError", e.getMessage());
            details.put("endpointsHealthy", false);
            return false;
        }
    }

    /**
     * Tests a specific API endpoint.
     * 
     * @param endpoint the API endpoint to test
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
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Instant requestStart = Instant.now();
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
            
            Map<String, Object> status = new HashMap<>();
            status.put("status", "HEALTHY");
            status.put("statusCode", response.getStatusCode().value());
            status.put("responseTime", responseTime + "ms");
            status.put("accessible", response.getStatusCode().is2xxSuccessful());
            
            // Check for rate limiting
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                status.put("rateLimited", true);
                String retryAfter = response.getHeaders().getFirst("Retry-After");
                if (retryAfter != null) {
                    status.put("retryAfter", retryAfter);
                }
            } else {
                status.put("rateLimited", false);
            }
            
            endpointStatus.put(name, status);
            
            log.debug("Endpoint {} test - Status: {}, Response time: {}ms", 
                     name, response.getStatusCode(), responseTime);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.warn("Endpoint {} test failed", name, e);
            Map<String, Object> status = new HashMap<>();
            status.put("status", "UNHEALTHY");
            status.put("error", e.getMessage());
            status.put("accessible", false);
            endpointStatus.put(name, status);
            return false;
        }
    }

    /**
     * Determines the overall API health status based on test results.
     * 
     * @param connectivityHealthy connectivity test result
     * @param authHealthy authentication test result
     * @param endpointsHealthy endpoints test result
     * @param responseTime total response time
     * @return the health status
     */
    private Status determineApiHealthStatus(boolean connectivityHealthy, boolean authHealthy, 
                                           boolean endpointsHealthy, long responseTime) {
        
        if (!connectivityHealthy) {
            return Status.DOWN;
        }
        
        if (!authHealthy) {
            return Status.DOWN;
        }
        
        if (!endpointsHealthy) {
            return Status.OUT_OF_SERVICE;
        }
        
        // Check response time
        if (responseTime > 10000) { // 10 seconds
            log.warn("OpenProject API response time is slow: {}ms", responseTime);
            return Status.OUT_OF_SERVICE;
        } else if (responseTime > 5000) { // 5 seconds
            log.warn("OpenProject API response time is elevated: {}ms", responseTime);
            return Status.UP; // Still healthy but slow
        }
        
        return Status.UP;
    }

    /**
     * Gets a summary of OpenProject API health status.
     * 
     * @return summary map with key metrics
     */
    public Map<String, Object> getApiHealthSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            Health health = health();
            Map<String, Object> details = health.getDetails();
            
            summary.put("status", health.getStatus().getCode());
            summary.put("baseUrl", baseUrl);
            summary.put("timestamp", System.currentTimeMillis());
            
            // Extract key information
            if (details.containsKey("connectivity")) {
                summary.put("connectivity", details.get("connectivity"));
            }
            
            if (details.containsKey("authentication")) {
                summary.put("authentication", details.get("authentication"));
            }
            
            if (details.containsKey("projectAccess")) {
                summary.put("projectAccess", details.get("projectAccess"));
            }
            
            if (details.containsKey("responseTimeMs")) {
                summary.put("responseTimeMs", details.get("responseTimeMs"));
            }
            
            if (details.containsKey("endpointsHealthy")) {
                summary.put("endpointsHealthy", details.get("endpointsHealthy"));
            }
            
            // Check for rate limiting
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
            
            // If health status is DOWN, extract error information
            if (health.getStatus() == Status.DOWN) {
                // Look for any error-related keys
                if (details.containsKey("error")) {
                    summary.put("error", details.get("error"));
                } else if (details.containsKey("connectivityError")) {
                    summary.put("error", details.get("connectivityError"));
                } else if (details.containsKey("authenticationError")) {
                    summary.put("error", details.get("authenticationError"));
                } else if (details.containsKey("endpointsError")) {
                    summary.put("error", details.get("endpointsError"));
                }
            }
            
        } catch (Exception e) {
            log.error("Error generating API health summary", e);
            summary.put("error", "Failed to generate API health summary: " + e.getMessage());
            summary.put("status", Status.DOWN.getCode());
        }
        
        return summary;
    }
}