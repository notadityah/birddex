import { createAuthClient } from 'better-auth/vue'
import { adminClient } from 'better-auth/client/plugins'

export const authClient = createAuthClient({
  baseURL: import.meta.env.VITE_API_URL,
  plugins: [adminClient()],
})
