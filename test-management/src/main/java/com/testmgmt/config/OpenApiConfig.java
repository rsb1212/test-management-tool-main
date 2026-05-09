package com.testmgmt.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Test Management Tool API",
        version     = "1.0.0",
        description = "Complete REST API for the Test Management System — covering Auth, Test Cases, Test Plans, Test Runs, Executions, Defects, and Reports.",
        contact     = @Contact(name = "Platform Team", email = "admin@testmgmt.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local development"),
        @Server(url = "https://api.yourdomain.com", description = "Production")
    }
)
@SecurityScheme(
    name        = "bearerAuth",
    type        = SecuritySchemeType.HTTP,
    scheme      = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {}
