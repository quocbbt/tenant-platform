package com.tenantcore.coreservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/core/health",
                                "/api/core/error-test",
                                "/actuator/health"
                        ).permitAll()

                        // Tạm thời cho phép hết trong giai đoạn dev skeleton
                        // Sau khi có JWT thì đổi thành .anyRequest().authenticated()
                        .anyRequest().permitAll()
                )
                .build();
    }
}