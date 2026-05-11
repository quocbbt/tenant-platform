package com.tenantcore.logiflowservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tenantcore")
public class LogiflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogiflowServiceApplication.class, args);
    }
}