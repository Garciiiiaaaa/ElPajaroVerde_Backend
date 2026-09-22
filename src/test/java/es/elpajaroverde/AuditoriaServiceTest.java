package es.elpajaroverde;

import es.elpajaroverde.dtos.AuditoriaResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.mappers.AuditoriaMapper;
import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.models.Auditoria;
import es.elpajaroverde.repositories.AuditoriaRepository;
import es.elpajaroverde.services.AuditoriaService;
import es.elpajaroverde.services.AutenticadoActual;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private AuditoriaMapper auditoriaMapper;

    @Mock
    private AutenticadoActual autenticadoActual;

    private AuditoriaService service;

    private final Administrador admin = new Administrador("admin", "hash", "admin@test.com");

    @BeforeEach
    void setUp() {
        service = new AuditoriaService(auditoriaRepository, auditoriaMapper, autenticadoActual);
    }

    @Test
    void registrar_conAdminAutenticado() {
        when(autenticadoActual.get()).thenReturn(Optional.of(admin));

        service.registrar(AuditoriaTipoAccion.CREAR, "Usuario", 5L);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria guardada = captor.getValue();
        assertEquals(AuditoriaTipoAccion.CREAR, guardada.getTipoAccion());
        assertEquals(admin, guardada.getAdmin());
        assertEquals("Usuario", guardada.getEntidadAfectada());
        assertEquals(5L, guardada.getEntidadId());
        assertEquals("Creación de Usuario (id=5)", guardada.getDescripcion());
    }

    @Test
    void registrar_usaPlantillaPorTipoAccion() {
        when(autenticadoActual.get()).thenReturn(Optional.empty());

        service.registrar(AuditoriaTipoAccion.MODIFICAR, "Reserva", 3L);
        service.registrar(AuditoriaTipoAccion.CAMBIAR_ESTADO, "Reserva", 3L);
        service.registrar(AuditoriaTipoAccion.ELIMINAR_OCULTAR, "Reserva", 3L);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals("Modificación de Reserva (id=3)", captor.getAllValues().get(0).getDescripcion());
        assertEquals("Cambio de estado de Reserva (id=3)", captor.getAllValues().get(1).getDescripcion());
        assertEquals("Cambio de visibilidad de Reserva (id=3)", captor.getAllValues().get(2).getDescripcion());
    }

    @Test
    void registrar_sinSesion_adminNulo() {
        when(autenticadoActual.get()).thenReturn(Optional.empty());

        service.registrar(AuditoriaTipoAccion.CREAR, "Usuario", 1L);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertNull(captor.getValue().getAdmin());
    }

    @Test
    void listar_ordenaPorFechaDescPorDefecto() {
        PageImpl<Auditoria> pagina = new PageImpl<>(List.of());
        when(auditoriaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        service.listar("Usuario", null, AuditoriaTipoAccion.CREAR,
                LocalDateTime.now().minusDays(2), LocalDateTime.now(), PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditoriaRepository).findAll(any(Specification.class), captor.capture());
        assertTrue(captor.getValue().getSort().getOrderFor("fecha").isDescending());
    }

    @Test
    void listar_filtraYMapea() {
        Auditoria fila = new Auditoria(AuditoriaTipoAccion.CREAR, LocalDateTime.now(), admin,
                "Usuario", 7L, "Creación de Usuario (id=7)");
        PageImpl<Auditoria> pagina = new PageImpl<>(List.of(fila));
        when(auditoriaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);
        AuditoriaResponse respuesta = new AuditoriaResponse();
        respuesta.setAdmin("admin");
        respuesta.setDescripcion("Creación de Usuario (id=7)");
        when(auditoriaMapper.toResponse(fila)).thenReturn(respuesta);

        var resultado = service.listar("Usuario", 7L, AuditoriaTipoAccion.CREAR,
                null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fecha")));

        assertEquals(1, resultado.getContent().size());
        assertEquals("admin", resultado.getContent().get(0).getAdmin());
    }
}