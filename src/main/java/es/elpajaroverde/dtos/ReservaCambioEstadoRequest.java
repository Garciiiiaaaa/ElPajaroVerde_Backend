package es.elpajaroverde.dtos;

import es.elpajaroverde.enums.ReservaEstado;
import jakarta.validation.constraints.NotNull;

/**
 * Petición de cambio de estado de una reserva (RF-17, RF-18).
 */
public class ReservaCambioEstadoRequest {

    @NotNull
    private ReservaEstado estado;

    public ReservaCambioEstadoRequest() {
    }

    public ReservaEstado getEstado() {
        return estado;
    }

    public void setEstado(ReservaEstado estado) {
        this.estado = estado;
    }
}