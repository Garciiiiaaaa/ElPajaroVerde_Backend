package es.elpajaroverde.config;

import es.elpajaroverde.dtos.AuthErrorResponse;
import es.elpajaroverde.services.CredencialesInvalidasException;
import es.elpajaroverde.services.CuentaBloqueadaException;
import es.elpajaroverde.services.SinTokenException;
import es.elpajaroverde.services.TokenExpiradoException;
import es.elpajaroverde.services.TokenInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<AuthErrorResponse> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<AuthErrorResponse> handleCuentaBloqueada(CuentaBloqueadaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<AuthErrorResponse> handleTokenInvalido(TokenInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(TokenExpiradoException.class)
    public ResponseEntity<AuthErrorResponse> handleTokenExpirado(TokenExpiradoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(SinTokenException.class)
    public ResponseEntity<AuthErrorResponse> handleSinToken(SinTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Error de validación");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<AuthErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse("Token no proporcionado"));
    }
}
