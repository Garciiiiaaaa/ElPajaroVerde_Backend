package es.elpajaroverde.mappers;

import es.elpajaroverde.dtos.FechasOcupadas;
import es.elpajaroverde.models.Reserva;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DisponibilidadMapper {

    FechasOcupadas toFechasOcupadas(Reserva reserva);
}