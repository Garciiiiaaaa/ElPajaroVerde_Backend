package es.elpajaroverde;

import es.elpajaroverde.dtos.LoginRequest;
import es.elpajaroverde.dtos.LoginResponse;
import es.elpajaroverde.exceptions.CredencialesInvalidasException;
import es.elpajaroverde.exceptions.CuentaBloqueadaException;
import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.repositories.AdministradorRepository;
import es.elpajaroverde.security.JwtUtil;
import es.elpajaroverde.services.AuthenticationService;
import es.elpajaroverde.services.interfaces.ILoginAttemptTracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AdministradorRepository administradorRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ILoginAttemptTracker loginAttemptTracker;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private Administrador admin;

    @BeforeEach
    void setUp() {
        admin = new Administrador("admin", "hashedPass", "admin@test.com");
    }

    @Test
    void login_success_returnsToken() {
        when(loginAttemptTracker.canLogin("admin")).thenReturn(true);
        when(administradorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password", "hashedPass")).thenReturn(true);
        when(jwtUtil.generateToken("admin")).thenReturn("jwt-token-123");

        LoginResponse response = authenticationService.login(new LoginRequest("admin", "password"));

        assertEquals("jwt-token-123", response.getToken());
        verify(loginAttemptTracker).clearAttempts("admin");
    }

    @Test
    void login_userNotFound_throwsException() {
        when(loginAttemptTracker.canLogin("admin")).thenReturn(true);
        when(administradorRepository.findByNombreUsuario("admin")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> authenticationService.login(new LoginRequest("admin", "password")));
        verify(loginAttemptTracker).registerFailedAttempt("admin");
    }

    @Test
    void login_wrongPassword_throwsException() {
        when(loginAttemptTracker.canLogin("admin")).thenReturn(true);
        when(administradorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "hashedPass")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class,
                () -> authenticationService.login(new LoginRequest("admin", "wrong")));
        verify(loginAttemptTracker).registerFailedAttempt("admin");
    }

    @Test
    void login_blockedAccount_throwsException() {
        when(loginAttemptTracker.canLogin("admin")).thenReturn(false);

        assertThrows(CuentaBloqueadaException.class,
                () -> authenticationService.login(new LoginRequest("admin", "password")));
    }

    @Test
    void login_afterLockoutExpired_allowsLogin() {
        when(loginAttemptTracker.canLogin("admin")).thenReturn(true);
        when(administradorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password", "hashedPass")).thenReturn(true);
        when(jwtUtil.generateToken("admin")).thenReturn("new-token");

        LoginResponse response = authenticationService.login(new LoginRequest("admin", "password"));
        assertNotNull(response.getToken());
    }

    @Test
    void login_correctPassword_clearsAttempts() {
        when(loginAttemptTracker.canLogin("admin")).thenReturn(true);
        when(administradorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password", "hashedPass")).thenReturn(true);
        when(jwtUtil.generateToken("admin")).thenReturn("token");

        authenticationService.login(new LoginRequest("admin", "password"));
        verify(loginAttemptTracker).clearAttempts("admin");
    }

    @Test
    void logout_validToken_rotatesKey() {
       when(jwtUtil.validateToken("valid-token")).thenReturn(true);

        authenticationService.logout("valid-token");
        verify(jwtUtil).rotateKey();
    }

    @Test
    void logout_nullToken_returnsSilently() {
        var response = authenticationService.logout(null);
        assertEquals("Sesión cerrada", response.getMessage());
        verify(jwtUtil, never()).rotateKey();
    }

    @Test
    void logout_expiredToken_returnsSilently() {
        when(jwtUtil.validateToken("expired-token")).thenReturn(false);

        var response = authenticationService.logout("expired-token");
        assertEquals("Sesión cerrada", response.getMessage());
        verify(jwtUtil, never()).rotateKey();
    }

    @Test
    void login_multipleSessions_bothTokensValid() {
        when(loginAttemptTracker.canLogin("admin")).thenReturn(true);
        when(administradorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password", "hashedPass")).thenReturn(true);
        when(jwtUtil.generateToken("admin")).thenReturn("token-1", "token-2");

        LoginResponse response1 = authenticationService.login(new LoginRequest("admin", "password"));
        LoginResponse response2 = authenticationService.login(new LoginRequest("admin", "password"));

        assertEquals("token-1", response1.getToken());
        assertEquals("token-2", response2.getToken());
    }
}
