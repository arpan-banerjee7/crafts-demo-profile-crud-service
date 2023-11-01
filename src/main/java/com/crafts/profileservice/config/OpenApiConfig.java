package com.crafts.profileservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Profile Service",
        version = "1.0",
        description = "Profile Service APIs",
        contact = @io.swagger.v3.oas.annotations.info.Contact(
                name = "Dev",
                email = "dev@email.com"
        )
))
public class OpenApiConfig {
}

