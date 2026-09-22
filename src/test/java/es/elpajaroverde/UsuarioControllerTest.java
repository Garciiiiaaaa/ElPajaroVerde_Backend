package es.elpajaroverde;

import es.elpajaroverde.dtos.UsuarioRequest;
import es.elpajaroverde.dtos.UsuarioResponse;
import es.elpajaroverde.dtos.UsuarioUpdateRequest;
import es.elpajaroverde.dtos.UsuarioVisibleRequest;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.services.interfaces.IUsuarioService;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest extends BaseIntegracion {

    @MockitoBean
    private IUsuarioService usuarioService;

    @Test
    void crearUsuarioSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"ana@x.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearUsuarioConTokenDevuelve201() throws Exception {
        when(usuarioService.crear(any(UsuarioRequest.class))).thenReturn(usuarioRespuesta(1L, "Ana", true));

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"ana@x.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.visible").value(true));
    }

    @Test
    void crearUsuarioInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"));
    }

    @Test
    void modificarUsuarioDevuelve200() throws Exception {
        when(usuarioService.modificar(eq(5L), any(UsuarioUpdateRequest.class)))
                .thenReturn(usuarioRespuesta(5L, "Ana Updated", true));

        mockMvc.perform(patch("/api/v1/usuarios/5")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Updated"));
    }

    @Test
    void modificarUsuarioInexistenteDevuelve404() throws Exception {
        when(usuarioService.modificar(eq(999L), any(UsuarioUpdateRequest.class)))
                .thenThrow(new EntidadNoEncontradaException("Usuario no encontrado"));

        mockMvc.perform(patch("/api/v1/usuarios/999")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void cambiarVisibleDevuelve200() throws Exception {
        when(usuarioService.cambiarVisible(eq(3L), any(UsuarioVisibleRequest.class)))
                .thenReturn(usuarioRespuestaFalsa(3L, "Ana"));

        mockMvc.perform(patch("/api/v1/usuarios/3/visible")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));
    }

    @Test
    void listarUsuariosDevuelvePagina() throws Exception {
        when(usuarioService.listar(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(usuarioRespuesta(1L, "Ana", true))));

        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].nombre").value("Ana"));
    }

    private UsuarioResponse usuarioRespuesta(Long id, String nombre, boolean visible) {
        UsuarioResponse r = new UsuarioResponse();
        r.setId(id);
        r.setNombre(nombre);
        r.setApellido("Lopez");
        r.setCorreo("x@x.com");
        r.setVisible(visible);
        return r;
    }

    private UsuarioResponse usuarioRespuestaFalsa(Long id, String nombre) {
        UsuarioResponse r = usuarioRespuesta(id, nombre, true);
        r.setVisible(false);
        return r;
    }
}