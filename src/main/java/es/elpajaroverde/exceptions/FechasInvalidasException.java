package es.elpajaroverde.exceptions;

public class FechasInvalidasException extends RuntimeException {

    public FechasInvalidasException() {
        super("La fecha de entrada debe ser anterior a la fecha de salida");
    }

    public FechasInvalidasException(String message) {
        super(message);
    }
}