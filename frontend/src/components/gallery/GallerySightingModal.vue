<script setup>
import { ref } from 'vue'
import { useFocusTrap } from '@/composables/useFocusTrap'
import { useModalLifecycle } from '@/composables/useModalLifecycle'
import { formatDateLong as formatDate } from '@/utils/dateFormat'

defineProps({
  sighting: { type: Object, required: true },
})

const emit = defineEmits(['close'])
const modalRef = ref(null)

useFocusTrap(modalRef)
useModalLifecycle(() => emit('close'))
</script>

<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center py-4 bg-black/50"
    @click.self="emit('close')"
    role="dialog"
    aria-modal="true"
    aria-labelledby="gallery-detail-title"
  >
    <div ref="modalRef" class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 max-h-[85vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-start justify-between p-5 border-b border-gray-100">
        <div>
          <h2 id="gallery-detail-title" class="text-lg font-bold text-gray-900">{{ sighting.bird_name }}</h2>
          <p class="text-sm text-gray-500 italic">{{ sighting.scientific_name }}</p>
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

      <!-- Body -->
      <div class="p-5 overflow-y-auto flex-1">
        <!-- Image -->
        <div v-if="sighting.image_url" class="mb-4 rounded-lg overflow-hidden">
          <img
            :src="sighting.image_url"
            :alt="sighting.bird_name"
            class="w-full object-contain max-h-96 bg-gray-100"
          />
        </div>

        <!-- Meta -->
        <div class="space-y-2">
          <p class="text-sm text-gray-700">
            <span class="font-medium">Spotted by</span> {{ sighting.user_name }}
          </p>
          <p class="text-sm text-gray-700">
            <span class="font-medium">Date</span> {{ formatDate(sighting.detected_at || sighting.created_at) }}
          </p>
          <p v-if="sighting.notes" class="text-sm text-gray-600 mt-3 bg-gray-50 rounded-lg p-3">
            {{ sighting.notes }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
