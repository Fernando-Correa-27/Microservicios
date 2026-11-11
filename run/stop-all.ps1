param(
    [int[]]$Ports = @(8761, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 9092, 9093, 9094, 9095, 9096, 9097, 9098)
)

foreach ($port in $Ports) {
    $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Host "Detenido puerto $port (PID $($conn.OwningProcess))" -ForegroundColor Yellow
    }
}
Write-Host "Servicios detenidos." -ForegroundColor Green
