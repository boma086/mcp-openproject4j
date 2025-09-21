package com.example.mcp.client;

import com.example.mcp.exception.OpenProjectApiException;
import com.example.mcp.exception.RateLimitExceededException;
import com.example.mcp.model.WorkPackage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenProject API Client for HTTP communication with OpenProject REST API v3.
 * OpenProject REST API v3 HTTP通信的客户端
 * 
 * <p>This client provides comprehensive functionality for interacting with the OpenProject API,
 * including authentication, work package retrieval, filtering, pagination, and error handling.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Basic Auth authentication with API key</li>
 *   <li>HATEOAS response handling</li>
 *   <li>Work package filtering and pagination</li>
 *   <li>Rate limit handling with retry logic</li>
 *   <li>Graceful error handling with custom exceptions</li>
 *   <li>Circuit breaker for resilience</li>
 * </ul>
 * 
 * <p>This implementation addresses the following requirements:</p>
 * <ul>
 *   <li><strong>FR2: OpenProject API Integration</strong> - Basic Auth, HATEOAS, filtering/pagination, rate limit handling</li>
 *   <li><strong>NFR3: Reliability</strong> - Graceful error handling, retry logic for transient failures</li>
 * </ul>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Service
@EnableConfigurationProperties
public class OpenProjectApiClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenProjectApiClient.class);
    
    private static final String API_V3_BASE = "/api/v3";
    private static final String WORK_PACKAGES_ENDPOINT = "/work_packages";
    private static final String PROJECTS_ENDPOINT = "/projects";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    
    @Value("${openproject.base-url}")
    private String baseUrl;
    
    @Value("${openproject.api-key}")
    private String apiKey;
    
      
    @Value("${openproject.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;
    
    // Legacy rate limiting tracking (deprecated - use RateLimiter instead)
    private final Map<String, Instant> requestTimestamps = new ConcurrentHashMap<>();
    private Instant rateLimitResetTime;
    private int remainingRequests = requestsPerMinute;
    
    @Autowired
    public OpenProjectApiClient(RestTemplate restTemplate, ObjectMapper objectMapper, RateLimiter rateLimiter) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        this.rateLimitResetTime = Instant.now().plus(1, ChronoUnit.MINUTES);
    }
    
    /**
     * Retrieves work packages from OpenProject with optional filtering and pagination.
     * 从OpenProject检索工作包，支持可选的过滤和分页。
     * 
     * @param filters Map of filter criteria (e.g., status, assignee, project)
     * @param pageSize Number of items per page
     * @param offset Page offset for pagination
     * @return List of WorkPackage objects
     * @throws OpenProjectApiException if API communication fails
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    @CircuitBreaker(name = "openProjectApi", fallbackMethod = "getWorkPackagesFallback")
    @Retry(name = "openProjectApi")
    public List<WorkPackage> getWorkPackages(Map<String, String> filters, int pageSize, int offset) 
            throws OpenProjectApiException, RateLimitExceededException {
        
        checkRateLimit();
        
        try {
            String url = buildWorkPackagesUrl(filters, pageSize, offset);
            logger.debug("Fetching work packages from: {}", url);
            
            ResponseEntity<String> response = executeGetRequest(url);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                handleErrorResponse(HttpStatus.resolve(response.getStatusCode().value()), response.getBody());
            }
            
            return parseWorkPackagesResponse(response.getBody());
            
        } catch (HttpClientErrorException e) {
            handleHttpClientError(e);
        } catch (HttpServerErrorException e) {
            handleHttpServerError(e);
        } catch (ResourceAccessException e) {
            handleConnectionError(e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching work packages", e);
            throw new OpenProjectApiException("Unexpected error while fetching work packages: " + e.getMessage(), 
                                            e, 500, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Retrieves a specific work package by its ID.
     * 根据ID检索特定的工作包。
     * 
     * @param workPackageId The ID of the work package to retrieve
     * @return WorkPackage object
     * @throws OpenProjectApiException if work package is not found or API communication fails
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    @CircuitBreaker(name = "openProjectApi", fallbackMethod = "getWorkPackageByIdFallback")
    @Retry(name = "openProjectApi")
    public WorkPackage getWorkPackageById(String workPackageId) 
            throws OpenProjectApiException, RateLimitExceededException {
        
        checkRateLimit();
        
        try {
            String url = baseUrl + API_V3_BASE + WORK_PACKAGES_ENDPOINT + "/" + workPackageId;
            logger.debug("Fetching work package by ID from: {}", url);
            
            ResponseEntity<String> response = executeGetRequest(url);
            
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw OpenProjectApiException.notFound("Work package with ID " + workPackageId);
            }
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                handleErrorResponse(HttpStatus.resolve(response.getStatusCode().value()), response.getBody());
            }
            
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            return WorkPackage.fromJson(responseNode.path("_embedded").path("elements").get(0));
            
        } catch (HttpClientErrorException e) {
            handleHttpClientError(e);
        } catch (HttpServerErrorException e) {
            handleHttpServerError(e);
        } catch (ResourceAccessException e) {
            handleConnectionError(e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching work package by ID: {}", workPackageId, e);
            throw new OpenProjectApiException("Unexpected error while fetching work package: " + e.getMessage(), 
                                            e, 500, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return null;
    }
    
    /**
     * Retrieves available projects from OpenProject.
     * 从OpenProject检索可用的项目。
     * 
     * @return List of project information
     * @throws OpenProjectApiException if API communication fails
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    @CircuitBreaker(name = "openProjectApi", fallbackMethod = "getProjectsFallback")
    @Retry(name = "openProjectApi")
    public List<ProjectInfo> getProjects() throws OpenProjectApiException, RateLimitExceededException {
        
        checkRateLimit();
        
        try {
            String url = baseUrl + API_V3_BASE + PROJECTS_ENDPOINT;
            logger.debug("Fetching projects from: {}", url);
            
            ResponseEntity<String> response = executeGetRequest(url);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                handleErrorResponse(HttpStatus.resolve(response.getStatusCode().value()), response.getBody());
            }
            
            return parseProjectsResponse(response.getBody());
            
        } catch (HttpClientErrorException e) {
            handleHttpClientError(e);
        } catch (HttpServerErrorException e) {
            handleHttpServerError(e);
        } catch (ResourceAccessException e) {
            handleConnectionError(e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching projects", e);
            throw new OpenProjectApiException("Unexpected error while fetching projects: " + e.getMessage(), 
                                            e, 500, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Checks if the OpenProject API is accessible and authentication is working.
     * 检查OpenProject API是否可访问以及身份验证是否有效。
     * 
     * @return true if API is accessible and authentication is successful
     */
    public boolean isApiAccessible() {
        try {
            String url = baseUrl + API_V3_BASE + PROJECTS_ENDPOINT + "?pageSize=1";
            ResponseEntity<String> response = executeGetRequest(url);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("API accessibility check failed", e);
            return false;
        }
    }
    
    /**
     * Gets the current rate limit status.
     * 获取当前的速率限制状态。
     * 
     * @return RateLimitStatus object with current rate limit information
     * @deprecated Use {@link #getAdvancedRateLimitStatus()} for more detailed information
     */
    @Deprecated
    public RateLimitStatus getRateLimitStatus() {
        return new RateLimitStatus(
            this.remainingRequests,
            this.requestsPerMinute,
            this.rateLimitResetTime
        );
    }
    
    /**
     * Gets advanced rate limit status using the new RateLimiter.
     * 使用新的速率限制器获取高级速率限制状态。
     * 
     * @return RateLimiter.RateLimitStatus object with detailed rate limit information
     */
    public RateLimiter.RateLimitStatus getAdvancedRateLimitStatus() {
        return rateLimiter.getStatus("openproject-api");
    }
    
    /**
     * Gets rate limiting performance metrics.
     * 获取速率限制性能指标。
     * 
     * @return RateLimiter.RateLimitMetrics object with performance metrics
     */
    public RateLimiter.RateLimitMetrics getRateLimitMetrics() {
        return rateLimiter.getMetrics();
    }
    
    /**
     * Adjusts rate limits based on server response headers.
     * 根据服务器响应头调整速率限制。
     * 
     * @param remainingRequests Remaining requests from server response
     * @param resetTime Reset time from server response
     */
    public void adjustRateLimitFromResponse(Integer remainingRequests, Instant resetTime) {
        rateLimiter.adjustFromServerResponse(remainingRequests, resetTime, "openproject-api");
    }
    
    /**
     * Updates rate limiter configuration dynamically.
     * 动态更新速率限制器配置。
     * 
     * @param requestsPerMinute New requests per minute limit
     * @param burstCapacity New burst capacity
     */
    public void updateRateLimitConfiguration(int requestsPerMinute, int burstCapacity) {
        rateLimiter.updateConfiguration(requestsPerMinute, burstCapacity);
        this.requestsPerMinute = requestsPerMinute;
    }
    
    // Private helper methods
    
    private String buildWorkPackagesUrl(Map<String, String> filters, int pageSize, int offset) {
        StringBuilder url = new StringBuilder(baseUrl + API_V3_BASE + WORK_PACKAGES_ENDPOINT);
        
        // Add pagination parameters
        url.append("?pageSize=").append(pageSize);
        url.append("&offset=").append(offset);
        
        // Add filters
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                url.append("&filters[]=").append(entry.getKey())
                   .append("=").append(urlEncode(entry.getValue()));
            }
        }
        
        // Add sorting by creation date (newest first)
        url.append("&sort[]=-createdAt");
        
        return url.toString();
    }
    
    private ResponseEntity<String> executeGetRequest(String url) {
        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("apikey", apiKey);
        headers.set("User-Agent", "OpenProject-MCP-Server/1.0");
        return headers;
    }
    
    private List<WorkPackage> parseWorkPackagesResponse(String responseBody) throws Exception {
        JsonNode responseNode = objectMapper.readTree(responseBody);
        JsonNode embeddedNode = responseNode.path("_embedded").path("elements");
        
        List<WorkPackage> workPackages = new ArrayList<>();
        if (embeddedNode.isArray()) {
            for (JsonNode element : embeddedNode) {
                try {
                    WorkPackage workPackage = WorkPackage.fromJson(element);
                    if (workPackage.isValid()) {
                        workPackages.add(workPackage);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse work package: {}", e.getMessage());
                }
            }
        }
        
        return workPackages;
    }
    
    private List<ProjectInfo> parseProjectsResponse(String responseBody) throws Exception {
        JsonNode responseNode = objectMapper.readTree(responseBody);
        JsonNode embeddedNode = responseNode.path("_embedded").path("elements");
        
        List<ProjectInfo> projects = new ArrayList<>();
        if (embeddedNode.isArray()) {
            for (JsonNode element : embeddedNode) {
                try {
                    ProjectInfo project = new ProjectInfo(
                        element.path("id").asText(),
                        element.path("identifier").asText(),
                        element.path("name").asText(),
                        element.path("description").asText(),
                        element.path("createdAt").asText(),
                        element.path("updatedAt").asText()
                    );
                    projects.add(project);
                } catch (Exception e) {
                    logger.warn("Failed to parse project: {}", e.getMessage());
                }
            }
        }
        
        return projects;
    }
    
    private void checkRateLimit() throws RateLimitExceededException {
        // Use the new RateLimiter for advanced rate limiting
        boolean acquired = rateLimiter.tryAcquire("openproject-api", 1);
        
        if (!acquired) {
            // Get detailed rate limit status for better error reporting
            RateLimiter.RateLimitStatus status = rateLimiter.getStatus("openproject-api");
            throw new RateLimitExceededException(
                "OpenProject API rate limit exceeded. Please wait before retrying.",
                status.estimatedWaitTime(),
                Instant.now().plus(status.estimatedWaitTime()),
                status.availablePermits(),
                status.maxPermits()
            );
        }
        
        // Fallback to legacy rate limiting for backward compatibility
        // This will be removed in future versions
        checkRateLimitLegacy();
    }
    
    /**
     * Legacy rate limiting method for backward compatibility.
     * 向后兼容的遗留速率限制方法。
     * 
     * @deprecated Use {@link #checkRateLimit()} with RateLimiter instead
     */
    @Deprecated
    private void checkRateLimitLegacy() throws RateLimitExceededException {
        Instant now = Instant.now();
        
        // Reset rate limit window if expired
        if (now.isAfter(rateLimitResetTime)) {
            requestTimestamps.clear();
            remainingRequests = requestsPerMinute;
            rateLimitResetTime = now.plus(1, ChronoUnit.MINUTES);
        }
        
        // Check if we've exceeded the rate limit
        if (remainingRequests <= 0) {
            Duration retryAfter = Duration.between(now, rateLimitResetTime);
            throw new RateLimitExceededException(
                "Rate limit exceeded. Wait until " + rateLimitResetTime,
                retryAfter, rateLimitResetTime, 0, requestsPerMinute
            );
        }
        
        // Track this request
        remainingRequests--;
        requestTimestamps.put(UUID.randomUUID().toString(), now);
    }
    
    private void handleHttpClientError(HttpClientErrorException e) throws OpenProjectApiException {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String responseBody = e.getResponseBodyAsString();
        
        logger.warn("HTTP client error: {} - {}", status, responseBody);
        
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            handleRateLimitResponse(e);
        } else if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            throw OpenProjectApiException.forbidden("Authentication failed: " + responseBody);
        } else if (status == HttpStatus.NOT_FOUND) {
            throw OpenProjectApiException.notFound("Requested resource not found");
        } else if (status.is4xxClientError()) {
            throw new OpenProjectApiException("Client error: " + responseBody, 
                                            status.value(), status);
        }
    }
    
    private void handleHttpServerError(HttpServerErrorException e) throws OpenProjectApiException {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String responseBody = e.getResponseBodyAsString();
        
        logger.error("HTTP server error: {} - {}", status, responseBody);
        throw OpenProjectApiException.serverError("Server error: " + responseBody, status.value());
    }
    
    private void handleConnectionError(ResourceAccessException e) throws OpenProjectApiException {
        logger.error("Connection error while communicating with OpenProject API", e);
        throw new OpenProjectApiException("Connection error: Unable to connect to OpenProject API. " +
                                        "Please check the base URL and network connectivity.", 
                                        e, 503, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    private void handleErrorResponse(HttpStatus status, String responseBody) throws OpenProjectApiException {
        logger.error("API error response: {} - {}", status, responseBody);
        
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            throw RateLimitExceededException.withRetryAfter(60); // Default 60 seconds
        } else if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            throw OpenProjectApiException.forbidden("Authentication failed: " + responseBody);
        } else if (status == HttpStatus.NOT_FOUND) {
            throw OpenProjectApiException.notFound("Requested resource not found");
        } else {
            throw new OpenProjectApiException("API request failed: " + responseBody, 
                                            status.value(), status);
        }
    }
    
    private void handleRateLimitResponse(HttpClientErrorException e) throws RateLimitExceededException {
        String retryAfterHeader = e.getResponseHeaders().getFirst("Retry-After");
        String resetTimeHeader = e.getResponseHeaders().getFirst("X-RateLimit-Reset");
        String remainingHeader = e.getResponseHeaders().getFirst("X-RateLimit-Remaining");
        
        Duration retryAfter = null;
        Instant resetTime = null;
        int remaining = 0;
        
        try {
            if (retryAfterHeader != null) {
                int retryAfterSeconds = Integer.parseInt(retryAfterHeader);
                retryAfter = Duration.ofSeconds(retryAfterSeconds);
                resetTime = Instant.now().plus(retryAfter);
            }
            
            if (resetTimeHeader != null) {
                resetTime = Instant.parse(resetTimeHeader);
                if (retryAfter == null) {
                    retryAfter = Duration.between(Instant.now(), resetTime);
                }
            }
            
            if (remainingHeader != null) {
                remaining = Integer.parseInt(remainingHeader);
            }
        } catch (Exception ex) {
            logger.warn("Failed to parse rate limit headers", ex);
        }
        
        if (retryAfter != null) {
            throw RateLimitExceededException.withRetryAfter((int) retryAfter.getSeconds());
        } else {
            throw RateLimitExceededException.withDetails(remaining, requestsPerMinute, 
                                                      resetTime != null ? resetTime : Instant.now().plus(60, ChronoUnit.SECONDS));
        }
    }
    
    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    // Fallback methods for circuit breaker
    
    private List<WorkPackage> getWorkPackagesFallback(Map<String, String> filters, int pageSize, int offset, Exception e) 
            throws OpenProjectApiException {
        logger.warn("Circuit breaker fallback for getWorkPackages", e);
        throw new OpenProjectApiException("OpenProject API is currently unavailable. Please try again later.", 
                                        e, 503, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    private WorkPackage getWorkPackageByIdFallback(String workPackageId, Exception e) 
            throws OpenProjectApiException {
        logger.warn("Circuit breaker fallback for getWorkPackageById", e);
        throw new OpenProjectApiException("OpenProject API is currently unavailable. Please try again later.", 
                                        e, 503, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    private List<ProjectInfo> getProjectsFallback(Exception e) throws OpenProjectApiException {
        logger.warn("Circuit breaker fallback for getProjects", e);
        throw new OpenProjectApiException("OpenProject API is currently unavailable. Please try again later.", 
                                        e, 503, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Project information DTO for lightweight project data.
     * 轻量级项目数据的项目信息DTO。
     */
    public record ProjectInfo(
        String id,
        String identifier,
        String name,
        String description,
        String createdAt,
        String updatedAt
    ) {}
    
    /**
     * Rate limit status information.
     * 速率限制状态信息。
     */
    public record RateLimitStatus(
        int remainingRequests,
        int requestLimit,
        Instant resetTime
    ) {
        public long getSecondsUntilReset() {
            return Math.max(0, Duration.between(Instant.now(), resetTime).getSeconds());
        }
        
        public boolean isRateLimited() {
            return remainingRequests <= 0;
        }
    }
}