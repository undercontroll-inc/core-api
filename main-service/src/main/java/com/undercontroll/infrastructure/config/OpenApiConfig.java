package com.undercontroll.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do Swagger/OpenAPI 3.0
 *
 * Documentação interativa da API disponível em:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Undercontroll API}")
    private String applicationName;

    @Value("${api.version:1.0.0}")
    private String apiVersion;

    @Value("${api.description:API RESTful para gerenciamento de componentes, pedidos e usuários}")
    private String apiDescription;

    /**
     * Configura a especificação OpenAPI com informações da API,
     * esquemas de segurança e servidores disponíveis.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(buildInfo())
                .servers(buildServers())
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(securitySchemeName, buildSecurityScheme())
                );
    }

    /**
     * Constrói as informações gerais da API
     */
    private Info buildInfo() {
        return new Info()
                .title(applicationName)
                .description(apiDescription)
                .version(apiVersion)
                .contact(buildContact())
                .license(buildLicense());
    }

    /**
     * Informações de contato do time de desenvolvimento
     */
    private Contact buildContact() {
        return new Contact()
                .name("Undercontroll Team")
                .email("contato@undercontroll.com")
                .url("https://github.com/undercontroll/api");
    }

    /**
     * Informações de licença da API
     */
    private License buildLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    /**
     * Define os servidores disponíveis para a API
     */
    private List<Server> buildServers() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Servidor Local de Desenvolvimento");

//        Server devServer = new Server()
//                .url("https://dev-api.undercontroll.com")
//                .description("Servidor de Desenvolvimento");
//
//        Server prodServer = new Server()
//                .url("https://api.undercontroll.com")
//                .description("Servidor de Produção");

        return List.of(localServer);
    }

    /**
     * Configura o esquema de segurança JWT Bearer
     */
    private SecurityScheme buildSecurityScheme() {
        return new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Autenticação via JWT Token.
                        
                        Para obter o token, faça login através do endpoint /v1/api/users/auth
                        
                        Exemplo de uso:
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        
                        O token deve ser enviado no header 'Authorization' com o prefixo 'Bearer '.
                        """);
    }
}

