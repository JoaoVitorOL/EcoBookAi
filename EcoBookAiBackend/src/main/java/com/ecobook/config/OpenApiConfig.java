package com.ecobook.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "EcoBook AI Backend API",
                version = "1.0.0-SNAPSHOT",
                description = "API REST do EcoBook AI para autenticacao, onboarding, materiais, discovery, solicitacoes, notificacoes, moderacao e LGPD.",
                contact = @Contact(name = "EcoBook AI"),
                license = @License(name = "Uso interno do projeto EcoBook AI")
        ),
        servers = {
                @Server(url = "/api", description = "Servidor local com context-path padrao do backend")
        }
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT emitido pelos endpoints de autenticacao do EcoBook AI"
)
public class OpenApiConfig {

    /**
     * Executes the eco book open api operation.
     * @return result of the operation
     */
    @Bean
    public OpenAPI ecoBookOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearer-jwt",
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT emitido pelos endpoints de autenticacao do EcoBook AI")
                ));
    }
}
