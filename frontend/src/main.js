import './assets/main.css'

import { Amplify } from 'aws-amplify'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

try {
  const outputs = await import('../amplify_outputs.json')
  Amplify.configure(outputs.default)
} catch {
  console.warn('amplify_outputs.json not found — Amplify not configured')
}

const app = createApp(App)

app.use(router)

app.mount('#app')
