$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

docker info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop must be running.' }

foreach ($name in 'frontend', 'cart-service', 'order-service', 'inventory-service', 'product-service', 'store-db') {
    docker container inspect $name *> $null
    if ($LASTEXITCODE -eq 0) {
        docker rm -f $name
        if ($LASTEXITCODE -ne 0) { throw "Could not remove $name." }
    }
}

docker network inspect store-net *> $null
if ($LASTEXITCODE -eq 0) {
    docker network rm store-net
    if ($LASTEXITCODE -ne 0) { throw 'Could not remove store-net.' }
}
