<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useFocusTrap } from '@/composables/useFocusTrap'
import { useBirdStore } from '@/stores/birds'

defineProps({
  bird: { type: Object, required: true },
})

const emit = defineEmits(['close'])
const modalRef = ref(null)
const birdStore = useBirdStore()
const togglingIds = ref(new Set())

useFocusTrap(modalRef)

async function togglePublic(sighting) {
  togglingIds.value.add(sighting.id)
  await birdStore.toggleSightingPublic(sighting.id, !sighting.public)
  togglingIds.value.delete(sighting.id)
}

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
    class="fixed inset-0 z-50 flex items-center justify-center py-4 bg-black/50"
    @click.self="emit('close')"
    role="dialog"
    aria-modal="true"
    aria-labelledby="bird-detail-title"
  >
    <div ref="modalRef" class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 max-h-[85vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-start justify-between p-5 border-b border-gray-100">
        <div>
          <h2 id="bird-detail-title" class="text-lg font-bold text-gray-900">{{ bird.name }}</h2>
          <p class="text-sm text-gray-500 italic">{{ bird.scientificName }}</p>
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
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  :disabled="togglingIds.has(sighting.id)"
                  @click="togglePublic(sighting)"
                  class="p-1 rounded transition-colors cursor-pointer"
                  :class="sighting.public ? 'text-green-600 hover:text-green-700' : 'text-gray-400 hover:text-gray-500'"
                  :title="sighting.public ? 'Shared to gallery — click to unshare' : 'Share to gallery'"
                  :aria-label="sighting.public ? 'Unshare from gallery' : 'Share to gallery'"
                >
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 21a9.004 9.004 0 008.716-6.747M12 21a9.004 9.004 0 01-8.716-6.747M12 21c2.485 0 4.5-4.03 4.5-9S14.485 3 12 3m0 18c-2.485 0-4.5-4.03-4.5-9S9.515 3 12 3m0 0a8.997 8.997 0 017.843 4.582M12 3a8.997 8.997 0 00-7.843 4.582m15.686 0A11.953 11.953 0 0112 10.5c-2.998 0-5.74-1.1-7.843-2.918m15.686 0A8.959 8.959 0 0121 12c0 .778-.099 1.533-.284 2.253m0 0A17.919 17.919 0 0112 16.5a17.92 17.92 0 01-8.716-2.247m0 0A9 9 0 013 12c0-1.605.42-3.113 1.157-4.418" />
                  </svg>
                </button>
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
