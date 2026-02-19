import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

import { useAuthStore } from '@/stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Wait for Firebase auth state to resolve once before mounting.
// initAuthListener() caches its promise, so the router guard
// and any later calls return instantly.
const authStore = useAuthStore()
authStore.initAuthListener().then(() => {
  app.mount('#app')
})
