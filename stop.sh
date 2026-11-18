#!/usr/bin/env bash
# Removes store containers, their anonymous volumes, and the store network.
# Database data is deleted; the next run initializes it from db/init.
docker rm -f -v frontend cart-service order-service inventory-service product-service shop-postgres 2>/dev/null || true
docker network rm store-net 2>/dev/null || true
echo "Stopped and removed. The next ./run.sh will re-seed the database."
