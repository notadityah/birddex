<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <main class="flex-1 flex items-center justify-center px-4 py-12 bg-white/90 backdrop-blur-none">
    <div class="w-full max-w-lg bg-white rounded-2xl shadow-xl p-8 sm:p-10">
      <!-- Heading -->
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p class="text-sm text-gray-500 mt-1">Welcome back, {{ authStore.displayName }}</p>
      </div>

      <!-- Profile card -->
      <div class="border border-gray-200 rounded-xl p-6 space-y-4">
        <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wider">Profile</h2>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <p class="text-xs text-gray-400 mb-1">Name</p>
            <p class="text-sm font-medium text-gray-900">
              {{ authStore.userProfile?.name || '—' }}
            </p>
          </div>
          <div>
            <p class="text-xs text-gray-400 mb-1">Email</p>
            <p class="text-sm font-medium text-gray-900">
              {{ authStore.userProfile?.email || '—' }}
            </p>
          </div>
          <div>
            <p class="text-xs text-gray-400 mb-1">Role</p>
            <span
              :class="[
                'inline-block text-xs font-semibold px-2.5 py-0.5 rounded-full',
                authStore.isAdmin ? 'bg-amber-100 text-amber-800' : 'bg-green-100 text-green-800',
              ]"
            >
              {{ authStore.userProfile?.role || 'user' }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>
