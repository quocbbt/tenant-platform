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
                        .requestMatchers(
                                "/api/logiflow/health",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_CREATE)
                        .requestMatchers(HttpMethod.PATCH, "/api/logiflow/orders/*/status")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_UPDATE)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders/*/assign")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_ASSIGN)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders/*/tracking")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_TRACKING)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/orders/*/cod")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_COD_UPDATE)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/orders", "/api/logiflow/orders/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_ORDER_VIEW)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/operations/cod/summary")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_COD_VIEW)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/operations/cod/daily")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_COD_VIEW)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/operations/reconciliation/by-driver")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_RECONCILIATION_VIEW)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/reconciliations")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_RECONCILIATION_CREATE)
                        .requestMatchers(HttpMethod.PATCH, "/api/logiflow/reconciliations/*/status")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_RECONCILIATION_UPDATE)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/reconciliations", "/api/logiflow/reconciliations/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_RECONCILIATION_VIEW)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/customers")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_CREATE)
                        .requestMatchers(HttpMethod.PUT, "/api/logiflow/customers/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_UPDATE)
                        .requestMatchers(HttpMethod.DELETE, "/api/logiflow/customers/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_DELETE)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/customers", "/api/logiflow/customers/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_CUSTOMER_VIEW)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/drivers")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_CREATE)
                        .requestMatchers(HttpMethod.PUT, "/api/logiflow/drivers/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_UPDATE)
                        .requestMatchers(HttpMethod.DELETE, "/api/logiflow/drivers/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_DELETE)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/drivers", "/api/logiflow/drivers/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_DRIVER_VIEW)
                        .requestMatchers(HttpMethod.POST, "/api/logiflow/vehicles")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_CREATE)
                        .requestMatchers(HttpMethod.PUT, "/api/logiflow/vehicles/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_UPDATE)
                        .requestMatchers(HttpMethod.DELETE, "/api/logiflow/vehicles/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_DELETE)
                        .requestMatchers(HttpMethod.GET, "/api/logiflow/vehicles", "/api/logiflow/vehicles/*")
                        .hasAuthority(SecurityConstants.PERMISSION_LOGIFLOW_VEHICLE_VIEW)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
