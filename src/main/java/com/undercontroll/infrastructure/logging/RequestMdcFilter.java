package com.undercontroll.infrastructure.logging;

import com.undercontroll.domain.support.Durations;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestMdcFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long started = System.nanoTime();
        String correlationId = firstHeader(request, REQUEST_ID_HEADER, CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (shouldLog(request.getRequestURI())) {
                log.info(
                        "{} {} status={} durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        Durations.millisSince(started)
                );
            }
            MDC.clear();
        }
    }

    static boolean shouldLog(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return !path.startsWith("/actuator")
                && !path.startsWith("/swagger-ui")
                && !path.startsWith("/v3/api-docs")
                && !path.startsWith("/h2-console");
    }

    private static String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
