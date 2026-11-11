---
tags: [lab, pruebas, http, glab-s11]
created: 2026-11-01
updated: 2026-11-01
---

# 12. Pruebas HTTP — GLAB-S11

Tabla consolidada de **42 pruebas automatizadas** ejecutadas con PowerShell (`run/smoke-test.ps1`) contra los 7 microservicios en perfil `dev` (puertos 8081–8087).

> [!note] Pruebas cubiertas
> - CRUD + validaciones Bean Validation (400)
> - 404 por ID inexistente
> - 409 por duplicados (DNI, CMP)
> - Comunicación entre servicios (RestClient) → 404 si recurso remoto no existe
> - 503 JSON cuando dependencia caída
> - 500 JSON cuando MySQL caído
> - Transiciones de estado de Cita + Atención 1:1

---

## 12.1 Tabla de resultados

| # | Servicio | Método | Endpoint / Caso | Esperado | Obtenido | ✓ |
|---|---|---|---|---|---|---|
| 1 | cliente | GET | `/api/clientes` | 200 | 200 | ✓ |
| 2 | cliente | GET | `/api/clientes/1` | 200 | 200 | ✓ |
| 3 | cliente | GET | `/api/clientes/99999` | 404 | 404 | ✓ |
| 4 | cliente | GET | `/api/clientes/abc` | 400 | 400 | ✓ |
| 5 | cliente | POST | `/api/clientes` (sin nombre) | 400 | 400 | ✓ |
| 6 | cliente | POST | `/api/clientes` (teléfono inválido) | 400 | 400 | ✓ |
| 7 | cliente | POST | `/api/clientes` (DNI duplicado 12345678) | 409 | 409 | ✓ |
| 8 | cliente | POST | `/api/clientes` (válido) | 201 | 201 | ✓ |
| 9 | categoria | GET | `/api/categorias` | 200 | 200 | ✓ |
| 10 | categoria | GET | `/api/categorias/abc` | 400 | 400 | ✓ |
| 11 | categoria | GET | `/api/categorias/99999` | 404 | 404 | ✓ |
| 12 | categoria | POST | `/api/categorias` (nombre vacío) | 400 | 400 | ✓ |
| 13 | categoria | PUT | `/api/categorias/99999` | 404 | 404 | ✓ |
| 14 | categoria | POST | `/api/categorias` (válido) | 201 | 201 | ✓ |
| 15 | producto | GET | `/api/productos` | 200 | 200 | ✓ |
| 16 | producto | GET | `/api/productos/99999` | 404 | 404 | ✓ |
| 17 | producto | POST | `/api/productos` (precio negativo) | 400 | 400 | ✓ |
| 18 | producto | POST | `/api/productos` (categoría 99999) | 404 | 404 | ✓ |
| 19 | producto | POST | `/api/productos` (válido) | 201 | 201 | ✓ |
| 20 | pedido | GET | `/api/pedidos` | 200 | 200 | ✓ |
| 21 | pedido | POST | `/api/pedidos` (sin detalles) | 400 | 400 | ✓ |
| 22 | pedido | POST | `/api/pedidos` (cliente 99999) | 404 | 404 | ✓ |
| 23 | pedido | POST | `/api/pedidos` (producto 99999) | 404 | 404 | ✓ |
| 24 | pedido | POST | `/api/pedidos` (válido) | 201 | 201 | ✓ |
| 25 | paciente | GET | `/api/pacientes` | 200 | 200 | ✓ |
| 26 | paciente | GET | `/api/pacientes/99999` | 404 | 404 | ✓ |
| 27 | paciente | POST | `/api/pacientes` (DNI inválido) | 400 | 400 | ✓ |
| 28 | paciente | POST | `/api/pacientes` (DNI duplicado) | 409 | 409 | ✓ |
| 29 | medico | GET | `/api/especialidades` | 200 | 200 | ✓ |
| 30 | medico | GET | `/api/medicos` | 200 | 200 | ✓ |
| 31 | medico | GET | `/api/medicos/99999` | 404 | 404 | ✓ |
| 32 | medico | POST | `/api/medicos` (especialidad 99999) | 404 | 404 | ✓ |
| 33 | medico | POST | `/api/medicos` (CMP duplicado) | 409 | 409 | ✓ |
| 34 | cita | GET | `/api/citas` | 200 | 200 | ✓ |
| 35 | cita | POST | `/api/citas` (paciente 99999) | 404 | 404 | ✓ |
| 36 | cita | POST | `/api/citas` (médico 99999) | 404 | 404 | ✓ |
| 37 | cita | POST | `/api/citas` (sin fecha) | 400 | 400 | ✓ |
| 38 | cita | POST | `/api/citas` (válida) | 201 | 201 | ✓ |
| 39 | cita | PUT | `/api/citas/{id}/estado` PROGRAMADA→CONFIRMADA | 200 | 200 | ✓ |
| 40 | cita | PUT | `/api/citas/{id}/estado` CONFIRMADA→ATENDIDA | 200 | 200 | ✓ |
| 41 | cita | PUT | `/api/citas/{id}/estado` ATENDIDA→CANCELADA | 409 | 409 | ✓ |
| 42 | cita | GET | `/api/citas/{id}/atencion` | 200 | 200 | ✓ |

**Total: 42 / 42 ✓**

---

## 12.2 Pruebas de resiliencia (dependencias caídas)

| Caso | Acción | Resultado | Comportamiento |
|---|---|---|---|
| **producto-service** llama a **categoria-service** (8082) | `POST /api/productos` con `categoriaId` válido mientras 8082 down | **503** | `{"codigo":"503","mensaje":"categoria-service no está disponible..."}` |
| **pedido-service** llama a **cliente-service** (8081) | `POST /api/pedidos` con `idCliente` válido mientras 8081 down | **503** | JSON controlado |
| **cita-service** llama a **paciente-service** (8085) | `POST /api/citas` con `idPaciente` válido mientras 8085 down | **503** | JSON controlado |
| **cita-service** llama a **medico-service** (8086) | `POST /api/citas` con `idMedico` válido mientras 8086 down | **503** | JSON controlado |

> [!success] Patrón implementado
> - `RestClient` con `Retry` + `CircuitBreaker` (Resilience4j)
> - Excepción `ServiceUnavailableException` → `GlobalExceptionHandler` → HTTP 503 JSON
> - No hay cascada de fallos; servicio llamador responde en < 5 s

---

## 12.3 Prueba de error interno (500)

| Caso | Acción | Resultado | Comportamiento |
|---|---|---|---|
| MySQL detenido (`mysqld` kill) | `GET /api/clientes` | **500** | `{"codigo":"500","mensaje":"Error interno del servidor","timestamp":"..."}` |

> [!note] El handler genérico (`@ExceptionHandler(Exception.class)`) captura `DataAccessResourceFailureException` / `CommunicationsException` y devuelve JSON uniforme sin stack trace.

---

## 12.4 Colecciones Postman

| Archivo | Descripción |
|---|---|
| `postman/GLAB-S11-Microservicios.postman_collection.json` | 4 servicios base + comunicación + errores |
| `postman/GLAB-S11-Clinica.postman_collection.json` | 3 servicios clínica + transiciones + atención |
| `postman/bodies/*.json` | Cuerpos de ejemplo listos para importar |

> [!tip] Ejecutar en CLI con Newman
> ```bash
> newman run postman/GLAB-S11-Microservicios.postman_collection.json -e postman/environment-dev.json
> newman run postman/GLAB-S11-Clinica.postman_collection.json -e postman/environment-dev.json
> ```

---

## 12.5 Evidencia de ejecución

Archivo generado: `run/smoke-results.json` (JSON con detalle de cada prueba: status, payload, timestamp).

```json
[
  {"Prueba":"Listar clientes","Metodo":"GET","Esperado":200,"Obtenido":200,"OK":"SI","Detalle":"[{...}]"},
  {"Prueba":"DNI duplicado","Metodo":"POST","Esperado":409,"Obtenido":409,"OK":"SI","Detalle":"{...}"}
  ...
]
```

> [!info] Ejecutar de nuevo
> ```powershell
> powershell -ExecutionPolicy Bypass -File run\smoke-test.ps1
> ```

---

## 12.6 Resumen de cobertura

| Categoría | Pruebas | Estado |
|---|---|---|
| CRUD + listado | 14 | ✓ |
| Validación DTO (400) | 9 | ✓ |
| 404 recurso inexistente | 9 | ✓ |
| 409 conflicto unicidad | 4 | ✓ |
| 201 creación | 6 | ✓ |
| Comunicación inter-servicio (404 remoto) | 4 | ✓ |
| 503 dependencia caída | 4 | ✓ |
| 500 DB caída | 1 | ✓ |
| Transiciones estado + atención | 4 | ✓ |
| **TOTAL** | **42** | **42/42** |

> [!check] **Todas las pruebas pasan** — laboratorio validado end-to-end.

## Pruebas GLAB-S12 (servicios reales)

> [!info] Entorno
> MySQL 8.4 en 3306, Eureka en 8761, servicios en 8081–8087. Pruebas con `curl.exe` y `run\smoke-test.ps1`. Los cuerpos se enviaron como UTF-8 sin BOM con `--data-binary @archivo.json`.

### Flujo de éxito

| # | Request | Esperado | Obtenido | Evidencia |
|---|---|---|---|---|
| 1 | `POST /api/categorias` `{"nombre":"Tecnología","estado":"ACTIVO"}` | 201 | **201** | id 9, tilde correcta |
| 2 | `POST /api/clientes` (Carlos, DNI 45678912) | 201 | **201** | id 6 |
| 3 | `POST /api/productos` `categoriaId=9` | 201 | **201** | `categoria` anidada |
| 4 | `GET /api/productos` | 200 | **200** | todos con `categoria` |
| 5 | `POST /api/pedidos` `{"clienteId":6,"productoId":9,"cantidad":2}` | 201 | **201** | id 1, PENDIENTE |
| 6 | `GET /api/pedidos/1` | 200 | **200** | pedido + cliente + producto |
| 7 | `PUT /api/pedidos/1/atender` | 200 | **200** | ATENDIDO |
| 8 | `PUT /api/pedidos/2/anular` | 200 | **200** | ANULADO |

### Errores y validaciones

| # | Request | Esperado | Obtenido |
|---|---|---|---|
| 9 | `POST /api/pedidos` clienteId 999 | 404 | **404** |
| 10 | `POST /api/pedidos` productoId 999 | 404 | **404** |
| 11 | `POST /api/pedidos` cantidad 0 | 400 | **400** |
| 12 | `POST /api/pedidos` sin cantidad | 400 | **400** |
| 13 | `GET /api/pedidos/999` | 404 | **404** |
| 14 | `PUT /api/pedidos/999/anular` | 404 | **404** |
| 15 | `PUT /api/pedidos/1/anular` (ya ATENDIDO) | 409 | **409** |
| 16 | `PUT /api/pedidos/2/atender` (ya ANULADO) | 409 | **409** |
| 17 | `POST /api/productos` precio 0 | 400 | **400** |
| 18 | `POST /api/productos` precio -5 | 400 | **400** |
| 19 | `POST /api/productos` nombre vacío | 400 | **400** |
| 20 | `POST /api/productos` categoriaId 777 | 404 | **404** |
| 21 | `POST /api/clientes` DNI 1234567 (7 dígitos) | 400 | **400** |
| 22 | `POST /api/clientes` DNI 1234567A (letras) | 400 | **400** |

### Servicio caído (prueba destructiva controlada)

| # | Acción | Esperado | Obtenido |
|---|---|---|---|
| 23 | Detener `producto-service` y `GET /api/pedidos/1` | 503 | **503** JSON controlado |
| 24 | Detener `producto-service` y `GET /api/pedidos` | 200 con `producto:null` | **200** con `producto:null` |
| 25 | Detener `producto-service` y `POST /api/pedidos` | 503 | **503** |
| 26 | Detener `categoria-service` y `GET /api/productos` | 200 con `categoria:null` | **200** con `categoria:null` |
| 27 | Detener `categoria-service` y `GET /api/productos/1` | 503 | **503** |
| 28 | Detener `categoria-service` y `POST /api/productos` | 503 | **503** |

### Regresión S11

```text
run\smoke-test.ps1
Total: 47  Correctas: 47  Fallas: 0
```

Incluye cliente, categoría, producto, pedido (contrato S12), paciente, médico, cita y transiciones de estado.

Enlaces: [[20 Comunicación entre servicios (flujo GET pedido)]] · [[11 Errores y soluciones]]


## Pruebas GLAB-S13 por el API Gateway

> [!info] Entorno
> MySQL 8.4.9 en 3306 · Eureka en 8761 · servicios en 8081–8084 · **gateway en 8080**. Todas las peticiones se hicieron contra `http://localhost:8080` con `curl.exe` desde `run\gateway-test.ps1`. Los cuerpos con tildes se enviaron como UTF-8 sin BOM con `--data-binary @archivo.json`.

### Tabla completa (42 pruebas)

| # | Prueba | Esperado | Obtenido | ✓ |
|---|---|---|---|---|
| 1 | GET `/api/categorias` | 200 | 200 | ✓ |
| 2 | GET `/api/categorias/1` | 200 | 200 | ✓ |
| 3 | GET `/api/productos` | 200 | 200 | ✓ |
| 4 | GET `/api/productos/1` | 200 | 200 | ✓ |
| 5 | GET `/api/clientes` | 200 | 200 | ✓ |
| 6 | GET `/api/clientes/1` | 200 | 200 | ✓ |
| 7 | GET `/api/pedidos` | 200 | 200 | ✓ |
| 8 | GET `/api/pedidos/1` (Feign) | 200 | 200 | ✓ |
| 9 | POST `/api/categorias` | 201 | 201 | ✓ |
| 10 | PUT `/api/categorias/{id}` | 200 | 200 | ✓ |
| 11 | DELETE `/api/categorias/{id}` | 200 | 200 | ✓ |
| 12 | POST `/api/productos` | 201 | 201 | ✓ |
| 13 | PUT `/api/productos/{id}` | 200 | 200 | ✓ |
| 14 | DELETE `/api/productos/{id}` | 200 | 200 | ✓ |
| 15 | POST `/api/clientes` | 201 | 201 | ✓ |
| 16 | PUT `/api/clientes/{id}` | 200 | 200 | ✓ |
| 17 | DELETE `/api/clientes/{id}` | 200 | 200 | ✓ |
| 18 | POST `/api/pedidos` | 201 | 201 | ✓ |
| 19 | PUT `/api/pedidos/{id}/anular` | 200 | 200 | ✓ |
| 20 | PUT `/api/pedidos/{id}/anular` (repetido) | 409 | 409 | ✓ |
| 21 | PUT `/api/pedidos/{id}/atender` | 200 | 200 | ✓ |
| 22 | POST `/api/categorias` inválido | 400 | 400 | ✓ |
| 23 | POST `/api/productos` inválido | 400 | 400 | ✓ |
| 24 | POST `/api/clientes` inválido | 400 | 400 | ✓ |
| 25 | POST `/api/pedidos` inválido | 400 | 400 | ✓ |
| 26 | GET `/api/productos/999999` | 404 | 404 | ✓ |
| 27 | GET `/api/categorias/999999` | 404 | 404 | ✓ |
| 28 | GET `/api/inexistente` | 404 | 404 | ✓ |
| 29 | GET `/api/inexistente/sub` | 404 | 404 | ✓ |
| 30 | POST `/api/categorias` `"Tecnología"` | 201 | 201 | ✓ |
| 31 | GET `/api/categorias/{id}` (tilde) | 200 | 200 | ✓ |
| 32 | DELETE `/api/categorias/{id}` | 200 | 200 | ✓ |
| 33 | POST `/api/productos` `"Cámara Web Ñandú"` | 201 | 201 | ✓ |
| 34 | GET `/api/productos/{id}` (tildes) | 200 | 200 | ✓ |
| 35 | DELETE `/api/productos/{id}` | 200 | 200 | ✓ |
| 36 | GET `/categoria-service/api/categorias` | 200 | 200 | ✓ |
| 37 | GET `/producto-service/api/productos` | 200 | 200 | ✓ |
| 38 | GET `/api/productos` con servicio caído | 503 | 503 | ✓ |
| 39 | GET `/api/categorias` con producto caído | 200 | 200 | ✓ |
| 40 | GET `/api/clientes` con producto caído | 200 | 200 | ✓ |
| 41 | GET `/api/pedidos` con producto caído | 200 | 200 | ✓ |
| 42 | GET `/api/productos` tras reiniciar | 200 | 200 | ✓ |

**Total: 42 / 42** — evidencia en `run/logs/gateway-test-results.md`.

### Errores propagados intactos

| Caso | Status | Cuerpo observado |
|---|---|---|
| POST categoría vacía | 400 | `{"codigo":"400","mensaje":"Error de validación en la petición","errores":{...}}` |
| POST producto precio 0 / stock -1 | 400 | `"El precio debe ser mayor a cero"`, `"El stock no puede ser negativo"` |
| POST cliente teléfono 123 | 400 | mapa `errores` con `"El teléfono debe tener exactamente 9 dígitos"` |
| POST pedido cantidad 0 | 400 | `"La cantidad debe ser mayor a cero"` |
| GET producto 999999 | 404 | JSON del servicio |
| GET `/api/inexistente` | 404 | JSON del **gateway** con `path` y `requestId` |

> [!success] El gateway no reescribe errores
> El status y el cuerpo viajan tal cual desde el servicio. El gateway solo genera sus propios 404 (ruta inexistente) y 503 (sin instancia).

### Servicio caído y recuperación

| Acción | Resultado | Tiempo |
|---|---|---|
| Detener `producto-service` (8083) | — | — |
| `GET /api/productos` por el gateway | **503** `Service Unavailable` | 70 s / 95 s |
| `GET /api/categorias`, `/clientes`, `/pedidos` | 200 | inmediato |
| Reiniciar `producto-service` | — | — |
| `GET /api/productos` | **200** | 10 s / 31 s |

```json
{"timestamp":"2026-11-11T22:28:52.973Z","path":"/api/productos",
 "status":503,"error":"Service Unavailable","requestId":"d3ad5568-141"}
```

Log del balanceador:

```text
WARN o.s.c.l.core.RoundRobinLoadBalancer : No servers available for service: producto-service
```

### UTF-8 de punta a punta

| Petición | Resultado |
|---|---|
| `POST /api/categorias` `{"nombre":"Tecnología","estado":"ACTIVO"}` | 201, devuelve `Tecnología` |
| `GET /api/categorias/{id}` | 200, `Tecnología` intacto |
| `POST /api/productos` `{"nombre":"Cámara Web Ñandú",...}` | 201, `ñ` y `ú` correctas |
| `GET /api/productos/{id}` | 200, tildes intactas |

### Rutas del gateway

```powershell
curl.exe -s http://localhost:8080/actuator/gateway/routes
# 9 rutas: 4 explicitas + 5 del discovery locator (incluye gateway-service)
```

### Newman (colección Postman)

```text
Carpetas 00–04, 06, 07 (la 05 incluye el caso 503 manual)
requests: 29   assertions: 53   failed: 0
```

Evidencia: `run/logs/newman-s13.log`.

### Cómo repetir

```powershell
# Todo el gateway (incluye detener y reiniciar producto-service)
powershell -ExecutionPolicy Bypass -File run\gateway-test.ps1

# Solo la colección Postman
powershell -ExecutionPolicy Bypass -File run\newman-s13.ps1
```

Enlaces: [[24 gateway-service]] · [[11 Errores y soluciones]] · [[22 Rutas del gateway (predicados, lb y balanceo)]]
