<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoadAnimation } from '@/composables/useAnimation'
import { useAuth } from '@/composables/useAuth'
import Header from '@/components/landingpage/HeaderComponent.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'

const router = useRouter()
const route = useRoute()
const { confirmPasswordReset, loading, error: authError } = useAuth()

const cardRef = ref(null)
const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const formError = ref('')
const fieldErrors = reactive({ code: '', newPassword: '', confirmPassword: '' })

useLoadAnimation(cardRef, { y: 30, duration: 0.7 })

onMounted(() => {
  email.value = route.query.email || ''
  if (!email.value) {
    router.replace({ name: 'forgot-password' })
  }
})

function clearErrors() {
  formError.value = ''
  fieldErrors.code = ''
  fieldErrors.newPassword = ''
  fieldErrors.confirmPassword = ''
}

async function handleReset() {
  clearErrors()

  let hasError = false
  if (!code.value || code.value.length !== 6) {
    fieldErrors.code = 'Please enter the 6-digit code.'
    hasError = true
  }
  if (!newPassword.value) {
    fieldErrors.newPassword = 'New password is required.'
    hasError = true
  } else if (newPassword.value.length < 8) {
    fieldErrors.newPassword = 'Password must be at least 8 characters.'
    hasError = true
  }
  if (newPassword.value !== confirmPassword.value) {
    fieldErrors.confirmPassword = 'Passwords do not match.'
    hasError = true
  }
  if (hasError) return

  try {
    await confirmPasswordReset(email.value, code.value, newPassword.value)
    router.push({ name: 'login', query: { reset: 'true' } })
  } catch {
    const msg = authError.value
    if (msg.includes('code')) {
      fieldErrors.code = msg
    } else if (msg.includes('password') || msg.includes('Password')) {
      fieldErrors.newPassword = msg
    } else {
      formError.value = msg
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
          <h1 class="text-2xl font-bold text-gray-900">Reset your password</h1>
          <p class="text-sm text-gray-500 mt-1">
            Enter the code sent to
            <span class="font-medium text-gray-700">{{ email }}</span>
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
        <form @submit.prevent="handleReset" class="space-y-4">
          <!-- Code input -->
          <div>
            <label for="reset-code" class="block text-sm font-medium text-gray-700 mb-1.5">
              Verification Code
            </label>
            <input
              id="reset-code"
              v-model="code"
              type="text"
              inputmode="numeric"
              maxlength="6"
              placeholder="123456"
              autocomplete="one-time-code"
              class="w-full px-4 py-3 border rounded-lg text-center text-2xl font-bold tracking-[0.5em] text-gray-900 placeholder:text-gray-300 placeholder:tracking-[0.5em] focus:outline-none focus:ring-2 transition-colors"
              :class="
                fieldErrors.code
                  ? 'border-red-500 focus:ring-red-400/40 focus:border-red-500'
                  : 'border-gray-300 focus:ring-primary-green/40 focus:border-primary-green'
              "
            />
            <p v-if="fieldErrors.code" class="mt-1 text-xs text-red-600">{{ fieldErrors.code }}</p>
          </div>

          <AuthFormInput
            v-model="newPassword"
            id="reset-new-password"
            label="New Password"
            type="password"
            placeholder="Enter new password"
            autocomplete="new-password"
            :error="fieldErrors.newPassword"
          />

          <AuthFormInput
            v-model="confirmPassword"
            id="reset-confirm-password"
            label="Confirm New Password"
            type="password"
            placeholder="Confirm new password"
            autocomplete="new-password"
            :error="fieldErrors.confirmPassword"
          />

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? 'Resetting...' : 'Reset Password' }}
          </button>
        </form>

        <!-- Back to login -->
        <p class="text-center text-sm text-gray-500 mt-6">
          <router-link
            to="/login"
            class="text-primary-green hover:text-forest-green font-semibold transition-colors"
          >
            Back to sign in
          </router-link>
        </p>
      </div>
    </main>
  </div>
</template>
