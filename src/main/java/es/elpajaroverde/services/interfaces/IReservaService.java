package es.elpajaroverde.services.interfaces;

import es.elpajaroverde.dtos.ReservaCambioEstadoRequest;
import es.elpajaroverde.dtos.ReservaRequest;
import es.elpajaroverde.dtos.ReservaResponse;
import es.elpajaroverde.dtos.ReservaUpdateRequest;
import es.elpajaroverde.dtos.ReservaVisibleRequest;
import es.elpajaroverde.enums.ReservaEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IReservaService {

    ReservaResponse crear(ReservaRequest request);

    ReservaResponse modificar(Long id, ReservaUpdateRequest request);

    ReservaResponse cambiarEstado(Long id, ReservaCambioEstadoRequest request);

    ReservaResponse cambiarVisible(Long id, ReservaVisibleRequest request);

    Page<ReservaResponse> listar(ReservaEstado estado, Boolean visible, Long usuarioId,
                                 LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable);
}