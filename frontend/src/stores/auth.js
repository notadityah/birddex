import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { authClient } from '@/lib/auth-client'
import { useRouter } from 'vue-router'

// Maps better-auth error codes to user-friendly messages.
// better-auth returns machine-readable codes; this provides the UI copy.
const ERROR_MAP = {
  INVALID_EMAIL_OR_PASSWORD: 'Invalid email or password.',
  EMAIL_NOT_VERIFIED: 'Please verify your email before signing in.',
  USER_ALREADY_EXISTS: 'An account with this email already exists.',
  TOO_MANY_REQUESTS: 'Too many attempts. Please try again later.',
}

// DEV_BYPASS: when true, skips the real better-auth session and creates a fake
// admin user. Lets you develop the frontend without running the backend.
const DEV_BYPASS = import.meta.env.VITE_DEV_BYPASS_AUTH === 'true'

export const useAuthStore = defineStore('auth', () => {
  // sessionRef wraps better-auth's reactive useSession() — a ref with { data, isPending }.
  // In DEV_BYPASS mode, it's a static ref with a fake admin user.
  const sessionRef = DEV_BYPASS
    ? ref({
        data: {
          user: {
            id: 'dev-user',
            name: 'Dev User',
            email: 'dev@localhost',
            emailVerified: true,
            role: 'admin',
          },
        },
        isPending: false,
      })
    : authClient.useSession()
  const router = useRouter()
  const error = ref(null)
  const pendingEmail = ref('')

  // --- Getters ---
  const user = computed(() => sessionRef.value.data?.user ?? null)
  const loading = computed(() => sessionRef.value.isPending)
  const isAuthenticated = computed(() => !!user.value?.emailVerified)
  const displayName = computed(() => user.value?.name || user.value?.email || '')
  const isAdmin = computed(() => user.value?.role === 'admin')

  // --- Internal helpers ---
  function setError(err) {
    error.value = ERROR_MAP[err?.code] || err?.message || 'Something went wrong.'
  }

  function clearError() {
    error.value = null
  }

  // --- Actions ---
  async function login(email, password) {
    clearError()
    const { error: err } = await authClient.signIn.email({ email, password })
    if (err) {
      if (err.code === 'EMAIL_NOT_VERIFIED') {
        pendingEmail.value = email
        return 'unverified'
      }
      setError(err)
      return false
    }
    return true
  }

  async function register(name, email, password) {
    clearError()
    const { error: err } = await authClient.signUp.email({
      email,
      password,
      name,
      callbackURL: window.location.origin + '/login',
    })
    if (err) {
      setError(err)
      return false
    }
    pendingEmail.value = email
    return true
  }

  function loginWithGoogle() {
    clearError()
    authClient.signIn.social({ provider: 'google', callbackURL: window.location.origin + '/mydex' })
  }

  async function logout() {
    clearError()
    await authClient.signOut()
    pendingEmail.value = ''
    // Wait for session data to clear before navigating, otherwise the
    // router guard may still see the stale authenticated state and
    // redirect back from the guestOnly /login route.
    await new Promise((resolve) => {
      if (!sessionRef.value.data?.user) return resolve()
      const stop = watch(
        () => sessionRef.value.data?.user,
        (u) => {
          if (!u) {
            stop()
            resolve()
          }
        },
      )
      // Safety timeout — if the reactive update never fires, navigate anyway
      setTimeout(() => {
        stop()
        resolve()
      }, 500)
    })
    router.push('/login')
  }

  async function resetPassword(email) {
    clearError()
    const { error: err } = await authClient.forgetPassword({
      email,
      redirectTo: window.location.origin + '/reset-password',
    })
    if (err) {
      setError(err)
      return false
    }
    return true
  }

  async function resendVerification() {
    clearError()
    const email = pendingEmail.value
    if (!email) {
      error.value = 'No unverified account found. Please register or log in again.'
      return false
    }
    const { error: err } = await authClient.sendVerificationEmail({
      email,
      callbackURL: window.location.origin + '/login',
    })
    if (err) {
      setError(err)
      return false
    }
    return true
  }

  async function changePassword(currentPassword, newPassword) {
    clearError()
    const { error: err } = await authClient.changePassword({
      currentPassword,
      newPassword,
    })
    if (err) {
      setError(err)
      return false
    }
    return true
  }

  async function deleteAccount(password) {
    clearError()
    const { error: err } = await authClient.deleteUser({
      password,
    })
    if (err) {
      setError(err)
      return false
    }
    pendingEmail.value = ''
    router.push('/login')
    return true
  }

  // Router guard calls this before checking auth state. Returns immediately if
  // session is already resolved; otherwise waits for the initial session fetch to
  // complete. This prevents flash-of-wrong-page on hard refresh.
  function initAuthListener() {
    if (!sessionRef.value.isPending) return Promise.resolve()
    return new Promise((resolve) => {
      const stop = watch(
        () => sessionRef.value.isPending,
        (pending) => {
          if (!pending) {
            stop()
            resolve()
          }
        },
      )
    })
  }

  return {
    // state
    user,
    loading,
    error,
    pendingEmail,
    // getters
    isAuthenticated,
    isAdmin,
    displayName,
    // actions
    login,
    register,
    loginWithGoogle,
    logout,
    resetPassword,
    resendVerification,
    changePassword,
    deleteAccount,
    clearError,
    initAuthListener,
  }
})
