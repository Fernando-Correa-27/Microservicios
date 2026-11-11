---
tags:
  - glab-s13
  - servicio
  - gateway
  - spring-cloud-gateway
fecha: 2026-11-11
estado: completado
---

# 24 · gateway-service

> [!info] Ficha
> **Puerto:** 8080 · **Sin base de datos** · **Rol:** API Gateway reactivo (Netty) · **Proyecto:** `gateway-service` · **Paquete:** `com.tecsup`

## Responsabilidad

Ser el **único punto de entrada** HTTP del sistema y enrutar cada prefijo `/api/...` al microservicio correspondiente usando `lb://` (Eureka + LoadBalancer). No tiene lógica de negocio ni persistencia.

## Estructura

```text
gateway-service/
├── pom.xml
└── src/main/
    ├── java/com/tecsup/GatewayServiceApplication.java
    └── resources/application.yaml
```

```java
package com.tecsup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
```

> [!note] Sin `spring-boot-starter-web`
> El gateway WebFlux corre sobre **Netty**. Agregar el starter MVC rompe el arranque porque conviven dos stacks web incompatibles.

## `pom.xml` (resumen)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.2</version>
</parent>

<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2025.1.4</spring-cloud.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## `application.yaml`

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service

  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true

          routes:
            - id: categoria-service
              uri: lb://categoria-service
              predicates:
                - Path=/api/categorias/**

            - id: producto-service
              uri: lb://producto-service
              predicates:
                - Path=/api/productos/**

            - id: cliente-service
              uri: lb://cliente-service
              predicates:
                - Path=/api/clientes/**

            - id: pedido-service
              uri: lb://pedido-service
              predicates:
                - Path=/api/pedidos/**

logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.cloud.loadbalancer: DEBUG

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka
    registry-fetch-interval-seconds: 5
    disable-delta: true
  instance:
    hostname: localhost
    prefer-ip-address: false
    lease-renewal-interval-in-seconds: 5
    lease-expiration-duration-in-seconds: 10

management:
  endpoint:
    gateway:
      access: READ_ONLY
  endpoints:
    web:
      exposure:
        include: gateway,health,info
```

> [!important] Prefijo de Gateway 5
> Todo cuelga de `spring.cloud.gateway.server.webflux.*`. El prefijo antiguo `spring.cloud.gateway.routes` **no se lee** y todas las peticiones responden 404 sin error visible. Ver [[25 Spring Cloud Gateway 5 (cambios y versiones)]].

> [!tip] Por qué `registry-fetch-interval-seconds: 5` y leases cortos
> Acelera la propagación de registro en desarrollo. En producción se usan los valores por defecto (30 s / 30 s / 90 s) para no generar tráfico innecesario a Eureka.

## Endpoints que expone

| Ruta pública | Destino | Servicio real |
|---|---|---|
| `/api/categorias/**` | `lb://categoria-service` | 8082 |
| `/api/productos/**` | `lb://producto-service` | 8083 |
| `/api/clientes/**` | `lb://cliente-service` | 8081 |
| `/api/pedidos/**` | `lb://pedido-service` | 8084 |
| `/{service-id}/**` | `lb://{SERVICE-ID}` | discovery locator |
| `/actuator/health` | — | health del gateway |
| `/actuator/gateway/routes` | — | rutas activas |

> [!warning] El gateway no implementa endpoints de negocio
> No hay controllers propios. Si una ruta no coincide con ningún predicado, responde **404** con el JSON de error del gateway.

## Registro en Eureka

- `spring.application.name: gateway-service` → aparece como `GATEWAY-SERVICE`.
- `register-with-eureka: true`: el gateway también se registra (por eso el discovery locator crea `/gateway-service/**`).
- `fetch-registry: true`: necesita el registro para resolver los `lb://`.

```text
GATEWAY-SERVICE -> UP
```

## Pruebas reales (resumen)

| Caso | Resultado |
|---|---|
| `GET /api/categorias` | 200 |
| `GET /api/productos` (con `categoria` anidada) | 200 |
| `GET /api/pedidos/1` (cliente + producto por Feign) | 200 |
| `POST /api/...` en las 4 rutas | 201 |
| `PUT`/`DELETE` en categoría, producto y cliente | 200 |
| `PUT /api/pedidos/{id}/anular` y `/atender` | 200 |
| Body inválido | 400 con el JSON del servicio intacto |
| Recurso inexistente | 404 del servicio |
| Ruta inexistente | 404 del gateway |
| `producto-service` detenido | 503 controlado; el resto sigue en 200 |
| `"Tecnología"` / `"Cámara Web Ñandú"` | UTF-8 correcto de punta a punta |

Detalle completo en [[12 Pruebas HTTP]] y en `run/logs/gateway-test-results.md`.

## Logs y diagnóstico

```powershell
Get-Content run\logs\gateway-service.out.log -Tail 60
Select-String -Path run\logs\gateway-service.out.log -Pattern 'Route matched|lb://|No servers available'
```

## Enlaces

- [[22 Rutas del gateway (predicados, lb y balanceo)]]
- [[23 Discovery locator vs rutas explícitas]]
- [[25 Spring Cloud Gateway 5 (cambios y versiones)]]
- [[21 API Gateway]]
