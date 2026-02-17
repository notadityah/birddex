import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  signOut,
  sendPasswordResetEmail,
  onAuthStateChanged,
} from 'firebase/auth'
import { auth, googleProvider } from '@/firebase'
import { createUserProfile, getUserProfile } from '@/services/userService'

export const useAuthStore = defineStore('auth', () => {
  // --- State ---
  const user = ref(null)
  const userProfile = ref(null)
  const loading = ref(true) // true until initial auth check completes
  const error = ref(null)
  let _authReadyPromise = null // cached so initAuthListener only runs once

  // --- Getters ---
  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => userProfile.value?.role === 'admin')
  const displayName = computed(
    () => userProfile.value?.name || user.value?.displayName || user.value?.email || '',
  )

  // --- Internal helpers ---
  function setError(err) {
    // Map Firebase error codes to friendly messages
    const messages = {
      'auth/email-already-in-use': 'An account with this email already exists.',
      'auth/invalid-email': 'Please enter a valid email address.',
      'auth/user-not-found': 'No account found with this email.',
      'auth/wrong-password': 'Incorrect password. Please try again.',
      'auth/invalid-credential': 'Invalid email or password.',
      'auth/weak-password': 'Password should be at least 6 characters.',
      'auth/too-many-requests': 'Too many attempts. Please try again later.',
      'auth/popup-closed-by-user': 'Sign-in popup was closed before completing.',
    }
    error.value = messages[err.code] || err.message
  }

  function clearError() {
    error.value = null
  }

  // --- Actions ---
  async function login(email, password) {
    clearError()
    loading.value = true
    try {
      const cred = await signInWithEmailAndPassword(auth, email, password)
      user.value = cred.user
      await fetchUserProfile(cred.user.uid)
      return true
    } catch (err) {
      setError(err)
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(name, email, password) {
    clearError()
    loading.value = true
    try {
      const cred = await createUserWithEmailAndPassword(auth, email, password)
      user.value = cred.user
      await createUserProfile(cred.user.uid, { name, email })
      await fetchUserProfile(cred.user.uid)
      return true
    } catch (err) {
      setError(err)
      return false
    } finally {
      loading.value = false
    }
  }

  async function loginWithGoogle() {
    clearError()
    loading.value = true
    try {
      const cred = await signInWithPopup(auth, googleProvider)
      user.value = cred.user

      // Check if profile exists, create if first-time Google login
      const existing = await getUserProfile(cred.user.uid)
      if (!existing) {
        await createUserProfile(cred.user.uid, {
          name: cred.user.displayName || '',
          email: cred.user.email || '',
        })
      }
      await fetchUserProfile(cred.user.uid)
      return true
    } catch (err) {
      setError(err)
      return false
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    clearError()
    await signOut(auth)
    user.value = null
    userProfile.value = null
    _authReadyPromise = null // reset so next session re-inits
  }

  async function resetPassword(email) {
    clearError()
    try {
      await sendPasswordResetEmail(auth, email)
      return true
    } catch (err) {
      setError(err)
      return false
    }
  }

  async function fetchUserProfile(uid, force = false) {
    // Skip Firestore read if profile is already cached for this uid
    if (!force && userProfile.value?._uid === uid) return
    const profile = await getUserProfile(uid)
    if (profile) {
      userProfile.value = { ...profile, _uid: uid }
    }
  }

  // --- Auth state listener ---
  // Returns a cached promise that resolves once the initial auth state is known.
  // Subsequent calls return the same promise instantly.
  function initAuthListener() {
    if (_authReadyPromise) return _authReadyPromise

    _authReadyPromise = new Promise((resolve) => {
      onAuthStateChanged(auth, async (firebaseUser) => {
        if (firebaseUser) {
          user.value = firebaseUser
          await fetchUserProfile(firebaseUser.uid)
        } else {
          user.value = null
          userProfile.value = null
        }
        loading.value = false
        resolve()
      })
    })

    return _authReadyPromise
  }

  return {
    // state
    user,
    userProfile,
    loading,
    error,
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
    clearError,
    initAuthListener,
  }
})
