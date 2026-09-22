package es.elpajaroverde;

import com.fasterxml.jackson.databind.JsonNode;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.models.Auditoria;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditoriaIntegrationTest extends BaseIntegracion {

    @Test
    void mensajePublicoRegistraAuditoriaConAdminNull() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cliente@x.com\",\"mensaje\":\"Hola, quiero reservar\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, auditoriaRepository.count());
        Auditoria auditoria = auditoriaRepository.findAll().get(0);
        assertEquals(AuditoriaTipoAccion.CREAR, auditoria.getTipoAccion());
        assertEquals("Usuario", auditoria.getEntidadAfectada());
        assertTrue(auditoria.getDescripcion().startsWith("Creación de Usuario (id="));
        assertEquals(null, auditoria.getAdmin());
    }

    @Test
    void accionPrivadaRegistraAuditoriaConAdminAutenticado() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"ana@x.com\"}"))
                .andExpect(status().isCreated());

        Auditoria auditoria = auditoriaRepository.findAll().get(0);
        assertEquals("Usuario", auditoria.getEntidadAfectada());
        assertEquals(AuditoriaTipoAccion.CREAR, auditoria.getTipoAccion());
        assertEquals("admin", auditoria.getAdmin().getNombreUsuario());
    }

    @Test
    void soloLecturaPostDevuelve405() throws Exception {
        mockMvc.perform(post("/api/v1/auditoria").header("Authorization", bearerToken()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void listadoAplicaFiltrosYOrdenDescendentePorFecha() throws Exception {
        Administrador admin = administradorRepository.findAll().get(0);
        auditoriaRepository.save(new Auditoria(AuditoriaTipoAccion.CREAR,
                LocalDateTime.now().minusMinutes(10), admin, "Reserva", 1L,
                "Creación de Reserva (id=1)"));
        auditoriaRepository.save(new Auditoria(AuditoriaTipoAccion.CAMBIAR_ESTADO,
                LocalDateTime.now(), admin, "Reserva", 1L,
                "Cambio de estado de Reserva (id=1)"));
        auditoriaRepository.save(new Auditoria(AuditoriaTipoAccion.CREAR,
                LocalDateTime.now().minusMinutes(5), admin, "Usuario", 9L,
                "Creación de Usuario (id=9)"));

        String body = mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", bearerToken())
                        .queryParam("entidadAfectada", "Reserva")
                        .queryParam("tipoAccion", "CREAR"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(body).get("content");
        assertEquals(1, content.size());
        assertEquals("Reserva", content.get(0).get("entidadAfectada").asText());
        assertEquals(AuditoriaTipoAccion.CREAR.name(), content.get(0).get("tipoAccion").asText());
        assertFalse(content.get(0).get("admin").isNull());

        String todos = mockMvc.perform(get("/api/v1/auditoria").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode contenido = objectMapper.readTree(todos).get("content");
        assertEquals(3, contenido.size());
        LocalDateTime primera = LocalDateTime.parse(contenido.get(0).get("fecha").asText());
        LocalDateTime segunda = LocalDateTime.parse(contenido.get(1).get("fecha").asText());
        assertTrue(primera.isAfter(segunda) || primera.isEqual(segunda));
    }

    @Test
    void listadoFiltraPorEntidadId() throws Exception {
        Administrador admin = administradorRepository.findAll().get(0);
        auditoriaRepository.save(new Auditoria(AuditoriaTipoAccion.CREAR,
                LocalDateTime.now(), admin, "Reserva", 41L, "Creación de Reserva (id=41)"));
        auditoriaRepository.save(new Auditoria(AuditoriaTipoAccion.CREAR,
                LocalDateTime.now(), admin, "Reserva", 42L, "Creación de Reserva (id=42)"));

        String body = mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", bearerToken())
                        .queryParam("entidadId", "42"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(body).get("content");
        assertEquals(1, content.size());
        assertEquals(42L, content.get(0).get("entidadId").asLong());
    }

    @Test
    void listadoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria"))
                .andExpect(status().isUnauthorized());
    }
}