package es.elpajaroverde.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.repositories.AdministradorRepository;
import es.elpajaroverde.repositories.AuditoriaRepository;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.repositories.MensajeRepository;
import es.elpajaroverde.repositories.ReservaRepository;
import es.elpajaroverde.repositories.UsuarioRepository;
import es.elpajaroverde.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

/**
 * Base compartida para los tests de integración y de controller (T11.1).
 * Siembra el admin y la fila singleton de Configuracion(id=1) exigida por el lock
 * pesimista y el cálculo de precio, y expone un token JWT válido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegracion {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AdministradorRepository administradorRepository;

    @Autowired
    protected ConfiguracionRepository configuracionRepository;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected ReservaRepository reservaRepository;

    @Autowired
    protected MensajeRepository mensajeRepository;

    @Autowired
    protected AuditoriaRepository auditoriaRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtUtil jwtUtil;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void prepararDatos() {
        auditoriaRepository.deleteAll();
        mensajeRepository.deleteAll();
        reservaRepository.deleteAll();
        usuarioRepository.deleteAll();
        administradorRepository.deleteAll();
        configuracionRepository.deleteAll();

        administradorRepository.save(new Administrador(
                "admin", passwordEncoder.encode("pass123"), "admin@test.com"));
        configuracionRepository.save(new Configuracion(new BigDecimal("50.00"), 2, 30));
    }

    protected String bearerToken() {
        return "Bearer " + jwtUtil.generateToken("admin");
    }
}