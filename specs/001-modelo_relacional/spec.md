# Spec: Modelo de Datos - Dominio Casa Rural

## Resumen
Modelo de datos fundacional del sistema de gestión de la casa rural (una única propiedad de 8 habitaciones, alquilada completa). Define las entidades, relaciones y reglas de negocio de las que dependerán las futuras features. No cubre habitaciones individuales ni pagos, al alquilarse la casa entera y gestionarse el pago fuera del sistema.

## Entidades

### Usuario
Persona que consulta disponibilidad, contacta con la casa rural, o tiene reservas asociadas. Se crea automáticamente al enviar un mensaje, o manualmente por el administrador.
- nombre
- apellido
- correo (unico por usuario, evitar duplicados)
- telefono
- visible (indica si el usuario está oculto)

### Reserva
Estancia de un usuario (o gestionada directamente por el admin) en la casa rural. Se crea siempre a mano por el administrador.
- fecha_reserva (fecha en que se registró la reserva)
- fecha_entrada
- fecha_salida
- numero_huespedes
- precio (precio total de la estancia, fijado en el momento de creación = precio_noche × noches de estancia; no se recalcula si la Configuración cambia después)
- estado (pendiente | confirmada | cancelada) Por defecto se crea en confirmada.    
- visible (indica si la reserva está oculta; solo puede ocultarse si su estado es "cancelada" o si su fecha_salida ya ha pasado; una reserva oculta puede volver a mostrarse)
- usuario (relación N:1 con Usuario; puede ser nulo si el admin crea la reserva sin asociarla a un usuario)

### Administrador
Persona con acceso al panel de gestión. Sin roles diferenciados. En principio solo existe un administrador; la relación con Mensaje se resuelve mediante el campo remitente.
- nombre_usuario (login, unico)
- contraseña (hash)
- correo (unico por admin)

### Mensaje
Comunicación entre un usuario y el administrador, agrupada por usuario (no por hilos de pregunta-respuesta individuales). Los mensajes son inmutables: una vez creados, no se borran ni modifican, solo se crean y leen.
- fecha_mensaje
- asunto
- mensaje
- remitente (usuario | admin)
- usuario (relación N:1 con Usuario; identifica la conversación a la que pertenece)

### Configuración
Registro único con los valores vigentes que rigen nuevas reservas. No mantiene histórico; al cambiar un valor, se sobrescribe.
- precio_noche (precio definitivo por noche; se usa para calcular el precio total de cada reserva)
- estancia_minima (en noches; 2 noches = 3 días)
- estancia_maxima (en noches; 2 noches = 3 días)

### Auditoria
Registro de acciones críticas realizadas por el administrador, con fines de trazabilidad interna. Es una entidad de negocio, distinta de los logs de aplicación/servidor. La validación de que tipo_accion sea válido según entidad_afectada se realiza en la capa de servicio (ej: cambiar_estado solo es válido para Reserva; Configuración solo admite modificar).
- tipo_accion (enum: crear, modificar, eliminar/ocultar, cambiar_estado)
- fecha
- admin (relación N:1 con Administrador, puede ser nulo si se ha creado un usuario automaticamente)
- entidad_afectada (limitado a: Reserva, Usuario, Configuración; Mensaje no se audita porque es inmutable)
- entidad_id
- descripcion (breve, sin datos personales — se referencia el dato mediante entidad_id, no se duplica en texto)

## Relaciones
- Usuario 1:N Reserva (una reserva tiene como máximo un usuario asociado; un usuario puede tener varias reservas)
- Usuario 1:N Mensaje
- Administrador 1:N Auditoria

## Reglas de negocio
- La fecha de entrada de una reserva siempre debe ser anterior a la fecha de salida.
- No pueden existir dos reservas que se solapen en fechas, contando tanto las reservas en estado "pendiente" como las "confirmadas". Las "canceladas" no bloquean fechas. No puede entrar una reserva el día que sale otra. Si se desea reactivar una reserva en fechas que ocupa una cancelada, esta debe cancelarse (o ya estar cancelada) previamente.
- La duración de la estancia (calculada en noches) debe estar entre la estancia_minima y la estancia_maxima de la Configuración.
- El Admin solo responde mensajes, no puede iniciar una conversación con un Usuario que nunca ha escrito antes. Sin embargo, si el usuario ha contactado por otro canal (teléfono, etc.), el admin puede crear el usuario y el primer mensaje de la conversación.

## Fuera de alcance
- Gestión de habitaciones individuales (la casa se alquila completa, no por habitaciones)
- Pagos
- Envío de mensajes por email
- Registro de nuevos administradores (se insertan manualmente en la base de datos)
- Roles o permisos diferenciados entre administradores

## Preguntas abiertas / a clarificar
Ninguna pendiente.