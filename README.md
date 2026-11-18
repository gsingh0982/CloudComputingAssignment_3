# PROG3350 Assignment 3 - Product-service, database and storefront

Commands to run the product-service, the PostgreSQL database and the React storefront using plain `docker run`
commands. Docker Compose is not used, as agreed with the instructor.


## Ports
product-service 8080, PostgreSQL 5433 (container 5432),
storefront 3000


## Before you start
- Docker Desktop is running.
- Ports **3000, 8080 and 5433** are free.
- A terminal opened in the **project root**, the folder that contains `db/`, `productService/` and `frontend/`.
  Use **Git Bash, macOS Terminal or Linux**. The commands use `\` line breaks and `$(pwd)`, which do not work in
  PowerShell or cmd (see the PowerShell note at the end).


**1. Remove any old containers with the same names:**
```bash
docker rm -f -v storefront product_service shop-postgres
docker network rm demo-net
```

**2. Create the network the containers share:**
```bash
docker network create demo-net
```

**3. Run the database container.** It creates the `shopdb` database and seeds the tables and 12 products from
`db/init` on first start:
```bash
docker run --name shop-postgres --network demo-net \
-e POSTGRES_DB=shopdb -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
-p 5433:5432 \
-v "$(pwd)/db/init:/docker-entrypoint-initdb.d" \
-d postgres:16
```
**Windows Git Bash only:** put `MSYS_NO_PATHCONV=1` in front of `docker run`. Without it, Git Bash rewrites the
`/docker-entrypoint-initdb.d` path and the database starts with no tables.

**4. Check that the database is ready** (wait a few seconds first):
```bash
docker logs shop-postgres --tail 5
```
The last line should say `database system is ready to accept connections`.

**5. Build and run the product-service container:**
```bash
docker build -t product_service ./productService

docker run --name product_service --network demo-net \
-e DB_URL=jdbc:postgresql://shop-postgres:5432/shopdb \
-p 8080:8080 -d product_service
```
The service connects to the database by the container name `shop-postgres` on port 5432. It does not use
`localhost:5433`, because inside a container `localhost` means the container itself.

**6. Build and run the storefront container:**
```bash
docker build -t storefront ./frontend
docker run --name storefront -p 3000:3000 -d storefront
```

## Check that it works
```bash
docker ps
docker logs product_service --tail 15
curl http://localhost:8080/api/products
```
- `docker ps` lists `shop-postgres`, `product_service` and `storefront`.
- The product-service log contains `Started ProductServiceApplication`.
- `curl` returns **12 products**.

## URLs to open
- Storefront: http://localhost:3000
- All products: http://localhost:8080/api/products
- One product: http://localhost:8080/api/products/1

**What you should see at http://localhost:3000:** a grid of 12 products with pictures, names and prices. The
**USB-C Hub** is marked **Out of stock**.


## What this submission covers
This submission covers the **product-service, the database and the storefront**. The cart, checkout and orders
pages need the cart-service, order-service and inventory-service, which are owned by other team members and are not
part of this setup. Clicking **Add to cart** or **Orders** here shows an error message. Without the inventory
service, the storefront shows each product's own `quantity` as its stock.

## Reset the database
PostgreSQL only runs `db/init` the first time its data volume is created. To reset the data, remove the container
and its volume, then run **step 3 again** to start a fresh, re-seeded database:
```bash
docker rm -f -v shop-postgres
```

## Stop and start again
```bash
docker stop storefront product_service shop-postgres

docker start shop-postgres
docker start product_service
docker start storefront
```
Start the database first and give it a few seconds before starting `product_service`.

## Clean up everything
```bash
docker rm -f -v storefront product_service shop-postgres
docker network rm demo-net
```


