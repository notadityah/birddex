<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useBirdStore } from '@/stores/birds'
import { useFocusTrap } from '@/composables/useFocusTrap'
import SpinnerIcon from '@/components/SpinnerIcon.vue'

const API = import.meta.env.VITE_API_URL
const authStore = useAuthStore()
const birdStore = useBirdStore()

// --- Profile ---
const avatarLetter = computed(() => (authStore.user?.name || authStore.user?.email || '?')[0].toUpperCase())
const memberSince = computed(() => {
  const d = authStore.user?.createdAt
  return d ? new Date(d).toLocaleDateString(undefined, { year: 'numeric', month: 'long' }) : ''
})
const gallerySharedCount = computed(() => {
  let count = 0
  for (const bird of birdStore.birds) {
    for (const s of bird.sightings) {
      if (s.public) count++
    }
  }
  return count
})

// --- Change Password ---
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordLoading = ref(false)
const passwordSuccess = ref('')
const passwordError = ref('')

async function onChangePassword() {
  passwordError.value = ''
  passwordSuccess.value = ''

  if (newPassword.value !== confirmPassword.value) {
    passwordError.value = 'Passwords do not match.'
    return
  }
  if (newPassword.value.length < 8) {
    passwordError.value = 'New password must be at least 8 characters.'
    return
  }

  passwordLoading.value = true
  const ok = await authStore.changePassword(currentPassword.value, newPassword.value)
  passwordLoading.value = false

  if (ok) {
    passwordSuccess.value = 'Password changed successfully.'
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } else {
    passwordError.value = authStore.error || 'Failed to change password. If you signed up with Google, you may not have a password set.'
    authStore.clearError()
  }
}

// --- Gallery Settings ---
const galleryAnonymous = ref(false)
const settingsLoading = ref(false)

onMounted(async () => {
  try {
    const res = await fetch(`${API}/api/account/settings`, { credentials: 'include' })
    if (res.ok) {
      const data = await res.json()
      galleryAnonymous.value = data.galleryAnonymous
    }
  } catch { /* ignore */ }
})

async function toggleAnonymous() {
  settingsLoading.value = true
  try {
    const res = await fetch(`${API}/api/account/settings`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ galleryAnonymous: !galleryAnonymous.value }),
    })
    if (res.ok) {
      galleryAnonymous.value = !galleryAnonymous.value
    }
  } catch { /* ignore */ }
  settingsLoading.value = false
}

// --- Danger Zone ---
const showResetConfirm = ref(false)
const resetting = ref(false)
const resetSuccess = ref('')

async function onResetCollection() {
  resetting.value = true
  try {
    await birdStore.deleteAllSightings()
    resetSuccess.value = 'All sightings deleted.'
    showResetConfirm.value = false
  } catch {
    resetSuccess.value = ''
  }
  resetting.value = false
}

const showDeleteModal = ref(false)
const deleteInput = ref('')
const deleting = ref(false)
const deleteError = ref('')
const deleteModalRef = ref(null)

// Determine if user has a password-based account (not OAuth-only)
const hasPassword = computed(() => {
  // If user signed up with email, they have a password
  // We can't easily determine this from session, so we'll accept both password and "DELETE" keyword
  return true
})

async function onDeleteAccount() {
  deleteError.value = ''
  if (!deleteInput.value) {
    deleteError.value = 'Please enter your password or type DELETE to confirm.'
    return
  }

  deleting.value = true
  // Try with password first; if that fails and input is "DELETE", try without
  const ok = await authStore.deleteAccount(deleteInput.value === 'DELETE' ? undefined : deleteInput.value)
  deleting.value = false

  if (!ok) {
    deleteError.value = authStore.error || 'Failed to delete account.'
    authStore.clearError()
  }
}

function openDeleteModal() {
  showDeleteModal.value = true
  deleteInput.value = ''
  deleteError.value = ''
}

function closeDeleteModal() {
  showDeleteModal.value = false
}

function onDeleteKeydown(e) {
  if (e.key === 'Escape') closeDeleteModal()
}
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 sm:px-6 py-6 pt-16 md:pt-6">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">My Account</h1>

    <!-- Profile Card -->
    <div class="bg-white border border-gray-200 rounded-xl p-5 mb-6">
      <div class="flex items-center gap-4">
        <div class="w-14 h-14 rounded-full bg-primary-green text-white flex items-center justify-center text-xl font-bold shrink-0">
          {{ avatarLetter }}
        </div>
        <div class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 truncate">{{ authStore.user?.name }}</h2>
          <p class="text-sm text-gray-500 truncate">{{ authStore.user?.email }}</p>
          <div class="flex items-center gap-1 mt-0.5">
            <span v-if="authStore.user?.emailVerified" class="text-xs text-green-600 flex items-center gap-0.5">
              <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
              Verified
            </span>
            <span v-else class="text-xs text-amber-600">Unverified</span>
          </div>
        </div>
      </div>
      <div class="mt-4 flex flex-wrap gap-4 text-sm text-gray-600">
        <span v-if="memberSince">Member since {{ memberSince }}</span>
        <span>{{ birdStore.foundCount }} of {{ birdStore.totalCount }} birds found</span>
        <span>{{ gallerySharedCount }} shared to gallery</span>
      </div>
    </div>

    <!-- Change Password -->
    <div class="bg-white border border-gray-200 rounded-xl p-5 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">Change Password</h2>

      <div v-if="passwordSuccess" class="mb-3 p-2 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
        {{ passwordSuccess }}
      </div>
      <div v-if="passwordError" class="mb-3 p-2 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
        {{ passwordError }}
      </div>

      <form @submit.prevent="onChangePassword" class="space-y-3">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
          <input
            v-model="currentPassword"
            type="password"
            autocomplete="current-password"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-green focus:border-transparent"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">New Password</label>
          <input
            v-model="newPassword"
            type="password"
            autocomplete="new-password"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-green focus:border-transparent"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
          <input
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-green focus:border-transparent"
          />
        </div>
        <button
          type="submit"
          :disabled="passwordLoading"
          class="px-5 py-2 bg-primary-green text-white text-sm font-medium rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50 cursor-pointer"
        >
          <span v-if="passwordLoading">Changing...</span>
          <span v-else>Change Password</span>
        </button>
      </form>
    </div>

    <!-- Gallery Settings -->
    <div class="bg-white border border-gray-200 rounded-xl p-5 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">Gallery Settings</h2>
      <label class="flex items-center gap-3">
        <button
          type="button"
          role="switch"
          :aria-checked="galleryAnonymous"
          :disabled="settingsLoading"
          @click="toggleAnonymous"
          class="relative inline-flex h-6 w-11 shrink-0 rounded-full border-2 border-transparent transition-colors cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary-green focus:ring-offset-2"
          :class="galleryAnonymous ? 'bg-primary-green' : 'bg-gray-200'"
        >
          <span
            class="pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transform transition-transform"
            :class="galleryAnonymous ? 'translate-x-5' : 'translate-x-0'"
          />
        </button>
        <span class="text-sm text-gray-700">Appear as Anonymous in gallery</span>
      </label>
      <p class="text-xs text-gray-400 mt-2">When enabled, your name won't be shown on shared sightings.</p>
    </div>

    <!-- Danger Zone -->
    <div class="border border-red-200 rounded-xl p-5">
      <h2 class="text-lg font-semibold text-red-700 mb-4">Danger Zone</h2>

      <div v-if="resetSuccess" class="mb-3 p-2 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
        {{ resetSuccess }}
      </div>

      <div class="space-y-4">
        <!-- Reset Collection -->
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-900">Reset Collection</p>
            <p class="text-xs text-gray-500">Delete all your sightings and images.</p>
          </div>
          <button
            v-if="!showResetConfirm"
            @click="showResetConfirm = true"
            type="button"
            class="px-4 py-1.5 text-sm font-medium text-red-700 border border-red-300 rounded-lg hover:bg-red-50 transition-colors cursor-pointer"
          >
            Reset
          </button>
          <div v-else class="flex gap-2">
            <button
              @click="onResetCollection"
              :disabled="resetting"
              type="button"
              class="px-4 py-1.5 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 cursor-pointer"
            >
              {{ resetting ? 'Deleting...' : 'Confirm Delete' }}
            </button>
            <button
              @click="showResetConfirm = false"
              type="button"
              class="px-4 py-1.5 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
            >
              Cancel
            </button>
          </div>
        </div>

        <!-- Delete Account -->
        <div class="flex items-center justify-between border-t border-red-100 pt-4">
          <div>
            <p class="text-sm font-medium text-gray-900">Delete Account</p>
            <p class="text-xs text-gray-500">Permanently remove your account and all data.</p>
          </div>
          <button
            @click="openDeleteModal"
            type="button"
            class="px-4 py-1.5 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors cursor-pointer"
          >
            Delete Account
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Account Modal -->
    <div
      v-if="showDeleteModal"
      class="fixed inset-0 z-50 flex items-center justify-center py-4 bg-black/50"
      @click.self="closeDeleteModal"
      @keydown="onDeleteKeydown"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-account-title"
    >
      <div ref="deleteModalRef" class="bg-white rounded-2xl shadow-xl w-full max-w-sm mx-4 p-6">
        <h3 id="delete-account-title" class="text-lg font-bold text-red-700 mb-2">Delete Account</h3>
        <p class="text-sm text-gray-600 mb-4">
          This action is permanent. Enter your password to confirm, or type <span class="font-mono font-bold">DELETE</span> if you use Google sign-in.
        </p>

        <div v-if="deleteError" class="mb-3 p-2 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          {{ deleteError }}
        </div>

        <input
          v-model="deleteInput"
          :type="deleteInput === 'DELETE' ? 'text' : 'password'"
          placeholder="Password or DELETE"
          autocomplete="off"
          class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent mb-4"
        />

        <div class="flex justify-end gap-2">
          <button
            @click="closeDeleteModal"
            type="button"
            class="px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
          >
            Cancel
          </button>
          <button
            @click="onDeleteAccount"
            :disabled="deleting || !deleteInput"
            type="button"
            class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 cursor-pointer"
          >
            {{ deleting ? 'Deleting...' : 'Delete My Account' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
