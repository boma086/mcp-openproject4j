package com.example.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration Class
 * 安全配置类
 * 
 * This configuration class sets up security for the MCP server including:
 * - API key authentication for OpenProject integration
 * - Basic authentication for MCP server access
 * - CSRF protection and security headers
 * - CORS configuration for cross-origin requests
 * - Session management and authentication providers
 * 
 * 此配置类设置MCP服务器的安全配置，包括：
 * - OpenProject集成的API密钥认证
 * - MCP服务器访问的基本认证
 * - CSRF保护和安全头
 * - 跨域请求的CORS配置
 * - 会话管理和认证提供者
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${mcp.username:admin}")
    private String mcpUsername;

    @Value("${mcp.password:admin}")
    private String mcpPassword;

    @Value("${openproject.api-key:}")
    private String openProjectApiKey;

    @Value("${spring.security.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${spring.security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${spring.security.cors.allowed-headers:*}")
    private String allowedHeaders;

    /**
     * Configure security filter chain
     * 配置安全过滤器链
     * 
     * @param http HttpSecurity builder
     * @return SecurityFilterChain instance
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security filter chain | 配置安全过滤器链");

        http
            // Disable CSRF for API endpoints
            // 为API端点禁用CSRF
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/mcp/**",
                    "/actuator/**",
                    "/health/**"
                )
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )

            // Configure CORS
            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configure authorization rules
            // 配置授权规则
            .authorizeHttpRequests(authz -> authz
                // Public health endpoints
                // 公共健康检查端点
                .requestMatchers("/health/**", "/actuator/health").permitAll()
                
                // Public actuator endpoints (read-only)
                // 公共执行器端点（只读）
                .requestMatchers("/actuator/info", "/actuator/metrics").permitAll()
                
                // MCP server endpoints require authentication
                // MCP服务器端点需要认证
                .requestMatchers("/api/mcp/**").authenticated()
                
                // Protected actuator endpoints
                // 受保护的执行器端点
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // All other requests require authentication
                // 所有其他请求需要认证
                .anyRequest().authenticated()
            )

            // Configure HTTP Basic authentication
            // 配置HTTP基本认证
            .httpBasic(basic -> basic
                .realmName("OpenProject MCP Server")
            )

            // Configure session management
            // 配置会话管理
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired")
            )

            // Configure security headers
            // 配置安全头
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'")
                )
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                .frameOptions(frame -> frame.sameOrigin())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    .preload(true)
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=(), payment=()")
                )
            );

        logger.info("Security filter chain configured successfully | 安全过滤器链配置成功");
        return http.build();
    }

    /**
     * Configure CORS settings
     * 配置CORS设置
     * 
     * @return CorsConfigurationSource instance
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS settings | 配置CORS设置");

        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from configuration
        // 从配置解析允许的源
        if ("*".equals(allowedOrigins)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }
        
        // Parse allowed methods from configuration
        // 从配置解析允许的方法
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        
        // Parse allowed headers from configuration
        // 从配置解析允许的头
        configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        
        // Allow credentials
        // 允许凭证
        configuration.setAllowCredentials(true);
        
        // Expose headers
        // 暴露头
        configuration.setExposedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With"));
        
        // Max age
        // 最大年龄
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        logger.info("CORS settings configured successfully | CORS设置配置成功");
        return source;
    }

    /**
     * Configure authentication manager
     * 配置认证管理器
     * 
     * @param authConfig Authentication configuration
     * @return AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        logger.info("Configuring authentication manager | 配置认证管理器");
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configure password encoder
     * 配置密码编码器
     * 
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("Configuring password encoder | 配置密码编码器");
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure in-memory user details service
     * 配置内存用户详情服务
     * 
     * @return UserDetailsService instance
     */
    @Bean
    public UserDetailsService userDetailsService() {
        logger.info("Configuring user details service | 配置用户详情服务");

        UserDetails admin = User.builder()
            .username(mcpUsername)
            .password(passwordEncoder().encode(mcpPassword))
            .roles("ADMIN", "USER")
            .authorities("ROLE_ADMIN", "ROLE_USER", "MCP_ACCESS")
            .build();

        logger.info("User details service configured for user: {} | 为用户配置用户详情服务: {}", mcpUsername);
        return new InMemoryUserDetailsManager(admin);
    }

    /**
     * Validate OpenProject API key
     * 验证OpenProject API密钥
     * 
     * @param apiKey API key to validate
     * @return true if valid, false otherwise
     */
    public boolean validateOpenProjectApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("OpenProject API key is null or empty | OpenProject API密钥为空");
            return false;
        }

        // Use the same validation logic as in McpServerConfig
        // 使用与McpServerConfig相同的验证逻辑
        boolean isValid = McpServerConfig.validateApiKey(apiKey);
        
        if (isValid) {
            logger.debug("OpenProject API key validation successful | OpenProject API密钥验证成功");
        } else {
            logger.warn("OpenProject API key validation failed | OpenProject API密钥验证失败");
        }

        return isValid;
    }

    /**
     * Get security configuration for logging and monitoring
     * 获取安全配置用于日志记录和监控
     * 
     * @return String representation of security settings
     */
    public String getSecurityConfig() {
        return String.format(
            "Security Configuration | 安全配置:\n" +
            "- Username: %s\n" +
            "- Password Set: %s\n" +
            "- OpenProject API Key Set: %s\n" +
            "- Allowed Origins: %s\n" +
            "- Allowed Methods: %s\n" +
            "- Allowed Headers: %s",
            mcpUsername,
            mcpPassword != null && !mcpPassword.isEmpty(),
            openProjectApiKey != null && !openProjectApiKey.isEmpty(),
            allowedOrigins,
            allowedMethods,
            allowedHeaders
        );
    }

    /**
     * Validate security configuration
     * 验证安全配置
     * 
     * @return true if configuration is valid, false otherwise
     */
    public boolean validateSecurityConfiguration() {
        logger.info("Validating security configuration | 验证安全配置");

        if (mcpUsername == null || mcpUsername.trim().isEmpty()) {
            logger.error("MCP username is not configured | MCP用户名未配置");
            return false;
        }

        if (mcpPassword == null || mcpPassword.trim().isEmpty()) {
            logger.error("MCP password is not configured | MCP密码未配置");
            return false;
        }

        if (openProjectApiKey != null && !openProjectApiKey.trim().isEmpty()) {
            if (!validateOpenProjectApiKey(openProjectApiKey)) {
                logger.error("OpenProject API key is invalid | OpenProject API密钥无效");
                return false;
            }
        } else {
            logger.warn("OpenProject API key is not configured - API calls will fail | OpenProject API密钥未配置 - API调用将失败");
        }

        logger.info("Security configuration validation passed | 安全配置验证通过");
        return true;
    }
}