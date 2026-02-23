<script setup>
import { useAuthStore } from '@/stores/auth'
import { useBirdStore } from '@/stores/birds'
import BirdCard from '@/components/dashboard/BirdCard.vue'

const authStore = useAuthStore()
const birdStore = useBirdStore()

const filters = [
  { label: 'All', value: 'all' },
  { label: 'Found', value: 'found' },
  { label: 'Not Found', value: 'not-found' },
]
</script>

<template>
  <div class="flex-1 p-6 sm:p-8 md:pl-8">
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">MyDex</h1>
      <p class="text-sm text-gray-500 mt-1">
        Welcome back, {{ authStore.displayName }}.
        You've found
        <span class="font-semibold text-primary-green">{{ birdStore.foundCount }}</span>
        of
        <span class="font-semibold">{{ birdStore.totalCount }}</span>
        birds.
      </p>
    </div>

    <!-- Filters -->
    <div class="flex gap-2 mb-6">
      <button
        v-for="f in filters"
        :key="f.value"
        @click="birdStore.setFilter(f.value)"
        class="px-4 py-1.5 rounded-full text-sm font-medium transition-colors cursor-pointer"
        :class="
          birdStore.filter === f.value
            ? 'bg-primary-green text-white'
            : 'bg-white text-gray-600 border border-gray-200 hover:border-gray-300'
        "
      >
        {{ f.label }}
      </button>
    </div>

    <!-- Bird grid -->
    <div
      v-if="birdStore.filteredBirds.length"
      class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4"
    >
      <BirdCard v-for="bird in birdStore.filteredBirds" :key="bird.id" :bird="bird" />
    </div>

    <!-- Empty state -->
    <div v-else class="text-center py-16 text-gray-400">
      <p class="text-lg font-medium">No birds to show</p>
      <p class="text-sm mt-1">Try changing the filter above.</p>
    </div>
  </div>
</template>
