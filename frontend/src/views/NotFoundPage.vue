<script setup>
import Footer from '@/components/landingpage/FooterComponent.vue'
</script>

<template>
  <main class="grow flex items-center justify-center p-8">
    <div class="text-center">
      <h1 class="text-6xl font-bold text-gray-300 mb-4">404</h1>
      <p class="text-xl font-semibold text-gray-900 mb-2">Page not found</p>
      <p class="text-gray-500 mb-6">The page you're looking for doesn't exist.</p>
      <router-link
        to="/"
        class="inline-block bg-primary-green text-white px-6 py-2.5 rounded-lg font-semibold hover:bg-opacity-90 transition-opacity"
      >
        Back to home
      </router-link>
    </div>
  </main>
  <Footer />
</template>
