---
tags: [lab, comandos, cheatsheet, glab-s11, powershell, mysql, maven, git]
created: 2026-11-01
updated: 2026-11-01
---

# 16. Comandos útiles — GLAB-S11

> [!note] Chuleta de comandos usados durante el laboratorio. Ejecutar desde `C:\Users\lenovo\Downloads\microservicios` salvo que se indique.

---

## 16.1 Entorno y build

```powershell
# Verificar versiones
java -version                    # JDK 25.0.5
mvn -version                     # Maven 3.9.16
Get-Command mysql                # MySQL 8.4.9

# Compilar un servicio (perfil dev)
cd categoria-service
mvn -q -DskipTests package       # JAR en target/*-SNAPSHOT.jar

# Compilar todos (script)
.\run\start-all.ps1              # Build + start 7 servicios (dev)
.\run\start-all.ps1 -SkipBuild   # Solo start (jars ya compilados)
.\run\stop-all.ps1               # Mata procesos en puertos 8081-8087 y 9092-9098
```

---

## 16.2 MySQL (instancia local en `C:\Users\lenovo\AppData\Local\mysql-lab\data`)

```powershell
# Iniciar mysqld (ventana aparte o background)
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' `
  --basedir='C:\Program Files\MySQL\MySQL Server 8.4' `
  --datadir='C:\Users\lenovo\AppData\Local\mysql-lab\data' `
  --port=3306 --bind-address=127.0.0.1 --console

# Conectar como root (sin password)
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -e "SHOW DATABASES;"

# Crear esquemas (dev + prod)
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root < sql\init-databases.sql

# Ver tablas de un servicio
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -e "USE bd_categoria_dev; SHOW TABLES; DESCRIBE categoria; SELECT * FROM categoria;"

# Matar mysqld si se cuelga
Get-Process mysqld -ErrorAction SilentlyContinue | Stop-Process -Force
```

---

## 16.3 Levantar / probar servicios individualmente

```powershell
# Variables
$JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot"
$JAR = "categoria-service\target\categoria-service-0.0.1-SNAPSHOT.jar"

# Perfil dev (puerto 8082)
& $JAVA_HOME\bin\java.exe -jar $JAR --spring.profiles.active=dev

# Perfil prod (puerto 9092, falla a propósito por host 'servidor')
& $JAVA_HOME\bin\java.exe -jar $JAR --spring.profiles.active=prod

# Health check rápido
Invoke-WebRequest http://localhost:8082/api/categorias -UseBasicParsing | Select-Object StatusCode
```

---

## 16.4 Pruebas HTTP (PowerShell)

```powershell
# GET listado
Invoke-RestMethod http://localhost:8081/api/clientes

# GET por ID
Invoke-RestMethod http://localhost:8081/api/clientes/1

# POST JSON (UTF-8 seguro)
$body = @{ nombre="Juan"; apellidos="Perez"; direccion="Av. X 1"; telefono="987654321"; nroDni="11111111"; estado="ACTIVO" } | ConvertTo-Json -Depth 3
Invoke-RestMethod -Uri http://localhost:8081/api/clientes -Method POST -ContentType 'application/json; charset=utf-8' -Body $body

# PUT actualizar
$body = @{ nombre="Juan Modificado"; apellidos="Perez"; direccion="Av. X 1"; telefono="987654321"; nroDni="11111111"; estado="ACTIVO" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8081/api/clientes/1 -Method PUT -ContentType 'application/json' -Body $body

# DELETE
Invoke-RestMethod -Uri http://localhost:8081/api/clientes/1 -Method DELETE

# Probar 404
Invoke-WebRequest http://localhost:8081/api/clientes/99999 -UseBasicParsing
# (catch para ver status 404)

# Probar 400 (path no numérico)
Invoke-WebRequest http://localhost:8081/api/clientes/abc -UseBasicParsing

# Probar 409 (DNI duplicado)
$body = @{ nombre="X"; apellidos="Y"; direccion="Z"; telefono="900000000"; nroDni="12345678"; estado="ACTIVO" } | ConvertTo-Json
Invoke-WebRequest -Uri http://localhost:8081/api/clientes -Method POST -ContentType 'application/json' -Body $body
```

> [!tip] **JSON con `curl.exe` (evita escaping PowerShell)**
> ```powershell
> curl.exe -X POST http://localhost:8082/api/categorias `
>   -H "Content-Type: application/json" `
>   --data-binary "@postman\bodies\categoria-create.json"
> ```

---

## 16.5 Scripts de smoke test

```powershell
# Ejecutar suite completa (42 pruebas)
powershell -ExecutionPolicy Bypass -File .\run\smoke-test.ps1

# Ver resultados JSON generados
Get-Content .\run\smoke-results.json | ConvertFrom-Json | Format-Table Prueba, Metodo, Esperado, Obtenido, OK -AutoSize
```

---

## 16.6 Postman / Newman

```bash
# Instalar Newman (requiere Node.js)
npm install -g newman

# Ejecutar colección core
newman run postman/GLAB-S11-Microservicios.postman_collection.json `
  -e postman/environment-dev.json `
  --reporters cli,json `
  --reporter-json-export run/newman-core-results.json

# Ejecutar colección clínica
newman run postman/GLAB-S11-Clinica.postman_collection.json `
  -e postman/environment-dev.json
```

---

## 16.7 Git (commits locales, sin push)

```powershell
# Estado
git status --short

# Ver diff
git diff
git diff --staged

# Commit incremental
git add -A
git commit -m "feat: descripción del avance"

# Log compacto
git log --oneline -10

# Ver archivos rastreados
git ls-files
```

---

## 16.8 Obsidian Vault

```powershell
# Abrir vault (requiere Obsidian instalado)
start "C:\Users\lenovo\Downloads\microservicios\Lab_Microservicios\00 Índice (MOC).md"

# O desde Obsidian: File → Open vault → C:\Users\lenovo\Downloads\microservicios\Lab_Microservicios
```

---

## 16.9 Puertos y procesos

```powershell
# Ver qué escucha en puertos del lab
Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -ge 8081 -and $_.LocalPort -le 8087 } | Select-Object LocalPort, OwningProcess
Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -ge 9092 -and $_.LocalPort -le 9098 } | Select-Object LocalPort, OwningProcess

# Matar proceso por puerto
$pid = (Get-NetTCPConnection -LocalPort 8082 -State Listen).OwningProcess
Stop-Process -Id $pid -Force

# Ver procesos Java activos
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Select-Object ProcessId, CommandLine | Format-List
```

---

## 16.10 Logs

```powershell
# Ver logs de un servicio (últimas 50 líneas)
Get-Content .\run\logs\categoria-service.out.log -Tail 50
Get-Content .\run\logs\categoria-service.err.log -Tail 50

# Seguir log en tiempo real (PowerShell 7+)
Get-Content .\run\logs\cliente-service.out.log -Wait -Tail 20
```

---

## 16.11 Limpieza de datos de prueba (reset BD dev)

```powershell
@"
DELETE FROM bd_cita_dev.atencion WHERE id_cita IN (SELECT id_cita FROM bd_cita_dev.cita WHERE id_cita > 4);
DELETE FROM bd_cita_dev.cita WHERE id_cita > 4;
DELETE FROM bd_pedido_dev.detalle_pedido WHERE id_pedido > 1;
DELETE FROM bd_pedido_dev.pedido WHERE id_pedido > 1;
DELETE FROM bd_cliente_dev.cliente WHERE id > 2;
DELETE FROM bd_producto_dev.producto WHERE id > 3;
DELETE FROM bd_categoria_dev.categoria WHERE id > 3;
DELETE FROM bd_paciente_dev.paciente WHERE id_paciente > 2;
DELETE FROM bd_medico_dev.horario_medico WHERE id_horario > 1;
DELETE FROM bd_medico_dev.medico WHERE id_medico > 2;
DELETE FROM bd_medico_dev.especialidad WHERE id_especialidad > 2;
"@ | & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root
```

---

## 16.12 Resilience4j / Actuator (si se habilita)

```powershell
# Ver estado CircuitBreaker (requiere actuator expuesto)
Invoke-RestMethod http://localhost:8083/actuator/circuitbreakers
Invoke-RestMethod http://localhost:8083/actuator/circuitbreakerevents
Invoke-RestMethod http://localhost:8083/actuator/health
```

---

## 16.13 Variables de entorno útiles

```powershell
# JAVA_HOME persistente (solo sesión actual)
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Perfil Spring Boot por variable de entorno
$env:SPRING_PROFILES_ACTIVE = "dev"
# o prod
$env:SPRING_PROFILES_ACTIVE = "prod"
```

---

> [!check] **Todos los comandos probados en Windows 11 + PowerShell 5.1** — ajustar rutas si tu instalación difiere.

## Comandos GLAB-S12

### Arranque (Eureka primero)

```powershell
# Compila todo y levanta Eureka + 7 servicios en orden
powershell -ExecutionPolicy Bypass -File run\start-all.ps1

# Solo los 4 servicios base (sin clínica)
powershell -ExecutionPolicy Bypass -File run\start-all.ps1 -CoreOnly

# Sin recompilar (jars ya construidos)
powershell -ExecutionPolicy Bypass -File run\start-all.ps1 -SkipBuild

# Detener todo
powershell -ExecutionPolicy Bypass -File run\stop-all.ps1
```

> [!warning] Antes de compilar
> Detén los servicios: en Windows el jar en ejecución bloquea el `repackage` de Maven.

### Eureka

```powershell
Invoke-WebRequest http://localhost:8761/ -UseBasicParsing
Invoke-WebRequest http://localhost:8761/eureka/apps -Headers @{Accept='application/json'} -UseBasicParsing
Invoke-WebRequest http://localhost:8761/eureka/apps/PEDIDO-SERVICE -Headers @{Accept='application/json'} -UseBasicParsing
```

### OpenFeign / flujo

```powershell
curl.exe -s http://localhost:8083/api/productos
curl.exe -s http://localhost:8084/api/pedidos/1
curl.exe -s -X PUT http://localhost:8084/api/pedidos/1/atender
curl.exe -s -X PUT http://localhost:8084/api/pedidos/1/anular
```

### Enviar JSON con tildes (UTF-8 sin BOM)

```powershell
$dir = "$env:TEMP\s12"
[System.IO.File]::WriteAllText("$dir\cat.json", '{"nombre":"Tecnología","estado":"ACTIVO"}', (New-Object System.Text.UTF8Encoding($false)))
curl.exe -s -X POST http://localhost:8082/api/categorias -H "Content-Type: application/json; charset=utf-8" --data-binary "@$dir\cat.json"
```

### MySQL

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -e "SELECT VERSION();"
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -e "SELECT table_schema,table_name,table_collation FROM information_schema.tables WHERE table_schema LIKE 'bd_%_dev';"
```

### Regresión

```powershell
powershell -ExecutionPolicy Bypass -File run\smoke-test.ps1
```

Enlaces: [[18 Eureka y service discovery]] · [[12 Pruebas HTTP]]

