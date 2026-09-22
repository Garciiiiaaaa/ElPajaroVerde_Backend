package es.elpajaroverde;

import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.models.Auditoria;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConfiguracionIntegrationTest extends BaseIntegracion {

    @Test
    void getConfiguracionExistente() throws Exception {
        mockMvc.perform(get("/api/v1/configuracion").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioNoche").value(50.00))
                .andExpect(jsonPath("$.estanciaMinima").value(2))
                .andExpect(jsonPath("$.estanciaMaxima").value(30));
    }

    @Test
    void getSinFilaDevuelve404() throws Exception {
        configuracionRepository.deleteAll();

        mockMvc.perform(get("/api/v1/configuracion").header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Configuración no encontrada"));
    }

    @Test
    void actualizarParcialSoloCamposPresentes() throws Exception {
        String body = "{\"estanciaMinima\":3}";

        mockMvc.perform(patch("/api/v1/configuracion")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioNoche").value(50.00))
                .andExpect(jsonPath("$.estanciaMinima").value(3))
                .andExpect(jsonPath("$.estanciaMaxima").value(30));

        assertEquals(3, configuracionRepository.findById(1L).orElseThrow().getEstanciaMinima());
        List<Auditoria> auditorias = auditoriaRepository.findAll().stream()
                .filter(a -> a.getTipoAccion() == AuditoriaTipoAccion.MODIFICAR).toList();
        assertEquals(1, auditorias.size());
        assertEquals("Modificación de Configuracion (id=1)", auditorias.get(0).getDescripcion());
    }

    @Test
    void actualizarReglaCruzadaMinimaMayorQueMaxima() throws Exception {
        String body = "{\"estanciaMinima\":10,\"estanciaMaxima\":5}";

        mockMvc.perform(patch("/api/v1/configuracion")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La configuración no es válida"));

        assertEquals(2, configuracionRepository.findById(1L).orElseThrow().getEstanciaMinima());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/configuracion"))
                .andExpect(status().isUnauthorized());
    }
}