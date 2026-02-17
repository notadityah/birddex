<script setup>
import { ref } from 'vue'
import { useBounceAnimation } from '@/composables/useAnimation'
import Header from '@/components/landingpage/HeaderComponent.vue'
import Hero from '@/components/landingpage/HeroComponent.vue'
import HowItWorks from '@/components/landingpage/HowItWorks.vue'
import Community from '@/components/landingpage/CommunityComponent.vue'
import CallToAction from '@/components/landingpage/CallToAction.vue'
import Footer from '@/components/landingpage/FooterComponent.vue'

const contentRef = ref(null)
const chevronRef = ref(null)

const { visible: showChevron } = useBounceAnimation(chevronRef, { containerRef: contentRef })
</script>

<template>
  <div class="min-h-screen font-sans flex flex-col">
    <Header />
    <main class="grow">
      <div ref="contentRef">
        <Hero />
        <HowItWorks />
        <Community />
      </div>
      <CallToAction />
    </main>
    <Footer />

    <!-- Global scroll-down indicator — visible until the CTA section -->
    <div
      v-if="showChevron"
      ref="chevronRef"
      class="fixed bottom-8 left-1/2 -translate-x-1/2 z-40 opacity-0 pointer-events-none"
    >
      <img src="/down-chevron-svgrepo-com.svg" alt="Scroll down" class="w-8 h-8" />
    </div>
  </div>
</template>
