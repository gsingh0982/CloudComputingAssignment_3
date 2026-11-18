# Cloud-native e-commerce store

PROG3350 Assignment Three. Four Spring Boot services share a PostgreSQL database, with each service owning its own tables. The React storefront displays the catalog, stock, and cart.

## Start PostgreSQL

Start Docker Desktop. From the project root, run `.\run.ps1` in Windows PowerShell or `bash run.sh` on macOS or Linux. This starts `postgres:16` on `localhost:5433` and mounts `db/init` into the container. On a fresh volume, PostgreSQL creates the tables and seeds 12 products and their inventory.

```text
docker logs -f shop-postgres
```

On the first run, wait for initialization to finish and PostgreSQL to report that it is ready to accept connections. Press Ctrl+C to leave the log view.

## Run the services in IntelliJ

Open the project in IntelliJ IDEA with JDK 21. Import each service's `pom.xml` as a Maven project if IntelliJ hasn't already linked it. Run the main classes below in order using the Run button beside each class. Wait for each app to finish starting before launching the next.

| Maven folder | Main class | Host port | Owner |
| --- | --- | --- | --- |
| `productService` | `ProductServiceApplication` | 8080 | gsingh0982 |
| `inventory-service` | `InventoryServiceApplication` | 8082 | DavidJay109 |
| `order-service` | `OrderServiceApplication` | 8081 | Favour Adesiyan |
| `cart-service` | `CartServiceApplication` | 8083 | mamir203626 |
| `frontend` | React storefront | 3000 | To confirm |
| PostgreSQL | `shop-postgres` container | 5433 | Shared |

The Java apps connect to `jdbc:postgresql://localhost:5433/shopdb` and use their existing localhost defaults for service calls.

## Run the storefront

From the project root, use Node.js 20.19+ (or Node.js 22.12+):

```text
cd frontend
npm ci
npm run dev
```

Open http://localhost:3000. Add a product to the cart, check the subtotal, and check out. To demonstrate the out-of-stock response, add more units of an in-stock product than the catalog shows, then try checkout.

The storefront reads names and prices from product-service and current stock from inventory-service. It sends cart actions and checkout to cart-service. Product images live in `frontend/public/images/` and their paths come from the product API.

## Product API

Product-service retains the Assignment Two write and search routes. Create with `POST /api/products`, update with `PUT /api/products/{id}`, and remove with `DELETE /api/products/{id}`. Search by name with `GET /api/products/search?name=keyboard` or by price with `GET /api/products/search/price?min=20&max=100`.

Product writes accept `name`, `price`, `quantity`, and an optional `imageUrl`. Inventory-service owns the live stock counts.

## Verify checkout through the API

Use a browser or an HTTP client to check the seeded products at `GET http://localhost:8080/api/products`, stock at `GET http://localhost:8082/api/inventory`, and orders at `GET http://localhost:8081/api/orders`.

1. Create a cart with `POST http://localhost:8083/api/carts`.
2. Add a line using `POST /api/carts/{id}/items` with JSON `{"productId":1,"quantity":1}`.
3. Call `POST /api/carts/{id}/checkout`. Check that it returns 201, creates an order, and reduces product 1's stock by one.
4. Create a new cart and repeat with product 3, which has zero stock in the seed. Check that checkout returns 409 and leaves orders and inventory unchanged.

Order-service looks up the current prices, reserves inventory, and saves one confirmed order per cart line. Cart-service marks the cart checked out after receiving those orders.

## Stop or reset

Stop the Java apps from IntelliJ. Run `docker stop shop-postgres` to stop PostgreSQL and keep its data. Resume it with `docker start shop-postgres`.

To reset the database, run `.\stop.ps1` on Windows or `bash stop.sh` on macOS or Linux. This deletes the database container and its volume. Run the startup script again to recreate the seeded database.
