package es.elpajaroverde.models;

import es.elpajaroverde.enums.MensajeRemitente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaMensaje;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MensajeRemitente remitente;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public Mensaje() {
    }

    public Mensaje(LocalDateTime fechaMensaje, String asunto, String mensaje,
                   MensajeRemitente remitente, Usuario usuario) {
        this.fechaMensaje = fechaMensaje;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.remitente = remitente;
        this.usuario = usuario;
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

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
