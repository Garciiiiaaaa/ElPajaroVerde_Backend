package es.elpajaroverde.services;

import es.elpajaroverde.dtos.MensajeRequest;
import es.elpajaroverde.dtos.MensajeResponse;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.enums.MensajeRemitente;
import es.elpajaroverde.mappers.MensajeMapper;
import es.elpajaroverde.models.Mensaje;
import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.repositories.MensajeRepository;
import es.elpajaroverde.repositories.UsuarioRepository;
import es.elpajaroverde.services.interfaces.IAuditoriaService;
import es.elpajaroverde.services.interfaces.IMensajeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MensajeService implements IMensajeService {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.DESC, "fechaMensaje");
    private static final String ASUNTO_POR_DEFECTO = "Consulta";
    private static final String NOMBRE_POR_DEFECTO = "Sin nombre";
    private static final String APELLIDO_POR_DEFECTO = "Sin apellido";

    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final MensajeMapper mensajeMapper;
    private final IAuditoriaService auditoriaService;

    public MensajeService(UsuarioRepository usuarioRepository,
                          MensajeRepository mensajeRepository,
                          MensajeMapper mensajeMapper,
                          IAuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.mensajeRepository = mensajeRepository;
        this.mensajeMapper = mensajeMapper;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public MensajeResponse enviar(MensajeRequest request) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.getCorreo())
                .orElseGet(() -> {
                    Usuario nuevo = new Usuario(
                            texto(request.getNombre(), NOMBRE_POR_DEFECTO),
                            texto(request.getApellido(), APELLIDO_POR_DEFECTO),
                            request.getCorreo(),
                            request.getTelefono(),
                            true);
                    Usuario guardado = usuarioRepository.save(nuevo);
                    auditoriaService.registrar(AuditoriaTipoAccion.CREAR, "Usuario", guardado.getId());
                    return guardado;
                });

        String asunto = texto(request.getAsunto(), ASUNTO_POR_DEFECTO);
        Mensaje mensaje = new Mensaje(LocalDateTime.now(), asunto, request.getMensaje(),
                MensajeRemitente.USUARIO, usuario);
        mensaje = mensajeRepository.save(mensaje);
        return mensajeMapper.toResponse(mensaje);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MensajeResponse> listar(Long usuarioId, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                        Pageable pageable) {
        Specification<Mensaje> spec = (root, query, cb) -> cb.conjunction();
        if (usuarioId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaMensaje"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaMensaje"), fechaHasta));
        }

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEN_POR_DEFECTO);
        }

        return mensajeRepository.findAll(spec, pageable).map(mensajeMapper::toResponse);
    }

    private String texto(String valor, String porDefecto) {
        return (valor == null || valor.isBlank()) ? porDefecto : valor;
    }
}