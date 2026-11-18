// All calls to the backend services live here.
// URLs are the host ports from the assignment; the browser calls the services directly (CORS allows localhost:3000).
const PRODUCT = import.meta.env.VITE_PRODUCT_URL || 'http://localhost:8080'
const ORDER = import.meta.env.VITE_ORDER_URL || 'http://localhost:8081'
const INVENTORY = import.meta.env.VITE_INVENTORY_URL || 'http://localhost:8082'
const CART = import.meta.env.VITE_CART_URL || 'http://localhost:8083'

// Turns a failed response into an Error with a readable message.
async function request(url, options) {
  let res
  try {
    res = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    throw new Error('Cannot reach the server. Is the system running?')
  }
  if (!res.ok) {
    let msg = ''
    try {
      const body = await res.json()
      msg = body.message || body.error || ''
    } catch { /* body was not JSON */ }
    const err = new Error(msg || `Request failed (${res.status})`)
    err.status = res.status
    throw err
  }
  return res.status === 204 ? null : res.json()
}

export const getProducts = () => request(`${PRODUCT}/api/products`)
export const getInventory = () => request(`${INVENTORY}/api/inventory`)
export const getOrders = () => request(`${ORDER}/api/orders`)

export const createCart = () => request(`${CART}/api/carts`, { method: 'POST' })
export const getCart = (id) => request(`${CART}/api/carts/${id}`)
export const addToCart = (cartId, productId, quantity) =>
  request(`${CART}/api/carts/${cartId}/items`, {
    method: 'POST',
    body: JSON.stringify({ productId, quantity }),
  })
export const removeFromCart = (cartId, itemId) =>
  request(`${CART}/api/carts/${cartId}/items/${itemId}`, { method: 'DELETE' })
export const checkout = (cartId) =>
  request(`${CART}/api/carts/${cartId}/checkout`, { method: 'POST' })
