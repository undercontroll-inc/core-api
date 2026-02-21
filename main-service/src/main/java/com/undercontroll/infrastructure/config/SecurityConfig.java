package com.undercontroll.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .csrf(CsrfConfigurer<HttpSecurity>::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**", "/actuator/health/**", "/actuator/prometheus").permitAll()
                        
                        // Public auth endpoints
                        .requestMatchers(HttpMethod.POST, "/v1/api/users/auth", "/v1/api/users/auth/google", "/v1/api/users").permitAll()
                        
                        // Public announcement endpoints
                        .requestMatchers(HttpMethod.GET, "/v1/api/announcements", "/v1/api/announcements/last").permitAll()
                        
                        // Customer and Admin shared endpoints
                        .requestMatchers(HttpMethod.PUT, "/v1/api/users/{userId}").hasAnyRole("SCOPE_CUSTOMER", "SCOPE_ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, "/v1/api/users/reset-password/{userId}").hasAnyRole("SCOPE_CUSTOMER", "SCOPE_ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/v1/api/orders/{orderId}", "/v1/api/orders/filter", "/v1/api/orders/export/{orderId}").hasAnyRole("SCOPE_CUSTOMER", "SCOPE_ADMINISTRATOR")
                        
                        // Admin-only endpoints
                        .requestMatchers(HttpMethod.POST, "/v1/api/announcements").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/v1/api/announcements/**").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/v1/api/announcements/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/dashboard/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/users/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/orders/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/components/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/service-orders/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/order-items/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/demands/**").hasRole("ADMINISTRATOR")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.applyPermitDefaultValues();
        configuration.setAllowedMethods(
                Arrays.asList(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.HEAD.name(),
                        HttpMethod.TRACE.name()
                )
        );

        configuration.setExposedHeaders(List.of(HttpHeaders.CONTENT_TYPE));

        UrlBasedCorsConfigurationSource origin = new UrlBasedCorsConfigurationSource();
        origin.registerCorsConfiguration("/**", configuration);

        return origin;
    }

}
