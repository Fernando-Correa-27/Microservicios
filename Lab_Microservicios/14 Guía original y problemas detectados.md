---
tags: [lab, guia-original, problemas, correcciones, glab-s11]
created: 2026-11-01
updated: 2026-11-01
---

# 14. Guía original y problemas detectados — GLAB-S11

> [!warning] Documento de referencia
> Basado en **GLAB-S11-RCOELLO-2026-01-3.docx** (guía oficial del curso).  
> Cada problema incluye: **código original** (o descripción), **fallo observado**, **solución aplicada**.

---

## 14.1 Problemas en servicios base (Partes 1–2)

### 1. Pedido / DetallePedido — recursión JSON infinita

| | |
|---|---|
| **Guía** | Relación bidireccional `@OneToMany(mappedBy="pedido")` + `@ManyToOne` sin `@JsonIgnore` |
| **Fallo** | `StackOverflowError` al serializar `Pedido → detalles → pedido → detalles…` |
| **Solución** | `@JsonIgnore` en `DetallePedido.pedido`; se expone `pedidoId` implícito en JSON del padre |

```java
// DetallePedido.java — ANTES (guía)
@ManyToOne
@JoinColumn(name = "id_pedido")
private Pedido pedido;

// DetallePedido.java — DESPUÉS (fix)
@ManyToOne
@JoinColumn(name = "id_pedido")
@JsonIgnore
private Pedido pedido;
```

---

### 2. `jpa:` mal indentado en `application.yml`

| | |
|---|---|
| **Guía** | `jpa:` al mismo nivel que `spring:` (hermano) |
| **Fallo** | Spring no lee `hibernate.ddl-auto`, `show-sql`, etc. |
| **Solución** | Anidar bajo `spring:` |

```yaml
# ANTES (guía)
spring:
  datasource: ...
jpa:
  hibernate:
    ddl-auto: update

# DESPUÉS (fix)
spring:
  datasource: ...
  jpa:
    hibernate:
      ddl-auto: update
```

---

### 3. `RuntimeException` genérica → 500 en lugar de 404

| | |
|---|---|
| **Guía** | `repo.findById(id).orElseThrow(() -> new RuntimeException("No encontrado"))` |
| **Fallo** | Cualquier `RuntimeException` no manejada → 500 Internal Server Error |
| **Solución** | Excepción de dominio `ResourceNotFoundException` + `@ExceptionHandler` → 404 JSON |

```java
// Service — ANTES
return repo.findById(id).orElseThrow(() -> new RuntimeException("No encontrado"));

// Service — DESPUÉS
return repo.findById(id).orElseThrow(
    () -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));

// GlobalExceptionHandler
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<Map<String,Object>> handleNotFound(ResourceNotFoundException ex) {
    return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
}
```

---

### 4. Falta de DTOs y Bean Validation

| | |
|---|---|
| **Guía** | `@RequestBody` directo a `@Entity`; validaciones manuales `if (x == null) throw...` |
| **Fallo** | Entidad expuesta en API; validaciones dispersas; sin mensajes estandarizados |
| **Solución** | Paquete `dto` con `*DTO` + anotaciones Jakarta (`@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@DecimalMin`, `@Past`, `@Email`); `@Valid` en controller |

```java
// ClienteDTO.java
@NotBlank(message = "El nombre es obligatorio")
@Size(max = 100)
private String nombre;

@NotBlank(message = "El DNI es obligatorio")
@Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
private String nroDni;

// Controller
@PostMapping
public ResponseEntity<Cliente> guardar(@Valid @RequestBody ClienteDTO dto) { ... }
```

---

### 5. Validaciones redundantes / nulas en service

| | |
|---|---|
| **Guía** | `if (categoria.getNombre() == null || categoria.getNombre().isEmpty()) throw...` |
| **Fallo** | Duplica lo que ya hace Bean Validation; código verbose |
| **Solución** | Eliminar checks manuales; confiar en `@Valid` + `MethodArgumentNotValidException` handler |

---

### 6. PedidoService.actualizar() — actualización de detalles incorrecta

| | |
|---|---|
| **Guía** | `pedido.setDetalles(nuevosDetalles)` sin limpiar colección previa |
| **Fallo** | Hibernate no detecta orfandad; quedan detalles huérfanos o duplicados |
| **Solución** | `detalles.clear()` + `addAll(nuevos)` con `orphanRemoval=true`, `cascade=ALL` |

```java
// Pedido.java
@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
private List<DetallePedido> detalles = new ArrayList<>();

// PedidoService.actualizar()
existente.getDetalles().clear();
for (DetallePedidoDTO d : dto.getDetalles()) {
    DetallePedido det = new DetallePedido();
    det.setPedido(existente);
    det.setIdProducto(d.getIdProducto());
    det.setCantidad(d.getCantidad());
    det.setPrecio(d.getPrecio());
    existente.getDetalles().add(det);
}
return repo.save(existente);
```

---

### 7. ClienteDTO / PacienteDTO — campos obligatorios faltantes

| | |
|---|---|
| **Guía** | Solo `@NotBlank` en `nombre` |
| **Fallo** | `direccion`, `telefono`, `estado`, `dni`/`nroDni` llegan null → NPE o BD inválida |
| **Solución** | Anotar **todos** los campos obligatorios con `@NotBlank` / `@NotNull` + `@Pattern` |

---

### 8. Producto — validación de categoría remota

| | |
|---|---|
| **Guía** | No valida existencia de `idCategoria` al crear producto |
| **Fallo** | FK violada en BD o dato inconsistente |
| **Solución** | `ProductoService.guardar()` llama `categoriaClient.findById(dto.getCategoriaId())` → 404 si no existe |

---

## 14.2 Problemas en comunicación (Parte 2)

### 9. URLs hardcodeadas

| | |
|---|---|
| **Guía** | `String url = "http://localhost:8082/api/categorias/" + id;` |
| **Fallo** | No funciona en prod; no testeable; acopla a puertos fijos |
| **Solución** | `application.yml` con `clients.categoria-service.url` + `@Value` / `@ConfigurationProperties` |

```yaml
# application-dev.yml
clients:
  categoria-service:
    url: http://localhost:8082
  cliente-service:
    url: http://localhost:8081
```

```java
@Value("${clients.categoria-service.url}")
private String categoriaBaseUrl;

public Optional<CategoriaDTO> findById(Long id) {
    return rest.get().uri(categoriaBaseUrl + "/api/categorias/{id}", id) ...
}
```

---

### 10. Falta de manejo de 404 remoto

| | |
|---|---|
| **Guía** | `rest.get()...body(Categoria.class)` sin `onStatus` |
| **Fallo** | 404 remoto lanza `RestClientResponseException` → 500 local |
| **Solución** | `.onStatus(HttpStatus::is4xxClientError, (req,res) -> throw ResourceNotFoundException)` → 404 propagado |

---

### 11. Falta de 503 controlado cuando dependencia down

| | |
|---|---|
| **Guía** | No contempla `ConnectException`, `ReadTimeout`, `CircuitBreaker open` |
| **Fallo** | Excepción cruda → 500 o timeout indefinido |
| **Solución** | `Retry` + `CircuitBreaker` (Resilience4j) + wrapper que lanza `ServiceUnavailableException` → handler global → 503 JSON |

```java
@Bean
public RestClient categoriaRestClient(RestClient.Builder builder,
                                      @Value("${clients.categoria-service.url}") String baseUrl) {
    return builder
        .baseUrl(baseUrl)
        .defaultHeader(MediaType.APPLICATION_JSON_VALUE)
        .requestInterceptor((req, body, exec) -> {
            // Resilience4j decorators aplicados aquí
        })
        .build();
}
```

---

## 14.3 Problemas en clínica (Parte 3)

### 12. Tablas clínica — columnas/tipos/constraints incompletos

| Tabla | Guía | Fix aplicado |
|---|---|---|
| `paciente` | `dni` sin `UNIQUE`, `fecha_nacimiento` sin `NOT NULL` | `dni VARCHAR(8) UNIQUE NOT NULL`, `fecha_nacimiento DATE NOT NULL` |
| `especialidad` | `estado` sin default | `estado CHAR(1) DEFAULT 'A'` |
| `medico` | `cmp` sin `UNIQUE`, `id_especialidad` sin FK | `cmp VARCHAR(20) UNIQUE NOT NULL`, `FK medico→especialidad` |
| `horario_medico` | Sin FK a `medico` | `FK horario_medico→medico` |
| `cita` | Sin `DEFAULT 'PROGRAMADA'`, sin validación estado | `estado VARCHAR(20) DEFAULT 'PROGRAMADA'`, validación en DTO `@Pattern(PROGRAMADA\|CONFIRMADA\|ATENDIDA\|CANCELADA)` |
| `atencion` | Sin `UNIQUE(id_cita)`, sin FK | `id_cita INT UNIQUE`, `FK atencion→cita` |

> [!note] **No hay FK cruzadas** entre servicios (paciente/medico son IDs planos validados por HTTP).

---

### 13. Transiciones de estado de Cita — faltantes en guía

| | |
|---|---|
| **Guía** | Solo menciona "cambiar estado" |
| **Fallo** | Permite transiciones inválidas (ej. `ATENDIDA → CANCELADA`) |
| **Solución** | Máquina de estados explícita en `CitaService.cambiarEstado()` |

```java
private static final Map<String, Set<String>> PERMITIDAS = Map.of(
    "PROGRAMADA", Set.of("CONFIRMADA", "CANCELADA"),
    "CONFIRMADA", Set.of("ATENDIDA", "CANCELADA"),
    "ATENDIDA", Set.of(),
    "CANCELADA", Set.of()
);

public Cita cambiarEstado(Integer id, EstadoCitaDTO dto) {
    Cita cita = obtener(id);
    if (!PERMITIDAS.get(cita.getEstado()).contains(dto.getEstado())) {
        throw new ConflictException("Transición no permitida: " + cita.getEstado() + " -> " + dto.getEstado());
    }
    cita.setEstado(dto.getEstado());
    if ("ATENDIDA".equals(dto.getEstado())) {
        Atencion a = new Atencion();
        a.setCita(cita);
        a.setDiagnostico(dto.getDiagnostico());
        a.setTratamiento(dto.getTratamiento());
        a.setObservaciones(dto.getObservaciones());
        atencionRepo.save(a);
    }
    return repo.save(cita);
}
```

---

### 14. Atención 1:1 — endpoint y constraint

| | |
|---|---|
| **Guía** | No define endpoint para obtener atención de una cita |
| **Fallo** | Atención inaccesible tras crearse; no hay `UNIQUE(id_cita)` |
| **Solución** | `Atencion.idCita` `@OneToOne` + `unique=true`; endpoint `GET /api/citas/{id}/atencion` |

```java
@GetMapping("/{id}/atencion")
public ResponseEntity<Atencion> getAtencion(@PathVariable Integer id) {
    return ResponseEntity.ok(atencionService.obtenerPorCita(id));
}
```

---

### 15. Cancelar cita = cambio de estado (no DELETE)

| | |
|---|---|
| **Guía** | Sugiere `DELETE /api/citas/{id}` |
| **Fallo** | Borra historial; no permite auditoría |
| **Solución** | `PUT /api/citas/{id}/cancelar` → delega a `cambiarEstado(CANCELADA)` |

```java
@PutMapping("/{id}/cancelar")
public ResponseEntity<Cita> cancelar(@PathVariable Integer id) {
    return ResponseEntity.ok(service.cambiarEstado(id, new EstadoCitaDTO("CANCELADA")));
}
```

---

## 14.4 Resumen de correcciones por archivo

| Archivo | Problemas corregidos |
|---|---|
| `Categoria.java` / `Producto.java` / `Cliente.java` / `Pedido.java` / `DetallePedido.java` | 1, 3, 4, 5, 6, 7 |
| `application.yml` / `application-dev.yml` / `application-prod.yml` | 2, 9 |
| `GlobalExceptionHandler.java` | 3, 4, 10, 11 |
| `CategoriaClient.java` / `ClienteClient.java` / `PacienteClient.java` / `MedicoClient.java` | 9, 10, 11 |
| `CategoriaService.java` / `ProductoService.java` / `PedidoService.java` / `ClienteService.java` / `PacienteService.java` | 3, 4, 5, 6, 7, 8 |
| `sql/schema-clinica.sql` | 12 |
| `Cita.java` / `Atencion.java` / `CitaService.java` / `CitaController.java` | 13, 14, 15 |
| `EstadoCitaDTO.java` | 13 |

---

## 14.5 Decisiones que **no** son correcciones a la guía (mejoras propias)

| Mejora | Descripción |
|---|---|
| Paquete `client` dedicado | Separación de responsabilidades; testable con `@MockBean` |
| `MethodArgumentTypeMismatchException` → 400 | Path `abc` en `/api/clientes/abc` devuelve 400 limpio |
| `DataIntegrityViolationException` → 409 | Duplicados de BD (únicos) mapeados a conflicto semántico |
| `NoResourceFoundException` → 404 | Rutas inexistentes (`/api/foo`) devuelven 404 JSON |
| Perfil `prod` con `servidor` inexistente | Demuestra externalización de config sin infra real |
| `BigDecimal` para dinero | Precisión monetaria correcta |

---

> [!check] **Todos los defectos de la guía identificados y corregidos** — el laboratorio cumple requisitos funcionales y no funcionales exigidos.

## Inconsistencias y trampas de la guía S12

| # | Problema en la guía | Corrección aplicada |
|---|---|---|
| 1 | Postman usa `/api/categorías` (con tilde); Feign usa `/api/categorias` | Ruta **sin tilde** en código, pruebas y colección |
| 2 | `ProductoService.guardar` guarda y luego consulta la categoría | Validar por Feign **antes** de guardar |
| 3 | `listar()` de producto no completa la categoría | Se completa; si falla, `categoria: null` sin romper el listado |
| 4 | No hay handlers específicos de Feign | `FeignException.NotFound` → 404; `FeignException` → 503; `Exception` → 500 |
| 5 | `apellido` en el JSON del pedido, `apellidos` en cliente, `nro_dni` en la guía | `apellidos` + `nroDni` en cliente; alias `apellidos` → `apellido` en `ClienteDTO` |
| 6 | Pedido de S11 con `fecha`, `total` y detalles | Refactor a `clienteId`, `productoId`, `cantidad`, `estado` |
| 7 | La guía no define transiciones terminales con claridad | `PENDIENTE → ATENDIDO/ANULADO`; ATENDIDO/ANULADO → 409 |
| 8 | Poms de Boot 3.5 + Cloud 2025.0.0 | Boot 4.1.2 + Cloud 2025.1.4; starters de Boot 4 |
| 9 | Se asume propagación inmediata de Eureka | Arranque secuencial + `disable-delta` + fetch de 5 s |
| 10 | No se prueba UTF-8 de punta a punta | MySQL utf8mb4 + `--data-binary @file.json` + verificación con "Tecnología" |

> [!note] Sobre el paso 3 de la guía
> Importar el módulo `eureka-server` en IntelliJ se omitió por indicación del usuario; se hará manualmente.

Enlaces: [[17 Decisión de versiones Boot 4 + Spring Cloud 2025.1.x]] · [[11 Errores y soluciones]]


## Inconsistencias y trampas de la guía S13

| # | Problema en la guía | Corrección aplicada |
|---|---|---|
| 1 | El paso 3 habla de la carpeta `gateway-server`, pero el proyecto se llama `gateway-service` | Se usó el nombre real del proyecto |
| 2 | Hay **dos pasos numerados 6** | Se renumeró la bitácora (Pasos 5 y 6 reales) |
| 3 | Starter `spring-cloud-starter-gateway` (Gateway 4) | `spring-cloud-starter-gateway-server-webflux` (Gateway 5) |
| 4 | Prefijo `spring.cloud.gateway.*` | `spring.cloud.gateway.server.webflux.*` |
| 5 | Asume Spring Boot 3.5.x + Spring Cloud 2025.0.0 + Java 17 | Boot 4.1.2 + Cloud 2025.1.4 + Java 25, como el resto del proyecto |
| 6 | El paso “Revisar Eureka-Server” lista URLs del gateway | Se hicieron **las dos**: captura de `GATEWAY-SERVICE` en Eureka y pruebas por 8080 |
| 7 | No advierte que el discovery locator expone **todos** los servicios registrados | Se dejó activo como pide la guía y se documentó el riesgo (ver [[23 Discovery locator vs rutas explícitas]]) |
| 8 | No menciona que Eureka tarda en propagar y que aparecen 503 iniciales | Se midieron tiempos (70–95 s para el 503; 10–31 s para recuperar) y se ajustaron fetch/leases |
| 9 | No aclara que el acceso directo a 8081–8084 sigue funcionando | Se documentó como limitación y como mejora (exponer solo el gateway) |
| 10 | `/actuator/gateway/routes` se presenta como opcional “si funciona con Boot 4” | Se habilitó con `management.endpoint.gateway.access: READ_ONLY` y se documentó el cambio de modelo de acceso |
| 11 | El pom de la guía no incluye `spring-cloud-starter-loadbalancer` | Se agregó explícitamente para `lb://` |
| 12 | No indica el paquete base correcto para el gateway | Se movió a `com.tecsup` para evitar dos clases main |
| 13 | No menciona que no se debe agregar `spring-boot-starter-web` al gateway | Se dejó solo WebFlux/Netty |

> [!note] Sobre el paso 3 de la guía
> Importar el módulo en IntelliJ se omitió por indicación del usuario; lo hará manualmente.

### Diferencias que no son errores de la guía

| Diferencia | Motivo |
|---|---|
| Rutas explícitas a los 4 servicios del catálogo | La Parte 2 las pide; la guía solo muestra 2 |
| `cliente-service` en 8081 y `categoria-service` en 8082 | Orden real de S11/S12, distinto al ejemplo de la guía |
| Servicios de clínica (8085–8087) también con ruta explícita | Son opcionales; se publicaron igual que el catálogo |
| `GET /api/pedidos/{id}` devuelve `estado` | Contrato de S12, no de S13 |

Enlaces: [[25 Spring Cloud Gateway 5 (cambios y versiones)]] · [[11 Errores y soluciones]]
