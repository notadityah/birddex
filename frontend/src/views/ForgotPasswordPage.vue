<script setup>
import { ref } from 'vue'
import { useLoadAnimation } from '@/composables/useAnimation'
import Header from '@/components/landingpage/HeaderComponent.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'

const cardRef = ref(null)
const email = ref('')
const sent = ref(false)
const loading = ref(false)
const error = ref('')

useLoadAnimation(cardRef, { y: 30, duration: 0.7 })

async function handleReset() {
  error.value = ''

  if (!email.value) {
    error.value = 'Please enter your email address.'
    return
  }

  loading.value = true

  // Import store lazily to keep the component light
  const { useAuthStore } = await import('@/stores/auth')
  const authStore = useAuthStore()

  const ok = await authStore.resetPassword(email.value)

  if (ok) {
    sent.value = true
  } else {
    error.value = authStore.error || 'Something went wrong. Please try again.'
  }

  loading.value = false
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
          <h1 class="text-2xl font-bold text-gray-900">Reset your password</h1>
          <p class="text-sm text-gray-500 mt-1">Enter your email and we'll send you a reset link</p>
        </div>

        <!-- Success state -->
        <div v-if="sent" class="text-center space-y-4">
          <div class="w-14 h-14 mx-auto bg-green-100 rounded-full flex items-center justify-center">
            <img src="/check-mark-svgrepo-com.svg" alt="Check Mark" class="w-6 h-6" />
          </div>
          <p class="text-sm text-gray-700">
            Check your inbox at <strong>{{ email }}</strong> for a password reset link.
          </p>
          <router-link
            to="/login"
            class="inline-block text-sm text-primary-green hover:text-forest-green font-semibold transition-colors"
          >
            Back to Sign In
          </router-link>
        </div>

        <!-- Form state -->
        <form v-else @submit.prevent="handleReset" class="space-y-4">
          <!-- Global error -->
          <div
            v-if="error"
            class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3"
          >
            {{ error }}
          </div>

          <AuthFormInput
            v-model="email"
            id="forgot-email"
            label="Email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
          />

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all mt-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? 'Sending...' : 'Send Reset Link' }}
          </button>
        </form>

        <!-- Back to login -->
        <p v-if="!sent" class="text-center text-sm text-gray-500 mt-6">
          Remember your password?
          <router-link
            to="/login"
            class="text-primary-green hover:text-forest-green font-semibold transition-colors"
          >
            Sign in
          </router-link>
        </p>
      </div>
    </main>
  </div>
</template>
