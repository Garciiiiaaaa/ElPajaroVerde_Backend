package es.elpajaroverde.exceptions;

public class SolapamientoReservaException extends RuntimeException {

    public SolapamientoReservaException() {
        super("Las fechas solicitadas solapan con otra reserva");
    }

    public SolapamientoReservaException(String message) {
        super(message);
    }
}