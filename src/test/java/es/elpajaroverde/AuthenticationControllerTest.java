package es.elpajaroverde;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.elpajaroverde.config.GlobalExceptionHandler;
import es.elpajaroverde.controllers.AuthenticationController;
import es.elpajaroverde.dtos.LoginRequest;
import es.elpajaroverde.dtos.LoginResponse;
import es.elpajaroverde.exceptions.CredencialesInvalidasException;
import es.elpajaroverde.services.IAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private IAuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthenticationController controller = new AuthenticationController(authenticationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("jwt-token-123"));

        mockMvc.perform(post("/api/v1/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"));
    }

    @Test
    void login_emptyFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"\",\"contrasena\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error de validación"));
    }

    @Test
    void login_wrongCredentials_returns401() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new CredencialesInvalidasException("Credenciales incorrectas"));

        mockMvc.perform(post("/api/v1/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }

    @Test
    void logout_noToken_returns200Silently() throws Exception {
        when(authenticationService.logout(null))
                .thenReturn(new es.elpajaroverde.dtos.AuthErrorResponse("Sesión cerrada"));

        mockMvc.perform(delete("/api/v1/sesion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sesión cerrada"));
    }
}
