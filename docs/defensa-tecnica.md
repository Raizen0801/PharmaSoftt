# Banco de Respuestas para la Defensa Técnica — PharmaSoft

Respuestas técnicas con fundamento y referencias directas al código fuente del proyecto para responder las preguntas del jurado evaluador.

---

### 1. ¿Por qué la paginación se hace en el servidor y no en la SPA? ¿Qué consultas ejecuta Oracle por cada página?
- **Fundamento:** Si la paginación se hiciera en la SPA, el backend tendría que enviar miles de registros por la red, saturando la memoria del navegador y la transferencia de datos. Al paginar en el servidor con Spring Data (`Pageable`), solo viaja el bloque solicitado (ej. 10 registros).
- **Consultas en Oracle:** Por cada página, Hibernate ejecuta exactamente dos sentencias:
  1. La consulta de datos con cláusulas `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY` (o subconsulta `ROWNUM`) para traer únicamente las 10 filas de la página actual.
  2. Una consulta de conteo: `SELECT COUNT(*) FROM ...` para saber el total de páginas y registros disponibles.
- **Archivo de código:** [`AuditoriaRepository.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/repository/AuditoriaRepository.java) y [`AuditoriaServiceImpl.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/service/impl/AuditoriaServiceImpl.java#L70-L85).

---

### 2. ¿Cómo garantizas que una venta no deja datos a medias y que su evento de auditoría no aparece si la venta falla?
- **Fundamento:** 
  1. El método está anotado con `@Transactional`. Si alguna regla falla (stock insuficiente, cliente inactivo o error de red), Spring ejecuta un *rollback* completo de toda la transacción (no se guarda la venta ni los detalles, y el stock de los productos no se descuenta).
  2. La auditoría utiliza un listener con `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. El evento en la bitácora **solo se procesa y persiste si la transacción hizo commit exitoso**. Si ocurre un rollback, el listener nunca se activa, impidiendo registros falsos en la bitácora.
- **Archivo de código:** [`VentaServiceImpl.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/service/impl/VentaServiceImpl.java#L55-L105) y [`AuditoriaServiceImpl.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/service/impl/AuditoriaServiceImpl.java#L33-L45).

---

### 3. ¿Qué ocurre, paso a paso, cuando el token vence a mitad de una venta?
- **Paso a paso:**
  1. El frontend envía la petición `POST /api/v1/ventas` incluyendo la cabecera `Authorization: Bearer <token_expirado>`.
  2. La petición llega al backend y es interceptada por `JwtAuthenticationFilter`.
  3. `JwtService.extraerUsername(token)` valida la firma y la fecha de expiración (`claims.getExpiration()`). Al detectar que venció, lanza `ExpiredJwtException`.
  4. El filtro no establece la autenticación en el `SecurityContext` y la petición es capturada por `RestAuthenticationEntryPoint`, que responde con código `HTTP 401 Unauthorized` y mensaje JSON estructurado.
  5. En el frontend, el interceptor HTTP de Angular captura el error `401`, limpia el almacenamiento de sesión (`sessionStorage.removeItem`) y redirige al usuario a la pantalla de `/login` con un aviso informativo.
- **Archivo de código:** [`JwtAuthenticationFilter.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/security/JwtAuthenticationFilter.java) y [`RestAuthenticationEntryPoint.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/security/RestAuthenticationEntryPoint.java).

---

### 4. ¿Por qué la caché de productos se invalida al registrar una venta? ¿Qué pasaría si no?
- **Fundamento:** Toda venta descuenta unidades del inventario de los productos comprados. Si no se invalidara la caché de productos en Redis, las siguientes consultas `GET /api/v1/productos` seguirían devolviendo los datos almacenados en memoria con el stock anterior (desactualizado). Esto ocasionaría inconsistencias graves, como sobrevender productos que ya no tienen existencias.
- **Mecanismo:** Se utiliza `@CacheEvict(cacheNames = "productos", allEntries = true)` en el método de registrar venta para purgar la caché y obligar a una recarga real desde Oracle en la próxima lectura.
- **Archivo de código:** [`VentaServiceImpl.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/service/impl/VentaServiceImpl.java#L52).

---

### 5. ¿Qué diferencia hay entre un log, una columna de auditoría y un evento de la bitácora?
- **Columna de auditoría:** Atributo dentro de la misma tabla de negocio (`creado_por`, `fecha_registro`). Solo muestra quién creó el registro o cuándo fue la última modificación.
- **Evento de la bitácora:** Fila histórica e inmutable en una tabla dedicada (`bitacora_auditoria`) que preserva la cronología de eventos de negocio significativos (acción, entidad, id, detalle) incluso si la entidad original se modifica o elimina.
- **Log:** Archivo de texto plano generado por la infraestructura de logging (`pharma-backend.log`) que sirve para diagnóstico técnico, depuración de errores y análisis de rendimiento, organizado por `[requestId]`.
- **Archivo de código:** [`Venta.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/entity/Venta.java#L38) y [`Auditoria.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/entity/Auditoria.java).

---

### 6. ¿Cómo decides si una prueba end-to-end es estable? ¿Qué hiciste con una que fallaba a veces?
- **Criterio de estabilidad:** Una prueba E2E es determinista y estable cuando pasa el 100% de las veces en múltiples corridas seguidas (ej. 3 repeticiones con `--repeat-each=3`) sin requerir reintentos automáticos ni pausas arbitrarias con `waitForTimeout()`.
- **Qué hicimos cuando una fallaba:** 
  1. No recurrimos a parches temporales ni retrasos ciegos (`sleep`).
  2. Abrimos la traza detallada con `npx playwright show-trace` para observar capturas DOM, tiempos y llamadas de red.
  3. Reemplazamos esperas de tiempo por selectores semánticos reactivos con `expect(locator).toBeVisible()` o `toBeEnabled()`, esperando condiciones reales del ciclo de vida de Angular.
- **Archivo de código:** `docs/incidencias.md` (Incidencia INC-05 y suite de Playwright).

---

### 7. ¿Por qué se utiliza `Propagation.REQUIRES_NEW` en el listener de auditoría?
- **Fundamento:** Al ejecutarse en la fase `AFTER_COMMIT`, la transacción principal de la venta o cliente ya terminó y se cerró. Para persistir el nuevo registro en la tabla `bitacora_auditoria`, Spring necesita abrir una **nueva transacción independiente** (`REQUIRES_NEW`). De lo contrario, fallaría indicando que no hay una transacción activa para ejecutar la inserción en base de datos.
- **Archivo de código:** [`AuditoriaServiceImpl.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/service/impl/AuditoriaServiceImpl.java#L33-L34).

---

### 8. ¿Cómo protege el backend el acceso no autorizado a los endpoints de auditoría?
- **Fundamento:** Se aplica defensa en profundidad:
  1. **A nivel de cadena de filtros:** En `SecurityConfig`, la ruta `/api/v1/auditoria/**` exige la autoridad `USUARIOS_ADMINISTRAR`.
  2. **A nivel de método / controlador:** En `AuditoriaController`, la clase está anotada con `@PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")`. Si un usuario con rol `VENDEDOR` envía su token válido, Spring Security intercepta la petición y responde con `HTTP 403 Forbidden` a través de `RestAccessDeniedHandler`.
- **Archivo de código:** [`SecurityConfig.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/config/SecurityConfig.java#L69) y [`AuditoriaController.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/controller/AuditoriaController.java#L17).

---

### 9. ¿Qué rol cumple Nginx como proxy inverso y por qué elimina la necesidad de CORS en producción?
- **Fundamento:** En desarrollo, la SPA corre en un puerto (ej. 4200) y el backend en otro (8080), lo que constituye orígenes cruzados (CORS). En producción, Nginx expone un único punto de entrada en el puerto 8081:
  - Las peticiones web (`/`, `/productos`, `/index.html`) las sirve localmente como archivos estáticos.
  - Las peticiones al API (`/api/`) las reenvía internamente al backend (`proxy_pass http://backend:8080`).
  Como tanto la página como la API comparten el mismo protocolo, dominio y puerto desde la perspectiva del navegador, no existen orígenes cruzados y no se requiere configurar cabeceras CORS en el cliente.
- **Archivo de código:** `deploy/nginx.conf` en el frontend.

---

### 10. ¿Cómo se diseñó el sistema para que siga funcionando si Redis se apaga intempestivamente?
- **Fundamento:** Se implementó el patrón de **degradación elegante (Graceful Degradation)** mediante un `SimpleCacheErrorHandler` personalizado en `CacheConfig`.
- **Comportamiento:** Si Redis se desconecta, se apaga o sufre problemas de red, el manejador captura las excepciones (`handleCacheGetError`, `handleCachePutError`), emite una advertencia en los logs (`WARN Cache no disponible...`) y permite que la petición continúe con normalidad consultando directamente a la base de datos Oracle, garantizando alta disponibilidad sin caída del servicio.
- **Archivo de código:** [`CacheConfig.java`](file:///c:/Users/diego/OneDrive/Documentos/PharmaBackend/src/main/java/pe/edu/upeu/PharmaBackend/config/CacheConfig.java#L21-L55).
