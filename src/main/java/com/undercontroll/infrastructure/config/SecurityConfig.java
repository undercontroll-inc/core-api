package com.undercontroll.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.infrastructure.security.AuthContextFilter;
import com.undercontroll.infrastructure.security.JsonAccessDeniedHandler;
import com.undercontroll.infrastructure.security.JsonAuthenticationEntryPoint;
import com.undercontroll.infrastructure.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

import jakarta.servlet.DispatcherType;
import java.util.List;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ORDER_BY_ID = "/v1/api/orders/{orderId}";
    private static final String ANNOUNCEMENTS = "/v1/api/announcements/**";
    private static final IpAddressMatcher LOCALHOST_IPV4 = new IpAddressMatcher("127.0.0.1");
    private static final IpAddressMatcher LOCALHOST_IPV6 = new IpAddressMatcher("::1");

    private final AuthContextFilter authFilter;
    private final RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper;

    @Value("${undercontroll.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/v1/api/**", "/h2-console/**")) // NOSONAR - Bearer JWT, no cookies
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                            .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers("/h2-console/**").access((authentication, context) -> {
                            String remoteAddr = context.getRequest().getRemoteAddr();
                            boolean isLocalhost = LOCALHOST_IPV4.matches(remoteAddr)
                                    || LOCALHOST_IPV6.matches(remoteAddr);
                            return new org.springframework.security.authorization.AuthorizationDecision(isLocalhost);
                        })
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**", "/actuator/health/**", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/api/auth", "/v1/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, ANNOUNCEMENTS, "/v1/api/announcements/latest").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/api/orders").hasAnyRole("CUSTOMER", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, "/v1/api/users/{userId}", "/v1/api/users/{userId}/password").hasAnyRole("CUSTOMER", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/v1/api/orders", ORDER_BY_ID, ORDER_BY_ID + "/export").hasAnyRole("CUSTOMER", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/v1/api/announcements").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, ANNOUNCEMENTS).hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, ANNOUNCEMENTS).hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/dashboard/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/analytics/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/insights/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/chats/**").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/v1/api/transcriptions").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/v1/api/users").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/v1/api/users/**").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, ORDER_BY_ID).hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, ORDER_BY_ID).hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/orders/*/items/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/orders/*/demands/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/components/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/v1/api/service-orders/**").hasRole("ADMINISTRATOR")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper))
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
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));
        configuration.setExposedHeaders(List.of(HttpHeaders.CONTENT_TYPE));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource origin = new UrlBasedCorsConfigurationSource();
        origin.registerCorsConfiguration("/**", configuration);
        return origin;
    }

}
