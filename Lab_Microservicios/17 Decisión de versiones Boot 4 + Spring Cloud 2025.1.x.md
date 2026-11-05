---
tags:
  - glab-s12
  - versiones
  - spring-boot
  - spring-cloud
fecha: 2026-11-05
estado: completado
---

# 17 · Decisión de versiones: Boot 4 + Spring Cloud 2025.1.x

> [!quote] Decisión tomada antes de empezar
> Mantener **Spring Boot 4.1.2** y **Java 25** (los de S11) y usar el tren **Spring Cloud 2025.1.4 (Oakwood)** en **todos** los proyectos, incluido `eureka-server`. No se copiaron literalmente los `pom.xml` de la guía.

## Qué pedía la guía y qué se usó

| Elemento | Guía S12 | Proyecto (decisión) |
|---|---|---|
| Spring Boot | 3.5.15 | **4.1.2** |
| Spring Cloud | 2025.0.0 | **2025.1.4** (Oakwood) |
| Java | 17 | **25** |
| Starter web | `spring-boot-starter-web` | `spring-boot-starter-webmvc` (nombre en Boot 4) |
| Eureka Server | 3.5.x | 4.1.2 + Cloud 2025.1.4 |
| Eureka Client | guía | 4.1.2 + Cloud 2025.1.4 |
| OpenFeign | guía | 4.1.2 + Cloud 2025.1.4 |

> [!warning] Por qué no se podía usar 2025.0.0
> Spring Cloud 2025.0.x está alineado con Spring Boot 3.5.x. Al arrancar con Boot 4 aparece `CompatibilityNotMetException`: *"Spring Boot [4.1.2] is not compatible with this Spring Cloud release train"*. La tabla oficial de compatibilidad exige **2025.1.x** para Boot 4.1.x (mínimo 2025.1.2; se usó la última disponible, **2025.1.4**).

## Cambios aplicados a cada `pom.xml`

Se partió del pom de S11 y se agregó solo lo nuevo:

```xml
<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2025.1.4</spring-cloud.version>
</properties>

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

Dependencias nuevas:

```xml
<!-- Todos los servicios -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- Solo producto-service y pedido-service -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

`eureka-server` usa `spring-cloud-starter-netflix-eureka-server` y **no** se registra a sí mismo (`register-with-eureka: false`, `fetch-registry: false`).

## Adaptaciones por Boot 4 / Spring Framework 7 / Jackson

- Los starters de web se mantienen como `spring-boot-starter-webmvc` (ya venían así de S11).
- Se conservan `jakarta.validation`, `jakarta.persistence` y `jakarta.annotation` (Boot 4 sigue sobre Jakarta EE).
- Los DTO usan `com.fasterxml.jackson.annotation.*` (`@JsonIgnoreProperties`, `@JsonAlias`); funcionan con el Jackson del stack de Boot 4.
- No se usaron APIs removidas de Spring Framework 7; el código de S11 compiló sin cambios.
- `@EnableDiscoveryClient` y `@EnableFeignClients` son de Spring Cloud 2025.1.x y son compatibles con Boot 4.

## Verificación real

```text
=== BUILD eureka-server ===      EXIT=0
=== BUILD categoria-service ===  EXIT=0
=== BUILD cliente-service ===    EXIT=0
=== BUILD producto-service ===   EXIT=0
=== BUILD pedido-service ===     EXIT=0
=== BUILD paciente-service ===   EXIT=0
=== BUILD medico-service ===     EXIT=0
=== BUILD cita-service ===       EXIT=0
```

> [!success] Resultado
> Ningún servicio lanzó `CompatibilityNotMetException`. Eureka arrancó en 8761 y los 7 clientes aparecieron **UP** en `/eureka/apps`. La decisión quedó validada en ejecución, no solo en compilación.

## Enlaces

- [[18 Eureka y service discovery]]
- [[19 OpenFeign]]
- [[14 Guía original y problemas detectados]]
