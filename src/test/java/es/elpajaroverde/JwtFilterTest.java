package es.elpajaroverde;

import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.repositories.AdministradorRepository;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Date;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    @BeforeEach
    void setUp() {
        administradorRepository.deleteAll();
        Administrador admin = new Administrador("testadmin", passwordEncoder.encode("pass123"), "test@test.com");
        administradorRepository.save(admin);

        configuracionRepository.deleteAll();
        configuracionRepository.save(new Configuracion(new BigDecimal("50.00"), 2, 30));
    }

    private String generateExpiredToken(String username) throws Exception {
        Field keyField = JwtUtil.class.getDeclaredField("signingKey");
        keyField.setAccessible(true);
        SecretKey key = (SecretKey) keyField.get(jwtUtil);

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now - 7200000))
                .expiration(new Date(now - 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void privateEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/configuracion"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token no proporcionado"));
    }

    @Test
    void privateEndpoint_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/configuracion")
                        .header("Authorization", "Bearer invalidtoken123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token inválido"));
    }

    @Test
    void privateEndpoint_expiredToken_returns401() throws Exception {
        String expiredToken = generateExpiredToken("testadmin");

        mockMvc.perform(get("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token expirado"));
    }

    @Test
    void privateEndpoint_validToken_returns200() throws Exception {
        String token = jwtUtil.generateToken("testadmin");

        mockMvc.perform(get("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void privateEndpoint_tokenInvalidatedByLogout_returns401() throws Exception {
        String token = jwtUtil.generateToken("testadmin");
        jwtUtil.rotateKey();

        mockMvc.perform(get("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteSesion_noToken_returns200Silently() throws Exception {
        mockMvc.perform(delete("/api/v1/sesion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sesión cerrada"));
    }
}
