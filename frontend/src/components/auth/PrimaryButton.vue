<script setup>
import SpinnerIcon from '@/components/SpinnerIcon.vue'

defineProps({
  loading: { type: Boolean, default: false },
  label: { type: String, required: true },
  loadingLabel: { type: String, required: true },
})
</script>

<template>
  <button
    type="submit"
    :disabled="loading"
    class="w-full flex items-center justify-center gap-2 bg-forest-green text-white py-2.5 rounded-lg font-semibold text-sm hover:shadow-lg hover:shadow-forest-green/25 transition-all mt-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
  >
    <SpinnerIcon v-if="loading" />
    {{ loading ? loadingLabel : label }}
  </button>
</template>
