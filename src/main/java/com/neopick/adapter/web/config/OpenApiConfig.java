package com.neopick.adapter.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI neopickOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NEOPick API")
                        .version("1.0")
                        .description("Guitar lesson booking marketplace")
                        .contact(new Contact()
                                .name("NEOPick Team")
                                .email("dev@neopick.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.neopick.com").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("01 - Auth")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi teachersApi() {
        return GroupedOpenApi.builder()
                .group("02 - Teachers")
                .pathsToMatch("/api/v1/teachers/**")
                .build();
    }

    @Bean
    public GroupedOpenApi bookingsApi() {
        return GroupedOpenApi.builder()
                .group("03 - Bookings")
                .pathsToMatch("/api/v1/bookings/**")
                .build();
    }

    @Bean
    public GroupedOpenApi paymentsApi() {
        return GroupedOpenApi.builder()
                .group("04 - Payments")
                .pathsToMatch("/api/v1/payments/**")
                .build();
    }

    @Bean
    public GroupedOpenApi reviewsApi() {
        return GroupedOpenApi.builder()
                .group("05 - Reviews")
                .pathsToMatch("/api/v1/reviews/**")
                .build();
    }

    @Bean
    public GroupedOpenApi messagesApi() {
        return GroupedOpenApi.builder()
                .group("06 - Messages")
                .pathsToMatch("/api/v1/conversations/**")
                .build();
    }

    @Bean
    public GroupedOpenApi homeAndMiscApi() {
        return GroupedOpenApi.builder()
                .group("07 - Home & Misc")
                .pathsToMatch(
                        "/api/v1/home/**",
                        "/api/v1/cities/**",
                        "/api/v1/users/**",
                        "/api/v1/favorites/**",
                        "/api/v1/notifications/**",
                        "/api/v1/media/**",
                        "/api/v1/health"
                )
                .build();
    }
}
