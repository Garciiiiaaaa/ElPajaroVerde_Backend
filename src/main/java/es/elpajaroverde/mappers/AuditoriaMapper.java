package es.elpajaroverde.mappers;

import es.elpajaroverde.dtos.AuditoriaResponse;
import es.elpajaroverde.models.Auditoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {

    @Mapping(target = "admin", source = "admin.nombreUsuario")
    AuditoriaResponse toResponse(Auditoria auditoria);

    @Mapping(target = "admin", ignore = true)
    Auditoria toEntity(AuditoriaResponse response);
}