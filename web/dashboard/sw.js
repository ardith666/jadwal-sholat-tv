const CACHE_NAME = 'masjidtv-dashboard-v1';
const STATIC_ASSETS = [
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/icon.svg',
  './icons/apple-touch-icon.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// Network-first strategy for dynamic monitoring data
self.addEventListener('fetch', (event) => {
  // Only handle GET requests
  if (event.request.method !== 'GET') return;
  
  const url = new URL(event.request.url);

  // If fetching static assets or icons, cache-first
  if (url.pathname.includes('/icons/') || url.pathname.endsWith('.json')) {
    event.respondWith(
      caches.match(event.request).then((cached) => {
        return cached || fetch(event.request).then((response) => {
          return caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, response.clone());
            return response;
          });
        });
      })
    );
    return;
  }

  // Network-first for HTML and API calls
  event.respondWith(
    fetch(event.request)
      .then((networkResponse) => {
        return networkResponse;
      })
      .catch(() => {
        return caches.match(event.request).then((cached) => {
          if (cached) return cached;
          return new Response(
            `<!DOCTYPE html>
            <html lang="id">
            <head><meta charset="UTF-8"><title>Offline - MasjidTV</title>
            <style>body{font-family:sans-serif;background:#064e3b;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center;padding:20px;}</style>
            </head>
            <body>
              <div>
                <h1 style="font-size:3rem;margin:0;">📡</h1>
                <h2>Koneksi Terputus</h2>
                <p>Anda sedang offline. Silakan periksa koneksi internet Anda untuk memuat dashboard MasjidTV.</p>
                <button onclick="location.reload()" style="background:#10b981;border:none;color:#fff;padding:12px 24px;border-radius:8px;font-weight:bold;cursor:pointer;">Muat Ulang</button>
              </div>
            </body>
            </html>`,
            { headers: { 'Content-Type': 'text/html' } }
          );
        });
      })
  );
});
