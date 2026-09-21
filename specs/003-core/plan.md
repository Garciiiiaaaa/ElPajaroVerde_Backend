# Plan: Núcleo de Negocio — Dominio Casa Rural

Traduce el modelo del [spec.md](./spec.md) a decisiones técnicas concretas. Cada decisión referencia los RF que cubre y, cuando aplica, la alternativa descartada.

## Clasificación de endpoints (Art. 9 constitución)

| Endpoint | Método | Público/Privado | RF cubierto |
|---|---|---|---|
| `/api/v1/disponibilidad` | GET | Público | RF-1, RF-2 |
| `/api/v1/mensajes` | POST | Público | RF-3, RF-4, RF-5, RF-6 (RF-7: ausencia de endpoints de modificar/eliminar) |
| `/api/v1/reservas` | POST | Privado | RF-8, RF-9, RF-10, RF-11, RF-12, RF-13, RF-14 |
| `/api/v1/reservas/{id}` | PATCH | Privado | RF-15, RF-16 (además RF-10, RF-11, RF-12, RF-13) |
| `/api/v1/reservas/{id}/estado` | PATCH | Privado | RF-17, RF-18 |
| `/api/v1/reservas/{id}/visible` | PATCH | Privado | RF-19, RF-20 |
| `/api/v1/reservas` | GET | Privado | RF-21 |
| `/api/v1/usuarios` | POST | Privado | RF-22, RF-23 |
| `/api/v1/usuarios/{id}` | PATCH | Privado | RF-24, RF-25 |
| `/api/v1/usuarios/{id}/visible` | PATCH | Privado | RF-26, RF-27 |
| `/api/v1/usuarios` | GET | Privado | RF-28 |
| `/api/v1/mensajes` | GET | Privado | RF-29 |
| `/api/v1/auditoria` | GET | Privado | RF-33 (RF-30, RF-31 se registran automáticamente; RF-32: sin endpoints de escritura) |
| `/api/v1/configuracion` | GET | Privado | RF-34 |
| `/api/v1/configuracion` | PATCH | Privado | RF-35, RF-36, RF-37 |

**Justificación de verbos:** la modificación es parcial (RF-15 y RF-35 describen campos individuales), por lo que se usa PATCH. Los sub-recursos `/estado` y `/visible` son actualizaciones de un solo campo de la reserva, no acciones externas, por lo que también usan PATCH. Todos los endpoints privados exigen JWT válido (Art. 9, spec 002).

**Seguridad (Art. 9):** se amplía `PublicEndpoints` (en `config/`) únicamente con `GET /api/v1/disponibilidad` y `POST /api/v1/mensajes`. El resto de los endpoints de esta spec quedan privados con el filtro JWT existente, sin cambios en `SecurityConfig`.

## DTOs (Art. 2, Art. 3, Art. 7 constitución)

Paquete: `dtos/`

**Convención:** en los PATCH, todos los campos son opcionales; un campo `null` no se modifica. Las restricciones de Bean Validation se aplican solo cuando el campo viene presente (con valores `null` no evalúan; con cadenas no se envían jamás vacías, se omite el campo).

| DTO | Tipo | Campos | Bean Validation | RF cubierto |
|---|---|---|---|---|
| `FechasOcupadas` | Output | `fechaEntrada` (LocalDate), `fechaSalida` (LocalDate) | Solo lectura | RF-1, RF-2 |
| `FechasOcupadasResponse` | Output | `ocupadas` (List<FechasOcupadas>) | Solo lectura | RF-1, RF-2 |
| `MensajeRequest` | Input | `correo`, `mensaje`, `nombre`, `apellido`, `telefono`, `asunto` | `@NotBlank @Email` en correo; `@NotBlank` en mensaje; resto opcional. `asunto` en blanco → se persiste `Consulta` (RF-3) | RF-3, RF-4, RF-5, RF-6 |
| `MensajeResponse` | Output | `id`, `fechaMensaje`, `asunto`, `mensaje`, `remitente`, `usuario` (UsuarioResponse) | Solo lectura | RF-6, RF-29 |
| `ReservaRequest` | Input | `fechaEntrada`, `fechaSalida`, `numeroHuespedes`, `estado` (opcional), y **una** de: `usuarioId`, `usuario` inline `{nombre, apellido, correo, telefono}`, o ninguno | `@NotNull` fechaEntrada/fechaSalida; `@NotNull @Min(1) @Max(value = ReservaReglas.MAX_HUESPEDES)` numeroHuespedes; usuario inline: `@NotBlank` en nombre/apellido/correo y `@Email` en correo (RF-22) | RF-8, RF-9, RF-10, RF-11, RF-12, RF-13, RF-14 |
| `ReservaUpdateRequest` | Input | `fechaEntrada`, `fechaSalida`, `numeroHuespedes`, `usuarioId`, `usuario` inline, o `usuario` presente con valor `null` (desvincular) | Mismas anotaciones que `ReservaRequest`, solo cuando el campo viene presente. La presencia de `usuario` se detecta con un deserializador de presencia (ver reglas de consistencia) | RF-15, RF-16 |
| `ReservaResponse` | Output | `id`, `fechaReserva`, `fechaEntrada`, `fechaSalida`, `numeroHuespedes`, `precio`, `estado`, `visible`, `usuario` (UsuarioResponse|null) | Solo lectura | RF-8 a RF-21 |
| `ReservaCambioEstadoRequest` | Input | `estado` (ReservaEstado) | `@NotNull` | RF-17, RF-18 |
| `ReservaVisibleRequest` | Input | `visible` (boolean) | `@NotNull` | RF-19, RF-20 |
| `UsuarioRequest` | Input | `nombre`, `apellido`, `correo`, `telefono` | `@NotBlank` nombre/apellido/correo; `@Email` correo | RF-22, RF-23 |
| `UsuarioUpdateRequest` | Input | `nombre`, `apellido`, `correo`, `telefono` | Todos opcionales; `@Email` si correo presente | RF-24, RF-25 |
| `UsuarioResponse` | Output | `id`, `nombre`, `apellido`, `correo`, `telefono`, `visible` | Solo lectura | RF-22 a RF-28 |
| `UsuarioVisibleRequest` | Input | `visible` (boolean) | `@NotNull` | RF-26 |
| `AuditoriaResponse` | Output | `id`, `fecha`, `admin` (nombreUsuario|null), `entidadAfectada`, `entidadId`, `tipoAccion`, `descripcion` | Solo lectura | RF-30 a RF-33 |
| `ConfiguracionRequest` | Input | `precioNoche` (BigDecimal), `estanciaMinima` (int), `estanciaMaxima` (int) | `@DecimalMin("0.01")` en precioNoche y `@Min(1)` en min/max, solo cuando el campo viene presente (PATCH parcial). Regla cruzada `min <= max` en Service sobre los valores resultantes (RF-36) | RF-35, RF-36 |
| `ConfiguracionResponse` | Output | `id`, `precioNoche`, `estanciaMinima`, `estanciaMaxima` | Solo lectura | RF-34 |

**Reglas de consistencia entre campos (Service, no Bean Validation):**
- `ReservaRequest`/`ReservaUpdateRequest` con usuario: `usuarioId` e inline son **excluyentes**; si vienen ambos → `ReferenciaInconsistenteException` (400). El inline exige `nombre`, `apellido` y `correo` no vacíos (RF-22); no aplican los placeholders de RF-5, que son exclusivos del flujo público de mensajes. Si no viene ni `usuarioId` ni `usuario` → reserva sin usuario (RF-8).
- `ReservaUpdateRequest` incorpora un **deserializador de presencia** para `usuario` (Jackson, por defecto, no distingue `usuario:null` de campo ausente): `usuario` presente con valor `null` → desvincula (RF-15); campo ausente → la asociación no cambia; objeto → crea/valida y asocia (reglas de RF-8, incluida la Auditoria CREAR del Usuario en modificación).
- `ReservaRequest` sin `estado` → el Service fija `CONFIRMADA` (RF-14). Si se aporta, se acepta cualquiera de los tres estados (RF-14).

## Mappers (Art. 3 constitución)

Paquete: `mappers/`. Se usa **MapStruct** (`@Mapper(componentModel = "spring")`), versión 1.6.0 ya declarada como dependencia del proyecto.

| Mapper | Pares Entidad↔DTO |
|---|---|
| `ReservaMapper` | `Reserva` ~ `ReservaResponse`; `ReservaRequest`→`Reserva` (campos escalares y fechaReserva) |
| `UsuarioMapper` | `Usuario` ~ `UsuarioResponse`; `UsuarioRequest`→`Usuario` |
| `MensajeMapper` | `Mensaje` ~ `MensajeResponse` |
| `AuditoriaMapper` | `Auditoria` ~ `AuditoriaResponse` |
| `ConfiguracionMapper` | `Configuracion` ~ `ConfiguracionResponse` y ~ `ConfiguracionRequest` |
| `DisponibilidadMapper` | `Reserva` → `FechasOcupadas` (los 2 campos de RF-1) |

**Notas:**
- La resolución del usuario de una reserva (buscar por `usuarioId`, o crear/validar el inline) **no** se hace en el mapper: requiere persistencia y reglas de negocio (RF-8, RF-23), por lo que vive en `ReservaService`. El mapper copia los campos escalares y `ReservaService` fija la asociación tras guardar/usuario resuelto (Art. 2: solo Service conoce el modelo real).
- La creación de Usuario desde mensaje (RF-5) se resuelve en `MensajeService` con `UsuarioMapper`.
- La distinción de presencia en `usuario` de `ReservaUpdateRequest` se resuelve con un deserializador de presencia en el propio DTO (Jackson), no en el mapper.

## Services (Art. 1, Art. 15 constitución)

Interfaces en `services/interfaces/`, implementaciones en `services/` (inyección por constructor, campos `private final`, sin Lombok).

### `IDisponibilidadService` / `DisponibilidadService`
- `obtenerFechasOcupadas() → FechasOcupadasResponse` (RF-1, RF-2).
- Consulta `ReservaRepository.findByEstadoInAndFechaSalidaGreaterThanEqual([PENDIENTE, CONFIRMADA], LocalDate.now())`. `LocalDate.now()` usa la zona horaria local del servidor (RNF). `FechasOcupadas` se construye con `DisponibilidadMapper` (Art. 3). Devuelve únicamente `fechaEntrada`/`fechaSalida`; nunca datos personales ni económicos (RNF privacidad).

### `IMensajeService` / `MensajeService`
- `enviar(MensajeRequest) → MensajeResponse` (RF-3, RF-4, RF-5, RF-6, RF-31).
- Flujo: buscar Usuario por `UsuarioRepository.findByCorreoIgnoreCase(correo)` (RF-4). Si no existe → crear Usuario con los datos aportados; nombre/apellido ausentes → `"Sin nombre"`/`"Sin apellido"` (RF-5, NOT NULL de spec 001) y registrar Auditoria CREAR con `admin=null` (RF-31). Si existe → no se modifica, aunque esté oculto (RF-4, caso límite). Persistir Mensaje con `remitente=USUARIO`, `fechaMensaje=now`, `asunto` dado o `"Consulta"` (RF-3, RF-6).
- No existe endpoint de modificar/eliminar (RF-7): el GET privado de mensajes (RF-29) vive en `IMensajeService.listar(...)` y devuelve `MensajeResponse` completo (`mensaje` y `usuario`). Es un endpoint privado: la RNF de privacidad aplica solo a RF-1.

### `IReservaService` / `ReservaService`
Dependencias: `ReservaRepository`, `UsuarioRepository`, `ConfiguracionRepository` (para precios y lock), `IAuditoriaService`, `ReservaMapper`, `UsuarioMapper`.

- `crear(ReservaRequest) → ReservaResponse` (RF-8..RF-14):
  1. Resolver usuario: `usuarioId` → buscar (404 si no existe); inline → crear o validar con unicidad case-insensitive (RF-23) y auditar CREAR del Usuario (RF-30); ninguno → null (RF-8).
  2. Validar `fechaEntrada < fechaSalida` (RF-10).
  3. Calcular noches con `ChronoUnit.DAYS` y validarlas contra `estancia_minima/maxima` vigentes (RF-11).
  4. Validar `numeroHuespedes` entre 1 y `ReservaReglas.MAX_HUESPEDES` (constante única de negocio = 10, referenciada también por el `@Min`/`@Max` del DTO, RF-12).
  5. Comprobar solapamiento con reservas PENDIENTE/CONFIRMADA (RF-13) — los tres estados de la reserva nueva quedan sujetos a la comprobación.
  6. Fijar `fechaReserva = LocalDate.now()`, `visible=true`, `estado` (por defecto `CONFIRMADA`, RF-14).
  7. Calcular precio = `precio_noche × noches` con `BigDecimal` scale 2, `ROUND_HALF_UP` (RF-9).
  8. Registrar Auditoria CREAR (RF-30).
- `modificar(id, ReservaUpdateRequest) → ReservaResponse` (RF-15, RF-16):
  - Resolución de usuario con la distinción de presencia del deserializador: reutilizar `usuario_id`, crear inline o desvincular con `usuario=null` (RF-15). SI el inline crea un Usuario nuevo, EL SISTEMA audita CREAR de ese Usuario además de la MODIFICAR de la reserva (RF-8, RF-15).
  - Solo se validan los campos presentes: RF-10, RF-12 siempre; RF-11 únicamente si la duración resultante difiere de la actual (decisión aprobada: conservar la misma duración no se rechaza aunque sea no conforme, RF-37); RF-13 excluyendo la propia reserva.
  - Si cambia `fechaEntrada` o `fechaSalida` → recalcular precio con el `precio_noche` vigente (RF-16). Si no cambian, el precio se mantiene (caso límite).
  - Registrar Auditoria MODIFICAR (RF-30).
- `cambiarEstado(id, ReservaCambioEstadoRequest)` (RF-17, RF-18): transición libre entre los tres estados; si pasa de CANCELADA a PENDIENTE/CONFIRMADA, comprobar solapamiento (RF-18) y rechazar con 409 si existe. No recalcula precio (caso límite). Audita CAMBIAR_ESTADO (RF-30).
- `cambiarVisible(id, ReservaVisibleRequest)` (RF-19, RF-20): ocultar solo si `estado=CANCELADA` o `fechaSalida` pasada (RF-19); mostrar sin restricción (RF-20). Audita ELIMINAR_OCULTAR.
- `listar(Filtros, Pageable) → Page<ReservaResponse>` (RF-21): filtros estado, visible, `usuarioId`, rango de fechas sobre `fecha_entrada`; **por defecto `visible=true`** (decisión aprobada, el filtro `visible=false` permite ver las ocultas); orden por defecto `fecha_entrada` ASC.

### `IUsuarioService` / `UsuarioService`
- `crear(UsuarioRequest)` (RF-22): valida unicidad de correo case-insensitive (RF-23) → 409 si existe. Audita CREAR (RF-30).
- `modificar(id, UsuarioUpdateRequest)` (RF-24): valida unicidad del nuevo correo excluyendo al propio usuario (RF-25). Audita MODIFICAR.
- `cambiarVisible(id, UsuarioVisibleRequest)` (RF-26): sin restricciones, y **no** propaga el cambio a reservas ni mensajes (RF-27). Audita ELIMINAR_OCULTAR en ambos sentidos (ocultar y mostrar; es el único valor del enum que aplica según spec 001).
- `listar(Filtros, Pageable)` (RF-28): filtros nombre, apellido, correo, visible; **por defecto `visible=true`**; orden por defecto `id` ASC.

### `IAuditoriaService` / `AuditoriaService`
- `registrar(...)` interno (llamado por los demás services): escribe `tipoAccion`, `fecha=now`, `admin` (nullable), `entidadAfectada`, `entidadId` y `descripcion` con la plantilla `<Acción> de <entidad> (id=<entidadId>)` (p. ej. "Reserva creada (id=3)", "Usuario ocultado (id=5)"), sin datos personales (RNF, spec 001). RF-30, RF-31.
- `listar(Filtros, Pageable)` (RF-33): filtros `entidadAfectada`, `entidadId`, `tipoAccion`, rango de fechas; orden por defecto `fecha` DESC.
- No se exponen endpoints de creación/edición/borrado (RF-32).

### `IConfiguracionService` / `ConfiguracionService` (se amplía)
- `obtener()` (RF-34): el `getById(1L)` existente se adapta para devolver `ConfiguracionResponse` a través de `ConfiguracionMapper`. **Corrige la violación de Art. 2 vigente** (el `ConfiguracionController` actual devuelve la entidad JPA).
- `actualizar(ConfiguracionRequest)` (RF-35, RF-36, RF-37): actualiza solo los campos presentes (PATCH parcial) y valida **sobre los valores resultantes** (`estanciaMinima ≥ 1`, `estanciaMaxima ≥ 1`, `precioNoche > 0`, `estanciaMinima ≤ estanciaMaxima`, RF-36); sobrescribe el singleton (id=1) sin tocar reservas existentes (RF-37). Audita MODIFICAR (RF-30).
- `getById` existente y la nueva lógica siguen en la misma interfaz (`IConfiguracionService`), Art. 15.

### Obtención del administrador autenticado (Art. 1)
Componente `services/AutenticadoActual` que lee `SecurityContextHolder`, extrae `nombre_usuario` y delega en `IAuthenticationService.buscarAdministradorPorNombre(...)`. Los services privados lo usan para poblar `Auditoria.admin`. Cumple Art. 1: los services (no controllers ni filtros) resuelven el dato de negocio.

## Repositorios (ampliaciones)

Paquete: `repositories/`

| Interface | Cambios | RF cubierto |
|---|---|---|
| `ReservaRepository` | añade `JpaSpecificationExecutor<Reserva>` (filtros dinámicos RF-21); `findByEstadoInAndFechaSalidaGreaterThanEqual(List<ReservaEstado>, LocalDate)` (RF-1); variante del `existsBy...` existente con `AndIdNot` para excluir la propia reserva en modificación (RF-13, RF-18) | RF-1, RF-13, RF-18, RF-21 |
| `UsuarioRepository` | añade `JpaSpecificationExecutor<Usuario>` (RF-28); `findByCorreoIgnoreCase(String)` para deduplicación case-insensitive (RF-4, RF-5, RF-8, RF-23, RF-25). El `findByCorreo` existente se reemplaza por este | RF-4, RF-5, RF-8, RF-23, RF-25, RF-28 |
| `MensajeRepository` | añade `JpaSpecificationExecutor<Mensaje>` (filtros `usuarioId` + rango sobre `fecha_mensaje`) | RF-29 |
| `AuditoriaRepository` | añade `JpaSpecificationExecutor<Auditoria>` (filtros `entidadAfectada`, `entidadId`, `tipoAccion`, rango) | RF-33 |
| `ConfiguracionRepository` | añade `findByIdWithLock` con `@Lock(PESSIMISTIC_WRITE)` sobre id=1 | RF-13 (concurrencia) |

**Unicidad case-insensitive (RF-4, RF-23, RF-25, RF-38):** se resuelve en la capa de repositorio con `findByCorreoIgnoreCase` (portable entre MySQL y H2 en tests). La constraint `unique` del modelo (spec 001) actúa como barrera final contra carreras; en MySQL su collation por defecto ya es case-insensitive.

## Concurrencia — caso límite "dos reservas simultáneas" (RF-13)

- **Decisión:** toda operación de creación, modificación, cambio de estado y cambio de visible de Reserva ejecuta en método `@Transactional` y adquiere un lock pesimista **`SELECT ... FOR UPDATE`** sobre la fila singleton de `Configuracion` (id=1) antes de la comprobación de solapamiento.
- **Justificación:** el solapamiento de rangos de fechas no puede garantizarse con una constraint de unicidad en BD; el patrón check-then-act sin lock permite que dos peticiones simultáneas superen la comprobación y persistan ambas. Serializar las escrituras de reservas reaprovecha la fila singleton ya existente (spec 001) y garantiza que solo una prospere.
- **Alternativa descartada:** reintentos con bloqueo optimista (`@Version`), porque la colisión se detecta solo al persistir y el dato no tiene un campo versionable por rango de fechas.
- **Dependencia operativa:** la mecánica de lock y el cálculo de precio suponen que la fila singleton `id=1` de Configuración existe. Los tests la siembran (como en spec 002) y en producción debe inicializarse al desplegar (seed/datos previos).

## Manejo de errores (Art. 8 constitución)

Se amplía `GlobalExceptionHandler` (en `config/`). Todas las excepciones devuelven JSON con `message` en español.

| Excepción | HTTP Status | message | RF |
|---|---|---|---|
| `EntidadNoEncontradaException` | 404 Not Found | "Reserva/Usuario no encontrado" | RF-15, RF-17, RF-19, RF-24, RF-26 |
| `SolapamientoReservaException` | 409 Conflict | "Las fechas solicitadas solapan con otra reserva" | RF-13, RF-18 |
| `CorreoDuplicadoException` | 409 Conflict | "Ya existe un usuario con ese correo" | RF-23, RF-25, RF-8 |
| `FechasInvalidasException` | 400 Bad Request | "La fecha de entrada debe ser anterior a la fecha de salida" | RF-10 |
| `DuracionEstanciaInvalidaException` | 400 Bad Request | "La duración debe estar entre la estancia mínima y máxima" | RF-11 |
| `NumeroHuespedesInvalidoException` | 400 Bad Request | "El número de huéspedes debe estar entre 1 y 10" | RF-12 |
| `OcultacionInvalidaException` | 400 Bad Request | "Solo puede ocultarse una reserva cancelada o pasada" | RF-19 |
| `ConfiguracionInvalidaException` | 400 Bad Request | "La configuración no es válida" | RF-36 |
| `ReferenciaInconsistenteException` | 400 Bad Request | "No se puede indicar usuario_id y usuario a la vez" | RF-8, RF-15 |
| Bean Validation (existente) | 400 Bad Request | `"message": "Error de validación"` + `errors` por campo | RF-3, RF-38, Art. 7 |

## Orden por defecto de los listados (duda abierta de la spec, resuelta)

| Listado | Orden por defecto |
|---|---|
| Reservas (RF-21) | `fecha_entrada` ASC |
| Usuarios (RF-28) | `id` ASC |
| Mensajes (RF-29) | `fecha_mensaje` DESC |
| Auditoría (RF-33) | `fecha` DESC |

Se aplica en el Service construyendo el `Pageable` con el `Sort` correspondiente.

### Paginación y filtros (RNF)

- `page` 0-based (Spring Data) y `size` con default 20 y clamp 1..100.
- Los filtros se envían como query params; los `estado` y `tipo_accion` se parsean a su enum en mayúsculas y un valor inválido devuelve 400.

## Responsabilidades de negocio por RF (resumen de implementación)

- RF-1 / RF-2 → `DisponibilidadService` + `ReservaRepository.findByEstadoInAndFechaSalidaGreaterThanEqual`.
- RF-3 → `MensajeRequest` (Bean Validation) + asunto por defecto `Consulta`.
- RF-4 / RF-5 / RF-6 → `MensajeService` (dedupe case-insensitive, placeholders `Sin nombre`/`Sin apellido`, `remitente=USUARIO`).
- RF-7 → ausencia de endpoints de modificar/eliminar mensajes.
- RF-8 → `ReservaService.crear` (usuario por id / inline / ninguno) + unicidad + Auditoria CREAR del usuario.
- RF-9 → precio = `precio_noche × noches`, `BigDecimal` scale 2 `ROUND_HALF_UP`.
- RF-10 → validación `fechaEntrada < fechaSalida`.
- RF-11 → validación de duración; en modificación solo si cambia.
- RF-12 → `numeroHuespedes` 1..`ReservaReglas.MAX_HUESPEDES` (constante única de negocio).
- RF-13 → comprobación de solapamiento (con `AndIdNot` en modificación) bajo lock pesimista.
- RF-14 → `visible=true` por defecto; estado por defecto `CONFIRMADA`, aceptando cualquiera de los tres en la petición.
- RF-15 / RF-16 → PATCH parcial; usuario id/inline/null; recálculo de precio al cambiar fechas.
- RF-17 / RF-18 → endpoint `/estado`; rechazo si reactivar solapa.
- RF-19 / RF-20 → endpoint `/visible` con regla de ocultación y re-muestra libre.
- RF-21 → listado paginado con filtros y default `visible=true` (decisión aprobada).
- RF-22 a RF-28 → `UsuarioService` (crear/modificar/visible/listar) con unicidad case-insensitive y sin cascada de visibilidad.
- RF-29 → listado de mensajes paginado (solo lectura).
- RF-30 / RF-31 / RF-32 / RF-33 → `AuditoriaService` (registro interno + listado filtrable; sin endpoints de escritura).
- RF-34 / RF-35 → `ConfiguracionService` con DTO y mapper dedicados (Art. 2, Art. 3).
- RF-36 → validación de Configuración (límites positivos + `min ≤ max`).
- RF-37 → la actualización de Configuración no recalcula reservas previas.
- RF-38 → Bean Validation en todos los DTOs de entrada (Art. 7).

## Validación contra la constitución

| Art. | Requisito | Cómo se cumple |
|---|---|---|
| Art. 1 | Controller → Service → Repository | Todos los controllers inyectan interfaces de Service. `DisponibilidadService`, `MensajeService`, `ReservaService`, `UsuarioService`, `AuditoriaService`, `ConfiguracionService` acceden a los repositorios. `AutenticadoActual` delega en `IAuthenticationService` para el admin autenticado, nunca en Repository. |
| Art. 2 | Controllers solo DTOs | Todos los endpoints reciben/devolven DTOs. `ConfiguracionController` se corrige para no devolver la entidad (RF-34). El Repository devuelve entidades al Service, que mapea antes de responder. |
| Art. 3 | Mapper dedicado por par | `mappers/`: `ReservaMapper`, `UsuarioMapper`, `MensajeMapper`, `AuditoriaMapper`, `ConfiguracionMapper`, `DisponibilidadMapper` (MapStruct). |
| Art. 4 | URLs sustantivos, verbos HTTP | `/api/v1/disponibilidad`, `/api/v1/mensajes`, `/api/v1/reservas`, `/api/v1/usuarios`, `/api/v1/auditoria`, `/api/v1/configuracion` + sub-recursos `/estado`, `/visible`. Modificaciones parciales con PATCH. |
| Art. 5 | Versionado `/api/v{n}/` | Prefijo `/api/v1/` en todos los endpoints. |
| Art. 6 | Test por lógica de negocio | Estrategia de tests abajo; los 38 RF tienen al menos un test. |
| Art. 7 | Bean Validation en DTOs de entrada | `MensajeRequest`, `ReservaRequest`, `ReservaUpdateRequest`, `ReservaCambioEstadoRequest`, `ReservaVisibleRequest`, `UsuarioRequest`, `UsuarioUpdateRequest`, `UsuarioVisibleRequest`, `ConfiguracionRequest` (RF-38). |
| Art. 8 | Campo `message` en errores | Tabla de manejo de errores; `GlobalExceptionHandler` devuelve `message` en español. |
| Art. 9 | JWT, público/privado | Clasificación documentada arriba; solo los 2 públicos se añaden a `PublicEndpoints`; el resto exige JWT. |
| Art. 10 | BCrypt para contraseñas | Sin cambios: se reutiliza el `BCryptPasswordEncoder` de spec 002. |
| Art. 11 | Sin datos sensibles en logs | `Auditoria.descripcion` sin datos personales (RNF); los services no loguean correos, teléfonos ni tokens. |
| Art. 15 | Interfaces en servicios | `IDisponibilidadService`, `IMensajeService`, `IReservaService`, `IUsuarioService`, `IAuditoriaService`; `IConfiguracionService` ampliada. Controllers inyectan las interfaces. |

## Estrategia de tests (Art. 6 constitución)

Paquete: `src/test/java/es/elpajaroverde/` (perfil `test`, H2).

### Unitarios (JUnit 5 + Mockito)

| Test | RF cubierto | Qué verifica |
|---|---|---|
| `DisponibilidadServiceTest` | RF-1, RF-2 | Devuelve solo fechas de PENDIENTE/CONFIRMADA futuras; excluye CANCELADA y pasadas; no expone más campos |
| `MensajeServiceTest` | RF-3, RF-4, RF-5, RF-6, RF-31 | Validación; asunto por defecto; dedupe case-insensitive; usuario oculto reutilizado; creación de usuario con placeholders y Auditoria `admin=null` |
| `ReservaServiceTest` | RF-8 a RF-20 | Creación (usuario id/inline/ninguno, estados, price calc, duración, huéspedes, solapamiento, visible); modificación (PATCH parcial, duración solo si cambia, recálculo, usuario null, excluir self); estado (transiciones, RF-18); visible (RF-19/RF-20); auditoría por acción |
| `UsuarioServiceTest` | RF-22 a RF-27 | Unicidad case-insensitive creando y modificando; visible sin cascada; 404 |
| `AuditoriaServiceTest` | RF-33 | Filtros y orden |
| `ConfiguracionServiceTest` | RF-35, RF-36, RF-37 | Acepta rango válido incl. `min=max`; rechaza min>max y límites positivos; no afecta reservas previas |

### Integración (Spring Boot Test + H2)

| Test | RF cubierto | Qué verifica |
|---|---|---|
| `ReservaIntegrationTest` | RF-8, RF-9, RF-11, RF-13, RF-14, RF-15, RF-16, RF-17, RF-18, RF-19, RF-20, RF-21 | Flujo completo de reservas sobre BD real (H2) + caso límite de dos peticiones simultáneas → solo una prospera |
| `UsuarioIntegrationTest` | RF-22, RF-23, RF-24, RF-25, RF-26, RF-27, RF-28 | Flujo de usuarios con unicidad case-insensitive y filtros |
| `MensajeContactoIntegrationTest` | RF-3, RF-4, RF-5, RF-6, RF-29, RF-31 | Mensaje público crea/reutiliza usuario; listado privado |
| `AuditoriaIntegrationTest` | RF-30, RF-31, RF-32, RF-33 | Registro automático por acción; admin correcto en auditoría |
| `ConfiguracionIntegrationTest` | RF-34, RF-35, RF-36, RF-37 | GET y PATCH de configuración persistente |

### Controller tests (MockMvc)

| Test | RF cubierto | Qué verifica |
|---|---|---|
| `DisponibilidadControllerTest` | RF-1, RF-2 | GET público; respuesta limpia sin datos sensibles |
| `MensajeControllerTest` | RF-3, RF-6 | POST público; validación 400; 201 con confirmación |
| `ReservaControllerTest` | RF-8, RF-15, RF-17, RF-19, RF-21 | Verbos y rutas; 404/409/400; privacidad (401 sin token) |
| `UsuarioControllerTest` | RF-22, RF-24, RF-26, RF-28 | Verbos y rutas; 409; privacidad |
| `AuditoriaControllerTest` | RF-32, RF-33 | Solo GET; filtros y paginación |
| `ConfiguracionControllerTest` | RF-34, RF-35, RF-36 | GET/PATCH; 400 por Bean Validation y por regla cruzada |

### Cobertura RF → test

Los casos límite de la spec (`numero_huespedes` en 1 y 10, `estancia_minima = estancia_maxima`, reservas retroactivas, creación en estado CANCELADA, duración no conforme modificable, correo inline duplicado, envío sin asunto con asunto genérico) tienen cobertura explícita en los integration tests correspondientes.

| RF | Tests |
|---|---|
| RF-1, RF-2 | `DisponibilidadServiceTest`, `DisponibilidadControllerTest` |
| RF-3 | `MensajeServiceTest`, `MensajeControllerTest`, `MensajeContactoIntegrationTest` |
| RF-4, RF-5 | `MensajeServiceTest`, `MensajeContactoIntegrationTest` |
| RF-6 | `MensajeServiceTest`, `MensajeControllerTest`, `MensajeContactoIntegrationTest` |
| RF-7 | `AuditoriaControllerTest`-style prueba de ausencia en `MensajeControllerTest` |
| RF-8, RF-9 | `ReservaServiceTest`, `ReservaIntegrationTest`, `ReservaControllerTest` |
| RF-10, RF-11, RF-12 | `ReservaServiceTest`, `ReservaIntegrationTest` |
| RF-13 | `ReservaServiceTest`, `ReservaIntegrationTest` (incl. caso de dos simultáneas) |
| RF-14 | `ReservaServiceTest`, `ReservaIntegrationTest` |
| RF-15, RF-16 | `ReservaServiceTest`, `ReservaIntegrationTest`, `ReservaControllerTest` |
| RF-17, RF-18 | `ReservaServiceTest`, `ReservaIntegrationTest`, `ReservaControllerTest` |
| RF-19, RF-20 | `ReservaServiceTest`, `ReservaIntegrationTest`, `ReservaControllerTest` |
| RF-21 | `ReservaIntegrationTest`, `ReservaControllerTest` |
| RF-22, RF-23 | `UsuarioServiceTest`, `UsuarioIntegrationTest`, `UsuarioControllerTest` |
| RF-24, RF-25 | `UsuarioServiceTest`, `UsuarioIntegrationTest` |
| RF-26, RF-27 | `UsuarioServiceTest`, `UsuarioIntegrationTest` |
| RF-28 | `UsuarioIntegrationTest`, `UsuarioControllerTest` |
| RF-29 | `MensajeContactoIntegrationTest`, `MensajeControllerTest` |
| RF-30 | `AuditoriaIntegrationTest`, `ReservaServiceTest`, `UsuarioServiceTest`, `ConfiguracionServiceTest` |
| RF-31 | `MensajeServiceTest`, `MensajeContactoIntegrationTest`, `AuditoriaIntegrationTest` |
| RF-32 | `AuditoriaIntegrationTest` |
| RF-33 | `AuditoriaServiceTest`, `AuditoriaIntegrationTest`, `AuditoriaControllerTest` |
| RF-34 | `ConfiguracionIntegrationTest`, `ConfiguracionControllerTest` |
| RF-35 | `ConfiguracionServiceTest`, `ConfiguracionIntegrationTest`, `ConfiguracionControllerTest` |
| RF-36 | `ConfiguracionServiceTest`, `ConfiguracionIntegrationTest`, `ConfiguracionControllerTest` |
| RF-37 | `ConfiguracionServiceTest` |
| RF-38 | Bean Validation cubierta en todos los Controller tests (400 con `errors`) |

## Dudas resueltas y cuestiones detectadas fuera de alcance

**De la spec, resueltas en este plan:**
- Orden por defecto de los listados → sección "Orden por defecto de los listados".
- `visible` por defecto en listados → `visible=true` con override por filtro (decisión del usuario, reflectida en RF-21/RF-28).

**Detectadas durante la revisión (fuera del alcance de spec 003, solo se documentan):**
- **Divergencia en spec 002:** la tarea T1.5 preveía `COLLATE utf8_bin` en `Administrador.nombre_usuario` y no está aplicada en la entidad. Es un tema de autenticación; migrate a `specs/002-autenticacion/tasks.md` si se quiere corregir.
- **Enmienda aplicada en `specs/002-autenticacion/plan.md`:** la fila Art. 2 ya refleja que `ConfiguracionController` devuelve DTOs (`ConfiguracionResponse`/`ConfiguracionRequest`); la "pendiente de enmienda" queda resuelta mediante RF-34/35.