<script setup>
defineProps({
  message: { type: String, required: true },
})
</script>

<template>
  <div class="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3 mb-4">
    {{ message }}
  </div>
</template>
