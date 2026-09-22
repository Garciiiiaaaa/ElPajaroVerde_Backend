# Tareas: Núcleo de Negocio

Generado a partir de [spec.md](./spec.md) y [plan.md](./plan.md).

Decisiones de implementación asumidas (aprobadas por el usuario):
- Los errores de negocio reutilizan `AuthErrorResponse` (DTO `{message}` de Art. 8).
- El usuario inline de una reserva es la clase anidada `UsuarioReserva` dentro de `ReservaRequest`, reutilizada por `ReservaUpdateRequest` (que le añade el deserializador de presencia).
- `GET /api/v1/configuracion` sin fila singleton → 404 `EntidadNoEncontradaException` (la actual `noContent` se sustituye).
- No existe seed de `Configuracion(id=1)` en producción: los tests lo siembran; en despliegues se inicializa por otro medio (documentado en plan.md).

## Fase 1 — Constantes y excepciones

Sin dependencias. Base para DTOs, services y manejo de errores.

- [x] **T1.1** Crear `ReservaReglas` en `services/` (constante de negocio)
  - `public static final int MAX_HUESPEDES = 10` (RF-12, decisión de producto)
  - Es la fuente única: la referencian `ReservaRequest`/`ReservaUpdateRequest` (`@Max`) y `ReservaService`
  - Hecho: compila; la constante existe y es importable desde `dtos/` y `services/`

- [x] **T1.2** Crear excepciones de negocio en `exceptions/`
  - Crear: `EntidadNoEncontradaException` (404), `SolapamientoReservaException` (409), `CorreoDuplicadoException` (409), `FechasInvalidasException` (400), `DuracionEstanciaInvalidaException` (400), `NumeroHuespedesInvalidoException` (400), `OcultacionInvalidaException` (400), `ConfiguracionInvalidaException` (400), `ReferenciaInconsistenteException` (400)
  - Cada una extiende `RuntimeException` con constructor que recibe `message`
  - Hecho: todas compilan con sus mensajes en español

- [x] **T1.3** Ampliar `GlobalExceptionHandler` en `config/`
  - Depende de: T1.2
  - Añadir un `@ExceptionHandler` por cada excepción de T1.2 devolviendo `AuthErrorResponse(message)` con su HTTP status (404/409/400 según tabla del plan)
  - Hecho: los 9 tipos se mapean con el status correcto; los handlers de autenticación existentes no se tocan

## Fase 2 — DTOs

Depende de Fase 1 (T1.1 para `ReservaRequest`). Resto independientes entre sí.

- [x] **T2.1** Crear `FechasOcupadas` en `dtos/` (output, solo lectura)
  - Campos: `fechaEntrada` (LocalDate), `fechaSalida` (LocalDate)
  - Hecho: compila

- [x] **T2.2** Crear `FechasOcupadasResponse` en `dtos/` (output, solo lectura)
  - Campo: `ocupadas` (`List<FechasOcupadas>`)
  - Hecho: compila

- [x] **T2.3** Crear `MensajeRequest` en `dtos/` (input, RF-3, RF-38)
  - Campos: `correo`, `mensaje`, `nombre`, `apellido`, `telefono`, `asunto`
  - Bean Validation: `@NotBlank @Email` en `correo`; `@NotBlank` en `mensaje`; resto opcional (RF-3)
  - Hecho: compila; validates parecen correctas

- [x] **T2.4** Crear `MensajeResponse` en `dtos/` (output, RF-6, RF-29)
  - Campos: `id`, `fechaMensaje`, `asunto`, `mensaje`, `remitente` (MensajeRemitente), `usuario` (UsuarioResponse)
  - Hecho: compila

- [x] **T2.5** Crear `ReservaRequest` en `dtos/` (input, RF-8 a RF-14, RF-38)
  - Depende de: T1.1
  - Campos: `fechaEntrada`, `fechaSalida`, `numeroHuespedes`, `estado` (opcional), y **una** de: `usuarioId`, `usuario` inline, o ninguno
  - Clase anidada `UsuarioReserva` con `nombre`, `apellido`, `correo`, `telefono`; `@NotBlank` en nombre/apellido/correo y `@Email` en correo (RF-22)
  - Bean Validation: `@NotNull` en fechas; `@NotNull @Min(1) @Max(value = ReservaReglas.MAX_HUESPEDES)` en `numeroHuespedes` (RF-12)
  - Hecho: compila con la constante de negocio referenciada

- [x] **T2.6** Crear `ReservaUpdateRequest` en `dtos/` (input, RF-15, RF-16, RF-38)
  - Depende de: T1.1, T2.5 (reutiliza `ReservaRequest.UsuarioReserva`)
  - Campos: `fechaEntrada`, `fechaSalida`, `numeroHuespedes`, `usuarioId`, `usuario` (inline o `null` para desvincular)
  - Deserializador de presencia para `usuario`: distingue campo ausente de `usuario:null` (semántica en las reglas de consistencia del plan)
  - Mismas anotaciones de Bean Validation que `ReservaRequest`, solo cuando el campo viene presente
  - Hecho: con Jackson, el DTO expone si `usuario` vino ausente, `null` u objeto

- [x] **T2.7** Crear `ReservaResponse` en `dtos/` (output, solo lectura)
  - Campos: `id`, `fechaReserva`, `fechaEntrada`, `fechaSalida`, `numeroHuespedes`, `precio`, `estado`, `visible`, `usuario` (UsuarioResponse|null)
  - Hecho: compila

- [x] **T2.8** Crear `ReservaCambioEstadoRequest` en `dtos/` (input, RF-17, RF-18)
  - Campo: `estado` (ReservaEstado) con `@NotNull`
  - Hecho: compila

- [x] **T2.9** Crear `ReservaVisibleRequest` en `dtos/` (input, RF-19, RF-20)
  - Campo: `visible` (boolean) con `@NotNull`
  - Hecho: compila

- [x] **T2.10** Crear `UsuarioRequest` en `dtos/` (input, RF-22, RF-23, RF-38)
  - Campos: `nombre`, `apellido`, `correo`, `telefono`
  - Bean Validation: `@NotBlank` en nombre/apellido/correo; `@Email` en correo
  - Hecho: compila

- [x] **T2.11** Crear `UsuarioUpdateRequest` en `dtos/` (input, RF-24, RF-25, RF-38)
  - Campos: `nombre`, `apellido`, `correo`, `telefono` (todos opcionales, PATCH parcial)
  - Bean Validation: `@Email` en correo si viene presente
  - Hecho: compila

- [x] **T2.12** Crear `UsuarioResponse` en `dtos/` (output, solo lectura)
  - Campos: `id`, `nombre`, `apellido`, `correo`, `telefono`, `visible`
  - Hecho: compila

- [x] **T2.13** Crear `UsuarioVisibleRequest` en `dtos/` (input, RF-26, RF-38)
  - Campo: `visible` (boolean) con `@NotNull`
  - Hecho: compila

- [x] **T2.14** Crear `AuditoriaResponse` en `dtos/` (output, solo lectura)
  - Campos: `id`, `fecha`, `admin` (nombreUsuario|null), `entidadAfectada`, `entidadId`, `tipoAccion` (AuditoriaTipoAccion), `descripcion`
  - Hecho: compila; el campo `admin` es solo el nombre de usuario, no la entidad JPA (Art. 2)

- [x] **T2.15** Crear `ConfiguracionRequest` en `dtos/` (input, RF-35, RF-36, RF-38)
  - Campos: `precioNoche` (BigDecimal), `estanciaMinima` (int), `estanciaMaxima` (int), todos opcionales (PATCH parcial)
  - Bean Validation: `@DecimalMin("0.01")` en precioNoche y `@Min(1)` en min/max, solo cuando el campo viene presente
  - Hecho: compila

- [x] **T2.16** Crear `ConfiguracionResponse` en `dtos/` (output, solo lectura)
  - Campos: `id`, `precioNoche`, `estanciaMinima`, `estanciaMaxima`
  - Hecho: compila

## Fase 3 — Mappers

Depende de Fase 2 (DTOs) y de las entidades de spec 001. Se usa MapStruct (`@Mapper(componentModel = "spring")`, dependencia ya declarada).

- [x] **T3.1** Crear `ReservaMapper` en `mappers/`
  - `Reserva` ↔ `ReservaResponse`; `ReservaRequest` → `Reserva` (campos escalares + `fechaReserva`)
  - La resolución de `usuario` la fija `ReservaService` (no el mapper, Art. 2)
  - Hecho: `./mvnw compile` genera la implementación sin errores

- [x] **T3.2** Crear `UsuarioMapper` en `mappers/`
  - `Usuario` ↔ `UsuarioResponse`; `UsuarioRequest` → `Usuario`
  - Hecho: compila con implementación generada

- [x] **T3.3** Crear `MensajeMapper` en `mappers/`
  - `Mensaje` ↔ `MensajeResponse`
  - Hecho: compila con implementación generada

- [x] **T3.4** Crear `AuditoriaMapper` en `mappers/`
  - `Auditoria` ↔ `AuditoriaResponse` (mapear `admin → admin.nombreUsuario | null`)
  - Hecho: compila con implementación generada

- [x] **T3.5** Crear `ConfiguracionMapper` en `mappers/`
  - `Configuracion` ↔ `ConfiguracionResponse`; `Configuracion` ↔ `ConfiguracionRequest`
  - Hecho: compila con implementación generada

- [x] **T3.6** Crear `DisponibilidadMapper` en `mappers/`
  - `Reserva` → `FechasOcupadas` (únicamente los 2 campos de RF-1, privacidad RNF)
  - Hecho: compila con implementación generada

## Fase 4 — Repositorios

Depende de las entidades existentes (spec 001). Independientes entre sí.

- [x] **T4.1** Ampliar `ReservaRepository`
  - Extender `JpaSpecificationExecutor<Reserva>` (filtros RF-21)
  - Añadir `List<Reserva> findByEstadoInAndFechaSalidaGreaterThanEqual(List<ReservaEstado> estados, LocalDate fecha)` (RF-1)
  - Añadir variante del solapamiento existente con exclusión: `...AndIdNot(Long id)` (RF-13 modificación, RF-18)
  - Hecho: compila; el método existente de solapamiento se conserva

- [x] **T4.2** Ampliar `UsuarioRepository`
  - Extender `JpaSpecificationExecutor<Usuario>` (RF-28)
  - Añadir `Optional<Usuario> findByCorreoIgnoreCase(String correo)` (RF-4, RF-5, RF-8, RF-23, RF-25)
  - Eliminar `findByCorreo` y actualizar cualquier referencia (grep previo)
  - Hecho: compila; no queda ninguna referencia a `findByCorreo`

- [x] **T4.3** Ampliar `MensajeRepository`
  - Extender `JpaSpecificationExecutor<Mensaje>` (filtros `usuarioId` + rango sobre `fechaMensaje`, RF-29)
  - El método `findByUsuarioIdOrderByFechaMensajeAsc` existente se conserva
  - Hecho: compila

- [x] **T4.4** Ampliar `AuditoriaRepository`
  - Extender `JpaSpecificationExecutor<Auditoria>` (filtros `entidadAfectada`, `entidadId`, `tipoAccion`, rango, RF-33)
  - Hecho: compila

- [x] **T4.5** Ampliar `ConfiguracionRepository`
  - Añadir `@Lock(PESSIMISTIC_WRITE)` en `@Query("SELECT c FROM Configuracion c WHERE c.id = 1")` → `Optional<Configuracion> findByIdWithLock()` (RF-13 concurrencia)
  - Hecho: compila; la query funciona en MySQL y H2 (tests)

## Fase 5 — Soporte de autenticación y Auditoría

Depende de Fase 3 (T3.4) y Fase 4 (T4.4). Base para el resto de services.

- [x] **T5.1** Crear `AutenticadoActual` en `services/`
  - Lee `SecurityContextHolder`, extrae `nombre_usuario` y delega en `IAuthenticationService.buscarAdministradorPorNombre(...)` → `Optional<Administrador>`
  - Sirve para cumplir Art. 1 (los services, no controllers ni filtros, resuelven el dato de negocio)
  - Hecho: compila; devuelve `Optional.empty()` cuando no hay admin autenticado

- [x] **T5.2** Crear `IAuditoriaService` + `AuditoriaService` en `services/` (y `services/interfaces/`)
  - Depende de: T5.1, T4.4, T3.4
  - `registrar(AuditoriaTipoAccion, String entidadAfectada, Long entidadId)`: `descripcion` con la plantilla `<Acción> de <entidad> (id=<entidadId>)` (p. ej. "Reserva creada (id=3)"), `admin` desde `AutenticadoActual` (nullable), `fecha=now`
  - `listar(entidadAfectada, entidadId, tipoAccion, fechaDesde, fechaHasta, Pageable) → Page<AuditoriaResponse>` con Specification y orden `fecha` DESC (RF-33)
  - Hecho: compila; la descripción no contiene datos personales (RNF, Art. 11)

## Fase 6 — Configuración

Depende de Fase 3 (T3.5), Fase 4 (T4.5) y Fase 5 (T5.2). RF-34 a RF-37.

- [x] **T6.1** Ampliar `IConfiguracionService` + `ConfiguracionService`
  - Depende de: T3.5, T4.5, T5.2
  - `obtener() → ConfiguracionResponse`: `findById(1L)` → mapper; si no existe → `EntidadNoEncontradaException` (RF-34)
  - `actualizar(ConfiguracionRequest) → ConfiguracionResponse` (RF-35, RF-36, RF-37): PATCH parcial (solo campos presentes); merge sobre la fila actual; validar sobre valores resultantes (`estanciaMinima ≥ 1`, `estanciaMaxima ≥ 1`, `precioNoche > 0`, `estanciaMinima ≤ estanciaMaxima`) → `ConfiguracionInvalidaException`; persistir con `findByIdWithLock`; auditar MODIFICAR; no toca reservas existentes
  - El `getById(Long)` original se reemplaza por `obtener()` (búsqueda de referencias de `getById`)
  - Hecho: compila; no queda referencia a `getById` fuera del service

- [x] **T6.2** Refactorizar `ConfiguracionController`
  - Depende de: T6.1
  - GET `/api/v1/configuracion` → `ResponseEntity<ConfiguracionResponse>` (404 si no existe; sustituye `noContent` y la entidad JPA, Art. 2)
  - Añadir PATCH `/api/v1/configuracion` → `ResponseEntity<ConfiguracionResponse>` con `@Valid ConfiguracionRequest`
  - Hecho: el controller solo recibe/devuelve DTOs (Art. 2); compila

## Fase 7 — Disponibilidad pública

Depende de Fase 4 (T4.1) y Fase 3 (T3.6). RF-1, RF-2.

- [x] **T7.1** Crear `IDisponibilidadService` + `DisponibilidadService`
  - `obtenerFechasOcupadas() → FechasOcupadasResponse` (RF-1, RF-2)
  - Consulta `findByEstadoInAndFechaSalidaGreaterThanEqual([PENDIENTE, CONFIRMADA], LocalDate.now())` (zona horaria local del servidor, RNF)
  - Mapear con `DisponibilidadMapper` (solo `fechaEntrada`/`fechaSalida`)
  - Hecho: compila; la query excluye CANCELADA y reservas pasadas

- [x] **T7.2** Crear `DisponibilidadController`
  - Depende de: T7.1
  - `@GetMapping("/api/v1/disponibilidad")` → `ResponseEntity<FechasOcupadasResponse>`
  - Será público una vez ampliado `PublicEndpoints` (T8.3)
  - Hecho: compila; URL sustantiva (Art. 4, Art. 5)

## Fase 8 — Mensajes de contacto

Depende de Fase 4 (T4.2, T4.3), Fase 3 (T3.2, T3.3) y Fase 5 (T5.2). RF-3 a RF-7, RF-29, RF-31.

- [x] **T8.1** Crear `IMensajeService` + `MensajeService`
  - `enviar(MensajeRequest) → MensajeResponse` (RF-3, RF-4, RF-5, RF-6, RF-31):
    - Buscar por `findByCorreoIgnoreCase(correo)` (RF-4)
    - No existe → crear `Usuario` con los datos aportados; si faltan nombre/apellido → `"Sin nombre"`/`"Sin apellido"` (RF-5, NOT NULL spec 001); auditar CREAR con `admin=null` (RF-31)
    - Existe → asociar sin modificar sus datos, incluso con `visible=false` (RF-4, caso límite)
    - Persistir `Mensaje` con `remitente=USUARIO`, `fechaMensaje=now`, asunto aportado o `"Consulta"` (RF-3, RF-6)
  - `listar(usuarioId, fechaDesde, fechaHasta, Pageable) → Page<MensajeResponse>` (RF-29) con Specification y orden `fechaMensaje` DESC; devuelve `MensajeResponse` completo (endpoint privado, la RNF de privacidad solo aplica a RF-1)
  - Sin endpoints de modificar/eliminar (RF-7)
  - Hecho: compila

- [x] **T8.2** Crear `MensajeController`
  - Depende de: T8.1
  - `POST /api/v1/mensajes` (público) → `ResponseEntity<MensajeResponse>` con HTTP 201 (RF-3, RF-6)
  - `GET /api/v1/mensajes` (privado) → `ResponseEntity<Page<MensajeResponse>>` (RF-29)
  - Sin rutas de modificar/eliminar (RF-7)
  - Hecho: compila; solo existen POST y GET

- [x] **T8.3** Ampliar `PublicEndpoints` en `config/`
  - Depende de: T7.2, T8.2
  - Añadir como públicos: `GET /api/v1/disponibilidad` y `POST /api/v1/mensajes`, combinados con `SESION` vía `RequestMatchers.anyOf` (o matchers adicionales equivalentes)
  - Hecho: los 2 endpoints públicos + sesión funcionan sin token; el resto sigue exigiendo JWT

## Fase 9 — Usuarios (administrador)

Depende de Fase 4 (T4.2), Fase 3 (T3.2) y Fase 5 (T5.2). RF-22 a RF-28.

- [x] **T9.1** Crear `IUsuarioService` + `UsuarioService`
  - `crear(UsuarioRequest) → UsuarioResponse` (RF-22, RF-23): unicidad case-insensitive vía `findByCorreoIgnoreCase` → `CorreoDuplicadoException` (409); auditar CREAR
  - `modificar(id, UsuarioUpdateRequest) → UsuarioResponse` (RF-24, RF-25): PATCH parcial; unicidad del nuevo correo excluyendo al propio usuario (comprobación por id); auditar MODIFICAR
  - `cambiarVisible(id, UsuarioVisibleRequest)` (RF-26, RF-27): sin restricciones; no propaga la visibilidad a reservas ni mensajes; auditar ELIMINAR_OCULTAR en ambos sentidos
  - `listar(nombre, apellido, correo, visible, Pageable) → Page<UsuarioResponse>` (RF-28): Specification; si `visible` no viene → `visible=true` por defecto; orden `id` ASC
  - Hecho: compila; unicidad y visibilidad con la semántica correcta

- [x] **T9.2** Crear `UsuarioController`
  - Depende de: T9.1
  - `POST /api/v1/usuarios` (RF-22), `PATCH /api/v1/usuarios/{id}` (RF-24), `PATCH /api/v1/usuarios/{id}/visible` (RF-26), `GET /api/v1/usuarios` paginado (RF-28)
  - Todos privados (JWT)
  - Hecho: compila; verbos y rutas según Art. 4, Art. 5

## Fase 10 — Reservas (administrador)

Depende de Fase 4 (T4.1, T4.2, T4.5), Fase 3 (T3.1, T3.2), Fase 5 (T5.2) y Fase 1 (T1.1). RF-8 a RF-21.

- [x] **T10.1** Crear `IReservaService` + `ReservaService`
  - Métodos `@Transactional`; adquirir `findByIdWithLock()` (lock pesimista fila singleton de Configuración) **antes** de las comprobaciones de solapamiento en crear/modificar/cambiarEstado/cambiarVisible (RF-13, caso límite de dos simultáneas)
  - `crear(ReservaRequest) → ReservaResponse` (RF-8..RF-14):
    - Resolver usuario: `usuarioId` (404 si no existe); inline (crear/validar con unicidad case-insensitive, auditar CREAR del Usuario, RF-8/RF-23); ninguno → null
    - `fechaEntrada < fechaSalida` → `FechasInvalidasException` (RF-10)
    - Noches (`ChronoUnit.DAYS`) entre `estancia_minima`/`estancia_maxima` vigentes → `DuracionEstanciaInvalidaException` (RF-11)
    - `numeroHuespedes` 1..`ReservaReglas.MAX_HUESPEDES` → `NumeroHuespedesInvalidoException` (RF-12)
    - Solapamiento con PENDIENTE/CONFIRMADA (los 3 estados de la reserva nueva quedan sujetos a la comprobación) → `SolapamientoReservaException` (RF-13)
    - `fechaReserva=now`, `visible=true`, estado por defecto `CONFIRMADA` o el aportado (RF-14)
    - Precio = `precio_noche × noches`, `BigDecimal` scale 2 con `ROUND_HALF_UP` (RF-9)
    - Auditar CREAR de la reserva (RF-30)
  - `modificar(id, ReservaUpdateRequest)` (RF-15, RF-16):
    - `usuario` según presencia del deserializador: `usuarioId` reutilizado / inline (con CREAR del Usuario) / `null` → desvincular
    - RF-10 y RF-12 siempre si el campo viene presente; RF-13 con `...AndIdNot` excluyendo self (casos límite: modificación que solapa consigo misma no se autorrechaza)
    - RF-11 solo si la duración resultante difiere de la actual (RF-37)
    - Si cambian `fechaEntrada`/`fechaSalida` → recalcular precio con `precio_noche` vigente; si no, se mantiene (RF-16, caso límite)
    - Auditar MODIFICAR (+ CREAR si hubo usuario inline)
  - `cambiarEstado(id, ReservaCambioEstadoRequest)` (RF-17, RF-18): transición libre entre los 3 estados; si CANCELADA → PENDIENTE/CONFIRMADA y solapa → `SolapamientoReservaException` (409); sin recálculo de precio; auditar CAMBIAR_ESTADO
  - `cambiarVisible(id, ReservaVisibleRequest)` (RF-19, RF-20): ocultar solo si CANCELADA o `fechaSalida` pasada → si no, `OcultacionInvalidaException`; mostrar sin restricción; auditar ELIMINAR_OCULTAR
  - `listar(estado, visible, usuarioId, fechaDesde, fechaHasta, Pageable) → Page<ReservaResponse>` (RF-21): Specification, rango sobre `fechaEntrada`; `visible=true` por defecto con override por filtro; orden `fechaEntrada` ASC
  - Hecho: compila; los casos límite del plan están cubiertos en el flujo

- [x] **T10.2** Crear `ReservaController`
  - Depende de: T10.1
  - `POST /api/v1/reservas` (RF-8), `PATCH /api/v1/reservas/{id}` (RF-15), `PATCH /api/v1/reservas/{id}/estado` (RF-17), `PATCH /api/v1/reservas/{id}/visible` (RF-19), `GET /api/v1/reservas` paginado (RF-21)
  - Todos privados (JWT)
  - Hecho: compila; verbos y rutas según Art. 4, Art. 5

- [x] **T10.3** Crear `AuditoriaController`
  - Depende de: T5.2
  - `GET /api/v1/auditoria` → `ResponseEntity<Page<AuditoriaResponse>>` (RF-33) con filtros `entidadAfectada`, `entidadId`, `tipoAccion`, rango de fechas y paginado
  - Solo GET: sin endpoints de creación/edición/borrado (RF-32); privado (JWT)
  - Hecho: compila; devuelve solo DTOs (Art. 2)

## Fase 11 — Tests

Depende de todas las fases anteriores. Paquete `src/test/java/es/elpajaroverde/` (perfil `test`, H2). RF-1 a RF-38 cubiertos (Art. 6).

- [ ] **T11.1** Preparar seed de `Configuracion(id=1)` para tests de integración
  - Asegurar que los integration tests que necesitan precio/lock disponen de la fila singleton (setup o script SQL de test)
  - Hecho: `ConfiguracionIntegrationTest`, `ReservaIntegrationTest` y demás en los que aplique arrancan con la fila presente

- [ ] **T11.2** Crear `ConfiguracionServiceTest` (unitario, Mockito, RF-35, RF-36, RF-37)
  - PATCH parcial aplica solo campos presentes; rechaza `min > max`; rechaza límites no positivos; acepta `min = max`; no recalcula reservas previas
  - Hecho: tests en verde

- [ ] **T11.3** Crear `AuditoriaServiceTest` (unitario, RF-33)
  - `registrar` produce la plantilla `<Acción> de <entidad> (id=<entidadId>)` sin datos personales; `listar` filtra por entidad/tipo/rango y ordena `fecha` DESC
  - Hecho: tests en verde

- [ ] **T11.4** Crear `DisponibilidadServiceTest` (unitario, RF-1, RF-2)
  - Solo devuelve PENDIENTE/CONFIRMADA futuras; excluye CANCELADA y pasadas; no expone más campos (privacidad RNF)
  - Hecho: tests en verde

- [ ] **T11.5** Crear `MensajeServiceTest` (unitario, RF-3, RF-4, RF-5, RF-6, RF-31)
  - Asunto por defecto `Consulta`; dedupe case-insensitive; usuario oculto reutilizado sin modificar; creación de usuario con placeholders y auditoría `admin=null`
  - Hecho: tests en verde

- [ ] **T11.6** Crear `UsuarioServiceTest` (unitario, RF-22 a RF-27)
  - Unicidad case-insensitive en crear y modificar; modificar excluye self; visible libre sin cascada
  - Hecho: tests en verde

- [ ] **T11.7** Crear `ReservaServiceTest` (unitario, RF-8 a RF-20) con los casos límite de la spec
  - Creación (usuario id/inline/ninguno, precio, duración, huéspedes 1 y 10, solapamiento, retroactivas, estado CANCELADA inicial); modificación (PATCH parcial, RF-11 solo si cambia duración, solape consigo mismo permitido, `usuario:null` desvincula); estado (RF-18 sin recálculo); visible (RF-19/RF-20); auditoría por acción
  - Hecho: tests en verde

- [ ] **T11.8** Crear `ConfiguracionIntegrationTest` (RF-34, RF-35, RF-36, RF-37)
  - GET y PATCH sobre BD H2 persistente; 404 si no existe la fila; validación de la regla cruzada
  - Depende de: T11.1
  - Hecho: tests en verde

- [ ] **T11.9** Crear `AuditoriaIntegrationTest` (RF-30, RF-31, RF-32, RF-33)
  - Registro automático por cada acción (con admin autenticado y `admin=null` en mensaje público); sin endpoints de escritura; listado con filtros
  - Hecho: tests en verde

- [ ] **T11.10** Crear `MensajeContactoIntegrationTest` (RF-3, RF-4, RF-5, RF-6, RF-29, RF-31)
  - Flujo público crea/reutiliza usuario; listado privado filtrado por `usuarioId` y rango
  - Hecho: tests en verde

- [ ] **T11.11** Crear `UsuarioIntegrationTest` (RF-22 a RF-28)
  - Unicidad case-insensitive y visibilidad sobre BD real (H2); filtros y paginación
  - Hecho: tests en verde

- [ ] **T11.12** Crear `ReservaIntegrationTest` (RF-8..RF-21)
  - Flujo completo sobre BD real (H2) + caso límite de dos peticiones simultáneas de creación/reactivación → solo una prospera (RF-13, RF-18)
  - Depende de: T11.1
  - Hecho: tests en verde

- [ ] **T11.13** Crear `DisponibilidadControllerTest` (MockMvc, RF-1, RF-2)
  - GET público sin token → 200; respuesta con solo las fechas (sin datos sensibles)
  - Depende de: T8.3
  - Hecho: tests en verde

- [ ] **T11.14** Crear `MensajeControllerTest` (MockMvc, RF-3, RF-6, RF-7, RF-29)
  - POST público sin token → 201; validación 400; GET privado requiere token (401); ausencia de rutas de modificar/eliminar (404)
  - Hecho: tests en verde

- [ ] **T11.15** Crear `UsuarioControllerTest` (MockMvc, RF-22, RF-24, RF-26, RF-28)
  - Verbos y rutas correctos; 409 por correo duplicado; 401 sin token
  - Hecho: tests en verde

- [ ] **T11.16** Crear `AuditoriaControllerTest` (MockMvc, RF-32, RF-33)
  - Solo GET; filtros y paginación; 401 sin token
  - Hecho: tests en verde

- [ ] **T11.17** Crear `ConfiguracionControllerTest` (MockMvc, RF-34, RF-35, RF-36)
  - GET → 200 con DTO; GET sin fila → 404; PATCH → 400 por Bean Validation y por regla cruzada
  - Hecho: tests en verde

- [ ] **T11.18** Crear `ReservaControllerTest` (MockMvc, RF-8, RF-15, RF-17, RF-19, RF-21)
  - Verbos y rutas; 404/409/400 según el caso; 401 sin token; página fuera de rango → lista vacía (caso límite)
  - Hecho: tests en verde

- [ ] **T11.19** Verificación final
  - `./mvnw test` completo en verde; los 38 RF tienen al menos un test (Art. 6); sin warnings de compilación en `./mvnw clean install`
  - Hecho: suite completa en verde y RF-1 a RF-38 cubiertos