# Guion de Demostración Técnica — PharmaSoft (10 Minutos)

Estructura de la sustentación técnica para la evaluación de la Unidad III.

---

### Bloque 1 (0:00 – 1:00) | Contexto y Arquitectura
- **Objetivo:** Explicar el problema de negocio de la botica y la arquitectura de solución.
- **Acciones:**
  1. Mostrar la lámina de arquitectura: Frontend Angular 22 servido por Nginx 1.27, Backend Spring Boot 4 stateless con JWT, base de datos Oracle XE en Docker y caché en memoria con Redis 7.
  2. Describir los perfiles: `dev` para desarrollo local y `prod` con configuración securizada, logging rotativo y variables de entorno.

---

### Bloque 2 (1:00 – 4:30) | Flujo Principal de Negocio
- **Objetivo:** Demostrar el recorrido transaccional completo.
- **Acciones:**
  1. Iniciar sesión como `vendedor`.
  2. Ir al módulo de Clientes y seleccionar un cliente activo.
  3. Ir al módulo de Productos y verificar el stock actual del producto de prueba (ej. Stock = 50 unidades).
  4. Realizar una venta de 2 unidades y hacer clic en **Registrar Venta**.
  5. Mostrar el comprobante generado con el subtotal, total y el usuario creador (`creado_por = vendedor`).
  6. Regresar al listado de Productos y comprobar que el stock se descontó de inmediato (Stock = 48 unidades), demostrando que la caché se invalidó automáticamente con `@CacheEvict`.

---

### Bloque 3 (4:30 – 6:00) | Seguridad y Control de Acceso
- **Objetivo:** Probar el sistema de autorización basado en roles y permisos.
- **Acciones:**
  1. Demostrar que el menú del vendedor no muestra las opciones administrativas (Auditoría, Usuarios).
  2. Intentar ingresar manualmente por URL a `/auditoria` o `/usuarios`: el `permisoGuard` de Angular bloquea la navegación y redirige.
  3. Ejecutar una petición en Postman o cURL al endpoint de auditoría con el token del vendedor, mostrando la respuesta `HTTP 403 Forbidden`.
  4. Mostrar la respuesta `HTTP 401 Unauthorized` cuando el token expira o es manipulado.

---

### Bloque 4 (6:00 – 7:30) | Volumen, Rendimiento y Caché Redis
- **Objetivo:** Evidenciar la optimización de carga y reducción de consultas a Oracle.
- **Acciones:**
  1. Abrir DevTools (F12) en la pestaña Red.
  2. Llamar al listado de Categorías: en la 1.ª llamada se aprecia el log de Hibernate en consola ejecutando la consulta en Oracle (~290 ms).
  3. Llamar por 2.ª vez: la respuesta es casi instantánea (~28 ms) sin ejecutar consultas SQL ni logs de acceso a base de datos.
  4. Ejecutar en terminal `docker exec deploy-redis-1 redis-cli KEYS "*"` y mostrar la clave `categorias::todas` con su TTL de 10 minutos.
  5. Consultar el endpoint de métricas de Spring Boot Actuator (`/actuator/metrics/cache.gets?tag=name:categorias&tag=result:hit`) mostrando el acierto de caché registrado.

---

### Bloque 5 (7:30 – 8:30) | Auditoría y Trazabilidad de Punta a Punta
- **Objetivo:** Demostrar el rastreo inmutable de operaciones críticas.
- **Acciones:**
  1. Cerrar sesión e ingresar con el usuario `admin`.
  2. Entrar a la pantalla `/auditoria`.
  3. Mostrar la fila de la venta registrada en el Bloque 2: fecha, usuario `vendedor`, acción `VENTA_REGISTRADA`, entidad `Venta` y detalle.
  4. Abrir la terminal y mostrar en el log del backend cómo la petición HTTP de la venta comparte el mismo `[requestId]` con la cabecera `X-Request-Id` y el registro de la bitácora.
  5. Explicar brevemente al jurado que el registro se realizó post-commit con `@TransactionalEventListener`, impidiendo registros si la venta hubiera fallado.

---

### Bloque 6 (8:30 – 10:00) | Calidad de Software e Incidencias Cerradas
- **Objetivo:** Presentar las pruebas automatizadas y la estabilidad del proyecto.
- **Acciones:**
  1. Ejecutar en vivo la suite de Playwright: `npx playwright test`.
  2. Mostrar cómo las 14 pruebas E2E recorren la aplicación en segundos y pasan en verde al 100%.
  3. Mostrar el reporte `docs/incidencias.md` demostrando que las 6 incidencias críticas/altas encontradas durante el desarrollo fueron resueltas y cerradas.
