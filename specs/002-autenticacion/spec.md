# Spec 002 — Autenticación

## Contexto y objetivo
Actualmente no existe ningún mecanismo para que el administrador acceda a los endpoints privados del sistema. Esta feature implementa el inicio de sesión mediante usuario y contraseña, la emisión de un token JWT que habilita el acceso a recursos privados, y su cierre de sesión, cumpliendo el nivel de seguridad fijado en la constitución (Art. 9-10).

## Usuarios / actores
- Administrador: único actor del sistema con acceso a endpoints privados. No existen roles diferenciados.

## Historias de usuario
- H1: Como administrador, quiero iniciar sesión con mi usuario y contraseña, para poder acceder a los endpoints privados del sistema.
- H2: Como administrador, quiero que mi sesión expire automáticamente pasado un tiempo, para reducir el riesgo si olvido cerrar sesión.
- H3: Como administrador, quiero poder cerrar sesión manualmente desde un dispositivo concreto, para invalidar el acceso desde ese dispositivo sin afectar a mis otras sesiones activas.
- H4: Como administrador, quiero que mi cuenta se bloquee temporalmente tras varios intentos fallidos consecutivos, para protegerme frente a ataques de fuerza bruta.

## Requisitos funcionales (criterios de aceptación en EARS)

- RF-1: CUANDO el administrador envía nombre_usuario y contraseña correctos al endpoint de login, EL SISTEMA genera un token JWT válido y lo devuelve en la respuesta. El tiempo de expiración del token será configurable.
- RF-2: EL SISTEMA valida mediante Bean Validation que nombre_usuario y contraseña no estén vacíos antes de procesar cualquier intento de login (Art. 7 constitución).
- RF-3: SI las credenciales enviadas no corresponden a un administrador válido (usuario inexistente o contraseña incorrecta), ENTONCES EL SISTEMA rechaza la petición sin emitir ningún token.
- RF-4: EL SISTEMA permite múltiples sesiones (tokens) activas simultáneamente para el mismo administrador, una por cada login realizado.
- RF-5: MIENTRAS el número de intentos fallidos consecutivos del administrador sea inferior a 5, EL SISTEMA procesa cada intento de login normalmente.
- RF-6: CUANDO el administrador alcanza 5 intentos fallidos consecutivos (el quinto se procesa y si falla, se bloquea), EL SISTEMA bloquea la cuenta durante un tiempo configurable, rechazando cualquier intento de login durante ese periodo aunque las credenciales sean correctas. El contador de intentos se almacena en memoria.
- RF-7: CUANDO el administrador intenta hacer login tras un bloqueo, EL SISTEMA calcula si han pasado los minutos de bloqueo desde el último intento fallido. Si han pasado, permite el login y reinicia el contador. Si no han pasado, rechaza la petición.
- RF-8: CUANDO el administrador realiza un login correcto, EL SISTEMA reinicia el contador de intentos fallidos a cero. Esto aplica tanto si la cuenta estaba bloqueada y ya se desbloqueó, como si nunca se bloqueó.
- RF-9: CUANDO llega una petición a un endpoint privado sin cabecera `Authorization`, EL SISTEMA rechaza la petición.
- RF-10: SI el token recibido en `Authorization` no es un JWT válido (formato incorrecto o firma inválida), ENTONCES EL SISTEMA rechaza la petición.
- RF-11: SI el token recibido es un JWT válido pero está expirado, ENTONCES EL SISTEMA rechaza la petición.
- RF-12: CUANDO el administrador solicita logout aportando un token válido, EL SISTEMA invalida ese token específico, de forma que deja de dar acceso a endpoints privados aunque no haya expirado todavía. La invalidación se consigue cambiando la firma del JWT, de forma que los tokens anteriores dejan de ser verificables.
- RF-13: CUANDO un administrador cierra sesión desde un dispositivo, EL SISTEMA mantiene activas el resto de sesiones (tokens) abiertas en otros dispositivos.
- RF-14: EL SISTEMA nunca registra la contraseña ni ningún fragmento del token JWT en logs de aplicación (Art. 11 constitución).
- RF-15: CUANDO un administrador solicita logout sin token, o con un token que ya está expirado, EL SISTEMA permite la operación de forma silenciosa (no devuelve error).

## Requisitos no funcionales
- Seguridad: las contraseñas se validan contra el hash almacenado con BCrypt (Art. 10); nunca se comparan en texto plano.
- Seguridad: el `nombre_usuario` distingue mayúsculas/minúsculas al hacer login. La collation de la columna en BD debe garantizar esto.
- Seguridad: la verificación de firma del JWT se realiza en cada petición a un endpoint privado, sin excepciones.
- Seguridad: el tiempo de expiración del token, el número máximo de intentos fallidos y el tiempo de bloqueo son configurables.
- Idioma: los mensajes de error se devuelven en español, siguiendo el formato de error fijado en Art. 8 (campo `message`).
- Idioma: el mensaje de error cuando la cuenta está bloqueado es el mismo que cuando la contraseña es incorrecta.

## Casos límite
- Login con nombre_usuario o contraseña vacíos.
- Login con nombre_usuario que no existe en el sistema.
- Login con contraseña incorrecta para un usuario existente.
- Login intentado mientras la cuenta está bloqueada, incluso con credenciales correctas.
- Login justo en el instante en que expira el bloqueo temporal (condición de carrera entre "sigue bloqueado" y "ya se desbloqueó"): se calcula al hacer login.
- Petición a endpoint privado con token bien formado pero manipulado (firma inválida).
- Petición a endpoint privado con token válido pero ya cerrado por logout.
- Logout enviado sin token, o con un token que ya estaba expirado: se permite silenciosamente.
- Logout solicitado dos veces seguidas con el mismo token.
- Login simultáneo desde múltiples dispositivos con las mismas credenciales: ambos intentos se procesan, si fallan ambos incrementan el contador.
- Reinicio del servidor durante un bloqueo activo: el bloqueo se pierde (el contador está en memoria).

## Fuera de alcance
- Cambio de contraseña del administrador.
- Recuperación de contraseña olvidada.
- Registro de nuevos administradores.
- Roles o permisos diferenciados.
- Refresh token / renovación de sesión sin volver a hacer login.
- Autenticación de doble factor.
- Auditar eventos de inicio de sesión y logout.
- Rate limiting por IP (el bloqueo es por usuario, no por IP).
- Blacklist de tokens (la invalidación se consigue cambiando la firma).

## Criterios de finalización
- Los 15 requisitos funcionales tienen al menos un test en verde (Art. 6 constitución).
- Demo manual: login correcto devuelve token; login incorrecto lo rechaza; tras 5 fallos la cuenta se bloquea y se desbloquea al intentar login pasados los minutos configurados; un endpoint privado rechaza peticiones sin token, con token inválido o expirado, y las acepta con token válido; logout invalida solo la sesión actual, dejando el resto operativas.

## Dudas abiertas
Ninguna pendiente.