package es.elpajaroverde.controllers;

import es.elpajaroverde.dtos.MensajeRequest;
import es.elpajaroverde.dtos.MensajeResponse;
import es.elpajaroverde.services.interfaces.IMensajeService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/mensajes")
public class MensajeController {

    private static final int TAMANIO_PAGINA_MAXIMO = 100;

    private final IMensajeService mensajeService;

    public MensajeController(IMensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @PostMapping
    public ResponseEntity<MensajeResponse> enviar(@Valid @RequestBody MensajeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensajeService.enviar(request));
    }

    @GetMapping
    public ResponseEntity<Page<MensajeResponse>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int tamanio = Math.max(1, Math.min(size, TAMANIO_PAGINA_MAXIMO));
        return ResponseEntity.ok(mensajeService.listar(usuarioId, fechaDesde, fechaHasta,
                PageRequest.of(Math.max(page, 0), tamanio)));
    }
}