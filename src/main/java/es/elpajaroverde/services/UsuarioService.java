package es.elpajaroverde.services;

import es.elpajaroverde.dtos.UsuarioRequest;
import es.elpajaroverde.dtos.UsuarioResponse;
import es.elpajaroverde.dtos.UsuarioUpdateRequest;
import es.elpajaroverde.dtos.UsuarioVisibleRequest;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.exceptions.CorreoDuplicadoException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.mappers.UsuarioMapper;
import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.repositories.UsuarioRepository;
import es.elpajaroverde.services.interfaces.IAuditoriaService;
import es.elpajaroverde.services.interfaces.IUsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UsuarioService implements IUsuarioService {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.ASC, "id");

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final IAuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          UsuarioMapper usuarioMapper,
                          IAuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.findByCorreoIgnoreCase(request.getCorreo()).isPresent()) {
            throw new CorreoDuplicadoException();
        }

        Usuario usuario = usuarioRepository.save(usuarioMapper.toEntity(request));
        auditoriaService.registrar(AuditoriaTipoAccion.CREAR, "Usuario", usuario.getId());
        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse modificar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado"));

        if (request.getNombre() != null) {
            usuario.setNombre(request.getNombre());
        }
        if (request.getApellido() != null) {
            usuario.setApellido(request.getApellido());
        }
        if (request.getTelefono() != null) {
            usuario.setTelefono(request.getTelefono());
        }
        if (request.getCorreo() != null) {
            Optional<Usuario> existente = usuarioRepository.findByCorreoIgnoreCase(request.getCorreo());
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new CorreoDuplicadoException();
            }
            usuario.setCorreo(request.getCorreo());
        }

        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaTipoAccion.MODIFICAR, "Usuario", usuario.getId());
        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse cambiarVisible(Long id, UsuarioVisibleRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado"));

        usuario.setVisible(request.getVisible());
        usuario = usuarioRepository.save(usuario);
        auditoriaService.registrar(AuditoriaTipoAccion.ELIMINAR_OCULTAR, "Usuario", usuario.getId());
        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String nombre, String apellido, String correo, Boolean visible,
                                        Pageable pageable) {
        Specification<Usuario> spec = (root, query, cb) -> cb.conjunction();
        if (nombre != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
        }
        if (apellido != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("apellido")), "%" + apellido.toLowerCase() + "%"));
        }
        if (correo != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("correo")), "%" + correo.toLowerCase() + "%"));
        }
        boolean visibilidad = (visible != null) ? visible : true;
        spec = spec.and((root, query, cb) -> cb.equal(root.get("visible"), visibilidad));

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEN_POR_DEFECTO);
        }

        return usuarioRepository.findAll(spec, pageable).map(usuarioMapper::toResponse);
    }
}