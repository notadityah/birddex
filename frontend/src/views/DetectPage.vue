<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBirdStore, mapBirdRow } from '@/stores/birds'
import { useImageResize } from '@/composables/useImageResize'
import { birdImageUrl, birdImageAttribution } from '@/data/birdImages'
import ImageUploader from '@/components/detect/ImageUploader.vue'
import DetectionResults from '@/components/detect/DetectionResults.vue'
import BirdStatsCard from '@/components/detect/BirdStatsCard.vue'
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
const uploadBlob = ref(null)
const predictions = ref([])
const matchedBird = ref(null)
// Full stats for every predicted bird, keyed by slug, so tapping another
// prediction swaps the matched bird without a network request.
const birdsBySlug = ref({})
const error = ref(null)

// Dex completion for the saved screen. Guarded: the store may be empty if
// loadBirds() failed, and 0/0 must not render as NaN%.
const dexPercent = computed(() =>
  birdStore.totalCount > 0
    ? Math.round((100 * birdStore.foundCount) / birdStore.totalCount)
    : null,
)

// The already-found check and dex % both read the bird store, so make sure
// it's populated even when the user lands directly on /detect.
onMounted(() => birdStore.loadBirds())

// AbortController: cancels in-flight detect API call on unmount or new photo selection,
// preventing stale results from appearing after the user has moved on.
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
    const [detectResult, uploadResult] = await Promise.all([
      resizeImage(file, 1280, 0.85),
      resizeImage(file, 2048, 0.92),
    ])
    resizedBase64.value = detectResult.base64
    resizedBlob.value = detectResult.blob
    uploadBlob.value = uploadResult.blob
    previewUrl.value = uploadResult.previewUrl
    URL.revokeObjectURL(detectResult.previewUrl)
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
      // topN=5: show top 5 predictions — enough for the user to pick the right bird
      // without overwhelming the UI. Max is 36 (all species) on the backend.
      body: JSON.stringify({ imageBase64: resizedBase64.value, topN: 5 }),
    })

    if (res.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (!res.ok) throw new Error('Detection failed')

    const data = await res.json()
    predictions.value = data.predictions
    // Normalize snake_case DB rows to the store's camelCase bird shape once,
    // here, so BirdStatsCard/SightingSaver see a single consistent contract.
    const map = {}
    for (const [slug, row] of Object.entries(data.birds ?? {})) {
      map[slug] = mapBirdRow(row)
    }
    birdsBySlug.value = map
    matchedBird.value = data.bird ? mapBirdRow(data.bird) : null
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

    // Upload image to S3 via presigned URL (higher-quality 2048px version)
    if (uploadUrl && uploadBlob.value) {
      const uploadRes = await fetch(uploadUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'image/jpeg' },
        body: uploadBlob.value,
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

function selectBird(slug) {
  // Prefer the detect response (full stats); fall back to the store in case a
  // slug is missing from the map (e.g. bird removed from DB after detection).
  matchedBird.value =
    birdsBySlug.value[slug] ??
    birdStore.birds.find((b) => b.slug === slug) ??
    matchedBird.value
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
  uploadBlob.value = null
  predictions.value = []
  matchedBird.value = null
  birdsBySlug.value = {}
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
      <div v-if="dexPercent !== null" class="max-w-xs mx-auto mb-6">
        <div class="flex items-center justify-between text-sm mb-1">
          <span class="font-medium text-gray-700">
            Dex {{ birdStore.foundCount }}/{{ birdStore.totalCount }}
          </span>
          <span class="text-gray-500">{{ dexPercent }}%</span>
        </div>
        <div class="w-full bg-gray-100 rounded-full h-2">
          <div
            class="bg-primary-green h-2 rounded-full transition-all duration-500"
            :style="{ width: dexPercent + '%' }"
          />
        </div>
      </div>
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

      <!-- Image preview: side-by-side with the reference photo once a bird is
           matched, so a wrong match is obvious at a glance -->
      <div v-if="previewUrl && pageState !== 'idle'" class="mb-4">
        <div
          v-if="['results', 'saving'].includes(pageState) && matchedBird"
          class="grid grid-cols-2 gap-3"
        >
          <figure>
            <img
              :src="previewUrl"
              alt="Selected bird photo"
              class="w-full h-48 sm:h-56 rounded-xl object-cover bg-gray-100"
            />
            <figcaption class="mt-1 text-xs text-center text-gray-500">Your photo</figcaption>
          </figure>
          <figure>
            <img
              :src="birdImageUrl(matchedBird.slug)"
              :alt="`Reference photo of ${matchedBird.name}`"
              class="w-full h-48 sm:h-56 rounded-xl object-cover bg-gray-100"
              @error="(e) => (e.target.src = '/bird-in-flight-origami-svgrepo-com.svg')"
            />
            <figcaption class="mt-1 text-xs text-center text-gray-500">
              Reference — {{ matchedBird.name }}
              <template v-if="birdImageAttribution(matchedBird.slug)">
                <br />
                <a
                  :href="birdImageAttribution(matchedBird.slug).sourceUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="text-[10px] underline hover:text-gray-700"
                >
                  Photo: {{ birdImageAttribution(matchedBird.slug).author }}
                  ({{ birdImageAttribution(matchedBird.slug).license }})
                </a>
              </template>
            </figcaption>
          </figure>
        </div>
        <img
          v-else
          :src="previewUrl"
          alt="Selected bird photo"
          class="w-full rounded-xl object-contain max-h-96 bg-gray-100"
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
        <DetectionResults
          :predictions="predictions"
          :selected-slug="matchedBird?.slug ?? null"
          class="mb-4"
          @select="selectBird"
        />

        <BirdStatsCard v-if="matchedBird" :bird="matchedBird" :show-photo="false" class="mb-4" />

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
