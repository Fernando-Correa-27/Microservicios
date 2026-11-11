---
tags:
  - glab-s13
  - versiones
  - spring-cloud-gateway
  - boot4
  - decisiones
fecha: 2026-11-11
estado: completado
---

# 25 · Spring Cloud Gateway 5 (cambios y versiones)

> [!abstract] Resumen
> La guía S13 asume **Spring Boot 3.5.x + Spring Cloud 2025.0.0 + Java 17** y un gateway con `spring-cloud-starter-gateway`. Este proyecto usa **Spring Boot 4.1.2 + Spring Cloud 2025.1.4 + Java 25**, lo que implica **Spring Cloud Gateway 5.0.4**. Gateway 5 cambió el nombre del starter, el prefijo de configuración y el modelo de acceso del endpoint de actuator.

## Versiones usadas

| Componente | Guía S13 | Proyecto | Motivo |
|---|---|---|---|
| Spring Boot | 3.5.x | **4.1.2** | Decisión tomada en S11/S12, no se cambia |
| Spring Cloud | 2025.0.0 | **2025.1.4** | Tren compatible con Boot 4 |
| Spring Cloud Gateway | 4.x | **5.0.4** | Versión que arrastra el BOM 2025.1.4 |
| Java | 17 | **25** | Igual que los otros 7 proyectos |
| Starter del gateway | `spring-cloud-starter-gateway` | **`spring-cloud-starter-gateway-server-webflux`** | Renombrado en Gateway 5 |
| Prefijo de config | `spring.cloud.gateway.*` | **`spring.cloud.gateway.server.webflux.*`** | Reorganización en Gateway 5 |

## Cambio 1 · Nombre del starter

```xml
<!-- Guía (Gateway 4 / Boot 3) -->
<artifactId>spring-cloud-starter-gateway</artifactId>

<!-- Proyecto (Gateway 5 / Boot 4) -->
<artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
```

> [!danger] Si usas el nombre viejo
> El arranque falla con clases no encontradas (`ClassNotFoundException` / `NoClassDefFoundError`) porque el artefacto ya no existe con ese nombre en el tren 2025.1.x.

Además:

- Se usa la variante **WebFlux** (reactiva, Netty). La variante MVC (`spring-cloud-starter-gateway-server-webmvc`) es otra opción, pero la guía y el ecosistema apuntan a WebFlux.
- **No** se agrega `spring-boot-starter-web`: mezclar MVC y WebFlux en el mismo proceso impide el arranque del servidor reactivo.

## Cambio 2 · Prefijo de configuración

```yaml
# Guía (ya no funciona en Gateway 5)
spring:
  cloud:
    gateway:
      routes:
        - id: categoria-service
          uri: lb://categoria-service
          predicates:
            - Path=/api/categorias/**
      discovery:
        locator:
          enabled: true

# Proyecto (Gateway 5)
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: categoria-service
              uri: lb://categoria-service
              predicates:
                - Path=/api/categorias/**
          discovery:
            locator:
              enabled: true
```

> [!warning] Síntoma típico del prefijo viejo
> El gateway arranca **sin errores**, pero **todas** las peticiones devuelven **404**. No hay ninguna advertencia: simplemente no hay rutas cargadas. Es el error más costoso de diagnosticar de todo el laboratorio.

Los niveles de logging **no cambiaron**:

```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.cloud.loadbalancer: DEBUG
```

## Cambio 3 · Endpoint de actuator y modelo de acceso

Gateway 5 declara el endpoint así:

```java
@RestControllerEndpoint(id = "gateway", defaultAccess = Access.NONE)
```

Es decir, **existe pero está deshabilitado por defecto**. En Boot 4 el modelo de acceso (`org.springframework.boot.actuate.endpoint.Access`) tiene tres valores: `NONE`, `READ_ONLY`, `UNRESTRICTED`.

```yaml
management:
  endpoint:
    gateway:
      access: READ_ONLY   # habilita GET /actuator/gateway/routes
  endpoints:
    web:
      exposure:
        include: gateway,health,info
```

Errores reales durante la implementación:

| Intento | Resultado |
|---|---|
| Solo `exposure.include: gateway` | `/actuator` no lista `gateway`; `/actuator/gateway/routes` → 404 |
| `access: read` | **Falla el arranque**: `Failed to convert to type Access` |
| `access: READ_ONLY` | ✅ `/actuator/gateway/routes` → 200 con las 15 rutas |

> [!note] ¿Por qué `READ_ONLY` y no `UNRESTRICTED`?
> `READ_ONLY` permite consultar rutas pero bloquea `POST`/`DELETE` sobre `/actuator/gateway/routes`, que permitirían modificar rutas en caliente. Para un laboratorio de solo lectura es la opción correcta.

## Cambio 4 · Paquete base y clase main

Spring Initializr generó `com.tecsup.gateway_service.GatewayServiceApplication`. Se movió a **`com.tecsup`** para ser consistente con los otros 7 proyectos y evitar el error de “dos clases main” que ya apareció en S11.

```text
gateway-service/src/main/java/com/tecsup/GatewayServiceApplication.java
```

## Cambio 5 · Starter de test

El BOM no trae `spring-boot-starter-webflux-test` en el repositorio local `.m2`, así que se mantuvo `spring-boot-starter-test`, igual que en los otros proyectos.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Resumen de la migración

| Elemento | Antes (Initializr / guía) | Después (S13) |
|---|---|---|
| Parent Boot | 4.1.2 | 4.1.2 (sin cambio) |
| Spring Cloud | — (sin BOM) | 2025.1.4 |
| Java | 25 | 25 |
| Starter gateway | `gateway-server-webmvc` (generado) | `gateway-server-webflux` |
| Eureka client | — | `spring-cloud-starter-netflix-eureka-client` |
| LoadBalancer | — | `spring-cloud-starter-loadbalancer` |
| Actuator | — | `spring-boot-starter-actuator` |
| Paquete | `com.tecsup.gateway_service` | `com.tecsup` |
| Prefijo YAML | `spring.cloud.gateway.*` | `spring.cloud.gateway.server.webflux.*` |

## Enlaces

- [[24 gateway-service]]
- [[17 Decisión de versiones Boot 4 + Spring Cloud 2025.1.x]]
- [[11 Errores y soluciones]]
