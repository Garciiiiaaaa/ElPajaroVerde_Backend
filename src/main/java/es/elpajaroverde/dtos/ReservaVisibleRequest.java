package es.elpajaroverde.dtos;

import jakarta.validation.constraints.NotNull;

/**
 * Petición de ocultar/mostrar una reserva (RF-19, RF-20).
 */
public class ReservaVisibleRequest {

    @NotNull
    private Boolean visible;

    public ReservaVisibleRequest() {
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}