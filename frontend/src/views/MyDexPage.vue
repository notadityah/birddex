<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useBirdStore } from '@/stores/birds'
import BirdCard from '@/components/dashboard/BirdCard.vue'
import BirdDetailModal from '@/components/dashboard/BirdDetailModal.vue'
import SpinnerIcon from '@/components/SpinnerIcon.vue'

const authStore = useAuthStore()
const birdStore = useBirdStore()
const selectedBird = ref(null)

const filters = [
  { label: 'All', value: 'all' },
  { label: 'Found', value: 'found' },
  { label: 'Not Found', value: 'not-found' },
]
</script>

<template>
  <div class="flex-1 p-6 pt-16 sm:p-8 sm:pt-16 md:pt-8 md:pl-8">
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

    <!-- Error banner -->
    <div
      v-if="birdStore.error"
      role="alert"
      class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between"
    >
      <p class="text-sm text-red-700">{{ birdStore.error }}</p>
      <button
        @click="birdStore.loadSightings()"
        class="text-sm font-medium text-red-700 hover:text-red-900 underline cursor-pointer"
      >
        Retry
      </button>
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

    <!-- Loading state -->
    <div v-if="birdStore.loading" class="flex justify-center py-16">
      <SpinnerIcon />
    </div>

    <!-- Bird grid -->
    <div
      v-else-if="birdStore.filteredBirds.length"
      class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4"
    >
      <BirdCard
        v-for="bird in birdStore.filteredBirds"
        :key="bird.id"
        :bird="bird"
        @open-detail="selectedBird = $event"
      />
    </div>

    <!-- Empty state -->
    <div v-else class="text-center py-16 text-gray-500">
      <p class="text-lg font-medium">No birds to show</p>
      <p class="text-sm mt-1">Try changing the filter above.</p>
    </div>

    <!-- Bird detail modal -->
    <BirdDetailModal
      v-if="selectedBird"
      :bird="selectedBird"
      @close="selectedBird = null"
    />
  </div>
</template>
