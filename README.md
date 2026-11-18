# Cloud-native e-commerce store

PROG3350 Assignment Three. Four Spring Boot services share PostgreSQL, with each service owning its own tables. The React storefront reads the catalog and stock and sends cart actions to cart-service.

## Run the six containers

Start Docker Desktop, unzip the project, and open a terminal in the project root. Ports 3000, 5433, and 8080–8083 must be free. Stop any earlier IntelliJ runs, frontend dev server, or local database container using those ports.

Run `.\run.ps1` in PowerShell or `bash run.sh` on macOS, Linux, or Git Bash. The scripts follow the commands below and wait for PostgreSQL before building and starting the apps. Everything builds inside Docker. If a run was interrupted, use the shutdown steps before trying again.

| Container | Host port | Container port | Owner |
| --- | --- | --- | --- |
| `store-db` | 5433 | 5432 | Shared |
| `product-service` | 8080 | 8080 | gsingh0982 |
| `inventory-service` | 8082 | 8082 | DavidJay109 |
| `order-service` | 8081 | 8081 | Favour Adesiyan |
| `cart-service` | 8083 | 8083 | mamir203626 |
| `frontend` | 3000 | 80 | To confirm |

A fresh checkout needs a Dockerfile in each of the five app folders. If product or inventory is missing its Dockerfile, get it from the service owner before starting the full stack. Include a `.dockerignore` in each build context to exclude local build output and dependencies.

### Manual commands (PowerShell)

Create the network and start the seeded database.

```powershell
$dbInit = Join-Path (Get-Location).Path 'db/init'
docker network create store-net
docker run -d --name store-db --network store-net -p 127.0.0.1:5433:5432 -e POSTGRES_DB=store -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres --mount "type=bind,source=$dbInit,target=/docker-entrypoint-initdb.d,readonly" postgres:16
docker logs store-db --tail 20
```

Wait for PostgreSQL to finish initialization and report that it is ready. The SQL in `db/init` seeds 12 products and 12 inventory rows on the first start. Product 3 has zero stock. The root `db.env` gives the Java containers the database address and demo login.

Build the five images. The `productService` directory builds the image named `product-service`.

```powershell
docker build -t product-service ./productService
docker build -t inventory-service ./inventory-service
docker build -t order-service ./order-service
docker build -t cart-service ./cart-service
docker build -t frontend ./frontend
```

Start the Java services and frontend on `store-net`.

```powershell
docker run -d --name product-service --network store-net -p 127.0.0.1:8080:8080 --restart on-failure --env-file db.env product-service
docker run -d --name inventory-service --network store-net -p 127.0.0.1:8082:8082 --restart on-failure --env-file db.env inventory-service
docker run -d --name order-service --network store-net -p 127.0.0.1:8081:8081 --restart on-failure --env-file db.env -e PRODUCT_SERVICE_URL=http://product-service:8080 -e INVENTORY_SERVICE_URL=http://inventory-service:8082 order-service
docker run -d --name cart-service --network store-net -p 127.0.0.1:8083:8083 --restart on-failure --env-file db.env -e ORDER_SERVICE_URL=http://order-service:8081 cart-service
docker run -d --name frontend --network store-net -p 127.0.0.1:3000:80 frontend
```

Open http://localhost:3000 once the Java services finish starting. Java containers reach each other by name on `store-net`. The browser reaches the services through their published localhost ports.

## Verify the demo

1. Run `docker ps --filter network=store-net` and check that all six containers are up.
2. Open the storefront and check that the 12 products load with photos, prices, and stock counts.
3. Add two products and check out. Confirm that two order numbers appear, two orders were saved, and stock went down.
4. Add more units of an in-stock product than the store shows are available. Checkout should display an error and leave orders and stock unchanged.

```powershell
docker exec store-db psql -U postgres -d store -c "SELECT * FROM orders;"
docker exec store-db psql -U postgres -d store -c "SELECT * FROM inventory ORDER BY product_id;"
```

Product-service also supports `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`, name search at `/api/products/search?name=keyboard`, and price search at `/api/products/search/price?min=20&max=100`. Product writes accept `name`, `price`, `quantity`, and an optional `imageUrl`. Inventory-service owns live stock counts.

## Database reset and shutdown

For a fresh seed, run `docker rm -f -v store-db` while the database container exists, then repeat the database start command. This deletes the database volume and all checkout data. Stop and restart the other services around a reset if they are running.

To remove the containers and network, run `.\stop.ps1` in PowerShell or `bash stop.sh` in Bash. This leaves the database volume behind. To pause and later resume the same containers and database, use `docker stop frontend cart-service order-service inventory-service product-service store-db`, then `docker start store-db product-service inventory-service order-service cart-service frontend`.
