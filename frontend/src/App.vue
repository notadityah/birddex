<script setup>
import Header from '@/components/landingpage/HeaderComponent.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <Header />
    <router-view />
  </div>
</template>
