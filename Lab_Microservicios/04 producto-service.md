---
tags:
  - glab-s11
  - servicio
  - producto
  - restclient
fecha: 2026-11-01
estado: completado
---

# 04 · producto-service

> [!info] Ficha
> **Puerto:** 8083 · **BD:** `bd_producto` (dev: `bd_producto_dev`) · **Perfil prod:** 9094

## Responsabilidad

Gestionar productos y **validar que la categoría exista** en `categoria-service` antes de guardar.

## Modelo

| Campo | Tipo | Validación |
|---|---|---|
| `id` | Long | autogenerado |
| `nombre` | String(150) | `@NotBlank`, `@Size` |
| `precio` | BigDecimal(10,2) | `@NotNull`, `@DecimalMin(0.0)` |
| `stock` | Integer | `@NotNull`, `@Min(0)` |
| `categoriaId` | Long | `@NotNull` + validación HTTP |

> [!note] BigDecimal
> Se usa `BigDecimal` en lugar de `Double` para dinero. Evita errores de redondeo en precios y totales.

## Comunicación con categoria-service

```java
@Component
public class CategoriaClient {
    private final RestClient restClient;

    public CategoriaClient(@Value("${servicios.categoria.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validarCategoria(Long categoriaId) {
        try {
            restClient.get().uri("/api/categorias/{id}", categoriaId)
                      .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404)
                throw new ResourceNotFoundException("Categoría no encontrada con id: " + categoriaId);
            throw new ServiceUnavailableException("categoria-service respondió con error " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("categoria-service no está disponible...");
        }
    }
}
```

Configuración:

```yaml
servicios:
  categoria:
    url: http://localhost:8082
```

## Endpoints

| Método | Ruta | Éxito | Errores |
|---|---|---|---|
| GET | `/api/productos` | 200 | 500 |
| GET | `/api/productos/{id}` | 200 | 404 |
| POST | `/api/productos` | 201 | 400, 404, 503 |
| PUT | `/api/productos/{id}` | 200 | 400, 404, 503 |
| DELETE | `/api/productos/{id}` | 200 | 404 |

## Resultados reales

| Caso | Status |
|---|---|
| Crear con `categoriaId=1` | 201 |
| Crear con `categoriaId=999` | 404 `Categoría no encontrada con id: 999` |
| Crear con categoria-service detenido | 503 `categoria-service no está disponible...` |

> [!tip] Por qué 503 y no 500
> Una dependencia caída no es un bug del servicio: es un fallo temporal de disponibilidad. 503 Service Unavailable comunica mejor la condición y permite reintentar.

## Enlaces

- [[03 categoria-service]]
- [[06 pedido-service]]
- [[12 Pruebas HTTP]]

## S12 · producto-service con OpenFeign hacia categoría

### Cambios

- `pom.xml`: BOM de Spring Cloud 2025.1.4 + `spring-cloud-starter-netflix-eureka-client` + `spring-cloud-starter-openfeign`.
- `ProductoServiceApplication`: `@EnableDiscoveryClient` y `@EnableFeignClients`.
- Nuevo `dto/CategoriaDTO` (`id`, `nombre`, `estado`, `@JsonIgnoreProperties`).
- `Producto`: nuevo campo `categoriaId` (columna `categoria_id`) y `@Transient CategoriaDTO categoria`.
- Nuevo `client/CategoriaClient`:

```java
@FeignClient(name = "categoria-service")
public interface CategoriaClient {
    @GetMapping("/api/categorias/{id}")
    CategoriaDTO obtenerCategoria(@PathVariable("id") Long id);
}
```

- `ProductoService`:
  - `guardar` y `actualizar` **validan la categoría antes de guardar** (corrige el bug de la guía).
  - `obtener` completa la categoría.
  - `listar` completa la categoría de cada producto y tolera fallos dejándola en `null`.
- `ProductoDTO.precio`: `@DecimalMin("0.01")` para exigir precio **mayor a cero**.

### Prueba real

```text
POST /api/productos  {"nombre":"Laptop","precio":2500,"stock":10,"categoriaId":9}
201 {"categoria":{"id":9,"nombre":"Tecnología","estado":"ACTIVO"},"categoriaId":9,"id":9,...}

GET /api/productos
200 [ ... todos con "categoria" anidada ... ]
```

Errores verificados:

- `categoriaId` inexistente → **404** `"Recurso no encontrado en el servicio remoto"` (y **no** se guarda el producto).
- `categoria-service` caído → **503** JSON; el listado sigue en **200** con `categoria: null`.
- `precio` 0 o negativo → **400**.

Enlaces: [[19 OpenFeign]] · [[12 Pruebas HTTP]]

## S13 · producto-service publicado en el API Gateway

| Aspecto | Valor |
|---|---|
| Ruta explícita | `Path=/api/productos/**` |
| Destino | `lb://producto-service` |
| URL pública | `http://localhost:8080/api/productos` |
| Ruta del discovery locator | `http://localhost:8080/producto-service/api/productos` |

- El gateway no reemplaza a Feign: `GET /api/productos` sigue completando la `categoria` anidada porque producto llama a categoria-service.
- CRUD completo verificado por el gateway: 200/201/200/200.
- `POST` inválido (`precio` 0, `stock` -1) → **400** con los mensajes del servicio.
- UTF-8: `{"nombre":"Cámara Web Ñandú"}` creado por el gateway se lee con `ñ` y `ú` correctas.
- Servicio detenido: `/api/productos` por el gateway → **503** controlado; categorías, clientes y pedidos siguen en 200. Al reiniciar, se recupera en segundos.

> [!important] Caso de estudio de resiliencia
> Es la ruta usada para demostrar el 503 del gateway: `RoundRobinLoadBalancer` registra `No servers available for service: producto-service` y el gateway responde JSON, no un stack trace.

Enlaces: [[24 gateway-service]] · [[12 Pruebas HTTP]]
