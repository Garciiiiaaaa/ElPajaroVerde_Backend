package es.elpajaroverde.services;

import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.services.interfaces.IAuthenticationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resuelve el administrador autenticado desde el contexto de seguridad.
 * Cumple Art. 1: los services (no controllers ni filtros) resuelven el dato de negocio.
 */
@Component
public class AutenticadoActual {

    private final IAuthenticationService authenticationService;

    public AutenticadoActual(IAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public Optional<Administrador> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authenticationService.buscarAdministradorPorNombre(authentication.getName());
    }
}