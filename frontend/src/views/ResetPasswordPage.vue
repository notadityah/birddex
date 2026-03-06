<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { authClient } from '@/lib/auth-client'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import AlertBanner from '@/components/AlertBanner.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'

const route = useRoute()

const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')
const done = ref(false)

async function handleSubmit() {
  error.value = ''

  if (!newPassword.value) {
    error.value = 'Please enter a new password.'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    error.value = 'Passwords do not match.'
    return
  }

  loading.value = true
  const { error: err } = await authClient.resetPassword({
    newPassword: newPassword.value,
    token: route.query.token,
  })
  loading.value = false

  if (err) {
    error.value = err.message || 'Failed to reset password. The link may have expired.'
    return
  }

  done.value = true
}
</script>

<template>
  <AuthLayout
    title="Set new password"
    subtitle="Enter and confirm your new password"
  >
    <!-- Success state -->
    <div v-if="done" class="text-center space-y-4">
      <div class="w-14 h-14 mx-auto bg-green-100 rounded-full flex items-center justify-center">
        <img src="/check-mark-svgrepo-com.svg" alt="Check Mark" class="w-6 h-6" />
      </div>
      <p class="text-sm text-gray-700">Your password has been reset successfully.</p>
      <router-link
        to="/login"
        class="inline-block text-sm text-primary-green hover:text-forest-green font-semibold transition-colors"
      >
        Sign in →
      </router-link>
    </div>

    <!-- Form state -->
    <form v-else @submit.prevent="handleSubmit" class="space-y-4">
      <AlertBanner v-if="error" :message="error" />

      <AuthFormInput
        v-model="newPassword"
        id="new-password"
        label="New Password"
        type="password"
        placeholder="••••••••"
        autocomplete="new-password"
      />

      <AuthFormInput
        v-model="confirmPassword"
        id="confirm-password"
        label="Confirm Password"
        type="password"
        placeholder="••••••••"
        autocomplete="new-password"
      />

      <PrimaryButton :loading="loading" label="Reset Password" loading-label="Resetting..." />
    </form>
  </AuthLayout>
</template>
