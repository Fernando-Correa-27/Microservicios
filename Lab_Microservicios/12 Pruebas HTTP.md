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

