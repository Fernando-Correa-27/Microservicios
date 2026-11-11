---
tags:
  - glab-s11
  - errores
  - troubleshooting
fecha: 2026-11-01
estado: completado
---

# 11 · Errores y soluciones

> [!summary] Tabla rápida
> | # | Error | Causa | Fix |
> |---|---|---|---|
> | 1 | Java 24, guía pide 25 | JDK desactualizado | Instalar JDK 25 |
> | 2 | MySQL no responde en 3306 | Instalado sin servicio/data dir | `--initialize-insecure` + `mysqld` |
> | 3 | “Unable to find a single main class” | Dos clases main | Eliminar subpaquete generado |
> | 4 | POST con curl devuelve 400 | PowerShell altera las comillas | `--data-binary "@archivo.json"` |
> | 5 | JSON infinito en pedidos | Relación bidireccional | `@JsonIgnore` en `DetallePedido.pedido` |
> | 6 | 500 donde debía ser 404 | `RuntimeException` genérica | `ResourceNotFoundException` + handler |
> | 7 | `jpa:` no se aplica | Estaba fuera de `spring:` | Reindentar en YAML |
> | 8 | prod falla con UnknownHost | Host `servidor` inexistente | Esperado; documentado |
> | 9 | curl se corta a los 15 s | Hikari espera 30 s | `--max-time` mayor o probar tras reconectar |
> | 10 | Rutas con `categoría` | Acento en carpeta Windows | Renombrar a `categoria-service` |
> | 11 | MySQL no reinicia con Start-Process | Argumentos con espacios | Lanzar `mysqld` con shell y comillas |

---

## 1 · Java 24 en lugar de Java 25

**Error:**

```text
java version "24.0.2"
```

**Causa:** el equipo tenía solo JDK 24; la guía exige Java 25 y el `pom.xml` declara `<java.version>25</java.version>`.

**Fix:**

```powershell
winget install --id Microsoft.OpenJDK.25 --silent `
  --accept-package-agreements --accept-source-agreements

$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot'
```

**Lección:** Maven usa `JAVA_HOME`; no basta con que `java` esté en PATH.

---

## 2 · MySQL instalado pero sin servicio

**Error:**

```text
Test-NetConnection localhost -Port 3306 → False
mysql: command not found
```

**Causa:** el MSI de MySQL 8.4 se instaló, pero no creó servicio ni data directory.

**Fix:**

```powershell
$data='C:\Users\lenovo\AppData\Local\mysql-lab\data'
New-Item -ItemType Directory -Force -Path $data

& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' `
  --initialize-insecure `
  --basedir='C:\Program Files\MySQL\MySQL Server 8.4' `
  --datadir=$data

& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' `
  --basedir='C:\Program Files\MySQL\MySQL Server 8.4' `
  --datadir=$data --port=3306 --bind-address=127.0.0.1 --console
```

**Lección:** `--initialize-insecure` deja `root` sin contraseña, como pide la guía.

---

## 3 · Dos clases main en el mismo proyecto

**Error:**

```text
Unable to find a single main class from the following candidates
[com.tecsup.CategoriaServiceApplication, com.tecsup.categoria_service.CategoriaServiceApplication]
```

**Causa:** Spring Initializr generó la clase en un subpaquete con guion bajo, además de la clase creada en `com.tecsup`.

**Fix:** eliminar el subpaquete `com.tecsup.categoria_service` (y equivalentes en los otros proyectos) y conservar `com.tecsup.XxxServiceApplication`.

**Lección:** el paquete base debe ser exactamente `com.tecsup`, no `com.tecsup.categoria_service`.

---

## 4 · POST con curl devuelve 400 por JSON “inválido”

**Error:**

```text
{"codigo":"400","mensaje":"El cuerpo JSON es inválido..."}
```

**Causa:** PowerShell elimina las comillas dobles al pasar `-d '{"nombre":"Laptops"}'` a un ejecutable nativo. El servidor recibe `{nombre:Laptops}`.

**Fix:** guardar el cuerpo en un archivo y usar:

```powershell
curl.exe -s -X POST http://localhost:8082/api/categorias `
  -H "Content-Type: application/json" `
  --data-binary "@postman/bodies/categoria-1.json"
```

**Lección:** para pruebas reproducibles, los cuerpos van en archivos, no inline.

---

## 5 · Recursión infinita Pedido ↔ DetallePedido

**Error:** `StackOverflowError` / JSON gigante al serializar un pedido.

**Causa:**

```text
Pedido → List<DetallePedido> → DetallePedido.pedido → Pedido → ...
```

**Fix:** `@JsonIgnore` en el lado “hijo”:

```java
@ManyToOne
@JoinColumn(name = "id_pedido")
@JsonIgnore
private Pedido pedido;
```

**Alternativas:** `@JsonManagedReference`/`@JsonBackReference` o DTOs de salida. Se eligió `@JsonIgnore` por simplicidad.

**Lección:** en relaciones bidireccionales, una de las dos direcciones no debe serializarse.

---

## 6 · 500 donde debía ser 404

**Error:** `GET /api/pedidos/999` devolvía 500.

**Causa:** el código de la guía lanzaba `new RuntimeException("Pedido no encontrado")`; el handler genérico lo trataba como error interno.

**Fix:**

```java
throw new ResourceNotFoundException("Pedido no encontrado con id: " + id);
```

y en el handler:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<...> manejarNoEncontrado(...) {
    return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
}
```

**Lección:** la semántica del error la decide la excepción, no el controller.

---

## 7 · `jpa:` fuera de `spring:` en YAML

**Error:** Hibernate no aplicaba `ddl-auto`, `show-sql` ni el dialecto.

**Causa:** la guía escribe:

```yaml
spring:
  datasource: ...
jpa:              # ← mal: al mismo nivel que spring
  hibernate: ...
```

**Fix:**

```yaml
spring:
  datasource: ...
  jpa:            # ← dentro de spring
    hibernate:
      ddl-auto: update
    show-sql: true
```

**Lección:** YAML es sensible a la indentación; un nivel de menos cambia el significado.

---

## 8 · Perfil prod falla con UnknownHostException

**Error:**

```text
java.net.UnknownHostException: Host desconocido (servidor)
Failed to initialize JPA EntityManagerFactory
```

**Causa:** `application-prod.yml` apunta a `jdbc:mysql://servidor/bd_categoria`; el host `servidor` no existe.

**Fix:** **no es un bug**. Es el comportamiento esperado para demostrar el perfil. El log confirma:

```text
The following 1 profile is active: "prod"
Tomcat initialized with port 9092 (http)
```

**Lección:** un perfil de producción simulado debe fallar de forma explícita si no hay infraestructura.

---

## 9 · curl se corta al probar 500 con MySQL caído

**Error:** `HTTP=000` con `--max-time 15`.

**Causa:** HikariCP espera ~30 s antes de fallar; el cliente cortó antes.

**Fix:** usar `--max-time 40` o esperar. El handler respondió:

```json
{"codigo":"500","mensaje":"Error interno del servidor","timestamp":"..."}
```

**Lección:** el timeout del cliente debe ser mayor que el timeout del pool.

---

## 10 · Carpeta `categoría-service` con acento

**Error:** rutas y salida de consola con `categoría`, problemas de codificación.

**Causa:** el nombre de carpeta incluía `í`.

**Fix:** renombrar a `categoria-service`; el `artifactId` ya era `categoria-service`.

**Lección:** evitar acentos en nombres de proyectos y rutas de build.

---

## 11 · Reinicio de MySQL con `Start-Process`

**Error:** `mysqld` no arrancaba al usar `Start-Process` con argumentos que contenían espacios.

**Causa:** los argumentos `--basedir=C:\Program Files\...` se partían mal.

**Fix:** lanzar con una shell que respete las comillas:

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' `
  --basedir='C:\Program Files\MySQL\MySQL Server 8.4' `
  --datadir='C:\Users\lenovo\AppData\Local\mysql-lab\data' `
  --port=3306 --bind-address=127.0.0.1 --console
```

**Lección:** para procesos con rutas espaciosas, usar `&` con argumentos citados o `Start-Process -ArgumentList` bien construido.

---

## Advertencias que no rompen la build

| Advertencia | Significado | Acción |
|---|---|---|
| `HHH90000025: MySQLDialect does not need to be specified` | Hibernate detecta el dialecto solo | Se mantiene por fidelidad a la guía |
| `HH000271: Using @Valid on a container (List) is deprecated` | Bean Validation 3 sugiere anotar el tipo | Se mantiene `@Valid` en la lista; funciona |
| `spring.jpa.open-in-view is enabled by default` | Sesión abierta durante la vista | Aceptable para el laboratorio |

## Enlaces

- [[10 Bitácora por pasos]]
- [[12 Pruebas HTTP]]
- [[14 Guía original y problemas detectados]]

## Errores y soluciones de GLAB-S12

| Error observado | Causa | Solución aplicada |
|---|---|---|
| `CompatibilityNotMetException` al arrancar | Spring Cloud 2025.0.x no es compatible con Boot 4.1.2 | Se usó Spring Cloud **2025.1.4** en los 8 proyectos |
| `503` en el primer `POST /api/pedidos` | `pedido-service` arrancó con el registro de Eureka vacío (todos los servicios a la vez) | Arranque secuencial con espera + `disable-delta: true` + `registry-fetch-interval-seconds: 5` |
| `Failed to execute repackage ... Unable to rename ...jar` | El jar estaba en uso por un proceso Java | Detener servicios (`run\stop-all.ps1`) antes de `mvn package` |
| `"Tecnología"` en consola | Codificación de la consola de Windows, no del servicio | Enviar con `curl.exe --data-binary @archivo.json` (UTF-8 sin BOM); verificar con `[System.IO.File]::ReadAllText(..., UTF8)` |
| Producto huérfano si la categoría no existe | La guía guardaba antes de validar | Validar por Feign **antes** de `save()` en producto y pedido |
| `500` genérico al fallar Feign | No había handlers específicos | `FeignException.NotFound` → 404; `FeignException` → 503; `Exception` → 500 |
| `apellido` vs `apellidos` | La guía mezcla ambos nombres | `apellidos` en cliente-service; `@JsonAlias("apellidos")` en `ClienteDTO` del pedido |
| `No instances available for ...` | Servicio caído o aún no registrado | Se espera el registro; si está caído se devuelve 503 controlado |
| `GET /api/pedidos` rompía todo si un remoto fallaba | Listado sin tolerancia | `try/catch` por pedido y campo remoto en `null` |
| `MySQL ... (10061)` | MySQL detenido | Iniciar `mysqld` en 3306 antes de probar |
| Acentos corruptos en columnas | Charset heredado de S11 | `ALTER DATABASE/TABLE ... utf8mb4` y conexión con `characterEncoding=UTF-8` |

> [!tip] Lección
> Los errores de S12 fueron mayormente de **integración** (registro, orden de arranque, codificación), no de lógica de negocio. Probar contra servicios reales fue lo que los hizo visibles.

Enlaces: [[18 Eureka y service discovery]] · [[19 OpenFeign]] · [[14 Guía original y problemas detectados]]

## Errores y soluciones de GLAB-S13

> [!summary] Tabla rápida
> | # | Error | Causa | Fix |
> |---|---|---|---|
> | 1 | Starter del gateway no encontrado | La guía usa `spring-cloud-starter-gateway`, inexistente en Cloud 2025.1 | `spring-cloud-starter-gateway-server-webflux` |
> | 2 | Todo responde 404 sin error | Configuración con prefijo viejo `spring.cloud.gateway.*` | Prefijo `spring.cloud.gateway.server.webflux.*` |
> | 3 | `/actuator/gateway/routes` da 404 | Endpoint con `defaultAccess = NONE` | `management.endpoint.gateway.access: READ_ONLY` |
> | 4 | El gateway no arranca con `access: read` | El enum `Access` no acepta `read` | Valores válidos: `NONE`, `READ_ONLY`, `UNRESTRICTED` |
> | 5 | Dos clases main | Paquete generado `com.tecsup.gateway_service` | Mover a `com.tecsup` y borrar el paquete viejo |
> | 6 | `Unable to access jarfile C:\Users\lenovo\Documents\Tecsup` | Ruta con espacios sin comillas en `Start-Process` | `'-jar', "`"$jar`""` |
> | 7 | 503 al inicio | Eureka aún no propaga el registro | Esperar/reintentar; fetch de 5 s y leases cortos |
> | 8 | 503 tarda 70–95 s en aparecer | Evicción de Eureka + caché del balanceador | Es el comportamiento normal; documentado |
> | 9 | 9 rutas donde se esperaban 8 | El propio gateway se registra en Eureka | Contar 4 explícitas + N registradas |
> | 10 | `repackage ... Unable to rename` | Jar en ejecución | Detener servicios antes de `mvn package` |
> | 11 | `spring-boot-starter-webflux-test` no resuelve | No está en el `.m2` local | Usar `spring-boot-starter-test` |
> | 12 | Scripts `.ps1` con acentos rotos | PowerShell 5.1 lee sin BOM como ANSI | Scripts en ASCII + payloads UTF-8 en archivos |

### 1 · Starter del gateway

```text
java.lang.ClassNotFoundException: org.springframework.cloud.gateway.config.GatewayAutoConfiguration
```

La guía usa `spring-cloud-starter-gateway`, que ya no existe en el tren 2025.1.x. Se reemplazó por `spring-cloud-starter-gateway-server-webflux` (variante reactiva/Netty). **No** se agregó `spring-boot-starter-web`.

### 2 · Prefijo de configuración

Con `spring.cloud.gateway.routes` el gateway **arranca sin errores**, pero no carga ninguna ruta y todo devuelve 404. Es el fallo más silencioso del laboratorio.

```yaml
# mal
spring.cloud.gateway.routes
# bien (Gateway 5)
spring.cloud.gateway.server.webflux.routes
```

### 3 y 4 · Endpoint de actuator

Inspección del jar 5.0.4:

```java
@RestControllerEndpoint(id = "gateway", defaultAccess = Access.NONE)
```

- Solo con `exposure.include: gateway` no aparece en `/actuator`.
- `access: read` rompe el arranque: `Invalid value 'read' ... Failed to convert to type Access`.
- `access: READ_ONLY` habilita `GET /actuator/gateway/routes` sin permitir modificaciones.

### 5 · Dos clases main

Spring Initializr generó `com.tecsup.gateway_service.GatewayServiceApplication`; al crear `com.tecsup.GatewayServiceApplication` quedaron dos. Se eliminó el paquete generado.

### 6 · Ruta con espacios en `Start-Process`

```text
Error: Unable to access jarfile C:\Users\lenovo\Documents\Tecsup
```

`Start-Process -ArgumentList '-jar', $jar` no cita la ruta. Fix:

```powershell
Start-Process -FilePath $java -ArgumentList '-jar', "`"$jar`"" ...
```

### 7 y 8 · Propagación de Eureka y el 503

| Momento | Tiempo observado |
|---|---|
| Detener `producto-service` → primer 503 controlado | **70 s** y **95 s** en dos corridas |
| Reiniciar `producto-service` → primer 200 | **10 s** y **31 s** |

Mientras Eureka no expulsa la instancia muerta, el balanceador intenta conectarse y puede devolver 500/503. El 503 limpio aparece cuando el registro ya no tiene instancias:

```text
WARN o.s.c.l.core.RoundRobinLoadBalancer : No servers available for service: producto-service
```

### 9 · Número de rutas

`/actuator/gateway/routes` devolvió **9**, no 8: el discovery locator crea una ruta también para `GATEWAY-SERVICE`. La aserción de Postman se ajustó a “al menos 8 y que existan las 4 explícitas”.

### 10 · Build con el jar en ejecución

Heredado de S12: Windows bloquea el jar y Maven falla al renombrarlo en `repackage`. Solución: `run\stop-all.ps1` antes de compilar.

### 11 · Starter de test

`spring-boot-starter-webflux-test` no estaba en el repositorio local. Se mantuvo `spring-boot-starter-test`, igual que en los otros 7 proyectos.

### 12 · Codificación de scripts

- Los `.ps1` se guardaron en **ASCII** y los datos con tildes en `run\payloads\*.json` (UTF-8 sin BOM), enviados con `curl.exe --data-binary`.
- En los scripts se construyen las tildes con `[char]0xED`, `[char]0xE1`, etc., para no depender de la codificación del archivo.
- La consola de Windows muestra mojibake aunque los archivos sean UTF-8 válidos; se verificó leyendo bytes con `[System.Text.UTF8Encoding]::new($false, $true)`.

> [!tip] Lección
> Los errores de S13 fueron de **configuración y versiones**, no de lógica. Un prefijo equivocado o un starter renombrado no producen un error claro: producen 404 o un fallo de arranque que hay que diagnosticar leyendo el jar y los metadatos.

Enlaces: [[25 Spring Cloud Gateway 5 (cambios y versiones)]] · [[10 Bitácora por pasos]]
