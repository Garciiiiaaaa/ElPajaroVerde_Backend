package es.elpajaroverde.exceptions;

public class EntidadNoEncontradaException extends RuntimeException {

    public EntidadNoEncontradaException() {
        super("Reserva/Usuario no encontrado");
    }

    public EntidadNoEncontradaException(String message) {
        super(message);
    }
}