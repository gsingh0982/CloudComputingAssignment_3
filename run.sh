#!/usr/bin/env bash
# PROG3350 Assignment Three - start the whole store
# Follows the Week 10 "Running the Docker Demo" format: one shared network,
# then each container started by hand. No Docker Compose.
#
# Usage:  ./run.sh           (from the project root, with Docker running)

set -e

# Builds an image only if that service has a Dockerfile yet, so the script
# still brings up the finished services while the rest are being written.
build_service() {
  local image="$1" folder="$2"
  if [ -f "$folder/Dockerfile" ]; then
    docker build -t "$image" "$folder"
    return 0
  fi
  echo "      skipping $folder (no Dockerfile yet)"
  return 1
}

# ---- Step 0: clean slate (safe to run every time) ----
echo
echo "[0/6] Removing anything left over..."
docker rm -f -v frontend cart-service order-service inventory-service product-service shop-postgres 2>/dev/null || true
docker network rm store-net 2>/dev/null || true

# ---- Step 1: the shared network ----
echo "[1/6] Creating network store-net..."
docker network create store-net >/dev/null

# ---- Step 2: PostgreSQL, seeded from db/init on first start ----
echo "[2/6] Starting PostgreSQL..."
docker run --name shop-postgres \
  --network store-net \
  -e POSTGRES_DB=shopdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  -v "$(pwd)/db/init:/docker-entrypoint-initdb.d" \
  -d postgres:16 >/dev/null

echo "      waiting for the database to accept connections..."
sleep 10
docker logs shop-postgres --tail 3

# ---- Step 3: build the images from each Dockerfile ----
echo
echo "[3/6] Building images (first run takes a few minutes)..."
has_product=0;   build_service store-product-service   ./productService    && has_product=1
has_inventory=0; build_service store-inventory-service ./inventory-service && has_inventory=1
has_order=0;     build_service store-order-service     ./order-service     && has_order=1
has_cart=0;      build_service store-cart-service      ./cart-service      && has_cart=1
has_frontend=0;  build_service store-frontend          ./frontend          && has_frontend=1

# ---- Step 4: the two services that call nobody ----
echo
echo "[4/6] Starting product-service and inventory-service..."
if [ "$has_product" = 1 ]; then
  docker run --name product-service --network store-net \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb \
    -e SPRING_DATASOURCE_USERNAME=postgres \
    -e SPRING_DATASOURCE_PASSWORD=postgres \
    -p 8080:8080 --restart on-failure -d store-product-service >/dev/null
fi

if [ "$has_inventory" = 1 ]; then
  docker run --name inventory-service --network store-net \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb \
    -e SPRING_DATASOURCE_USERNAME=postgres \
    -e SPRING_DATASOURCE_PASSWORD=postgres \
    -p 8082:8082 --restart on-failure -d store-inventory-service >/dev/null
fi

# ---- Step 5: order-service, then cart-service ----
# Containers reach each other by container name on store-net, not localhost.
echo "[5/6] Starting order-service and cart-service..."
if [ "$has_order" = 1 ]; then
  docker run --name order-service --network store-net \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb \
    -e SPRING_DATASOURCE_USERNAME=postgres \
    -e SPRING_DATASOURCE_PASSWORD=postgres \
    -e PRODUCT_SERVICE_URL=http://product-service:8080 \
    -e INVENTORY_SERVICE_URL=http://inventory-service:8082 \
    -p 8081:8081 --restart on-failure -d store-order-service >/dev/null
fi

if [ "$has_cart" = 1 ]; then
  docker run --name cart-service --network store-net \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://shop-postgres:5432/shopdb \
    -e SPRING_DATASOURCE_USERNAME=postgres \
    -e SPRING_DATASOURCE_PASSWORD=postgres \
    -e ORDER_SERVICE_URL=http://order-service:8081 \
    -p 8083:8083 --restart on-failure -d store-cart-service >/dev/null
fi

# ---- Step 6: the storefront ----
echo "[6/6] Starting the frontend..."
if [ "$has_frontend" = 1 ]; then
  docker run --name frontend --network store-net \
    -p 3000:3000 --restart on-failure -d store-frontend >/dev/null
else
  echo "      skipped - no frontend yet"
fi

echo
echo "Running containers:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

if [ "$has_frontend" = 1 ]; then
  echo
  echo "Open http://localhost:3000"
else
  echo
  echo "No frontend yet. Test the services directly, for example:"
  echo "  curl http://localhost:8080/api/products"
  echo "  curl http://localhost:8081/api/orders"
fi
echo "Stop everything with:  ./stop.sh"
echo
