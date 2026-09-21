# Spec 003 — Núcleo de Negocio (Reservas, Usuarios, Mensajes, Auditoría, Configuración)

### Contexto y objetivo

Implementa la lógica de negocio central del sistema: consulta pública de disponibilidad, envío de mensajes de contacto, y gestión administrativa completa de reservas, usuarios, auditoría y configuración. Se apoya en el modelo de datos ya implementado (spec 001) y en la autenticación ya implementada (spec 002). Cubre tanto la parte pública de la web (sin autenticación) como el panel de administración (endpoints privados).

### Usuarios / actores

- **Visitante/Usuario público**: cualquier persona sin autenticar que consulta disponibilidad o envía un mensaje de contacto. No inicia sesión en ningún momento.
- **Administrador**: actor autenticado (JWT, spec 002) con acceso completo a la gestión de reservas, usuarios, mensajes recibidos, auditoría y configuración.

### Historias de usuario

- H1: Como visitante, quiero consultar qué fechas están ocupadas, para saber cuándo puedo reservar la casa.
- H2: Como visitante, quiero enviar un mensaje de contacto, para hacer una consulta o pedir información.
- H3: Como administrador, quiero crear reservas asociándolas a un usuario existente o a uno nuevo, para gestionar las estancias.
- H4: Como administrador, quiero modificar los datos de una reserva existente, para corregir errores o reflejar cambios acordados con el cliente.
- H5: Como administrador, quiero cambiar el estado de una reserva, para reflejar su confirmación, cancelación o vuelta a pendiente.
- H6: Como administrador, quiero ocultar o volver a mostrar una reserva, para mantener limpio el listado sin perder el histórico.
- H7: Como administrador, quiero crear y modificar usuarios manualmente, para mantener actualizada la base de contactos.
- H8: Como administrador, quiero ocultar o volver a mostrar un usuario, para gestionar su visibilidad sin borrar sus datos.
- H9: Como administrador, quiero consultar el histórico de auditoría con filtros, para trazabilidad de las acciones realizadas.
- H10: Como administrador, quiero modificar la configuración de precio y duración de estancias, para ajustar las condiciones bajo las que se crean nuevas reservas.
- H11: Como administrador, quiero listar reservas, usuarios y mensajes con filtros y paginación, para gestionar grandes volúmenes de datos sin cargarlos todos de golpe.

### Requisitos funcionales (criterios de aceptación en EARS)

#### Consulta pública de disponibilidad

- RF-1: CUANDO un visitante solicita las fechas ocupadas, EL SISTEMA devuelve únicamente `fecha_entrada` y `fecha_salida` de las reservas en estado PENDIENTE o CONFIRMADA cuya `fecha_salida` sea igual o posterior a la fecha actual, sin incluir ningún otro dato (precio, usuario, estado, numero_huespedes, visible, reservas pasadas). La condición `visible` es irrelevante para esta consulta: toda reserva ocultable (RF-19) es CANCELADA o pasada, por lo que ya queda excluida por los criterios de estado y fecha. `fecha actual` se interpreta en la zona horaria local del servidor.
- RF-2: EL SISTEMA nunca incluye reservas en estado CANCELADA en la respuesta de disponibilidad pública, independientemente de sus fechas.

#### Mensajes de contacto (público)

- RF-3: EL SISTEMA valida que correo y mensaje no estén vacíos, y que correo tenga formato de email válido, antes de procesar el envío (Art. 7 constitución). Nombre, apellido, telefono y asunto son opcionales. SI el visitante no aporta asunto, EL SISTEMA lo persiste con el valor genérico `Consulta` (el campo es obligatorio en el modelo, spec 001).
- RF-4: CUANDO un visitante envía un mensaje con correo ya registrado en un Usuario existente, EL SISTEMA asocia el mensaje a ese Usuario sin modificar ninguno de sus datos, incluso si el Usuario tiene `visible=false`. La coincidencia del correo se determina de forma case-insensitive, coherente con la unicidad de RF-23.
- RF-5: CUANDO un visitante envía un mensaje con un correo que no corresponde a ningún Usuario existente, EL SISTEMA crea un Usuario nuevo con los datos aportados (nombre, apellido, telefono si se proporcionan) antes de asociar el mensaje. SI el visitante no aporta nombre o apellido, EL SISTEMA los persiste con los valores genéricos `Sin nombre` y `Sin apellido` respectivamente (ambos campos son obligatorios en el modelo, spec 001). La búsqueda de Usuario existente se realiza de forma case-insensitive (RF-4).
- RF-6: EL SISTEMA guarda todo mensaje enviado por un visitante con `remitente=USUARIO` y `fecha_mensaje` igual al momento de creación.
- RF-7: EL SISTEMA no expone ningún endpoint para modificar ni eliminar un mensaje una vez creado.

#### Gestión de reservas (administrador)

- RF-8: EL SISTEMA permite al administrador crear una reserva asociándola a un Usuario existente mediante su id, a un Usuario nuevo cuyos datos se aportan en la misma petición de creación, o sin asociar a ningún Usuario. CUANDO se aportan datos de un Usuario nuevo, EL SISTEMA aplica la unicidad de correo (RF-23, case-insensitive) y registra una entrada de Auditoria de tipo CREAR para el Usuario además de la de la Reserva (RF-30).
- RF-9: CUANDO se crea una reserva, EL SISTEMA calcula su precio como `precio_noche` (valor vigente en Configuración) × número de noches entre `fecha_entrada` y `fecha_salida`.
- RF-10: EL SISTEMA rechaza la creación o modificación de una reserva si `fecha_entrada` no es estrictamente anterior a `fecha_salida`.
- RF-11: EL SISTEMA rechaza la creación de una reserva si la duración en noches no está entre `estancia_minima` y `estancia_maxima` (valores vigentes en Configuración). En una modificación, EL SISTEMA valida la duración únicamente si la nueva duración difiere de la actual; si se conserva la misma duración (aunque sea no conforme con la Configuración vigente por un cambio posterior, RF-37), no se rechaza por este motivo.
- RF-12: EL SISTEMA rechaza la creación o modificación de una reserva si `numero_huespedes` no está entre 1 y 10. El tope de 10 es una decisión de producto (la casa se alquila completa) y se trata como constante de negocio.
- RF-13: EL SISTEMA rechaza la creación o modificación de una reserva si sus fechas solapan con otra reserva existente en estado PENDIENTE o CONFIRMADA (al modificar, se excluye la propia reserva de la comprobación). La comprobación aplica a reservas en cualquier estado, incluida CANCELADA. El solapamiento incluye el caso en que la `fecha_entrada` de una coincida con la `fecha_salida` de la otra (no puede entrar una reserva el día que sale otra, spec 001).
- RF-14: EL SISTEMA crea toda reserva nueva con `visible=true`. El estado inicial por defecto es CONFIRMADA; el administrador puede cambiar el estado en la misma petición de creación a cualquiera de los tres estados (PENDIENTE, CONFIRMADA, CANCELADA).

- RF-15: EL SISTEMA permite modificar `fecha_entrada`, `fecha_salida`, `numero_huespedes` y usuario de una reserva existente mediante un endpoint de modificación. Para el usuario permite reutilizar un `usuario_id` existente, crear un Usuario nuevo inline (con las reglas de RF-8) o anular la asociación (`usuario=null`). Los campos `estado` y `visible` se gestionan en endpoints dedicados (RF-17, RF-19, RF-20).
- RF-16: CUANDO se modifican `fecha_entrada` o `fecha_salida` de una reserva existente, EL SISTEMA recalcula su precio usando el `precio_noche` vigente en el momento de la modificación (no el precio original).
- RF-17: EL SISTEMA permite cambiar el estado de una reserva, mediante un endpoint dedicado, entre PENDIENTE, CONFIRMADA y CANCELADA, en cualquier dirección.
- RF-18: SI se solicita cambiar el estado de una reserva CANCELADA a PENDIENTE o CONFIRMADA y sus fechas solapan con otra reserva existente en estado PENDIENTE o CONFIRMADA, ENTONCES EL SISTEMA rechaza el cambio de estado.
- RF-19: EL SISTEMA permite ocultar una reserva (`visible=false`), mediante un endpoint dedicado, únicamente si su estado es CANCELADA o su `fecha_salida` ya ha pasado. En cualquier otro caso, rechaza la operación.
- RF-20: EL SISTEMA permite volver a mostrar (`visible=true`) una reserva oculta sin ninguna restricción adicional.
- RF-21: EL SISTEMA lista las reservas visibles de forma paginada, con tamaño de página configurable (por defecto 20), permitiendo filtrar por estado, visible, `usuario_id` y rango de fechas (el rango se interpreta sobre `fecha_entrada`).

#### Gestión de usuarios (administrador)

- RF-22: EL SISTEMA permite al administrador crear un Usuario manualmente indicando nombre, apellido, correo y telefono.
- RF-23: EL SISTEMA rechaza la creación de un Usuario si el correo coincide con el de un Usuario ya existente.
- RF-24: EL SISTEMA permite al administrador modificar todos los campos de un Usuario existente (nombre, apellido, correo, telefono).
- RF-25: EL SISTEMA rechaza la modificación de un Usuario si el nuevo correo coincide con el de otro Usuario ya existente.
- RF-26: EL SISTEMA permite ocultar (`visible=false`) y volver a mostrar (`visible=true`) un Usuario mediante un endpoint dedicado, sin restricciones adicionales.
- RF-27: CUANDO se oculta o se vuelve a mostrar un Usuario, EL SISTEMA no modifica la visibilidad de sus reservas ni de sus mensajes asociados.
- RF-28: EL SISTEMA lista los usuarios visibles de forma paginada, con tamaño de página configurable (por defecto 20), permitiendo filtrar por nombre, apellido, correo y visible.
- RF-29: EL SISTEMA lista los mensajes de forma paginada, con tamaño de página configurable (por defecto 20), permitiendo filtrar por `usuario_id` y rango de fechas (el rango se interpreta sobre `fecha_mensaje`).

#### Auditoría (administrador)

- RF-30: CUANDO el administrador crea, modifica, oculta/muestra o cambia el estado de una Reserva, un Usuario o la Configuración, EL SISTEMA registra automáticamente una entrada de Auditoria con el tipo de acción correspondiente, sin intervención manual del administrador.
- RF-31: CUANDO se crea un Usuario automáticamente al recibir un mensaje de un visitante (sin intervención del administrador), EL SISTEMA registra una entrada de Auditoria de tipo CREAR con `admin=null`.
- RF-32: EL SISTEMA no expone ningún endpoint para crear, modificar ni eliminar entradas de auditoría manualmente.
- RF-33: EL SISTEMA lista las entradas de auditoría de forma paginada, con tamaño de página configurable (por defecto 20), permitiendo filtrar por `entidad_afectada`, `entidad_id`, `tipo_accion` y rango de fechas.

#### Configuración (administrador)

- RF-34: EL SISTEMA permite al administrador consultar la Configuración vigente, devuelta mediante un DTO dedicado y su mapper (Art. 2 y 3 constitución).
- RF-35: EL SISTEMA permite al administrador modificar `precio_noche`, `estancia_minima` y `estancia_maxima` mediante un DTO de entrada dedicado y su mapper (Art. 2 y 3 constitución).
- RF-36: EL SISTEMA rechaza la modificación de Configuración si `estancia_minima` es mayor que `estancia_maxima`, o si incumple los límites positivos: `estancia_minima` ≥ 1, `estancia_maxima` ≥ 1 y `precio_noche` > 0.
- RF-37: EL SISTEMA permite modificar la Configuración aunque existan reservas PENDIENTE o CONFIRMADA cuya duración ya no cumpla los nuevos límites de estancia; estas reservas no se recalculan ni se ven afectadas retroactivamente.

#### Validación general

- RF-38: EL SISTEMA valida mediante Bean Validation todos los DTOs de entrada de los endpoints descritos en esta spec, incluyendo formato de correo electrónico donde aplique (Art. 7 constitución).

### Requisitos no funcionales

- **Paginación:** todo listado (reservas, usuarios, mensajes, auditoría) es paginado, con tamaño de página configurable por el cliente y valor por defecto de 20 elementos y máximo de 100.
- **Seguridad:** todos los endpoints de esta spec salvo consulta de disponibilidad (RF-1) y envío de mensaje (RF-3 a RF-7) son privados y exigen JWT válido (Art. 9 constitución, spec 002).
- **Privacidad:** la respuesta pública de disponibilidad (RF-1) nunca expone datos personales ni económicos.
- **Privacidad:** la descripción (`descripcion`) de una entrada de Auditoria nunca contiene datos personales del usuario ni de la reserva; referencia el dato mediante `entidad_id` (spec 001, Art. 11 constitución).
- **Idioma:** los mensajes de error se devuelven en español, siguiendo el formato de error fijado en Art. 8 de la constitución.

### Casos límite

- Envío de mensaje con correo de un Usuario oculto (`visible=false`): se acepta y se asocia con normalidad; el Usuario permanece oculto.
- Dos peticiones simultáneas de creación/reactivación de reserva para las mismas fechas: solo una debe prosperar, la otra debe ser rechazada por solapamiento.
- Modificación de fechas de una reserva que, tras el cambio, solapa con otra reserva PENDIENTE o CONFIRMADA distinta: se rechaza.
- Modificación de fechas de una reserva que la haga solapar consigo misma (sin cambio real de fechas ocupadas): debe permitirse, no debe autorrechazarse.
- Cambio de estado de una reserva sin modificar sus fechas (ej. CONFIRMADA → PENDIENTE): no debe disparar recálculo de precio.
- Intento de ocultar una reserva CONFIRMADA con fecha_salida futura: rechazado (RF-19).
- Intento de crear un Usuario o modificarlo con un correo ya existente pero con distinta combinación de mayúsculas/minúsculas. La unicidad de los correos electrónicos es **case-insensitive**.
- Filtro de listado con página fuera de rango (ej. página 50 de un total de 2): debe devolver una lista vacía, no error.
- Cambio de Configuración con `estancia_minima = estancia_maxima` (rango de una sola noche válida): debe permitirse.
- `numero_huespedes` en los límites exactos (1 y 10): deben aceptarse.
- Creación de reservas con fechas en el pasado (reservas correctivas/retroactivas): permitida; solo aplican las validaciones de RF-10 a RF-13.
- Creación de una reserva directamente en estado CANCELADA: permitida (RF-14).
- Modificación de fechas de una reserva conservando la misma duración en noches, aunque esta sea no conforme con la Configuración vigente (RF-37): no se rechaza por RF-11.
- Creación/modificación de una reserva con datos de un Usuario nuevo cuyo correo ya existe (en cualquier combinación de mayúsculas/minúsculas): se rechaza por unicidad (RF-23, case-insensitive).
- Envío de mensaje sin asunto: el mensaje se guarda con el asunto genérico `Consulta` (RF-3).

### Fuera de alcance

- Respuesta del administrador a mensajes de usuarios (quedará en una spec futura, junto con el envío por email).
- Envío de correos electrónicos de cualquier tipo.
- Eliminación física de cualquier entidad (Reserva, Usuario, Mensaje, Auditoria, Configuración); solo existe ocultar/mostrar donde aplica.
- Gestión de habitaciones individuales y pagos (fuera de alcance también en spec 001).
- Registro de nuevos administradores y roles diferenciados (spec 002).
- Refresh token o renovación de sesión (spec 002).

### Criterios de finalización

- Todos los requisitos funcionales (RF-1 a RF-38) tienen al menos un test en verde (Art. 6 constitución).
- Demo manual: un visitante consulta disponibilidad y ve solo fechas futuras ocupadas sin datos privados; un visitante envía un mensaje y se crea/reutiliza el Usuario correctamente; un administrador crea, modifica, cambia estado y oculta/muestra una reserva respetando las reglas de solapamiento y estancia; un administrador crea y modifica usuarios; un administrador consulta auditoría y configuración con filtros y paginación funcionando en los cuatro listados (reservas, usuarios, mensajes, auditoría).

### Dudas abiertas

- [NECESITA ACLARACIÓN] Orden por defecto de cada listado paginado (ej. reservas por `fecha_entrada`, usuarios por id, mensajes por `fecha_mensaje`, auditoría por fecha) — ¿algún criterio concreto o lo decidimos libremente en `plan.md`?