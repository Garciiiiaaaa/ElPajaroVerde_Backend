package es.elpajaroverde.repositories;

import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.models.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {

    boolean existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoIn(
            LocalDate fechaSalida,
            LocalDate fechaEntrada,
            List<ReservaEstado> estados);

    boolean existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoInAndIdNot(
            LocalDate fechaSalida,
            LocalDate fechaEntrada,
            List<ReservaEstado> estados,
            Long id);

    List<Reserva> findByEstadoInAndFechaSalidaGreaterThanEqual(List<ReservaEstado> estados, LocalDate fecha);
}