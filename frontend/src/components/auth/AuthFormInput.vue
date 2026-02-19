<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  id: { type: String, required: true },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: '' },
  error: { type: String, default: '' },
})

const model = defineModel()
const showPassword = ref(false)

const resolvedType = computed(() => {
  if (props.type === 'password' && showPassword.value) return 'text'
  return props.type
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-1.5">
      <label :for="id" class="block text-sm font-medium text-gray-700">
        {{ label }}
      </label>
      <button
        v-if="type === 'password'"
        type="button"
        @click="showPassword = !showPassword"
        class="text-xs text-gray-400 hover:text-gray-600 transition-colors cursor-pointer select-none"
        :aria-label="showPassword ? 'Hide password' : 'Show password'"
      >
        {{ showPassword ? 'Hide' : 'Show' }}
      </button>
    </div>
    <input
      :id="id"
      v-model="model"
      :type="resolvedType"
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
