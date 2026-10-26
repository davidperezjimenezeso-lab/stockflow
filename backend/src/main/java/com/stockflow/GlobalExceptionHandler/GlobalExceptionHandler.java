package com.stockflow.GlobalExceptionHandler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        System.out.println("VALIDACIONES ACTIVADAS");

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
            );
        });

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 400);
        response.put("message", "Error de validación");
        response.put("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", ex.getStatusCode().value());
        response.put("message", ex.getReason() != null
                ? ex.getReason()
                : "Error en la petición");
        response.put("errors", Map.of());

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(response);
    }

    @ExceptionHandler(
            org.springframework.http.converter.HttpMessageNotReadableException.class
    )
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {

        System.err.println("ERROR AL LEER JSON: " + ex.getMessage());

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 400);
        response.put("message", "El cuerpo JSON no es válido");
        response.put("errors", Map.of());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
