import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * Composable that wraps an auth store action with loading state,
 * error clearing, and optional redirect on success.
 *
 * @param {string|null} redirect - Route to navigate to on success, or null to skip
 * @returns {{ loading: Ref<boolean>, run: Function, authStore: Object, onSuccess: Function }}
 */
export function useAuthAction(redirect = '/dashboard') {
  const loading = ref(false)
  const authStore = useAuthStore()
  const router = useRouter()

  async function run(actionFn) {
    authStore.clearError()
    loading.value = true
    const ok = await actionFn()
    loading.value = false
    if (ok === true && redirect) router.push(redirect)
    return ok
  }

  function onSuccess() {
    if (redirect) router.push(redirect)
  }

  return { loading, run, authStore, onSuccess }
}
