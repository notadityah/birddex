<script setup>
import { ref } from 'vue'
import { useScrollAnimation } from '@/composables/useAnimation'

const headingRef = ref(null)
const stepsRef = ref(null)

const steps = [
  {
    number: '01',
    iconSrc: '/camera-svgrepo-com.svg',
    iconAlt: 'Camera',
    title: 'Snap a Photo',
    description:
      'Take a picture of any bird you spot — in your backyard, at the park, or anywhere outdoors.',
  },
  {
    number: '02',
    iconSrc: '/bulb-on-svgrepo-com.svg',
    iconAlt: 'Identify',
    title: 'Get Instant ID',
    description:
      'Our bird detection model analyses your photo and identifies the bird species in seconds.',
  },
  {
    number: '03',
    iconSrc: '/collection-data-database-document-svgrepo-com.svg',
    iconAlt: 'Collection',
    title: 'Build Your Collection',
    description:
      'Every identified bird gets added to your personal collection. Track your progress and share sightings.',
  },
]

useScrollAnimation(headingRef, { animateChildren: true, y: 30, start: 'top 75%' })
useScrollAnimation(stepsRef, { animateChildren: true, y: 50, stagger: 0.15 })
</script>

<template>
  <section id="how-it-works" class="py-24 relative overflow-hidden bg-white/90 backdrop-blur-none">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div ref="headingRef" class="text-center mb-16">
        <span class="block text-forest-green font-bold tracking-wide uppercase text-sm mb-2"
          >How It Works</span
        >
        <h2 class="text-3xl md:text-4xl font-bold text-gray-900 mb-4">Three simple steps</h2>
        <p class="text-lg text-gray-600 max-w-2xl mx-auto">
          Getting started with BirdDex is easy. No expertise required.
        </p>
      </div>

      <div class="relative">
        <!-- Connecting line (desktop only) -->
        <div
          class="hidden md:block absolute top-16 left-[20%] right-[20%] h-0.5 bg-gradient-to-r from-forest-green/20 via-forest-green/40 to-forest-green/20 z-0"
        ></div>

        <div ref="stepsRef" class="grid md:grid-cols-3 gap-8 relative z-10">
          <div v-for="(step, index) in steps" :key="index" class="relative text-center">
            <!-- Step number circle -->
            <div
              class="w-14 h-14 bg-forest-green/10 rounded-full flex items-center justify-center mx-auto mb-6 relative z-10 border-4 border-forest-green"
            >
              <img :src="step.iconSrc" :alt="step.iconAlt" width="28" height="28" />
            </div>

            <!-- Step number badge -->
            <span
              class="inline-block text-xs font-bold text-forest-green bg-forest-green/10 px-3 py-1 rounded-full mb-3"
            >
              Step {{ step.number }}
            </span>

            <h3 class="text-xl font-bold text-gray-900 mb-3">{{ step.title }}</h3>
            <p class="text-gray-600 leading-relaxed max-w-xs mx-auto">{{ step.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
