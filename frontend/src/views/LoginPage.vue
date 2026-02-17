<script setup>
import { ref } from 'vue'
import { useLoadAnimation } from '@/composables/useAnimation'
import Header from '@/components/landingpage/HeaderComponent.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import SocialLoginButtons from '@/components/auth/SocialLoginButtons.vue'

const cardRef = ref(null)

const email = ref('')
const password = ref('')

useLoadAnimation(cardRef, { y: 30, duration: 0.7 })
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
          <h1 class="text-2xl font-bold text-gray-900">Welcome back</h1>
          <p class="text-sm text-gray-500 mt-1">Sign in to your BirdDex account</p>
        </div>

        <!-- Form -->
        <form @submit.prevent class="space-y-4">
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
              <a
                href="#"
                class="text-xs text-primary-green hover:text-forest-green transition-colors font-medium"
              >
                Forgot password?
              </a>
            </div>
          </div>

          <button
            type="submit"
            class="w-full bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all mt-2 cursor-pointer"
          >
            Sign In
          </button>
        </form>

        <!-- Social Login -->
        <SocialLoginButtons />

        <!-- Register link -->
        <p class="text-center text-sm text-gray-500 mt-6">
          Don't have an account?
          <router-link
            to="/register"
            class="text-primary-green hover:text-forest-green font-semibold transition-colors"
          >
            Sign up
          </router-link>
        </p>
      </div>
    </main>
  </div>
</template>
