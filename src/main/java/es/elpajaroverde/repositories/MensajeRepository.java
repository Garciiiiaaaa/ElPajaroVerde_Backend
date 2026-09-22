package es.elpajaroverde.repositories;

import es.elpajaroverde.models.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long>, JpaSpecificationExecutor<Mensaje> {

    List<Mensaje> findByUsuarioIdOrderByFechaMensajeAsc(Long usuarioId);
}