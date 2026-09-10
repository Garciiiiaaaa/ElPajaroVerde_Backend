# Constitution - API ElPajaroVerde

Principios innegociables. Toda spec, plan y task deben cumplirlos

## Arquitectura

Art. 1 - La comunicación entre capas es estricta: Controller → Service → Repository. Ningún componente puede saltarse una capa (ej. un controller no puede inyectar ni usar un repository directamente).
*Justificación: aísla responsabilidades, facilita el testing por capas y evita acoplamientos que dificulten el mantenimiento.*

Art. 2 - Los controllers nunca reciben ni devuelven entidades JPA. Toda comunicación con el exterior de la API se realiza exclusivamente mediante DTOs.
*Justificación: evita exponer el modelo interno de base de datos, desacopla el contrato público de la API del esquema real, y reduce riesgo de fuga de datos sensibles o de estructura interna.*

Art. 3 - El mapeo entre entidades y DTOs se realiza mediante mappers dedicados (MapStruct), nunca de forma manual dispersa en los services.
*Justificación: consistencia y mantenibilidad del mapeo; MapStruct ya forma parte de las dependencias del proyecto.*

Art. 4 - El diseño de la API sigue los principios REST: los recursos se identifican por URL, se usan los verbos HTTP según su semántica (GET/POST/PUT/PATCH/DELETE), y la API es stateless (no se guarda estado de negocio en sesión de servidor).
*Justificación: es el modelo arquitectónico elegido para el diseño de la API; el statelessness es además requisito para que la autenticación JWT (Art. 8) funcione correctamente.*

## Calidad

Art. 5 - Toda funcionalidad nueva debe incluir al menos un test antes de considerarse completa.
*Justificación: regla mínima acordada para evitar regresiones no detectadas; no se exige cobertura específica, solo que exista test.*

Art. 6 - Todo endpoint que reciba datos de entrada debe validar esos datos mediante Bean Validation en el DTO correspondiente, sin excepción.
*Justificación: evita que datos inválidos o inseguros lleguen al dominio de negocio; regla no negociable independientemente de la criticidad del endpoint.*

Art. 7 - Todos los errores de la API se devuelven en un formato de respuesta único y consistente en toda la aplicación (mismo esquema JSON de error en todos los endpoints).
*Justificación: permite a cualquier consumidor de la API (frontend, cliente externo, tests) manejar errores de forma predecible sin casos especiales por endpoint.*

## Seguridad

Art. 8 - La API usa Spring Security con autenticación basada en JWT. Cada endpoint se clasifica explícitamente como público o privado; los privados exigen un JWT válido, no expirado y con firma verificada en la cabecera `Authorization: Bearer`. No existen roles o niveles de autorización diferenciados por ahora — la única distinción de acceso es autenticado / no autenticado.
*Justificación: fija un nivel de seguridad concreto y verificable en revisión de código; deja constancia expresa de que no hay RBAC todavía, para que no se asuma por error en features futuras.*

Art. 9 - Las contraseñas de usuario nunca se almacenan en texto plano ni con algoritmos reversibles. Se almacenan siempre mediante un algoritmo de hashing seguro con salt (BCrypt o equivalente vigente).
*Justificación: protege las credenciales incluso si la base de datos se ve comprometida; es una regla estándar de seguridad no negociable en cualquier sistema con autenticación propia.*

## Enmiendas

Art. 10 - Esta constitución solo puede modificarse mediante propuesta explícita del agente y aprobación explícita del usuario. Ninguna modificación se aplica de forma autónoma.
*Justificación: mantiene el control humano sobre las reglas fundamentales del proyecto, evitando que cambios de arquitectura o seguridad ocurran sin supervisión.*