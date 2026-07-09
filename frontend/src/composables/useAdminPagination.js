import { ref, watch } from 'vue'

const STORAGE_KEY = 'admin.pageSize'
const DEFAULT_SIZE = 10

// Shared pagination state for the admin tabs. pageSize is persisted to
// localStorage (one setting shared across all tabs); page resets to 0
// whenever the size changes. Call the returned `reload` yourself after
// changing filters — the composable only reacts to page/pageSize.
export function useAdminPagination(load) {
  const stored = Number(localStorage.getItem(STORAGE_KEY))
  const pageSize = ref(Number.isInteger(stored) && stored > 0 ? stored : DEFAULT_SIZE)
  const page = ref(0)

  watch(pageSize, (size) => {
    localStorage.setItem(STORAGE_KEY, String(size))
    if (page.value === 0) load()
    else page.value = 0 // triggers the page watcher below
  })

  watch(page, () => load())

  function reload() {
    if (page.value === 0) load()
    else page.value = 0
  }

  return { page, pageSize, reload }
}
