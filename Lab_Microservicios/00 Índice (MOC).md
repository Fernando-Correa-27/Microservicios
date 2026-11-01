---
tags:
  - glab-s11
  - microservicios
  - moc
fecha: 2026-11-01
estado: completado
---

# 00 · Índice (MOC) — GLAB-S11 Microservicios

> [!abstract] Propósito del vault
> Este vault es el **segundo cerebro** del laboratorio **GLAB-S11 “Creación de Microservicios”** (Tecsup). Reúne entorno, arquitectura, código, decisiones, pruebas reales y conclusiones para poder estudiar o replicar el laboratorio sin volver a la guía.

## Mapa de notas

| # | Nota | Contenido |
|---|---|---|
| 01 | [[01 Entorno y requisitos]] | Java 25, Maven, MySQL 8.4, curl, instalación y verificación |
| 02 | [[02 Arquitectura de microservicios]] | Monolito vs microservicios, diagrama, capas, acoplamiento |
| 03 | [[03 categoria-service]] | Puerto 8082, bd_categoria, CRUD y validación |
| 04 | [[04 producto-service]] | Puerto 8083, validación de categoría por HTTP |
| 05 | [[05 cliente-service]] | Puerto 8081, DNI único, validaciones |
| 06 | [[06 pedido-service]] | Puerto 8084, detalles, total calculado, comunicación |
| 07 | [[07 paciente-service]] | Puerto 8085, clínica, DNI/correo |
| 08 | [[08 medico-service]] | Puerto 8086, especialidad, CMP, horarios |
| 09 | [[09 cita-service]] | Puerto 8087, estados, atención 1:1, validación HTTP |
| 10 | [[10 Bitácora por pasos]] | Qué hice, comandos, resultado y por qué |
| 11 | [[11 Errores y soluciones]] | Error → causa → fix |
| 12 | [[12 Pruebas HTTP]] | Tabla de requests, status esperado y obtenido |
| 13 | [[13 Conceptos clave]] | Capas, DTO, validación, JPA, perfiles, RestClient |
| 14 | [[14 Guía original y problemas detectados]] | Código original y correcciones |
| 15 | [[15 Conclusiones]] | 5 conclusiones basadas en lo hecho |
| 16 | [[16 Comandos útiles]] | Build, arranque, MySQL, curl |

## Estado de avance

| Parte | Entregable | Estado |
|---|---|---|
| Parte 1 | 4 microservicios base (categoría, producto, cliente, pedido) | ✅ Completado |
| Parte 1 | YAML + perfiles dev/prod en los 4 servicios | ✅ Completado |
| Parte 2 | Comunicación HTTP y manejo de 404/503 | ✅ Completado y probado |
| Parte 3 | Clínica: paciente, médico, cita + atención | ✅ Completado |
| Postman | 2 colecciones importables + cuerpos JSON | ✅ Completado |
| Bitácora | Este vault | ✅ Completado |

## Puertos de un vistazo

```text
8081 cliente-service     bd_cliente_dev
8082 categoria-service   bd_categoria_dev
8083 producto-service    bd_producto_dev
8084 pedido-service      bd_pedido_dev
8085 paciente-service    bd_paciente_dev
8086 medico-service      bd_medico_dev
8087 cita-service        bd_cita_dev
```

> [!tip] Arranque rápido
> `powershell -ExecutionPolicy Bypass -File run\start-all.ps1`
> Detener: `run\stop-all.ps1`. Más detalle en [[16 Comandos útiles]].

## Cómo estudiar este vault

1. Lee [[02 Arquitectura de microservicios]] para el panorama.
2. Sigue [[10 Bitácora por pasos]] para reproducir el laboratorio.
3. Usa [[12 Pruebas HTTP]] para comprobar que tu implementación responde igual.
4. Repasa [[11 Errores y soluciones]] antes de exponer el laboratorio.
5. Cierra con [[15 Conclusiones]].

> [!info] Convenciones
> - `dev` = perfil por defecto, BD `*_dev`.
> - `prod` = perfil de ejemplo, host `servidor` inexistente (fallo esperado).
> - Todas las pruebas se hicieron contra servicios reales con `curl.exe`; no se usó la GUI de Postman.
