# GLAB-S13 - Ejecuta la coleccion Postman (Newman) contra el API Gateway.
# Excluye la carpeta "05" porque su ultima peticion exige detener
# producto-service (ese caso se valida con run\gateway-test.ps1).
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$col  = Join-Path $root 'postman\GLAB-S13-Microservicios.postman_collection.json'
$log  = Join-Path $PSScriptRoot 'logs\newman-s13.log'

$json = [System.IO.File]::ReadAllText($col, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$folders = @($json.item | Where-Object { $_.name -notlike '05*' } | ForEach-Object { $_.name })

$newmanArgs = @('--yes', 'newman', 'run', $col)
foreach ($f in $folders) {
    $newmanArgs += @('--folder', $f)
}

$output = & npx @newmanArgs 2>&1
$output | Set-Content $log -Encoding UTF8
$output | Select-Object -Last 50
Write-Output ('EXIT=' + $LASTEXITCODE)
