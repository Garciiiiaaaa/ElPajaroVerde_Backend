package es.elpajaroverde.dtos;

import java.time.LocalDate;

/**
 * Par de fechas ocupadas devuelto por la consulta pública de disponibilidad (RF-1).
 */
public class FechasOcupadas {

    private LocalDate fechaEntrada;

    private LocalDate fechaSalida;

    public FechasOcupadas() {
    }

    public FechasOcupadas(LocalDate fechaEntrada, LocalDate fechaSalida) {
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }
}