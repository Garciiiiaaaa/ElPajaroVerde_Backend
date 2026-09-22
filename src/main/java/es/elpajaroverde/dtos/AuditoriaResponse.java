package es.elpajaroverde.dtos;

import es.elpajaroverde.enums.AuditoriaTipoAccion;

import java.time.LocalDateTime;

/**
 * Entrada de auditoría devuelta en el listado privado (RF-33).
 */
public class AuditoriaResponse {

    private Long id;

    private LocalDateTime fecha;

    private String admin;

    private String entidadAfectada;

    private Long entidadId;

    private AuditoriaTipoAccion tipoAccion;

    private String descripcion;

    public AuditoriaResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
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

    public AuditoriaTipoAccion getTipoAccion() {
        return tipoAccion;
    }

    public void setTipoAccion(AuditoriaTipoAccion tipoAccion) {
        this.tipoAccion = tipoAccion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}