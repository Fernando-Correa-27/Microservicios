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
