---
tags: [lab, conclusiones, glab-s11, retrospectiva]
created: 2026-11-01
updated: 2026-11-01
---

# 15. Conclusiones — GLAB-S11

> [!note] Retrospectiva final del laboratorio "Creación de Microservicios" (7 servicios, 14 BD, testing real, vault Obsidian).

---

## 15.1 Conclusión 1 — La guía es un punto de partida, no una especificación completa

> **Hecho**: La guía oficial (GLAB-S11-RCOELLO-2026-01-3.docx) omite validaciones críticas, manejo de errores HTTP, resiliencia, y define esquemas BD incompletos.
>
> **Impacto**: Si se implementa "tal cual", se obtienen:
> - 500 en vez de 404/409
> - StackOverflow en serialización
> - Datos inconsistentes en BD (sin UNIQUE, sin FK, sin DEFAULT)
> - Acoplamiento duro a `localhost:808X`
>
> **Aprendizaje**: **Leer la guía como requisitos mínimos**; completar con:
> - DTOs + Bean Validation en **toda** entrada
> - Jerarquía de excepciones de dominio (`ResourceNotFound`, `Conflict`, `ServiceUnavailable`)
> - `GlobalExceptionHandler` centralizado
> - Contratos de comunicación externos (YAML) + `RestClient` + Resilience4j
> - Máquinas de estado explícitas para flujos de negocio

---

## 15.2 Conclusión 2 — Spring Boot 4 + Java 25 funcionan, pero requieren ajustes de dependencias

> **Hecho**: El laboratorio usa **Spring Boot 4.1.2** (no 4.1.0 de la guía) sobre **JDK 25**.
>
> **Cambios relevantes vs Boot 3**:
> - Starter web: `spring-boot-starter-webmvc` (ya no `web`)
> - Jakarta EE 10+ (namespace `jakarta.*` obligatorio)
> - Hibernate 7.x (API cambios menores)
> - `RestClient` nativo (reemplaza `RestTemplate`/`WebClient` para sincrónico simple)
> - Resilience4j 2.x compatible
>
> **Problemas encontrados**:
> - `MethodArgumentTypeMismatchException` no existía en Boot 3 igual; handler necesario para 400 limpio en path variables
> - `spring.jpa.open-in-view` warning ruidoso → configurar explícito
> - MySQL Connector/J 9.x requiere `allowPublicKeyRetrieval=true` + `useSSL=false`
>
> **Recomendación**: Fijar versiones en `dependencyManagement`; probar upgrade de patch (4.1.2 → 4.1.x) en CI antes de adoptar.

---

## 15.3 Conclusión 3 — Testing HTTP real > MockMvc > Sin tests

> **Hecho**: Se ejecutaron **42 pruebas reales** contra servicios levantados en puertos 8081–8087 (MySQL real, red real).
>
> **Lo que detectaron tests reales que MockMvc NO**:
> - Puerto ocupado / servicio no levantado
> - DNS `servidor` no resuelto en perfil prod
> - `CommunicationsException` real cuando MySQL cae → 500 JSON
> - `ConnectException` / timeout real cuando dependencia down → 503 JSON
> - Codificación UTF-8 en payloads PowerShell (`Invoke-RestMethod` vs `curl.exe`)
> - HikariCP pool exhaustion tras reinicio de MySQL (conexiones stale)
>
> **Estrategia ganadora**:
> 1. Unit tests con MockMvc para lógica pura (rápidos, CI)
> 2. **Integration tests reales** (Testcontainers o instancia local) para contratos HTTP
> 3. Smoke test script (`run/smoke-test.ps1`) ejecutable manual y en CI
> 4. Colecciones Postman/Newman como contrato compartido con frontend/QA

---

## 15.4 Conclusión 4 — Configuración externa y perfiles evitan "funciona en mi máquina"

> **Hecho**: Dos perfiles (`dev` / `prod`) con **valores distintos** para puertos, URLs de dependencias, y host de BD.
>
> **Evidencia**:
> - `dev`: `localhost:3306`, puertos 8081–8087, URLs `http://localhost:808X` → **todo verde**
> - `prod`: `servidor:3306` (inexistente), puertos 9092–9098, URLs `http://localhost:808X` (hardcodeadas a dev a propósito) → **falla controlada al arrancar**
>
> **Clave**: **Ningún valor de configuración en código Java**. Todo vía `@Value` / `@ConfigurationProperties` / `Environment`.
>
> **Próximo paso natural**: Spring Cloud Config / Vault / Kubernetes ConfigMap para secretos y descubrimiento de servicios.

---

## 15.5 Conclusión 5 — Documentación viva (Obsidian + Postman) supera a PDF estático

> **Hecho**: Vault `Lab_Microservicios` con 16 notas enlazadas (wikilinks), frontmatter, callouts, tablas Markdown + colecciones Postman JSON versionadas en Git.
>
> **Ventajas vs Word/PDF**:
> - **Navegable**: `[[03 categoria-service]]` salta al detalle
> - **Buscable**: grep / Obsidian search por tag `#error` `#prueba` `#config`
> - **Ejecutable**: bloques de código copiables; `run/smoke-test.ps1` corre en 30 seg
> - **Versionable**: `git diff` muestra qué cambió en arquitectura / decisiones
> - **Compartible**: `git clone` → `open 00 Índice (MOC).md` → todo el contexto
>
> **Estructura mínima recomendada para futuros laboratorios**:
> ```
> 00 Índice (MOC).md           # Mapa + estado
> 01 Entorno y requisitos.md   # JDK, Maven, BD, puertos
> 02 Arquitectura.md           # Diagrama + decisiones
> 03..09 Servicio-X.md         # Uno por servicio (endpoints, DTOs, BD, tests)
> 10 Bitácora.md               # Cronológico
> 11 Errores y soluciones.md   # Referencia rápida
> 12 Pruebas HTTP.md           # Tabla resultados
> 13 Conceptos clave.md        # Patrones + snippets
> 14 Guía vs Realidad.md       # Gap analysis
> 15 Conclusiones.md           # Esta nota
> 16 Comandos útiles.md        # Cheatsheet
> ```

---

## 15.6 Métricas finales del laboratorio

| Métrica | Valor |
|---|---|
| **Servicios implementados** | 7 (4 core + 3 clínica) |
| **Bases de datos** | 14 (7 dev + 7 prod) |
| **Líneas de código Java** | ~4,200 (excluyendo target/) |
| **Archivos YAML config** | 21 (3 por servicio) |
| **Pruebas HTTP automatizadas** | 42 (100% pass) |
| **Colecciones Postman** | 2 + 28 bodies |
| **Commits Git locales** | 4 (incrementales) |
| **Notas Obsidian** | 16 (índice + 15 contenido) |
| **Tiempo total estimado** | ~18 horas efectivas |
| **Desviaciones de guía corregidas** | 15 problemas mayores |

---

## 15.7 Próximos pasos (fuera de alcance GLAB-S11)

| Área | Acción sugerida |
|---|---|
| **Seguridad** | Spring Security + JWT stateless; roles `ADMIN`, `OPERADOR` |
| **Observabilidad** | Micrometer + Prometheus + Grafana; OpenTelemetry traces |
| **Despliegue** | Dockerfile multi-stage + docker-compose.yml + Kubernetes manifests |
| **CI/CD** | GitHub Actions: build → test → smoke → publish Postman collections |
| **Contratos** | OpenAPI 3.1 generado desde código (`springdoc-openapi`) + contract testing (Pact) |
| **Datos** | Flyway/Liquibase migrations versionadas; Testcontainers en CI |
| **Resiliencia** | Bulkhead, RateLimiter, fallback responses (problem details RFC 9457) |
| **API Gateway** | Spring Cloud Gateway para routing, auth, rate-limit centralizado |

---

> [!quote] **Cierre**
> > "Los microservicios no son gratis: exigen contratos, observabilidad, resiliencia y automatización. Este laboratorio demostró que **empezar bien** (DTOs, excepciones, config externa, tests reales) ahorra semanas de debugging después."
>
> — Entregado: **2026-11-01** — Vault: `C:\Users\lenovo\Downloads\microservicios\Lab_Microservicios`