package es.elpajaroverde.services;

import es.elpajaroverde.models.Configuracion;

import java.util.Optional;

public interface IConfiguracionService {

    Optional<Configuracion> getById(Long id);
}
