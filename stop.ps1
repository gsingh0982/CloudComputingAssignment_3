# PROG3350 Assignment Three - stop and remove everything.
# -v also deletes the database volume, so db/init re-seeds on the next run.

docker rm -f -v frontend cart-service order-service inventory-service product-service shop-postgres 2>$null
docker network rm store-net 2>$null

Write-Host "Stopped and removed. The next .\run.ps1 will re-seed the database."
