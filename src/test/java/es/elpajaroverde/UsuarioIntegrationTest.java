package es.elpajaroverde;

import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioIntegrationTest extends BaseIntegracion {

    @Test
    void crearUsuarioExitoso() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"ana@x.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.correo").value("ana@x.com"))
                .andExpect(jsonPath("$.visible").value(true));
    }

    @Test
    void crearCorreoDuplicadoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"ana@x.com\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\",\"apellido\":\"Garcia\",\"correo\":\"ANA@x.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un usuario con ese correo"));
    }

    @Test
    void crearSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"ana@x.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modificarCorreoDuplicadoDevuelve409PeroSelfPermitido() throws Exception {
        Long id = crear("Ana", "ana@x.com");
        Long otroId = crear("Luis", "luis@x.com");

        mockMvc.perform(patch("/api/v1/usuarios/" + id)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"luis@x.com\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/usuarios/" + id)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@x.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));

        mockMvc.perform(patch("/api/v1/usuarios/" + otroId)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luis Enrique\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Luis Enrique"))
                .andExpect(jsonPath("$.correo").value("luis@x.com"));
    }

    @Test
    void cambiarVisibleAfectaAlListadoPorDefecto() throws Exception {
        Long id = crear("Ocultable", "oculta@x.com");

        mockMvc.perform(patch("/api/v1/usuarios/" + id + "/visible")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));

        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .queryParam("visible", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].correo").value("oculta@x.com"));
    }

    @Test
    void listarFiltraPorNombreYPagina() throws Exception {
        crear("Alba", "alba@x.com");
        crear("Alberto", "berto@x.com");
        crear("Carla", "carla@x.com");

        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .queryParam("nombre", "Al")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .queryParam("nombre", "Al")
                        .queryParam("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    private Long crear(String nombre, String correo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"apellido\":\"Ap\",\"correo\":\"" + correo + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}