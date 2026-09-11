package es.elpajaroverde.services;

public class TokenExpiradoException extends RuntimeException {

    public TokenExpiradoException(String message) {
        super(message);
    }
}
