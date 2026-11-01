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
