<script setup>
import { ref } from 'vue'
import { useLoadAnimation } from '@/composables/useAnimation'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, required: true },
})

const cardRef = ref(null)
useLoadAnimation(cardRef, { y: 30, duration: 0.7 })
</script>

<template>
  <main class="flex-1 flex items-center justify-center px-4 py-12 bg-white/90 backdrop-blur-none">
    <div ref="cardRef" class="w-full max-w-md bg-white rounded-2xl shadow-xl p-8 sm:p-10">
      <!-- Logo and heading -->
      <div class="text-center mb-8">
        <router-link to="/" class="inline-flex items-center gap-2 mb-4">
          <div class="w-10 h-10 bg-primary-green rounded-full flex items-center justify-center">
            <img src="/bird-svgrepo-com.svg" alt="BirdDex" width="24" height="24" />
          </div>
        </router-link>
        <h1 class="text-2xl font-bold text-gray-900">{{ title }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ subtitle }}</p>
      </div>

      <slot />
    </div>
  </main>
</template>
