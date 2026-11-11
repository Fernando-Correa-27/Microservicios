param(
    [switch]$SkipBuild,
    [switch]$CoreOnly,
    [switch]$NoGateway
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$javaHome = "C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot"
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

# GLAB-S12: Eureka SIEMPRE primero (service discovery).
$eureka = @{ name = "eureka-server"; port = 8761 }

# Servicios del laboratorio base (S11/S12). Todos son clientes de Eureka.
# S13: categoria y producto primero, luego cliente y pedido.
$core = @(
    @{ name = "categoria-service"; port = 8082 },
    @{ name = "producto-service";  port = 8083 },
    @{ name = "cliente-service";   port = 8081 },
    @{ name = "pedido-service";    port = 8084 }
)

# Servicios de la actividad de citas (S11). Opcionales.
$clinic = @(
    @{ name = "paciente-service"; port = 8085 },
    @{ name = "medico-service";   port = 8086 },
    @{ name = "cita-service";     port = 8087 }
)

# S13: API Gateway. Se levanta SIEMPRE al final, cuando ya hay servicios registrados.
$gateway = @{ name = "gateway-service"; port = 8080 }

$services = if ($CoreOnly) { $core } else { $core + $clinic }

$logDir = Join-Path $root "run\logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Build-Service($name) {
    $project = Join-Path $root $name
    Write-Host "Compilando $name..." -ForegroundColor Cyan
    Push-Location $project
    mvn -q -DskipTests package
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { throw "Fallo la compilacion de $name" }
}

function Start-Service($name, $port) {
    $project = Join-Path $root $name
    $jar = Join-Path $project "target\$name-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) { throw "No existe el jar: $jar. Ejecute sin -SkipBuild." }

    $out = Join-Path $logDir "$name.out.log"
    $err = Join-Path $logDir "$name.err.log"
    Start-Process -FilePath "$javaHome\bin\java.exe" `
        -ArgumentList "-jar", "`"$jar`"" `
        -RedirectStandardOutput $out -RedirectStandardError $err `
        -WindowStyle Hidden
    Write-Host "Iniciado $name en segundo plano (puerto $port)" -ForegroundColor Green
}

function Wait-Port($name, $port, $seconds = 120) {
    for ($i = 0; $i -lt ($seconds * 2); $i++) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $client.Connect("127.0.0.1", $port)
            $client.Close()
            $client.Dispose()
            return $true
        } catch {
            Start-Sleep -Milliseconds 500
        } finally {
            if ($client) { $client.Dispose() }
        }
    }
    return $false
}

# 1) Eureka
if (-not $SkipBuild) { Build-Service $eureka.name }
Start-Service $eureka.name $eureka.port
if (-not (Wait-Port $eureka.name $eureka.port 120)) {
    throw "eureka-server no respondio en el puerto 8761. Revise run\logs\eureka-server.err.log"
}
Write-Host "  OK  eureka-server -> http://localhost:8761" -ForegroundColor Green

# 2) Servicios, en orden y esperando el puerto de cada uno.
foreach ($s in $services) {
    if (-not $SkipBuild) { Build-Service $s.name }
    Start-Service $s.name $s.port
    if (Wait-Port $s.name $s.port 180) {
        Write-Host "  OK  $($s.name) -> http://localhost:$($s.port)" -ForegroundColor Green
    } else {
        Write-Host "  ERROR $($s.name) no respondio. Revise run\logs\$($s.name).err.log" -ForegroundColor Red
    }
    # Dar tiempo a que se registre en Eureka antes de levantar el siguiente.
    Start-Sleep -Seconds 3
}

# 3) API Gateway (S13), después de los servicios para que Eureka ya tenga instancias.
if (-not $NoGateway) {
    if (-not $SkipBuild) { Build-Service $gateway.name }
    Start-Service $gateway.name $gateway.port
    if (Wait-Port $gateway.name $gateway.port 180) {
        Write-Host "  OK  $($gateway.name) -> http://localhost:$($gateway.port)" -ForegroundColor Green
    } else {
        Write-Host "  ERROR $($gateway.name) no respondio. Revise run\logs\$($gateway.name).err.log" -ForegroundColor Red
    }
    Start-Sleep -Seconds 3
}

Write-Host "`nVerificando registro en Eureka..." -ForegroundColor Cyan
Start-Sleep -Seconds 10
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8761/eureka/apps" -Headers @{ Accept = "application/json" } -UseBasicParsing -TimeoutSec 30
    $apps = ($r.Content | ConvertFrom-Json).applications.application
    foreach ($a in $apps) {
        $inst = @($a.instance)
        Write-Host ("  {0} -> {1}" -f $a.name, (($inst | ForEach-Object { $_.status }) -join ", ")) -ForegroundColor Green
    }
} catch {
    Write-Host "  No se pudo leer /eureka/apps: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host "`nListo. Logs en $logDir" -ForegroundColor Yellow
