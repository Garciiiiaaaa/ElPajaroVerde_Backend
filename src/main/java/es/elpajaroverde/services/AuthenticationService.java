package es.elpajaroverde.services;

import es.elpajaroverde.dtos.AuthErrorResponse;
import es.elpajaroverde.dtos.LoginRequest;
import es.elpajaroverde.dtos.LoginResponse;
import es.elpajaroverde.exceptions.CredencialesInvalidasException;
import es.elpajaroverde.exceptions.CuentaBloqueadaException;
import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.repositories.AdministradorRepository;
import es.elpajaroverde.security.JwtUtil;
import es.elpajaroverde.services.interfaces.IAuthenticationService;
import es.elpajaroverde.services.interfaces.ILoginAttemptTracker;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticationService implements IAuthenticationService {

    private final AdministradorRepository administradorRepository;
    private final JwtUtil jwtUtil;
    private final ILoginAttemptTracker loginAttemptTracker;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(AdministradorRepository administradorRepository,
                                 JwtUtil jwtUtil,
                                 ILoginAttemptTracker loginAttemptTracker,
                                 PasswordEncoder passwordEncoder) {
        this.administradorRepository = administradorRepository;
        this.jwtUtil = jwtUtil;
        this.loginAttemptTracker = loginAttemptTracker;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String nombreUsuario = request.getNombreUsuario();

        if (!loginAttemptTracker.canLogin(nombreUsuario)) {
            throw new CuentaBloqueadaException("Credenciales incorrectas");
        }

        Optional<Administrador> adminOpt = administradorRepository.findByNombreUsuario(nombreUsuario);
        if (adminOpt.isEmpty()) {
            loginAttemptTracker.registerFailedAttempt(nombreUsuario);
            throw new CredencialesInvalidasException("Credenciales incorrectas");
        }

        Administrador admin = adminOpt.get();
        if (!passwordEncoder.matches(request.getContrasena(), admin.getContrasena())) {
            loginAttemptTracker.registerFailedAttempt(nombreUsuario);
            throw new CredencialesInvalidasException("Credenciales incorrectas");
        }

        loginAttemptTracker.clearAttempts(nombreUsuario);
        String token = jwtUtil.generateToken(nombreUsuario);
        return new LoginResponse(token);
    }

    @Override
    public AuthErrorResponse logout(String token) {
        if (token == null || !jwtUtil.validateToken(token)) {
            return new AuthErrorResponse("Sesión cerrada");
        }
        jwtUtil.rotateKey();
        return new AuthErrorResponse("Sesión cerrada");
    }

    @Override
    public Optional<Administrador> buscarAdministradorPorNombre(String nombreUsuario) {
        return administradorRepository.findByNombreUsuario(nombreUsuario);
    }
}
