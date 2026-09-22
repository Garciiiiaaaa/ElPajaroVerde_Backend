package es.elpajaroverde;

import es.elpajaroverde.dtos.ConfiguracionRequest;
import es.elpajaroverde.dtos.ConfiguracionResponse;
import es.elpajaroverde.exceptions.ConfiguracionInvalidaException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.services.interfaces.IConfiguracionService;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfiguracionControllerTest extends BaseIntegracion {

    @MockitoBean
    private IConfiguracionService configuracionService;

    @Test
    void sinonTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/configuracion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerDevuelve200() throws Exception {
        when(configuracionService.obtener()).thenReturn(configuracionRespuesta());

        mockMvc.perform(get("/api/v1/configuracion").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioNoche").value(50.00))
                .andExpect(jsonPath("$.estanciaMinima").value(2))
                .andExpect(jsonPath("$.estanciaMaxima").value(30));
    }

    @Test
    void obtenerSinFilaDevuelve404() throws Exception {
        when(configuracionService.obtener())
                .thenThrow(new EntidadNoEncontradaException("Configuración no encontrada"));

        mockMvc.perform(get("/api/v1/configuracion").header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Configuración no encontrada"));
    }

    @Test
    void actualizarDevuelve200() throws Exception {
        when(configuracionService.actualizar(any(ConfiguracionRequest.class)))
                .thenReturn(configuracionRespuesta());

        mockMvc.perform(patch("/api/v1/configuracion")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estanciaMinima\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estanciaMinima").value(2));
    }

    @Test
    void actualizarDatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/configuracion")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estanciaMinima\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"));
    }

    @Test
    void actualizarReglaCruzadaInvalidaDevuelve400() throws Exception {
        when(configuracionService.actualizar(any(ConfiguracionRequest.class)))
                .thenThrow(new ConfiguracionInvalidaException());

        mockMvc.perform(patch("/api/v1/configuracion")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estanciaMinima\":10,\"estanciaMaxima\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La configuración no es válida"));
    }

    private ConfiguracionResponse configuracionRespuesta() {
        ConfiguracionResponse r = new ConfiguracionResponse();
        r.setPrecioNoche(new BigDecimal("50.00"));
        r.setEstanciaMinima(2);
        r.setEstanciaMaxima(30);
        return r;
    }
}