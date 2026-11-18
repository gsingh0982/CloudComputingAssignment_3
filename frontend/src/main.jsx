import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api } from './api.js';
import './styles.css';

const currency = new Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD' });
const price = (amount) => currency.format(amount);
const stockByProduct = (rows) => new Map(rows.map(row => [row.productId, row.available]));

function ProductCard({ product, available, busy, onAdd }) {
  const [quantity, setQuantity] = useState('1');
  const inStock = available > 0;
  const inputId = `quantity-${product.id}`;
  const parsed = Number(quantity);
  const valid = Number.isInteger(parsed) && parsed >= 1 && parsed <= 2147483647;

  return (
    <article className="product-card">
      <div className="product-photo">
        <img src={product.imageUrl} alt={product.name} width="360" height="360" />
      </div>
      <div className="product-details">
        <div className="product-title">
          <h2>{product.name}</h2>
          <span>{price(product.price)}</span>
        </div>
        <p className={inStock ? 'stock' : 'stock unavailable'}>
          {available === undefined ? 'Stock unavailable' : inStock ? `${available} in stock` : 'Out of stock'}
        </p>
        <form className="add-row" onSubmit={event => {
          event.preventDefault();
          if (valid && inStock && !busy) onAdd(product.id, parsed);
        }}>
          <label htmlFor={inputId}>Qty</label>
          <input id={inputId} aria-label={`Quantity for ${product.name}`} type="number"
            min="1" max="2147483647" value={quantity}
            onChange={event => setQuantity(event.target.value)} disabled={!inStock || busy} required />
          <button type="submit" disabled={!inStock || !valid || busy}
            aria-label={`Add ${product.name} to cart`}>Add to cart</button>
        </form>
      </div>
    </article>
  );
}

function Cart({ cart, products, busy, notice, orders, onRemove, onCheckout }) {
  const items = cart?.items ?? [];
  const catalog = new Map(products.map(product => [product.id, product]));
  const total = items.reduce((sum, item) => sum + Math.round(catalog.get(item.productId).price * 100) * item.quantity, 0);
  return (
    <aside className="cart" id="cart" aria-labelledby="cart-title">
      <div className="cart-heading">
        <h2 id="cart-title">Your cart</h2>
        <span>{items.reduce((sum, item) => sum + item.quantity, 0)} items</span>
      </div>
      {notice && <p className="error" role="alert">{notice}</p>}
      {orders.length > 0 && (
        <div className="confirmation" role="status">
          <strong>Order confirmed</strong>
          <ul>{orders.map(order => (
            <li key={order.id}>Order #{order.id} · {price(order.totalPrice)}</li>
          ))}</ul>
        </div>
      )}
      {items.length === 0 ? <p className="empty-cart">Your cart is empty.</p> : (
        <ul className="cart-items">
          {items.map(item => {
            const product = catalog.get(item.productId);
            return (
              <li key={item.id}>
                <div>
                  <strong>{product.name}</strong>
                  <div className="line-quantity">{item.quantity} × {price(product.price)}</div>
                </div>
                <button className="remove" type="button" disabled={busy} onClick={() => onRemove(item.id)}
                  aria-label={`Remove ${product.name} from cart`}>Remove</button>
              </li>
            );
          })}
        </ul>
      )}
      <div className="cart-total"><span>Subtotal</span><strong>{price(total / 100)}</strong></div>
      <button className="checkout" type="button" disabled={busy || items.length === 0} onClick={onCheckout}>
        Checkout
      </button>
    </aside>
  );
}

function App() {
  const [products, setProducts] = useState([]);
  const [stock, setStock] = useState(new Map());
  const [catalogStatus, setCatalogStatus] = useState('loading');
  const [cart, setCart] = useState(null);
  const [orders, setOrders] = useState([]);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  async function loadCatalog() {
    setCatalogStatus('loading');
    try {
      const [catalog, inventory] = await Promise.all([api.products(), api.inventory()]);
      setProducts(catalog.toSorted((first, second) => first.id - second.id));
      setStock(stockByProduct(inventory));
      setCatalogStatus('ready');
    } catch {
      setCatalogStatus('error');
    }
  }

  useEffect(() => { loadCatalog(); }, []);

  async function runCartAction(action, fallback) {
    setBusy(true);
    setNotice('');
    try {
      await action();
    } catch (error) {
      setNotice(error.status === 409
        ? 'There is not enough stock to complete checkout. Remove the affected item or add it again with a smaller quantity.'
        : fallback);
    } finally {
      setBusy(false);
    }
  }

  function addItem(productId, quantity) {
    runCartAction(async () => {
      const currentCart = cart ?? await api.createCart();
      setCart(currentCart);
      setCart(await api.addItem(currentCart.id, productId, quantity));
      setOrders([]);
    }, 'Could not add this item. Check that cart-service is running and try again.');
  }

  function removeItem(itemId) {
    runCartAction(async () => {
      await api.removeItem(cart.id, itemId);
      setCart({ ...cart, items: cart.items.filter(item => item.id !== itemId) });
    }, 'Could not remove this item. Try again.');
  }

  function checkout() {
    runCartAction(async () => {
      setOrders(await api.checkout(cart.id));
      setCart(null);
      try {
        setStock(stockByProduct(await api.inventory()));
      } catch {
        setNotice('The order is confirmed. Reload the page to see updated stock.');
      }
    }, 'Could not confirm checkout. Check the order history before trying again.');
  }

  return (
    <>
      <header className="topbar">
        <div className="topbar-inner">
          <a className="brand" href="#catalog">Tech Supply</a>
          <a className="cart-link" href="#cart">Cart <span>{cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0}</span></a>
        </div>
      </header>
      <main className="store" id="catalog">
        <section className="catalog" aria-labelledby="catalog-title">
          <div className="catalog-heading">
            <h1 id="catalog-title">Shop products</h1>
            {catalogStatus === 'ready' && <span>{products.length} products</span>}
          </div>
          {catalogStatus === 'loading' && <p role="status">Loading products…</p>}
          {catalogStatus === 'error' && <div className="load-error" role="alert">
            <p>Could not load the catalog. Check that product-service and inventory-service are running.</p>
            <button type="button" onClick={loadCatalog}>Try again</button>
          </div>}
          {catalogStatus === 'ready' && (
            <div className="product-grid">
              {products.map(product => <ProductCard key={product.id} product={product}
                available={stock.get(product.id)} busy={busy} onAdd={addItem} />)}
            </div>
          )}
        </section>
        <Cart cart={cart} products={products} busy={busy} notice={notice} orders={orders}
          onRemove={removeItem} onCheckout={checkout} />
      </main>
    </>
  );
}

createRoot(document.getElementById('root')).render(<App />);
