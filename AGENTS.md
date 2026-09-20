# AGENTS.md

## Resumen del proyecto
API REST desarrollada con Spring Boot 4 + Java 21 y BD MySQL.
Para la gestión y administración de las reservas y usuarios de una casa rural.

## Cómo construir y ejecutar
- Requiere: JDK 21
- Instalar dependencias: `./mvnw clean install`
- Levantar en local: `./mvnw spring-boot:run`
- Tests: `./mvnw test` (unitarios e integración se ejecutan juntos, sin comandos separados)

## Variables de entorno
- `DB_URL` (default: `jdbc:mysql://localhost:3306/elpajaroverde`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default: vacío)
- `APP_BASE_URL` (default: `http://localhost:8080`)
- Todas tienen valor por defecto para desarrollo local; no es obligatorio definirlas a mano salvo que quieras sobreescribirlas.
- Nota: aún no se ha decidido dónde se declararán los valores reales (sistema, IDE o `.env`); por ahora el proyecto corre con los defaults.

## Convenciones de código
- Estilo de nombrado: camelCase para variables y métodos, PascalCase para clases, UPPER_SNAKE_CASE para constantes
- Estructura de carpetas: por capas — `controllers/`, `services/`, `repositories/`, `models/`, `dtos/`, `config/`, `security/`, `mappers/`

## Zonas prohibidas o sensibles
- No modificar `pom.xml` ni `application.properties`  sin autorización explícita del usuario (sí se puede revisar y proponer cambios)

## Reglas
- Lee /docs/constitution.md y la spec activa en /spec antes de tocar código.
- No modifiques archivos dentro de /spec salvo petición explícita.
