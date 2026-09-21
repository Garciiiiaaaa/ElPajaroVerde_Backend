package es.elpajaroverde.controllers;

import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.services.interfaces.IConfiguracionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion")
public class ConfiguracionController {

    private final IConfiguracionService configuracionService;

    public ConfiguracionController(IConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping
    public ResponseEntity<Configuracion> get() {
        return configuracionService.getById(1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
