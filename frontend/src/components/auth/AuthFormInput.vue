<script setup>
defineProps({
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  id: { type: String, required: true },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: '' },
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
      class="w-full px-4 py-2.5 border border-gray-300 rounded-lg text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-green/40 focus:border-primary-green transition-colors"
    />
  </div>
</template>
