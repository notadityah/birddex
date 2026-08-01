import { ref, watch } from 'vue'

const STORAGE_KEY = 'onboarding.seen.v1'

// Bump the key suffix to deliberately re-show the welcome to everyone
// after a meaningful content change.
function readSeen() {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true'
  } catch {
    return false
  }
}

// Module-level state so the sidebar Help button and the modal host in
// App.vue share one source of truth.
const seen = ref(readSeen())
const open = ref(false)

watch(seen, (value) => {
  try {
    localStorage.setItem(STORAGE_KEY, String(value))
  } catch {
    // Safari private mode throws on write — the modal just re-shows next visit.
  }
})

export function useOnboarding() {
  return {
    open,
    openOnboarding() {
      open.value = true
    },
    // Every close path routes through here so dismissing always records it.
    dismiss() {
      open.value = false
      seen.value = true
    },
    maybeAutoOpen() {
      if (!seen.value) open.value = true
    },
  }
}
