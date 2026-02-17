import './assets/main.css'

import { Amplify } from 'aws-amplify'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

const outputs = await import('../amplify_outputs.json')
Amplify.configure(outputs.default)

const app = createApp(App)

app.use(router)

app.mount('#app')
