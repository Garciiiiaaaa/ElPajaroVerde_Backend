package es.elpajaroverde.exceptions;

import es.elpajaroverde.services.ReservaReglas;

public class NumeroHuespedesInvalidoException extends RuntimeException {

    public NumeroHuespedesInvalidoException() {
        super("El número de huéspedes debe estar entre 1 y " + ReservaReglas.MAX_HUESPEDES);
    }

    public NumeroHuespedesInvalidoException(String message) {
        super(message);
    }
}