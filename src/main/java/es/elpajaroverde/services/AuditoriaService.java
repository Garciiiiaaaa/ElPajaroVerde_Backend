package es.elpajaroverde.services;

import es.elpajaroverde.dtos.AuditoriaResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.mappers.AuditoriaMapper;
import es.elpajaroverde.models.Administrador;
import es.elpajaroverde.models.Auditoria;
import es.elpajaroverde.repositories.AuditoriaRepository;
import es.elpajaroverde.services.interfaces.IAuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuditoriaService implements IAuditoriaService {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.DESC, "fecha");

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaMapper auditoriaMapper;
    private final AutenticadoActual autenticadoActual;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            AuditoriaMapper auditoriaMapper,
                            AutenticadoActual autenticadoActual) {
        this.auditoriaRepository = auditoriaRepository;
        this.auditoriaMapper = auditoriaMapper;
        this.autenticadoActual = autenticadoActual;
    }

    @Override
    @Transactional
    public void registrar(AuditoriaTipoAccion tipoAccion, String entidadAfectada, Long entidadId) {
        Optional<Administrador> admin = autenticadoActual.get();
        Auditoria auditoria = new Auditoria(
                tipoAccion,
                LocalDateTime.now(),
                admin.orElse(null),
                entidadAfectada,
                entidadId,
                descripcion(tipoAccion, entidadAfectada, entidadId));
        auditoriaRepository.save(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> listar(String entidadAfectada, Long entidadId,
                                          AuditoriaTipoAccion tipoAccion,
                                          LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                          Pageable pageable) {
        Specification<Auditoria> spec = (root, query, cb) -> cb.conjunction();
        if (entidadAfectada != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entidadAfectada"), entidadAfectada));
        }
        if (entidadId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entidadId"), entidadId));
        }
        if (tipoAccion != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipoAccion"), tipoAccion));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
        }

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEN_POR_DEFECTO);
        }

        return auditoriaRepository.findAll(spec, pageable).map(auditoriaMapper::toResponse);
    }

    private String descripcion(AuditoriaTipoAccion tipoAccion, String entidadAfectada, Long entidadId) {
        String accion = switch (tipoAccion) {
            case CREAR -> "Creación de " + entidadAfectada;
            case MODIFICAR -> "Modificación de " + entidadAfectada;
            case ELIMINAR_OCULTAR -> "Cambio de visibilidad de " + entidadAfectada;
            case CAMBIAR_ESTADO -> "Cambio de estado de " + entidadAfectada;
        };
        return accion + " (id=" + entidadId + ")";
    }
}