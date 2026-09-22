package es.elpajaroverde;

import es.elpajaroverde.dtos.FechasOcupadasResponse;
import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.mappers.DisponibilidadMapper;
import es.elpajaroverde.models.Reserva;
import es.elpajaroverde.repositories.ReservaRepository;
import es.elpajaroverde.services.DisponibilidadService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisponibilidadServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private DisponibilidadMapper disponibilidadMapper;

    private DisponibilidadService service;

    @BeforeEach
    void setUp() {
        service = new DisponibilidadService(reservaRepository, disponibilidadMapper);
    }

    @Test
    void obtenerFechasOcupadas_soloPendientesYConfirmadasFuturas() {
        LocalDate hoy = LocalDate.now();
        when(reservaRepository.findByEstadoInAndFechaSalidaGreaterThanEqual(any(), any())).thenReturn(List.of());

        service.obtenerFechasOcupadas();

        ArgumentCaptor<List<ReservaEstado>> estados = ArgumentCaptor.captor();
        ArgumentCaptor<LocalDate> fecha = ArgumentCaptor.forClass(LocalDate.class);
        verify(reservaRepository).findByEstadoInAndFechaSalidaGreaterThanEqual(estados.capture(), fecha.capture());
        assertEquals(List.of(ReservaEstado.PENDIENTE, ReservaEstado.CONFIRMADA), estados.getValue());
        assertEquals(hoy, fecha.getValue());
    }

    @Test
    void obtenerFechasOcupadas_mapeaSoloLasDosFechas() {
        Reserva reserva = new Reserva(LocalDate.now(),
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15),
                2, new BigDecimal("250.00"), ReservaEstado.CONFIRMADA, true, null);
        when(reservaRepository.findByEstadoInAndFechaSalidaGreaterThanEqual(any(), any())).thenReturn(List.of(reserva));
        when(disponibilidadMapper.toFechasOcupadas(reserva)).thenReturn(
                new es.elpajaroverde.dtos.FechasOcupadas(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15)));

        FechasOcupadasResponse respuesta = service.obtenerFechasOcupadas();

        assertNotNull(respuesta.getOcupadas());
        assertEquals(1, respuesta.getOcupadas().size());
        assertEquals(LocalDate.of(2026, 10, 10), respuesta.getOcupadas().get(0).getFechaEntrada());
        assertEquals(LocalDate.of(2026, 10, 15), respuesta.getOcupadas().get(0).getFechaSalida());
    }
}