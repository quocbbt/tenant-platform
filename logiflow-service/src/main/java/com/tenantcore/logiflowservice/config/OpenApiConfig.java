package com.tenantcore.logiflowservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logiflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TenantCore LogiFlow API")
                        .version("v1")
                        .description("Logistics service APIs (tenant-aware, JWT + permission secured)")
                        .contact(new Contact().name("TenantCore Team"))
                        .license(new License().name("Internal")));
    }
}
