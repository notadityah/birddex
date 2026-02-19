<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
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

const errors = reactive({
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const { loading, run, authStore } = useAuthAction('/verify-email')
const router = useRouter()

function validate() {
  let valid = true

  errors.name = ''
  errors.email = ''
  errors.password = ''
  errors.confirmPassword = ''

  if (!name.value.trim()) {
    errors.name = 'Name is required.'
    valid = false
  }

  if (!email.value.trim()) {
    errors.email = 'Email is required.'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    errors.email = 'Please enter a valid email address.'
    valid = false
  }

  if (!password.value) {
    errors.password = 'Password is required.'
    valid = false
  } else if (password.value.length < 6) {
    errors.password = 'Password must be at least 6 characters.'
    valid = false
  }

  if (!confirmPassword.value) {
    errors.confirmPassword = 'Please confirm your password.'
    valid = false
  } else if (password.value !== confirmPassword.value) {
    errors.confirmPassword = 'Passwords do not match.'
    valid = false
  }

  return valid
}

function handleRegister() {
  if (!validate()) return
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
        :error="errors.name"
      />

      <AuthFormInput
        v-model="email"
        id="register-email"
        label="Email"
        type="email"
        placeholder="you@example.com"
        autocomplete="email"
        :error="errors.email"
      />

      <AuthFormInput
        v-model="password"
        id="register-password"
        label="Password"
        type="password"
        placeholder="Create a password"
        autocomplete="new-password"
        :error="errors.password"
      />

      <AuthFormInput
        v-model="confirmPassword"
        id="register-confirm-password"
        label="Confirm Password"
        type="password"
        placeholder="Confirm your password"
        autocomplete="new-password"
        :error="errors.confirmPassword"
      />

      <PrimaryButton
        :loading="loading"
        label="Create Account"
        loading-label="Creating account..."
      />
    </form>

    <SocialLoginButtons @success="() => router.push('/dashboard')" />

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
