package es.elpajaroverde.exceptions;

public class TokenExpiradoException extends RuntimeException {

    public TokenExpiradoException(String message) {
        super(message);
    }
}
