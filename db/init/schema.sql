CREATE TABLE products(
    id BIGSERIAL PRIMARY KEY ,
    name VARCHAR(255) NOT NULL ,
    price DOUBLE PRECISION NOT NULL ,
    quantity INTEGER NOT NULL DEFAULT 0,
    image_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS inventory (
        id BIGSERIAL PRIMARY KEY,
        product_id BIGINT NOT NULL UNIQUE,
        available INTEGER NOT NULL DEFAULT 0,
        CHECK (available >= 0)
);
CREATE TABLE carts (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    CHECK (status IN ('OPEN', 'CHECKED_OUT'))
);

CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,

    FOREIGN KEY (cart_id) REFERENCES carts(id),
    CHECK (product_id > 0),
    CHECK (quantity > 0)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    total_price DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL
);
