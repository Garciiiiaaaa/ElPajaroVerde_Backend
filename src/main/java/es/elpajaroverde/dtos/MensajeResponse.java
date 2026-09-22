package es.elpajaroverde.dtos;

import es.elpajaroverde.enums.MensajeRemitente;

import java.time.LocalDateTime;

/**
 * Mensaje devuelto en la creación pública y el listado privado (RF-6, RF-29).
 */
public class MensajeResponse {

    private Long id;

    private LocalDateTime fechaMensaje;

    private String asunto;

    private String mensaje;

    private MensajeRemitente remitente;

    private UsuarioResponse usuario;

    public MensajeResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaMensaje() {
        return fechaMensaje;
    }

    public void setFechaMensaje(LocalDateTime fechaMensaje) {
        this.fechaMensaje = fechaMensaje;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public MensajeRemitente getRemitente() {
        return remitente;
    }

    public void setRemitente(MensajeRemitente remitente) {
        this.remitente = remitente;
    }

    public UsuarioResponse getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioResponse usuario) {
        this.usuario = usuario;
    }
}