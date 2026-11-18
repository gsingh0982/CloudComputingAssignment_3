# Cloud-Native E-Commerce Store

PROG3350 - Cloud Computing & Application Development
Assignment Three

A React storefront backed by four Spring Boot microservices and one PostgreSQL
database, all in Docker containers.

> Docker Compose is **not** used. Per the Week 10 lecture and the instructor's
> announcement, the system is started the way it was demonstrated in class:
> one shared Docker network, then each container started with `docker run`.

---

## Who built what

| Component | Owner |
| --- | --- |
| product-service | gsingh0982 |
| inventory-service | DavidJay109 |
| cart-service | mamir203626 |
| order-service | Favour Adesiyan |
| frontend | TBC |
| database schema + seed | shared |

---

## Requirements

Docker Desktop, running. Nothing else - no JDK, Maven, Node or PostgreSQL.
Each service is built from source inside Docker by its own multi-stage
Dockerfile.

---

## Running it

From the project root:

**Windows (PowerShell)**

```powershell
.\run.ps1
```

**macOS / Linux**

```bash
./run.sh
```

Then open <http://localhost:3000>

The first run takes a few minutes while Maven downloads dependencies inside
the build containers. Later runs are much faster.

### Stopping

```powershell
.\stop.ps1      # Windows
./stop.sh       # macOS / Linux
```

---

## Ports

| Container | Host port | Port inside the container |
| --- | --- | --- |
| frontend | 3000 | 3000 |
| product-service | 8080 | 8080 |
| order-service | 8081 | 8081 |
| inventory-service | 8082 | 8082 |
| cart-service | 8083 | 8083 |
| shop-postgres | 5433 | 5432 |

PostgreSQL is mapped to 5433 on the host so it does not clash with a
PostgreSQL already installed on the machine. Inside Docker it is still 5432,
and the containers reach it as `shop-postgres:5432`.

---

## How the containers find each other

All containers join a user-defined network called `store-net`, so each one is
reachable by its container name:

```
frontend  ->  cart-service:8083
cart      ->  order-service:8081
order     ->  product-service:8080  and  inventory-service:8082
all four  ->  shop-postgres:5432
```

Service addresses are never hard-coded. Each service reads them from
`application.properties`, and `run.ps1` / `run.sh` override them with `-e`
environment variables at `docker run` time.

---

## The database

`db/init` is mounted into the Postgres container at
`/docker-entrypoint-initdb.d`. PostgreSQL runs every `.sql` file there, in
name order, the first time the data volume is created:

- `schema.sql` - products, inventory, carts, cart_items, orders
- `seed.sql` - 12 products with images, an inventory row for each, and one
  product with 0 stock so the out-of-stock case can be demonstrated

Hibernate does not create any tables. Every service runs with
`spring.jpa.hibernate.ddl-auto=validate`.

### Resetting the database

Init scripts run **only** when the data volume is created. If you edit
`seed.sql` and nothing changes, the old volume is still there. The stop
scripts delete it:

```powershell
.\stop.ps1      # removes containers AND the volume
.\run.ps1       # re-seeds from db/init
```

---

## Checking it works

```bash
docker ps                                            # six containers up

curl http://localhost:8080/api/products              # catalog
curl http://localhost:8082/api/inventory             # stock levels
curl http://localhost:8081/api/orders                # orders (empty at first)
```

Full checkout flow:

1. Open <http://localhost:3000>
2. Add two products to the cart
3. Check out - order numbers appear and stock goes down
4. Try to buy more than the available stock - checkout is refused with a
   clear message and no order is saved

### Useful commands

```bash
docker logs -f order-service        # live log for one service
docker ps -a                        # including stopped containers
docker exec shop-postgres psql -U postgres -d shopdb -c "SELECT * FROM orders;"
```

---

## The checkout flow

```
frontend -> cart-service    POST /api/carts/{id}/checkout
cart     -> order-service   POST /api/orders
order    -> product-service GET  /api/products/{id}      (confirm + price)
order    -> inventory       POST /api/inventory/reserve  (all lines or none)
```

Stock is reserved before any order is saved. If any line is short,
inventory-service returns 409, reserves nothing, and no order is written.

Services call each other with Spring's `RestClient`. No service reads or
writes another service's tables.
