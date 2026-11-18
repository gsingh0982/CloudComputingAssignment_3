import { useCallback, useEffect, useMemo, useState } from 'react'
import * as api from './api.js'

const money = (n) => `$${Number(n).toFixed(2)}`
const CART_KEY = 'cartId'

// Small inline image used when a product image is missing or fails to load.
const PLACEHOLDER =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='300'><rect width='100%' height='100%' fill='%23e8eef0'/><text x='50%' y='52%' text-anchor='middle' font-family='sans-serif' font-size='18' fill='%2395a5ab'>No image</text></svg>"

function stockInfo(available) {
  if (available <= 0) return { label: 'Out of stock', cls: 'out' }
  if (available <= 5) return { label: `Only ${available} left`, cls: 'low' }
  return { label: `${available} in stock`, cls: 'in' }
}

export default function App() {
  const [page, setPage] = useState('shop') // shop | orders
  const [products, setProducts] = useState([])
  const [stock, setStock] = useState({}) // productId -> available
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')

  const [cartId, setCartId] = useState(() => localStorage.getItem(CART_KEY))
  const [cartItems, setCartItems] = useState([])
  const [cartOpen, setCartOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const [toast, setToast] = useState(null) // {type, text}
  const [result, setResult] = useState(null) // {orders} after checkout
  const [checkoutError, setCheckoutError] = useState('')

  const notify = useCallback((type, text) => {
    setToast({ type, text })
    setTimeout(() => setToast(null), 3500)
  }, [])

  // Load products + stock together so cards can show both.
  const loadCatalog = useCallback(async () => {
    try {
      const prods = await api.getProducts()
      setProducts(prods)
      try {
        const inv = await api.getInventory()
        setStock(Object.fromEntries(inv.map((i) => [i.productId, i.available])))
      } catch {
        // inventory-service is not running: fall back to the quantity stored on each product
        setStock(Object.fromEntries(prods.map((p) => [p.id, p.quantity])))
      }
      setLoadError('')
    } catch (e) {
      setLoadError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  // Make sure we have a cart; reload it from the cart-service.
  const loadCart = useCallback(async () => {
    if (!cartId) return setCartItems([])
    try {
      const cart = await api.getCart(cartId)
      if (cart.status === 'CHECKED_OUT') {
        localStorage.removeItem(CART_KEY)
        setCartId(null)
        setCartItems([])
      } else {
        setCartItems(cart.items || [])
      }
    } catch (e) {
      if (e.status === 404) {
        localStorage.removeItem(CART_KEY)
        setCartId(null)
      }
      setCartItems([])
    }
  }, [cartId])

  useEffect(() => { loadCatalog() }, [loadCatalog])
  useEffect(() => { loadCart() }, [loadCart])

  const byId = useMemo(() => Object.fromEntries(products.map((p) => [p.id, p])), [products])

  const lines = cartItems.map((it) => {
    const p = byId[it.productId]
    return { ...it, name: p?.name ?? `Product #${it.productId}`, price: p?.price ?? 0, imageUrl: p?.imageUrl }
  })
  const total = lines.reduce((s, l) => s + l.price * l.quantity, 0)
  const count = lines.reduce((s, l) => s + l.quantity, 0)

  async function ensureCart() {
    if (cartId) return cartId
    const cart = await api.createCart()
    localStorage.setItem(CART_KEY, cart.id)
    setCartId(cart.id)
    return cart.id
  }

  async function handleAdd(product) {
    setBusy(true)
    try {
      const id = await ensureCart()
      await api.addToCart(id, product.id, 1)
      const cart = await api.getCart(id)
      setCartItems(cart.items || [])
      notify('ok', `${product.name} added to cart`)
    } catch (e) {
      notify('err', e.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove(itemId) {
    setBusy(true)
    try {
      await api.removeFromCart(cartId, itemId)
      setCartItems((items) => items.filter((i) => i.id !== itemId))
    } catch (e) {
      notify('err', e.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleCheckout() {
    setBusy(true)
    setCheckoutError('')
    try {
      const orders = await api.checkout(cartId)
      setResult({ orders: Array.isArray(orders) ? orders : [orders] })
      localStorage.removeItem(CART_KEY)
      setCartId(null)
      setCartItems([])
      setCartOpen(false)
      loadCatalog() // stock went down
    } catch (e) {
      if (e.status === 409) {
        setCheckoutError(e.message.startsWith('Request failed')
          ? 'Sorry, one or more items are out of stock or short on stock. Nothing was ordered — please adjust your cart.'
          : e.message)
      } else if (e.status === 400) {
        setCheckoutError('Your cart is empty.')
      } else {
        setCheckoutError(e.message)
      }
      loadCatalog()
    } finally {
      setBusy(false)
    }
  }

  const visible = products.filter((p) => p.name.toLowerCase().includes(search.toLowerCase()))

  return (
    <>
      <header className="topbar">
        <div className="wrap topbar-inner">
          <button className="brand" onClick={() => setPage('shop')}>
            Cloud Store
          </button>
          <nav className="nav">
            <button className={page === 'shop' ? 'active' : ''} onClick={() => setPage('shop')}>Shop</button>
            <button className={page === 'orders' ? 'active' : ''} onClick={() => setPage('orders')}>My orders</button>
          </nav>
          <button className="cart-btn" onClick={() => { setCheckoutError(''); setCartOpen(true) }} aria-label="Open cart">
            Cart
            {count > 0 && <b className="badge">{count}</b>}
          </button>
        </div>
      </header>

      <main className="wrap">
        {page === 'shop' ? (
          <>
            <section className="page-head">
              <div>
                <h1>Products</h1>
                <p>{products.length} items</p>
              </div>
              <input
                className="search"
                type="search"
                placeholder="Search products…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </section>

            {loading && <div className="grid">{Array.from({ length: 8 }).map((_, i) => <div key={i} className="card skeleton" />)}</div>}

            {loadError && (
              <div className="notice err">
                <b>Couldn’t load the store.</b> {loadError}
                <button onClick={() => { setLoading(true); loadCatalog() }}>Retry</button>
              </div>
            )}

            {!loading && !loadError && visible.length === 0 && <p className="muted center">No products found.</p>}

            <div className="grid">
              {visible.map((p) => {
                const avail = stock[p.id] ?? 0
                const s = stockInfo(avail)
                const inCart = cartItems.filter((i) => i.productId === p.id).reduce((n, i) => n + i.quantity, 0)
                return (
                  <article key={p.id} className={`card ${avail <= 0 ? 'soldout' : ''}`}>
                    <div className="thumb">
                      <img
                        src={p.imageUrl || PLACEHOLDER}
                        alt={p.name}
                        loading="lazy"
                        onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = PLACEHOLDER }}
                      />
                    </div>
                    <div className="card-body">
                      <h3>{p.name}</h3>
                      <span className={`stock ${s.cls}`}>{s.label}</span>
                      <div className="row">
                        <span className="price">{money(p.price)}</span>
                        <button className="primary" disabled={busy || avail <= 0 || inCart >= avail} onClick={() => handleAdd(p)}>
                          {avail <= 0 ? 'Sold out' : inCart >= avail ? 'Max in cart' : 'Add to cart'}
                        </button>
                      </div>
                    </div>
                  </article>
                )
              })}
            </div>
          </>
        ) : (
          <OrdersPage byId={byId} />
        )}
      </main>

      <footer className="footer">Cloud-Native E-Commerce Store · PROG3350</footer>

      {/* Cart drawer */}
      <div className={`overlay ${cartOpen ? 'show' : ''}`} onClick={() => setCartOpen(false)} />
      <aside className={`drawer ${cartOpen ? 'open' : ''}`} aria-hidden={!cartOpen}>
        <div className="drawer-head">
          <h2>Your cart</h2>
          <button className="icon" onClick={() => setCartOpen(false)} aria-label="Close cart">✕</button>
        </div>

        <div className="drawer-body">
          {lines.length === 0 ? (
            <div className="empty">
              <p>Your cart is empty.</p>
              <button className="ghost" onClick={() => setCartOpen(false)}>Keep shopping</button>
            </div>
          ) : (
            lines.map((l) => (
              <div className="line" key={l.id}>
                <img src={l.imageUrl || PLACEHOLDER} alt="" onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = PLACEHOLDER }} />
                <div className="line-info">
                  <b>{l.name}</b>
                  <span className="muted">{l.quantity} × {money(l.price)}</span>
                </div>
                <div className="line-right">
                  <b>{money(l.price * l.quantity)}</b>
                  <button className="link" disabled={busy} onClick={() => handleRemove(l.id)}>Remove</button>
                </div>
              </div>
            ))
          )}
        </div>

        {lines.length > 0 && (
          <div className="drawer-foot">
            {checkoutError && <div className="notice err">{checkoutError}</div>}
            <div className="total"><span>Total</span><b>{money(total)}</b></div>
            <button className="primary big" disabled={busy} onClick={handleCheckout}>
              {busy ? 'Working…' : 'Checkout'}
            </button>
          </div>
        )}
      </aside>

      {/* Order confirmation */}
      {result && (
        <div className="modal-wrap" onClick={() => setResult(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h2>Order confirmed</h2>
            <p className="muted">Your order {result.orders.length > 1 ? 'numbers are' : 'number is'}:</p>
            <ul className="order-list">
              {result.orders.map((o) => (
                <li key={o.id}>
                  <b>#{o.id}</b>
                  <span>{byId[o.productId]?.name ?? `Product #${o.productId}`} × {o.quantity}</span>
                  <span>{money(o.totalPrice)}</span>
                </li>
              ))}
            </ul>
            <div className="row-end">
              <button className="ghost" onClick={() => { setResult(null); setPage('orders') }}>View my orders</button>
              <button className="primary" onClick={() => setResult(null)}>Continue shopping</button>
            </div>
          </div>
        </div>
      )}

      {toast && <div className={`toast ${toast.type}`}>{toast.text}</div>}
    </>
  )
}

function OrdersPage({ byId }) {
  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.getOrders().then((o) => setOrders([...o].sort((a, b) => b.id - a.id))).catch((e) => setError(e.message))
  }, [])

  return (
    <section className="orders">
      <div className="page-head"><h1>My orders</h1></div>
      {error && <div className="notice err">{error}</div>}
      {!orders && !error && <p className="muted">Loading…</p>}
      {orders && orders.length === 0 && <p className="muted">No orders yet.</p>}
      {orders && orders.length > 0 && (
        <div className="table">
          <div className="tr th"><span>Order</span><span>Product</span><span>Qty</span><span>Total</span><span>Status</span></div>
          {orders.map((o) => (
            <div className="tr" key={o.id}>
              <span><b>#{o.id}</b></span>
              <span>{byId[o.productId]?.name ?? `Product #${o.productId}`}</span>
              <span>{o.quantity}</span>
              <span>{money(o.totalPrice)}</span>
              <span><i className={`status ${String(o.status).toLowerCase()}`}>{o.status}</i></span>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
