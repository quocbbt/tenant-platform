package com.tenantcore.logiflowservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.tenantcore")
@ConfigurationPropertiesScan(basePackages = "com.tenantcore.logiflowservice")
public class LogiflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogiflowServiceApplication.class, args);
    }
}
