package es.elpajaroverde.dtos;

import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.services.ReservaReglas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Petición de creación de una reserva (RF-8 a RF-14).
 * El usuario se resuelve por {@code usuarioId}, por datos inline ({@code usuario})
 * o sin asociar (ninguno de los dos).
 */
public class ReservaRequest {

    @NotNull
    private LocalDate fechaEntrada;

    @NotNull
    private LocalDate fechaSalida;

    @NotNull
    @Min(1)
    @Max(value = ReservaReglas.MAX_HUESPEDES)
    private Integer numeroHuespedes;

    private ReservaEstado estado;

    private Long usuarioId;

    @Valid
    private UsuarioReserva usuario;

    public ReservaRequest() {
    }

    /**
     * Datos de un usuario nuevo aportados inline en la petición (RF-8).
     */
    public static class UsuarioReserva {

        @NotBlank
        private String nombre;

        @NotBlank
        private String apellido;

        @NotBlank
        @Email
        private String correo;

        private String telefono;

        public UsuarioReserva() {
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

    public Integer getNumeroHuespedes() {
        return numeroHuespedes;
    }

    public void setNumeroHuespedes(Integer numeroHuespedes) {
        this.numeroHuespedes = numeroHuespedes;
    }

    public ReservaEstado getEstado() {
        return estado;
    }

    public void setEstado(ReservaEstado estado) {
        this.estado = estado;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UsuarioReserva getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioReserva usuario) {
        this.usuario = usuario;
    }
}