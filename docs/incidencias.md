# Registro de Incidencias Técnicas — PharmaSoft (Sesiones 13 y 14)

Este documento registra al menos 6 incidencias reales encontradas durante las sesiones de optimización, integración y estabilización, detallando su causa raíz, severidad, solución aplicada y verificación.

---

### Resumen de Incidencias

| ID | Módulo | Severidad | Estado | Causa Raíz |
| :--- | :--- | :---: | :---: | :--- |
| **INC-01** | Backend - Caché Redis | **Alta** | **Cerrada** | DTOs de respuesta sin implementar `Serializable`. |
| **INC-02** | Base de Datos - Oracle | **Media** | **Cerrada** | Nombres de columnas con tildes (`fecha_modificación`). |
| **INC-03** | Despliegue - Nginx | **Alta** | **Cerrada** | Recarga de página (F5) devolvía error HTTP 404. |
| **INC-04** | Backend - Resiliencia | **Alta** | **Cerrada** | Peticiones bloqueadas por tiempo indefinido si Redis cae. |
| **INC-05** | Backend - Pruebas Unitarias | **Baja** | **Cerrada** | Incompatibilidad de inferencia de comodín `List<?>` en AssertJ. |
| **INC-06** | Backend - Consistencia Stock | **Crítica** | **Cerrada** | Ventas registradas no actualizaban el stock en la caché de productos. |

---

### Detalle de Incidencias

#### INC-01: Error 500 al guardar listado en Redis por falta de serialización
- **Fecha:** 05/11/2026
- **Módulo:** `CategoriaServiceImpl` / `ProductoServiceImpl`
- **Severidad:** Alta
- **Descripción:** Al consultar categorías con Redis activo, la aplicación lanzaba `SerializationException` al intentar serializar `CategoriaResponseDTO`.
- **Causa Raíz:** Los DTOs almacenados en Redis no implementaban `java.io.Serializable`.
- **Corrección:** Se agregó `implements Serializable` y `serialVersionUID = 1L` a `CategoriaResponseDTO` y `ProductoResponseDTO`.
- **Commit:** `29bc4d1` (*perf(cache): caché Redis de categorías y productos*)
- **Prueba asociada:** Verificación de clave `categorias::todas` almacenada en Redis CLI sin excepciones en logs.

---

#### INC-02: Inconsistencia de mapeo en columnas de fecha en Oracle
- **Fecha:** 05/11/2026
- **Módulo:** Entidades JPA (`Categoria`, `Cliente`)
- **Severidad:** Media
- **Descripción:** Las tablas `categorias` y `clientes` generaban columnas con caracteres acentuados (`fecha_modificación`).
- **Causa Raíz:** Anotación `@Column(name = "fecha_modificación")` con tilde en lugar de la convención estándar SQL `fecha_modificacion`.
- **Corrección:** Se estandarizó la anotación en ambas entidades a `@Column(name = "fecha_modificacion")`.
- **Commit:** `9acd7c8`
- **Prueba asociada:** Verificación de metadata de tablas en Oracle Database sin errores de codificación.

---

#### INC-03: Error 404 al recargar con F5 en rutas internas de Angular servidas por Nginx
- **Fecha:** 05/11/2026
- **Módulo:** Frontend - Despliegue Nginx
- **Severidad:** Alta
- **Descripción:** Al navegar a `http://localhost:8081/categorias` y presionar F5, Nginx devolvía 404 Not Found.
- **Causa Raíz:** Nginx buscaba un archivo físico `/categorias/index.html` en el sistema de archivos en lugar de delegar el enrutamiento a la SPA.
- **Corrección:** Se añadió la directiva `try_files $uri $uri/ /index.html;` en el bloque `location /` de `nginx.conf`.
- **Commit:** `e22e9c8` (*chore(deploy): Nginx con política de caché y docker-compose*)
- **Prueba asociada:** Prueba 6 del catálogo de producción: llamada HTTP a `/categorias` respondiendo `200 OK`.

---

#### INC-04: Latencia excesiva y degradación del backend cuando Redis se apaga
- **Fecha:** 05/11/2026
- **Módulo:** Backend - Configuración de Red
- **Severidad:** Alta
- **Descripción:** Al detener el contenedor de Redis, las peticiones HTTP demoraban más de 30 segundos antes de responder.
- **Causa Raíz:** El driver de Lettuce por defecto esperaba tiempos de conexión prolongados antes de lanzar timeout.
- **Corrección:** Se configuró `spring.data.redis.timeout: 500ms` y un `SimpleCacheErrorHandler` que captura la excepción y ejecuta la consulta sobre Oracle inmediatamente.
- **Commit:** `29bc4d1`
- **Prueba asociada:** Prueba 10: `docker compose stop redis` y consulta a `/api/v1/categorias` respondiendo en <100 ms con WARN en log.

---

#### INC-05: Fallo de compilación en prueba unitaria de JWT
- **Fecha:** 05/11/2026
- **Módulo:** Backend - Pruebas Unitarias (`JwtServiceTest`)
- **Severidad:** Baja
- **Descripción:** `mvnw test-compile` fallaba indicando que `claims.get("authorities", List.class)` no coincidía con el método `contains` de AssertJ.
- **Causa Raíz:** El compilador de Java 21 infería `capture#1 of ?`, bloqueando la llamada varargs de strings.
- **Corrección:** Se realizó el cast explícito a `List<String> authorities`.
- **Commit:** `9acd7c8`
- **Prueba asociada:** Ejecución exitosa de `JwtServiceTest` (5 de 5 pruebas aprobadas).

---

#### INC-06: Desincronización de inventario en caché tras registrar una venta
- **Fecha:** 05/11/2026
- **Módulo:** Backend - Servicios de Ventas y Productos
- **Severidad:** Crítica
- **Descripción:** Tras procesar una venta, el endpoint de productos continuaba mostrando el stock anterior no descontado.
- **Causa Raíz:** La caché de productos (`cacheNames = "productos"`) no se invalidaba al registrar transacciones de venta.
- **Corrección:** Se agregó `@CacheEvict(cacheNames = "productos", allEntries = true)` en el método `registrar` de `VentaServiceImpl`.
- **Commit:** `29bc4d1`
- **Prueba asociada:** Prueba 9: Registrar venta y verificar inmediatamente en `GET /api/v1/productos` el stock actualizado.

---

### Resumen Final
- **Incidencias Críticas:** 1 (Cerrada)
- **Incidencias Altas:** 3 (Cerradas)
- **Incidencias Medias / Bajas:** 2 (Cerradas)
- **Incidencias Abiertas:** **0** (Ninguna incidencia pendiente).
