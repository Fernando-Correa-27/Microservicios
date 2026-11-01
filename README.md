# GLAB-S11 · Creación de Microservicios (Tecsup)

Laboratorio individual de **Desarrollo de Aplicaciones Web Avanzado**. Implementa 7 microservicios Spring Boot independientes, cada uno con su propia base de datos MySQL, arquitectura por capas, DTO + Bean Validation, manejo global de excepciones y comunicación HTTP entre servicios.

## Requisitos del entorno

| Herramienta | Versión usada | Verificación |
|---|---|---|
| Java | **25.0.5** (Microsoft OpenJDK) | `java -version` |
| Maven | 3.9.16 | `mvn -version` |
| MySQL | 8.4.9 en `localhost:3306`, `root` sin contraseña | `mysql -u root -e "SELECT VERSION();"` |
| Spring Boot | 4.1.2 (guía: 4.1.0; se conserva el patch más reciente) | `pom.xml` |
| SO | Windows 11 | — |

## Servicios, puertos y bases de datos

### Parte 1 — Catálogo, clientes y pedidos

| Servicio | Puerto dev | Puerto prod | BD dev | BD prod (ejemplo) |
|---|---:|---:|---|---|
| cliente-service | 8081 | 9093 | bd_cliente_dev | bd_cliente |
| categoria-service | 8082 | 9092 | bd_categoria_dev | bd_categoria |
| producto-service | 8083 | 9094 | bd_producto_dev | bd_producto |
| pedido-service | 8084 | 9095 | bd_pedido_dev | bd_pedido |

### Parte 3 — Clínica Salud Integral

| Servicio | Puerto dev | Puerto prod | BD dev | BD prod (ejemplo) |
|---|---:|---:|---|---|
| paciente-service | 8085 | 9096 | bd_paciente_dev | bd_paciente |
| medico-service | 8086 | 9097 | bd_medico_dev | bd_medico |
| cita-service | 8087 | 9098 | bd_cita_dev | bd_cita |

> Los puertos de producción de los servicios distintos de categoría son una propuesta de consistencia (la guía solo fija 9092 para categoría). El perfil `prod` apunta a `jdbc:mysql://servidor/...`, un host inexistente a propósito: sirve para demostrar la selección de perfil y el fallo controlado de conexión.

## Cómo levantar todo

```powershell
# 1. Crear las bases de datos (MySQL debe estar iniciado)
mysql -u root < sql/init-databases.sql

# 2. Compilar y arrancar los 7 servicios
powershell -ExecutionPolicy Bypass -File run\start-all.ps1

# 3. Detener todo
powershell -ExecutionPolicy Bypass -File run\stop-all.ps1
```

`start-all.ps1` compila cada proyecto con Maven, arranca los jars en segundo plano y espera a que los puertos respondan. Los logs quedan en `run\logs\`.

Para un solo servicio:

```powershell
cd categoria-service
mvn spring-boot:run
# o
java -jar target\categoria-service-0.0.1-SNAPSHOT.jar
```

Perfil de producción (ejemplo, falla porque `servidor` no existe — es lo esperado):

```powershell
java -jar categoria-service\target\categoria-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Endpoints principales

### categoria-service (8082)
- `GET /api/categorias`
- `GET /api/categorias/{id}`
- `POST /api/categorias` → 201
- `PUT /api/categorias/{id}`
- `DELETE /api/categorias/{id}`

### producto-service (8083)
- CRUD `/api/productos`
- Al crear/actualizar valida `categoriaId` contra `categoria-service` (404 si no existe, 503 si está caído).

### cliente-service (8081)
- CRUD `/api/clientes`
- DNI de 8 dígitos, único (409 si se duplica).

### pedido-service (8084)
- CRUD `/api/pedidos`
- Valida `idCliente` y cada `idProducto` por HTTP.
- El total se recalcula en el servidor a partir de los detalles.
- Al actualizar, los detalles se reemplazan (`orphanRemoval`).

### paciente-service (8085)
- CRUD `/api/pacientes`
- DNI único de 8 dígitos, correo válido, fecha de nacimiento pasada.

### medico-service (8086)
- CRUD `/api/medicos` (CMP único, especialidad existente en la misma BD).
- CRUD `/api/especialidades`.
- `GET|POST /api/medicos/{id}/horarios`, `DELETE /api/horarios/{id}`.

### cita-service (8087)
- `GET /api/citas`, `GET /api/citas/{id}`
- `POST /api/citas` → 201 (estado inicial `PROGRAMADA`)
- `PUT /api/citas/{id}/estado` → cambia estado; si es `ATENDIDA`, registra la atención 1:1.
- `PUT /api/citas/{id}/cancelar` → cambia a `CANCELADA` (no borra).
- `GET /api/citas/{id}/atencion`

Transiciones válidas:

```text
PROGRAMADA → CONFIRMADA | CANCELADA
CONFIRMADA → ATENDIDA   | CANCELADA
ATENDIDA   → (terminal)
CANCELADA  → (terminal)
```

## Colecciones Postman

- `postman/GLAB-S11-Microservicios.postman_collection.json`
- `postman/GLAB-S11-Clinica.postman_collection.json`
- `postman/bodies/` contiene los JSON de ejemplo usados en las pruebas con `curl`.

Importar en Postman: **Import → File** y seleccionar el archivo. Las variables `categoriaUrl`, `productoUrl`, etc. ya están definidas.

## Decisiones y cambios respecto a la guía

1. **Spring Boot 4.1.2** en lugar de 4.1.0: los proyectos ya venían con el patch más reciente; no se bajó la versión. Se usa `spring-boot-starter-webmvc` (nombre del starter web en Boot 4) y se añadió `spring-boot-starter-validation`.
2. **Carpeta `categoría-service` renombrada a `categoria-service`**: el acento rompía rutas y codificación en Windows. El `artifactId` ya era `categoria-service`.
3. **Recursión Pedido ↔ DetallePedido**: se resolvió con `@JsonIgnore` en `DetallePedido.pedido`. El pedido serializa sus detalles sin repetir el padre.
4. **YAML**: `jpa:` se movió dentro de `spring:` (en la guía estaba al mismo nivel). `password: ""` explícito para evitar nulos.
5. **Excepciones**: `PedidoService`/`PedidoController` ya no lanzan `RuntimeException`; usan `ResourceNotFoundException` (404). Hay `GlobalExceptionHandler` en los 7 servicios con 400, 404, 409, 500 y 503.
6. **DTO + `@Valid`** en los 7 servicios. En pedidos se añadió `PedidoDTO` y `DetallePedidoDTO`; el total se calcula en el servidor.
7. **Chequeos `== null` eliminados** en los controllers: el service lanza la excepción.
8. **`PedidoService.actualizar`**: ahora actualiza cabecera y reemplaza los detalles (clear + orphanRemoval). Antes no tocaba los detalles.
9. **Integridad entre microservicios**: producto → categoría; pedido → cliente/producto; cita → paciente/médico. Se usa `RestClient` con URLs en `application-dev.yml`. Si el servicio remoto no existe: 404; si está caído: 503 JSON sin stack trace.
10. **Clínica sin FK cruzadas**: `cita.id_paciente` e `cita.id_medico` son columnas simples porque cada servicio tiene su BD. Las FK internas (`medico → especialidad`, `horario_medico → medico`, `atencion → cita`) sí se mantienen.
11. **Atención 1:1**: se crea al pasar la cita a `ATENDIDA`; exige diagnóstico. `GET /api/citas/{id}/atencion` la consulta.
12. **Cancelar = cambiar estado**, no borrar. `ATENDIDA` y `CANCELADA` son terminales.
13. **Formato de dinero**: `BigDecimal` en producto/pedido para evitar errores de redondeo de `Double`.

## Pruebas

Las pruebas se ejecutaron contra los servicios reales con `curl.exe` y cuerpos JSON en `postman/bodies/`. Resultados principales:

| Caso | Endpoint | Esperado | Obtenido |
|---|---|---|---|
| Crear categoría | POST /api/categorias | 201 | 201 |
| Categoría inválida | POST /api/categorias | 400 | 400 |
| Categoría inexistente | GET /api/categorias/999 | 404 | 404 |
| Crear producto con categoría válida | POST /api/productos | 201 | 201 |
| Producto con categoría inexistente | POST /api/productos | 404 | 404 |
| Categoría caída | POST /api/productos | 503 | 503 |
| Crear cliente | POST /api/clientes | 201 | 201 |
| DNI duplicado | POST /api/clientes | 409 | 409 |
| Crear pedido | POST /api/pedidos | 201 | 201 |
| Pedido con cliente inexistente | POST /api/pedidos | 404 | 404 |
| Producto caído | POST /api/pedidos | 503 | 503 |
| Crear paciente | POST /api/pacientes | 201 | 201 |
| Paciente inválido | POST /api/pacientes | 400 | 400 |
| DNI duplicado | POST /api/pacientes | 409 | 409 |
| Crear cita | POST /api/citas | 201 | 201 |
| Cita con fecha pasada | POST /api/citas | 400 | 400 |
| Cita con paciente inexistente | POST /api/citas | 404 | 404 |
| Confirmar cita | PUT /api/citas/1/estado | 200 | 200 |
| Atender cita | PUT /api/citas/1/estado | 200 | 200 |
| Consultar atención | GET /api/citas/1/atencion | 200 | 200 |
| Transición inválida | PUT /api/citas/3/estado | 409 | 409 |
| Cancelar cita terminal | PUT /api/citas/1/cancelar | 409 | 409 |
| Paciente caído | POST /api/citas | 503 | 503 |

## Bitácora

Vault de Obsidian: `C:\Users\lenovo\Downloads\microservicios\Lab_Microservicios` (abrir como vault). Empieza por `00 Índice (MOC).md`.
