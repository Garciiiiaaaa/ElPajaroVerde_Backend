package es.elpajaroverde.repositories;

import es.elpajaroverde.models.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByNombreUsuario(String nombreUsuario);
}
