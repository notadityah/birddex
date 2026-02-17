<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoadAnimation } from '@/composables/useAnimation'
import { useAuthStore } from '@/stores/auth'
import Header from '@/components/landingpage/HeaderComponent.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import SocialLoginButtons from '@/components/auth/SocialLoginButtons.vue'
import SpinnerIcon from '@/components/SpinnerIcon.vue'

const router = useRouter()
const authStore = useAuthStore()
const cardRef = ref(null)

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const confirmError = ref('')
const loading = ref(false)

useLoadAnimation(cardRef, { y: 30, duration: 0.7 })

async function handleRegister() {
  authStore.clearError()
  confirmError.value = ''

  if (password.value !== confirmPassword.value) {
    confirmError.value = 'Passwords do not match.'
    return
  }

  loading.value = true
  const ok = await authStore.register(name.value, email.value, password.value)
  loading.value = false
  if (ok) router.push('/dashboard')
}

function onGoogleSuccess() {
  router.push('/dashboard')
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
          <h1 class="text-2xl font-bold text-gray-900">Create your account</h1>
          <p class="text-sm text-gray-500 mt-1">Start building your bird collection</p>
        </div>

        <!-- Global error -->
        <div
          v-if="authStore.error"
          class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3 mb-4"
        >
          {{ authStore.error }}
        </div>

        <!-- Form -->
        <form @submit.prevent="handleRegister" class="space-y-4">
          <AuthFormInput
            v-model="name"
            id="register-name"
            label="Full Name"
            type="text"
            placeholder="Jane Doe"
            autocomplete="name"
          />

          <AuthFormInput
            v-model="email"
            id="register-email"
            label="Email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
          />

          <AuthFormInput
            v-model="password"
            id="register-password"
            label="Password"
            type="password"
            placeholder="Create a password"
            autocomplete="new-password"
          />

          <AuthFormInput
            v-model="confirmPassword"
            id="register-confirm-password"
            label="Confirm Password"
            type="password"
            placeholder="Confirm your password"
            autocomplete="new-password"
            :error="confirmError"
          />

          <button
            type="submit"
            :disabled="loading"
            class="w-full flex items-center justify-center gap-2 bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all mt-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <SpinnerIcon v-if="loading" />
            {{ loading ? 'Creating account...' : 'Create Account' }}
          </button>
        </form>

        <!-- Social Login -->
        <SocialLoginButtons @success="onGoogleSuccess" />

        <!-- Login link -->
        <p class="text-center text-sm text-gray-500 mt-6">
          Already have an account?
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
