const productsUrl = import.meta.env.VITE_PRODUCT_API || 'http://localhost:8080';
const inventoryUrl = import.meta.env.VITE_INVENTORY_API || 'http://localhost:8082';
const cartUrl = import.meta.env.VITE_CART_API || 'http://localhost:8083';

async function request(url, method = 'GET', body) {
  const response = await fetch(url, {
    method,
    ...(body && {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }),
  });

  if (!response.ok) {
    throw Object.assign(new Error(`Request failed (${response.status})`), { status: response.status });
  }
  return response.status === 204 ? null : response.json();
}

export const api = {
  products: () => request(`${productsUrl}/api/products`),
  inventory: () => request(`${inventoryUrl}/api/inventory`),
  createCart: () => request(`${cartUrl}/api/carts`, 'POST'),
  addItem: (id, productId, quantity) =>
    request(`${cartUrl}/api/carts/${id}/items`, 'POST', { productId, quantity }),
  removeItem: (id, itemId) => request(`${cartUrl}/api/carts/${id}/items/${itemId}`, 'DELETE'),
  checkout: (id) => request(`${cartUrl}/api/carts/${id}/checkout`, 'POST'),
};
