package es.elpajaroverde.repositories;

import es.elpajaroverde.models.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByUsuarioIdOrderByFechaMensajeAsc(Long usuarioId);
}
