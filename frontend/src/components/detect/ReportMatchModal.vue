<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useFocusTrap } from '@/composables/useFocusTrap'
import { useModalLifecycle } from '@/composables/useModalLifecycle'

const API = import.meta.env.VITE_API_URL
const router = useRouter()

const props = defineProps({
  predictions: { type: Array, default: () => [] },
  matchedBird: { type: Object, default: null },
  // The in-memory 2048px upload blob from DetectPage (jpeg).
  imageBlob: { type: Object, default: null },
  previewUrl: { type: String, default: null },
})

const emit = defineEmits(['close'])

const dialogRef = ref(null)
const note = ref('')
const submitting = ref(false)
const success = ref(false)
const error = ref(null)

useFocusTrap(dialogRef)
useModalLifecycle(() => emit('close'))

function formatLabel(slug) {
  return slug
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

// Auto-captured prediction context, previewed to the user and sent as the message.
const details = computed(() => {
  const lines = []
  if (props.matchedBird) {
    lines.push(`Best match shown: ${props.matchedBird.name} (${props.matchedBird.slug})`)
  }
  if (props.predictions.length) {
    lines.push('Predictions:')
    for (const p of props.predictions) {
      lines.push(`  - ${formatLabel(p.label)} — ${p.confidence}%`)
    }
  }
  return lines.join('\n')
})

async function submit() {
  submitting.value = true
  error.value = null
  try {
    const trimmedNote = note.value.trim()
    let message = `Wrong / missing match reported from the detect screen.\n\n${details.value}`
    if (trimmedNote) message += `\n\nUser note: ${trimmedNote}`
    // Keep within the backend's 2000-char message limit.
    message = message.slice(0, 2000)

    const res = await fetch(`${API}/api/feedback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        category: 'detection',
        message,
        pageUrl: '/detect',
        // Only request an upload URL when we actually have an image to send.
        imageExt: props.imageBlob ? 'jpg' : undefined,
      }),
    })

    if (res.status === 401) {
      router.push({ name: 'login' })
      return
    }
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body.error || 'Failed to submit report')
    }

    const { uploadUrl } = await res.json()

    // Upload the photo to S3 via the presigned PUT URL (same as saveSighting).
    if (uploadUrl && props.imageBlob) {
      const uploadRes = await fetch(uploadUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'image/jpeg' },
        body: props.imageBlob,
      })
      if (!uploadRes.ok) throw new Error('Failed to upload image')
    }

    success.value = true
    setTimeout(() => emit('close'), 2000)
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-[100] flex items-center justify-center">
      <div class="fixed inset-0 bg-black/40" @click="emit('close')" aria-hidden="true" />
      <div
        ref="dialogRef"
        role="dialog"
        aria-modal="true"
        aria-labelledby="report-match-title"
        class="relative bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6 max-h-[90vh] overflow-y-auto"
      >
        <!-- Success State -->
        <div v-if="success" class="text-center py-4">
          <div class="text-3xl mb-2 text-green-600">&#10003;</div>
          <p class="text-lg font-medium text-gray-900">Thanks for the report!</p>
          <p class="text-sm text-gray-500 mt-1">We'll use it to improve detection.</p>
        </div>

        <!-- Form -->
        <template v-else>
          <h3 id="report-match-title" class="text-lg font-semibold text-gray-900 mb-1">
            Report this bird
          </h3>
          <p class="text-sm text-gray-500 mb-4">
            Not the right match, or not in the dex? Tell us and we'll take a look.
          </p>

          <!-- User's photo -->
          <div v-if="previewUrl" class="mb-4">
            <img
              :src="previewUrl"
              alt="Your uploaded bird photo"
              class="w-full h-40 rounded-lg object-cover bg-gray-100"
            />
          </div>

          <!-- Auto-captured details -->
          <label class="block text-sm font-medium text-gray-700 mb-1">Detection details</label>
          <pre
            class="p-3 mb-4 bg-gray-50 rounded-lg text-xs text-gray-700 whitespace-pre-wrap break-words font-mono"
          >{{ details || 'No prediction data.' }}</pre>

          <!-- Optional note -->
          <label class="block text-sm font-medium text-gray-700 mb-1">
            What bird do you think this is? (optional)
          </label>
          <textarea
            v-model="note"
            rows="3"
            maxlength="1000"
            placeholder="e.g. I think this is a Rainbow Lorikeet — not in the list."
            class="w-full px-3 py-2 mb-1 border border-gray-300 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
          />
          <p class="text-xs text-gray-400 mb-4 text-right">{{ note.length }} / 1000</p>

          <div
            v-if="error"
            class="mb-4 p-2 bg-red-50 border border-red-200 text-red-700 rounded text-sm"
          >
            {{ error }}
          </div>

          <div class="flex justify-end gap-3">
            <button
              @click="emit('close')"
              type="button"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              @click="submit"
              :disabled="submitting"
              type="button"
              class="px-4 py-2 text-sm font-medium text-white bg-primary-green rounded-lg hover:bg-primary-green/90 transition-colors disabled:opacity-50 cursor-pointer"
            >
              {{ submitting ? 'Sending...' : 'Submit report' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>
