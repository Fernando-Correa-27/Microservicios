---
tags:
  - glab-s11
  - glab-s12
  - microservicios
  - moc
fecha: 2026-11-05
estado: completado
---

# 00 · Índice (MOC) — GLAB-S11 + S12 Microservicios

> [!abstract] Propósito del vault
> Este vault es el **segundo cerebro** de los laboratorios **GLAB-S11 "Creación de Microservicios"** y **GLAB-S12 "Comunicación entre Servicios"** (Tecsup). Reúne entorno, arquitectura, código, decisiones, pruebas reales y conclusiones para poder estudiar o replicar ambos laboratorios sin volver a la guía.

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
| Bitácora | Este vault | ✅ Actualizado a S12 |

## Puertos de un vistazo

```text
8761 eureka-server       (service discovery, sin BD)

8081 cliente-service     bd_cliente_dev
8082 categoria-service   bd_categoria_dev
8083 producto-service    bd_producto_dev
8084 pedido-service      bd_pedido_dev
8085 paciente-service    bd_paciente_dev
8086 medico-service      bd_medico_dev
8087 cita-service        bd_cita_dev
```

> [!tip] Arranque rápido (S12: Eureka primero)
> `powershell -ExecutionPolicy Bypass -File run\start-all.ps1`
> Detener: `run\stop-all.ps1`. El script levanta Eureka en 8761, espera, y luego los servicios en orden. Más detalle en [[16 Comandos útiles]].

## Cómo estudiar este vault

1. Lee [[02 Arquitectura de microservicios]] para el panorama y el diagrama con Eureka.
2. Revisa [[17 Decisión de versiones Boot 4 + Spring Cloud 2025.1.x]] antes de tocar los `pom.xml`.
3. Entiende el descubrimiento con [[18 Eureka y service discovery]].
4. Aprende la comunicación declarativa en [[19 OpenFeign]] y el flujo en [[20 Comunicación entre servicios (flujo GET pedido)]].
5. Sigue [[10 Bitácora por pasos]] para reproducir el laboratorio.
6. Usa [[12 Pruebas HTTP]] para comprobar que tu implementación responde igual.
7. Repasa [[11 Errores y soluciones]] antes de exponer el laboratorio.
8. Cierra con [[15 Conclusiones]].

> [!info] Convenciones
> - `dev` = perfil por defecto, BD `*_dev`.
> - `prod` = perfil de ejemplo, host `servidor` inexistente (fallo esperado).
> - Todas las pruebas se hicieron contra servicios reales con `curl.exe` y `run\smoke-test.ps1`; no se usó la GUI de Postman.
> - S12 mantiene **Spring Boot 4.1.2 + Java 25 + Spring Cloud 2025.1.4** en los 8 proyectos.
