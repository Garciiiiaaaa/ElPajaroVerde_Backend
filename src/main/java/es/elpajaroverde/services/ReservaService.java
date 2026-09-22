package es.elpajaroverde.services;

import es.elpajaroverde.dtos.ReservaCambioEstadoRequest;
import es.elpajaroverde.dtos.ReservaRequest;
import es.elpajaroverde.dtos.ReservaResponse;
import es.elpajaroverde.dtos.ReservaUpdateRequest;
import es.elpajaroverde.dtos.ReservaVisibleRequest;
import es.elpajaroverde.enums.AuditoriaTipoAccion;
import es.elpajaroverde.enums.ReservaEstado;
import es.elpajaroverde.exceptions.DuracionEstanciaInvalidaException;
import es.elpajaroverde.exceptions.EntidadNoEncontradaException;
import es.elpajaroverde.exceptions.FechasInvalidasException;
import es.elpajaroverde.exceptions.NumeroHuespedesInvalidoException;
import es.elpajaroverde.exceptions.OcultacionInvalidaException;
import es.elpajaroverde.exceptions.SolapamientoReservaException;
import es.elpajaroverde.mappers.ReservaMapper;
import es.elpajaroverde.models.Configuracion;
import es.elpajaroverde.models.Reserva;
import es.elpajaroverde.models.Usuario;
import es.elpajaroverde.repositories.ConfiguracionRepository;
import es.elpajaroverde.repositories.ReservaRepository;
import es.elpajaroverde.repositories.UsuarioRepository;
import es.elpajaroverde.services.interfaces.IAuditoriaService;
import es.elpajaroverde.services.interfaces.IReservaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ReservaService implements IReservaService {

    private static final List<ReservaEstado> ESTADOS_OCUPAN = List.of(ReservaEstado.PENDIENTE, ReservaEstado.CONFIRMADA);
    private static final ReservaEstado ESTADO_POR_DEFECTO = ReservaEstado.CONFIRMADA;
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.ASC, "fechaEntrada");

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final ReservaMapper reservaMapper;
    private final IAuditoriaService auditoriaService;

    public ReservaService(ReservaRepository reservaRepository,
                          UsuarioRepository usuarioRepository,
                          ConfiguracionRepository configuracionRepository,
                          ReservaMapper reservaMapper,
                          IAuditoriaService auditoriaService) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracionRepository = configuracionRepository;
        this.reservaMapper = reservaMapper;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public ReservaResponse crear(ReservaRequest request) {
        Usuario usuario = resolverUsuario(request.getUsuarioId(), request.getUsuario());

        validarFechas(request.getFechaEntrada(), request.getFechaSalida());
        Configuracion configuracion = configuracionActual();
        long noches = validarDuracion(request.getFechaEntrada(), request.getFechaSalida(), configuracion);
        validarHuespedes(request.getNumeroHuespedes());
        comprobarSolapamiento(request.getFechaEntrada(), request.getFechaSalida(), null);

        Reserva reserva = reservaMapper.toEntity(request);
        reserva.setFechaReserva(LocalDate.now());
        reserva.setVisible(true);
        reserva.setEstado(reserva.getEstado() != null ? reserva.getEstado() : ESTADO_POR_DEFECTO);
        reserva.setUsuario(usuario);
        reserva.setPrecio(calcularPrecio(configuracion.getPrecioNoche(), noches));

        reserva = reservaRepository.save(reserva);
        auditoriaService.registrar(AuditoriaTipoAccion.CREAR, "Reserva", reserva.getId());
        return reservaMapper.toResponse(reserva);
    }

    @Override
    @Transactional
    public ReservaResponse modificar(Long id, ReservaUpdateRequest request) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada"));

        if (request.getUsuarioId() != null) {
            reserva.setUsuario(resolverUsuario(request.getUsuarioId(), null));
        } else if (request.isUsuarioPresente()) {
            reserva.setUsuario(request.getUsuario() != null
                    ? resolverUsuario(null, request.getUsuario())
                    : null);
        }

        boolean cambianFechas = request.getFechaEntrada() != null || request.getFechaSalida() != null;
        if (cambianFechas) {
            LocalDate entrada = request.getFechaEntrada() != null ? request.getFechaEntrada() : reserva.getFechaEntrada();
            LocalDate salida = request.getFechaSalida() != null ? request.getFechaSalida() : reserva.getFechaSalida();

            validarFechas(entrada, salida);
            Configuracion configuracion = configuracionActual();

            long nochesActuales = ChronoUnit.DAYS.between(reserva.getFechaEntrada(), reserva.getFechaSalida());
            long nochesResultantes = ChronoUnit.DAYS.between(entrada, salida);
            if (nochesResultantes != nochesActuales) {
                validarDuracion(entrada, salida, configuracion);
            }

            comprobarSolapamiento(entrada, salida, id);

            reserva.setFechaEntrada(entrada);
            reserva.setFechaSalida(salida);
            reserva.setPrecio(calcularPrecio(configuracion.getPrecioNoche(), nochesResultantes));
        }

        if (request.getNumeroHuespedes() != null) {
            validarHuespedes(request.getNumeroHuespedes());
            reserva.setNumeroHuespedes(request.getNumeroHuespedes());
        }

        reserva = reservaRepository.save(reserva);
        auditoriaService.registrar(AuditoriaTipoAccion.MODIFICAR, "Reserva", reserva.getId());
        return reservaMapper.toResponse(reserva);
    }

    @Override
    @Transactional
    public ReservaResponse cambiarEstado(Long id, ReservaCambioEstadoRequest request) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada"));

        if (reserva.getEstado() == ReservaEstado.CANCELADA
                && request.getEstado() != ReservaEstado.CANCELADA) {
            configuracionActual();
            comprobarSolapamiento(reserva.getFechaEntrada(), reserva.getFechaSalida(), id);
        }

        reserva.setEstado(request.getEstado());
        reserva = reservaRepository.save(reserva);
        auditoriaService.registrar(AuditoriaTipoAccion.CAMBIAR_ESTADO, "Reserva", reserva.getId());
        return reservaMapper.toResponse(reserva);
    }

    @Override
    @Transactional
    public ReservaResponse cambiarVisible(Long id, ReservaVisibleRequest request) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reserva no encontrada"));

        if (!request.getVisible() && reserva.getEstado() != ReservaEstado.CANCELADA
                && !reserva.getFechaSalida().isBefore(LocalDate.now())) {
            throw new OcultacionInvalidaException();
        }

        reserva.setVisible(request.getVisible());
        reserva = reservaRepository.save(reserva);
        auditoriaService.registrar(AuditoriaTipoAccion.ELIMINAR_OCULTAR, "Reserva", reserva.getId());
        return reservaMapper.toResponse(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaResponse> listar(ReservaEstado estado, Boolean visible, Long usuarioId,
                                        LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable) {
        Specification<Reserva> spec = (root, query, cb) -> cb.conjunction();
        if (estado != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        boolean visibilidad = (visible != null) ? visible : true;
        spec = spec.and((root, query, cb) -> cb.equal(root.get("visible"), visibilidad));
        if (usuarioId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId));
        }
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaEntrada"), fechaDesde));
        }
        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaEntrada"), fechaHasta));
        }

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEN_POR_DEFECTO);
        }

        return reservaRepository.findAll(spec, pageable).map(reservaMapper::toResponse);
    }

    private Configuracion configuracionActual() {
        return configuracionRepository.findByIdWithLock()
                .orElseThrow(() -> new EntidadNoEncontradaException("Configuración no encontrada"));
    }

    private Usuario resolverUsuario(Long usuarioId, ReservaRequest.UsuarioReserva inline) {
        if (usuarioId != null) {
            return usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado"));
        }
        if (inline != null) {
            Optional<Usuario> existente = usuarioRepository.findByCorreoIgnoreCase(inline.getCorreo());
            if (existente.isPresent()) {
                return existente.get();
            }
            Usuario nuevo = new Usuario(inline.getNombre(), inline.getApellido(), inline.getCorreo(),
                    inline.getTelefono(), true);
            Usuario guardado = usuarioRepository.save(nuevo);
            auditoriaService.registrar(AuditoriaTipoAccion.CREAR, "Usuario", guardado.getId());
            return guardado;
        }
        return null;
    }

    private void validarFechas(LocalDate entrada, LocalDate salida) {
        if (!entrada.isBefore(salida)) {
            throw new FechasInvalidasException();
        }
    }

    private long validarDuracion(LocalDate entrada, LocalDate salida, Configuracion configuracion) {
        long noches = ChronoUnit.DAYS.between(entrada, salida);
        if (noches < configuracion.getEstanciaMinima() || noches > configuracion.getEstanciaMaxima()) {
            throw new DuracionEstanciaInvalidaException();
        }
        return noches;
    }

    private void validarHuespedes(int numeroHuespedes) {
        if (numeroHuespedes < 1 || numeroHuespedes > ReservaReglas.MAX_HUESPEDES) {
            throw new NumeroHuespedesInvalidoException();
        }
    }

    private void comprobarSolapamiento(LocalDate entrada, LocalDate salida, Long excluirId) {
        boolean solapa;
        if (excluirId == null) {
            solapa = reservaRepository.existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoIn(
                    salida, entrada, ESTADOS_OCUPAN);
        } else {
            solapa = reservaRepository
                    .existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoInAndIdNot(
                            salida, entrada, ESTADOS_OCUPAN, excluirId);
        }
        if (solapa) {
            throw new SolapamientoReservaException();
        }
    }

    private BigDecimal calcularPrecio(BigDecimal precioNoche, long noches) {
        return precioNoche.multiply(BigDecimal.valueOf(noches)).setScale(2, RoundingMode.HALF_UP);
    }
}