package es.elpajaroverde.services;

import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.services.interfaces.IConfiguracionService;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfiguracionService implements IConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionService(ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Override
    public Optional<Configuracion> getById(Long id) {
        return configuracionRepository.findById(id);
    }
}
