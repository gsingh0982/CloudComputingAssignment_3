$ErrorActionPreference = 'Stop'

$initPath = Join-Path $PSScriptRoot 'db/init'
docker run --name shop-postgres `
    -e POSTGRES_DB=shopdb `
    -e POSTGRES_USER=postgres `
    -e POSTGRES_PASSWORD=postgres `
    -p 127.0.0.1:5433:5432 `
    --mount "type=bind,source=$initPath,target=/docker-entrypoint-initdb.d,readonly" `
    -d postgres:16

if ($LASTEXITCODE -ne 0) {
    throw 'Could not start shop-postgres. Check Docker Desktop and whether the container already exists.'
}

Write-Host 'Run docker logs -f shop-postgres and wait for PostgreSQL to finish initialization before starting the services in IntelliJ.'
