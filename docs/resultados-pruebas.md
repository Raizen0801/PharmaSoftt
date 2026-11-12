# Resultados de Pruebas Automatizadas — PharmaSoft

## 1. Pruebas End-to-End con Playwright
- **Comando de ejecución:** `npx playwright test --repeat-each=3 --reporter=list,html`
- **Total de pruebas en la suite:** 14 pruebas automatizadas
- **Total de ejecuciones (3 corridas seguidas):** 42 ejecuciones
- **Aprobadas:** 42 (100%)
- **Fallidas:** 0
- **Reintentos:** 0 (Sin llamadas a `waitForTimeout` ni `retries`)

### Matriz de Pruebas E2E Implementadas

| ID | Nombre de la Prueba | Resultado | Qué Demuestra |
| :--- | :--- | :---: | :--- |
| **E2E-01** | Login exitoso con rol Administrador | APROBADA | Emisión y guardado de token JWT en storage. |
| **E2E-02** | Login fallido con credenciales erróneas | APROBADA | Manejo de HTTP 401 y mensaje de error al usuario. |
| **E2E-03** | Listado de Categorías desde Nginx | APROBADA | Consumo del API mediante proxy `/api/v1` relativo. |
| **E2E-04** | Creación y listado inmediato de Categoría | APROBADA | Invalidación de caché en Redis y reactividad en la UI. |
| **E2E-05** | Carga diferida con `@defer` en Inicio | APROBADA | El chunk `mas-vendidos` solo se descarga en viewport con permiso. |
| **E2E-06** | Recarga F5 en ruta `/categorias` | APROBADA | Nginx resuelve `try_files` y sirve scripts desde disco (`304` / `0 B`). |
| **E2E-07** | Flujo completo de Venta con comprobante | APROBADA | Transacción completa y descuento automático de stock. |
| **E2E-08** | Venta con cantidad mayor al stock | APROBADA | Backend responde HTTP 409 y la UI muestra alerta sin registrar venta. |
| **E2E-09** | Venta sin productos seleccionados | APROBADA | Botón «Registrar» deshabilitado o rechazo con HTTP 400. |
| **E2E-10** | Cliente nuevo y venta inmediata | APROBADA | Integración fluida entre módulo de Clientes y Ventas. |
| **E2E-11** | Filtro de fechas en listados paginados | APROBADA | El paginador regresa a la página 1 al cambiar los filtros de fecha. |
| **E2E-12** | Pantalla de Auditoría como Administrador | APROBADA | La venta recién registrada aparece en la bitácora con su detalle. |
| **E2E-13** | Intento de acceso a Auditoría como Vendedor | APROBADA | Opción oculta en menú y redirección o acceso denegado (403). |
| **E2E-14** | Token alterado o manipulado en almacenamiento | APROBADA | Redirección inmediata al login con aviso de sesión inválida. |

---

## 2. Resultados de la Colección de Postman

- **Archivo:** `postman/PharmaBackend.postman_collection.json`

| Caso | Endpoint | Código Esperado | Código Obtenido | Resultado |
| :--- | :--- | :---: | :---: | :---: |
| 01. Login Válido | `POST /api/v1/auth/login` | 200 OK | 200 OK | PASÓ |
| 02. Credenciales Inválidas | `POST /api/v1/auth/login` | 401 Unauthorized | 401 Unauthorized | PASÓ |
| 03. Registro de Venta | `POST /api/v1/ventas` | 201 Created | 201 Created | PASÓ |
| 04. Venta sin Detalles | `POST /api/v1/ventas` | 400 Bad Request | 400 Bad Request | PASÓ |
| 05. Stock Excedido | `POST /api/v1/ventas` | 409 Conflict | 409 Conflict | PASÓ |
| 06. Producto No Existente | `GET /api/v1/productos/999999` | 404 Not Found | 404 Not Found | PASÓ |
| 07. Bitácora Paginada (Admin) | `GET /api/v1/auditoria` | 200 OK | 200 OK | PASÓ |
| 08. Auditoría con Vendedor | `GET /api/v1/auditoria` | 403 Forbidden | 403 Forbidden | PASÓ |
