package com.resolve.support.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OperationCustomizer documentUnexpectedErrors() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }
            if (!operation.getResponses().containsKey("400")) {
                operation.getResponses().addApiResponse("400", errorResponse("Invalid request or parameter."));
            }
            if (!operation.getResponses().containsKey("500")) {
                operation.getResponses().addApiResponse("500", errorResponse("Unexpected server error."));
            }
            return operation;
        };
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))));
    }

    @Bean
    OpenAPI resolveOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resolve Support Ticket Management API")
                        .version("1.0.0")
                        .description("REST API for support ticket creation, assignment, management, comments, authentication, and role-based access control."))
                .tags(List.of(
                        new Tag().name("Authentication").description("Public registration and login endpoints."),
                        new Tag().name("Tickets").description("Ticket creation, retrieval, filtering, assignment, and status workflow."),
                        new Tag().name("Comments").description("Ticket conversation endpoints."),
                        new Tag().name("Ticket History").description("Read-only audit history for ticket creation, status, and assignment changes."),
                        new Tag().name("Administration").description("Administrative user and role management.")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by POST /api/v1/auth/login. Enter the token only; Swagger UI adds the Bearer prefix.")));
    }
}
