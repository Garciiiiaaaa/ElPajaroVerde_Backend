package es.elpajaroverde.services;

import es.elpajaroverde.dtos.ConfiguracionRequest;
import es.elpajaroverde.dtos.ConfiguracionResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.exceptions.ConfiguracionInvalidaException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.mappers.ConfiguracionMapper;
import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.services.interfaces.IConfiguracionService;
import es.elpajaroverde.services.interfaces.IAuditoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ConfiguracionService implements IConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;
    private final ConfiguracionMapper configuracionMapper;
    private final IAuditoriaService auditoriaService;

    public ConfiguracionService(ConfiguracionRepository configuracionRepository,
                                ConfiguracionMapper configuracionMapper,
                                IAuditoriaService auditoriaService) {
        this.configuracionRepository = configuracionRepository;
        this.configuracionMapper = configuracionMapper;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionResponse obtener() {
        Configuracion configuracion = configuracionRepository.findById(1L)
                .orElseThrow(() -> new EntidadNoEncontradaException("Configuración no encontrada"));
        return configuracionMapper.toResponse(configuracion);
    }

    @Override
    @Transactional
    public ConfiguracionResponse actualizar(ConfiguracionRequest request) {
        Configuracion configuracion = configuracionRepository.findByIdWithLock()
                .orElseThrow(() -> new EntidadNoEncontradaException("Configuración no encontrada"));

        if (request.getPrecioNoche() != null) {
            configuracion.setPrecioNoche(request.getPrecioNoche());
        }
        if (request.getEstanciaMinima() != null) {
            configuracion.setEstanciaMinima(request.getEstanciaMinima());
        }
        if (request.getEstanciaMaxima() != null) {
            configuracion.setEstanciaMaxima(request.getEstanciaMaxima());
        }

        validar(configuracion);

        configuracionRepository.save(configuracion);
        auditoriaService.registrar(AuditoriaTipoAccion.MODIFICAR, "Configuracion", configuracion.getId());
        return configuracionMapper.toResponse(configuracion);
    }

    private void validar(Configuracion configuracion) {
        boolean valida = configuracion.getPrecioNoche() != null
                && configuracion.getPrecioNoche().compareTo(BigDecimal.ZERO) > 0
                && configuracion.getEstanciaMinima() >= 1
                && configuracion.getEstanciaMaxima() >= 1
                && configuracion.getEstanciaMinima() <= configuracion.getEstanciaMaxima();
        if (!valida) {
            throw new ConfiguracionInvalidaException();
        }
    }
}