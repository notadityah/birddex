<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import Header from '@/components/landingpage/HeaderComponent.vue'

const router = useRouter()
const { user, isAdmin, logout, loading } = useAuth()

async function handleLogout() {
  await logout()
  router.push({ name: 'home' })
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <Header />

    <main class="flex-1 flex items-center justify-center px-4 py-12 bg-white/90 backdrop-blur-none">
      <div class="w-full max-w-2xl">
        <!-- Welcome card -->
        <div class="bg-white rounded-2xl shadow-xl p-8 sm:p-10 mb-6">
          <div class="flex items-center gap-4 mb-6">
            <div
              class="w-14 h-14 bg-forest-green rounded-full flex items-center justify-center text-white text-xl font-bold shrink-0"
            >
              {{ user?.name?.charAt(0)?.toUpperCase() || '?' }}
            </div>
            <div>
              <h1 class="text-2xl font-bold text-gray-900">
                Welcome, {{ user?.name || 'Birder' }}!
              </h1>
              <p class="text-sm text-gray-500">{{ user?.email }}</p>
            </div>
            <div v-if="isAdmin" class="ml-auto">
              <span
                class="bg-primary-green/10 text-forest-green text-xs font-bold px-3 py-1 rounded-full"
              >
                Admin
              </span>
            </div>
          </div>

          <p class="text-gray-600 mb-8">
            Your BirdDex dashboard. Start exploring and building your bird collection!
          </p>

          <div class="grid sm:grid-cols-2 gap-4">
            <!-- Placeholder cards for future features -->
            <div class="border border-gray-200 rounded-xl p-5 text-center">
              <div
                class="w-10 h-10 bg-forest-green/10 rounded-full flex items-center justify-center mx-auto mb-3"
              >
                <svg
                  class="w-5 h-5 text-forest-green"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
                  />
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
                  />
                </svg>
              </div>
              <h3 class="font-semibold text-gray-900 text-sm">Identify a Bird</h3>
              <p class="text-xs text-gray-500 mt-1">Upload a photo to identify</p>
            </div>

            <div class="border border-gray-200 rounded-xl p-5 text-center">
              <div
                class="w-10 h-10 bg-forest-green/10 rounded-full flex items-center justify-center mx-auto mb-3"
              >
                <svg
                  class="w-5 h-5 text-forest-green"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                  />
                </svg>
              </div>
              <h3 class="font-semibold text-gray-900 text-sm">My Collection</h3>
              <p class="text-xs text-gray-500 mt-1">View your spotted birds</p>
            </div>

            <router-link
              v-if="isAdmin"
              to="/admin"
              class="border border-primary-green/30 bg-primary-green/5 rounded-xl p-5 text-center hover:bg-primary-green/10 transition-colors sm:col-span-2"
            >
              <div
                class="w-10 h-10 bg-primary-green/20 rounded-full flex items-center justify-center mx-auto mb-3"
              >
                <svg
                  class="w-5 h-5 text-forest-green"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"
                  />
                </svg>
              </div>
              <h3 class="font-semibold text-forest-green text-sm">Admin Dashboard</h3>
              <p class="text-xs text-gray-500 mt-1">Manage users and roles</p>
            </router-link>
          </div>
        </div>

        <!-- Sign out -->
        <div class="text-center">
          <button
            @click="handleLogout"
            :disabled="loading"
            class="text-sm text-gray-500 hover:text-red-600 font-medium transition-colors cursor-pointer disabled:opacity-50"
          >
            Sign out
          </button>
        </div>
      </div>
    </main>
  </div>
</template>
