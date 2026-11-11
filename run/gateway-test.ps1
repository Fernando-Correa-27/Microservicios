# GLAB-S13 - Pruebas del API Gateway (http://localhost:8080)
# Uso: powershell -ExecutionPolicy Bypass -File run\gateway-test.ps1
# Requiere: eureka + 4 servicios + gateway levantados.
# Nota: este archivo se mantiene en ASCII; los datos con tildes viven en run\payloads\*.json (UTF-8).

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root       = Split-Path -Parent $PSScriptRoot
$base       = 'http://localhost:8080'
$payloadDir = Join-Path $PSScriptRoot 'payloads'
$logDir     = Join-Path $PSScriptRoot 'logs'
$tmpBody    = Join-Path $env:TEMP 'gw-body.txt'
$java       = 'C:\Program Files\Microsoft\jdk-25.0.5.21-hotspot\bin\java.exe'
$results    = New-Object System.Collections.ArrayList

function Req {
    param(
        [string]$Method,
        [string]$Path,
        [string]$BodyFile,
        [string]$ContentType = 'application/json'
    )
    $url = if ($Path.StartsWith('http')) { $Path } else { $base + $Path }
    $curlArgs = @('-s', '-o', $tmpBody, '-w', '%{http_code}', '-X', $Method)
    if ($BodyFile) {
        $curlArgs += @('-H', "Content-Type: $ContentType", '--data-binary', "@$BodyFile")
    }
    $code = & curl.exe @curlArgs $url
    $body = ''
    if (Test-Path $tmpBody) {
        $body = [System.IO.File]::ReadAllText($tmpBody, [System.Text.Encoding]::UTF8)
    }
    return [pscustomobject]@{ Status = [int]$code; Body = $body }
}

function Check {
    param(
        [string]$Name,
        [int]$Expected,
        [pscustomobject]$Response,
        [string]$MustContain = ''
    )
    $ok = ($Response.Status -eq $Expected)
    if ($ok -and $MustContain -ne '') { $ok = $Response.Body.Contains($MustContain) }
    $state = if ($ok) { 'OK' } else { 'FALLO' }
    [void]$results.Add([pscustomobject]@{
        Prueba   = $Name
        Esperado = $Expected
        Obtenido = $Response.Status
        Resultado = $state
    })
    Write-Host ("  [{0,-5}] {1} -> {2} (esperado {3})" -f $state, $Name, $Response.Status, $Expected)
    if (-not $ok) {
        $preview = $Response.Body
        if ($preview.Length -gt 300) { $preview = $preview.Substring(0, 300) }
        Write-Host ("          body: " + $preview) -ForegroundColor DarkYellow
    }
}

function Wait-Status {
    param([string]$Path, [int[]]$Wanted, [int]$Seconds = 120)
    $deadline = (Get-Date).AddSeconds($Seconds)
    $last = $null
    while ((Get-Date) -lt $deadline) {
        $last = Req -Method GET -Path $Path
        if ($Wanted -contains $last.Status) { return $last }
        Start-Sleep -Seconds 3
    }
    return $last
}

Write-Host "`n=== GLAB-S13 - Pruebas por el API Gateway ($base) ===" -ForegroundColor Cyan

# ---------------------------------------------------------------- 1. GET
Write-Host "`n1) GET (listas y por id)" -ForegroundColor Yellow
Check 'GET /api/categorias'        200 (Req GET '/api/categorias')
Check 'GET /api/categorias/1'      200 (Req GET '/api/categorias/1')
Check 'GET /api/productos'         200 (Req GET '/api/productos') 'categoria'
Check 'GET /api/productos/1'       200 (Req GET '/api/productos/1')
Check 'GET /api/clientes'          200 (Req GET '/api/clientes')
Check 'GET /api/clientes/1'        200 (Req GET '/api/clientes/1')
Check 'GET /api/pedidos'           200 (Req GET '/api/pedidos')
Check 'GET /api/pedidos/1 (Feign)' 200 (Req GET '/api/pedidos/1') '"cliente"'

# ------------------------------------------------------- 2. CRUD categoria
Write-Host "`n2) CRUD categorias (POST/PUT/DELETE)" -ForegroundColor Yellow
$r = Req -Method POST -Path '/api/categorias' -BodyFile (Join-Path $payloadDir 'categoria-valid.json')
Check 'POST /api/categorias' 201 $r
$catId = ($r.Body | ConvertFrom-Json).id
$putCat = Join-Path $env:TEMP 'gw-put-categoria.json'
[System.IO.File]::WriteAllText($putCat, '{"nombre":"Categoria Gateway Editada","estado":"ACTIVO"}', ([System.Text.UTF8Encoding]::new($false)))
Check "PUT /api/categorias/$catId" 200 (Req -Method PUT -Path "/api/categorias/$catId" -BodyFile $putCat) 'Editada'
Check "DELETE /api/categorias/$catId" 200 (Req -Method DELETE -Path "/api/categorias/$catId")

# -------------------------------------------------------- 3. CRUD producto
Write-Host "`n3) CRUD productos" -ForegroundColor Yellow
$r = Req -Method POST -Path '/api/productos' -BodyFile (Join-Path $payloadDir 'producto-valid.json')
Check 'POST /api/productos' 201 $r
$prodId = ($r.Body | ConvertFrom-Json).id
$putProd = Join-Path $env:TEMP 'gw-put-producto.json'
[System.IO.File]::WriteAllText($putProd, '{"nombre":"Producto Gateway Editado","precio":15.75,"stock":7,"categoriaId":1}', ([System.Text.UTF8Encoding]::new($false)))
Check "PUT /api/productos/$prodId" 200 (Req -Method PUT -Path "/api/productos/$prodId" -BodyFile $putProd) 'Editado'
Check "DELETE /api/productos/$prodId" 200 (Req -Method DELETE -Path "/api/productos/$prodId")

# --------------------------------------------------------- 4. CRUD cliente
Write-Host "`n4) CRUD clientes" -ForegroundColor Yellow
$dni = (Get-Random -Minimum 10000000 -Maximum 99999999).ToString()
$tel = '9' + (Get-Random -Minimum 10000000 -Maximum 99999999).ToString()
$cliJson = '{"nombre":"Lucia","apellidos":"Quispe Rojas","direccion":"Av. Arequipa 123","telefono":"' + $tel + '","nroDni":"' + $dni + '","estado":"ACTIVO"}'
$cliFile = Join-Path $env:TEMP 'gw-cliente.json'
[System.IO.File]::WriteAllText($cliFile, $cliJson, ([System.Text.UTF8Encoding]::new($false)))
$r = Req -Method POST -Path '/api/clientes' -BodyFile $cliFile
Check 'POST /api/clientes' 201 $r
$cliId = ($r.Body | ConvertFrom-Json).id
$putCli = Join-Path $env:TEMP 'gw-put-cliente.json'
[System.IO.File]::WriteAllText($putCli, ($cliJson -replace '"Lucia"', '"Lucia Editada"'), ([System.Text.UTF8Encoding]::new($false)))
Check "PUT /api/clientes/$cliId" 200 (Req -Method PUT -Path "/api/clientes/$cliId" -BodyFile $putCli) 'Editada'
Check "DELETE /api/clientes/$cliId" 200 (Req -Method DELETE -Path "/api/clientes/$cliId")

# ---------------------------------------------------------- 5. CRUD pedido
Write-Host "`n5) Pedidos (POST + anular/atender)" -ForegroundColor Yellow
$r = Req -Method POST -Path '/api/pedidos' -BodyFile (Join-Path $payloadDir 'pedido-valid.json')
Check 'POST /api/pedidos' 201 $r
$pedId = ($r.Body | ConvertFrom-Json).id
Check "PUT /api/pedidos/$pedId/anular" 200 (Req -Method PUT -Path "/api/pedidos/$pedId/anular") 'ANULADO'
Check "PUT /api/pedidos/$pedId/anular (repetido)" 409 (Req -Method PUT -Path "/api/pedidos/$pedId/anular")

$r2 = Req -Method POST -Path '/api/pedidos' -BodyFile (Join-Path $payloadDir 'pedido-valid.json')
$pedId2 = ($r2.Body | ConvertFrom-Json).id
Check "PUT /api/pedidos/$pedId2/atender" 200 (Req -Method PUT -Path "/api/pedidos/$pedId2/atender") 'ATENDIDO'

# --------------------------------------------------------- 6. Validaciones
Write-Host "`n6) Errores de validacion (400) propagados intactos" -ForegroundColor Yellow
Check 'POST /api/categorias (invalido)' 400 (Req -Method POST -Path '/api/categorias' -BodyFile (Join-Path $payloadDir 'categoria-invalid.json')) 'El nombre es obligatorio'
Check 'POST /api/productos (invalido)'  400 (Req -Method POST -Path '/api/productos' -BodyFile (Join-Path $payloadDir 'producto-invalid.json')) 'El precio debe ser mayor a cero'
Check 'POST /api/clientes (invalido)'   400 (Req -Method POST -Path '/api/clientes' -BodyFile (Join-Path $payloadDir 'cliente-invalid.json')) ('El tel' + [char]0xE9 + 'fono debe tener exactamente 9 d' + [char]0xED + 'gitos')
Check 'POST /api/pedidos (invalido)'    400 (Req -Method POST -Path '/api/pedidos' -BodyFile (Join-Path $payloadDir 'pedido-invalid.json')) 'La cantidad debe ser mayor a cero'

# ------------------------------------------------------------- 7. 404
Write-Host "`n7) Recursos y rutas inexistentes (404)" -ForegroundColor Yellow
Check 'GET /api/productos/999999'  404 (Req GET '/api/productos/999999')
Check 'GET /api/categorias/999999' 404 (Req GET '/api/categorias/999999')
Check 'GET /api/inexistente'       404 (Req GET '/api/inexistente')
Check 'GET /api/inexistente/sub'   404 (Req GET '/api/inexistente/sub')

# ------------------------------------------------------------- 8. UTF-8
Write-Host "`n8) UTF-8 de punta a punta" -ForegroundColor Yellow
$acento = 'Tecnolog' + [char]0xED + 'a'
$r = Req -Method POST -Path '/api/categorias' -BodyFile (Join-Path $payloadDir 'categoria-utf8.json')
Check 'POST /api/categorias (Tecnologia con tilde)' 201 $r $acento
$catUtf8 = ($r.Body | ConvertFrom-Json).id
Check "GET /api/categorias/$catUtf8 (tilde)" 200 (Req GET "/api/categorias/$catUtf8") $acento
Check "DELETE /api/categorias/$catUtf8" 200 (Req -Method DELETE -Path "/api/categorias/$catUtf8")

$camara = 'C' + [char]0xE1 + 'mara Web ' + [char]0xD1 + 'and' + [char]0xFA
$r = Req -Method POST -Path '/api/productos' -BodyFile (Join-Path $payloadDir 'producto-utf8.json')
Check 'POST /api/productos (Camara con enie y tilde)' 201 $r $camara
$prodUtf8 = ($r.Body | ConvertFrom-Json).id
Check "GET /api/productos/$prodUtf8 (tildes)" 200 (Req GET "/api/productos/$prodUtf8") $camara
Check "DELETE /api/productos/$prodUtf8" 200 (Req -Method DELETE -Path "/api/productos/$prodUtf8")

# ------------------------------------------------- 9. discovery locator
Write-Host "`n9) Discovery locator (ruta automatica por servicio)" -ForegroundColor Yellow
Check 'GET /categoria-service/api/categorias' 200 (Req GET '/categoria-service/api/categorias')
Check 'GET /producto-service/api/productos'   200 (Req GET '/producto-service/api/productos')

# -------------------------------------------- 10. servicio caido (503)
Write-Host "`n10) Servicio caido: producto-service" -ForegroundColor Yellow
$conn = Get-NetTCPConnection -LocalPort 8083 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    $pidProd = $conn.OwningProcess
    Stop-Process -Id $pidProd -Force
    Write-Host ("  producto-service detenido (PID {0}). Esperando que Eureka lo expire..." -f $pidProd)
} else {
    Write-Host '  producto-service ya estaba detenido.'
}

$t0 = Get-Date
$down = Wait-Status -Path '/api/productos' -Wanted @(503) -Seconds 150
$seg = [int]((Get-Date) - $t0).TotalSeconds
Check "GET /api/productos con servicio caido (${seg}s)" 503 $down
Write-Host ("          body: " + $down.Body) -ForegroundColor DarkGray
Check 'GET /api/categorias sigue OK' 200 (Req GET '/api/categorias')
Check 'GET /api/clientes sigue OK'   200 (Req GET '/api/clientes')
Check 'GET /api/pedidos sigue OK'    200 (Req GET '/api/pedidos')

Write-Host '  Reiniciando producto-service...'
$jar = Join-Path $root 'producto-service\target\producto-service-0.0.1-SNAPSHOT.jar'
Start-Process -FilePath $java -ArgumentList '-jar', "`"$jar`"" `
    -RedirectStandardOutput (Join-Path $logDir 'producto-service.out.log') `
    -RedirectStandardError (Join-Path $logDir 'producto-service.err.log') `
    -WindowStyle Hidden

for ($i = 0; $i -lt 120; $i++) {
    try {
        $c = New-Object Net.Sockets.TcpClient
        $c.Connect('127.0.0.1', 8083)
        $c.Close(); $c.Dispose()
        break
    } catch { Start-Sleep -Milliseconds 1000 }
}
$t1 = Get-Date
$up = Wait-Status -Path '/api/productos' -Wanted @(200) -Seconds 120
$seg2 = [int]((Get-Date) - $t1).TotalSeconds
Check "GET /api/productos tras recuperar (${seg2}s)" 200 $up 'categoria'

# ------------------------------------------------------------- resumen
$okCount = @($results | Where-Object { $_.Resultado -eq 'OK' }).Count
$total = $results.Count
Write-Host ("`n=== RESULTADO: {0}/{1} pruebas OK ===" -f $okCount, $total) -ForegroundColor Cyan

$md = @()
$md += '# GLAB-S13 - Resultados de pruebas por el API Gateway'
$md += ''
$md += ('Fecha: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm') + '  ')
$md += ('Base: ' + $base + '  ')
$md += ('Resultado: **' + $okCount + '/' + $total + '**')
$md += ''
$md += '| # | Prueba | Esperado | Obtenido | Resultado |'
$md += '|---|--------|----------|----------|-----------|'
$n = 0
foreach ($r in $results) {
    $n++
    $md += ('| {0} | {1} | {2} | {3} | {4} |' -f $n, $r.Prueba, $r.Esperado, $r.Obtenido, $r.Resultado)
}
$outFile = Join-Path $logDir 'gateway-test-results.md'
[System.IO.File]::WriteAllLines($outFile, $md, ([System.Text.UTF8Encoding]::new($false)))
Write-Host ("Tabla guardada en " + $outFile) -ForegroundColor Green

if ($okCount -ne $total) { exit 1 }

