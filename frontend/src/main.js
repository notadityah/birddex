import './assets/main.css'

import { Amplify } from 'aws-amplify'
import outputs from '../amplify_outputs.json'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

Amplify.configure(outputs)

const app = createApp(App)

app.use(router)

app.mount('#app')
