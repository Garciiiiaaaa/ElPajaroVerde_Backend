package es.elpajaroverde.repositories;

import es.elpajaroverde.models.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEntidadAfectadaAndEntidadId(String entidadAfectada, Long entidadId);
}
