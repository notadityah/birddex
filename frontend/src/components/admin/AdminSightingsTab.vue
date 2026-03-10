<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import ConfirmModal from './ConfirmModal.vue'

const adminStore = useAdminStore()

const filterUserId = ref('')
const filterBirdId = ref('')
const page = ref(0)
const pageSize = 50

const confirmOpen = ref(false)
const confirmAction = ref(null)

function doSearch() {
  page.value = 0
  adminStore.loadSightings({
    userId: filterUserId.value,
    birdId: filterBirdId.value,
    limit: pageSize,
    offset: 0,
  })
}

function nextPage() {
  page.value++
  adminStore.loadSightings({
    userId: filterUserId.value,
    birdId: filterBirdId.value,
    limit: pageSize,
    offset: page.value * pageSize,
  })
}

function prevPage() {
  if (page.value > 0) page.value--
  adminStore.loadSightings({
    userId: filterUserId.value,
    birdId: filterBirdId.value,
    limit: pageSize,
    offset: page.value * pageSize,
  })
}

function handleDelete(sighting) {
  confirmAction.value = async () => {
    await adminStore.deleteSighting(sighting.id)
    confirmOpen.value = false
  }
  confirmOpen.value = true
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString()
}

onMounted(() => doSearch())
</script>

<template>
  <div>
    <!-- Filters -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <input
        v-model="filterUserId"
        @keyup.enter="doSearch"
        type="text"
        placeholder="Filter by User ID..."
        aria-label="Filter by user ID"
        class="w-full sm:max-w-[220px] px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
      />
      <input
        v-model="filterBirdId"
        @keyup.enter="doSearch"
        type="text"
        placeholder="Filter by Bird ID..."
        aria-label="Filter by bird ID"
        class="w-full sm:max-w-[180px] px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
      />
      <button
        @click="doSearch"
        class="px-4 py-2 text-sm font-medium text-white bg-forest-green rounded-lg hover:bg-forest-green/90 transition-colors cursor-pointer"
      >
        Filter
      </button>
    </div>

    <!-- Loading -->
    <div v-if="adminStore.loading" class="text-center py-8 text-gray-500">Loading...</div>

    <!-- Table -->
    <div v-else class="overflow-x-auto bg-white rounded-xl shadow-sm border border-gray-200">
      <table class="w-full min-w-[700px] text-sm">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="text-left px-4 py-3 font-medium text-gray-600">ID</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">User</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Bird</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Detected</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Notes</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Image</th>
            <th class="text-right px-4 py-3 font-medium text-gray-600">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="s in adminStore.sightings"
            :key="s.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3 text-gray-500 font-mono text-xs break-all">
              {{ s.id }}
            </td>
            <td class="px-4 py-3">
              <div class="font-medium">{{ s.user_name }}</div>
              <div class="text-xs text-gray-500">{{ s.user_email }}</div>
            </td>
            <td class="px-4 py-3">{{ s.bird_name }}</td>
            <td class="px-4 py-3 text-gray-500 text-xs">{{ formatDate(s.detected_at) }}</td>
            <td class="px-4 py-3 text-gray-600 max-w-[200px] truncate">{{ s.notes || '-' }}</td>
            <td class="px-4 py-3">
              <span v-if="s.image_key" class="text-xs text-green-600">Yes</span>
              <span v-else class="text-xs text-gray-400">No</span>
            </td>
            <td class="px-4 py-3 text-right">
              <button
                @click="handleDelete(s)"
                class="text-xs text-red-600 hover:text-red-800 font-medium cursor-pointer"
              >
                Delete
              </button>
            </td>
          </tr>
          <tr v-if="adminStore.sightings.length === 0">
            <td colspan="7" class="px-4 py-8 text-center text-gray-500">No sightings found.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="flex items-center justify-between mt-4">
      <button
        @click="prevPage"
        :disabled="page === 0"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 cursor-pointer"
      >
        Previous
      </button>
      <span class="text-sm text-gray-500">Page {{ page + 1 }}</span>
      <button
        @click="nextPage"
        :disabled="adminStore.sightings.length < pageSize"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 cursor-pointer"
      >
        Next
      </button>
    </div>

    <ConfirmModal
      :open="confirmOpen"
      title="Delete Sighting"
      message="This will permanently delete this sighting and its associated image. This cannot be undone."
      confirm-label="Delete"
      @confirm="confirmAction?.()"
      @cancel="confirmOpen = false"
    />
  </div>
</template>
