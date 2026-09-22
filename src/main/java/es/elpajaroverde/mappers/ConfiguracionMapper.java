package es.elpajaroverde.mappers;

import es.elpajaroverde.dtos.ConfiguracionRequest;
import es.elpajaroverde.dtos.ConfiguracionResponse;
import es.elpajaroverde.models.Configuracion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConfiguracionMapper {

    ConfiguracionResponse toResponse(Configuracion configuracion);

    @Mapping(target = "id", ignore = true)
    Configuracion toEntity(ConfiguracionRequest request);
}