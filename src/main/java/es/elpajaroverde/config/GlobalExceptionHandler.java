package es.elpajaroverde.config;

import es.elpajaroverde.dtos.AuthErrorResponse;
import es.elpajaroverde.exceptions.ConfiguracionInvalidaException;
import es.elpajaroverde.exceptions.CorreoDuplicadoException;
import es.elpajaroverde.exceptions.CredencialesInvalidasException;
import es.elpajaroverde.exceptions.CuentaBloqueadaException;
import es.elpajaroverde.exceptions.DuracionEstanciaInvalidaException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.exceptions.FechasInvalidasException;
import es.elpajaroverde.exceptions.NumeroHuespedesInvalidoException;
import es.elpajaroverde.exceptions.OcultacionInvalidaException;
import es.elpajaroverde.exceptions.ReferenciaInconsistenteException;
import es.elpajaroverde.exceptions.SinTokenException;
import es.elpajaroverde.exceptions.SolapamientoReservaException;
import es.elpajaroverde.exceptions.TokenExpiradoException;
import es.elpajaroverde.exceptions.TokenInvalidoException;
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

    @ExceptionHandler(EntidadNoEncontradaException.class)
    public ResponseEntity<AuthErrorResponse> handleEntidadNoEncontrada(EntidadNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(SolapamientoReservaException.class)
    public ResponseEntity<AuthErrorResponse> handleSolapamientoReserva(SolapamientoReservaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<AuthErrorResponse> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(FechasInvalidasException.class)
    public ResponseEntity<AuthErrorResponse> handleFechasInvalidas(FechasInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DuracionEstanciaInvalidaException.class)
    public ResponseEntity<AuthErrorResponse> handleDuracionEstanciaInvalida(DuracionEstanciaInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NumeroHuespedesInvalidoException.class)
    public ResponseEntity<AuthErrorResponse> handleNumeroHuespedesInvalido(NumeroHuespedesInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(OcultacionInvalidaException.class)
    public ResponseEntity<AuthErrorResponse> handleOcultacionInvalida(OcultacionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConfiguracionInvalidaException.class)
    public ResponseEntity<AuthErrorResponse> handleConfiguracionInvalida(ConfiguracionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReferenciaInconsistenteException.class)
    public ResponseEntity<AuthErrorResponse> handleReferenciaInconsistente(ReferenciaInconsistenteException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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
