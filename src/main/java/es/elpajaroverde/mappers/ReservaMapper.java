package es.elpajaroverde.mappers;

import es.elpajaroverde.dtos.ReservaRequest;
import es.elpajaroverde.dtos.ReservaResponse;
import es.elpajaroverde.models.Reserva;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UsuarioMapper.class)
public interface ReservaMapper {

    ReservaResponse toResponse(Reserva reserva);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaReserva", ignore = true)
    @Mapping(target = "precio", ignore = true)
    @Mapping(target = "visible", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Reserva toEntity(ReservaRequest request);
}