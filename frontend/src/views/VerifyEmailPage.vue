<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import AlertBanner from '@/components/AlertBanner.vue'
import SpinnerIcon from '@/components/SpinnerIcon.vue'

const authStore = useAuthStore()
const resending = ref(false)
const resent = ref(false)

async function handleResend() {
  resending.value = true
  resent.value = false
  const ok = await authStore.resendVerification()
  resending.value = false
  if (ok) resent.value = true
}
</script>

<template>
  <AuthLayout
    title="Verify your email"
    subtitle="We sent a verification link to your email address"
  >
    <AlertBanner v-if="authStore.error" :message="authStore.error" />

    <div class="text-center space-y-4">
      <div
        class="w-16 h-16 bg-primary-green/10 rounded-full flex items-center justify-center mx-auto"
      >
        <img src="/email-1-svgrepo-com.svg" alt="Email" class="w-8 h-8" />
      </div>

      <p class="text-sm text-gray-600">
        Click the link in the email to activate your account, then come back here to sign in.
      </p>

      <p v-if="resent" class="text-sm text-primary-green font-medium">Verification email resent.</p>

      <button
        @click="handleResend"
        :disabled="resending"
        class="text-sm text-primary-green hover:text-forest-green font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-1 cursor-pointer"
      >
        <SpinnerIcon v-if="resending" />
        {{ resending ? 'Sending...' : "Didn't get the email? Resend" }}
      </button>
    </div>

    <p class="text-center text-sm text-gray-500 mt-6">
      Already verified?
      <router-link
        to="/login"
        class="text-primary-green hover:text-forest-green font-semibold transition-colors"
      >
        Sign in
      </router-link>
    </p>
  </AuthLayout>
</template>
