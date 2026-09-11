package es.elpajaroverde.services;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
