import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const API = import.meta.env.VITE_API_URL
const PAGE_SIZE = 20

export const useGalleryStore = defineStore('gallery', () => {
  const sightings = ref([])
  const total = ref(0)
  const loading = ref(false)
  const error = ref(null)
  const birdFilter = ref(null)
  const page = ref(0)
  let controller = null

  const hasMore = computed(() => sightings.value.length < total.value)

  async function loadGallery(reset = false) {
    if (controller) controller.abort()
    controller = new AbortController()

    if (reset) {
      sightings.value = []
      page.value = 0
    }

    loading.value = true
    error.value = null

    try {
      const params = new URLSearchParams({
        limit: String(PAGE_SIZE),
        offset: String(page.value * PAGE_SIZE),
      })
      if (birdFilter.value) params.set('birdId', String(birdFilter.value))

      const res = await fetch(`${API}/api/gallery?${params}`, {
        credentials: 'include',
        signal: controller.signal,
      })
      if (!res.ok) throw new Error('Failed to load gallery')

      const data = await res.json()
      if (reset || page.value === 0) {
        sightings.value = data.sightings
      } else {
        sightings.value = [...sightings.value, ...data.sightings]
      }
      total.value = data.total
    } catch (err) {
      if (err.name !== 'AbortError') {
        error.value = 'Failed to load gallery'
      }
    } finally {
      loading.value = false
    }
  }

  function loadMore() {
    page.value++
    loadGallery()
  }

  function setBirdFilter(birdId) {
    birdFilter.value = birdId || null
    loadGallery(true)
  }

  function reset() {
    if (controller) controller.abort()
    sightings.value = []
    total.value = 0
    loading.value = false
    error.value = null
    birdFilter.value = null
    page.value = 0
  }

  return {
    sightings,
    total,
    loading,
    error,
    birdFilter,
    hasMore,
    loadGallery,
    loadMore,
    setBirdFilter,
    reset,
  }
})
