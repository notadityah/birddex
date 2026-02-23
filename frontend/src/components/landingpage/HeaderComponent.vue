<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLoadAnimation } from '@/composables/useAnimation'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const mobileMenuOpen = ref(false)
const headerRef = ref(null)

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}

const navLinks = []

useLoadAnimation(headerRef, { y: 0, duration: 0.6 })
</script>

<template>
  <header ref="headerRef" class="w-full bg-forest-green border-b border-white/10 sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between items-center h-16">
        <!-- Logo -->
        <router-link to="/" class="shrink-0 flex items-center gap-2">
          <div
            class="w-8 h-8 bg-primary-green rounded-full flex items-center justify-center text-white"
          >
            <img src="/bird-svgrepo-com.svg" alt="Bird" width="20" height="20" />
          </div>
          <span class="font-bold text-xl text-white tracking-tight">BirdDex</span>
        </router-link>

        <!-- Desktop Navigation -->
        <nav class="hidden md:flex items-center space-x-8">
          <a
            v-for="link in navLinks"
            :key="link.label"
            :href="link.href"
            class="text-gray-300 hover:text-primary-green font-medium transition-colors text-sm"
          >
            {{ link.label }}
          </a>
        </nav>

        <!-- Desktop Auth -->
        <div class="hidden md:flex items-center gap-4">
          <template v-if="authStore.isAuthenticated">
            <router-link
              to="/mydex"
              class="text-gray-300 hover:text-white font-medium transition-colors text-sm"
            >
              MyDex
            </router-link>
            <button
              @click="handleLogout"
              class="text-gray-300 hover:text-red-400 font-medium transition-colors text-sm cursor-pointer"
            >
              Sign out
            </button>
          </template>
          <template v-else>
            <router-link
              to="/login"
              class="text-gray-300 hover:text-white font-medium transition-colors text-sm"
            >
              Login
            </router-link>
            <router-link
              to="/register"
              class="bg-primary-green text-white px-5 py-2 rounded-md font-semibold hover:bg-opacity-90 transition-opacity shadow-sm text-sm"
            >
              Register
            </router-link>
          </template>
        </div>

        <!-- Mobile Menu Button -->
        <button
          @click="mobileMenuOpen = !mobileMenuOpen"
          class="md:hidden text-white p-2 rounded-md hover:bg-white/10 transition-colors"
          aria-label="Toggle menu"
        >
          <svg
            v-if="mobileMenuOpen"
            class="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
          <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 6h16M4 12h16M4 18h16"
            />
          </svg>
        </button>
      </div>
    </div>

    <!-- Mobile Menu -->
    <div
      class="md:hidden overflow-hidden transition-all duration-300 ease-in-out"
      :class="mobileMenuOpen ? 'max-h-64 opacity-100' : 'max-h-0 opacity-0'"
    >
      <div class="px-4 pb-4 space-y-1 border-t border-white/10">
        <a
          v-for="link in navLinks"
          :key="link.label"
          :href="link.href"
          @click="mobileMenuOpen = false"
          class="block py-3 text-gray-300 hover:text-primary-green font-medium transition-colors text-sm"
        >
          {{ link.label }}
        </a>
        <template v-if="authStore.isAuthenticated">
          <router-link
            to="/mydex"
            @click="mobileMenuOpen = false"
            class="block py-3 text-gray-300 hover:text-white font-medium text-sm"
          >
            MyDex
          </router-link>
          <button
            @click="(handleLogout(), (mobileMenuOpen = false))"
            class="block w-full text-left py-3 text-gray-300 hover:text-red-400 font-medium text-sm cursor-pointer"
          >
            Sign out
          </button>
        </template>
        <template v-else>
          <router-link
            to="/login"
            @click="mobileMenuOpen = false"
            class="block py-3 text-gray-300 hover:text-white font-medium text-sm"
          >
            Login
          </router-link>
          <router-link
            to="/register"
            @click="mobileMenuOpen = false"
            class="block py-3 text-primary-green font-semibold text-sm"
          >
            Register
          </router-link>
        </template>
      </div>
    </div>
  </header>
</template>
