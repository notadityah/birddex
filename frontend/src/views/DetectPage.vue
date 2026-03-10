<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBirdStore } from '@/stores/birds'
import { useImageResize } from '@/composables/useImageResize'
import ImageUploader from '@/components/detect/ImageUploader.vue'
import DetectionResults from '@/components/detect/DetectionResults.vue'
import SightingSaver from '@/components/detect/SightingSaver.vue'
import SpinnerIcon from '@/components/SpinnerIcon.vue'

const API = import.meta.env.VITE_API_URL
const router = useRouter()
const birdStore = useBirdStore()
const { resizeImage } = useImageResize()

// State machine: idle | selected | detecting | results | saving | saved | error
const pageState = ref('idle')
const selectedFile = ref(null)
const previewUrl = ref(null)
const resizedBase64 = ref(null)
const resizedBlob = ref(null)
const predictions = ref([])
const matchedBird = ref(null)
const error = ref(null)

let abortController = null

function cleanup() {
  if (abortController) abortController.abort()
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
}

onUnmounted(cleanup)

async function onFileSelected(file) {
  cleanup()
  error.value = null
  selectedFile.value = file
  pageState.value = 'selected'

  try {
    const result = await resizeImage(file)
    resizedBase64.value = result.base64
    resizedBlob.value = result.blob
    previewUrl.value = result.previewUrl
  } catch {
    error.value = 'Failed to process image. Try a different photo.'
    pageState.value = 'error'
  }
}

async function detect() {
  if (!resizedBase64.value) return
  error.value = null
  pageState.value = 'detecting'
  abortController = new AbortController()

  try {
    const res = await fetch(`${API}/api/detect`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      signal: abortController.signal,
      body: JSON.stringify({ imageBase64: resizedBase64.value, topN: 5 }),
    })

    if (res.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (!res.ok) throw new Error('Detection failed')

    const data = await res.json()
    predictions.value = data.predictions
    matchedBird.value = data.bird
    pageState.value = 'results'
  } catch (err) {
    if (err.name === 'AbortError') return
    error.value = 'Detection failed. Please try again.'
    pageState.value = 'error'
  }
}

async function saveSighting(notes, isPublic) {
  if (!matchedBird.value) return
  pageState.value = 'saving'
  error.value = null

  try {
    // Create sighting with image upload
    const res = await fetch(`${API}/api/sightings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        birdId: matchedBird.value.id,
        imageExt: 'jpg',
        notes: notes || undefined,
        public: isPublic || undefined,
      }),
    })

    if (!res.ok) throw new Error('Failed to save sighting')
    const { uploadUrl } = await res.json()

    // Upload image to S3 via presigned URL
    if (uploadUrl && resizedBlob.value) {
      const uploadRes = await fetch(uploadUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'image/jpeg' },
        body: resizedBlob.value,
      })
      if (!uploadRes.ok) throw new Error('Failed to upload image')
    }

    // Update bird store optimistically
    const bird = birdStore.birds.find((b) => b.slug === matchedBird.value.slug)
    if (bird) bird.found = true

    // Refresh sightings in background to get presigned image URLs
    birdStore.loadSightings()

    pageState.value = 'saved'
  } catch {
    error.value = 'Failed to save sighting. Please try again.'
    pageState.value = 'results'
  }
}

function retry() {
  if (resizedBase64.value) {
    detect()
  } else {
    reset()
  }
}

function reset() {
  cleanup()
  selectedFile.value = null
  previewUrl.value = null
  resizedBase64.value = null
  resizedBlob.value = null
  predictions.value = []
  matchedBird.value = null
  error.value = null
  pageState.value = 'idle'
}

function changePhoto() {
  reset()
}
</script>

<template>
  <div class="max-w-xl mx-auto px-4 sm:px-6 pt-16 pb-6">
    <!-- Header -->
    <div class="mb-6 text-center">
      <h1 class="text-2xl font-bold text-gray-900">Detect</h1>
      <p class="text-sm text-gray-500 mt-1">Upload a photo to identify a bird.</p>
    </div>

    <!-- Error banner -->
    <div
      v-if="error"
      role="alert"
      class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between"
    >
      <p class="text-sm text-red-700">{{ error }}</p>
      <button
        @click="retry"
        class="text-sm font-medium text-red-700 hover:text-red-900 underline cursor-pointer"
      >
        Retry
      </button>
    </div>

    <!-- Saved success -->
    <div v-if="pageState === 'saved'" role="status" class="text-center py-12">
      <div class="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
        <svg class="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <h2 class="text-xl font-semibold text-gray-900 mb-2">Sighting saved!</h2>
      <p class="text-gray-500 mb-6">
        <span class="font-medium">{{ matchedBird?.name }}</span> has been added to your collection.
      </p>
      <div class="flex justify-center gap-3">
        <router-link
          :to="{ name: 'mydex' }"
          class="px-5 py-2 bg-primary-green text-white text-sm font-medium rounded-lg hover:bg-green-700 transition-colors"
        >
          View in MyDex
        </router-link>
        <button
          @click="reset"
          type="button"
          class="px-5 py-2 bg-white text-gray-700 text-sm font-medium rounded-lg border border-gray-300 hover:border-gray-400 transition-colors cursor-pointer"
        >
          Detect Another
        </button>
      </div>
    </div>

    <!-- Main content -->
    <div v-else>
      <ImageUploader v-if="pageState === 'idle'" @file-selected="onFileSelected" />

      <!-- Image preview -->
      <div v-if="previewUrl && pageState !== 'idle'" class="mb-4">
        <img
          :src="previewUrl"
          alt="Selected bird photo"
          class="w-full rounded-xl object-cover max-h-80"
        />
        <button
          v-if="pageState === 'selected' || pageState === 'results'"
          @click="changePhoto"
          type="button"
          class="mt-2 text-sm text-primary-green hover:underline cursor-pointer"
        >
          Change photo
        </button>
      </div>

      <!-- Detect button -->
      <button
        v-if="pageState === 'selected' || pageState === 'error'"
        @click="detect"
        :disabled="pageState === 'detecting'"
        type="button"
        class="w-full py-3 bg-primary-green text-white font-medium rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 cursor-pointer"
      >
        Detect Bird
      </button>

      <!-- Detecting spinner -->
      <div v-if="pageState === 'detecting'" aria-live="polite" class="text-center py-8">
        <div class="flex items-center justify-center gap-2 text-gray-600">
          <SpinnerIcon />
          <span class="text-sm">Detecting...</span>
        </div>
        <p class="text-xs text-gray-400 mt-2">This may take a moment on first use.</p>
      </div>

      <!-- Results + save -->
      <div v-if="['results', 'saving'].includes(pageState)">
        <DetectionResults :predictions="predictions" :bird="matchedBird" class="mb-4" />

        <SightingSaver
          v-if="matchedBird"
          :bird="matchedBird"
          :saving="pageState === 'saving'"
          :already-found="!!birdStore.birds.find((b) => b.slug === matchedBird.slug)?.found"
          @save="saveSighting"
        />
      </div>
    </div>
  </div>
</template>
