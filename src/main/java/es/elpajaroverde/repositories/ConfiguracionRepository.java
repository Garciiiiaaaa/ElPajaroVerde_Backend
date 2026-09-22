package es.elpajaroverde.repositories;

import es.elpajaroverde.models.Configuracion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Configuracion c WHERE c.id = 1")
    Optional<Configuracion> findByIdWithLock();
}