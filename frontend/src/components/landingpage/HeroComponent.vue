<script setup>
import { ref } from 'vue'
import { useLoadAnimation } from '@/composables/useAnimation'

const heroLeft = ref(null)
const heroRight = ref(null)

useLoadAnimation(heroLeft, { animateChildren: true })
useLoadAnimation(heroRight, { x: 60, y: 0, duration: 1 })
</script>

<template>
  <section
    class="relative min-h-[calc(100vh-4rem)] flex items-center pt-16 pb-24 overflow-hidden bg-white/90 backdrop-blur-none"
  >
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="lg:grid lg:grid-cols-2 lg:gap-16 items-center">
        <!-- Left Column -->
        <div ref="heroLeft" class="mb-12 lg:mb-0">
          <h1
            class="text-4xl md:text-5xl lg:text-6xl font-bold tracking-tight text-gray-900 mb-6 leading-tight"
          >
            Keep track of the birds you spot with <span class="text-forest-green">BirdDex</span>
          </h1>
        </div>

        <!-- Right Column -->
        <div ref="heroRight" class="relative">
          <div
            class="relative rounded-2xl overflow-hidden shadow-2xl bg-gray-100 aspect-4/3 w-full group"
          >
            <!-- Hero Image -->
            <img
              src="/images/kingfisher_2.jpg"
              alt="Common Kingfisher"
              class="object-cover w-full h-full"
            />

            <!-- Identification Card Overlay -->
            <div
              class="absolute bottom-6 left-6 right-6 bg-white/90 backdrop-blur-sm rounded-xl p-4 shadow-lg flex items-center gap-4 border border-white/50"
            >
              <div
                class="w-10 h-10 bg-forest-green/10 rounded-full flex items-center justify-center shrink-0"
              >
                <img src="/check-mark-svgrepo-com.svg" alt="Check Mark" class="w-4 h-4" />
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="font-bold text-gray-900 text-sm">Kingfisher identified!</h3>
              </div>
              <div
                class="bg-forest-green text-white text-xs font-bold px-2.5 py-1 rounded-full shrink-0"
              >
                Match
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
