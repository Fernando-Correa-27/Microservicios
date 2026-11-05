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
