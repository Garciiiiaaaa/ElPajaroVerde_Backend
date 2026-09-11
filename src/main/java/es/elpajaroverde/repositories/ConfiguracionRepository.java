package es.elpajaroverde.repositories;

import es.elpajaroverde.models.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {
}
