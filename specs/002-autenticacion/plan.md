# Plan: Autenticación — Dominio Casa Rural

Traduce el modelo del [spec.md](./spec.md) a decisiones técnicas concretas. Cada decisión referencia los RF que cubre y, cuando aplica, la alternativa descartada.

## Clasificación de endpoints (Art. 9 constitución)

| Endpoint | Método | Público/Privado | RF cubierto |
|---|---|---|---|
| `/api/v1/sesion` | POST | Público | RF-1, RF-2, RF-3, RF-5, RF-6, RF-7, RF-8 |
| `/api/v1/sesion` | DELETE | Público* | RF-12, RF-13, RF-15 |
| Todos los demás | (varios) | Privado | RF-9, RF-10, RF-11 |

*DELETE es público en el sentido de que no requiere un token válido para ejecutarse (RF-15 permite logout silencioso sin token o con token expirado). Sin embargo, si se aporta un token válido, se invalida.

**Justificación:** Login (POST) produce un efecto (emisión de token). Logout (DELETE) produce un efecto (invalidación). Ambos son verbos correctos según Art. 4 (acciones no CRUD usan POST o DELETE). Se usa `sesion` (sustantivo singular) en vez de `login`/`logout` (verbos).

**Alternativa descartada:** Dos endpoints separados (`/api/v1/login` y `/api/v1/logout`). Descartado porque usa verbos en las URLs (viola Art. 4).

## JWT — decisiones técnicas

### Algoritmo de firma (RF-1, RF-10, RF-11)

- **Decisión:** HMAC-SHA256 (HS256) con clave simétrica configurable via `application.properties`.
- **Justificación:** Simplifica la rotación de clave en logout (RF-12). Con clave simétrica, rotar la clave es un cambio de valor; con asimétrica requeriría regenerar parejas de claves.
- **Alternativa descartada:** RSA (RS256). Más seguro para distribuir tokens a terceros, pero innecesario en un sistema con un único emisor y verificador. Complejidad sin beneficio.

### Contenido del token (RF-1)

Claims estándar:
- `sub`: nombre_usuario del administrador
- `iat`: fecha de emisión (epoch seconds)
- `exp`: fecha de expiración (epoch seconds)

No se incluye `jti` (ID único) porque no hay blacklist (RF-12 se resuelve por rotación de clave).

### Tiempo de expiración (RF-1, RNF)

- **Decisión:** Configurable via `app.jwt.expiration-hours` en `application.properties`, valor por defecto 8 horas.
- **Justificación:** El spec dice "configurable". Un valor por defecto razonable evita configuración obligatoria en desarrollo.

### Rotación de clave en logout (RF-12, RF-13)

- **Decisión:** Al hacer logout, se genera una nueva clave HMAC y se actualiza en memoria. Todos los tokens firmados con la clave anterior dejan de ser verificables.
- **Justificación:** Cumple RF-12 (invalida tokens emitidos con la clave anterior) y RF-13 (logout invalida todas las sesiones activas, forzando re-autenticación).
- **Alternativa descartada:** Blacklist de tokens en BD. Descartada por el spec (fuera de alcance).

### Firma del token (RF-1)

- **Decisión:** La clave HMAC se carga de `app.jwt.secret` en `application.properties`. Si la propiedad no está definida o está vacía, se genera una clave aleatoria de 256 bits al arrancar y se mantiene en memoria en `JwtUtil`. No se persiste en BD ni en archivo.
- **Justificación:** Permite clave predefinida en producción (estabilidad entre reinicios) o generación automática en desarrollo (sin configuración obligatoria).

## DTOs (Art. 2, Art. 3, Art. 7 constitución)

Paquete: `dtos/`

| DTO | Tipo | Campos | Bean Validation | RF cubierto |
|---|---|---|---|---|
| `LoginRequest` | Input | `nombreUsuario` (String), `contrasena` (String) | `@NotBlank` en ambos | RF-2 |
| `LoginResponse` | Output | `token` (String) | Solo lectura | RF-1 |
| `AuthErrorResponse` | Output | `message` (String) | Solo lectura | RF-3, RF-6, RF-9, RF-10, RF-11 |

**Nota:** `AuthErrorResponse` se usa para respuestas de error con campo `message` (Art. 8). Para errores de validación de Bean Validation, se usa un formato genérico con `message` + `errors` por campo.

**Mappers dedicados (Art. 3):**
- No se necesita mapper para `LoginRequest` → entidad (no hay conversión, se usa `nombreUsuario` directamente).
- `LoginResponse` se construye en el service con el token generado.
- `AuthErrorResponse` se construye directamente, no requiere mapper.

**Alternativa descartada:** Mapper manual en el service para `LoginRequest`. Descartado porque no hay conversión Entidad↔DTO; el DTO solo contiene credenciales.

## Servicio de autenticación

Paquete: `services/`

`AuthenticationService` — dependencias: `AdministradorRepository`, `JwtUtil`

### Login (RF-1, RF-2, RF-3, RF-5, RF-6, RF-7, RF-8)

```
login(LoginRequest) → LoginResponse
```

Flujo:
1. Validar Bean Validation (RF-2) — Esto lo hace Spring automáticamente antes de llegar al service.
2. Verificar si la cuenta está bloqueada (RF-6, RF-7):
   - Si está bloqueada y no han pasado los minutos configurados → lanzar excepción (mismo mensaje que contraseña incorrecta, RNF).
   - Si está bloqueada y sí han pasado → desbloquear, continuar.
3. Buscar administrador por `nombreUsuario` (case-sensitive).
4. Si no existe → lanzar excepción (RF-3).
5. Verificar contraseña con `BCryptPasswordEncoder.matches()` (Art. 10, RF-3).
6. Si no coincide → incrementar contador de fallos. Si alcanza 5, bloquear (RF-6). Lanzar excepción (RF-3).
7. Si coincide → reiniciar contador a cero (RF-8). Generar token JWT. Devolver `LoginResponse`.

**Manejo de errores:** Todas las excepciones de login se traducen a respuestas con `message` en español (Art. 8, RNF). La excepción de validación de Bean Validation devuelve `message` genérico + detalle por campo.

### Logout (RF-12, RF-13, RF-15)

```
logout(String token) → AuthErrorResponse
```

Flujo:
1. Si token es null o expirado → retornar respuesta silenciosa (RF-15).
2. Si token es válido → rotar la clave HMAC en `JwtUtil` (RF-12).
3. Retornar `AuthErrorResponse` (las demás sesiones que usen la nueva clave siguen válidas, RF-13).

**Respuesta:** 200 OK con `AuthErrorResponse` (`message: "Sesión cerrada"`). Si el token es null o expirado (RF-15), también devuelve 200 con el mismo body (operación silenciosa).

### Búsqueda de administrador (Art. 1)

```
buscarAdministradorPorNombre(String nombreUsuario) → Optional<Administrador>
```

Delega en `AdministradorRepository.findByNombreUsuario()`. Expuesto para que el filtro de seguridad obtenga el usuario autenticado sin acceder directamente al Repository.

### Múltiples sesiones (RF-4)

- **Decisión:** Cada login genera un token firmado con la clave vigente. Múltiples sesiones son válidas simultáneamente mientras no se haga logout. Al hacer logout (rotar clave), todos los tokens anteriores quedan invalidados.
- **Justificación:** RF-4 pide múltiples sesiones activas. RF-13 pide que logout invalide todas las sesiones. Ambos se cumplen: múltiples tokens funcionan hasta que el admin decide cerrar sesión.

### Bloqueo de cuenta (RF-5, RF-6, RF-7, RF-8)

**Clase:** `LoginAttemptTracker` (nueva clase en `services/`).

- **Decisión:** Clase separada de `AuthenticationService`, inyectada como dependencia. Contiene un `ConcurrentHashMap<String, LoginAttempt>` en memoria.
- **Justificación:** Separa la lógica de bloqueo de la lógica de autenticación. Facilita los tests unitarios (se puede testear `LoginAttemptTracker` aisladamente).
- **Consecuencia conocida:** Reinicio del servidor pierde el estado de bloqueo (caso límite del spec).

Estructura interna de `LoginAttempt` (clase estática dentro de `LoginAttemptTracker`):
```java
class LoginAttempt {
    int failedAttempts;
    LocalDateTime lockoutTime;  // null si no está bloqueado
}
```

Lógica:
- `failedAttempts` se incrementa en cada fallo (RF-5).
- Cuando `failedAttempts == 5`, se registra `lockoutTime = ahora` y la cuenta se bloquea (RF-6).
- Al intentar login, si `lockoutTime != null`:
  - Si `ahora - lockoutTime < minutosBloqueo` → rechazar (RF-7).
  - Si `ahora - lockoutTime >= minutosBloqueo` → permitir login, limpiar intento (RF-7).
- Login correcto → eliminar entrada del mapa (RF-8).

### Configuración (RNF)

Parámetros configurables en `application.properties`:
- `app.jwt.secret` — clave HMAC (generada si no se provee)
- `app.jwt.expiration-hours` — horas de expiración (default: 8)
- `app.auth.max-failed-attempts` — intentos máximos (default: 5)
- `app.auth.lockout-minutes` — minutos de bloqueo (default: 5)

## Filtro de seguridad JWT

Paquete: `security/`

`JwtFilter` — extiende `OncePerRequestFilter`. No es Service ni Repository, actúa en el ciclo de petición (Art. 1).

**Dependencias:** `JwtUtil`, `AuthenticationService`.

**Flujo (RF-9, RF-10, RF-11):**
1. Si la URL es pública (login o logout) → pasar al siguiente filtro.
2. Extraer token de cabecera `Authorization: Bearer {token}`.
3. Si no hay cabecera → rechazar 401 (RF-9).
4. Validar firma y expiración con `JwtUtil`:
   - Firma inválida → rechazar 401 (RF-10).
   - Expirado → rechazar 401 (RF-11).
5. Extraer `nombre_usuario` del `sub` del token.
6. Llamar a `AuthenticationService.buscarAdministradorPorNombre(nombreUsuario)`.
7. Si existe → autenticar en `SecurityContextHolder`.
8. Si no existe → rechazar 401.

**Cumplimiento Art. 1:** El filtro solicita datos de negocio a Service (`AuthenticationService`), nunca al Repository directamente. La cadena se respeta: el filtro actúa como componente externo y delega en Service.

## Configuración de Spring Security

Paquete: `config/`

`SecurityConfig` — clase `@Configuration` con `@EnableWebSecurity`.

- CSRF deshabilitado (API REST stateless, Art. 4).
- Sesiones stateless (no crea `HttpSession`, Art. 4).
- Filtro `JwtFilter` antes de `UsernamePasswordAuthenticationFilter`.
- Endpoints públicos: `POST /api/v1/sesion` (login), `DELETE /api/v1/sesion` (logout).
- Todo lo demás: autenticado.
- `BCryptPasswordEncoder` como bean (Art. 10).

## Collation de nombre_usuario (RNF)

- **Decisión:** Modificar la entidad `Administrador` (ya creada en spec 001) añadiendo `@Column(columnDefinition = "VARCHAR(50) COLLATE utf8_bin")` en el campo `nombre_usuario`. Esto requiere que Hibernate regenere la columna con `ddl-auto=update`.
- **Justificación:** MySQL `utf8_general_ci` es insensible a mayúsculas/minúsculas por defecto. El RNF exige case-sensitivity. `utf8_bin` compara por valor binario, garantizando la distinción.
- **Alternativa descartada:** Usar `utf8mb4_0900_ai_ci` (case-insensitive). No cumple el RNF.

## Manejo de errores (Art. 8 constitución)

Todas las excepciones se capturan en un `@RestControllerAdvice` global que devuelve JSON con campo `message`.

| Excepción | HTTP Status | message | RF |
|---|---|---|---|
| `CredencialesInvalidasException` | 401 Unauthorized | "Credenciales incorrectas" | RF-3 |
| `CuentaBloqueadaException` | 401 Unauthorized | "Credenciales incorrectas" (mismo msg, RNF) | RF-6 |
| `TokenInvalidoException` | 401 Unauthorized | "Token inválido" | RF-10 |
| `TokenExpiradoException` | 401 Unauthorized | "Token expirado" | RF-11 |
| `SinTokenException` | 401 Unauthorized | "Token no proporcionado" | RF-9 |
| `BeanValidationException` (global) | 400 Bad Request | `"message": "Error de validación"` + `errors` | RF-2, Art. 7 |

## Validación contra la constitución

| Art. | Requisito | Cómo se cumple |
|---|---|---|
| Art. 1 | Controller → Service → Repository | `AuthenticationController` inyecta `IAuthenticationService`. `ConfiguracionController` inyecta `IConfiguracionService`. Filtro JWT delega en `IAuthenticationService`, nunca en Repository. |
| Art. 2 | Controllers solo DTOs | `AuthenticationController` recibe `LoginRequest` y devuelve `LoginResponse`/`AuthErrorResponse`. `ConfiguracionController` devuelve `ConfiguracionResponse`/`ConfiguracionRequest` (DTOs) — enmienda resuelta en spec 003, RF-34/35. |
| Art. 3 | Mapper dedicado por par | No se necesita mapper para Login (sin conversión Entidad↔DTO). `LoginResponse` se construye en Service. |
| Art. 4 | URLs sustantivos, verbos HTTP | `/api/v1/sesion` con POST (login) y DELETE (logout). |
| Art. 5 | Versionado `/api/v{n}/` | Prefijo `/api/v1/` en ambos endpoints. |
| Art. 6 | Test por lógica de negocio | Ver sección de tests abajo. |
| Art. 7 | Bean Validation en DTOs de entrada | `LoginRequest` usa `@NotBlank` en ambos campos. |
| Art. 8 | Campo `message` en errores | Todas las excepciones devuelven `AuthErrorResponse` con `message`. |
| Art. 9 | JWT, público/privado | Clasificación documentada arriba. Filtro valida en cada petición privada. |
| Art. 10 | BCrypt para contraseñas | `BCryptPasswordEncoder` como bean. Validación con `matches()`. |
| Art. 11 | Sin datos sensibles en logs | `AuthenticationService` no loguea contraseñas ni tokens. `JwtFilter` no loguea el token. |
| Art. 15 | Interfaces en servicios | `IAuthenticationService`, `ILoginAttemptTracker`, `IConfiguracionService`. Controllers inyectan interfaces. |

## Estrategia de tests (Art. 6 constitución)

Paquete: `src/test/java/es/elpajaroverde/`

### Unitarios (JUnit + Mockito)

| Test | RF cubierto | Qué verifica |
|---|---|---|
| `JwtUtilTest` | RF-1, RF-10, RF-11 | Generación de token, validación de firma, detección de expiración, rotación de clave |
| `AuthenticationServiceTest` | RF-1, RF-3, RF-5, RF-6, RF-7, RF-8 | Login exitoso, login fallido, bloqueo tras 5 fallos, desbloqueo por tiempo, reinicio de contador |
| `LoginAttemptTrackerTest` | RF-5, RF-6, RF-7, RF-8 | Incremento de contador, bloqueo, desbloqueo on-demand, limpieza en login correcto |

### Integración (Spring Boot Test + H2 o Testcontainers)

| Test | RF cubierto | Qué verifica |
|---|---|---|
| `AuthenticationIntegrationTest` | RF-1, RF-3, RF-6, RF-7, RF-8 | Flujo completo: login → token → login fallido → bloqueo → desbloqueo → login correcto |

### Controller tests (MockMvc)

| Test | RF cubierto | Qué verifica |
|---|---|---|
| `AuthenticationControllerTest` | RF-1, RF-2, RF-3 | POST /api/v1/sesion: login exitoso devuelve token, login fallido devuelve error, campos vacíos rechazados por Bean Validation |
| `JwtFilterTest` | RF-9, RF-10, RF-11, RF-12 | Endpoint privado rechaza sin token, con token inválido, con token expirado; acepta token válido; rechaza token invalidado por logout |

### Cobertura total

Los 15 RF quedan cubiertos por al menos un test:
- RF-1: JwtUtilTest, AuthenticationServiceTest, AuthenticationControllerTest
- RF-2: AuthenticationControllerTest
- RF-3: AuthenticationServiceTest, AuthenticationControllerTest
- RF-4: AuthenticationServiceTest (login múltiple)
- RF-5: AuthenticationServiceTest, LoginAttemptTrackerTest
- RF-6: AuthenticationServiceTest, LoginAttemptTrackerTest, AuthenticationIntegrationTest
- RF-7: AuthenticationServiceTest, LoginAttemptTrackerTest, AuthenticationIntegrationTest
- RF-8: AuthenticationServiceTest, LoginAttemptTrackerTest, AuthenticationIntegrationTest
- RF-9: JwtFilterTest
- RF-10: JwtFilterTest
- RF-11: JwtFilterTest
- RF-12: JwtFilterTest
- RF-13: JwtFilterTest (logout mantiene otras sesiones)
- RF-14: No tiene test directo (se verifica por inspección del código: no hay `log.info` con contraseñas/tokens)
- RF-15: AuthenticationControllerTest (logout sin token)
