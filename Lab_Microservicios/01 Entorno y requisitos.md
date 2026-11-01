---
tags:
  - glab-s11
  - entorno
  - instalacion
fecha: 2026-11-01
estado: completado
---

# 01 · Entorno y requisitos

> [!info] Resumen
> El equipo tenía **Java 24**, no Java 25; MySQL estaba instalado pero **sin servicio ni data directory**. Se instaló JDK 25 con winget y se inicializó una instancia local de MySQL 8.4 con `root` sin contraseña. Maven y Git ya estaban disponibles.

## Verificación inicial (real)

```powershell
java -version
# openjdk 24.0.2  → NO cumplía la guía (Java 25)

mvn -version
# Apache Maven 3.9.16, Java 24

Get-Command mysql
# no encontrado
Test-NetConnection localhost -Port 3306
# False
```

| Requisito | Estado inicial | Acción |
|---|---|---|
| Java 25 | ❌ solo Java 24 | `winget install Microsoft.OpenJDK.25` |
| Maven 3.9 | ✅ 3.9.16 | — |
| MySQL 8.4 | ⚠️ instalado, sin servicio | Inicializar data dir y arrancar `mysqld` |
| Git | ✅ 2.x | `git init` en la carpeta del lab |
| Herramienta HTTP | ✅ `curl.exe` | Pruebas con archivos JSON |

## Instalación de Java 25

```powershell
winget install --id Microsoft.OpenJDK.25 --silent `
  --accept-package-agreements --accept-source-agreements
```

Ruta instalada:

```text
C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot
```

Verificación:

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot'
& "$env:JAVA_HOME\bin\java.exe" -version
# openjdk version "25.0.5.0.2" 2026-10-22 LTS
```

> [!warning] JAVA_HOME
> Maven usa `JAVA_HOME`. Si la sesión de PowerShell no lo tiene, Maven compila con Java 24 y falla el `release 25`. Los scripts `run\start-all.ps1` lo fijan explícitamente.

## Instalación e inicialización de MySQL

Se instaló MySQL 8.4.9:

```powershell
winget install --id Oracle.MySQL --silent `
  --accept-package-agreements --accept-source-agreements
```

El MSI **no dejó un servicio** ni un data directory. Se inicializó una instancia local:

```powershell
$data='C:\Users\lenovo\AppData\Local\mysql-lab\data'
New-Item -ItemType Directory -Force -Path $data

& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' `
  --initialize-insecure `
  --basedir='C:\Program Files\MySQL\MySQL Server 8.4' `
  --datadir=$data
```

`--initialize-insecure` crea el usuario `root` **sin contraseña**, igual que la guía.

Arranque (en segundo plano o consola aparte):

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' `
  --basedir='C:\Program Files\MySQL\MySQL Server 8.4' `
  --datadir='C:\Users\lenovo\AppData\Local\mysql-lab\data' `
  --port=3306 --bind-address=127.0.0.1 --console
```

Verificación:

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' `
  -h 127.0.0.1 -u root -e "SELECT VERSION();"
# 8.4.9
```

> [!caution] MySQL no quedó como servicio de Windows
> Si se reinicia la máquina hay que volver a lanzar `mysqld` con el comando anterior. Alternativa: registrar el servicio con `mysqld --install`, pero el laboratorio se resolvió con instancia local para no alterar el equipo.

## Bases de datos creadas

```sql
CREATE DATABASE bd_categoria;
CREATE DATABASE bd_producto;
CREATE DATABASE bd_cliente;
CREATE DATABASE bd_pedido;
CREATE DATABASE bd_categoria_dev;
CREATE DATABASE bd_producto_dev;
CREATE DATABASE bd_cliente_dev;
CREATE DATABASE bd_pedido_dev;
CREATE DATABASE bd_paciente;
CREATE DATABASE bd_medico;
CREATE DATABASE bd_cita;
CREATE DATABASE bd_paciente_dev;
CREATE DATABASE bd_medico_dev;
CREATE DATABASE bd_cita_dev;
```

Script reproducible: `sql/init-databases.sql`.

## Versiones finales

| Herramienta | Versión |
|---|---|
| Java | 25.0.5 (Microsoft OpenJDK) |
| Maven | 3.9.16 |
| Spring Boot | 4.1.2 |
| MySQL | 8.4.9 |
| MySQL Connector/J | 9.7.0 (gestionado por Boot) |
| Hibernate | 7.4.13.Final |
| Tomcat | 11.0.27 |

> [!note] Spring Boot 4.1.2 vs guía 4.1.0
> Los proyectos venían con 4.1.2 (patch más reciente). No se bajó la versión; se documenta como cambio mínimo. En Boot 4 el starter web se llama `spring-boot-starter-webmvc`.

## Enlaces

- Siguiente: [[02 Arquitectura de microservicios]]
- Bitácora: [[10 Bitácora por pasos]]
- Comandos: [[16 Comandos útiles]]
