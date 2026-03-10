<script setup>
import { ref } from 'vue'

defineProps({
  sighting: { type: Object, required: true },
})

defineEmits(['open-detail'])

const imgError = ref(false)

function relativeDate(dateStr) {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now - date
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'
  if (diffDays < 7) return `${diffDays} days ago`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}w ago`
  if (diffDays < 365) return `${Math.floor(diffDays / 30)}mo ago`
  return `${Math.floor(diffDays / 365)}y ago`
}
</script>

<template>
  <div
    role="button"
    tabindex="0"
    class="bg-white rounded-xl border border-gray-200 overflow-hidden hover:shadow-md transition-shadow cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary-green focus:ring-offset-2"
    @click="$emit('open-detail', sighting)"
    @keydown.enter.space.prevent="$emit('open-detail', sighting)"
  >
    <div class="aspect-square bg-gray-100 relative">
      <img
        v-if="sighting.image_url && !imgError"
        :src="sighting.image_url"
        :alt="sighting.bird_name"
        loading="lazy"
        class="w-full h-full object-cover"
        @error="imgError = true"
      />
      <div v-else class="w-full h-full flex items-center justify-center text-gray-300">
        <svg class="w-12 h-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z" />
        </svg>
      </div>
    </div>
    <div class="p-3">
      <p class="text-sm font-semibold text-gray-900 truncate">{{ sighting.bird_name }}</p>
      <p class="text-xs text-gray-500 mt-0.5">by {{ sighting.user_name }}</p>
      <p class="text-xs text-gray-400 mt-0.5">{{ relativeDate(sighting.created_at) }}</p>
    </div>
  </div>
</template>
