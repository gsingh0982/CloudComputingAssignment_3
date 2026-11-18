$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

function Invoke-Docker {
    docker @args
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed: docker $($args -join ' ')"
    }
}

foreach ($folder in 'productService', 'inventory-service', 'order-service', 'cart-service', 'frontend') {
    if (-not (Test-Path -LiteralPath (Join-Path $PSScriptRoot "$folder/Dockerfile") -PathType Leaf)) {
        throw "Missing $folder/Dockerfile. Get it from the service owner before starting the store."
    }
}

Push-Location -LiteralPath $PSScriptRoot
try {
    Invoke-Docker network create store-net
    $initPath = Join-Path $PSScriptRoot 'db/init'
    Invoke-Docker run -d --name store-db --network store-net -p '127.0.0.1:5433:5432' `
        -e POSTGRES_DB=store -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres `
        --mount "type=bind,source=$initPath,target=/docker-entrypoint-initdb.d,readonly" postgres:16

    $deadline = (Get-Date).AddSeconds(60)
    do {
        docker exec store-db pg_isready -h 127.0.0.1 -U postgres -d store -q 2>$null
        if ($LASTEXITCODE -eq 0) { break }
        if ((Get-Date) -ge $deadline) {
            throw 'PostgreSQL did not become ready. Run docker logs store-db.'
        }
        Start-Sleep -Seconds 1
    } while ($true)

    Invoke-Docker build -t product-service ./productService
    Invoke-Docker build -t inventory-service ./inventory-service
    Invoke-Docker build -t order-service ./order-service
    Invoke-Docker build -t cart-service ./cart-service
    Invoke-Docker build -t frontend ./frontend

    Invoke-Docker run -d --name product-service --network store-net -p '127.0.0.1:8080:8080' `
        --restart on-failure --env-file db.env product-service
    Invoke-Docker run -d --name inventory-service --network store-net -p '127.0.0.1:8082:8082' `
        --restart on-failure --env-file db.env inventory-service
    Invoke-Docker run -d --name order-service --network store-net -p '127.0.0.1:8081:8081' `
        --restart on-failure --env-file db.env -e PRODUCT_SERVICE_URL=http://product-service:8080 `
        -e INVENTORY_SERVICE_URL=http://inventory-service:8082 order-service
    Invoke-Docker run -d --name cart-service --network store-net -p '127.0.0.1:8083:8083' `
        --restart on-failure --env-file db.env -e ORDER_SERVICE_URL=http://order-service:8081 cart-service
    Invoke-Docker run -d --name frontend --network store-net -p '127.0.0.1:3000:80' frontend

    Invoke-Docker ps --filter network=store-net
    Write-Host 'Open http://localhost:3000 once the Java services have started.'
} finally {
    Pop-Location
}
