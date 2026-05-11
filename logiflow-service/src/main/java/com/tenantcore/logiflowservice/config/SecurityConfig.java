package com.tenantcore.logiflowservice.config;

import com.tenantcore.common.security.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/logiflow/health", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_CREATE)
                        .requestMatchers(HttpMethod.PATCH, "/api/logiflow/orders/*/status")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_UPDATE)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders/*/assign")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_ASSIGN)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders/*/tracking")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_TRACKING)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders/*/cod")
                        .hasAuthority("LOGIFLOW_COD_UPDATE")
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/orders", "/api/logiflow/orders/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_VIEW)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
