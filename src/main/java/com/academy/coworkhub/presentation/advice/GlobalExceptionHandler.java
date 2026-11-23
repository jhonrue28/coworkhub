package com.academy.coworkhub.presentation.advice;

import com.academy.coworkhub.business.exception.BusinessRuleException;
import com.academy.coworkhub.business.exception.InvalidRequestException;
import com.academy.coworkhub.business.exception.ResourceNotFoundException;
import com.academy.coworkhub.presentation.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/** Traduce toda excepción al cuerpo de error único. Ninguna regla de negocio llega como 500. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // ---------------------------------------------------------------- 409

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> businessRule(BusinessRuleException e) {
        return build(HttpStatus.CONFLICT, e.getMessage(), e.getCode());
    }

    /** Respaldo de RN-11 / RN-12 si dos solicitudes pasan la validación a la vez y choca la restricción de la base. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException e) {
        String detail = String.valueOf(e.getMostSpecificCause().getMessage());
        boolean duplicate = detail.contains("Duplicate");
        return build(HttpStatus.CONFLICT,
                duplicate ? "Ya existe un registro con esos datos" : "El registro está en uso y no se puede modificar ni eliminar",
                duplicate ? "RN-11" : "RN-12");
    }

    // ---------------------------------------------------------------- 404

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), ErrorResponse.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noRoute(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "La ruta /" + e.getResourcePath() + " no existe", ErrorResponse.NOT_FOUND);
    }

    // ---------------------------------------------------------------- 400

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .sorted((a, b) -> a.getField().compareTo(b.getField()))
                .map(GlobalExceptionHandler::describe)
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Los datos enviados no son válidos";
        }
        return build(HttpStatus.BAD_REQUEST, message, ErrorResponse.INVALID_DATA);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> invalidParameters(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, ErrorResponse.INVALID_DATA);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableBody(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud falta o no es un JSON válido (revise formatos de fecha, hora y valores de enumeraciones)",
                ErrorResponse.INVALID_DATA);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST,
                "El valor '" + e.getValue() + "' no es válido para el parámetro " + e.getName(),
                ErrorResponse.INVALID_DATA);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio " + e.getParameterName(), ErrorResponse.INVALID_DATA);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> invalidRequest(InvalidRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), ErrorResponse.INVALID_DATA);
    }

    // ---------------------------------------------------------------- 403 / 405 / 500

    /** Lo lanza @PreAuthorize dentro del controlador (fase 7). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException e) {
        return build(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción", ErrorResponse.FORBIDDEN);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "El método " + e.getMethod() + " no está permitido en esta ruta",
                ErrorResponse.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e) {
        log.error("Error no controlado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", ErrorResponse.INTERNAL_ERROR);
    }

    // ----------------------------------------------------------------

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String code) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(message, code, Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)));
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
