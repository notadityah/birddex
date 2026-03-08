<script setup>
defineProps({
  predictions: { type: Array, required: true },
  bird: { type: Object, default: null },
})

function formatLabel(slug) {
  return slug
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl p-5">
    <h3 class="text-lg font-semibold text-gray-900 mb-4">Results</h3>

    <div v-if="predictions.length === 0" class="text-center py-4">
      <p class="text-gray-500">No birds detected — try a clearer photo.</p>
    </div>

    <ul v-else class="space-y-3">
      <li v-for="(pred, i) in predictions" :key="pred.label">
        <div class="flex items-center justify-between mb-1">
          <span class="text-sm font-medium text-gray-800">
            {{ formatLabel(pred.label) }}
            <span
              v-if="i === 0"
              class="ml-2 inline-block px-2 py-0.5 text-xs font-semibold bg-green-100 text-green-800 rounded-full"
            >
              Best Match
            </span>
          </span>
          <span class="text-sm text-gray-500">{{ pred.confidence }}%</span>
        </div>
        <div class="w-full bg-gray-100 rounded-full h-2">
          <div
            class="bg-primary-green h-2 rounded-full transition-all duration-300"
            :style="{ width: pred.confidence + '%' }"
          />
        </div>
      </li>
    </ul>
  </div>
</template>
