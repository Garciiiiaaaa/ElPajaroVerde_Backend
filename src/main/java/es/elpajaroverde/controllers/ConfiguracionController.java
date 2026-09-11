package es.elpajaroverde.controllers;

import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion")
public class ConfiguracionController {

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionController(ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @GetMapping
    public ResponseEntity<Configuracion> get() {
        return configuracionRepository.findById(1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
