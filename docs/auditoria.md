# Documento de Auditoría y Trazabilidad — PharmaSoft

## 1. ¿Qué se audita y por qué?
En un sistema transaccional farmacéutico y comercial, la auditoría garantiza:
1. **No repudio e integridad:** Saber exactamente qué usuario ejecutó una operación crítica de negocio.
2. **Control financiero y de inventario:** Toda salida o alteración de stock (ventas) debe tener registro inmutable.
3. **Cumplimiento normativo y seguridad:** Las altas de clientes, creación de productos y borrado de catálogos quedan registrados con fecha, hora e identificador de petición.

---

## 2. Tabla de Eventos de Auditoría

| Acción | Disparador (Cuándo se registra) | Entidad Afectada | Información guardada en el Detalle |
| :--- | :--- | :--- | :--- |
| `VENTA_REGISTRADA` | Al confirmar con éxito la venta y descontar stock. | `Venta` | Total de la venta (S/), Nombre completo del cliente, cantidad de ítems. |
| `CLIENTE_REGISTRADO` | Al registrar un nuevo cliente en el sistema. | `Cliente` | DNI del cliente, nombres y apellidos completos. |
| `CATEGORIA_ELIMINADA` | Al eliminar una categoría sin productos asociados. | `Categoria` | Nombre de la categoría eliminada. |
| `PRODUCTO_CREADO` | Al crear un nuevo producto en el catálogo. | `Producto` | Nombre del producto, precio unitario y stock inicial. |

---

## 3. Diferencia entre Columnas de Auditoría, Bitácora y Logs

| Nivel | Mecanismo | Propósito | Ejemplo en PharmaSoft |
| :--- | :--- | :--- | :--- |
| **Columnas de auditoría** | Campos directos en la tabla de la entidad (`creado_por`, `fecha_creacion`, `fecha_modificacion`). | Rastrear el estado actual del registro (quién lo creó y cuándo cambió por última vez). | Columna `creado_por` y `fecha_registro` en tabla `ventas`. |
| **Bitácora de auditoría** | Tabla histórica independiente (`bitacora_auditoria`) con inserción post-commit. | Mantener un historial cronológico e inmutable de operaciones de negocio. | Fila con evento `VENTA_REGISTRADA`, usuario `vendedor`, ID venta `101`. |
| **Logs de aplicación** | Archivos de texto rotativos (`pharma-backend.log`) gestionados por Logback/SLF4J con `MDC`. | Diagnóstico técnico, trazabilidad de excepciones y monitoreo de peticiones HTTP. | `INFO [a1b2c3d4] RequestIdFilter - POST /api/v1/ventas -> 201 (45 ms)`. |

---

## 4. ¿Por qué se registra después del Commit?
Se utiliza `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` con `Propagation.REQUIRES_NEW`. 
- **Razón técnica:** Si la transacción de negocio principal hace *rollback* (por ejemplo, por stock insuficiente, cliente inactivo o fallo de base de datos), la auditoría **NO se persiste**, evitando registrar eventos fantasmas de acciones que nunca ocurrieron.
- **¿Por qué el login fallido no entra a la bitácora post-commit?** El intento de login no es una transacción de entidad persistida en base de datos; además, si un usuario ingresa una contraseña incorrecta, registrarla o bloquear la base de datos con transacciones de auditoría por cada ataque de fuerza bruta degradaría el rendimiento. Los intentos de login fallidos se registran en los **logs de seguridad** (`WARN Bad credentials`) con su respectivo `requestId` e IP.

---

## 5. ¿Qué datos NUNCA se deben guardar en Auditoría ni en Logs?
1. **Contraseñas en texto plano o hashes:** Nunca guardar contraseñas de usuarios.
2. **Tokens de autenticación:** Encabezados `Authorization: Bearer <JWT>` completos (evita secuestro de sesión si se filtran los logs).
3. **Datos financieros sensibles:** Números completos de tarjetas de crédito/débito, códigos CVV o PINs.
4. **Información médica privada no anonimizada.**

---

## 6. Caso de Trazabilidad de Punta a Punta

Cuando se procesa una venta:
1. **Log de acceso:** Genera un identificador único en MDC: `requestId = "55014a7b"`.
2. **Entidad `Venta`:** Se almacena en la tabla `ventas` con columna `creado_por = "vendedor"`.
3. **Bitácora de auditoría:** Se inserta en `bitacora_auditoria` el evento `VENTA_REGISTRADA` con `usuario = "vendedor"` y `request_id = "55014a7b"`.
4. **Trazabilidad:** Cualquier auditor puede tomar el `X-Request-Id` de la cabecera HTTP de la venta y encontrar exactamente las líneas en el archivo de log del servidor y la fila correspondiente en la bitácora de base de datos.
