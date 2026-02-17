<script setup>
import { ref } from 'vue'
import { useScrollAnimation } from '@/composables/useAnimation'

const headingRef = ref(null)
const gridRef = ref(null)

const photos = [
  { src: '/images/crow.jpg', name: 'Crow' },
  { src: '/images/owl.jpg', name: 'Owl' },
  { src: '/images/kingfisher_1.jpg', name: 'Kingfisher' },
  { src: '/images/myna.jpg', name: 'Myna' },
  { src: '/images/peacock.jpg', name: 'Peacock' },
  { src: '/images/pigeon.jpg', name: 'Pigeon' },
  { src: '/images/sparrow.jpg', name: 'Sparrow' },
]

useScrollAnimation(headingRef, { animateChildren: true, y: 30, start: 'top 75%' })
useScrollAnimation(gridRef, {
  animateChildren: true,
  scale: 0.9,
  y: 0,
  stagger: 0.08,
  duration: 0.6,
  ease: 'power2.out',
})
</script>

<template>
  <section id="community" class="py-24 relative overflow-hidden bg-white/90 backdrop-blur-none">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div ref="headingRef" class="text-center mb-16">
        <h2 class="text-3xl md:text-4xl font-bold text-gray-900 mb-4">Community Gallery</h2>
        <p class="text-lg text-gray-600 max-w-2xl mx-auto">
          Stunning shots captured by the BirdDex community. Join today to get featured!
        </p>
      </div>

      <div
        ref="gridRef"
        class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 auto-rows-[200px]"
      >
        <!-- Large item (index 0) -->
        <div class="col-span-2 row-span-2 relative rounded-2xl overflow-hidden group shadow-md">
          <img :src="photos[0].src" :alt="photos[0].name" class="object-cover w-full h-full" />
          <div
            class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60 group-hover:opacity-40 transition-opacity"
          ></div>
          <div
            class="absolute bottom-4 left-4 text-white font-bold text-xl drop-shadow-md translate-y-2 group-hover:translate-y-0 transition-transform duration-300"
          >
            {{ photos[0].name }}
          </div>
        </div>

        <!-- Regular items (others) -->
        <div
          v-for="(photo, i) in photos.slice(1)"
          :key="i"
          class="relative rounded-2xl overflow-hidden group shadow-md"
          :class="i === 2 || i === 5 ? 'col-span-2 md:col-span-1' : ''"
        >
          <img :src="photo.src" :alt="photo.name" class="object-cover w-full h-full" />
          <div
            class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60 group-hover:opacity-40 transition-opacity"
          ></div>
          <div
            class="absolute bottom-4 left-4 text-white font-bold text-lg drop-shadow-md translate-y-2 group-hover:translate-y-0 transition-transform duration-300"
          >
            {{ photo.name }}
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
