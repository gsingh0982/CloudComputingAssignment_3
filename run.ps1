# Starts services with available Dockerfiles. Run from the project root.
# Removes existing store containers and their anonymous volumes, including database data.

$ErrorActionPreference = "Stop"

# Missing Dockerfiles allow a partial development setup.
function Build-Service($image, $folder) {
    if (Test-Path (Join-Path $folder "Dockerfile")) {
        docker build -t $image $folder
        return $true
    }
    Write-Host "      skipping $folder (no Dockerfile yet)" -ForegroundColor Yellow
    return $false
}

Write-Host "`n[0/6] Removing anything left over..." -ForegroundColor Cyan
docker rm -f -v frontend cart-service order-service inventory-service product-service shop-postgres 2>$null
docker network rm store-net 2>$null

Write-Host "[1/6] Creating network store-net..." -ForegroundColor Cyan
docker network create store-net | Out-Null

Write-Host "[2/6] Starting PostgreSQL..." -ForegroundColor Cyan
docker run --name shop-postgres `
  --network store-net `
  -e POSTGRES_DB=shopdb `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5433:5432 `
  -v "${PWD}/db/init:/docker-entrypoint-initdb.d" `
  -d postgres:16 | Out-Null

Write-Host "      waiting for the database to accept connections..."
Start-Sleep -Seconds 10
docker logs shop-postgres --tail 3

Write-Host "`n[3/6] Building images (first run takes a few minutes)..." -ForegroundColor Cyan
$hasProduct   = Build-Service store-product-service   ./productService
$hasInventory = Build-Service store-inventory-service ./inventory-service
$hasOrder     = Build-Service store-order-service     ./order-service
$hasCart      = Build-Service store-cart-service      ./cart-service
$hasFrontend  = Build-Service store-frontend          ./frontend

Write-Host "`n[4/6] Starting product-service and inventory-service..." -ForegroundColor Cyan
if ($hasProduct) {
  docker run --name product-service --network store-net `
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb `
    -e SPRING_DATASOURCE_USERNAME=postgres `
    -e SPRING_DATASOURCE_PASSWORD=postgres `
    -p 8080:8080 --restart on-failure -d store-product-service | Out-Null
}

if ($hasInventory) {
  docker run --name inventory-service --network store-net `
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb `
    -e SPRING_DATASOURCE_USERNAME=postgres `
    -e SPRING_DATASOURCE_PASSWORD=postgres `
    -p 8082:8082 --restart on-failure -d store-inventory-service | Out-Null
}

Write-Host "[5/6] Starting order-service and cart-service..." -ForegroundColor Cyan
if ($hasOrder) {
  docker run --name order-service --network store-net `
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb `
    -e SPRING_DATASOURCE_USERNAME=postgres `
    -e SPRING_DATASOURCE_PASSWORD=postgres `
    -e PRODUCT_SERVICE_URL=http://product-service:8080 `
    -e INVENTORY_SERVICE_URL=http://inventory-service:8082 `
    -p 8081:8081 --restart on-failure -d store-order-service | Out-Null
}

if ($hasCart) {
  docker run --name cart-service --network store-net `
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb `
    -e SPRING_DATASOURCE_USERNAME=postgres `
    -e SPRING_DATASOURCE_PASSWORD=postgres `
    -e ORDER_SERVICE_URL=http://order-service:8081 `
    -p 8083:8083 --restart on-failure -d store-cart-service | Out-Null
}

Write-Host "[6/6] Starting the frontend..." -ForegroundColor Cyan
if ($hasFrontend) {
  docker run --name frontend --network store-net `
    -p 3000:3000 --restart on-failure -d store-frontend | Out-Null
} else {
  Write-Host "      skipped - no frontend yet" -ForegroundColor Yellow
}

Write-Host "`nRunning containers:" -ForegroundColor Green
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

if ($hasFrontend) {
  Write-Host "`nOpen http://localhost:3000" -ForegroundColor Green
} else {
  Write-Host "`nNo frontend yet. Test the services directly, for example:" -ForegroundColor Yellow
  Write-Host "  curl http://localhost:8080/api/products"
  Write-Host "  curl http://localhost:8081/api/orders"
}
Write-Host "Stop everything with:  .\stop.ps1`n"
