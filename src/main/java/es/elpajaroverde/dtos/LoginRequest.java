package es.elpajaroverde.dtos;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank
    private String nombreUsuario;

    @NotBlank
    private String contrasena;

    public LoginRequest() {
    }

    public LoginRequest(String nombreUsuario, String contrasena) {
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contrasena;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
