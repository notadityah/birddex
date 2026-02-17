<script setup>
import { ref } from 'vue'
import { useAuthAction } from '@/composables/useAuthAction'
import AuthLayout from '@/components/auth/AuthLayout.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import AlertBanner from '@/components/AlertBanner.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SocialLoginButtons from '@/components/auth/SocialLoginButtons.vue'

const email = ref('')
const password = ref('')

const { loading, run, authStore, onSuccess } = useAuthAction()

function handleLogin() {
  run(() => authStore.login(email.value, password.value))
}
</script>

<template>
  <AuthLayout title="Welcome back" subtitle="Sign in to your BirdDex account">
    <AlertBanner v-if="authStore.error" :message="authStore.error" />

    <form @submit.prevent="handleLogin" class="space-y-4">
      <AuthFormInput
        v-model="email"
        id="login-email"
        label="Email"
        type="email"
        placeholder="you@example.com"
        autocomplete="email"
      />

      <div>
        <AuthFormInput
          v-model="password"
          id="login-password"
          label="Password"
          type="password"
          placeholder="Enter your password"
          autocomplete="current-password"
        />
        <div class="flex justify-end mt-1.5">
          <router-link
            to="/forgot-password"
            class="text-xs text-primary-green hover:text-forest-green transition-colors font-medium"
          >
            Forgot password?
          </router-link>
        </div>
      </div>

      <PrimaryButton :loading="loading" label="Sign In" loading-label="Signing in..." />
    </form>

    <SocialLoginButtons @success="onSuccess" />

    <p class="text-center text-sm text-gray-500 mt-6">
      Don't have an account?
      <router-link
        to="/register"
        class="text-primary-green hover:text-forest-green font-semibold transition-colors"
      >
        Sign up
      </router-link>
    </p>
  </AuthLayout>
</template>
