# Cloud-native e-commerce store

PROG3350 Assignment Three. Four Spring Boot services share a PostgreSQL database, with each service owning its own tables. The planned React storefront calls the APIs directly.

## Current status

The repository contains all four backend services, database initialization scripts, and Dockerfiles for order-service and cart-service. Product-service and inventory-service Dockerfiles, the React frontend, and image assets are still missing. The run scripts skip components without Dockerfiles, so they currently provide a partial setup. Full checkout requires product-service and inventory-service.

The scripts use a shared Docker network and individual `docker run` commands, following the team's Week 10 startup approach. This repository does not contain a Compose file.

## Ownership

| Component | Owner |
| --- | --- |
| product-service | gsingh0982 |
| inventory-service | DavidJay109 |
| cart-service | mamir203626 |
| order-service | Favour Adesiyan |
| frontend | Unassigned |
| Database schema and seed data | Shared |

## Run

Start Docker Desktop, then run from the project root. Available Dockerfiles build from source inside Docker; local Java, Maven, Node, and PostgreSQL installations are not required.

Windows PowerShell:

```powershell
.\run.ps1
```

macOS or Linux:

```bash
bash run.sh
```

Each run removes the existing store containers and their anonymous volumes, including database data. PostgreSQL then initializes a fresh database from `db/init`. The scripts wait ten seconds after starting PostgreSQL; this is a fixed delay, not a readiness check. Check container status and logs before sending requests.

The frontend URL will be http://localhost:3000 once the frontend is implemented and running.

## Ports

| Container | Host port | Container port |
| --- | --- | --- |
| frontend (pending) | 3000 | 3000 |
| product-service (Dockerfile pending) | 8080 | 8080 |
| order-service | 8081 | 8081 |
| inventory-service (Dockerfile pending) | 8082 | 8082 |
| cart-service | 8083 | 8083 |
| shop-postgres | 5433 | 5432 |

Containers use names on `store-net` to reach each other. Order-service calls `product-service:8080` and `inventory-service:8082`; cart-service calls `order-service:8081`. All backend services connect to `shop-postgres:5432`. The scripts supply these addresses through environment variables.

Browser requests use the published localhost ports, not Docker container names.

## Database and reset

PostgreSQL 16 reads `db/init/schema.sql` and `db/init/seed.sql` through `/docker-entrypoint-initdb.d` when it initializes an empty data volume. The seed defines 12 products with image paths and matching inventory rows, including one zero-stock product. Image files are not included yet.

Each service uses `spring.jpa.hibernate.ddl-auto=validate`; SQL scripts define the schema.

To remove containers, database data, and the network:

```powershell
.\stop.ps1
```

```bash
bash stop.sh
```

Run the startup script again to recreate and seed the database. Both startup and stop scripts delete existing database data.

## Verification

Inspect containers and order-service logs:

```bash
docker ps -a
docker logs order-service
docker exec shop-postgres psql -U postgres -d shopdb -c "SELECT * FROM orders;"
```

Check the order endpoint after it starts:

```bash
curl http://localhost:8081/api/orders
```

In Windows PowerShell, use `curl.exe` for these curl commands.

After adding and starting the missing services:

```bash
curl http://localhost:8080/api/products
curl http://localhost:8082/api/inventory
```

Once the frontend is available, verify the full flow:

1. Add two products to a cart and check out.
2. Confirm that each cart line creates an order and reduces inventory.
3. Request more stock than is available. Confirm a clear error, no new orders, and no inventory deduction.

## Checkout

```text
frontend -> cart-service     POST /api/carts/{id}/checkout
cart     -> order-service    POST /api/orders
order    -> product-service  GET /api/products/{id}
order    -> inventory-service POST /api/inventory/reserve
```

Order-service fetches current prices, reserves inventory, and saves one confirmed order per cart line. Inventory reserves all requested lines or returns 409 without deductions. Cart-service marks the cart checked out after receiving the orders.

The services communicate through Spring RestClient and access only their own tables. Checkout is not a distributed transaction: a failure to save orders does not undo a completed inventory reservation.
