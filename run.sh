#!/usr/bin/env bash
set -e

cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
project_dir="$(pwd)"
case "$(uname -s)" in
  MINGW*|MSYS*)
    project_dir="$(pwd -W)"
    export MSYS_NO_PATHCONV=1
    ;;
esac

for folder in productService inventory-service order-service cart-service frontend; do
  if [[ ! -f "$folder/Dockerfile" ]]; then
    echo "Missing $folder/Dockerfile. Get it from the service owner before starting the store." >&2
    exit 1
  fi
done

docker network create store-net
docker run -d --name store-db --network store-net -p 127.0.0.1:5433:5432 \
  -e POSTGRES_DB=store -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  --mount "type=bind,source=$project_dir/db/init,target=/docker-entrypoint-initdb.d,readonly" postgres:16

deadline=$((SECONDS + 60))
until docker exec store-db pg_isready -h 127.0.0.1 -U postgres -d store -q 2>/dev/null; do
  if (( SECONDS >= deadline )); then
    echo 'PostgreSQL did not become ready. Run docker logs store-db.' >&2
    exit 1
  fi
  sleep 1
done

docker build -t product-service ./productService
docker build -t inventory-service ./inventory-service
docker build -t order-service ./order-service
docker build -t cart-service ./cart-service
docker build -t frontend ./frontend

docker run -d --name product-service --network store-net -p 127.0.0.1:8080:8080 \
  --restart on-failure --env-file db.env product-service
docker run -d --name inventory-service --network store-net -p 127.0.0.1:8082:8082 \
  --restart on-failure --env-file db.env inventory-service
docker run -d --name order-service --network store-net -p 127.0.0.1:8081:8081 \
  --restart on-failure --env-file db.env -e PRODUCT_SERVICE_URL=http://product-service:8080 \
  -e INVENTORY_SERVICE_URL=http://inventory-service:8082 order-service
docker run -d --name cart-service --network store-net -p 127.0.0.1:8083:8083 \
  --restart on-failure --env-file db.env -e ORDER_SERVICE_URL=http://order-service:8081 cart-service
docker run -d --name frontend --network store-net -p 127.0.0.1:3000:80 frontend

docker ps --filter network=store-net
echo 'Open http://localhost:3000 once the Java services have started.'
