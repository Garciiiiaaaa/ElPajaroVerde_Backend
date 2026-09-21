package es.elpajaroverde.exceptions;

public class ConfiguracionInvalidaException extends RuntimeException {

    public ConfiguracionInvalidaException() {
        super("La configuración no es válida");
    }

    public ConfiguracionInvalidaException(String message) {
        super(message);
    }
}