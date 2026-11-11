---
tags:
  - glab-s11
  - bitacora
  - pasos
fecha: 2026-11-01
estado: completado
---

# 10 · Bitácora por pasos

> [!info] Cómo leer esta nota
> Cada paso registra **qué hice**, **qué comandos ejecuté**, **qué resultado obtuve** y **por qué**. Los errores están ampliados en [[11 Errores y soluciones]].

## Línea de tiempo

```mermaid
timeline
    title GLAB-S11 ejecutado
    Verificación : Java 24 detectado
                 : MySQL sin servicio
    Preparación  : JDK 25 instalado
                 : MySQL 8.4 inicializado
                 : 14 BD creadas
    Parte 1      : 4 servicios base
                 : DTO + validación + handler
                 : YAML y perfiles
    Parte 2      : RestClient
                 : 404 y 503 probados
    Parte 3      : Clínica 3 servicios
                 : Citas y atención
    Cierre       : Postman
                 : Git
                 : Vault Obsidian
```

---

## Paso 0 · Verificación del entorno

**Qué hice:** comprobar SO, Java, Maven, MySQL, Git y herramienta HTTP.

```powershell
java -version
mvn -version
Get-Command mysql
Test-NetConnection localhost -Port 3306
```

**Resultado:**

- Windows 11.
- Java **24.0.2** (la guía pide 25).
- Maven 3.9.16.
- MySQL no encontrado y puerto 3306 cerrado.
- Git disponible.

**Por qué:** no se asume nada instalado; el laboratorio exige Java 25 y MySQL en 3306 con root sin contraseña.

**Acción:** instalar JDK 25 y MySQL; ver [[01 Entorno y requisitos]].

---

## Paso 1 · Crear las bases de datos

**Qué hice:** crear las 4 BD de la guía, las 4 `_dev` del paso YAML y las 6 de la clínica.

```sql
CREATE DATABASE bd_categoria;
CREATE DATABASE bd_producto;
CREATE DATABASE bd_cliente;
CREATE DATABASE bd_pedido;
CREATE DATABASE bd_categoria_dev;
CREATE DATABASE bd_producto_dev;
CREATE DATABASE bd_cliente_dev;
CREATE DATABASE bd_pedido_dev;
CREATE DATABASE bd_paciente;
CREATE DATABASE bd_medico;
CREATE DATABASE bd_cita;
CREATE DATABASE bd_paciente_dev;
CREATE DATABASE bd_medico_dev;
CREATE DATABASE bd_cita_dev;
```

**Resultado:** `SHOW DATABASES LIKE 'bd_%'` devolvió las 14.

**Por qué:** cada microservicio debe ser dueño de su esquema; los perfiles `dev` usan las `_dev` para no mezclar datos de pruebas con los de la BD “principal”.

---

## Paso 2 · Revisar los 4 proyectos Maven

**Qué hice:** leer los `pom.xml` y compararlos con la guía.

| Punto | Encontrado | Acción |
|---|---|---|
| Spring Boot | 4.1.2 | Se conserva (patch más nuevo que 4.1.0) |
| Java | 25 | OK |
| Group / package | `com.tecsup` | OK |
| Packaging | jar | OK |
| Web | `spring-boot-starter-webmvc` | Nombre correcto en Boot 4 |
| Data JPA | presente | OK |
| DevTools | presente | OK |
| MySQL Driver | presente | OK |
| Validation | ausente | Se agregó `spring-boot-starter-validation` |
| Clase main duplicada | `com.tecsup.categoria_service` | Se eliminó |

**Por qué:** Spring Boot 4 renombró el starter web; además Initializr había generado una clase main en un subpaquete, lo que hacía fallar `repackage` con “Unable to find a single main class”.

**Resultado:** los 4 proyectos compilaron con `mvn -q -DskipTests package`.

---

## Paso 3 · Estructura de paquetes

**Qué hice:** crear en cada proyecto:

```text
src/main/java/com/tecsup/
├── controller
├── dto
├── exception
├── model
├── repository
└── services
```

**Por qué:** la guía exige esa separación y el paquete `services` en plural. En los servicios que consumen a otros se añadió `client` (capa de infraestructura HTTP), sin romper la estructura pedida.

---

## Paso 4 · categoria-service

**Qué hice:** implementar entidad, DTO, repositorio, service, controller, excepción y handler; agregar validación.

**Comandos:**

```powershell
cd categoria-service
mvn -q -DskipTests package
java -jar target\categoria-service-0.0.1-SNAPSHOT.jar
```

**Pruebas:**

```powershell
curl.exe -s -X POST http://localhost:8082/api/categorias `
  -H "Content-Type: application/json" `
  --data-binary "@postman/bodies/categoria-1.json"
# 201 {"id":1,"nombre":"Laptops","estado":"ACTIVO"}

curl.exe -s http://localhost:8082/api/categorias
# 200 [...]
```

**Resultado:** 201, 200, 400 con DTO vacío, 404 con ID 999.

**Por qué:** es el servicio base y el ejemplo de código de la guía; sirve de plantilla para los demás.

> [!warning] Primer intento de POST con curl
> `curl.exe -d '{"nombre":"Laptops"}'` desde PowerShell envió el JSON sin comillas y devolvió 400. **Solución:** usar `--data-binary "@archivo.json"`. Ver [[11 Errores y soluciones]].

---

## Paso 5 · cliente-service y producto-service

**Qué hice:** como la guía no trae código, replicar la arquitectura de categoría.

- **Cliente:** nombre, apellidos, dirección, teléfono, `nroDni` único, estado. Validaciones de DNI (8), teléfono (9), estado.
- **Producto:** nombre, precio `BigDecimal`, stock, `categoriaId`. Validación de categoría por HTTP.

**Pruebas:**

```powershell
# clientes
curl.exe ... http://localhost:8081/api/clientes
# productos
curl.exe ... http://localhost:8083/api/productos
```

**Resultado:** 201 al crear, 409 al repetir DNI, 404 si `categoriaId` no existe, 503 si categoría está caída.

**Por qué:** mantener el mismo estilo (controller → service → repository) reduce la curva de aprendizaje y hace el código predecible.

---

## Paso 6 · pedido-service y recursión

**Qué hice:** implementar `Pedido`, `DetallePedido`, repositorio, service, controller y DTOs.

**Problema:** la relación bidireccional producía recursión infinita al serializar.

**Solución:** `@JsonIgnore` en `DetallePedido.pedido`.

```java
@ManyToOne
@JoinColumn(name = "id_pedido")
@JsonIgnore
private Pedido pedido;
```

**Prueba:**

```powershell
curl.exe -s -X POST http://localhost:8084/api/pedidos `
  -H "Content-Type: application/json" `
  --data-binary "@postman/bodies/pedido-1.json"
# 201, JSON con detalles y sin "pedido" anidado
```

**Resultado:** pedido 1 total 3679.80; pedido 2 total 579.78.

**Por qué:** el cliente necesita ver los detalles, pero no que cada detalle repita el pedido completo.

---

## Paso 7 · Pruebas con los JSON de la guía

**Qué hice:** cargar los datos en el orden pedido: categorías → productos → clientes → pedidos.

| Orden | Servicio | Resultado |
|---|---|---|
| 1 | Categorías (3) | 201, IDs 1–3 |
| 2 | Productos (3) | 201, IDs 1–3 |
| 3 | Clientes (2) | 201, IDs 1–2 |
| 4 | Pedidos (2) | 201, IDs 1–2 |

**Por qué:** respetar el orden evita referencias inexistentes; productos necesitan categorías y pedidos necesitan clientes/productos.

---

## Paso 8 · Migración a YAML y perfiles

**Qué hice:** reemplazar `application.properties` por:

```text
application.yml        → spring.profiles.active: dev
application-dev.yml    → puerto, BD _dev, JPA, empresa.ambiente
application-prod.yml   → puerto 9092 (categoría), host servidor
```

**Corrección importante:** en la guía `jpa:` estaba al mismo nivel que `spring:`. Se movió **dentro** de `spring:`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bd_categoria_dev
    username: root
    password: ""
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

**Prueba dev:**

```powershell
curl.exe -s http://localhost:8082/api/categorias
# []  → BD _dev vacía
curl.exe -s -X POST ... --data-binary "@categoria-1.json"
# 201 → confirma que escribe en bd_categoria_dev
```

**Resultado:** los 4 servicios arrancaron con perfil `dev` y escribieron en las BD `_dev`.

**Por qué:** los perfiles permiten separar configuración sin recompilar; `dev` usa datos de prueba y `prod` simula un servidor real.

---

## Paso 9 · Perfil prod (fallo esperado)

**Qué hice:** arrancar categoría con `--spring.profiles.active=prod`.

```powershell
java -jar target\categoria-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

**Resultado:**

```text
The following 1 profile is active: "prod"
Tomcat initialized with port 9092 (http)
...
UnknownHostException: Host desconocido (servidor)
Failed to initialize JPA EntityManagerFactory
```

**Por qué:** el perfil prod apunta a `jdbc:mysql://servidor/...`, un host que no existe. Es lo esperado: demuestra que el perfil se selecciona y que el puerto 9092 se configura antes de fallar la conexión.

> [!success] Verificado
> El log confirma `profile "prod"` y `port 9092`. No se consideró un error del laboratorio.

---

## Paso 10 · Comunicación entre microservicios

**Qué hice:** agregar `RestClient` con URL configurable.

| Consumidor | Valida | URL |
|---|---|---|
| producto-service | categoría | `servicios.categoria.url` |
| pedido-service | cliente | `servicios.cliente.url` |
| pedido-service | producto | `servicios.producto.url` |
| cita-service | paciente | `servicios.paciente.url` |
| cita-service | médico | `servicios.medico.url` |

**Pruebas de error:**

```powershell
# detener categoria-service
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8082 -State Listen).OwningProcess -Force
curl.exe -s -X POST http://localhost:8083/api/productos ... 
# 503 {"codigo":"503","mensaje":"categoria-service no está disponible..."}
```

```powershell
# detener producto-service
curl.exe -s -X POST http://localhost:8084/api/pedidos ...
# 503 {"codigo":"503","mensaje":"producto-service no está disponible..."}
```

```powershell
# detener paciente-service
curl.exe -s -X POST http://localhost:8087/api/citas ...
# 503 {"codigo":"503","mensaje":"paciente-service no está disponible..."}
```

**Resultado:** 404 cuando el recurso remoto no existe; 503 cuando el servicio está caído; siempre JSON, sin stack trace.

**Por qué:** en microservicios la red falla; el consumidor debe degradar de forma controlada.

---

## Paso 11 · Clínica: bases y proyectos

**Qué hice:** crear `paciente-service` (8085), `medico-service` (8086) y `cita-service` (8087) con la misma configuración Maven/Java/Spring Boot.

**BD:** `bd_paciente`, `bd_medico`, `bd_cita` + `_dev`.

**Por qué:** la actividad pide 3 microservicios independientes; se mantiene la coherencia con la Parte 1.

---

## Paso 12 · Clínica: modelo y reglas

**Qué hice:**

- `paciente`: DNI único, correo, fecha de nacimiento pasada.
- `medico`: CMP único, especialidad (FK local), horarios (FK local).
- `cita`: fecha/hora, paciente/médico como columnas simples, estado, atención 1:1.

**Decisión clave:** no crear FK `cita.id_paciente` ni `cita.id_medico` porque las tablas están en otras BD. Se validan por HTTP.

**Máquina de estados:**

```text
PROGRAMADA → CONFIRMADA | CANCELADA
CONFIRMADA → ATENDIDA   | CANCELADA
ATENDIDA   → terminal
CANCELADA  → terminal
```

**Por qué:** una cita atendida o cancelada no debe “revivir”; conservar historia es requisito de negocio.

---

## Paso 13 · Pruebas de la clínica

**Orden:** pacientes → especialidades → médicos → horarios → citas.

| Prueba | Resultado |
|---|---|
| 2 pacientes | 201 |
| 2 especialidades | 201 |
| 2 médicos | 201 |
| 1 horario | 201 |
| 2 citas | 201 |
| Confirmar cita 1 | 200 |
| Atender cita 1 con diagnóstico | 200 |
| Consultar atención | 200 |
| Cancelar cita 2 | 200 |
| Fecha pasada | 400 |
| DNI/CMP duplicado | 409 |
| Paciente/médico inexistente | 404 |
| Transición inválida | 409 |
| Servicio caído | 503 |

**Resultado:** todos los casos coincidieron con lo esperado. Ver [[12 Pruebas HTTP]].

---

## Paso 14 · Postman y cuerpos JSON

**Qué hice:** generar dos colecciones v2.1 importables y guardar los cuerpos usados por `curl`.

```text
postman/
├── GLAB-S11-Microservicios.postman_collection.json
├── GLAB-S11-Clinica.postman_collection.json
└── bodies/*.json
```

**Por qué:** el informe suele pedir evidencia en Postman; las colecciones permiten reejecutar todo sin reconstruir requests a mano.

> [!tip] No se usó la GUI
> Las pruebas se ejecutaron con `curl.exe`; las colecciones se validaron con `ConvertFrom-Json` y quedan listas para importar.

---

## Paso 15 · Git y bitácora

**Qué hice:** `git init` y commits locales por avance:

```text
chore: estructura Maven, perfiles YAML, scripts de ejecucion y bases de datos
feat: microservicios de categorias, productos, clientes y pedidos...
feat: sistema de citas con paciente, medico, especialidad, horario, cita y atencion
docs: colecciones Postman de microservicios y clinica
```

**Por qué:** dejar historia revisable y no perder el avance. No se hizo push.

**Bitácora:** este vault, con notas atómicas y enlaces.

---

## Enlaces

- [[11 Errores y soluciones]]
- [[12 Pruebas HTTP]]
- [[14 Guía original y problemas detectados]]
- [[15 Conclusiones]]

## Bitácora GLAB-S12 (2026-11-05)

> [!abstract] Resumen
> S12 se construyó sobre el tag `s11-final`. Se mantuvo Boot 4.1.2/Java 25, se agregó Spring Cloud 2025.1.4, Eureka y OpenFeign, se refactorizó pedido y se probó todo contra servicios reales.

### Paso 0 · Conservar S11

```powershell
git add -u
git commit -m "chore: estado final de GLAB-S11 antes de iniciar GLAB-S12"
git tag -a s11-final -m "Entrega final GLAB-S11"
```

### Paso 1 · Verificar entorno

- MySQL 8.4 en 3306 estaba detenido; se inició con `mysqld --datadir=...` y se verificó `SELECT VERSION()` → `8.4.9`.
- Se confirmó `JAVA_HOME` = JDK 25.

### Paso 2 · Corregir eureka-server

- El proyecto creado por Initializr tenía el paquete `com.tecsup.eureka_server` y no tenía `@EnableEurekaServer`.
- Se movió a `com.tecsup`, se agregó `@EnableEurekaServer`, `application.yaml` con puerto 8761 y `register-with-eureka: false` / `fetch-registry: false`.
- Verificación: `http://localhost:8761` → HTTP 200.

### Paso 3 · Poms y clientes Eureka

- Se agregó el BOM `spring-cloud-dependencies` 2025.1.4 y `spring-cloud-starter-netflix-eureka-client` a categoria, cliente, producto, pedido (y luego a paciente, médico, cita).
- `@EnableDiscoveryClient` en las clases principales.
- `eureka:` en los `application-dev.yml` con `defaultZone: http://localhost:8761/eureka`.
- Se conservaron perfiles dev/prod y `empresa.ambiente`.

### Paso 4 · OpenFeign en producto

- `spring-cloud-starter-openfeign` + `@EnableFeignClients`.
- `CategoriaDTO`, `categoriaId` + `@Transient categoria` en `Producto`, `CategoriaClient`.
- Validación de categoría **antes** de guardar; listado tolerante.
- Prueba real con `categoriaId=9` → 201 con categoría anidada.

### Paso 5 · Refactor de pedido

- Se eliminaron `DetallePedido` y `DetallePedidoDTO`.
- Nuevo `Pedido` con `clienteId`, `productoId`, `cantidad`, `estado`.
- Se eliminaron las tablas `pedido` y `detalle_pedido` de `bd_pedido_dev`; Hibernate recreó `pedido`.
- `ClienteClient`, `ProductoClient`, `ClienteDTO`, `ProductoDTO`, `PedidoResponseDTO`.
- Endpoints `PUT /{id}/anular` y `PUT /{id}/atender`; sin DELETE.
- `ConflictException` + handler 409.

### Paso 6 · Arranque y verificación Eureka

- `run\start-all.ps1` actualizado: Eureka primero, espera 8761, luego servicios en orden.
- `/eureka/apps` mostró los 7 servicios **UP**.
- Se detectó un 503 inicial por caché de registro vacía en pedido; se corrigió con arranque secuencial, `disable-delta: true` y `registry-fetch-interval-seconds: 5`.

### Paso 7 · Pruebas

- Flujo éxito: cliente 6 → producto 9 → pedido 1 → `GET /api/pedidos/1` con cliente y producto.
- Errores: 400 (cantidad, DNI, precio), 404 (cliente/producto/pedido), 409 (transiciones), 503 (servicio caído).
- Regresión S11: `run\smoke-test.ps1` → **47/47 correctas**.

### Paso 8 · Postman y bitácora

- Nueva colección `postman/GLAB-S12-Microservicios.postman_collection.json`.
- Colección S11 actualizada al contrato de pedido de S12.
- Cuerpos JSON nuevos en `postman/bodies/`.
- Este vault actualizado.

Enlaces: [[17 Decisión de versiones Boot 4 + Spring Cloud 2025.1.x]] · [[18 Eureka y service discovery]] · [[19 OpenFeign]] · [[12 Pruebas HTTP]]

## Bitácora GLAB-S13 (2026-11-11)

> [!abstract] Resumen
> S13 se construyó sobre el tag `s12-final`. Se migró `gateway-service` a Spring Cloud Gateway 5 (WebFlux), se publicaron las 4 rutas de negocio con `lb://`, se habilitó el endpoint de actuator, se probó todo por `http://localhost:8080` (42/42) y se actualizó Postman y este vault.

### Paso 0 · Congelar S12

```powershell
git add -A
git commit -m "chore: estado final de GLAB-S12 antes de iniciar GLAB-S13"
git tag -a s12-final -m "Entrega final GLAB-S12"
```

Resultado: commit `0296fbe` y tag `s12-final`. A partir de aquí, todos los commits son locales (sin push).

### Paso 1 · Verificar el punto de partida

- MySQL **8.4.9** en 3306 respondiendo; existen las 7 BD `bd_*_dev`.
- `run\start-all.ps1 -CoreOnly` levantó eureka + categoria + producto + cliente + pedido; los 4 aparecieron **UP** en Eureka.
- `GET http://localhost:8084/api/pedidos/1` devolvió cliente y producto (OpenFeign de S12 operativo).
- Puerto **8080 libre** (sin Tomcat de XAMPP).

### Paso 2 · Alinear `gateway-service` a Boot 4 / Gateway 5

- El `pom.xml` generado ya tenía Boot 4.1.2 y Java 25, pero usaba `spring-cloud-starter-gateway-server-webmvc` y **no** tenía BOM de Spring Cloud ni clientes de Eureka.
- Se reescribió el pom: `spring-cloud-starter-gateway-server-webflux`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-loadbalancer`, actuator, test y BOM `spring-cloud-dependencies` **2025.1.4**.
- Se movió la clase de `com.tecsup.gateway_service` a **`com.tecsup`** y se agregó `@EnableDiscoveryClient`.
- Se eliminó el paquete viejo para no dejar dos clases main (error ya visto en S11).

### Paso 3 · `application.yaml` con el prefijo nuevo

- `spring.cloud.gateway.server.webflux.*` para rutas y discovery locator.
- Rutas iniciales: categoria (`/api/categorias/**`) y producto (`/api/productos/**`).
- `discovery.locator.enabled: true` + `lower-case-service-id: true`.
- `logging.level.org.springframework.cloud.gateway: DEBUG` y `...loadbalancer: DEBUG`.
- Sección `eureka` con `defaultZone: http://localhost:8761/eureka`, `registry-fetch-interval-seconds: 5`, leases de 5/10 s y `disable-delta: true`.

### Paso 4 · Build y primer arranque

```powershell
cd gateway-service
mvn -q -DskipTests package     # EXIT=0
```

Primer arranque falló:

```text
Error: Unable to access jarfile C:\Users\lenovo\Documents\Tecsup
```

Causa: `Start-Process -ArgumentList '-jar', $jar` partió la ruta por los espacios. Fix: `'-jar', "`"$jar`""`.

Segundo arranque correcto:

```text
Registering application GATEWAY-SERVICE with eureka with status UP
Discovery Client initialized ... with initial instances count: 4
```

### Paso 5 · Verificación de la Parte 1

| Petición | Resultado |
|---|---|
| `GET http://localhost:8080/api/categorias` | 200 |
| `GET http://localhost:8080/api/productos` | 200 (con `categoria` anidada) |
| `GET http://localhost:8080/categoria-service/api/categorias` | 200 (discovery locator) |
| `GET http://localhost:8080/api/inexistente` | 404 del gateway |

Los logs DEBUG mostraron `Route matched: producto-service` y `Mapping [Exchange: GET http://localhost:8080/api/productos] to Route{... uri=lb://producto-service}`: el enrutamiento con balanceo quedó confirmado.

### Paso 6 · Parte 2 · publicar cliente y pedido

- Se agregaron las rutas `cliente-service` (`/api/clientes/**`) y `pedido-service` (`/api/pedidos/**`) con `lb://`.
- `/**` cubre `/api/pedidos/{id}/anular` y `/api/pedidos/{id}/atender`, así que no se necesitaron rutas adicionales.
- Se reconstruyó y reinició el gateway; las 4 rutas quedaron activas.

### Paso 7 · Endpoint opcional de actuator

- `GET /actuator` listaba solo `info` y `health`; `/actuator/gateway/routes` devolvía 404.
- Se inspeccionó el jar de Gateway 5.0.4: `@RestControllerEndpoint(id = "gateway", defaultAccess = Access.NONE)`.
- Primer intento `management.endpoint.gateway.access: read` → **el gateway no arrancó** (`Failed to convert to type Access`).
- Correcto: `access: READ_ONLY` (valores válidos: `NONE`, `READ_ONLY`, `UNRESTRICTED`).
- Resultado: `/actuator/gateway/routes` → 200 con **9 rutas** (4 explícitas + 5 del discovery locator, incluida la del propio gateway).

### Paso 8 · Matriz de pruebas por el gateway

```powershell
powershell -ExecutionPolicy Bypass -File run\gateway-test.ps1
```

**42/42 OK**. Incluye CRUD en las 4 rutas, 400/404/409, UTF-8, discovery locator y el caso de servicio caído.

- `producto-service` detenido → `/api/productos` respondió **503** a los **95 s** (evicción de Eureka + refresco del balanceador); las otras 3 rutas siguieron en 200.
- Al reiniciar `producto-service`, la ruta se recuperó en **31 s**.

### Paso 9 · Postman y Newman

- Nueva colección `postman/GLAB-S13-Microservicios.postman_collection.json` con `baseUrl = http://localhost:8080`, 8 carpetas y 36 requests.
- Newman (carpetas 00–04, 06 y 07): **29 requests, 53 aserciones, 0 fallos**.
- La carpeta 05 incluye el caso 503 que exige detener producto-service; se validó con `run\gateway-test.ps1` (no con Newman).
- Ajuste: la aserción de rutas esperaba 8 y encontró **9** porque el gateway también se registra en Eureka. Se cambió a “al menos 8 y existen las 4 explícitas”.

### Paso 10 · Scripts y bitácora

- `run\start-all.ps1`: el gateway se levanta **al final**, con `-NoGateway` para omitirlo y `-CoreOnly` para no incluir la clínica.
- `run\stop-all.ps1`: incluye el puerto **8080**.
- Este vault: notas 21–25 nuevas y actualización de MOC, arquitectura, servicios, errores, pruebas, guía, conclusiones y comandos.

Enlaces: [[24 gateway-service]] · [[25 Spring Cloud Gateway 5 (cambios y versiones)]] · [[12 Pruebas HTTP]]
