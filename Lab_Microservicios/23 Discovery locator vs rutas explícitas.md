---
tags:
  - glab-s13
  - api-gateway
  - discovery-locator
  - decisiones
fecha: 2026-11-11
estado: completado
---

# 23 · Discovery locator vs rutas explícitas

> [!abstract] Resumen
> El **discovery locator** genera automáticamente una ruta por cada servicio registrado en Eureka. Las **rutas explícitas** se escriben a mano en YAML y publican solo lo que se decide exponer. S13 usa **las dos** porque la guía lo pide, pero en producción se recomienda quedarse con las explícitas.

## Configuración del discovery locator

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true
```

| Propiedad | Efecto |
|---|---|
| `enabled: true` | Crea una ruta `/{service-id}/**` por cada app de Eureka |
| `lower-case-service-id: true` | Usa `categoria-service` en la URL, no `CATEGORIA-SERVICE` |

Con esto, además de `/api/categorias`, también funciona:

```text
GET http://localhost:8080/categoria-service/api/categorias
GET http://localhost:8080/producto-service/api/productos
```

## Comparación

| Criterio | Discovery locator | Rutas explícitas |
|---|---|---|
| **Esfuerzo** | Cero: aparece sola al registrarse el servicio | Hay que declarar cada ruta |
| **Velocidad de alta** | Inmediata para servicios nuevos | Requiere editar y reiniciar el gateway |
| **Control de lo expuesto** | Expone **todo** lo registrado, incluido el propio gateway | Expone solo lo declarado |
| **Contrato público** | Inestable: depende del nombre interno del servicio | Estable: `/api/...` es una decisión de API |
| **Seguridad** | Superficie de ataque mayor | Permite publicar solo endpoints revisados |
| **Legibilidad** | Rutas “mágicas” | El YAML documenta la intención |
| **Uso recomendado** | Desarrollo, demos, prototipos | Producción |

## Lo que se observó en el laboratorio

1. Con `enabled: true` aparecieron **5 rutas automáticas**: una por cada servicio registrado (`categoria`, `producto`, `cliente`, `pedido`) **más la del propio `gateway-service`**, que también se registra en Eureka.
2. El listado de `/actuator/gateway/routes` mostró **9 rutas**: 4 explícitas + 5 automáticas.
3. Las rutas automáticas **no reescriben** el prefijo: `/categoria-service/api/categorias` se reenvía tal cual al servicio, y por eso incluye `/api/categorias` después del nombre del servicio.
4. Ambas formas conviven sin conflicto porque los predicados son distintos (`/api/...` vs `/{service-id}/...`).

```text
ReactiveCompositeDiscoveryClient_CATEGORIA-SERVICE  ->  /categoria-service/**   -> lb://CATEGORIA-SERVICE
categoria-service                                  ->  /api/categorias/**      -> lb://categoria-service
```

## Riesgos del discovery locator en producción

> [!warning] Expone todo servicio registrado
> Cualquier servicio que se registre en Eureka queda accesible desde el gateway, aunque no estuviera pensado para el exterior. Eso incluye servicios internos, de soporte o el propio gateway.

> [!warning] Contrato acoplado al nombre interno
> Si el `spring.application.name` cambia, la URL pública cambia. Eso rompe consumidores sin que exista un cambio funcional.

> [!warning] Sin control de versiones de API
> Las rutas explícitas permiten publicar `/api/v1/...` y `/api/v2/...` de forma controlada. El locator publica el path interno tal cual.

## Decisión en S13

- Se dejó `enabled: true` **porque la guía lo exige** y sirve para demostrar el concepto.
- Se añadieron rutas explícitas para las 4 APIs reales, que son las que usa Postman.
- Se documenta que en producción se debería poner `enabled: false` y publicar solo rutas explícitas.

```yaml
# Producción (recomendado)
discovery:
  locator:
    enabled: false
```

## Enlaces

- [[22 Rutas del gateway (predicados, lb y balanceo)]]
- [[24 gateway-service]]
- [[21 API Gateway]]
