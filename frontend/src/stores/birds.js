import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { BIRDS } from '@/data/birds'

const API = import.meta.env.VITE_API_URL

export const useBirdStore = defineStore('birds', () => {
  const birds = ref(BIRDS.map((b) => ({ ...b, found: false })))
  const filter = ref('all')
  const loading = ref(false)
  const error = ref(null)
  let loadController = null

  const filteredBirds = computed(() => {
    if (filter.value === 'found') return birds.value.filter((b) => b.found)
    if (filter.value === 'not-found') return birds.value.filter((b) => !b.found)
    return birds.value
  })

  const foundCount = computed(() => birds.value.filter((b) => b.found).length)
  const totalCount = computed(() => birds.value.length)

  function setFilter(val) {
    filter.value = val
  }

  async function loadSightings() {
    // Cancel any in-flight request
    if (loadController) loadController.abort()
    loadController = new AbortController()

    loading.value = true
    error.value = null
    try {
      const res = await fetch(`${API}/api/sightings`, {
        credentials: 'include',
        signal: loadController.signal,
      })
      if (!res.ok) {
        error.value = 'Failed to load sightings'
        return
      }
      const sightings = await res.json()
      sightings.forEach((s) => {
        const bird = birds.value.find((b) => b.slug === s.slug)
        if (bird) bird.found = true
      })
    } catch (err) {
      if (err.name !== 'AbortError') {
        error.value = 'Network error loading sightings'
      }
    } finally {
      loading.value = false
    }
  }

  function resetFound() {
    if (loadController) loadController.abort()
    birds.value.forEach((b) => (b.found = false))
    error.value = null
  }

  async function markFound(slug) {
    const bird = birds.value.find((b) => b.slug === slug)
    if (!bird || bird.found) return

    bird.found = true
    try {
      const res = await fetch(`${API}/api/sightings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ birdId: bird.id }),
      })
      if (!res.ok) {
        bird.found = false
        error.value = 'Failed to save sighting'
      }
    } catch {
      bird.found = false
      error.value = 'Network error saving sighting'
    }
  }

  return {
    birds,
    filter,
    loading,
    error,
    filteredBirds,
    foundCount,
    totalCount,
    setFilter,
    markFound,
    loadSightings,
    resetFound,
  }
})
