<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoadAnimation } from '@/composables/useAnimation'
import { useAuth } from '@/composables/useAuth'
import Header from '@/components/landingpage/HeaderComponent.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'

const router = useRouter()
const { forgotPassword, loading, error: authError } = useAuth()

const cardRef = ref(null)
const email = ref('')
const formError = ref('')
const fieldErrors = ref('')

useLoadAnimation(cardRef, { y: 30, duration: 0.7 })

async function handleSubmit() {
  formError.value = ''
  fieldErrors.value = ''

  if (!email.value) {
    fieldErrors.value = 'Email is required.'
    return
  }

  try {
    await forgotPassword(email.value)
    router.push({ name: 'reset-password', query: { email: email.value } })
  } catch {
    formError.value = authError.value
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
          <h1 class="text-2xl font-bold text-gray-900">Forgot password?</h1>
          <p class="text-sm text-gray-500 mt-1">
            Enter your email and we'll send you a reset code.
          </p>
        </div>

        <!-- Error banner -->
        <div
          v-if="formError"
          class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700"
        >
          {{ formError }}
        </div>

        <!-- Form -->
        <form @submit.prevent="handleSubmit" class="space-y-4">
          <AuthFormInput
            v-model="email"
            id="forgot-email"
            label="Email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
            :error="fieldErrors"
          />

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? 'Sending...' : 'Send Reset Code' }}
          </button>
        </form>

        <!-- Back to login -->
        <p class="text-center text-sm text-gray-500 mt-6">
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
