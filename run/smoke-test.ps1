# Smoke test de los 7 microservicios (GLAB-S11)
# Uso: powershell -ExecutionPolicy Bypass -File run\smoke-test.ps1
$ErrorActionPreference = "Continue"
$results = New-Object System.Collections.Generic.List[object]

function Req {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [string]$Body,
        [int]$Expected
    )
    $status = 0
    $payload = $null
    try {
        $params = @{ Uri = $Url; Method = $Method; TimeoutSec = 30 }
        if ($Body) {
            $params["ContentType"] = "application/json; charset=utf-8"
            $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes($Body)
        }
        $resp = Invoke-WebRequest @params -UseBasicParsing
        $status = [int]$resp.StatusCode
        $payload = $resp.Content
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $payload = $reader.ReadToEnd()
        } else {
            $payload = $_.Exception.Message
        }
    }
    $ok = ($status -eq $Expected)
    $results.Add([pscustomobject]@{
        Prueba   = $Name
        Metodo   = $Method
        Esperado = $Expected
        Obtenido = $status
        OK       = if ($ok) { "SI" } else { "NO" }
        Detalle  = ($payload -replace "\s+", " ")
    })
    Write-Host ("{0,-6} {1,-6} {2,-48} esperado={3} obtenido={4}" -f $(if($ok){"OK"}else{"FALLA"}), $Method, $Name, $Expected, $status)
}

$b = "http://localhost"
$hoy = Get-Date -Format "yyyy-MM-dd"

# ---------- cliente-service (8081) ----------
Req "Listar clientes"                "GET"  "$b`:8081/api/clientes" $null 200
Req "Obtener cliente 1"              "GET"  "$b`:8081/api/clientes/1" $null 200
Req "Cliente inexistente"            "GET"  "$b`:8081/api/clientes/99999" $null 404
Req "Cliente id no numerico"         "GET"  "$b`:8081/api/clientes/abc" $null 400
Req "Cliente sin nombre"             "POST" "$b`:8081/api/clientes" '{"nombre":"","apellidos":"X","direccion":"Av. X 1","telefono":"987654321","nroDni":"11111111","estado":"ACTIVO"}' 400
Req "Cliente telefono invalido"      "POST" "$b`:8081/api/clientes" '{"nombre":"Ana","apellidos":"Perez","direccion":"Av. X 1","telefono":"123","nroDni":"11111111","estado":"ACTIVO"}' 400
Req "DNI duplicado"                  "POST" "$b`:8081/api/clientes" '{"nombre":"Juan","apellidos":"Perez","direccion":"Av. X 1","telefono":"987654321","nroDni":"12345678","estado":"ACTIVO"}' 409
Req "Crear cliente valido"           "POST" "$b`:8081/api/clientes" ('{"nombre":"Smoke","apellidos":"Test","direccion":"Av. X 1","telefono":"900000000","nroDni":"' + (Get-Random -Minimum 10000000 -Maximum 99999999) + '","estado":"ACTIVO"}') 201

# ---------- categoria-service (8082) ----------
Req "Listar categorias"              "GET"  "$b`:8082/api/categorias" $null 200
Req "Categoria id no numerico"       "GET"  "$b`:8082/api/categorias/abc" $null 400
Req "Categoria inexistente"          "GET"  "$b`:8082/api/categorias/99999" $null 404
Req "Categoria nombre vacio"         "POST" "$b`:8082/api/categorias" '{"nombre":"","estado":"ACTIVO"}' 400
Req "Actualizar categoria inexistente" "PUT" "$b`:8082/api/categorias/99999" '{"nombre":"X","estado":"ACTIVO"}' 404
Req "Crear categoria valida"         "POST" "$b`:8082/api/categorias" ('{"nombre":"Smoke ' + (Get-Random) + '","estado":"ACTIVO"}') 201

# ---------- producto-service (8083) ----------
Req "Listar productos"               "GET"  "$b`:8083/api/productos" $null 200
Req "Producto inexistente"           "GET"  "$b`:8083/api/productos/99999" $null 404
Req "Producto precio negativo"       "POST" "$b`:8083/api/productos" '{"nombre":"Prod X","precio":-1,"stock":5,"categoriaId":1}' 400
Req "Producto categoria inexistente" "POST" "$b`:8083/api/productos" '{"nombre":"Prod X","precio":10.5,"stock":5,"categoriaId":99999}' 404
Req "Crear producto valido"          "POST" "$b`:8083/api/productos" ('{"nombre":"Smoke ' + (Get-Random) + '","precio":12.50,"stock":3,"categoriaId":1}') 201

# ---------- pedido-service (8084) - GLAB-S12: solo IDs + estado ----------
Req "Listar pedidos"                 "GET"  "$b`:8084/api/pedidos" $null 200
Req "Pedido cliente inexistente"     "POST" "$b`:8084/api/pedidos" '{"clienteId":99999,"productoId":9,"cantidad":1}' 404
Req "Pedido producto inexistente"    "POST" "$b`:8084/api/pedidos" '{"clienteId":6,"productoId":99999,"cantidad":1}' 404
Req "Pedido cantidad 0"              "POST" "$b`:8084/api/pedidos" '{"clienteId":6,"productoId":9,"cantidad":0}' 400
Req "Pedido sin cantidad"            "POST" "$b`:8084/api/pedidos" '{"clienteId":6,"productoId":9}' 400
Req "Pedido inexistente"             "GET"  "$b`:8084/api/pedidos/99999" $null 404
Req "Crear pedido valido"            "POST" "$b`:8084/api/pedidos" '{"clienteId":6,"productoId":9,"cantidad":1}' 201

# ---------- GLAB-S12: respuesta compuesta pedido + cliente + producto ----------
$pedidoPendiente = $null
try {
    $lista = Invoke-RestMethod -Uri "$b`:8084/api/pedidos" -Method GET
    $pedidoPendiente = ($lista | Where-Object { $_.estado -eq "PENDIENTE" } | Select-Object -First 1)
    if ($pedidoPendiente) {
        $compuesto = ($pedidoPendiente.cliente -ne $null) -and ($pedidoPendiente.producto -ne $null) -and
                     ($pedidoPendiente.cliente.nombre -ne $null) -and ($pedidoPendiente.producto.nombre -ne $null)
        $results.Add([pscustomobject]@{
            Prueba   = "GET pedido compuesto (cliente+producto)"
            Metodo   = "GET"
            Esperado = 200
            Obtenido = if ($compuesto) { 200 } else { 500 }
            OK       = if ($compuesto) { "SI" } else { "NO" }
            Detalle  = ($pedidoPendiente | ConvertTo-Json -Compress -Depth 5)
        })
        Write-Host ("{0,-6} {1,-6} {2,-48} esperado=200 obtenido={3}" -f $(if($compuesto){"OK"}else{"FALLA"}), "GET", "GET pedido compuesto (cliente+producto)", $(if($compuesto){200}else{500}))
    }
} catch {
    Write-Host "AVISO: no se pudo validar la respuesta compuesta: $($_.Exception.Message)" -ForegroundColor Yellow
}

if ($pedidoPendiente) {
    $id = $pedidoPendiente.id
    Req "Anular pedido PENDIENTE"      "PUT" "$b`:8084/api/pedidos/$id/anular" $null 200
    Req "Pedido ANULADO no se atiende" "PUT" "$b`:8084/api/pedidos/$id/atender" $null 409
} else {
    Write-Host "AVISO: no hay pedido PENDIENTE para probar transiciones" -ForegroundColor Yellow
}

# ---------- paciente-service (8085) ----------
Req "Listar pacientes"               "GET"  "$b`:8085/api/pacientes" $null 200
Req "Paciente inexistente"           "GET"  "$b`:8085/api/pacientes/99999" $null 404
Req "Paciente dni invalido"          "POST" "$b`:8085/api/pacientes" '{"dni":"123","nombres":"Luis","apellidos":"Ramos"}' 400
Req "Paciente dni duplicado"         "POST" "$b`:8085/api/pacientes" '{"dni":"45678912","nombres":"Luis","apellidos":"Ramos"}' 409

# ---------- medico-service (8086) ----------
Req "Listar especialidades"          "GET"  "$b`:8086/api/especialidades" $null 200
Req "Listar medicos"                 "GET"  "$b`:8086/api/medicos" $null 200
Req "Medico inexistente"             "GET"  "$b`:8086/api/medicos/99999" $null 404
Req "Medico especialidad inexistente" "POST" "$b`:8086/api/medicos" '{"cmp":"99999","nombres":"Test","apellidos":"Test","idEspecialidad":99999}' 404
Req "Medico cmp duplicado"           "POST" "$b`:8086/api/medicos" '{"cmp":"45678","nombres":"Test","apellidos":"Test","idEspecialidad":1}' 409

# ---------- cita-service (8087) ----------
$pacId = 2
$medId = 1
try {
    $pacs = Invoke-RestMethod -Uri "$b`:8085/api/pacientes" -Method GET
    if ($pacs) { $pacId = @($pacs)[0].idPaciente }
    $meds = Invoke-RestMethod -Uri "$b`:8086/api/medicos" -Method GET
    if ($meds) { $medId = @($meds)[0].idMedico }
} catch { }
Req "Listar citas"                   "GET"  "$b`:8087/api/citas" $null 200
Req "Cita paciente inexistente"      "POST" "$b`:8087/api/citas" ('{"idPaciente":99999,"idMedico":' + $medId + ',"fecha":"' + $hoy + '","hora":"10:00:00","motivo":"x"}') 404
Req "Cita medico inexistente"        "POST" "$b`:8087/api/citas" ('{"idPaciente":' + $pacId + ',"idMedico":99999,"fecha":"' + $hoy + '","hora":"10:00:00","motivo":"x"}') 404
Req "Cita sin fecha"                 "POST" "$b`:8087/api/citas" ('{"idPaciente":' + $pacId + ',"idMedico":' + $medId + ',"hora":"10:00:00"}') 400
Req "Crear cita valida"              "POST" "$b`:8087/api/citas" ('{"idPaciente":' + $pacId + ',"idMedico":' + $medId + ',"fecha":"2026-12-20","hora":"11:00:00","motivo":"Smoke"}') 201

# ---------- Transiciones de estado ----------
$cita = $null
try {
    $r = Invoke-RestMethod -Uri "$b`:8087/api/citas" -Method GET
    $cita = ($r | Where-Object { $_.estado -eq "PROGRAMADA" } | Select-Object -First 1)
} catch { }
if ($cita) {
    $id = $cita.idCita
    Req "Transicion PROGRAMADA->CONFIRMADA" "PUT" "$b`:8087/api/citas/$id/estado" '{"estado":"CONFIRMADA"}' 200
    Req "Transicion CONFIRMADA->ATENDIDA"   "PUT" "$b`:8087/api/citas/$id/estado" '{"estado":"ATENDIDA","diagnostico":"Control","tratamiento":"Reposo","observaciones":"OK"}' 200
    Req "Cita terminal no cambia"           "PUT" "$b`:8087/api/citas/$id/estado" '{"estado":"CANCELADA"}' 409
    Req "Obtener atencion de la cita"       "GET" "$b`:8087/api/citas/$id/atencion" $null 200
} else {
    Write-Host "AVISO: no hay cita PROGRAMADA para probar transiciones" -ForegroundColor Yellow
}

$results | Format-Table -AutoSize -Wrap | Out-String -Width 200 | Write-Host
$fallas = @($results | Where-Object { $_.OK -eq "NO" }).Count
Write-Host ""
Write-Host ("Total: {0}  Correctas: {1}  Fallas: {2}" -f $results.Count, ($results.Count - $fallas), $fallas) -ForegroundColor $(if ($fallas -eq 0) { "Green" } else { "Red" })
$results | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 (Join-Path $PSScriptRoot "smoke-results.json")
