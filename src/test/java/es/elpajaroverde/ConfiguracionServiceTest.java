package es.elpajaroverde;

import es.elpajaroverde.dtos.ConfiguracionRequest;
import es.elpajaroverde.dtos.ConfiguracionResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.exceptions.ConfiguracionInvalidaException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.mappers.ConfiguracionMapper;
import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.services.ConfiguracionService;
import es.elpajaroverde.services.interfaces.IAuditoriaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @Mock
    private ConfiguracionMapper configuracionMapper;

    @Mock
    private IAuditoriaService auditoriaService;

    private ConfiguracionService service;

    private final Configuracion base = new Configuracion(new BigDecimal("50.00"), 2, 30);

    @BeforeEach
    void setUp() {
        service = new ConfiguracionService(configuracionRepository, configuracionMapper, auditoriaService);
        when(configuracionMapper.toResponse(any(Configuracion.class))).thenAnswer(i -> respuestaPara(i.getArgument(0)));
    }

    private ConfiguracionResponse respuestaPara(Configuracion c) {
        ConfiguracionResponse r = new ConfiguracionResponse();
        r.setId(c.getId());
        r.setPrecioNoche(c.getPrecioNoche());
        r.setEstanciaMinima(c.getEstanciaMinima());
        r.setEstanciaMaxima(c.getEstanciaMaxima());
        return r;
    }

    @Test
    void obtener_mapeaConfiguracionExistente() {
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(base));
        when(configuracionMapper.toResponse(base)).thenReturn(respuestaPara(base));

        ConfiguracionResponse respuesta = service.obtener();

        assertEquals(1L, respuesta.getId());
        assertEquals(new BigDecimal("50.00"), respuesta.getPrecioNoche());
    }

    @Test
    void obtener_sinFila_lanzaEntidadNoEncontrada() {
        when(configuracionRepository.findById(1L)).thenReturn(Optional.empty());

        EntidadNoEncontradaException ex = assertThrows(EntidadNoEncontradaException.class, service::obtener);

        assertEquals("Configuración no encontrada", ex.getMessage());
    }

    @Test
    void actualizar_aplicaSoloCamposPresentes() {
        when(configuracionRepository.findByIdWithLock()).thenReturn(Optional.of(base));
        ConfiguracionRequest request = new ConfiguracionRequest();
        request.setEstanciaMinima(3);

        ConfiguracionResponse respuesta = service.actualizar(request);

        ArgumentCaptor<Configuracion> captor = ArgumentCaptor.forClass(Configuracion.class);
        verify(configuracionRepository).save(captor.capture());
        Configuracion guardada = captor.getValue();
        assertEquals(3, guardada.getEstanciaMinima());
        assertEquals(30, guardada.getEstanciaMaxima());
        assertEquals(new BigDecimal("50.00"), guardada.getPrecioNoche());
        verify(auditoriaService).registrar(eq(AuditoriaTipoAccion.MODIFICAR), eq("Configuracion"), eq(1L));
        assertEquals(3, respuesta.getEstanciaMinima());
    }

    @Test
    void actualizar_aceptaMinIgualMax() {
        when(configuracionRepository.findByIdWithLock()).thenReturn(Optional.of(base));
        ConfiguracionRequest request = new ConfiguracionRequest();
        request.setEstanciaMinima(2);
        request.setEstanciaMaxima(2);

        service.actualizar(request);

        verify(configuracionRepository).save(any(Configuracion.class));
    }

    @Test
    void actualizar_rechazaMinMayorQueMax() {
        when(configuracionRepository.findByIdWithLock()).thenReturn(Optional.of(base));
        ConfiguracionRequest request = new ConfiguracionRequest();
        request.setEstanciaMinima(10);
        request.setEstanciaMaxima(5);

        assertThrows(ConfiguracionInvalidaException.class, () -> service.actualizar(request));
        verify(configuracionRepository, never()).save(any());
    }

    @Test
    void actualizar_rechazaLimpiteNoPositivo() {
        when(configuracionRepository.findByIdWithLock()).thenReturn(Optional.of(base));

        ConfiguracionRequest request = new ConfiguracionRequest();
        request.setEstanciaMinima(0);
        assertThrows(ConfiguracionInvalidaException.class, () -> service.actualizar(request));

        ConfiguracionRequest requestPrecioCero = new ConfiguracionRequest();
        requestPrecioCero.setPrecioNoche(new BigDecimal("0.00"));
        assertThrows(ConfiguracionInvalidaException.class, () -> service.actualizar(requestPrecioCero));
        verify(configuracionRepository, never()).save(any());
    }

    @Test
    void actualizar_noRecalculaReservasPrevias() {
        when(configuracionRepository.findByIdWithLock()).thenReturn(Optional.of(base));
        ConfiguracionRequest request = new ConfiguracionRequest();
        request.setPrecioNoche(new BigDecimal("80.00"));

        ConfiguracionResponse respuesta = service.actualizar(request);

        assertEquals(new BigDecimal("80.00"), respuesta.getPrecioNoche());
        verify(configuracionRepository).save(any(Configuracion.class));
    }
}