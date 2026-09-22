package es.elpajaroverde;

import es.elpajaroverde.dtos.FechasOcupadas;
import es.elpajaroverde.dtos.FechasOcupadasResponse;
import es.elpajaroverde.services.interfaces.IDisponibilidadService;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisponibilidadControllerTest extends BaseIntegracion {

    @MockitoBean
    private IDisponibilidadService disponibilidadService;

    @Test
    void consultaPublicaDevuelveFechasOcupadas() throws Exception {
        when(disponibilidadService.obtenerFechasOcupadas()).thenReturn(new FechasOcupadasResponse(
                List.of(new FechasOcupadas(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15)))));

        mockMvc.perform(get("/api/v1/disponibilidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupadas").isArray())
                .andExpect(jsonPath("$.ocupadas.length()").value(1))
                .andExpect(jsonPath("$.ocupadas[0].fechaEntrada").value("2026-10-10"))
                .andExpect(jsonPath("$.ocupadas[0].fechaSalida").value("2026-10-15"))
                .andExpect(jsonPath("$.ocupadas[0].id").doesNotExist());
    }

    @Test
    void consultaSinReservasDevuelveListaVacia() throws Exception {
        when(disponibilidadService.obtenerFechasOcupadas())
                .thenReturn(new FechasOcupadasResponse(List.of()));

        mockMvc.perform(get("/api/v1/disponibilidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupadas").isEmpty());
    }
}