<script setup>
import { birdImageUrl } from '@/data/birdImages'

defineProps({
  predictions: { type: Array, required: true },
  selectedSlug: { type: String, default: null },
})

const emit = defineEmits(['select'])

function formatLabel(slug) {
  return slug
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

function onImageError(e) {
  e.target.src = '/bird-in-flight-origami-svgrepo-com.svg'
}
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl p-5">
    <h3 class="text-lg font-semibold text-gray-900 mb-1">Results</h3>

    <div v-if="predictions.length === 0" class="text-center py-4">
      <p class="text-gray-500">No birds detected — try a clearer photo.</p>
    </div>

    <template v-else>
      <p class="text-sm text-gray-500 mb-4">Not this bird? Tap another match.</p>
      <ul class="space-y-2">
        <li v-for="(pred, i) in predictions" :key="pred.label">
          <button
            type="button"
            @click="emit('select', pred.label)"
            :aria-pressed="pred.label === selectedSlug"
            class="w-full flex items-center gap-3 rounded-lg border p-2 text-left transition-colors cursor-pointer"
            :class="
              pred.label === selectedSlug
                ? 'border-primary-green bg-green-50/50 ring-1 ring-primary-green'
                : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
            "
          >
            <img
              :src="birdImageUrl(pred.label)"
              :alt="`${formatLabel(pred.label)} reference`"
              loading="lazy"
              class="w-12 h-12 rounded-md object-cover bg-gray-100 shrink-0"
              @error="onImageError"
            />
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-1">
                <span class="text-sm font-medium text-gray-800 truncate">
                  {{ formatLabel(pred.label) }}
                  <span
                    v-if="i === 0"
                    class="ml-2 inline-block px-2 py-0.5 text-xs font-semibold bg-green-100 text-green-800 rounded-full"
                  >
                    Best Match
                  </span>
                </span>
                <span class="text-sm text-gray-500 ml-2 shrink-0">{{ pred.confidence }}%</span>
              </div>
              <div class="w-full bg-gray-100 rounded-full h-2">
                <div
                  class="bg-primary-green h-2 rounded-full transition-all duration-300"
                  :style="{ width: pred.confidence + '%' }"
                />
              </div>
            </div>
          </button>
        </li>
      </ul>
    </template>
  </div>
</template>
