<script setup>
import { ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'

const API = import.meta.env.VITE_API_URL
const route = useRoute()

const open = ref(false)
const category = ref('bug')
const message = ref('')
const submitting = ref(false)
const success = ref(false)
const error = ref(null)

const dialogRef = ref(null)

const categories = [
  { value: 'bug', label: 'Bug Report' },
  { value: 'detection', label: 'Detection Issue' },
  { value: 'suggestion', label: 'Suggestion' },
  { value: 'other', label: 'Other' },
]

function openModal() {
  open.value = true
  error.value = null
  success.value = false
  nextTick(() => {
    document.body.style.overflow = 'hidden'
    dialogRef.value?.querySelector('select')?.focus()
  })
}

function closeModal() {
  open.value = false
  document.body.style.overflow = ''
}

function reset() {
  category.value = 'bug'
  message.value = ''
  error.value = null
  success.value = false
}

async function submit() {
  const trimmed = message.value.trim()
  if (!trimmed) {
    error.value = 'Please enter a message.'
    return
  }
  if (trimmed.length > 2000) {
    error.value = 'Message must be 2000 characters or less.'
    return
  }

  submitting.value = true
  error.value = null
  try {
    const res = await fetch(`${API}/api/feedback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        category: category.value,
        message: trimmed,
        pageUrl: route.path,
      }),
    })
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body.error || 'Failed to submit feedback')
    }
    success.value = true
    setTimeout(() => {
      closeModal()
      reset()
    }, 2000)
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'Escape') closeModal()
}
</script>

<template>
  <!-- Floating Button -->
  <button
    @click="openModal"
    aria-label="Send feedback"
    class="fixed bottom-6 right-6 z-30 w-12 h-12 rounded-full bg-primary-green text-white shadow-lg hover:bg-primary-green/90 transition-colors flex items-center justify-center cursor-pointer"
  >
    <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
    </svg>
  </button>

  <!-- Modal -->
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-[100] flex items-center justify-center" @keydown="onKeydown">
      <div class="fixed inset-0 bg-black/40" @click="closeModal" aria-hidden="true" />
      <div ref="dialogRef" role="dialog" aria-modal="true" aria-labelledby="feedback-title" class="relative bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
        <!-- Success State -->
        <div v-if="success" class="text-center py-4">
          <div class="text-3xl mb-2">&#10003;</div>
          <p class="text-lg font-medium text-gray-900">Thanks for your feedback!</p>
          <p class="text-sm text-gray-500 mt-1">We'll review it shortly.</p>
        </div>

        <!-- Form -->
        <template v-else>
          <h3 id="feedback-title" class="text-lg font-semibold text-gray-900 mb-4">Send Feedback</h3>

          <label class="block text-sm font-medium text-gray-700 mb-1">Category</label>
          <select
            v-model="category"
            class="w-full px-3 py-2 mb-4 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
          >
            <option v-for="cat in categories" :key="cat.value" :value="cat.value">{{ cat.label }}</option>
          </select>

          <label class="block text-sm font-medium text-gray-700 mb-1">Message</label>
          <textarea
            v-model="message"
            rows="4"
            maxlength="2000"
            placeholder="Describe the issue or suggestion..."
            class="w-full px-3 py-2 mb-1 border border-gray-300 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
          />
          <p class="text-xs text-gray-400 mb-4 text-right">{{ message.length }} / 2000</p>

          <div v-if="error" class="mb-4 p-2 bg-red-50 border border-red-200 text-red-700 rounded text-sm">{{ error }}</div>

          <div class="flex justify-end gap-3">
            <button
              @click="closeModal(); reset()"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              @click="submit"
              :disabled="submitting"
              class="px-4 py-2 text-sm font-medium text-white bg-primary-green rounded-lg hover:bg-primary-green/90 transition-colors disabled:opacity-50 cursor-pointer"
            >
              {{ submitting ? 'Sending...' : 'Submit' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>
