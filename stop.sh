#!/usr/bin/env bash
# Stop and remove everything. -v deletes the database volume, so db/init
# re-seeds on the next run.
docker rm -f -v frontend cart-service order-service inventory-service product-service shop-postgres 2>/dev/null || true
docker network rm store-net 2>/dev/null || true
echo "Stopped and removed. The next ./run.sh will re-seed the database."
