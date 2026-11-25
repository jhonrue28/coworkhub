package com.academy.coworkhub.config;

import com.academy.coworkhub.presentation.dto.response.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CoWorkHub API",
                version = "1.0",
                description = """
                        API REST para gestionar las reservas de una red de coworking: sedes, salas, \
                        equipamiento, servicios adicionales, planes de membresía, miembros y reservas.

                        **Cómo probarla:** inicie sesión con `POST /auth/login`, copie el token y péguelo en **Authorize**.

                        Todo error responde con el mismo cuerpo: `{"error", "codigo", "timestamp"}`, \
                        donde `codigo` es la regla de negocio violada (RN-xx) o el tipo de error."""),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(
        name = OpenApiConfig.BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT obtenido en POST /auth/login. Vence a los 60 minutos.")
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
    private static final String ERROR_SCHEMA = "#/components/schemas/ErrorResponse";

    // ---------------------------------------------------------------- grupos

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder().group("todo").displayName("Toda la API")
                .pathsToMatch("/auth/**", "/api/**").build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder().group("autenticacion").displayName("Autenticación")
                .pathsToMatch("/auth/**").build();
    }

    @Bean
    public GroupedOpenApi catalogApi() {
        return GroupedOpenApi.builder().group("catalogos").displayName("Catálogos")
                .pathsToMatch("/api/sedes/**", "/api/salas/**", "/api/equipamientos/**",
                        "/api/servicios-adicionales/**", "/api/planes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder().group("miembros").displayName("Miembros")
                .pathsToMatch("/api/miembros/**").build();
    }

    @Bean
    public GroupedOpenApi bookingApi() {
        return GroupedOpenApi.builder().group("reservas").displayName("Reservas y disponibilidad")
                .pathsToMatch("/api/reservas/**", "/api/disponibilidad/**").build();
    }

    // ---------------------------------------------------------------- respuestas de error

    /** Registra el esquema ErrorResponse, que ninguna operación devuelve directamente. */
    @Bean
    public GlobalOpenApiCustomizer errorSchemaCustomizer() {
        return openApi -> openApi.getComponents().addSchemas("ErrorResponse",
                ModelConverters.getInstance().readAllAsResolvedSchema(ErrorResponse.class).schema);
    }

    /**
     * Agrega a cada operación las respuestas de error que puede devolver, con un ejemplo de cuerpo:
     * 401/403 si requiere token, 400 si recibe datos, 404 si usa un id en la ruta y 409 si escribe.
     */
    @Bean
    public GlobalOperationCustomizer errorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            boolean secured = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
            boolean readsInput = hasParameter(handlerMethod, RequestBody.class)
                    || hasParameter(handlerMethod, RequestParam.class)
                    || hasParameter(handlerMethod, PathVariable.class);
            boolean write = !handlerMethod.hasMethodAnnotation(GetMapping.class);

            if (readsInput) {
                addError(operation, "400", "Datos faltantes o con formato inválido",
                        "nombre: no debe estar vacío", ErrorResponse.INVALID_DATA);
            }
            if (secured) {
                addError(operation, "401", "Sin token, o con un token alterado o vencido",
                        "Se requiere un token válido", ErrorResponse.UNAUTHORIZED);
                addError(operation, "403", "El usuario no tiene permiso",
                        "No tiene permiso para realizar esta acción", ErrorResponse.FORBIDDEN);
            }
            if (hasParameter(handlerMethod, PathVariable.class)) {
                addError(operation, "404", "El recurso no existe",
                        "Sala con id 99 no existe", ErrorResponse.NOT_FOUND);
            }
            if (write) {
                addError(operation, "409", "Se viola una regla de negocio o hay un duplicado",
                        "La sala ya está reservada en ese horario", "RN-01");
            }
            return operation;
        };
    }

    private static boolean hasParameter(HandlerMethod method, Class<? extends java.lang.annotation.Annotation> annotation) {
        return Arrays.stream(method.getMethodParameters()).anyMatch(p -> p.hasParameterAnnotation(annotation));
    }

    private static void addError(Operation operation, String code, String description, String message, String errorCode) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        if (operation.getResponses().containsKey(code)) {
            return; // la operación ya documenta este código
        }
        Map<String, Object> body = Map.of("error", message, "codigo", errorCode, "timestamp", "2030-06-03T13:00:00Z");
        operation.getResponses().addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType()
                        .schema(new Schema<>().$ref(ERROR_SCHEMA))
                        .addExamples(errorCode, new Example().value(body)))));
    }
}
