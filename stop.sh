#!/usr/bin/env bash
set -e

docker info --format '{{.ServerVersion}}' >/dev/null

for name in frontend cart-service order-service inventory-service product-service store-db; do
  if docker container inspect "$name" >/dev/null 2>&1; then
    docker rm -f "$name"
  fi
done

if docker network inspect store-net >/dev/null 2>&1; then
  docker network rm store-net
fi
