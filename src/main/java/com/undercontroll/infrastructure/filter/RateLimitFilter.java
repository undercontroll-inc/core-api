package com.undercontroll.infrastructure.filter;

import com.undercontroll.infrastructure.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.BucketProxy;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH        = "/v1/api/users/auth";
    private static final String AUTH_GOOGLE_PATH  = "/v1/api/users/auth/google";
    private static final String AUTH_REFRESH_PATH = "/v1/api/users/auth/refresh";

    private final RateLimitProperties props;

    private CaffeineProxyManager<String> authManager;
    private CaffeineProxyManager<String> generalManager;
    private BucketConfiguration          authConfig;
    private BucketConfiguration          generalConfig;

    @PostConstruct
    void init() {
        authManager = new CaffeineProxyManager<>(
                Caffeine.newBuilder()
                        .maximumSize(10_000),
                Duration.ofSeconds(10)
        );
        generalManager = new CaffeineProxyManager<>(
                Caffeine.newBuilder()
                        .maximumSize(100_000),
                Duration.ofSeconds(10)
        );
        authConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(props.getAuthRequestsPerMinute())
                        .refillGreedy(props.getAuthRequestsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build();
        generalConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(props.getGeneralRequestsPerMinute())
                        .refillGreedy(props.getGeneralRequestsPerMinute(), Duration.ofMinutes(1))
                        .build())
                .build();
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String ip      = resolveClientIp(request);
        String path    = request.getServletPath();
        boolean isAuth = isAuthEndpoint(request.getMethod(), path);

        BucketProxy bucket = isAuth
                ? authManager.builder().build(ip, authConfig)
                : generalManager.builder().build(ip, generalConfig);

        if (bucket.tryConsume(1)) {
            long remaining = bucket.getAvailableTokens();
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} path={}", ip, path);
            sendTooManyRequests(response, isAuth);
        }
    }


    private boolean isAuthEndpoint(String method, String path) {
        return "POST".equalsIgnoreCase(method)
                && (path.equals(AUTH_PATH) || path.equals(AUTH_GOOGLE_PATH) || path.equals(AUTH_REFRESH_PATH));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequests(HttpServletResponse response, boolean isAuth) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = isAuth
                ? "Too many authentication attempts. Please wait before trying again."
                : "Too many requests. Please slow down.";
        response.getWriter().write("{\"error\":\"" + message + "\",\"status\":429}");
    }
}
