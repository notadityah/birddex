<script setup>
import { ref } from 'vue'

const imgError = ref(false)

defineProps({
  bird: { type: Object, required: true },
})

const emit = defineEmits(['open-detail'])
</script>

<template>
  <div
    @click="emit('open-detail', bird)"
    @keydown.enter.space.prevent="emit('open-detail', bird)"
    role="button"
    tabindex="0"
    class="cursor-pointer hover:ring-2 hover:ring-primary-green focus:ring-2 focus:ring-primary-green focus:outline-none bg-white rounded-xl shadow-sm hover:shadow-md transition-shadow overflow-hidden"
  >
    <div class="aspect-square relative">
      <img
        v-if="bird.found && bird.imageUrl && !imgError"
        :src="bird.imageUrl"
        :alt="bird.name"
        class="w-full h-full object-cover"
        loading="lazy"
        @error="imgError = true"
      />
      <div v-else class="w-full h-full bg-gray-100 flex items-center justify-center">
        <img src="/bird-in-flight-origami-svgrepo-com.svg" alt="Not found" class="w-16 h-16 opacity-30" />
      </div>
      <span
        v-if="bird.found"
        class="absolute top-2 right-2 bg-primary-green text-white text-xs font-semibold px-2 py-0.5 rounded-full"
      >
        Found
      </span>
    </div>
    <div class="p-3">
      <p class="font-semibold text-sm text-gray-900 truncate">{{ bird.name }}</p>
      <p class="text-xs text-gray-500 italic truncate">{{ bird.scientificName }}</p>
    </div>
  </div>
</template>
