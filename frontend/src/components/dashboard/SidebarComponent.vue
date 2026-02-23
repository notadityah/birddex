<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()
const mobileOpen = ref(false)

const navItems = [
  {
    label: 'MyDex',
    to: '/mydex',
    icon: '/squares-four-bold-svgrepo-com.svg',
  },
  {
    label: 'Detect',
    to: '/detect',
    icon: '/camera2.svg',
  },
  {
    label: 'Gallery',
    to: '/gallery',
    icon: '/image-svgrepo-com.svg',
  },
  {
    label: 'My Account',
    to: '/account',
    icon: '/user-svgrepo-com.svg',
  },
]

async function handleLogout() {
  mobileOpen.value = false
  await authStore.logout()
  router.push('/login')
}

function closeMobile() {
  mobileOpen.value = false
}
</script>

<template>
  <!-- Mobile toggle button -->
  <button
    @click="mobileOpen = true"
    class="md:hidden fixed top-4 left-4 z-50 bg-forest-green text-white p-2 rounded-lg shadow-lg"
    aria-label="Open menu"
  >
    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M4 6h16M4 12h16M4 18h16"
      />
    </svg>
  </button>

  <!-- Mobile backdrop -->
  <div v-if="mobileOpen" @click="closeMobile" class="md:hidden fixed inset-0 bg-black/50 z-40" />

  <!-- Sidebar -->
  <aside
    :class="[
      'fixed md:sticky top-0 left-0 z-50 md:z-auto h-screen w-64 bg-forest-green flex flex-col transition-transform duration-300 md:translate-x-0 shrink-0',
      mobileOpen ? 'translate-x-0' : '-translate-x-full',
    ]"
  >
    <!-- Logo -->
    <div class="flex items-center gap-2 px-6 h-16 border-b border-white/10">
      <div class="w-8 h-8 bg-primary-green rounded-full flex items-center justify-center">
        <img src="/bird-svgrepo-com.svg" alt="Bird" width="20" height="20" />
      </div>
      <span class="font-bold text-xl text-white tracking-tight">BirdDex</span>

      <!-- Mobile close -->
      <button
        @click="closeMobile"
        class="md:hidden ml-auto text-white/60 hover:text-white"
        aria-label="Close menu"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M6 18L18 6M6 6l12 12"
          />
        </svg>
      </button>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-3 py-4 space-y-1">
      <router-link
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        @click="closeMobile"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors"
        :class="
          $route.path === item.to
            ? 'bg-white/15 text-white'
            : 'text-white/60 hover:bg-white/10 hover:text-white'
        "
      >
        <img :src="item.icon" :alt="item.label" class="w-5 h-5 shrink-0" />
        {{ item.label }}
      </router-link>
    </nav>

    <!-- User + Sign Out -->
    <div class="px-3 pb-4 border-t border-white/10 pt-4">
      <div class="px-3 mb-3">
        <p class="text-sm font-medium text-white truncate">{{ authStore.displayName }}</p>
        <p class="text-xs text-white/50 truncate">{{ authStore.userProfile?.email }}</p>
      </div>
      <button
        @click="handleLogout"
        class="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm font-medium text-white/60 hover:bg-red-500/20 hover:text-red-300 transition-colors cursor-pointer"
      >
        <img src="/sign-out-svgrepo-com.svg" alt="Sign Out" class="w-5 h-5 shrink-0" />
        Sign Out
      </button>
    </div>
  </aside>
</template>
