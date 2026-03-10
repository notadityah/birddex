<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import ConfirmModal from './ConfirmModal.vue'
import FeedbackDetailModal from './FeedbackDetailModal.vue'

const adminStore = useAdminStore()

const filterStatus = ref('')
const page = ref(0)
const pageSize = 50

const confirmOpen = ref(false)
const confirmAction = ref(null)

const detailOpen = ref(false)
const selectedFeedback = ref(null)

function doSearch() {
  page.value = 0
  adminStore.loadFeedback({
    status: filterStatus.value,
    limit: pageSize,
    offset: 0,
  })
}

function nextPage() {
  page.value++
  adminStore.loadFeedback({
    status: filterStatus.value,
    limit: pageSize,
    offset: page.value * pageSize,
  })
}

function prevPage() {
  if (page.value > 0) page.value--
  adminStore.loadFeedback({
    status: filterStatus.value,
    limit: pageSize,
    offset: page.value * pageSize,
  })
}

function handleView(fb) {
  selectedFeedback.value = fb
  detailOpen.value = true
}

function handleDelete(fb) {
  confirmAction.value = async () => {
    await adminStore.deleteFeedback(fb.id)
    confirmOpen.value = false
  }
  confirmOpen.value = true
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString()
}

const statusColor = {
  open: 'bg-yellow-100 text-yellow-800',
  reviewed: 'bg-blue-100 text-blue-800',
  closed: 'bg-gray-100 text-gray-600',
}

const categoryColor = {
  bug: 'bg-red-100 text-red-700',
  detection: 'bg-orange-100 text-orange-700',
  suggestion: 'bg-purple-100 text-purple-700',
  other: 'bg-gray-100 text-gray-600',
}

onMounted(() => doSearch())
</script>

<template>
  <div>
    <!-- Filters -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <select
        v-model="filterStatus"
        aria-label="Filter by status"
        class="w-full sm:max-w-[180px] px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
      >
        <option value="">All Statuses</option>
        <option value="open">Open</option>
        <option value="reviewed">Reviewed</option>
        <option value="closed">Closed</option>
      </select>
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
      <table class="w-full min-w-[800px] text-sm">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="text-left px-4 py-3 font-medium text-gray-600">ID</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">User</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Category</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Message</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Page</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Status</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Date</th>
            <th class="text-right px-4 py-3 font-medium text-gray-600">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="fb in adminStore.feedback"
            :key="fb.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3 text-gray-500 font-mono text-xs break-all">
              {{ fb.id }}
            </td>
            <td class="px-4 py-3">
              <div class="font-medium">{{ fb.user_name }}</div>
              <div class="text-xs text-gray-500">{{ fb.user_email }}</div>
            </td>
            <td class="px-4 py-3">
              <span
                class="inline-block px-2 py-0.5 rounded-full text-xs font-medium"
                :class="categoryColor[fb.category] || categoryColor.other"
              >
                {{ fb.category }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-600 max-w-[200px] truncate">{{ fb.message }}</td>
            <td class="px-4 py-3 text-gray-500 text-xs max-w-[120px] truncate">
              {{ fb.page_url || '-' }}
            </td>
            <td class="px-4 py-3">
              <span
                class="inline-block px-2 py-0.5 rounded-full text-xs font-medium"
                :class="statusColor[fb.status] || statusColor.open"
              >
                {{ fb.status }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-500 text-xs">{{ formatDate(fb.created_at) }}</td>
            <td class="px-4 py-3 text-right space-x-2">
              <button
                @click="handleView(fb)"
                class="text-xs text-blue-600 hover:text-blue-800 font-medium cursor-pointer"
              >
                View
              </button>
              <button
                @click="handleDelete(fb)"
                class="text-xs text-red-600 hover:text-red-800 font-medium cursor-pointer"
              >
                Delete
              </button>
            </td>
          </tr>
          <tr v-if="adminStore.feedback.length === 0">
            <td colspan="8" class="px-4 py-8 text-center text-gray-500">No feedback found.</td>
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
        :disabled="adminStore.feedback.length < pageSize"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 cursor-pointer"
      >
        Next
      </button>
    </div>

    <ConfirmModal
      :open="confirmOpen"
      title="Delete Feedback"
      message="This will permanently delete this feedback entry. This cannot be undone."
      confirm-label="Delete"
      @confirm="confirmAction?.()"
      @cancel="confirmOpen = false"
    />

    <FeedbackDetailModal
      v-if="detailOpen"
      :feedback="selectedFeedback"
      @close="detailOpen = false"
      @updated="doSearch"
    />
  </div>
</template>
