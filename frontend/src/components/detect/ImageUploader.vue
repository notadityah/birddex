<script setup>
import { ref } from 'vue'

const emit = defineEmits(['file-selected'])

const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const MAX_SIZE = 20 * 1024 * 1024 // 20MB

const dragging = ref(false)
const validationError = ref(null)
const fileInput = ref(null)
const cameraInput = ref(null)
const hasCamera = ref('mediaDevices' in navigator)

function validateAndEmit(file) {
  validationError.value = null
  if (!file) return

  if (!ALLOWED_TYPES.has(file.type)) {
    validationError.value = 'Invalid file type. Please use JPG, PNG, or WebP.'
    return
  }
  if (file.size > MAX_SIZE) {
    validationError.value = 'File is too large. Maximum size is 20MB.'
    return
  }
  emit('file-selected', file)
}

function onFileChange(e) {
  validateAndEmit(e.target.files?.[0])
}

function onDrop(e) {
  dragging.value = false
  const file = e.dataTransfer?.files?.[0]
  validateAndEmit(file)
}

function openFilePicker() {
  fileInput.value?.click()
}

function openCamera() {
  cameraInput.value?.click()
}
</script>

<template>
  <div
    class="border-2 border-dashed rounded-xl p-8 text-center transition-colors"
    :class="dragging ? 'border-primary-green bg-green-50' : 'border-gray-300'"
    @dragover.prevent="dragging = true"
    @dragleave.prevent="dragging = false"
    @drop.prevent="onDrop"
  >
    <div class="text-gray-400 mb-4">
      <svg class="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.5"
          d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
        />
      </svg>
    </div>
    <p class="text-sm text-gray-500 mb-4">Drag and drop a bird photo here, or</p>
    <div class="flex justify-center gap-3">
      <button
        type="button"
        @click="openFilePicker"
        class="px-4 py-2 bg-primary-green text-white text-sm font-medium rounded-lg hover:bg-green-700 transition-colors cursor-pointer"
      >
        Choose File
      </button>
      <button
        v-if="hasCamera"
        type="button"
        @click="openCamera"
        class="px-4 py-2 bg-white text-gray-700 text-sm font-medium rounded-lg border border-gray-300 hover:border-gray-400 transition-colors cursor-pointer"
      >
        Take Photo
      </button>
    </div>
    <p class="text-xs text-gray-400 mt-3">JPG, PNG, or WebP up to 20MB</p>

    <p v-if="validationError" class="mt-3 text-sm text-red-600">{{ validationError }}</p>

    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      class="hidden"
      @change="onFileChange"
    />
    <input
      ref="cameraInput"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      capture="environment"
      class="hidden"
      @change="onFileChange"
    />
  </div>
</template>
