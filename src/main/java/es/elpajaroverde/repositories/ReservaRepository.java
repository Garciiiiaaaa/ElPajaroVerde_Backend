package es.elpajaroverde.repositories;

import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.models.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoIn(
            LocalDate fechaSalida,
            LocalDate fechaEntrada,
            List<ReservaEstado> estados);
}
