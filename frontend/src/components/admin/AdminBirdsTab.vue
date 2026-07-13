<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { useAdminPagination } from '@/composables/useAdminPagination'
import AdminPagination from './AdminPagination.vue'
import ConfirmModal from './ConfirmModal.vue'

const adminStore = useAdminStore()

const search = ref('')
const { page, pageSize, reload } = useAdminPagination(() =>
  adminStore.loadBirds({
    query: search.value,
    limit: pageSize.value,
    offset: page.value * pageSize.value,
  }),
)

const confirmOpen = ref(false)
const confirmAction = ref(null)
const confirmMessage = ref('')

function doSearch() {
  reload()
}

function handleDelete(bird) {
  confirmMessage.value = `Delete "${bird.name}"? This cannot be undone.`
  confirmAction.value = async () => {
    await adminStore.deleteBird(bird.id)
    confirmOpen.value = false
  }
  confirmOpen.value = true
}

onMounted(() => doSearch())
</script>

<template>
  <div>
    <!-- Search -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <input
        v-model="search"
        @keyup.enter="doSearch"
        type="text"
        placeholder="Search birds..."
        aria-label="Search birds"
        class="flex-1 min-w-[200px] max-w-md px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-green/50 focus:border-primary-green"
      />
    </div>

    <!-- Loading -->
    <div v-if="adminStore.loading" class="text-center py-8 text-gray-500">Loading...</div>

    <!-- Table -->
    <div v-else class="overflow-x-auto bg-white rounded-xl shadow-sm border border-gray-200">
      <table class="w-full min-w-[600px] text-sm">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="text-left px-4 py-3 font-medium text-gray-600">ID</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Name</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Scientific Name</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Slug</th>
            <th class="text-right px-4 py-3 font-medium text-gray-600">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="bird in adminStore.birds"
            :key="bird.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3 text-gray-500">{{ bird.id }}</td>
            <td class="px-4 py-3 font-medium">{{ bird.name }}</td>
            <td class="px-4 py-3 text-gray-600 italic">{{ bird.scientific_name }}</td>
            <td class="px-4 py-3 text-gray-500 font-mono text-xs">{{ bird.slug }}</td>
            <td class="px-4 py-3 text-right space-x-2">
              <button
                @click="handleDelete(bird)"
                class="text-xs text-red-600 hover:text-red-800 font-medium cursor-pointer"
              >
                Delete
              </button>
            </td>
          </tr>
          <tr v-if="adminStore.birds.length === 0">
            <td colspan="5" class="px-4 py-8 text-center text-gray-500">No birds found.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <AdminPagination
      v-model:page="page"
      v-model:pageSize="pageSize"
      :item-count="adminStore.birds.length"
    />

    <ConfirmModal
      :open="confirmOpen"
      title="Delete Bird"
      :message="confirmMessage"
      confirm-label="Delete"
      @confirm="confirmAction?.()"
      @cancel="confirmOpen = false"
    />
  </div>
</template>
