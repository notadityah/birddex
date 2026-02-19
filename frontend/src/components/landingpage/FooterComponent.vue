<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useScrollAnimation } from '@/composables/useAnimation'

const footerRef = ref(null)

useScrollAnimation(footerRef, { animateChildren: true, y: 30, start: 'top 90%' })
</script>

<template>
  <footer ref="footerRef" class="bg-forest-green pt-16 pb-8 border-t border-white/10">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 mb-16">
        <!-- Column 1: Brand -->
        <div class="lg:col-span-1">
          <div class="flex items-center gap-2 mb-6">
            <div
              class="w-8 h-8 bg-primary-green rounded-full flex items-center justify-center text-white"
            >
              <img src="/bird-svgrepo-com.svg" alt="Bird" width="20" height="20" />
            </div>
            <span class="font-bold text-xl tracking-tight text-white">BirdDex</span>
          </div>
        </div>

        <!-- Column 2: Legal -->
        <div>
          <h3 class="text-white font-semibold mb-4">Legal</h3>
          <ul class="space-y-2">
            <li>
              <RouterLink
                to="/privacy-policy"
                class="text-gray-400 hover:text-white transition-colors text-sm"
              >
                Privacy Policy
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>

      <div
        class="border-t border-white/10 pt-8 flex flex-col md:flex-row justify-between items-center gap-4"
      >
        <p class="text-gray-400 text-sm">Developed by Aditya Hariharan</p>
      </div>
    </div>
  </footer>
</template>
