# Constitution - API ElPajaroVerde

Principios innegociables. Toda spec, plan y task deben cumplirlos

## Arquitectura

Art. 1 - Un componente es toda clase gestionada por el contenedor de Spring, ya sea anotada directamente o generada como bean por una herramienta de build. La cadena de dependencia obligatoria es Controller → Service → Repository; un controller nunca inyecta ni usa un Repository directamente. El resto de componentes (mappers, validadores, manejadores de excepciones, filtros de seguridad, etc.) no están sujetos a esta cadena y actúan en el punto del ciclo de petición que les corresponda. Si un filtro de seguridad necesita datos de negocio, los solicita a Service, nunca al Repository directamente.
*Justificación: aísla responsabilidades y evita acoplamientos que dificulten el mantenimiento, incluso en componentes que actúan fuera de la cadena principal.*

Art. 2 - Los controllers no conocen entidades JPA ni mappers; solo reciben y devuelven DTOs. Solo la capa de Service conoce el modelo de datos real e invoca los mappers. El Repository puede usar projections internamente, pero nunca las devuelve directamente al Controller; deben pasar por Service.
*Justificación: evita exponer el modelo interno de base de datos y desacopla el contrato público de la API del esquema real, incluso cuando Spring Data ofrece atajos técnicos que podrían saltarse esa barrera.*

Art. 3 - El mapeo entre entidades y DTOs se realiza mediante un mapper dedicado por cada par Entidad-DTO relacionado; no se permite ni mapeo manual disperso en los services ni un mapper único para todo el proyecto.
*Justificación: garantiza consistencia en el mapeo sin caer en un componente monolítico difícil de mantener.*

Art. 4 - Los recursos se identifican con URLs formadas por sustantivos, nunca verbos. Se usan los verbos HTTP GET, POST, PUT, PATCH y DELETE devolviendo el código de estado correspondiente. Las acciones que no sean operaciones CRUD se implementan como GET (si son de solo lectura) o POST (si producen un efecto). La API es stateless. No es obligatorio implementar HATEOAS.
*Justificación: fija un diseño REST consistente, compatible con la autenticación JWT (Art. 9).*

Art. 5 - Toda la API se versiona en la URL con el prefijo `/api/v{n}/`.
*Justificación: permite evolucionar la API sin romper a los clientes existentes.*

## Calidad

Art. 6 - Toda lógica de negocio nueva, en cualquier componente, debe incluir al menos un test.
*Justificación: evita regresiones no detectadas sin depender de una lista cerrada de componentes que siempre dejará huecos.*

Art. 7 - Todo DTO que reciba datos de entrada de un endpoint debe validarse mediante Bean Validation.
*Justificación: evita que datos inválidos lleguen al dominio de negocio.*

Art. 8 - Toda respuesta de error de la API debe incluir un campo `message` no vacío. En errores de validación, `message` puede ser un mensaje genérico; el detalle por campo se define en el plan.md correspondiente.
*Justificación: garantiza que ningún error se devuelva sin contexto mínimo, sin obligar a un formato detallado que corresponde decidir por feature.*

## Seguridad

Art. 9 - La API usa Spring Security con autenticación JWT. Cada endpoint es público o privado; los privados exigen un JWT válido, no expirado y con firma verificada en la cabecera `Authorization: Bearer`. La autorización distingue solo autenticado / no autenticado; no existen roles. La clasificación público/privado de cada endpoint concreto se documenta en el plan.md de la feature correspondiente.
*Justificación: fija un nivel de seguridad concreto y verificable, dejando constancia de dónde se documenta el detalle por endpoint.*

Art. 10 - Las contraseñas se almacenan siempre con un algoritmo de hashing seguro con salt (BCrypt o equivalente), nunca en texto plano ni con algoritmos reversibles.
*Justificación: protege las credenciales aunque la base de datos se vea comprometida.*

Art. 11 - Los logs nunca registran: contraseñas, ningún fragmento de un token JWT, ni estos datos personales de usuarios: nombre completo, email, teléfono, DNI/NIE, dirección postal.
*Justificación: evita exponer información sensible en ficheros de log, incluida la exposición parcial de tokens.*

Art. 15 - Toda clase anotada con `@Service` debe declararse como implementación de una interfaz dedicada cuyo nombre siga la convención `I{NombreDelServicio}`. Los `@Controller` inyectan la interfaz, nunca la clase concreta.
*Justificación: facilita el testeo con mocks, mantiene el bajo acoplamiento entre capas y permite sustituir implementaciones sin modificar los consumidores.*

## Enmiendas

Art. 12 - Esta constitución solo se modifica mediante propuesta del agente y aprobación explícita del usuario.
*Justificación: mantiene el control humano sobre las reglas fundamentales del proyecto.*

Art. 13 - Los artículos nuevos se incorporan en su sección temática correspondiente. El número de un artículo es fijo y nunca se reutiliza; la numeración dentro de una sección puede no ser consecutiva. Un artículo derogado se marca explícitamente como "Derogado" conservando su número, nunca se elimina del documento.
*Justificación: permite mantener la organización temática sin invalidar referencias a artículos existentes, y deja constancia de qué artículos dejaron de aplicar y cuáles fueron simplemente saltados por error.*

Art. 14 - `AGENTS.md` regula el contexto operativo (comandos, entorno, convenciones mecánicas); esta constitución regula los principios de arquitectura, calidad y seguridad no negociables. Ambos ámbitos no deben solaparse; si ocurriera un solapamiento no previsto, prevalece esta constitución.
*Justificación: la constitución es, por definición del proyecto, el documento contra el que se valida todo lo demás.*