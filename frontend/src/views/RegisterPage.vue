<script setup>
import { ref } from 'vue'
import { useLoadAnimation } from '@/composables/useAnimation'
import Header from '@/components/landingpage/HeaderComponent.vue'
import AuthFormInput from '@/components/auth/AuthFormInput.vue'
import SocialLoginButtons from '@/components/auth/SocialLoginButtons.vue'

const cardRef = ref(null)

const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')

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
          <h1 class="text-2xl font-bold text-gray-900">Create your account</h1>
          <p class="text-sm text-gray-500 mt-1">Start building your bird collection</p>
        </div>

        <!-- Form -->
        <form @submit.prevent class="space-y-4">
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
          />

          <button
            type="submit"
            class="w-full bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all mt-2 cursor-pointer"
          >
            Create Account
          </button>
        </form>

        <!-- Social Login -->
        <SocialLoginButtons />

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
