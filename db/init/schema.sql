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