package com.undercontroll.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * Max requests per minute per IP for authentication endpoints
     * (POST /v1/api/users/auth and POST /v1/api/users/auth/google).
     */
    private int authRequestsPerMinute = 10;

    /**
     * Max requests per minute per IP for all other endpoints.
     */
    private int generalRequestsPerMinute = 100;
}

