---
tags:
  - glab-s11
  - glab-s12
  - glab-s13
  - microservicios
  - moc
fecha: 2026-11-05
actualizado: 2026-11-11
estado: completado
---

# 00 · Índice (MOC) — GLAB-S11 + S12 + S13 Microservicios

> [!abstract] Propósito del vault
> Este vault es el **segundo cerebro** de los laboratorios **GLAB-S11 "Creación de Microservicios"**, **GLAB-S12 "Comunicación entre Servicios"** y **GLAB-S13 "Service Discovery y Gateway"** (Tecsup). Reúne entorno, arquitectura, código, decisiones, pruebas reales y conclusiones para poder estudiar o replicar los tres laboratorios sin volver a la guía.

## Mapa de notas

| # | Nota | Contenido |
|---|---|---|
| 01 | [[01 Entorno y requisitos]] | Java 25, Maven, MySQL 8.4, curl, instalación y verificación |
| 02 | [[02 Arquitectura de microservicios]] | Monolito vs microservicios, diagrama, capas, acoplamiento, Eureka |
| 03 | [[03 categoria-service]] | Puerto 8082, bd_categoria, CRUD y validación, cliente Eureka |
| 04 | [[04 producto-service]] | Puerto 8083, OpenFeign hacia categoría, DTO anidado |
| 05 | [[05 cliente-service]] | Puerto 8081, DNI único, validaciones, cliente Eureka |
| 06 | [[06 pedido-service]] | Puerto 8084, IDs + estado, OpenFeign a cliente/producto |
| 07 | [[07 paciente-service]] | Puerto 8085, clínica, DNI/correo, cliente Eureka |
| 08 | [[08 medico-service]] | Puerto 8086, especialidad, CMP, horarios, cliente Eureka |
| 09 | [[09 cita-service]] | Puerto 8087, estados, atención 1:1, cliente Eureka |
| 10 | [[10 Bitácora por pasos]] | Qué hice, comandos, resultado y por qué (S11 + S12) |
| 11 | [[11 Errores y soluciones]] | Error → causa → fix (incluye errores de S12) |
| 12 | [[12 Pruebas HTTP]] | Tabla de requests, status esperado y obtenido (S11 + S12) |
| 13 | [[13 Conceptos clave]] | Capas, DTO, validación, JPA, perfiles, RestClient |
| 14 | [[14 Guía original y problemas detectados]] | Código original y correcciones (S11 + S12) |
| 15 | [[15 Conclusiones]] | 5 conclusiones S11 + 5 conclusiones S12 |
| 16 | [[16 Comandos útiles]] | Build, arranque, MySQL, curl, Eureka, Feign |
| 17 | [[17 Decisión de versiones Boot 4 + Spring Cloud 2025.1.x]] | Por qué 2025.1.4 y diferencias con la guía |
| 18 | [[18 Eureka y service discovery]] | Qué es, configuración, registro y verificación |
| 19 | [[19 OpenFeign]] | Cliente declarativo, resolución por nombre, errores |
| 20 | [[20 Comunicación entre servicios (flujo GET pedido)]] | Flujo completo pedido → cliente/producto |
| 21 | [[21 API Gateway]] | Qué es, punto único de entrada, ventajas, Eureka vs Gateway |
| 22 | [[22 Rutas del gateway (predicados, lb y balanceo)]] | Predicados `Path`, `lb://`, balanceo, rutas activas |
| 23 | [[23 Discovery locator vs rutas explícitas]] | Rutas automáticas vs declaradas, riesgos en producción |
| 24 | [[24 gateway-service]] | Puerto 8080, pom, YAML, endpoints, pruebas |
| 25 | [[25 Spring Cloud Gateway 5 (cambios y versiones)]] | Starter, prefijo nuevo, actuator y decisión de versiones |

## Estado de avance

| Parte | Entregable | Estado |
|---|---|---|
| S11 · Parte 1 | 4 microservicios base (categoría, producto, cliente, pedido) | ✅ Completado |
| S11 · Parte 1 | YAML + perfiles dev/prod en los 4 servicios | ✅ Completado |
| S11 · Parte 2 | Comunicación HTTP y manejo de 404/503 | ✅ Completado y probado |
| S11 · Parte 3 | Clínica: paciente, médico, cita + atención | ✅ Completado |
| S12 · Parte 1 | eureka-server en 8761 y 7 servicios registrados UP | ✅ Completado y verificado |
| S12 · Parte 2 | producto → categoría con OpenFeign | ✅ Completado y probado |
| S12 · Parte 3 | pedido → cliente/producto con OpenFeign + estados | ✅ Completado y probado |
| S12 | Manejo de 400/404/409/500/503 en JSON | ✅ Completado y probado |
| S12 | Colección Postman actualizada | ✅ Completado |
| S12 | Regresión S11 (47 pruebas) | ✅ 47/47 |
| S13 · Parte 1 | gateway-service en 8080 con rutas categoria y producto | ✅ Completado y probado |
| S13 · Parte 2 | Rutas cliente y pedido publicadas por el gateway | ✅ Completado y probado |
| S13 | CRUD + 400/404/503 + UTF-8 por el gateway | ✅ 42/42 pruebas |
| S13 | Colección Postman con `baseUrl = :8080` (Newman 29 requests) | ✅ 53/53 aserciones |
| S13 | Actuator `/actuator/gateway/routes` | ✅ 9 rutas listadas |
| Bitácora | Este vault | ✅ Actualizado a S13 |

## Puertos de un vistazo

```text
8761 eureka-server       (service discovery, sin BD)
8080 gateway-service     (API Gateway, sin BD)  <-- S13

8081 cliente-service     bd_cliente_dev
8082 categoria-service   bd_categoria_dev
8083 producto-service    bd_producto_dev
8084 pedido-service      bd_pedido_dev
8085 paciente-service    bd_paciente_dev
8086 medico-service      bd_medico_dev
8087 cita-service        bd_cita_dev
```

> [!tip] Arranque rápido (S13: Eureka → servicios → gateway)
> `powershell -ExecutionPolicy Bypass -File run\start-all.ps1`
> Detener: `run\stop-all.ps1`. El script levanta Eureka en 8761, espera, luego los servicios en orden y **al final el gateway** en 8080. Con `-CoreOnly` omite la clínica; con `-NoGateway` no levanta el gateway. Más detalle en [[16 Comandos útiles]].

## Cómo estudiar este vault

1. Lee [[02 Arquitectura de microservicios]] para el panorama y el diagrama con Eureka y el gateway.
2. Revisa [[17 Decisión de versiones Boot 4 + Spring Cloud 2025.1.x]] antes de tocar los `pom.xml`.
3. Entiende el descubrimiento con [[18 Eureka y service discovery]].
4. Aprende la comunicación declarativa en [[19 OpenFeign]] y el flujo en [[20 Comunicación entre servicios (flujo GET pedido)]].
5. Entiende el punto único de entrada en [[21 API Gateway]] y las rutas en [[22 Rutas del gateway (predicados, lb y balanceo)]].
6. Revisa [[25 Spring Cloud Gateway 5 (cambios y versiones)]] antes de copiar configuración de la guía.
7. Sigue [[10 Bitácora por pasos]] para reproducir el laboratorio.
8. Usa [[12 Pruebas HTTP]] para comprobar que tu implementación responde igual.
9. Repasa [[11 Errores y soluciones]] antes de exponer el laboratorio.
10. Cierra con [[15 Conclusiones]].

> [!info] Convenciones
> - `dev` = perfil por defecto, BD `*_dev`.
> - `prod` = perfil de ejemplo, host `servidor` inexistente (fallo esperado).
> - Todas las pruebas se hicieron contra servicios reales con `curl.exe`, `run\smoke-test.ps1` y `run\gateway-test.ps1`; no se usó la GUI de Postman.
> - S12 mantiene **Spring Boot 4.1.2 + Java 25 + Spring Cloud 2025.1.4** en los 8 proyectos.
> - S13 agrega `gateway-service` con las **mismas versiones** (Gateway 5.0.4): 9 proyectos en total.
