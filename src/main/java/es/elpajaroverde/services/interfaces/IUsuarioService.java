package es.elpajaroverde.services.interfaces;

import es.elpajaroverde.dtos.UsuarioRequest;
import es.elpajaroverde.dtos.UsuarioResponse;
import es.elpajaroverde.dtos.UsuarioUpdateRequest;
import es.elpajaroverde.dtos.UsuarioVisibleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUsuarioService {

    UsuarioResponse crear(UsuarioRequest request);

    UsuarioResponse modificar(Long id, UsuarioUpdateRequest request);

    UsuarioResponse cambiarVisible(Long id, UsuarioVisibleRequest request);

    Page<UsuarioResponse> listar(String nombre, String apellido, String correo, Boolean visible, Pageable pageable);
}