package es.elpajaroverde.repositories;

import es.elpajaroverde.models.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {

    List<Auditoria> findByEntidadAfectadaAndEntidadId(String entidadAfectada, Long entidadId);
}