package es.elpajaroverde.mappers;

import es.elpajaroverde.dtos.UsuarioRequest;
import es.elpajaroverde.dtos.UsuarioResponse;
import es.elpajaroverde.models.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reservas", ignore = true)
    @Mapping(target = "mensajes", ignore = true)
    Usuario toEntity(UsuarioRequest request);
}