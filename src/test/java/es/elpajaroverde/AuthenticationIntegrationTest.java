package es.elpajaroverde;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.repositories.AdministradorRepository;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.services.ILoginAttemptTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ILoginAttemptTracker loginAttemptTracker;

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        administradorRepository.deleteAll();
        loginAttemptTracker.clearAttempts("admin");
        Administrador admin = new Administrador("admin", passwordEncoder.encode("pass123"), "admin@test.com");
        administradorRepository.save(admin);

        configuracionRepository.deleteAll();
        configuracionRepository.save(new Configuracion(new BigDecimal("50.00"), 2, 30));
    }

    @Test
    void login_success_andAccessPrivateEndpoint() throws Exception {
        String loginJson = objectMapper.writeValueAsString(
                new es.elpajaroverde.dtos.LoginRequest("admin", "pass123"));

        String responseBody = mockMvc.perform(post("/api/v1/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(responseBody).get("token").asText();

        mockMvc.perform(get("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void login_failed5Times_accountBlocked() throws Exception {
        String wrongLoginJson = objectMapper.writeValueAsString(
                new es.elpajaroverde.dtos.LoginRequest("admin", "wrong"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/sesion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongLoginJson))
                    .andExpect(status().isUnauthorized());
        }

        String correctLoginJson = objectMapper.writeValueAsString(
                new es.elpajaroverde.dtos.LoginRequest("admin", "pass123"));
        mockMvc.perform(post("/api/v1/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctLoginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_afterLockoutExpired_allowsAccess() throws Exception {
        String wrongLoginJson = objectMapper.writeValueAsString(
                new es.elpajaroverde.dtos.LoginRequest("admin", "wrong"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/sesion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongLoginJson))
                    .andExpect(status().isUnauthorized());
        }

        loginAttemptTracker.clearAttempts("admin");

        String correctLoginJson = objectMapper.writeValueAsString(
                new es.elpajaroverde.dtos.LoginRequest("admin", "pass123"));
        mockMvc.perform(post("/api/v1/sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctLoginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
