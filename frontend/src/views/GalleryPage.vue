<script setup>
import { ref, onMounted } from 'vue'
import { useGalleryStore } from '@/stores/gallery'
import { useBirdStore } from '@/stores/birds'
import GallerySightingCard from '@/components/gallery/GallerySightingCard.vue'
import GallerySightingModal from '@/components/gallery/GallerySightingModal.vue'
import SpinnerIcon from '@/components/SpinnerIcon.vue'

const galleryStore = useGalleryStore()
const birdStore = useBirdStore()
const selectedSighting = ref(null)

onMounted(async () => {
  await birdStore.loadBirds()
  galleryStore.loadGallery(true)
})

function onFilterChange(e) {
  galleryStore.setBirdFilter(e.target.value)
}
</script>

<template>
  <div class="p-6 pt-16 sm:p-8 sm:pt-16 md:pt-8 md:pl-8">
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Community Gallery</h1>
      <p class="text-sm text-gray-500 mt-1">Bird sightings shared by the community.</p>
    </div>

    <!-- Filter -->
    <div class="mb-6">
      <label for="bird-filter" class="sr-only">Filter by bird</label>
      <select
        id="bird-filter"
        :value="galleryStore.birdFilter || ''"
        @change="onFilterChange"
        class="block w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-green focus:border-transparent bg-white"
      >
        <option value="">All birds</option>
        <option v-for="bird in birdStore.birds" :key="bird.id" :value="bird.id">
          {{ bird.name }}
        </option>
      </select>
    </div>

    <!-- Error -->
    <div
      v-if="galleryStore.error"
      role="alert"
      class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg flex items-center justify-between"
    >
      <p class="text-sm text-red-700">{{ galleryStore.error }}</p>
      <button
        @click="galleryStore.loadGallery(true)"
        class="text-sm font-medium text-red-700 hover:text-red-900 underline cursor-pointer"
      >
        Retry
      </button>
    </div>

    <!-- Loading (initial) -->
    <div v-if="galleryStore.loading && galleryStore.sightings.length === 0" class="flex justify-center py-12">
      <SpinnerIcon />
    </div>

    <!-- Empty state -->
    <div
      v-else-if="!galleryStore.loading && galleryStore.sightings.length === 0 && !galleryStore.error"
      class="text-center py-12"
    >
      <p class="text-gray-400">No sightings shared yet. Be the first!</p>
    </div>

    <!-- Grid -->
    <div v-else>
      <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
        <GallerySightingCard
          v-for="sighting in galleryStore.sightings"
          :key="sighting.id"
          :sighting="sighting"
          @open-detail="selectedSighting = $event"
        />
      </div>

      <!-- Load more -->
      <div v-if="galleryStore.hasMore" class="mt-6 text-center">
        <button
          @click="galleryStore.loadMore()"
          :disabled="galleryStore.loading"
          type="button"
          class="px-6 py-2 bg-white text-gray-700 text-sm font-medium rounded-lg border border-gray-300 hover:border-gray-400 transition-colors disabled:opacity-50 cursor-pointer"
        >
          <span v-if="galleryStore.loading" class="flex items-center gap-2">
            <SpinnerIcon class="w-4 h-4" /> Loading...
          </span>
          <span v-else>Load More</span>
        </button>
      </div>
    </div>

    <!-- Detail modal -->
    <GallerySightingModal
      v-if="selectedSighting"
      :sighting="selectedSighting"
      @close="selectedSighting = null"
    />
  </div>
</template>
