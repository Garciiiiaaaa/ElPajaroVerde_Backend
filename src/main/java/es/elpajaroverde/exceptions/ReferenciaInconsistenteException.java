package es.elpajaroverde.exceptions;

public class ReferenciaInconsistenteException extends RuntimeException {

    public ReferenciaInconsistenteException() {
        super("No se puede indicar usuario_id y usuario a la vez");
    }

    public ReferenciaInconsistenteException(String message) {
        super(message);
    }
}