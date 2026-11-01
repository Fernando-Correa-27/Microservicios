param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$javaHome = "C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot"
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

$services = @(
    @{ name = "cliente-service";   port = 8081 },
    @{ name = "categoria-service"; port = 8082 },
    @{ name = "producto-service";  port = 8083 },
    @{ name = "pedido-service";    port = 8084 },
    @{ name = "paciente-service";  port = 8085 },
    @{ name = "medico-service";    port = 8086 },
    @{ name = "cita-service";      port = 8087 }
)

$logDir = Join-Path $root "run\logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

foreach ($s in $services) {
    $project = Join-Path $root $s.name
    if (-not $SkipBuild) {
        Write-Host "Compilando $($s.name)..." -ForegroundColor Cyan
        Push-Location $project
        mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw "Falló la compilación de $($s.name)" }
        Pop-Location
    }

    $jar = Join-Path $project "target\$($s.name)-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) { throw "No existe el jar: $jar. Ejecute sin -SkipBuild." }

    $out = Join-Path $logDir "$($s.name).out.log"
    $err = Join-Path $logDir "$($s.name).err.log"
    Start-Process -FilePath "$javaHome\bin\java.exe" `
        -ArgumentList "-jar", "`"$jar`"" `
        -RedirectStandardOutput $out -RedirectStandardError $err `
        -WindowStyle Hidden
    Write-Host "Iniciado $($s.name) en segundo plano (puerto $($s.port))" -ForegroundColor Green
}

Write-Host "`nEsperando puertos..." -ForegroundColor Cyan
foreach ($s in $services) {
    $ok = $false
    for ($i = 0; $i -lt 60; $i++) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $client.Connect("127.0.0.1", $s.port)
            $ok = $true
            $client.Close()
            break
        } catch {
            Start-Sleep -Milliseconds 500
        } finally {
            $client.Dispose()
        }
    }
    if ($ok) {
        Write-Host "  OK  $($s.name) -> http://localhost:$($s.port)" -ForegroundColor Green
    } else {
        Write-Host "  ERROR $($s.name) no respondió. Revise run\logs\$($s.name).err.log" -ForegroundColor Red
    }
}

Write-Host "`nListo. Logs en $logDir" -ForegroundColor Yellow
