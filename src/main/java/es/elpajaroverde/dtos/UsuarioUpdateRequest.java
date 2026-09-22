package es.elpajaroverde.dtos;

import jakarta.validation.constraints.Email;

/**
 * Petición de modificación parcial de un Usuario (RF-24, RF-25).
 * Todos los campos son opcionales (PATCH parcial).
 */
public class UsuarioUpdateRequest {

    private String nombre;

    private String apellido;

    @Email
    private String correo;

    private String telefono;

    public UsuarioUpdateRequest() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}