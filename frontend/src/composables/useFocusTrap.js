import { onMounted, onUnmounted } from 'vue'

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

export function useFocusTrap(containerRef) {
  let previouslyFocused = null

  function handleKeydown(e) {
    if (e.key !== 'Tab' || !containerRef.value) return

    const focusable = [...containerRef.value.querySelectorAll(FOCUSABLE)]
    if (!focusable.length) return

    const first = focusable[0]
    const last = focusable[focusable.length - 1]

    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault()
        last.focus()
      }
    } else {
      if (document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }
  }

  onMounted(() => {
    previouslyFocused = document.activeElement
    document.addEventListener('keydown', handleKeydown)

    // Auto-focus first focusable element
    if (containerRef.value) {
      const first = containerRef.value.querySelector(FOCUSABLE)
      if (first) first.focus()
    }
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
    if (previouslyFocused && typeof previouslyFocused.focus === 'function') {
      previouslyFocused.focus()
    }
  })
}
