import { ref, computed, readonly } from 'vue'
import {
  signIn,
  signUp,
  signOut,
  confirmSignUp,
  resendSignUpCode,
  resetPassword,
  confirmResetPassword,
  getCurrentUser,
  fetchAuthSession,
  signInWithRedirect,
} from 'aws-amplify/auth'
import { Hub } from 'aws-amplify/utils'

// ── Shared reactive state (singleton across all component instances) ──
const user = ref(null)
const loading = ref(true)
const error = ref('')

const isAuthenticated = computed(() => !!user.value)
const userGroups = computed(() => user.value?.groups ?? [])
const isAdmin = computed(() => userGroups.value.includes('ADMINS'))

// ── Internal helpers ──
async function _hydrateUser() {
  try {
    const currentUser = await getCurrentUser()
    const session = await fetchAuthSession()
    const idToken = session.tokens?.idToken
    const groups = idToken?.payload?.['cognito:groups'] ?? []
    const name =
      idToken?.payload?.name ??
      idToken?.payload?.['custom:name'] ??
      currentUser.signInDetails?.loginId ??
      ''

    user.value = {
      userId: currentUser.userId,
      username: currentUser.username,
      email: currentUser.signInDetails?.loginId ?? idToken?.payload?.email ?? '',
      name,
      groups: Array.isArray(groups) ? groups : [],
    }
  } catch {
    user.value = null
  }
}

// Hydrate once on import & listen for auth events
_hydrateUser().finally(() => {
  loading.value = false
})

Hub.listen('auth', async ({ payload }) => {
  switch (payload.event) {
    case 'signedIn':
      await _hydrateUser()
      break
    case 'signedOut':
      user.value = null
      break
    case 'tokenRefresh':
      await _hydrateUser()
      break
  }
})

// ── Composable ──
export function useAuth() {
  /** Sign in with email + password */
  async function login(email, password) {
    error.value = ''
    loading.value = true
    try {
      const result = await signIn({ username: email, password })
      if (result.nextStep?.signInStep === 'CONFIRM_SIGN_UP') {
        return { needsConfirmation: true }
      }
      await _hydrateUser()
      return { success: true }
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** Register a new account */
  async function register(name, email, password) {
    error.value = ''
    loading.value = true
    try {
      const { nextStep } = await signUp({
        username: email,
        password,
        options: {
          userAttributes: { name },
        },
      })
      return { nextStep }
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** Confirm sign-up with the 6-digit code */
  async function confirmAccount(email, code) {
    error.value = ''
    loading.value = true
    try {
      await confirmSignUp({ username: email, confirmationCode: code })
      return { success: true }
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** Resend the sign-up confirmation code */
  async function resendCode(email) {
    error.value = ''
    try {
      await resendSignUpCode({ username: email })
      return { success: true }
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    }
  }

  /** Initiate social login (Google / Apple) */
  async function socialLogin(provider) {
    error.value = ''
    try {
      await signInWithRedirect({ provider })
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    }
  }

  /** Sign out the current user */
  async function logout() {
    error.value = ''
    loading.value = true
    try {
      await signOut()
      user.value = null
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** Initiate forgot-password flow — sends a code to the email */
  async function forgotPassword(email) {
    error.value = ''
    loading.value = true
    try {
      await resetPassword({ username: email })
      return { success: true }
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** Confirm password reset with code + new password */
  async function confirmPasswordReset(email, code, newPassword) {
    error.value = ''
    loading.value = true
    try {
      await confirmResetPassword({
        username: email,
        confirmationCode: code,
        newPassword,
      })
      return { success: true }
    } catch (err) {
      error.value = _friendlyError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** Re-check current auth state (useful for route guards) */
  async function checkAuth() {
    loading.value = true
    await _hydrateUser()
    loading.value = false
    return isAuthenticated.value
  }

  return {
    // State (readonly to prevent external mutation)
    user: readonly(user),
    loading: readonly(loading),
    error,
    isAuthenticated,
    userGroups,
    isAdmin,

    // Actions
    login,
    register,
    confirmAccount,
    resendCode,
    socialLogin,
    logout,
    forgotPassword,
    confirmPasswordReset,
    checkAuth,
  }
}

// ── Error message mapping ──
function _friendlyError(err) {
  const code = err?.name ?? err?.code ?? ''
  const map = {
    UserNotFoundException: 'No account found with that email.',
    NotAuthorizedException: 'Incorrect email or password.',
    UsernameExistsException: 'An account with that email already exists.',
    InvalidPasswordException: 'Password does not meet requirements.',
    CodeMismatchException: 'Invalid verification code. Please try again.',
    ExpiredCodeException: 'Verification code has expired. Request a new one.',
    LimitExceededException: 'Too many attempts. Please try again later.',
    UserNotConfirmedException: 'Please verify your email before signing in.',
  }
  return map[code] || err?.message || 'An unexpected error occurred.'
}
