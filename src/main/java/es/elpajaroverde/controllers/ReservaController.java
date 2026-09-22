package es.elpajaroverde.controllers;

import es.elpajaroverde.dtos.ReservaCambioEstadoRequest;
import es.elpajaroverde.dtos.ReservaRequest;
import es.elpajaroverde.dtos.ReservaResponse;
import es.elpajaroverde.dtos.ReservaUpdateRequest;
import es.elpajaroverde.dtos.ReservaVisibleRequest;
import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.services.interfaces.IReservaService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final IReservaService reservaService;

    public ReservaController(IReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReservaResponse> modificar(@PathVariable Long id,
                                                     @Valid @RequestBody ReservaUpdateRequest request) {
        return ResponseEntity.ok(reservaService.modificar(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ReservaResponse> cambiarEstado(@PathVariable Long id,
                                                         @Valid @RequestBody ReservaCambioEstadoRequest request) {
        return ResponseEntity.ok(reservaService.cambiarEstado(id, request));
    }

    @PatchMapping("/{id}/visible")
    public ResponseEntity<ReservaResponse> cambiarVisible(@PathVariable Long id,
                                                          @Valid @RequestBody ReservaVisibleRequest request) {
        return ResponseEntity.ok(reservaService.cambiarVisible(id, request));
    }

    @GetMapping
    public ResponseEntity<Page<ReservaResponse>> listar(
            @RequestParam(required = false) ReservaEstado estado,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int tamanio = Math.max(1, Math.min(size, TAMANIO_PAGINA_MAXIMO));
        return ResponseEntity.ok(reservaService.listar(estado, visible, usuarioId, fechaDesde, fechaHasta,
                PageRequest.of(Math.max(page, 0), tamanio)));
    }
}