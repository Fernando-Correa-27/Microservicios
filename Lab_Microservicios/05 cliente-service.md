---
tags:
  - glab-s11
  - servicio
  - cliente
fecha: 2026-11-01
estado: completado
---

# 05 · cliente-service

> [!info] Ficha
> **Puerto:** 8081 · **BD:** `bd_cliente` (dev: `bd_cliente_dev`) · **Perfil prod:** 9093

## Responsabilidad

Administrar clientes. Es consumido por `pedido-service` para validar `idCliente`.

## Modelo

| Campo | Tipo | Validación |
|---|---|---|
| `id` | Long | autogenerado |
| `nombre` | String(100) | `@NotBlank`, `@Size` |
| `apellidos` | String(100) | `@NotBlank`, `@Size` |
| `direccion` | String(200) | `@NotBlank`, `@Size` |
| `telefono` | String(15) | `@NotBlank`, `@Pattern(\\d{9})` |
| `nroDni` | String(8), UNIQUE | `@NotBlank`, `@Pattern(\\d{8})` |
| `estado` | String(20) | `@Pattern(ACTIVO|INACTIVO)` |

## Endpoints

| Método | Ruta | Éxito | Errores |
|---|---|---|---|
| GET | `/api/clientes` | 200 | 500 |
| GET | `/api/clientes/{id}` | 200 | 404 |
| POST | `/api/clientes` | 201 | 400, 409 |
| PUT | `/api/clientes/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/clientes/{id}` | 200 | 404 |

## Regla de DNI único

```java
if (repo.existsByNroDni(dto.getNroDni())) {
    throw new DuplicateResourceException("Ya existe un cliente con DNI: " + dto.getNroDni());
}
```

El handler traduce `DuplicateResourceException` a **409 Conflict**. En actualización se usa `existsByNroDniAndIdNot` para permitir conservar el mismo DNI.

## Ejemplo

```json
{
  "nombre": "Juan",
  "apellidos": "Pérez Gómez",
  "direccion": "Av. Javier Prado 123, Lima",
  "telefono": "987654321",
  "nroDni": "12345678",
  "estado": "ACTIVO"
}
```

> [!success] Resultados
> 201 al crear, 400 con teléfono/DNI inválido, 409 al repetir DNI, 404 al buscar ID inexistente.

## Enlaces

- [[06 pedido-service]] (consumidor)
- [[12 Pruebas HTTP]]

## S12 · cliente-service como cliente de Eureka

- Se agregó `spring-cloud-starter-netflix-eureka-client` y `@EnableDiscoveryClient`.
- Se agregó la sección `eureka:` en `application-dev.yml`.
- **No cambió** el CRUD ni el puerto 8081.
- Es consumido por `pedido-service` mediante `ClienteClient` (Feign, nombre lógico `cliente-service`).

### Criterio de nombres

| Campo | Nombre en cliente-service | Nombre en la respuesta del pedido |
|---|---|---|
| Apellidos | `apellidos` | `apellido` (mapeado con `@JsonAlias("apellidos")`) |
| DNI | `nroDni` | no se expone en el pedido |
| Estado | `estado` | no se expone en el pedido |

> [!note] Por qué el alias
> La guía mezcla `"apellido"` (JSON del pedido) con `"apellidos"` (validaciones de cliente). Se mantuvo `apellidos` como contrato de cliente-service y se agregó un alias en el `ClienteDTO` del pedido para que la salida coincida con el ejemplo.

Validaciones vigentes: nombre, apellidos, dirección, teléfono, estado y `nroDni` obligatorios; DNI exactamente 8 dígitos (`@Pattern("\\d{8}")`). DNI duplicado → **409**.

Enlaces: [[19 OpenFeign]] · [[12 Pruebas HTTP]]
