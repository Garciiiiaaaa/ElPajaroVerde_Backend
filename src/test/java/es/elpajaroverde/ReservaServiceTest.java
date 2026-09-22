package es.elpajaroverde;

import es.elpajaroverde.dtos.ReservaCambioEstadoRequest;
import es.elpajaroverde.dtos.ReservaRequest;
import es.elpajaroverde.dtos.ReservaUpdateRequest;
import es.elpajaroverde.dtos.ReservaVisibleRequest;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.exceptions.DuracionEstanciaInvalidaException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.exceptions.FechasInvalidasException;
import es.elpajaroverde.exceptions.NumeroHuespedesInvalidoException;
import es.elpajaroverde.exceptions.OcultacionInvalidaException;
import es.elpajaroverde.exceptions.SolapamientoReservaException;
import es.elpajaroverde.mappers.ReservaMapper;
import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.models.Reserva;
import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.repositories.ReservaRepository;
import es.elpajaroverde.repositories.UsuarioRepository;
import es.elpajaroverde.services.ReservaService;
import es.elpajaroverde.services.interfaces.IAuditoriaService;

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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConfiguracionRepository configuracionRepository;

    @Mock
    private ReservaMapper reservaMapper;

    @Mock
    private IAuditoriaService auditoriaService;

    private ReservaService service;

    private final Configuracion config = new Configuracion(new BigDecimal("50.00"), 2, 30);

    @BeforeEach
    void setUp() {
        service = new ReservaService(reservaRepository, usuarioRepository, configuracionRepository,
                reservaMapper, auditoriaService);
        when(configuracionRepository.findByIdWithLock()).thenReturn(Optional.of(config));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> {
            Reserva r = i.getArgument(0);
            if (r.getId() == null) {
                r.setId(21L);
            }
            return r;
        });
    }

    private Usuario usuario(Long id, String correo) {
        Usuario u = new Usuario("Ana", "Lopez", correo, "600000000", true);
        u.setId(id);
        return u;
    }

    private Reserva reservaBase(LocalDate entrada, LocalDate salida) {
        Reserva r = new Reserva();
        r.setFechaEntrada(entrada);
        r.setFechaSalida(salida);
        r.setNumeroHuespedes(2);
        r.setEstado(null);
        return r;
    }

    private ReservaRequest requestEntrada() {
        ReservaRequest r = new ReservaRequest();
        r.setFechaEntrada(LocalDate.of(2026, 10, 10));
        r.setFechaSalida(LocalDate.of(2026, 10, 12));
        r.setNumeroHuespedes(2);
        return r;
    }

    @Test
    void crear_sinUsuario_usuarioNulo() {
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)));

        service.crear(requestEntrada());

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertNull(captor.getValue().getUsuario());
    }

    @Test
    void crear_porUsuarioId() {
        Usuario u = usuario(9L, "ana@x.com");
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(u));
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)));

        ReservaRequest request = requestEntrada();
        request.setUsuarioId(9L);

        service.crear(request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(u, captor.getValue().getUsuario());
        assertEquals(ReservaEstado.CONFIRMADA, captor.getValue().getEstado());
        assertTrue(captor.getValue().isVisible());
        assertEquals(LocalDate.now(), captor.getValue().getFechaReserva());
        verify(auditoriaService).registrar(AuditoriaTipoAccion.CREAR, "Reserva", 21L);
    }

    @Test
    void crear_usuarioInexistente404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ReservaRequest request = requestEntrada();
        request.setUsuarioId(99L);

        assertThrows(EntidadNoEncontradaException.class, () -> service.crear(request));
    }

    @Test
    void crear_inlineNuevoCreaUsuarioYAuda() {
        when(usuarioRepository.findByCorreoIgnoreCase("nuevo@x.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setId(3L);
            return u;
        });
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)));

        ReservaRequest.UsuarioReserva inline = new ReservaRequest.UsuarioReserva();
        inline.setNombre("Ana");
        inline.setApellido("Lopez");
        inline.setCorreo("nuevo@x.com");
        ReservaRequest request = requestEntrada();
        request.setUsuario(inline);

        service.crear(request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(3L, captor.getValue().getUsuario().getId());
        verify(auditoriaService).registrar(AuditoriaTipoAccion.CREAR, "Usuario", 3L);
        verify(auditoriaService).registrar(AuditoriaTipoAccion.CREAR, "Reserva", 21L);
    }

    @Test
    void crear_inlineCorreoExistenteReutiliza() {
        Usuario u = usuario(4L, "ana@x.com");
        when(usuarioRepository.findByCorreoIgnoreCase("ana@x.com")).thenReturn(Optional.of(u));
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)));

        ReservaRequest.UsuarioReserva inline = new ReservaRequest.UsuarioReserva();
        inline.setNombre("Ana");
        inline.setApellido("Lopez");
        inline.setCorreo("ana@x.com");
        ReservaRequest request = requestEntrada();
        request.setUsuario(inline);

        service.crear(request);

        verify(usuarioRepository, never()).save(any());
        verify(auditoriaService, never())
                .registrar(eq(AuditoriaTipoAccion.CREAR), eq("Usuario"), anyLong());
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(4L, captor.getValue().getUsuario().getId());
    }

    @Test
    void crear_calculaPrecioPrecision() {
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 13)));

        ReservaRequest request = requestEntrada();
        request.setFechaSalida(LocalDate.of(2026, 10, 13));

        service.crear(request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("150.00").compareTo(captor.getValue().getPrecio()));
        assertEquals(2, captor.getValue().getPrecio().scale());
    }

    @Test
    void crear_fechasInvalidas() {
        ReservaRequest request = requestEntrada();
        request.setFechaEntrada(LocalDate.of(2026, 10, 10));
        request.setFechaSalida(LocalDate.of(2026, 10, 10));

        assertThrows(FechasInvalidasException.class, () -> service.crear(request));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_duracionFueraDeRango() {
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11)));

        ReservaRequest request = requestEntrada();
        request.setFechaSalida(LocalDate.of(2026, 10, 11));
        assertThrows(DuracionEstanciaInvalidaException.class, () -> service.crear(request));
    }

    @Test
    void crear_huespedesLimite1Y10Permitidos() {
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenAnswer(i -> reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)));

        ReservaRequest uno = requestEntrada();
        uno.setNumeroHuespedes(1);
        service.crear(uno);

        ReservaRequest diez = requestEntrada();
        diez.setNumeroHuespedes(10);
        service.crear(diez);

        verify(reservaRepository, org.mockito.Mockito.times(2)).save(any(Reserva.class));
    }

    @Test
    void crear_huespedesFueraDeRango() {
        ReservaRequest cero = requestEntrada();
        cero.setNumeroHuespedes(0);
        assertThrows(NumeroHuespedesInvalidoException.class, () -> service.crear(cero));

        ReservaRequest once = requestEntrada();
        once.setNumeroHuespedes(11);
        assertThrows(NumeroHuespedesInvalidoException.class, () -> service.crear(once));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_solapamientoRechaza() {
        when(reservaRepository.existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoIn(
                any(), any(), anyList())).thenReturn(true);
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)));

        assertThrows(SolapamientoReservaException.class, () -> service.crear(requestEntrada()));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crear_estadoCanceladaInicialAceptado() {
        Reserva entidad = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        entidad.setEstado(ReservaEstado.CANCELADA);
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(entidad);

        ReservaRequest request = requestEntrada();
        request.setEstado(ReservaEstado.CANCELADA);

        service.crear(request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(ReservaEstado.CANCELADA, captor.getValue().getEstado());
    }

    @Test
    void crear_retroactivaPermitida() {
        when(reservaMapper.toEntity(any(ReservaRequest.class))).thenReturn(reservaBase(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 12)));

        ReservaRequest request = requestEntrada();
        request.setFechaEntrada(LocalDate.of(2025, 1, 10));
        request.setFechaSalida(LocalDate.of(2025, 1, 12));

        service.crear(request);

        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void modificar_parcialSoloCampoPresente() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setPrecio(new BigDecimal("100.00"));
        existente.setNumeroHuespedes(2);
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));

        ReservaUpdateRequest request = new ReservaUpdateRequest();
        request.setNumeroHuespedes(5);

        service.modificar(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getNumeroHuespedes());
        assertEquals(0, new BigDecimal("100.00").compareTo(captor.getValue().getPrecio()));
        verify(auditoriaService).registrar(AuditoriaTipoAccion.MODIFICAR, "Reserva", 21L);
    }

    @Test
    void modificar_solapeConsigoMismoPermitido() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setPrecio(new BigDecimal("100.00"));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));
        when(reservaRepository.existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoInAndIdNot(
                any(), any(), anyList(), anyLong())).thenReturn(false);

        ReservaUpdateRequest request = new ReservaUpdateRequest();
        request.setFechaEntrada(LocalDate.of(2026, 11, 1));
        request.setFechaSalida(LocalDate.of(2026, 11, 3));

        service.modificar(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(LocalDate.of(2026, 11, 1), captor.getValue().getFechaEntrada());
        assertEquals(LocalDate.of(2026, 11, 3), captor.getValue().getFechaSalida());
    }

    @Test
    void modificar_duracionConformeConservadaNoSeValida() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11));
        existente.setPrecio(new BigDecimal("50.00"));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));
        when(reservaRepository.existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoInAndIdNot(
                any(), any(), anyList(), anyLong())).thenReturn(false);

        ReservaUpdateRequest request = new ReservaUpdateRequest();
        request.setFechaEntrada(LocalDate.of(2026, 11, 1));
        request.setFechaSalida(LocalDate.of(2026, 11, 2));

        service.modificar(21L, request);

        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void modificar_duracionCambiaNoConformeRechaza() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));

        ReservaUpdateRequest request = new ReservaUpdateRequest();
        request.setFechaEntrada(LocalDate.of(2026, 10, 10));
        request.setFechaSalida(LocalDate.of(2026, 10, 11));

        assertThrows(DuracionEstanciaInvalidaException.class, () -> service.modificar(21L, request));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void modificar_recargaPrecioAlCambiarFechas() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setPrecio(new BigDecimal("100.00"));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));
        when(reservaRepository.existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoInAndIdNot(
                any(), any(), anyList(), anyLong())).thenReturn(false);

        ReservaUpdateRequest request = new ReservaUpdateRequest();
        request.setFechaEntrada(LocalDate.of(2026, 10, 10));
        request.setFechaSalida(LocalDate.of(2026, 10, 15));

        service.modificar(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("250.00").compareTo(captor.getValue().getPrecio()));
    }

    @Test
    void modificar_usuarioNullDesvincula() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setUsuario(usuario(1L, "ana@x.com"));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));

        ReservaUpdateRequest request = new ReservaUpdateRequest();
        request.setUsuarioPresente(true);
        request.setUsuario(null);

        service.modificar(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertNull(captor.getValue().getUsuario());
    }

    @Test
    void modificar_usuarioIdReutiliza() {
        Usuario u = usuario(6L, "otra@x.com");
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(u));

        ReservaUpdateRequest request = new ReservaUpdateRequest();
        request.setUsuarioId(6L);

        service.modificar(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(u, captor.getValue().getUsuario());
    }

    @Test
    void modificar_404() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntidadNoEncontradaException.class,
                () -> service.modificar(99L, new ReservaUpdateRequest()));
    }

    @Test
    void cambiarEstado_transicionLibreSinRecalculo() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setEstado(ReservaEstado.PENDIENTE);
        existente.setPrecio(new BigDecimal("100.00"));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));

        ReservaCambioEstadoRequest request = new ReservaCambioEstadoRequest();
        request.setEstado(ReservaEstado.CONFIRMADA);

        service.cambiarEstado(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(ReservaEstado.CONFIRMADA, captor.getValue().getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(captor.getValue().getPrecio()));
        verify(auditoriaService).registrar(AuditoriaTipoAccion.CAMBIAR_ESTADO, "Reserva", 21L);
    }

    @Test
    void cambiarEstado_reactivaCanceladaSinSolape() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setEstado(ReservaEstado.CANCELADA);
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));
        when(reservaRepository.existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoInAndIdNot(
                any(), any(), anyList(), anyLong())).thenReturn(false);

        ReservaCambioEstadoRequest request = new ReservaCambioEstadoRequest();
        request.setEstado(ReservaEstado.PENDIENTE);

        service.cambiarEstado(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(ReservaEstado.PENDIENTE, captor.getValue().getEstado());
    }

    @Test
    void cambiarEstado_reactivaCanceladaConSolapeRechaza() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setEstado(ReservaEstado.CANCELADA);
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));
        when(reservaRepository.existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoInAndIdNot(
                any(), any(), anyList(), anyLong())).thenReturn(true);

        ReservaCambioEstadoRequest request = new ReservaCambioEstadoRequest();
        request.setEstado(ReservaEstado.CONFIRMADA);

        assertThrows(SolapamientoReservaException.class, () -> service.cambiarEstado(21L, request));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cambiarVisible_rechazaActivaFutura() {
        Reserva existente = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        existente.setEstado(ReservaEstado.CONFIRMADA);
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));

        ReservaVisibleRequest request = new ReservaVisibleRequest();
        request.setVisible(false);

        assertThrows(OcultacionInvalidaException.class, () -> service.cambiarVisible(21L, request));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cambiarVisible_ocultaCanceladaYPasada() {
        Reserva cancelada = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        cancelada.setEstado(ReservaEstado.CANCELADA);
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(cancelada));

        ReservaVisibleRequest request = new ReservaVisibleRequest();
        request.setVisible(false);
        service.cambiarVisible(21L, request);

        Reserva pasada = reservaBase(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));
        pasada.setEstado(ReservaEstado.CONFIRMADA);
        when(reservaRepository.findById(22L)).thenReturn(Optional.of(pasada));
        service.cambiarVisible(22L, request);

        verify(reservaRepository, org.mockito.Mockito.times(2)).save(any(Reserva.class));
        verify(auditoriaService, org.mockito.Mockito.times(2))
                .registrar(eq(AuditoriaTipoAccion.ELIMINAR_OCULTAR), eq("Reserva"), anyLong());
    }

    @Test
    void cambiarVisible_mostrarSinRestriccion() {
        Reserva activa = reservaBase(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12));
        activa.setEstado(ReservaEstado.CONFIRMADA);
        activa.setVisible(false);
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(activa));

        ReservaVisibleRequest request = new ReservaVisibleRequest();
        request.setVisible(true);

        service.cambiarVisible(21L, request);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertTrue(captor.getValue().isVisible());
    }

    @Test
    void cambiarVisible_404() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        ReservaVisibleRequest request = new ReservaVisibleRequest();
        request.setVisible(true);

        assertThrows(EntidadNoEncontradaException.class, () -> service.cambiarVisible(99L, request));
    }

    @Test
    void listar_ordenaFechaEntradaAscPorDefecto() {
        PageImpl<Reserva> pagina = new PageImpl<>(List.of());
        when(reservaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        service.listar(null, null, null, null, null, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservaRepository).findAll(any(Specification.class), captor.capture());
        assertTrue(captor.getValue().getSort().getOrderFor("fechaEntrada").isAscending());
    }

    @Test
    void listar_conFiltrosUsaSpecification() {
        PageImpl<Reserva> pagina = new PageImpl<>(List.of());
        when(reservaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

        service.listar(ReservaEstado.CONFIRMADA, false, 5L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), PageRequest.of(0, 20));

        verify(reservaRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}