package es.elpajaroverde.services.interfaces;

import es.elpajaroverde.dtos.ConfiguracionRequest;
import es.elpajaroverde.dtos.ConfiguracionResponse;

public interface IConfiguracionService {

    ConfiguracionResponse obtener();

    ConfiguracionResponse actualizar(ConfiguracionRequest request);
}