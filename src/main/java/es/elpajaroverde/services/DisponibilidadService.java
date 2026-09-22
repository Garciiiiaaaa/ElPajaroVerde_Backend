package es.elpajaroverde.services;

import es.elpajaroverde.dtos.FechasOcupadas;
import es.elpajaroverde.dtos.FechasOcupadasResponse;
import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.mappers.DisponibilidadMapper;
import es.elpajaroverde.models.Reserva;
import es.elpajaroverde.repositories.ReservaRepository;
import es.elpajaroverde.services.interfaces.IDisponibilidadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DisponibilidadService implements IDisponibilidadService {

    private static final List<ReservaEstado> ESTADOS_OCUPAN = List.of(ReservaEstado.PENDIENTE, ReservaEstado.CONFIRMADA);

    private final ReservaRepository reservaRepository;
    private final DisponibilidadMapper disponibilidadMapper;

    public DisponibilidadService(ReservaRepository reservaRepository,
                                 DisponibilidadMapper disponibilidadMapper) {
        this.reservaRepository = reservaRepository;
        this.disponibilidadMapper = disponibilidadMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public FechasOcupadasResponse obtenerFechasOcupadas() {
        List<Reserva> reservas = reservaRepository.findByEstadoInAndFechaSalidaGreaterThanEqual(
                ESTADOS_OCUPAN, LocalDate.now());
        List<FechasOcupadas> ocupadas = reservas.stream()
                .map(disponibilidadMapper::toFechasOcupadas)
                .toList();
        return new FechasOcupadasResponse(ocupadas);
    }
}