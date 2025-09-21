package com.example.mcp.client;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for OpenProject API client.
 * OpenProject API客户端的配置类。
 * 
 * <p>This class configures the RestTemplate with appropriate HTTP client settings,
 * connection pooling, timeouts, and other performance optimizations for communicating
 * with the OpenProject API.</p>
 * 
 * @since 1.0
 * @author OpenProject MCP Team
 */
@Configuration
public class OpenProjectClientConfig {

    @Value("${openproject.connection.max-connections:10}")
    private int maxConnections;

    @Value("${openproject.connection.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${openproject.connection.read-timeout:10000}")
    private int readTimeout;

    @Value("${openproject.timeout:30000}")
    private int timeout;

    /**
     * Configures and provides a RestTemplate bean with optimized HTTP client settings.
     * 配置并提供具有优化HTTP客户端设置的RestTemplate bean。
     * 
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(httpComponentsClientHttpRequestFactory());
    }

    /**
     * Creates and configures an HTTP client with connection pooling and timeout settings.
     * 创建并配置具有连接池和超时设置的HTTP客户端。
     * 
     * @return Configured HttpComponentsClientHttpRequestFactory
     */
    private HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(createHttpClient());
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    /**
     * Creates a configured HTTP client with connection pooling.
     * 创建具有连接池的配置HTTP客户端。
     * 
     * @return Configured CloseableHttpClient
     */
    private CloseableHttpClient createHttpClient() {
        try {
            // Create SSL context that accepts self-signed certificates (for development)
            // 创建接受自签名证书的SSL上下文（用于开发）
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(new TrustAllStrategy())
                    .build();

            // Create socket factory registry
            // 创建套接字工厂注册表
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                    .build();

            // Create connection manager with pooling
            // 创建具有池化功能的连接管理器
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connectionManager.setMaxTotal(maxConnections);
            connectionManager.setDefaultMaxPerRoute(maxConnections);

            // Configure request timeouts
            // 配置请求超时
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                    .setConnectionRequestTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                    .setResponseTimeout(readTimeout, TimeUnit.MILLISECONDS)
                    .build();

            // Build the HTTP client
            // 构建HTTP客户端
            return HttpClientBuilder.create()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .evictExpiredConnections() // Evict expired connections
                    .disableAutomaticRetries() // We handle retries at the application level
                    .build();

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("Failed to create HTTP client", e);
        }
    }
}