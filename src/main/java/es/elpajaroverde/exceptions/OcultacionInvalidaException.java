package es.elpajaroverde.exceptions;

public class OcultacionInvalidaException extends RuntimeException {

    public OcultacionInvalidaException() {
        super("Solo puede ocultarse una reserva cancelada o pasada");
    }

    public OcultacionInvalidaException(String message) {
        super(message);
    }
}