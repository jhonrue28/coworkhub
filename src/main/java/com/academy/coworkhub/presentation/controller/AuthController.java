package com.academy.coworkhub.presentation.controller;

import com.academy.coworkhub.business.security.AuthService;
import com.academy.coworkhub.presentation.dto.request.LoginRequest;
import com.academy.coworkhub.presentation.dto.response.ErrorResponse;
import com.academy.coworkhub.presentation.dto.response.LoginResponse;
import com.academy.coworkhub.presentation.dto.response.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT y perfil del usuario (RF-11, RF-14)")
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Inicia sesión", description = "Acceso: público. Devuelve un JWT que vence en 60 minutos; envíelo como Authorization: Bearer <token>. Credenciales incorrectas: 401.")
    @SecurityRequirements // público: sin candado en Swagger
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenciales correctas: devuelve el token"),
            @ApiResponse(responseCode = "400", description = "Falta el usuario o la contraseña",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"error\": \"password: no debe estar vacío\", \"codigo\": \"DATOS_INVALIDOS\", \"timestamp\": \"2030-06-03T13:00:00Z\"}"))),
            @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"error\": \"Usuario o contraseña incorrectos\", \"codigo\": \"NO_AUTENTICADO\", \"timestamp\": \"2030-06-03T13:00:00Z\"}")))
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Perfil del usuario", description = "Acceso: cualquier usuario autenticado. Usuario, rol y miembroId leídos del token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del usuario del token"),
            @ApiResponse(responseCode = "401", description = "Sin token, o con un token alterado o vencido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = @ExampleObject(
                            value = "{\"error\": \"Se requiere un token válido\", \"codigo\": \"NO_AUTENTICADO\", \"timestamp\": \"2030-06-03T13:00:00Z\"}")))
    })
    @GetMapping("/perfil")
    public ProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
        Number memberId = jwt.getClaim(AuthService.MEMBER_ID_CLAIM);
        return new ProfileResponse(jwt.getSubject(), jwt.getClaimAsString(AuthService.ROLE_CLAIM),
                memberId == null ? null : memberId.longValue());
    }
}
