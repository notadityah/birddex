import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { BIRDS } from '@/data/birds'

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

  function markFound(slug) {
    const bird = birds.value.find((b) => b.slug === slug)
    if (bird) bird.found = true
  }

  return { birds, filter, filteredBirds, foundCount, totalCount, setFilter, markFound }
})
