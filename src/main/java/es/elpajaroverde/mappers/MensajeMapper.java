package es.elpajaroverde.mappers;

import es.elpajaroverde.dtos.MensajeResponse;
import es.elpajaroverde.models.Mensaje;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UsuarioMapper.class)
public interface MensajeMapper {

    MensajeResponse toResponse(Mensaje mensaje);
}