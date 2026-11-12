# Lista de Verificación Previa para la Demostración — PharmaSoft

Lista de chequeo antes de iniciar la sustentación frente al jurado.

---

## 1. Verificación de Infraestructura y Servicios

- [x] **Base de Datos Oracle en Docker:** Contenedor `oracle-local` activo en puerto `1522` y saludable (`healthy`).
- [x] **Caché Redis en Docker:** Contenedor `deploy-redis-1` activo en puerto `6379`.
- [x] **Backend Spring Boot:** En ejecución en puerto `8080` con perfil configurado.
- [x] **Nginx y Frontend:** Contenedor `deploy-web-1` activo en puerto `8081` sirviendo la SPA compilada.
- [x] **Usuarios y Contraseñas de Prueba:**
  - `admin` / `password` (Permisos completos: Auditoría, Reportes, Usuarios).
  - `vendedor` / `password` (Permisos operativos: Catálogo, Clientes, Ventas).
- [x] **Datos de Prueba Iniciales:**
  - Cliente existente: ID `1`.
  - Productos con stock conocido en base de datos.
- [x] **Pestañas del Navegador Preparadas:**
  1. `http://localhost:8081` (Sesión cerrada).
  2. `http://localhost:8080/swagger-ui.html` o Postman.
  3. Terminal lista con comandos de Docker y logs.

---

## 2. Registro de Corridas Manuales Previas del Flujo

| Corrida | Fecha | Duración | Desvíos Observados | Estado |
| :---: | :---: | :---: | :--- | :---: |
| **1** | 10/11/2026 | 9m 15s | Ninguno; flujo fluido de venta y comprobante. | OK |
| **2** | 11/11/2026 | 8m 50s | Ninguno; tiempo ajustado según el cronómetro. | OK |
| **3** | 12/11/2026 | 9m 05s | Verificado descuento de stock y bitácora post-commit. | OK |

---

## 3. Plan de Contingencia (Plan B)
- Si un puerto entra en conflicto: reiniciar los contenedores con `docker compose restart`.
- Si la red local falla: toda la demostración corre localmente sobre Docker (`localhost`), sin dependencia de internet externo.
- Video de respaldo grabado y disponible como enlace de contingencia.
