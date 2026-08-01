<script setup>
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useBirdStore } from '@/stores/birds'
import Header from '@/components/landingpage/HeaderComponent.vue'
import Sidebar from '@/components/dashboard/SidebarComponent.vue'
import SpinnerIcon from '@/components/SpinnerIcon.vue'
import FeedbackButton from '@/components/FeedbackButton.vue'
import WelcomeModal from '@/components/dashboard/WelcomeModal.vue'
import { useOnboarding } from '@/composables/useOnboarding'

const authStore = useAuthStore()
const birdStore = useBirdStore()
const route = useRoute()
const router = useRouter()
const onboarding = useOnboarding()

watch(
  () => authStore.isAuthenticated && route.meta.guestOnly,
  (shouldRedirect) => {
    if (shouldRedirect) router.replace('/mydex')
  },
)

watch(
  () => authStore.isAuthenticated,
  (authed) => {
    if (authed) {
      birdStore.loadSightings()
      onboarding.maybeAutoOpen()
    } else {
      birdStore.resetFound()
    }
  },
  { immediate: true },
)
</script>

<template>
  <!-- Loading gate -->
  <div
    v-if="authStore.loading"
    class="min-h-screen flex items-center justify-center bg-white/90 backdrop-blur-none"
  >
    <SpinnerIcon />
  </div>

  <!-- Authenticated layout: sidebar + content -->
  <div v-else-if="authStore.isAuthenticated" class="min-h-screen flex bg-gray-50">
    <Sidebar />
    <main class="flex-1 overflow-y-auto">
      <router-view />
    </main>
    <FeedbackButton />
    <WelcomeModal v-if="onboarding.open.value" @close="onboarding.dismiss()" />
  </div>

  <!-- Public layout: header + content -->
  <div v-else class="min-h-screen flex flex-col">
    <Header />
    <router-view />
  </div>
</template>
