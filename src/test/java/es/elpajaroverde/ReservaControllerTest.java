package es.elpajaroverde;

import es.elpajaroverde.dtos.ReservaCambioEstadoRequest;
import es.elpajaroverde.dtos.ReservaRequest;
import es.elpajaroverde.dtos.ReservaResponse;
import es.elpajaroverde.dtos.ReservaUpdateRequest;
import es.elpajaroverde.dtos.ReservaVisibleRequest;
import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.exceptions.DuracionEstanciaInvalidaException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.exceptions.SolapamientoReservaException;
import es.elpajaroverde.services.interfaces.IReservaService;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservaControllerTest extends BaseIntegracion {

    @MockitoBean
    private IReservaService reservaService;

    @Test
    void crearReservaDevuelve201() throws Exception {
        when(reservaService.crear(any(ReservaRequest.class))).thenReturn(reservaRespuesta(1L));

        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-12\","
                                + "\"numeroHuespedes\":2,\"usuario\":{\"nombre\":\"Ana\",\"apellido\":\"Lopez\","
                                + "\"correo\":\"a@x.com\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void crearReservaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-12\","
                                + "\"numeroHuespedes\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"));
    }

    @Test
    void crearReservaSolapadaDevuelve409() throws Exception {
        when(reservaService.crear(any(ReservaRequest.class)))
                .thenThrow(new SolapamientoReservaException());

        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-12\","
                                + "\"numeroHuespedes\":2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void crearReservaDuracionInvalidaDevuelve400() throws Exception {
        when(reservaService.crear(any(ReservaRequest.class)))
                .thenThrow(new DuracionEstanciaInvalidaException());

        mockMvc.perform(post("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaEntrada\":\"2026-10-10\",\"fechaSalida\":\"2026-10-11\","
                                + "\"numeroHuespedes\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificarReservaDevuelve200() throws Exception {
        when(reservaService.modificar(eq(1L), any(ReservaUpdateRequest.class)))
                .thenReturn(reservaRespuesta(1L));

        mockMvc.perform(patch("/api/v1/reservas/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroHuespedes\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void modificarReservaInexistenteDevuelve404() throws Exception {
        when(reservaService.modificar(eq(999L), any(ReservaUpdateRequest.class)))
                .thenThrow(new EntidadNoEncontradaException("Reserva no encontrada"));

        mockMvc.perform(patch("/api/v1/reservas/999")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroHuespedes\":3}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Reserva no encontrada"));
    }

    @Test
    void cambiarEstadoDevuelve200() throws Exception {
        when(reservaService.cambiarEstado(eq(1L), any(ReservaCambioEstadoRequest.class)))
                .thenReturn(reservaRespuesta(1L));

        mockMvc.perform(patch("/api/v1/reservas/1/estado")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/reservas/1/estado")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INEXISTENTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarVisibleDevuelve200() throws Exception {
        when(reservaService.cambiarVisible(eq(1L), any(ReservaVisibleRequest.class)))
                .thenReturn(reservaRespuesta(1L));

        mockMvc.perform(patch("/api/v1/reservas/1/visible")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/reservas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarDevuelvePagina() throws Exception {
        when(reservaService.listar(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reservaRespuesta(1L))));

        mockMvc.perform(get("/api/v1/reservas").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void listarClamaSizeInferiorA1() throws Exception {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(reservaService.listar(isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .queryParam("size", "0"))
                .andExpect(status().isOk());

        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    void listarClamaSizeSuperiorA100() throws Exception {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(reservaService.listar(isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/reservas")
                        .header("Authorization", bearerToken())
                        .queryParam("size", "200"))
                .andExpect(status().isOk());

        assertEquals(100, captor.getValue().getPageSize());
    }

    private ReservaResponse reservaRespuesta(Long id) {
        ReservaResponse r = new ReservaResponse();
        r.setId(id);
        r.setFechaReserva(LocalDate.now());
        r.setFechaEntrada(LocalDate.of(2026, 10, 10));
        r.setFechaSalida(LocalDate.of(2026, 10, 12));
        r.setNumeroHuespedes(2);
        r.setPrecio(new BigDecimal("100.00"));
        r.setEstado(ReservaEstado.CONFIRMADA);
        r.setVisible(true);
        return r;
    }
}