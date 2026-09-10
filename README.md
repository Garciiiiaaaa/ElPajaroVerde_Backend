# El Pájaro Verde

API REST para la gestión y administración de reservas y usuarios de una casa rural.

## Stack

- Java 21
- Spring Boot 4
- MySQL
- Maven

## Requisitos previos

- JDK 21
- Maven
- MySQL corriendo en local con una base de datos `elpajaroverde` creada

## Configuración

Antes de arrancar, revisa `src/main/resources/application.properties` y ajusta la conexión a tu base de datos local (usuario, contraseña, puerto, etc.).

## Cómo ejecutar

```bash
mvn spring-boot:run
```

La API quedará disponible en `http://localhost:8080`.

## Cómo testear

```bash
mvn test
```

## Estructura del proyecto

```
src/main/java/.../
├── controller/
├── service/
├── repository/
├── model/
├── dto/
├── config/
└── security/
```

## Estado del proyecto

En desarrollo.
