<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import AdminUsersTab from '@/components/admin/AdminUsersTab.vue'
import AdminBirdsTab from '@/components/admin/AdminBirdsTab.vue'
import AdminSightingsTab from '@/components/admin/AdminSightingsTab.vue'

const adminStore = useAdminStore()
const activeTab = ref('users')

const tabs = [
  { key: 'users', label: 'Users' },
  { key: 'birds', label: 'Birds' },
  { key: 'sightings', label: 'Sightings' },
]

onMounted(() => {
  adminStore.loadStats()
})
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 py-6 pt-16 md:pt-6">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Admin Dashboard</h1>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
      <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
        <p class="text-sm font-medium text-gray-500">Total Users</p>
        <p class="text-3xl font-bold text-gray-900 mt-1">{{ adminStore.stats.users }}</p>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
        <p class="text-sm font-medium text-gray-500">Total Birds</p>
        <p class="text-3xl font-bold text-gray-900 mt-1">{{ adminStore.stats.birds }}</p>
      </div>
      <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
        <p class="text-sm font-medium text-gray-500">Total Sightings</p>
        <p class="text-3xl font-bold text-gray-900 mt-1">{{ adminStore.stats.sightings }}</p>
      </div>
    </div>

    <!-- Tab Bar -->
    <div class="border-b border-gray-200 mb-6">
      <nav class="flex gap-6" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="activeTab = tab.key"
          role="tab"
          :aria-selected="activeTab === tab.key"
          :aria-controls="`tabpanel-${tab.key}`"
          :id="`tab-${tab.key}`"
          class="pb-3 text-sm font-medium border-b-2 transition-colors cursor-pointer"
          :class="
            activeTab === tab.key
              ? 'border-primary-green text-primary-green'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
          "
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Error Banner -->
    <div
      v-if="adminStore.error"
      role="alert"
      class="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm flex items-center justify-between"
    >
      <span>{{ adminStore.error }}</span>
      <button @click="adminStore.clearError()" class="text-red-500 hover:text-red-700 font-medium">
        Dismiss
      </button>
    </div>

    <!-- Tab Content -->
    <div v-if="activeTab === 'users'" role="tabpanel" id="tabpanel-users" aria-labelledby="tab-users">
      <AdminUsersTab />
    </div>
    <div v-else-if="activeTab === 'birds'" role="tabpanel" id="tabpanel-birds" aria-labelledby="tab-birds">
      <AdminBirdsTab />
    </div>
    <div v-else-if="activeTab === 'sightings'" role="tabpanel" id="tabpanel-sightings" aria-labelledby="tab-sightings">
      <AdminSightingsTab />
    </div>
  </div>
</template>
