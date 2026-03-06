import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

import { useAuthStore } from '@/stores/auth'

if (!import.meta.env.VITE_API_URL) {
  console.error('VITE_API_URL is not set. API calls will fail.')
}

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Wait for auth session to resolve once before mounting.
// initAuthListener() caches its promise, so the router guard
// and any later calls return instantly.
const authStore = useAuthStore()
authStore.initAuthListener().then(() => {
  app.mount('#app')
})
