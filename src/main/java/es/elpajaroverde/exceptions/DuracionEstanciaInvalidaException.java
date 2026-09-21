package es.elpajaroverde.exceptions;

public class DuracionEstanciaInvalidaException extends RuntimeException {

    public DuracionEstanciaInvalidaException() {
        super("La duración debe estar entre la estancia mínima y máxima");
    }

    public DuracionEstanciaInvalidaException(String message) {
        super(message);
    }
}