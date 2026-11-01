---
tags:
  - glab-s11
  - servicio
  - clinica
  - paciente
fecha: 2026-11-01
estado: completado
---

# 07 · paciente-service

> [!info] Ficha
> **Puerto:** 8085 · **BD:** `bd_paciente` (dev: `bd_paciente_dev`) · **Perfil prod:** 9096

## Responsabilidad

Administrar pacientes de la clínica **Salud Integral**. `cita-service` lo consulta para validar `idPaciente`.

## Tabla `paciente` (según la guía)

| Columna | Tipo | Restricción |
|---|---|---|
| `id_paciente` | INT | PK, autoincrement |
| `dni` | VARCHAR(8) | UNIQUE |
| `nombres` | VARCHAR(100) | NOT NULL |
| `apellidos` | VARCHAR(100) | NOT NULL |
| `fecha_nacimiento` | DATE | — |
| `sexo` | CHAR(1) | M/F |
| `telefono` | VARCHAR(15) | 9 dígitos |
| `direccion` | VARCHAR(200) | — |
| `correo` | VARCHAR(100) | formato email |
| `estado` | CHAR(1) | DEFAULT 'A' |

## Validaciones del DTO

```java
@NotBlank @Pattern(regexp = "\\d{8}")        private String dni;
@NotBlank @Size(max = 100)                   private String nombres;
@NotBlank @Size(max = 100)                   private String apellidos;
@Past                                        private LocalDate fechaNacimiento;
@Pattern(regexp = "M|F")                     private String sexo;
@Pattern(regexp = "\\d{9}")                  private String telefono;
@Email @Size(max = 100)                      private String correo;
@Pattern(regexp = "A|I")                     private String estado;
```

> [!note] Estado por defecto
> Si el cliente no envía `estado`, el service asigna `"A"` (activo), respetando el `DEFAULT 'A'` de la guía.

## Endpoints

| Método | Ruta | Éxito | Errores |
|---|---|---|---|
| GET | `/api/pacientes` | 200 | 500 |
| GET | `/api/pacientes/{id}` | 200 | 404 |
| POST | `/api/pacientes` | 201 | 400, 409 |
| PUT | `/api/pacientes/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/pacientes/{id}` | 200 | 404 |

## Resultados reales

| Caso | Status |
|---|---|
| Registrar Juan Carlos Quispe (DNI 45678912) | 201 |
| Registrar María Fernanda Rojas (DNI 41234567) | 201 |
| DNI de 3 dígitos + correo inválido + fecha futura | 400 con 7 errores por campo |
| DNI duplicado | 409 |
| GET 999 | 404 |

## Enlaces

- [[09 cita-service]] (consumidor)
- [[08 medico-service]]
- [[12 Pruebas HTTP]]
