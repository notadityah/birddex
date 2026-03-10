import { onMounted, onUnmounted } from 'vue'

export function useModalLifecycle(onClose) {
  function onKeydown(e) {
    if (e.key === 'Escape') onClose()
  }

  onMounted(() => {
    document.addEventListener('keydown', onKeydown)
    document.body.style.overflow = 'hidden'
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', onKeydown)
    document.body.style.overflow = ''
  })
}
