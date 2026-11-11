---
tags:
  - glab-s11
  - servicio
  - categoria
fecha: 2026-11-01
estado: completado
---

# 03 · categoria-service

> [!info] Ficha
> **Puerto:** 8082 · **BD:** `bd_categoria` (dev: `bd_categoria_dev`) · **Paquete:** `com.tecsup` · **Perfil prod:** 9092

## Responsabilidad

Gestionar el catálogo de categorías. Es el servicio base del laboratorio y el primero que se implementó. `producto-service` lo consulta por HTTP.

## Entidades y DTO

| Clase | Campos | Validaciones |
|---|---|---|
| `Categoria` | `id`, `nombre`, `estado` | — |
| `CategoriaDTO` | `nombre`, `estado` | `@NotBlank`, `@Size(max=100/20)` |

## Endpoints

| Método | Ruta | Éxito | Errores |
|---|---|---|---|
| GET | `/api/categorias` | 200 | 500 |
| GET | `/api/categorias/{id}` | 200 | 404, 500 |
| POST | `/api/categorias` | 201 | 400, 500 |
| PUT | `/api/categorias/{id}` | 200 | 400, 404, 500 |
| DELETE | `/api/categorias/{id}` | 200 | 404, 500 |

## Ejemplo

```powershell
curl.exe -s -X POST http://localhost:8082/api/categorias `
  -H "Content-Type: application/json" `
  --data-binary "@postman/bodies/categoria-1.json"
# 201 {"id":1,"nombre":"Laptops","estado":"ACTIVO"}
```

## Decisiones

- Se eliminaron los chequeos `if (c == null)` del controller original: el service lanza `ResourceNotFoundException` y el handler responde 404.
- El controller mapea DTO → entidad; el service concentra persistencia y reglas.
- Se añadió `spring-boot-starter-validation` (la guía lo pide).
- `@Transactional` en lectura y escritura.

> [!success] Prueba real
> POST 201, GET 200, GET 999 404, POST vacío 400. Ver [[12 Pruebas HTTP]].

## Enlaces

- [[02 Arquitectura de microservicios]]
- [[04 producto-service]] (consumidor)
- [[14 Guía original y problemas detectados]]

## S12 · categoria-service como cliente de Eureka

- Se agregó `spring-cloud-starter-netflix-eureka-client` y `@EnableDiscoveryClient`.
- Se agregó la sección `eureka:` en `application-dev.yml` con `defaultZone: http://localhost:8761/eureka`.
- **No cambió** el CRUD ni el puerto 8082. Sigue siendo el proveedor de categorías para producto-service.

> [!warning] Ruta sin tilde
> La colección Postman de la guía usaba `/api/categorías` (con tilde), pero el `@FeignClient` usa `/api/categorias`. Se estandarizó **sin tilde** en código, pruebas y colección.

Verificación real: `GET http://localhost:8761/eureka/apps` mostró `CATEGORIA-SERVICE -> UP`. El nombre con tilde `"Tecnología"` se guardó y recuperó correctamente en UTF-8.

Enlaces: [[18 Eureka y service discovery]] · [[12 Pruebas HTTP]]

## S13 · categoria-service publicado en el API Gateway

| Aspecto | Valor |
|---|---|
| Ruta explícita | `Path=/api/categorias/**` |
| Destino | `lb://categoria-service` |
| URL pública | `http://localhost:8080/api/categorias` |
| Ruta del discovery locator | `http://localhost:8080/categoria-service/api/categorias` |

- No se tocó código Java del servicio: el gateway solo enruta.
- El servicio sigue escuchando en **8082** y sigue registrado como `CATEGORIA-SERVICE -> UP`.
- Verificado por el gateway: `GET` lista y por id, `POST` 201, `PUT` 200, `DELETE` 200, `POST` inválido 400 y `GET /999999` 404 con el JSON del servicio intacto.
- UTF-8: `POST {"nombre":"Tecnología"}` por el gateway devuelve 201 y la tilde se conserva al leerlo.

> [!tip] Doble camino
> `GET /api/productos` por el gateway termina mostrando la `categoria` anidada porque **producto-service** llama a **categoria-service** por Feign. Es decir, el gateway solo interviene en la primera llamada; el resto es tráfico interno.

Enlaces: [[24 gateway-service]] · [[22 Rutas del gateway (predicados, lb y balanceo)]]
