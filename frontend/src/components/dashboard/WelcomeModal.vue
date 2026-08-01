<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useFocusTrap } from '@/composables/useFocusTrap'
import { useModalLifecycle } from '@/composables/useModalLifecycle'
import { onboardingSteps } from '@/constants/onboardingSteps'

const emit = defineEmits(['close'])
const modalRef = ref(null)
const authStore = useAuthStore()
const router = useRouter()

useFocusTrap(modalRef)
useModalLifecycle(() => emit('close'))

function goToDetect() {
  emit('close')
  router.push('/detect')
}
</script>

<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center py-4 bg-black/50"
    @click.self="emit('close')"
    role="dialog"
    aria-modal="true"
    aria-labelledby="welcome-title"
  >
    <div
      ref="modalRef"
      class="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 max-h-[85vh] flex flex-col"
    >
      <!-- Header -->
      <div class="flex items-start justify-between p-5 border-b border-gray-100">
        <div>
          <h2 id="welcome-title" class="text-lg font-bold text-gray-900">
            Welcome to BirdDex, {{ authStore.displayName }}!
          </h2>
          <p class="text-sm text-gray-500 mt-0.5">Here's how it works.</p>
        </div>
        <button
          @click="emit('close')"
          class="text-gray-400 hover:text-gray-600 transition-colors cursor-pointer p-1"
          aria-label="Close"
        >
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Steps -->
      <div class="p-5 overflow-y-auto flex-1 space-y-5">
        <div v-for="step in onboardingSteps" :key="step.number" class="flex items-start gap-4">
          <div
            class="w-11 h-11 shrink-0 bg-forest-green/10 rounded-full flex items-center justify-center border-2 border-forest-green"
          >
            <img :src="step.iconSrc" :alt="step.iconAlt" width="20" height="20" />
          </div>
          <div>
            <h3 class="text-sm font-bold text-gray-900">{{ step.title }}</h3>
            <p class="text-sm text-gray-600 leading-relaxed mt-0.5">{{ step.description }}</p>
          </div>
        </div>

        <p class="text-sm text-gray-500 border-t border-gray-100 pt-4">
          Your Dex starts with every bird greyed out — each one fills in once you photograph it.
        </p>
      </div>

      <!-- Actions -->
      <div class="flex items-center justify-end gap-2 p-5 border-t border-gray-100">
        <button
          @click="emit('close')"
          class="px-4 py-2 rounded-lg text-sm font-medium bg-gray-100 text-gray-700 border border-gray-200 hover:bg-gray-200 transition-colors cursor-pointer"
        >
          Explore MyDex
        </button>
        <button
          @click="goToDetect"
          class="px-4 py-2 rounded-lg text-sm font-medium bg-primary-green text-white hover:opacity-90 transition-opacity cursor-pointer"
        >
          Try Detect
        </button>
      </div>
    </div>
  </div>
</template>
