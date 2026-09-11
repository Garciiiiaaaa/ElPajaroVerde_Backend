package es.elpajaroverde.models;

import es.elpajaroverde.enums.AuditoriaTipoAccion;
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
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditoriaTipoAccion tipoAccion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Administrador admin;

    @Column(nullable = false, length = 50)
    private String entidadAfectada;

    @Column(nullable = false)
    private Long entidadId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    public Auditoria() {
    }

    public Auditoria(AuditoriaTipoAccion tipoAccion, LocalDateTime fecha, Administrador admin,
                     String entidadAfectada, Long entidadId, String descripcion) {
        this.tipoAccion = tipoAccion;
        this.fecha = fecha;
        this.admin = admin;
        this.entidadAfectada = entidadAfectada;
        this.entidadId = entidadId;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuditoriaTipoAccion getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(AuditoriaTipoAccion tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Administrador getAdmin() {
        return admin;
    }

    public void setAdmin(Administrador admin) {
        this.admin = admin;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }

    public void setEntidadAfectada(String entidadAfectada) {
        this.entidadAfectada = entidadAfectada;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
