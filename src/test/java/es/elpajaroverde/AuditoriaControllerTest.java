package es.elpajaroverde;

import es.elpajaroverde.dtos.AuditoriaResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.services.interfaces.IAuditoriaService;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditoriaControllerTest extends BaseIntegracion {

    @MockitoBean
    private IAuditoriaService auditoriaService;

    @Test
    void listadoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listadoConFiltrosDevuelveRegistros() throws Exception {
        when(auditoriaService.listar(eq("Reserva"), eq(1L), eq(AuditoriaTipoAccion.CREAR),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(auditoriaRespuesta())));

        mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", bearerToken())
                        .queryParam("entidadAfectada", "Reserva")
                        .queryParam("entidadId", "1")
                        .queryParam("tipoAccion", "CREAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entidadAfectada").value("Reserva"))
                .andExpect(jsonPath("$.content[0].tipoAccion").value("CREAR"))
                .andExpect(jsonPath("$.content[0].descripcion")
                        .value("Creación de Reserva (id=1)"));
    }

    @Test
    void listadoFiltraPorFechas() throws Exception {
        when(auditoriaService.listar(isNull(), isNull(), isNull(),
                any(), any(), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(auditoriaRespuesta())));

        mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", bearerToken())
                        .queryParam("fechaDesde", "2026-09-01T00:00:00")
                        .queryParam("fechaHasta", "2026-12-01T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void postSobreAuditoriaDevuelve405() throws Exception {
        mockMvc.perform(post("/api/v1/auditoria").header("Authorization", bearerToken()))
                .andExpect(status().isMethodNotAllowed());
    }

    private AuditoriaResponse auditoriaRespuesta() {
        AuditoriaResponse r = new AuditoriaResponse();
        r.setId(1L);
        r.setFecha(LocalDateTime.now());
        r.setAdmin("admin");
        r.setEntidadAfectada("Reserva");
        r.setEntidadId(1L);
        r.setTipoAccion(AuditoriaTipoAccion.CREAR);
        r.setDescripcion("Creación de Reserva (id=1)");
        return r;
    }
}