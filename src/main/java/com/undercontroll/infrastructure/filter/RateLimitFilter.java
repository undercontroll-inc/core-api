package com.undercontroll.infrastructure.filter;

import com.undercontroll.infrastructure.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH        = "/v1/api/users/auth";
    private static final String AUTH_GOOGLE_PATH  = "/v1/api/users/auth/google";
    private static final String AUTH_REFRESH_PATH = "/v1/api/users/auth/refresh";

    /** Separate bucket maps so auth endpoints have their own strict quota. */
    private final Map<String, Bucket> authBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    private final RateLimitProperties props;

    // ── Filter logic ─────────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String ip      = resolveClientIp(request);
        String path    = request.getServletPath();
        boolean isAuth = isAuthEndpoint(request.getMethod(), path);

        Bucket bucket = isAuth
                ? authBuckets.computeIfAbsent(ip, k -> buildAuthBucket())
                : generalBuckets.computeIfAbsent(ip, k -> buildGeneralBucket());

        if (bucket.tryConsume(1)) {
            // Expose remaining tokens to callers
            long remaining = bucket.getAvailableTokens();
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} path={}", ip, path);
            sendTooManyRequests(response, isAuth);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isAuthEndpoint(String method, String path) {
        return "POST".equalsIgnoreCase(method)
                && (path.equals(AUTH_PATH) || path.equals(AUTH_GOOGLE_PATH) || path.equals(AUTH_REFRESH_PATH));
    }

    private Bucket buildAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(props.getAuthRequestsPerMinute())
                .refillGreedy(props.getAuthRequestsPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket buildGeneralBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(props.getGeneralRequestsPerMinute())
                .refillGreedy(props.getGeneralRequestsPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Attempts to extract the real client IP, honouring common reverse-proxy
     * headers (X-Forwarded-For, X-Real-IP) before falling back to
     * {@link HttpServletRequest#getRemoteAddr()}.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // header may contain a comma-separated list; first entry is the originating IP
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

