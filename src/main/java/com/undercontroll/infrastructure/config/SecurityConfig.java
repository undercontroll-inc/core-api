package com.undercontroll.infrastructure.config;

import com.undercontroll.infrastructure.filter.AuthContextFilter;
import com.undercontroll.infrastructure.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final IpAddressMatcher LOCALHOST_IPV4 = new IpAddressMatcher("127.0.0.1");
    private static final IpAddressMatcher LOCALHOST_IPV6 = new IpAddressMatcher("::1");

    private final AuthContextFilter authFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .csrf(CsrfConfigurer<HttpSecurity>::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // H2 console: localhost only (dev profile)
                                .requestMatchers("/h2-console/**").access((authentication, context) -> {
                            String remoteAddr = context.getRequest().getRemoteAddr();
                            boolean isLocalhost = LOCALHOST_IPV4.matches(remoteAddr)
                                    || LOCALHOST_IPV6.matches(remoteAddr);
                            return new org.springframework.security.authorization.AuthorizationDecision(isLocalhost);
                        })
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**", "/actuator/health/**", "/actuator/prometheus").permitAll()
                        
                        // Public auth endpoints
                        .requestMatchers(HttpMethod.POST, "/v1/api/users/auth", "/v1/api/users/auth/google", "/v1/api/users/auth/refresh", "/v1/api/users").permitAll()

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
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
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
