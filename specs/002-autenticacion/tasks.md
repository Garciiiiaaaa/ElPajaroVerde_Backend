# Tareas: Autenticación

Generado a partir de [spec.md](./spec.md) y [plan.md](./plan.md).

## Fase 1 — Configuración y DTOs

Sin dependencias entre sí. Son la base para el resto de fases.

- [x] **T1.1** Añadir propiedades de JWT y autenticación a `application.properties`
  - `app.jwt.secret` (vacío por defecto), `app.jwt.expiration-hours` (8), `app.auth.max-failed-attempts` (5), `app.auth.lockout-minutes` (5)
  - RF: RF-1 (configurable), RNF (configurables)
  - Hecho: propiedades visibles en `application.properties` con valores por defecto

- [x] **T1.2** Crear `LoginRequest` en `dtos/`
  - Campos: `nombreUsuario` (String), `contrasena` (String). Bean Validation: `@NotBlank` en ambos.
  - RF: RF-2
  - Hecho: compila, anotaciones Bean Validation correctas

- [x] **T1.3** Crear `LoginResponse` en `dtos/`
  - Campo: `token` (String). Solo lectura.
  - RF: RF-1
  - Hecho: compila

- [x] **T1.4** Crear `AuthErrorResponse` en `dtos/`
  - Campo: `message` (String). Solo lectura.
  - RF: RF-3, RF-6, RF-9, RF-10, RF-11, Art. 8
  - Hecho: compila

- [x] **T1.5** Modificar entidad `Administrador` — collation `nombre_usuario`
  - Añadir `@Column(columnDefinition = "VARCHAR(50) COLLATE utf8_bin")` al campo `nombre_usuario`
  - RF: RNF (case-sensitivity)
  - Hecho: compila, Hibernate regenera la columna con `ddl-auto=update`

## Fase 2 — Excepciones de autenticación

Sin dependencias. Crear todas las excepciones que usará el service y el filtro.

- [x] **T2.1** Crear excepciones de autenticación en `services/` (o `config/`)
  - Crear: `CredencialesInvalidasException`, `CuentaBloqueadaException`, `TokenInvalidoException`, `TokenExpiradoException`, `SinTokenException`
  - Cada una con campo `message` en español
  - RF: RF-3, RF-6, RF-9, RF-10, RF-11
  - Hecho: todas las excepciones compilan

## Fase 3 — Utilidades JWT y bloqueo de cuenta

Depende de Fase 1 (T1.1 para properties). Las dos tareas son independientes entre sí.

- [x] **T3.1** Crear `JwtUtil` en `security/`
  - Método `generateToken(String nombreUsuario)` → String (RF-1)
  - Método `validateToken(String token)` → boolean (RF-10, RF-11)
  - Método `extractUsername(String token)` → String
  - Método `isTokenExpired(String token)` → boolean (RF-11)
  - Método `rotateKey()` → void (RF-12)
  - Claims: `sub`, `iat`, `exp`
  - Carga clave de `app.jwt.secret`; si vacía, genera aleatoria de 256 bits
  - Hecho: compila, genera y valida tokens

- [x] **T3.2** Crear `LoginAttemptTracker` en `services/`
  - `ConcurrentHashMap<String, LoginAttempt>` con clase interna `LoginAttempt` (`failedAttempts`, `lockoutTime`)
  - Método `registerFailedAttempt(String nombreUsuario)` → void (RF-5, RF-6)
  - Método `isBlocked(String nombreUsuario)` → boolean (RF-6, RF-7)
  - Método `clearAttempts(String nombreUsuario)` → void (RF-8)
  - Método `canLogin(String nombreUsuario)` → boolean (RF-7): calcula si pasaron los minutos de bloqueo
  - RF: RF-5, RF-6, RF-7, RF-8
  - Hecho: compila

## Fase 4 — Servicio de autenticación

Depende de Fase 1 (T1.2–T1.3), Fase 2 (T2.1), Fase 3 (T3.1, T3.2), y `AdministradorRepository` (ya existe).

- [x] **T4.1** Crear `AuthenticationService` en `services/`
  - Dependencias: `AdministradorRepository`, `JwtUtil`, `LoginAttemptTracker`, `BCryptPasswordEncoder`
  - Método `login(LoginRequest)` → `LoginResponse` (RF-1, RF-3, RF-5, RF-6, RF-7, RF-8)
    - Verificar bloqueo → buscar admin → verificar BCrypt → generar token o lanzar excepción
  - Método `logout(String token)` → `AuthErrorResponse` (RF-12, RF-13, RF-15)
    - Token null/expirado → retorno silencioso. Válido → rotar clave.
  - Método `buscarAdministradorPorNombre(String)` → `Optional<Administrador>` (Art. 1)
    - Para que el filtro JWT acceda vía Service, nunca vía Repository
  - RF: RF-1, RF-3, RF-4, RF-5, RF-6, RF-7, RF-8, RF-12, RF-13, RF-15, Art. 1
  - Hecho: compila

## Fase 5 — Filtro JWT y configuración de seguridad

Depende de Fase 3 (T3.1 JwtUtil) y Fase 4 (T4.1 AuthenticationService).

- [x] **T5.1** Crear `JwtFilter` en `security/`
  - Extiende `OncePerRequestFilter`
  - Dependencias: `JwtUtil`, `AuthenticationService`
  - Flujo: URL pública → skip. Sin cabecera → 401 (endpoints privados) o permitir (DELETE logout). Token inválido → 401. Expirado → 401. Válido → autenticar en SecurityContext.
  - RF: RF-9, RF-10, RF-11, RF-15, Art. 1
  - Hecho: compila

- [x] **T5.2** Crear `SecurityConfig` en `config/`
  - `@Configuration` + `@EnableWebSecurity`
  - CSRF off, sesiones stateless
  - `JwtFilter` antes de `UsernamePasswordAuthenticationFilter`
  - Públicos: `POST /api/v1/sesion` (login), `DELETE /api/v1/sesion` (logout)
  - Resto: autenticado
  - `BCryptPasswordEncoder` como bean (Art. 10)
  - RF: RF-9 (endpoints privados), RF-15 (logout público), Art. 4, Art. 5, Art. 10
  - Hecho: compila, Spring Security configurado

## Fase 6 — Controlador y manejo de errores

Depende de Fase 1 (T1.2–T1.4), Fase 2 (T2.1), Fase 4 (T4.1).

- [x] **T6.1** Crear `AuthenticationController` en `controllers/`
  - `@RestController` + `@RequestMapping("/api/v1/sesion")`
  - `POST` → `login(@RequestBody @Valid LoginRequest)` → `ResponseEntity<LoginResponse>` (RF-1, RF-2)
  - `DELETE` → `logout(@RequestHeader(value = "Authorization", required = false) String authHeader)` → `ResponseEntity<AuthErrorResponse>` (RF-12, RF-15)
  - Dependencia: `AuthenticationService`
  - RF: RF-1, RF-2, RF-12, RF-15, Art. 1, Art. 2, Art. 4, Art. 5
  - Hecho: compila

- [x] **T6.2** Crear `GlobalExceptionHandler` en `config/`
  - `@RestControllerAdvice`
  - Captura: `CredencialesInvalidasException`, `CuentaBloqueadaException`, `TokenInvalidoException`, `TokenExpiradoException`, `SinTokenException` → `AuthErrorResponse` con HTTP 401
  - Captura: `MethodArgumentNotValidException` (Bean Validation) → respuesta con `message` genérico + `errors` por campo, HTTP 400
  - Captura: `MissingRequestHeaderException` (Authorization header faltante en endpoints privados) → `AuthErrorResponse` "Token no proporcionado", HTTP 401
  - RF: RF-3, RF-6, RF-9, RF-10, RF-11, RF-2, Art. 7, Art. 8
  - Hecho: compila

## Fase 7 — Tests

Depende de todas las fases anteriores.

- [x] **T7.1** Crear `JwtUtilTest` en `test/`
  - Test: generar token → extraer username → verificar no expirado
  - Test: token expirado → `validateToken` devuelve false
  - Test: token con firma manipulada → `validateToken` devuelve false
  - Test: `rotateKey` → tokens antiguos dejan de ser válidos
  - RF: RF-1, RF-10, RF-11, RF-12
  - Hecho: tests en verde

- [x] **T7.2** Crear `LoginAttemptTrackerTest` en `test/`
  - Test: 4 fallos → no bloqueado (RF-5)
  - Test: 5 fallos → bloqueado (RF-6)
  - Test: bloqueado + minutos no pasados → `canLogin` false (RF-7)
  - Test: bloqueado + minutos pasados → `canLogin` true (RF-7)
  - Test: login correcto → `clearAttempts` limpia (RF-8)
  - RF: RF-5, RF-6, RF-7, RF-8
  - Hecho: tests en verde

- [x] **T7.3** Crear `AuthenticationServiceTest` en `test/` (Unitario con Mockito)
  - Test: login exitoso → devuelve `LoginResponse` con token (RF-1)
  - Test: login usuario inexistente → `CredencialesInvalidasException` (RF-3)
  - Test: login contraseña incorrecta → `CredencialesInvalidasException` (RF-3)
  - Test: login tras 5 fallos → `CuentaBloqueadaException` (RF-6)
  - Test: login con bloqueo expirado → permite login (RF-7)
  - Test: login correcto → reinicia contador (RF-8)
  - Test: logout token válido → rotar clave (RF-12)
  - Test: logout token null → retorno silencioso (RF-15)
  - Test: login múltiples sesiones → ambos tokens válidos (RF-4)
  - RF: RF-1, RF-3, RF-4, RF-5, RF-6, RF-7, RF-8, RF-12, RF-15
  - Hecho: tests en verde

- [x] **T7.4** Crear `AuthenticationControllerTest` en `test/` (MockMvc)
  - Test: POST /api/v1/sesion con credenciales correctas → 200 + token (RF-1)
  - Test: POST /api/v1/sesion con campos vacíos → 400 Bean Validation (RF-2)
  - Test: POST /api/v1/sesion con credenciales incorrectas → 401 (RF-3)
  - Test: DELETE /api/v1/sesion sin token → 200 silencioso (RF-15)
  - RF: RF-1, RF-2, RF-3, RF-15, Art. 7, Art. 8
  - Hecho: tests en verde

- [x] **T7.5** Crear `JwtFilterTest` en `test/` (MockMvc)
  - Test: endpoint privado sin cabecera → 401 "Token no proporcionado" (RF-9)
  - Test: endpoint privado con token firma inválida → 401 "Token inválido" (RF-10)
  - Test: endpoint privado con token expirado → 401 "Token expirado" (RF-11)
  - Test: endpoint privado con token válido → 200
  - Test: endpoint privado con token invalidado por logout (rotación) → 401 (RF-12)
  - Test: DELETE /api/v1/sesion sin token → 200 silencioso (RF-15)
  - RF: RF-9, RF-10, RF-11, RF-12, RF-15
  - Hecho: tests en verde

- [x] **T7.6** Crear `AuthenticationIntegrationTest` en `test/` (Spring Boot Test)
  - Test: login exitoso → token → usar token en endpoint privado → 200
  - Test: login fallido ×5 → bloqueo → login con credenciales correctas → rechazado
  - Test: esperar minutos de bloqueo → login correcto → permite acceso
  - RF: RF-1, RF-3, RF-6, RF-7, RF-8
  - Hecho: tests en verde
