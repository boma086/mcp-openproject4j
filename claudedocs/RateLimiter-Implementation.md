# Rate Limiter Implementation

## Overview

This document describes the comprehensive rate limiting implementation for OpenProject API calls in the MCP server. The rate limiter uses the Token Bucket algorithm to provide smooth and efficient rate limiting while supporting multiple contexts and dynamic configuration.

## Architecture

### Core Components

1. **RateLimiter** (`/src/main/java/com/example/mcp/client/RateLimiter.java`)
   - Main rate limiting service using Token Bucket algorithm
   - Supports multiple rate limit contexts (global, per-project, per-user)
   - Thread-safe concurrent access
   - Dynamic configuration updates

2. **RateLimiterConfig** (`/src/main/java/com/example/mcp/config/RateLimiterConfig.java`)
   - Configuration properties for rate limiting
   - Spring Boot configuration integration
   - Validation support

3. **OpenProjectApiClient Integration**
   - Seamless integration with existing API client
   - Backward compatibility with legacy rate limiting
   - Enhanced rate limit status reporting

### Token Bucket Algorithm

The implementation uses the Token Bucket algorithm which provides:

- **Smooth rate limiting**: Gradual token refill prevents request bursts
- **Burst handling**: Configurable burst capacity for handling temporary spikes
- **Fairness**: All requests are treated equally within rate limits
- **Predictability**: Consistent behavior under load

## Features

### 1. Multiple Rate Limit Contexts

```java
// Global rate limiting
rateLimiter.tryAcquire("global");

// Per-project rate limiting
rateLimiter.tryAcquire("project:123");

// Per-user rate limiting
rateLimiter.tryAcquire("user:456");
```

### 2. Thread-Safe Operation

- Uses `AtomicInteger` for permit tracking
- `ReentrantLock` for configuration updates
- `ConcurrentHashMap` for context isolation

### 3. Dynamic Configuration

```java
// Update rate limits at runtime
rateLimiter.updateConfiguration(120, 15);
```

### 4. Wait Time Calculation

```java
// Estimate wait time for rate-limited requests
Duration waitTime = rateLimiter.getStatus("context").estimatedWaitTime();
```

### 5. Performance Monitoring

```java
// Get detailed metrics
RateLimitMetrics metrics = rateLimiter.getMetrics();
System.out.println("Rate limited: " + metrics.getRateLimitPercentage() + "%");
System.out.println("Average wait time: " + metrics.getAverageWaitTimeMillis() + "ms");
```

## Configuration

### Application Properties

```yaml
openproject:
  rate-limit:
    requests-per-minute: 100          # Base rate limit
    burst-capacity: 10                # Burst handling capacity
    wait-timeout-seconds: 30           # Max wait time for blocking operations
    adaptive-rate-limiting: true       # Enable adaptive rate limiting
    throttling-factor: 0.8             # Factor for rate reduction when throttled
    window-size-seconds: 60            # Time window for rate calculation
```

### Environment Variables

```bash
export OPENPROJECT_RATE_LIMIT=100
export OPENPROJECT_BURST_CAPACITY=10
export OPENPROJECT_WAIT_TIMEOUT=30
export OPENPROJECT_ADAPTIVE_RATE_LIMITING=true
export OPENPROJECT_THROTTLING_FACTOR=0.8
export OPENPROJECT_WINDOW_SIZE_SECONDS=60
```

## Usage Examples

### Basic Usage

```java
@Autowired
private RateLimiter rateLimiter;

public void makeApiCall() {
    if (rateLimiter.tryAcquire("openproject-api")) {
        // Make API call
        callOpenProjectApi();
    } else {
        // Handle rate limit exceeded
        handleRateLimitExceeded();
    }
}
```

### Blocking with Timeout

```java
public void makeApiCallWithRetry() {
    try {
        rateLimiter.acquire("openproject-api");
        callOpenProjectApi();
    } catch (RateLimitExceededException e) {
        logger.error("Rate limit exceeded", e);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

### Context-Based Rate Limiting

```java
public void makeProjectSpecificCall(String projectId) {
    String context = "project:" + projectId;
    
    if (rateLimiter.tryAcquire(context)) {
        callProjectApi(projectId);
    } else {
        logger.warn("Rate limit exceeded for project: {}", projectId);
    }
}
```

### Monitoring and Metrics

```java
public void monitorRateLimits() {
    RateLimitStatus status = rateLimiter.getStatus("openproject-api");
    RateLimitMetrics metrics = rateLimiter.getMetrics();
    
    logger.info("Rate limit status: {}/{} permits available, {}% utilized",
               status.availablePermits(),
               status.maxPermits(),
               status.getUtilizationPercentage());
    
    logger.info("Performance: {} total requests, {} rate limited ({:.1f}%), average wait: {}ms",
               metrics.totalRequests(),
               metrics.rateLimitedRequests(),
               metrics.getRateLimitPercentage(),
               metrics.getAverageWaitTimeMillis());
}
```

## Performance Characteristics

### Time Complexity

- **tryAcquire()**: O(1) - Constant time operation
- **acquire()**: O(1) for available permits, O(wait time) for blocked operations
- **getStatus()**: O(1) - Constant time status retrieval
- **updateConfiguration()**: O(n) where n is number of active contexts

### Memory Usage

- **Per Context**: ~100 bytes for TokenBucket state
- **Global**: Minimal overhead for metrics and configuration
- **Thread Safe**: No synchronization bottlenecks

### Concurrency

- **High Concurrency**: Designed for thousands of concurrent requests
- **No Lock Contention**: Uses atomic operations for permit management
- **Context Isolation**: Different contexts don't block each other

## Integration with OpenProjectApiClient

### Enhanced Rate Limit Check

```java
private void checkRateLimit() throws RateLimitExceededException {
    // Use new RateLimiter for advanced rate limiting
    boolean acquired = rateLimiter.tryAcquire("openproject-api", 1);
    
    if (!acquired) {
        // Get detailed status for better error reporting
        RateLimitStatus status = rateLimiter.getStatus("openproject-api");
        throw new RateLimitExceededException(
            "OpenProject API rate limit exceeded",
            status.estimatedWaitTime(),
            Instant.now().plus(status.estimatedWaitTime()),
            status.availablePermits(),
            status.maxPermits()
        );
    }
}
```

### Dynamic Configuration Updates

```java
public void adjustRateLimitFromResponse(Integer remaining, Instant resetTime) {
    rateLimiter.adjustFromServerResponse(remaining, resetTime, "openproject-api");
}
```

## Testing

The implementation includes comprehensive unit tests covering:

- Basic rate limiting functionality
- Context isolation
- Concurrent access patterns
- Configuration updates
- Performance under load
- Edge cases and error conditions

### Running Tests

```bash
mvn test -Dtest=RateLimiterTest
```

## Monitoring and Observability

### Metrics Exposed

1. **Rate Limit Utilization**: Percentage of capacity used
2. **Request Success Rate**: Success vs rate-limited requests
3. **Wait Time Statistics**: Average and maximum wait times
4. **Context Count**: Number of active rate limiting contexts

### Integration with Monitoring Systems

The rate limiter integrates with Spring Boot Actuator and can be monitored through:

- **Prometheus**: Custom metrics for rate limiting
- **Grafana**: Dashboards for rate limit visualization
- **Logging**: Detailed rate limit events and statistics

## Best Practices

### 1. Context Naming

```java
// Good: Specific context names
rateLimiter.tryAcquire("project:" + projectId);
rateLimiter.tryAcquire("user:" + userId);

// Avoid: Too generic or too specific
rateLimiter.tryAcquire("api"); // Too generic
rateLimiter.tryAcquire("user:123:action:456:timestamp:789"); // Too specific
```

### 2. Configuration Tuning

```java
// Start with conservative limits
int requestsPerMinute = 60;  // 1 per second
int burstCapacity = 10;     // Handle small bursts

// Monitor usage and adjust
if (metrics.getRateLimitPercentage() > 80) {
    rateLimiter.updateConfiguration(120, 20);
}
```

### 3. Error Handling

```java
try {
    rateLimiter.acquire("api-context");
    // Make API call
} catch (RateLimitExceededException e) {
    // Use exponential backoff for retries
    Duration backoff = e.getSuggestedBackoff(attemptNumber);
    Thread.sleep(backoff.toMillis());
    // Retry logic
}
```

### 4. Adaptive Rate Limiting

```java
// Enable adaptive rate limiting for better server integration
if (rateLimitExceeded) {
    int newLimit = (int) (currentLimit * 0.8); // Reduce by 20%
    rateLimiter.updateConfiguration(newLimit, burstCapacity);
}
```

## Troubleshooting

### Common Issues

1. **High Rate Limit Percentage**
   - Increase `requests-per-minute` configuration
   - Check for inefficient API usage patterns
   - Implement request batching where possible

2. **Long Wait Times**
   - Adjust `burst-capacity` to handle request spikes
   - Implement asynchronous request processing
   - Consider increasing `wait-timeout-seconds`

3. **Context Leaks**
   - Use consistent context naming patterns
   - Monitor `activeContexts` metric
   - Clean up unused contexts periodically

### Debug Logging

```yaml
logging:
  level:
    com.example.mcp.client.RateLimiter: DEBUG
```

## Future Enhancements

1. **Distributed Rate Limiting**: Redis-based rate limiting for cluster deployments
2. **Machine Learning**: Adaptive rate limiting based on usage patterns
3. **Hierarchical Rate Limiting**: Nested context relationships
4. **Circuit Breaker Integration**: Automatic rate limit adjustment during failures
5. **Real-time Dashboards**: Live monitoring and configuration management

## Conclusion

The rate limiter implementation provides a robust, scalable, and feature-rich solution for managing OpenProject API call rates. It addresses the performance requirements (NFR1) and API integration needs (FR2) while providing extensive monitoring, configuration options, and backward compatibility.

The Token Bucket algorithm ensures smooth rate limiting with burst handling, while the context-based approach provides fine-grained control over different types of API usage. The integration with existing systems maintains compatibility while offering enhanced capabilities for future development.