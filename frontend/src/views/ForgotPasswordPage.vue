<script setup>
import { ref } from 'vue'
import { useAuthAction } from '@/composables/useAuthAction'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import AlertBanner from '@/components/AlertBanner.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'

const email = ref('')
const sent = ref(false)
const localError = ref('')

const { loading, run, authStore } = useAuthAction(null)

async function handleReset() {
  localError.value = ''

  if (!email.value) {
    localError.value = 'Please enter your email address.'
    return
  }

  const ok = await run(() => authStore.resetPassword(email.value))
  if (ok) {
    sent.value = true
  } else {
    localError.value = authStore.error || 'Something went wrong. Please try again.'
  }
}
</script>

<template>
  <AuthLayout
    title="Reset your password"
    subtitle="Enter your email and we'll send you a reset link"
  >
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
      <AlertBanner v-if="localError" :message="localError" />

      <AuthFormInput
        v-model="email"
        id="forgot-email"
        label="Email"
        type="email"
        placeholder="you@example.com"
        autocomplete="email"
      />

      <PrimaryButton :loading="loading" label="Send Reset Link" loading-label="Sending..." />
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
  </AuthLayout>
</template>
