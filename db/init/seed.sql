INSERT INTO products (name, price, quantity, image_url) VALUES
                                                            ('Wireless Headphones',   79.99, 25, '/images/headphones.jpg'),
                                                            ('Mechanical Keyboard',  119.50, 12, '/images/keyboard.jpg'),
                                                            ('USB-C Hub',             34.00,  0, '/images/usb-hub.jpg'),
                                                            ('Wireless Mouse',        29.99, 40, '/images/mouse.jpg'),
                                                            ('27-inch Monitor',      289.00,  8, '/images/monitor.jpg'),
                                                            ('Laptop Stand',          45.00, 30, '/images/laptop-stand.jpg'),
                                                            ('Webcam 1080p',          59.99, 18, '/images/webcam.jpg'),
                                                            ('Bluetooth Speaker',     64.50, 22, '/images/speaker.jpg'),
                                                            ('Portable SSD 1TB',     119.00, 15, '/images/ssd.jpg'),
                                                            ('Phone Charger 65W',     39.99, 50, '/images/charger.jpg'),
                                                            ('Desk Lamp',             32.00, 27, '/images/desk-lamp.jpg'),
                                                            ('Backpack',              69.00, 10, '/images/backpack.jpg');


INSERT INTO inventory (product_id, available) VALUES
                                                  (1, 25),
                                                  (2, 12),
                                                  (3, 0),
                                                  (4, 40),
                                                  (5, 8),
                                                  (6, 30),
                                                  (7, 18),
                                                  (8, 22),
                                                  (9, 15),
                                                  (10, 50),
                                                  (11, 27),
                                                  (12, 10)
    ON CONFLICT (product_id) DO NOTHING;