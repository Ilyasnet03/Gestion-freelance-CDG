package com.cdg.freelance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod & !production")
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Plateforme de Gestion des Freelances - CDG BEP")
                        .description("API REST complète pour la gestion des freelances, des projets, des feuilles de temps et de la facturation pour CDG BEP.\n" +
                                "Auteur : Ilyas BENLOUALI (Stage CDG BEP - Digital Factory)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ilyas BENLOUALI")
                                .email("ilyas.benlouali@cdg.ma")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
