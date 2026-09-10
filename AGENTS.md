# AGENTS.md

## Resumen del proyecto
API REST desarrollada con Spring Boot 4 + Java 21 y BD MySQL.
Para la gestión y administración de las reservas y usuarios de una casa rural.

## Cómo construir y ejecutar
- Instalar dependencias: `mvn clean install`
- Levantar en local: `mvn spring-boot:run`
- Tests: `mvn test`
- Variables de entorno necesarias: [pendiente] 

## Convenciones de código
- Estilo de nombrado: camelCase para variables y métodos, PascalCase para clases, UPPER_SNAKE_CASE para constantes
- Estructura de carpetas: por capas — `controller/`, `service/`, `repository/`, `model/`, `dto/`, `config/`, `security/`

## Zonas prohibidas o sensibles
- No modificar `pom.xml` sin autorización explícita del usuario (sí se puede revisar y proponer cambios)
- `application.properties` contiene credenciales de BD en texto plano (usuario `root`, sin contraseña) — pendiente de migrar a variables de entorno

## Reglas
- Lee /docs/constitution.md y la spec activa en /spec antes de tocar código.
- No modifiques archivos dentro de /spec salvo petición explícita.
