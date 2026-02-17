<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useLoadAnimation } from '@/composables/useAnimation'
import { useAuth } from '@/composables/useAuth'
import Header from '@/components/landingpage/HeaderComponent.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import SocialLoginButtons from '@/components/auth/SocialLoginButtons.vue'

const router = useRouter()
const { login, loading, error: authError } = useAuth()

const cardRef = ref(null)
const email = ref('')
const password = ref('')
const formError = ref('')
const fieldErrors = reactive({ email: '', password: '' })

useLoadAnimation(cardRef, { y: 30, duration: 0.7 })

function clearErrors() {
  formError.value = ''
  fieldErrors.email = ''
  fieldErrors.password = ''
}

async function handleLogin() {
  clearErrors()

  if (!email.value) {
    fieldErrors.email = 'Email is required.'
    return
  }
  if (!password.value) {
    fieldErrors.password = 'Password is required.'
    return
  }

  try {
    const result = await login(email.value, password.value)

    if (result?.needsConfirmation) {
      router.push({ name: 'confirm', query: { email: email.value } })
      return
    }

    router.push({ name: 'dashboard' })
  } catch (err) {
    const code = err?.name ?? ''
    if (code === 'UserNotConfirmedException') {
      router.push({ name: 'confirm', query: { email: email.value } })
    } else if (code === 'UserNotFoundException') {
      fieldErrors.email = authError.value
    } else if (code === 'NotAuthorizedException') {
      fieldErrors.password = authError.value
    } else {
      formError.value = authError.value
    }
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <Header />

    <main class="flex-1 flex items-center justify-center px-4 py-12 bg-white/90 backdrop-blur-none">
      <div ref="cardRef" class="w-full max-w-md bg-white rounded-2xl shadow-xl p-8 sm:p-10">
        <!-- Logo and heading -->
        <div class="text-center mb-8">
          <router-link to="/" class="inline-flex items-center gap-2 mb-4">
            <div class="w-10 h-10 bg-primary-green rounded-full flex items-center justify-center">
              <img src="/bird-svgrepo-com.svg" alt="BirdDex" width="24" height="24" />
            </div>
          </router-link>
          <h1 class="text-2xl font-bold text-gray-900">Welcome back</h1>
          <p class="text-sm text-gray-500 mt-1">Sign in to your BirdDex account</p>
        </div>

        <!-- General error banner -->
        <div
          v-if="formError"
          class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700"
        >
          {{ formError }}
        </div>

        <!-- Form -->
        <form @submit.prevent="handleLogin" class="space-y-4">
          <AuthFormInput
            v-model="email"
            id="login-email"
            label="Email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
            :error="fieldErrors.email"
          />

          <div>
            <AuthFormInput
              v-model="password"
              id="login-password"
              label="Password"
              type="password"
              placeholder="Enter your password"
              autocomplete="current-password"
              :error="fieldErrors.password"
            />
            <div class="flex justify-end mt-1.5">
              <router-link
                to="/forgot-password"
                class="text-xs text-primary-green hover:text-forest-green transition-colors font-medium"
              >
                Forgot password?
              </router-link>
            </div>
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all mt-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <!-- Social Login -->
        <SocialLoginButtons />

        <!-- Register link -->
        <p class="text-center text-sm text-gray-500 mt-6">
          Don't have an account?
          <router-link
            to="/register"
            class="text-primary-green hover:text-forest-green font-semibold transition-colors"
          >
            Sign up
          </router-link>
        </p>
      </div>
    </main>
  </div>
</template>
