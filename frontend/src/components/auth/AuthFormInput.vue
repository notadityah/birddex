<script setup>
defineProps({
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  id: { type: String, required: true },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: '' },
  error: { type: String, default: '' },
})

const model = defineModel()
</script>

<template>
  <div>
    <label :for="id" class="block text-sm font-medium text-gray-700 mb-1.5">
      {{ label }}
    </label>
    <input
      :id="id"
      v-model="model"
      :type="type"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      :class="[
        'w-full px-4 py-2.5 border rounded-lg text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 transition-colors',
        error
          ? 'border-red-500 focus:ring-red-400/40 focus:border-red-500'
          : 'border-gray-300 focus:ring-primary-green/40 focus:border-primary-green',
      ]"
    />
    <p v-if="error" class="text-xs text-red-600 mt-1">{{ error }}</p>
  </div>
</template>
