package es.elpajaroverde.services;

import es.elpajaroverde.dtos.AuthErrorResponse;
import es.elpajaroverde.dtos.LoginRequest;
import es.elpajaroverde.dtos.LoginResponse;
import es.elpajaroverde.models.Administrador;

import java.util.Optional;

public interface IAuthenticationService {

    LoginResponse login(LoginRequest request);

    AuthErrorResponse logout(String token);

    Optional<Administrador> buscarAdministradorPorNombre(String nombreUsuario);
}
