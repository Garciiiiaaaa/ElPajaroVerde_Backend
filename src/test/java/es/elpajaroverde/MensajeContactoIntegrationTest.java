package es.elpajaroverde;

import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MensajeContactoIntegrationTest extends BaseIntegracion {

    @Test
    void envioPublicoCreaUsuarioConPlaceholdersYAsuntoPorDefecto() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nuevo@x.com\",\"nombre\":\"\",\"apellido\":\"Perez\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.asunto").value("Consulta"))
                .andExpect(jsonPath("$.usuario.correo").value("nuevo@x.com"));

        assertEquals(1, usuarioRepository.findAll().size());
        Usuario usuario = usuarioRepository.findAll().get(0);
        assertEquals("Sin nombre", usuario.getNombre());
        assertEquals("Perez", usuario.getApellido());
    }

    @Test
    void envioUsaAsuntoProporcionado() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"con@x.com\",\"asunto\":\"Precios\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.asunto").value("Precios"));
    }

    @Test
    void correoDuplicadoReutilizaUsuarioSinDuplicar() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"U@x.com\",\"nombre\":\"Ana\",\"mensaje\":\"primero\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"u@x.com\",\"nombre\":\"Otro\",\"mensaje\":\"segundo\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, usuarioRepository.findAll().size());
        assertEquals(2, mensajeRepository.findAll().size());
    }

    @Test
    void mensajeInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-email\",\"mensaje\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listadoPrivadoFiltraPorUsuarioYFechas() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cli@x.com\",\"mensaje\":\"a\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cli@x.com\",\"mensaje\":\"b\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"otro@x.com\",\"mensaje\":\"c\"}"))
                .andExpect(status().isCreated());

        Long usuarioId = usuarioRepository.findAll().get(0).getId();
        LocalDateTime desde = LocalDateTime.now().minusHours(1);
        LocalDateTime hasta = LocalDateTime.now().plusHours(1);

        mockMvc.perform(get("/api/v1/mensajes")
                        .header("Authorization", bearerToken())
                        .queryParam("usuarioId", String.valueOf(usuarioId))
                        .queryParam("fechaDesde", desde.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .queryParam("fechaHasta", hasta.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].usuario.correo").value("cli@x.com"));
    }

    @Test
    void listadoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/mensajes"))
                .andExpect(status().isUnauthorized());
    }
}