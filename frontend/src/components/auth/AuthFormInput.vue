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

const inputType = computed(() => {
  if (props.type !== 'password') return props.type
  return showPassword.value ? 'text' : 'password'
})
</script>

<template>
  <div>
    <label :for="id" class="block text-sm font-medium text-gray-700 mb-1.5">
      {{ label }}
    </label>
    <div class="relative">
      <input
        :id="id"
        v-model="model"
        :type="inputType"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        :class="[
          'w-full px-4 py-2.5 border rounded-lg text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 transition-colors',
          type === 'password' ? 'pr-10' : '',
          error
            ? 'border-red-500 focus:ring-red-400/40 focus:border-red-500'
            : 'border-gray-300 focus:ring-primary-green/40 focus:border-primary-green',
        ]"
      />
      <button
        v-if="type === 'password'"
        type="button"
        @click="showPassword = !showPassword"
        class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors cursor-pointer"
        tabindex="-1"
      >
        <img
          v-if="!showPassword"
          src="/eye-open-svgrepo-com.svg"
          alt="Show password"
          class="w-5 h-5"
        />
        <img v-else src="/eye-close-svgrepo-com.svg" alt="Hide password" class="w-5 h-5" />
      </button>
    </div>
    <p v-if="error" class="text-xs text-red-600 mt-1">{{ error }}</p>
  </div>
</template>
