<script setup>
import { onMounted, onUnmounted } from 'vue'

defineProps({
  bird: { type: Object, required: true },
})

const emit = defineEmits(['close'])

function onKeydown(e) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  document.body.style.overflow = 'hidden'
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString()
}
</script>

<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
    @click.self="emit('close')"
    role="dialog"
    aria-modal="true"
  >
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 max-h-[85vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-start justify-between p-5 border-b border-gray-100">
        <div>
          <h2 class="text-lg font-bold text-gray-900">{{ bird.name }}</h2>
          <p class="text-sm text-gray-400 italic">{{ bird.scientificName }}</p>
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
        <!-- Sighting count -->
        <div class="mb-4">
          <span
            v-if="bird.sightings?.length"
            class="inline-flex items-center bg-primary-green/10 text-primary-green text-sm font-medium px-3 py-1 rounded-full"
          >
            {{ bird.sightings.length }} sighting{{ bird.sightings.length === 1 ? '' : 's' }}
          </span>
          <span
            v-else
            class="inline-flex items-center bg-gray-100 text-gray-500 text-sm font-medium px-3 py-1 rounded-full"
          >
            Not yet found
          </span>
        </div>

        <!-- Sightings list -->
        <div v-if="bird.sightings?.length" class="space-y-3">
          <div
            v-for="sighting in bird.sightings"
            :key="sighting.id"
            class="bg-gray-50 rounded-lg p-3"
          >
            <div class="flex items-center justify-between">
              <p class="text-sm font-medium text-gray-700">
                {{ formatDate(sighting.detectedAt) }}
              </p>
              <a
                v-if="sighting.imageUrl"
                :href="sighting.imageUrl"
                download
                class="text-xs text-primary-green hover:underline font-medium"
                @click.stop
              >
                Download image
              </a>
            </div>
            <p v-if="sighting.notes" class="text-sm text-gray-500 mt-1">
              {{ sighting.notes }}
            </p>
          </div>
        </div>

        <!-- Empty state -->
        <div v-else class="text-center py-6">
          <p class="text-gray-400 mb-3">No sightings yet.</p>
          <router-link
            :to="{ name: 'detect' }"
            class="text-sm text-primary-green hover:underline font-medium"
            @click="emit('close')"
          >
            Go to Detect to find this bird
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>
