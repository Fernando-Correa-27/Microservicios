---
tags:
  - glab-s12
  - openfeign
  - http
  - microservicios
fecha: 2026-11-05
estado: completado
---

# 19 · OpenFeign

> [!quote] Idea central
> OpenFeign convierte una interfaz Java en un cliente HTTP declarativo. Se escribe **qué** endpoint se quiere llamar y Spring Cloud resuelve **dónde** está la instancia usando Eureka + LoadBalancer.

## Dependencia y activación

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PedidoServiceApplication { ... }
```

Solo `producto-service` y `pedido-service` llevan `@EnableFeignClients` y la dependencia.

## Diferencia con RestClient / RestTemplate

| Aspecto | RestClient / RestTemplate (S11) | OpenFeign (S12) |
|---|---|---|
| Cómo se define | Código imperativo: `restClient.get().uri(...)` | Interfaz con anotaciones (`@GetMapping`) |
| URL | Fija en `application.yml` (`servicios.categoria.url`) | Nombre lógico (`@FeignClient(name="categoria-service")`) |
| Descubrimiento | Manual | Integrado con Eureka + Spring Cloud LoadBalancer |
| Serialización | Manual / convertidores | Automática (JSON ↔ DTO) |
| Errores | `RestClientResponseException` | `FeignException` y subclases (`NotFound`, …) |
| Balanceo | No incluido | Incluido (varias instancias) |

> [!important] Sin URLs fijas
> En S12 se eliminaron las propiedades `servicios.*.url` de producto y pedido. La única referencia remota es el **nombre del servicio registrado** en Eureka.

## Clientes creados

`producto-service`:

```java
@FeignClient(name = "categoria-service")
public interface CategoriaClient {
    @GetMapping("/api/categorias/{id}")
    CategoriaDTO obtenerCategoria(@PathVariable("id") Long id);
}
```

`pedido-service`:

```java
@FeignClient(name = "cliente-service")
public interface ClienteClient {
    @GetMapping("/api/clientes/{id}")
    ClienteDTO obtenerCliente(@PathVariable("id") Long id);
}

@FeignClient(name = "producto-service")
public interface ProductoClient {
    @GetMapping("/api/productos/{id}")
    ProductoDTO obtenerProducto(@PathVariable("id") Long id);
}
```

> [!tip] `@PathVariable("id")` explícito
> Sin el nombre explícito, algunos compiladores/versiones de Spring pueden no conservar los nombres de parámetros. La guía lo pide así y se respetó.

## DTOs tolerantes

Los DTO remotos ignoran campos desconocidos para no romperse si el otro servicio agrega datos:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
}
```

`ClienteDTO` además acepta el nombre `apellidos` de cliente-service y lo expone como `apellido` en la respuesta del pedido:

```java
@JsonAlias("apellidos")
private String apellido;
```

Así el JSON de salida coincide con el ejemplo de la guía (`"apellido":"Ramirez"`) sin cambiar el contrato de cliente-service (`apellidos`).

## Manejo de errores

En `GlobalExceptionHandler` de producto y pedido:

```java
@ExceptionHandler(FeignException.NotFound.class)
public ResponseEntity<?> noEncontrado(FeignException.NotFound ex) {
    return respuesta(HttpStatus.NOT_FOUND, "Recurso no encontrado en el servicio remoto");
}

@ExceptionHandler(FeignException.class)
public ResponseEntity<?> dependenciaCaida(FeignException ex) {
    return respuesta(HttpStatus.SERVICE_UNAVAILABLE,
        "Un servicio dependiente no está disponible o respondió con error");
}

@ExceptionHandler(Exception.class)
public ResponseEntity<?> generico(Exception ex) {
    return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
}
```

Resultados reales:

- Categoría/cliente/producto inexistente → **404** con mensaje claro.
- Servicio detenido o sin instancia → **503** JSON, sin stack trace.
- Cualquier error no previsto → **500** JSON.

> [!warning] Validar antes de guardar
> La guía guardaba el producto y **después** consultaba la categoría. Si la categoría no existía, quedaba un producto huérfano y se devolvía 500. En S12 se consulta por Feign **antes** de `save()`, tanto en producto como en pedido.

## Listados tolerantes

`ProductoService.listar()` completa la categoría de cada producto y, si una falla, la deja en `null`:

```java
try {
    producto.setCategoria(categoriaClient.obtenerCategoria(producto.getCategoriaId()));
} catch (RuntimeException ex) {
    producto.setCategoria(null);
}
```

`PedidoService.listar()` hace lo mismo con cliente y producto. Así el listado responde **200** aunque una dependencia esté caída; solo el detalle individual devuelve 503.

## Enlaces

- [[18 Eureka y service discovery]]
- [[20 Comunicación entre servicios (flujo GET pedido)]]
- [[11 Errores y soluciones]]
