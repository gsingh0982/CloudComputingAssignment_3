#!/usr/bin/env bash
set -e

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
docker run --name shop-postgres \
    -e POSTGRES_DB=shopdb \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=postgres \
    -p 127.0.0.1:5433:5432 \
    --mount "type=bind,source=$project_dir/db/init,target=/docker-entrypoint-initdb.d,readonly" \
    -d postgres:16

echo 'Run docker logs -f shop-postgres and wait for PostgreSQL to finish initialization before starting the services in IntelliJ.'
