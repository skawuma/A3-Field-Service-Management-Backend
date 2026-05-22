package com.a3solutions.fsm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * @author samuelkawuma
 * @project A3 Field Service Management Backend
 * Updated to integrate JWT exception handling and stable API auth flows.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.auth.allow-self-registration:false}")
    private boolean selfRegistrationEnabled;

    @Value("${app.auth.allow-admin-bootstrap:false}")
    private boolean adminBootstrapEnabled;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            AuthenticationProvider authProvider,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authProvider = authProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<String> publicEndpoints = new ArrayList<>(List.of(
                "/api/auth/login",
                "/api/auth/refresh",
                "/swagger-ui/**",
                "/api-docs/**",
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/prometheus",
                "/ws",
                "/ws/**"
        ));

        if (selfRegistrationEnabled) {
            publicEndpoints.add("/api/auth/register");
        }

        if (adminBootstrapEnabled) {
            publicEndpoints.add("/api/auth/create-admin");
        }

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)  // 401 handler
                        .accessDeniedHandler(accessDeniedHandler)            // 403 handler
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(publicEndpoints.toArray(String[]::new)).permitAll()

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


@Bean
public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);

    config.setAllowedOrigins(
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .toList()
    );

    config.addAllowedHeader("*");
    config.addAllowedMethod("*");

    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
}
