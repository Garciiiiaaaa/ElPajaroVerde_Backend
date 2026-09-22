package es.elpajaroverde.controllers;

import es.elpajaroverde.dtos.FechasOcupadasResponse;
import es.elpajaroverde.services.interfaces.IDisponibilidadService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/disponibilidad")
public class DisponibilidadController {

    private final IDisponibilidadService disponibilidadService;

    public DisponibilidadController(IDisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping
    public ResponseEntity<FechasOcupadasResponse> obtenerFechasOcupadas() {
        return ResponseEntity.ok(disponibilidadService.obtenerFechasOcupadas());
    }
}