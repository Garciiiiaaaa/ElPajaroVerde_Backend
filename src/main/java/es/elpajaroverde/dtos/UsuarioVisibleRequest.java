package es.elpajaroverde.dtos;

import jakarta.validation.constraints.NotNull;

/**
 * Petición de ocultar/mostrar un Usuario (RF-26).
 */
public class UsuarioVisibleRequest {

    @NotNull
    private Boolean visible;

    public UsuarioVisibleRequest() {
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}