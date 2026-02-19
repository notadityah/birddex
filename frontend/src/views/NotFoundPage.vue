<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-4 bg-white/90 backdrop-blur-none">
    <div class="text-center">
      <p class="text-8xl font-bold text-primary-green">404</p>
      <h1 class="mt-4 text-2xl font-semibold text-gray-800">Page not found</h1>
      <p class="mt-2 text-gray-500 text-sm">The page you're looking for doesn't exist.</p>
      <button
        @click="router.push('/')"
        class="mt-6 px-5 py-2.5 bg-primary-green hover:bg-forest-green text-white text-sm font-medium rounded-lg transition-colors cursor-pointer"
      >
        Go home
      </button>
    </div>
  </div>
</template>
