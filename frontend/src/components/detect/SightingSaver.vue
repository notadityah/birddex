<script setup>
import { ref } from 'vue'

defineProps({
  bird: { type: Object, required: true },
  saving: { type: Boolean, default: false },
  alreadyFound: { type: Boolean, default: false },
})

const emit = defineEmits(['save'])
const notes = ref('')
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl p-5">
    <h3 class="text-lg font-semibold text-gray-900 mb-1">Save Sighting</h3>
    <p class="text-sm text-gray-500 mb-4">
      Detected <span class="font-medium text-gray-800">{{ bird.name }}</span>
    </p>

    <p
      v-if="alreadyFound"
      class="mb-3 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded-lg px-3 py-2"
    >
      You've already recorded this bird — save again to add another sighting.
    </p>

    <label class="block text-sm font-medium text-gray-700 mb-1">Notes (optional)</label>
    <textarea
      v-model="notes"
      rows="2"
      class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-green focus:border-transparent resize-none"
      placeholder="Where did you spot it?"
    />

    <button
      type="button"
      :disabled="saving"
      @click="emit('save', notes)"
      class="mt-3 px-5 py-2 bg-primary-green text-white text-sm font-medium rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 cursor-pointer"
    >
      <span v-if="saving">Saving...</span>
      <span v-else>Save to MyDex</span>
    </button>
  </div>
</template>
