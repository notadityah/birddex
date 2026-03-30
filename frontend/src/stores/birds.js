import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const API = import.meta.env.VITE_API_URL

export const useBirdStore = defineStore('birds', () => {
  const birds = ref([])
  const filter = ref('all')
  const loading = ref(false)
  const error = ref(null)
  // AbortController: cancels in-flight sighting fetches when loadSightings() is called
  // again (e.g., navigating away and back) or on logout (resetFound). Prevents stale
  // responses from overwriting newer data.
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

  async function loadBirds() {
    if (birds.value.length > 0) return
    try {
      const res = await fetch(`${API}/api/birds`)
      if (!res.ok) {
        error.value = 'Failed to load birds'
        return
      }
      const rows = await res.json()
      birds.value = rows.map((b) => ({
        id: b.id,
        name: b.name,
        scientificName: b.scientific_name,
        slug: b.slug,
        found: false,
        sightings: [],
        imageUrl: null,
      }))
    } catch {
      error.value = 'Network error loading birds'
    }
  }

  async function loadSightings() {
    // Cancel any in-flight request
    if (loadController) loadController.abort()
    loadController = new AbortController()

    const isInitialLoad = birds.value.length === 0
    if (isInitialLoad) loading.value = true
    error.value = null
    try {
      // Ensure birds are loaded first
      await loadBirds()

      const res = await fetch(`${API}/api/sightings`, {
        credentials: 'include',
        signal: loadController.signal,
      })
      if (!res.ok) {
        error.value = 'Failed to load sightings'
        return
      }
      const sightings = await res.json()

      // Group sightings by bird slug so we can merge them into the birds array.
      // This avoids O(n*m) lookups when matching sightings to birds.
      const sightingsBySlug = {}
      sightings.forEach((s) => {
        if (!sightingsBySlug[s.slug]) sightingsBySlug[s.slug] = []
        sightingsBySlug[s.slug].push({
          id: s.id,
          imageUrl: s.image_url || null,
          detectedAt: s.detected_at,
          notes: s.notes,
          createdAt: s.created_at,
          public: s.public,
        })
      })

      // Apply to birds
      birds.value.forEach((b) => {
        const birdSightings = sightingsBySlug[b.slug] || []
        b.sightings = birdSightings
        b.found = birdSightings.length > 0
        b.imageUrl = birdSightings.length > 0 ? birdSightings[0].imageUrl : null
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
    birds.value.forEach((b) => {
      b.found = false
      b.sightings = []
      b.imageUrl = null
    })
    error.value = null
  }

  // Optimistic update: flip the public flag in local state immediately for instant UI
  // feedback. If the API call fails, rollback to the previous value.
  async function toggleSightingPublic(sightingId, isPublic) {
    let found = null
    for (const bird of birds.value) {
      const s = bird.sightings.find((s) => s.id === sightingId)
      if (s) {
        found = s
        s.public = isPublic
        break
      }
    }
    if (!found) return

    try {
      const res = await fetch(`${API}/api/sightings/${sightingId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ public: isPublic }),
      })
      if (!res.ok) throw new Error('Failed to toggle public')
    } catch {
      // Rollback
      found.public = !isPublic
    }
  }

  async function deleteAllSightings() {
    const res = await fetch(`${API}/api/sightings`, {
      method: 'DELETE',
      credentials: 'include',
    })
    if (!res.ok) throw new Error('Failed to delete sightings')
    resetFound()
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
    loadBirds,
    loadSightings,
    resetFound,
    toggleSightingPublic,
    deleteAllSightings,
  }
})
