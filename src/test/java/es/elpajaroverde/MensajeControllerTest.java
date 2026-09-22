package es.elpajaroverde;

import es.elpajaroverde.dtos.MensajeRequest;
import es.elpajaroverde.dtos.MensajeResponse;
import es.elpajaroverde.enums.MensajeRemitente;
import es.elpajaroverde.services.interfaces.IMensajeService;
import es.elpajaroverde.support.BaseIntegracion;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MensajeControllerTest extends BaseIntegracion {

    @MockitoBean
    private IMensajeService mensajeService;

    @Test
    void envioPublicoDevuelve201() throws Exception {
        MensajeResponse respuesta = new MensajeResponse();
        respuesta.setId(1L);
        respuesta.setAsunto("Consulta");
        respuesta.setMensaje("Hola");
        respuesta.setRemitente(MensajeRemitente.USUARIO);
        respuesta.setFechaMensaje(LocalDateTime.now());
        when(mensajeService.enviar(any(MensajeRequest.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cli@x.com\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.asunto").value("Consulta"));
    }

    @Test
    void envioInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/mensajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"no-email\",\"mensaje\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"));
    }

    @Test
    void listadoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/mensajes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchYDeleteSobreMensajeDevuelven404() throws Exception {
        mockMvc.perform(patch("/api/v1/mensajes/1")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/mensajes/1")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listadoPrivadoFuncionaConToken() throws Exception {
        MensajeResponse respuesta = new MensajeResponse();
        respuesta.setId(7L);
        respuesta.setAsunto("Consulta");
        respuesta.setFechaMensaje(LocalDateTime.now());
        respuesta.setRemitente(MensajeRemitente.USUARIO);
        when(mensajeService.listar(isNull(), isNull(), isNull(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(respuesta)));

        mockMvc.perform(get("/api/v1/mensajes").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7L));
    }
}