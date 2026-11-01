---
tags: [lab, conceptos, glab-s11, spring-boot, microservicios]
created: 2026-11-01
updated: 2026-11-01
---

# 13. Conceptos clave — GLAB-S11

> [!note] Resumen de los patrones, decisiones y tecnologías aplicadas en el laboratorio.

---

## 13.1 Arquitectura de microservicios

| Concepto | Implementación en el lab |
|---|---|
| **Descomposición por dominio** | 4 servicios core (cliente, categoría, producto, pedido) + 3 clínica (paciente, médico, cita) |
| **Base de datos por servicio** | 7 esquemas `bd_*_dev` aislados; sin FK cruzadas |
| **Comunicación síncrona** | `RestClient` (Spring Boot 4) con `Retry` + `CircuitBreaker` (Resilience4j) |
| **Configuración externa** | Perfiles `dev` / `prod` en `application-{dev,prod}.yml` |
| **API contract-first** | DTOs + Bean Validation; OpenAPI implícito (Postman collections exportables) |

---

## 13.2 Stack tecnológico

| Capa | Tecnología / Versión |
|---|---|
| **Lenguaje** | Java 25 (Microsoft build 25.0.5) |
| **Framework** | Spring Boot 4.1.2 (`spring-boot-starter-webmvc`) |
| **Build** | Maven 3.9.16 |
| **Persistencia** | Spring Data JPA / Hibernate 7.4.13 / MySQL 8.4.9 |
| **Validación** | Jakarta Bean Validation 3.0 (`spring-boot-starter-validation`) |
| **Resiliencia** | Resilience4j (`Retry`, `CircuitBreaker`, `TimeLimiter`) |
| **Serialización** | Jackson (`@JsonIgnore` para romper ciclos) |
| **Testing HTTP** | PowerShell `Invoke-RestMethod` / `Invoke-WebRequest` + Newman (Postman CLI) |
| **Documentación** | Obsidian vault + Postman collections |
| **Control de versiones** | Git local (commits incrementales, sin push) |

---

## 13.3 Patrones de diseño aplicados

### DTO + Bean Validation
- **Entrada**: `*DTO` en `@RequestBody @Valid`
- **Reglas**: `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@Min`, `@DecimalMin`, `@Past`, `@Email`
- **Salida**: Entidades JPA directamente (sin mapper dedicado por simplicidad del lab)

### Excepciones controladas → HTTP JSON
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)  // 400
    @ExceptionHandler(ResourceNotFoundException.class)        // 404
    @ExceptionHandler(ServiceUnavailableException.class)      // 503
    @ExceptionHandler(DataIntegrityViolationException.class)  // 409
    @ExceptionHandler(MethodArgumentTypeMismatchException.class) // 400 (path no numérico)
    @ExceptionHandler(Exception.class)                         // 500
}
```
> [!tip] Respuesta uniforme
> ```json
> {"codigo":"404","mensaje":"Cliente no encontrado con id: 99999","timestamp":"2026-11-01T..."}
> ```

### Comunicación tolerante a fallos
```java
@Component
public class CategoriaClient {
    private final RestClient rest = RestClient.builder()
        .baseUrl("http://localhost:8082")
        .defaultHeader(MediaType.APPLICATION_JSON)
        .build();

    public Optional<CategoriaDTO> findById(Long id) {
        return retryTemplate.execute(ctx -> 
            rest.get().uri("/api/categorias/{id}", id).retrieve()
                .onStatus(HttpStatus::is4xxClientError, (req, res) -> { throw new ResourceNotFoundException(...); })
                .body(CategoriaDTO.class)
        );
    }
}
```
- **Retry**: 3 intentos, backoff exponencial 200 ms
- **CircuitBreaker**: 50% fallos en 10 seg → open 30 seg
- **Timeout**: 3 s por llamada

### Transiciones de estado (máquina de estados)
```
PROGRAMADA ──► CONFIRMADA ──► ATENDIDA (terminal)
    │                │
    └───────────────► CANCELADA (terminal)
```
- Implementado en `CitaService.cambiarEstado()` con validación `Map<Estado, Set<Estado>> PERMITIDAS`
- `ATENDIDA` y `CANCELADA` son **terminales** (bloquean cualquier transición posterior → 409)
- Atención 1:1: `Atencion.idCita` `@OneToOne` + `unique=true` + endpoint dedicado `GET /api/citas/{id}/atencion`

### Cálculo de total en servidor
- `PedidoService.guardar()` recalcula `total = Σ(detalle.precio * detalle.cantidad)`
- Cliente **no** envía `total`; evita manipulación

### Detalle de pedido — reemplazo completo
- `Pedido.detalles` con `orphanRemoval=true`, `cascade=ALL`
- `actualizar()` hace `detalles.clear()` + añade nuevos → semántica PUT = reemplazo total

---

## 13.4 Perfiles de configuración

| Perfil | Puerto | DB Host | DB Name | Uso |
|---|---|---|---|---|
| `dev` | 8081–8087 | `localhost` | `bd_*_dev` | Desarrollo / lab |
| `prod` | 9092–9098 | `servidor` (inexistente) | `bd_*` | Demostración de fallo controlado |

> [!warning] Perfil `prod` **intencionalmente falla** al resolver `servidor` → valida que la config es externa y el código no hardcodea credenciales.

---

## 13.5 Estructura de paquetes (por servicio)

```
com.tecsup
├── controller      // @RestController, @RequestMapping("/api/...")
├── dto             // *DTO, *Request, Bean Validation
├── exception       // ResourceNotFoundException, ServiceUnavailableException, GlobalExceptionHandler
├── model           // @Entity JPA
├── repository      // JpaRepository
├── services        // @Service, lógica de negocio, transacciones
└── client          // RestClient wrappers (solo servicios que llaman a otros)
```

---

## 13.6 Decisiones de modelado relevantes

| Decisión | Justificación |
|---|---|
| `BigDecimal` para precios/totales | Precisión monetaria; evita `float`/`double` |
| `@JsonIgnore` en `DetallePedido.pedido` y `Cita.atencion` | Rompe ciclos de serialización bidireccional |
| `estado` como `String` (no `enum`) en BD | Flexibilidad; validación en DTO con `@Pattern` |
| `id` `Long` en core, `Integer` en clínica | Coherencia con guía original; sin impacto funcional |
| Sin FK cruzadas entre servicios | Autonomía de despliegue; validación por HTTP |
| `ddl-auto: update` solo en dev | En prod usar Flyway/Liquibase (fuera de alcance) |

---

## 13.7 Seguridad y observabilidad (alcance del lab)

| Aspecto | Estado | Nota |
|---|---|---|
| Autenticación / JWT | ❌ No implementado | Fuera de alcance GLAB-S11 |
| HTTPS / TLS | ❌ No configurado | Solo HTTP localhost |
| Métricas / Actuator | ❌ No expuesto | `spring-boot-starter-actuator` presente pero sin endpoints |
| Trazas distribuidas | ❌ No | Requiere Micrometer + Zipkin/OTel |
| Logs estructurados | Básico | `slf4j` + Logback default |

> [!info] Próximos pasos naturales
> - Añadir `spring-boot-starter-security` + JWT filter
> - Exponer `/actuator/health`, `/actuator/prometheus`
> - Integrar `micrometer-registry-prometheus` + Grafana
> - OpenTelemetry agent para trazas W3C

---

## 13.8 Referencias rápidas

| Enlace / Ruta | Descripción |
|---|---|
| `sql/init-databases.sql` | Crea 14 esquemas (dev + prod) |
| `sql/schema-clinica.sql` | DDL documentado de tablas clínica |
| `run/start-all.ps1` | Compila y levanta los 7 servicios (perfil dev) |
| `run/stop-all.ps1` | Mata procesos en puertos 8081–8087 y 9092–9098 |
| `run/smoke-test.ps1` | 42 pruebas HTTP automatizadas |
| `postman/*.json` | Colecciones importables |
| `Lab_Microservicios/00 Índice (MOC).md` | Punto de entrada al vault |

---

> [!check] **Conceptos validados**: DTO+validación, excepción→HTTP, RestClient+resiliencia, máquinas de estado, perfiles, DB-per-servicio, testing real.