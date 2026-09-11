# Tareas: Modelo Relacional

Generado a partir de [spec.md](./spec.md) y [plan.md](./plan.md).

## Fase 1 — Enums

Sin dependencias. Las 3 tareas son independientes entre sí.

- [x] **T1.1** Crear `ReservaEstado` en `enums/`
  - Valores: `PENDIENTE, CONFIRMADA, CANCELADA`
  - Hecho: archivo compila sin errores

- [x] **T1.2** Crear `MensajeRemitente` en `enums/`
  - Valores: `USUARIO, ADMIN`
  - Hecho: archivo compila sin errores

- [x] **T1.3** Crear `AuditoriaTipoAccion` en `enums/`
  - Valores: `CREAR, MODIFICAR, ELIMINAR_OCULTAR, CAMBIAR_ESTADO`
  - Hecho: archivo compila sin errores

## Fase 2 — Entidades JPA

Depende de Fase 1. Las entidades sin relaciones entre sí son independientes.

- [x] **T2.1** Crear `Usuario` en `models/`
  - Campos: id, nombre, apellido, correo (unique), telefono, visible
  - Relaciones: OneToMany → Reserva, Mensaje (solo declarar, las listas se añaden cuando existan las otras entidades)
  - Hecho: compila, anotaciones JPA correctas

- [x] **T2.2** Crear `Reserva` en `models/`
  - Dependencias: `T1.1` (ReservaEstado), `T2.1` (Usuario)
  - Campos: id, fechaReserva, fechaEntrada, fechaSalida, numeroHuespedes, precio, estado, visible
  - Relaciones: ManyToOne → Usuario (nullable)
  - Hecho: compila, enum referenciado correctamente

- [x] **T2.3** Crear `Administrador` en `models/`
  - Campos: id, nombreUsuario (unique), contrasena, correo (unique)
  - Relaciones: OneToMany → Auditoria (se declara cuando exista la entidad)
  - Hecho: compila sin errores

- [x] **T2.4** Crear `Mensaje` en `models/`
  - Dependencias: `T1.2` (MensajeRemitente), `T2.1` (Usuario)
  - Campos: id, fechaMensaje, asunto, mensaje (TEXT), remitente
  - Relaciones: ManyToOne → Usuario (not null)
  - Hecho: compila, enum referenciado correctamente

- [x] **T2.5** Crear `Configuracion` en `models/`
  - Campos: id (sin @GeneratedValue, fijo=1), precioNoche, estanciaMinima, estanciaMaxima
  - Sin relaciones FK
  - Hecho: compila, PK sin auto-generación

- [x] **T2.6** Crear `Auditoria` en `models/`
  - Dependencias: `T1.3` (AuditoriaTipoAccion), `T2.3` (Administrador)
  - Campos: id, tipoAccion, fecha, admin (nullable), entidadAfectada (String), entidadId, descripcion
  - Relaciones: ManyToOne → Administrador (nullable)
  - Hecho: compila, enum y relación referenciados correctamente

## Fase 3 — Repositorios

Depende de Fase 2. Cada repositorio depende de su entidad correspondiente.

- [x] **T3.1** Crear `UsuarioRepository` en `repositories/`
  - Extiende `JpaRepository<Usuario, Long>`
  - Método: `Optional<Usuario> findByCorreo(String correo)`
  - Hecho: compila

- [x] **T3.2** Crear `ReservaRepository` en `repositories/`
  - Dependencias: `T1.1` (ReservaEstado)
  - Extiende `JpaRepository<Reserva, Long>`
  - Método: `boolean existsByFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqualAndEstadoIn(LocalDate salida, LocalDate entrada, List<ReservaEstado> estados)`
  - Hecho: compila, firma del método correcta

- [x] **T3.3** Crear `AdministradorRepository` en `repositories/`
  - Extiende `JpaRepository<Administrador, Long>`
  - Método: `Optional<Administrador> findByNombreUsuario(String nombreUsuario)`
  - Hecho: compila

- [x] **T3.4** Crear `MensajeRepository` en `repositories/`
  - Extiende `JpaRepository<Mensaje, Long>`
  - Método: `List<Mensaje> findByUsuarioIdOrderByFechaMensajeAsc(Long usuarioId)`
  - Hecho: compila

- [x] **T3.5** Crear `ConfiguracionRepository` en `repositories/`
  - Extiende `JpaRepository<Configuracion, Long>`
  - Sin métodos custom
  - Hecho: compila

- [x] **T3.6** Crear `AuditoriaRepository` en `repositories/`
  - Extiende `JpaRepository<Auditoria, Long>`
  - Método: `List<Auditoria> findByEntidadAfectadaAndEntidadId(String entidad, Long entidadId)`
  - Hecho: compila
