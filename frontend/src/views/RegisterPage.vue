<script setup>
import { ref } from 'vue'
import { useAuthAction } from '@/composables/useAuthAction'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import AlertBanner from '@/components/AlertBanner.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SocialLoginButtons from '@/components/auth/SocialLoginButtons.vue'

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const confirmError = ref('')

const { loading, run, authStore, onSuccess } = useAuthAction()

function handleRegister() {
  confirmError.value = ''

  if (password.value !== confirmPassword.value) {
    confirmError.value = 'Passwords do not match.'
    return
  }

  run(() => authStore.register(name.value, email.value, password.value))
}
</script>

<template>
  <AuthLayout title="Create your account" subtitle="Start building your bird collection">
    <AlertBanner v-if="authStore.error" :message="authStore.error" />

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

      <PrimaryButton
        :loading="loading"
        label="Create Account"
        loading-label="Creating account..."
      />
    </form>

    <SocialLoginButtons @success="onSuccess" />

    <p class="text-center text-sm text-gray-500 mt-6">
      Already have an account?
      <router-link
        to="/login"
        class="text-primary-green hover:text-forest-green font-semibold transition-colors"
      >
        Sign in
      </router-link>
    </p>
  </AuthLayout>
</template>
