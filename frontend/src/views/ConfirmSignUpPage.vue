<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoadAnimation } from '@/composables/useAnimation'
import { useAuth } from '@/composables/useAuth'
import Header from '@/components/landingpage/HeaderComponent.vue'

const router = useRouter()
const route = useRoute()
const { confirmAccount, resendCode, loading, error: authError } = useAuth()

const cardRef = ref(null)
const email = ref('')
const code = ref('')
const formError = ref('')
const codeError = ref('')
const successMessage = ref('')

useLoadAnimation(cardRef, { y: 30, duration: 0.7 })

onMounted(() => {
  email.value = route.query.email || ''
  if (!email.value) {
    router.replace({ name: 'register' })
  }
})

async function handleConfirm() {
  formError.value = ''
  codeError.value = ''

  if (!code.value || code.value.length !== 6) {
    codeError.value = 'Please enter the 6-digit code.'
    return
  }

  try {
    await confirmAccount(email.value, code.value)
    router.push({ name: 'login', query: { confirmed: 'true' } })
  } catch {
    const msg = authError.value
    if (msg.includes('code')) {
      codeError.value = msg
    } else {
      formError.value = msg
    }
  }
}

async function handleResend() {
  formError.value = ''
  successMessage.value = ''
  try {
    await resendCode(email.value)
    successMessage.value = 'A new code has been sent to your email.'
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
          <h1 class="text-2xl font-bold text-gray-900">Verify your email</h1>
          <p class="text-sm text-gray-500 mt-1">
            We sent a 6-digit code to
            <span class="font-medium text-gray-700">{{ email }}</span>
          </p>
        </div>

        <!-- Success message -->
        <div
          v-if="successMessage"
          class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700"
        >
          {{ successMessage }}
        </div>

        <!-- Error banner -->
        <div
          v-if="formError"
          class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700"
        >
          {{ formError }}
        </div>

        <!-- Code input -->
        <form @submit.prevent="handleConfirm" class="space-y-4">
          <div>
            <label for="confirm-code" class="block text-sm font-medium text-gray-700 mb-1.5">
              Verification Code
            </label>
            <input
              id="confirm-code"
              v-model="code"
              type="text"
              inputmode="numeric"
              maxlength="6"
              placeholder="123456"
              autocomplete="one-time-code"
              class="w-full px-4 py-3 border rounded-lg text-center text-2xl font-bold tracking-[0.5em] text-gray-900 placeholder:text-gray-300 placeholder:tracking-[0.5em] focus:outline-none focus:ring-2 transition-colors"
              :class="
                codeError
                  ? 'border-red-500 focus:ring-red-400/40 focus:border-red-500'
                  : 'border-gray-300 focus:ring-primary-green/40 focus:border-primary-green'
              "
            />
            <p v-if="codeError" class="mt-1 text-xs text-red-600">{{ codeError }}</p>
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ loading ? 'Verifying...' : 'Verify Email' }}
          </button>
        </form>

        <!-- Resend -->
        <p class="text-center text-sm text-gray-500 mt-6">
          Didn't receive a code?
          <button
            type="button"
            :disabled="loading"
            @click="handleResend"
            class="text-primary-green hover:text-forest-green font-semibold transition-colors cursor-pointer disabled:opacity-50"
          >
            Resend code
          </button>
        </p>

        <!-- Back to register -->
        <p class="text-center text-sm text-gray-500 mt-3">
          <router-link
            to="/register"
            class="text-primary-green hover:text-forest-green font-semibold transition-colors"
          >
            Back to registration
          </router-link>
        </p>
      </div>
    </main>
  </div>
</template>
