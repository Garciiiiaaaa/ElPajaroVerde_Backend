# Plan: Modelo Relacional — Dominio Casa Rural

Traduce el modelo del [spec.md](./spec.md) a decisiones JPA y repositorios.

## Enums

Paquete: `enums/`

| Enum | Valores | Uso |
|---|---|---|
| `ReservaEstado` | `PENDIENTE, CONFIRMADA, CANCELADA` | `Reserva.estado` |
| `MensajeRemitente` | `USUARIO, ADMIN` | `Mensaje.remitente` |
| `AuditoriaTipoAccion` | `CREAR, MODIFICAR, ELIMINAR_OCULTAR, CAMBIAR_ESTADO` | `Auditoria.tipo_accion` |

`Auditoria.entidad_afectada` es `String`, no enum — más flexible, validado en Service.

## Entidades JPA

Paquete: `models/`

Convenciones:
- `@Entity` + `@Table(name="...")` con nombre explícito
- IDs: `Long id` con `@GeneratedValue(strategy = IDENTITY)`
- Enums: `@Enumerated(EnumType.STRING)`
- Sin Lombok

### Usuario

| Campo | Tipo JPA | Columna | Constraints |
|---|---|---|---|
| `id` | `Long` | `id` | `@Id @GeneratedValue(IDENTITY)` |
| `nombre` | `String` | `nombre` | `nullable=false, length=100` |
| `apellido` | `String` | `apellido` | `nullable=false, length=100` |
| `correo` | `String` | `correo` | `nullable=false, unique=true, length=150` |
| `telefono` | `String` | `telefono` | `length=20` |
| `visible` | `boolean` | `visible` | `nullable=false, default=true` |

Relaciones:
- `@OneToMany(mappedBy="usuario") List<Reserva> reservas`
- `@OneToMany(mappedBy="usuario") List<Mensaje> mensajes`

### Reserva

| Campo | Tipo JPA | Columna | Constraints |
|---|---|---|---|
| `id` | `Long` | `id` | `@Id @GeneratedValue(IDENTITY)` |
| `fechaReserva` | `LocalDate` | `fecha_reserva` | `nullable=false` |
| `fechaEntrada` | `LocalDate` | `fecha_entrada` | `nullable=false` |
| `fechaSalida` | `LocalDate` | `fecha_salida` | `nullable=false` |
| `numeroHuespedes` | `int` | `numero_huespedes` | `nullable=false` |
| `precio` | `BigDecimal` | `precio` | `nullable=false, precision=10, scale=2` |
| `estado` | `ReservaEstado` | `estado` | `nullable=false, default CONFIRMADA` |
| `visible` | `boolean` | `visible` | `nullable=false, default=true` |

Relaciones:
- `@ManyToOne @JoinColumn(name="usuario_id") Usuario usuario` (nullable)

### Administrador

| Campo | Tipo JPA | Columna | Constraints |
|---|---|---|---|
| `id` | `Long` | `id` | `@Id @GeneratedValue(IDENTITY)` |
| `nombreUsuario` | `String` | `nombre_usuario` | `nullable=false, unique=true, length=50` |
| `contrasena` | `String` | `contrasena` | `nullable=false, length=255` — BCrypt (Art. 10) |
| `correo` | `String` | `correo` | `nullable=false, unique=true, length=150` |

Relaciones:
- `@OneToMany(mappedBy="admin") List<Auditoria> auditorias`

### Mensaje

| Campo | Tipo JPA | Columna | Constraints |
|---|---|---|---|
| `id` | `Long` | `id` | `@Id @GeneratedValue(IDENTITY)` |
| `fechaMensaje` | `LocalDateTime` | `fecha_mensaje` | `nullable=false` |
| `asunto` | `String` | `asunto` | `nullable=false, length=200` |
| `mensaje` | `String` | `mensaje` | `nullable=false, columnDefinition="TEXT"` |
| `remitente` | `MensajeRemitente` | `remitente` | `nullable=false` |

Relaciones:
- `@ManyToOne @JoinColumn(name="usuario_id") Usuario usuario` (nullable=false)

### Configuracion

| Campo | Tipo JPA | Columna | Constraints |
|---|---|---|---|
| `id` | `Long` | `id` | `@Id` — fijo, siempre = 1, sin `@GeneratedValue` |
| `precioNoche` | `BigDecimal` | `precio_noche` | `nullable=false, precision=10, scale=2` |
| `estanciaMinima` | `int` | `estancia_minima` | `nullable=false` |
| `estanciaMaxima` | `int` | `estancia_maxima` | `nullable=false` |

Singleton: solo puede existir fila con `id=1`. La PK sin `@GeneratedValue` + validación en Service garantiza la integridad.

### Auditoria

| Campo | Tipo JPA | Columna | Constraints |
|---|---|---|---|
| `id` | `Long` | `id` | `@Id @GeneratedValue(IDENTITY)` |
| `tipoAccion` | `AuditoriaTipoAccion` | `tipo_accion` | `nullable=false` |
| `fecha` | `LocalDateTime` | `fecha` | `nullable=false` |
| `admin` | `Administrador` | `admin_id` | `nullable` (puede ser nulo si se creó usuario automáticamente) |
| `entidadAfectada` | `String` | `entidad_afectada` | `nullable=false, length=50` — "RESERVA", "USUARIO", "CONFIGURACION" |
| `entidadId` | `Long` | `entidad_id` | `nullable=false` |
| `descripcion` | `String` | `descripcion` | `columnDefinition="TEXT"` |

Relaciones:
- `@ManyToOne @JoinColumn(name="admin_id") Administrador admin` (nullable)

## Repositorios

Paquete: `repositories/`

| Interface | Métodos custom | Justificación |
|---|---|---|
| `UsuarioRepository` | `Optional<Usuario> findByCorreo(String correo)` | Deduplicación al crear usuario desde mensaje |
| `ReservaRepository` | `boolean existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoIn(LocalDate salida, LocalDate entrada, List<ReservaEstado> estados)` | Comprobación de solapamiento (solo PENDIENTE + CONFIRMADA) |
| `AdministradorRepository` | `Optional<Administrador> findByNombreUsuario(String nombreUsuario)` | Login futuro |
| `MensajeRepository` | `List<Mensaje> findByUsuarioIdOrderByFechaMensajeAsc(Long usuarioId)` | Listar conversación por usuario |
| `ConfiguracionRepository` | (sin métodos custom) | CRUD básico, findById(1L) |
| `AuditoriaRepository` | `List<Auditoria>.findByEntidadAfectadaAndEntidadId(String entidad, Long entidadId)` | Historial por entidad |

## Restricciones de integridad

| Restricción | Capa | Mecanismo |
|---|---|---|
| Configuracion: solo una fila | Service + DB | Service valida id=1. PK sin `@GeneratedValue` |
| Reserva: sin solapamiento | Service | Query `existsBy...` + lógica en `ReservaService` |
| Reserva: precio = precio_noche × noches | Service | Cálculo en `ReservaService.crear()` |
| Configuracion: estanciaMinima ≤ estanciaMaxima | Service | Validación antes de persistir |
| Auditoria: tipo_accion compatible con entidad_afectada | Service | Validación de compatibilidad |
| Mensaje: inmutabilidad | Service | No se exponen endpoints de UPDATE/DELETE |
| Administrador: contrasena = BCrypt | Service | `BCryptPasswordEncoder` antes de persistir (Art. 10) |