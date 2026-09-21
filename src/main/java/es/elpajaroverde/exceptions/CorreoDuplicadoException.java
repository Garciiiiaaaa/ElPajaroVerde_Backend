package es.elpajaroverde.exceptions;

public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException() {
        super("Ya existe un usuario con ese correo");
    }

    public CorreoDuplicadoException(String message) {
        super(message);
    }
}