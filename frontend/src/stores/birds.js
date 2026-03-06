import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { BIRDS } from '@/data/birds'

const API = import.meta.env.VITE_API_URL

export const useBirdStore = defineStore('birds', () => {
  const birds = ref(BIRDS.map((b) => ({ ...b, found: false })))
  const filter = ref('all')

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
    const res = await fetch(`${API}/api/sightings`, { credentials: 'include' })
    if (!res.ok) return
    const sightings = await res.json()
    sightings.forEach((s) => {
      const bird = birds.value.find((b) => b.slug === s.slug)
      if (bird) bird.found = true
    })
  }

  function resetFound() {
    birds.value.forEach((b) => (b.found = false))
  }

  async function markFound(slug) {
    const bird = birds.value.find((b) => b.slug === slug)
    if (!bird || bird.found) return
    bird.found = true // optimistic update
    await fetch(`${API}/api/sightings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ birdId: bird.id }),
    })
  }

  return {
    birds,
    filter,
    filteredBirds,
    foundCount,
    totalCount,
    setFilter,
    markFound,
    loadSightings,
    resetFound,
  }
})
