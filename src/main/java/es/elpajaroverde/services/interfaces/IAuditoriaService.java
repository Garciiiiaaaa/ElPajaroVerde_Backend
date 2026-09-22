package es.elpajaroverde.services.interfaces;

import es.elpajaroverde.dtos.AuditoriaResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface IAuditoriaService {

    void registrar(AuditoriaTipoAccion tipoAccion, String entidadAfectada, Long entidadId);

    Page<AuditoriaResponse> listar(String entidadAfectada, Long entidadId, AuditoriaTipoAccion tipoAccion,
                                   LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);
}