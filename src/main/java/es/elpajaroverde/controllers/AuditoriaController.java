package es.elpajaroverde.controllers;

import es.elpajaroverde.dtos.AuditoriaResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.services.interfaces.IAuditoriaService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditoriaController {

    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final IAuditoriaService auditoriaService;

    public AuditoriaController(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditoriaResponse>> listar(
            @RequestParam(required = false) String entidadAfectada,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) AuditoriaTipoAccion tipoAccion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int tamanio = Math.max(1, Math.min(size, TAMANIO_PAGINA_MAXIMO));
        return ResponseEntity.ok(auditoriaService.listar(entidadAfectada, entidadId, tipoAccion,
                fechaDesde, fechaHasta, PageRequest.of(Math.max(page, 0), tamanio)));
    }
}