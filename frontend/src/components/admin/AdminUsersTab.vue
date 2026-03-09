<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { useAuthStore } from '@/stores/auth'
import ConfirmModal from './ConfirmModal.vue'

const adminStore = useAdminStore()
const authStore = useAuthStore()

const search = ref('')
const page = ref(0)
const pageSize = 50

const confirmOpen = ref(false)
const confirmAction = ref(null)
const confirmTitle = ref('')
const confirmMessage = ref('')

function doSearch() {
  page.value = 0
  adminStore.loadUsers({ query: search.value, limit: pageSize, offset: 0 })
}

function nextPage() {
  page.value++
  adminStore.loadUsers({ query: search.value, limit: pageSize, offset: page.value * pageSize })
}

function prevPage() {
  if (page.value > 0) page.value--
  adminStore.loadUsers({ query: search.value, limit: pageSize, offset: page.value * pageSize })
}

function isSelf(userId) {
  return authStore.user?.id === userId
}

function handleSetRole(userId, role) {
  if (isSelf(userId)) return
  confirmTitle.value = 'Change Role'
  confirmMessage.value = `Set this user's role to "${role}"?`
  confirmAction.value = async () => {
    await adminStore.setUserRole(userId, role)
    confirmOpen.value = false
  }
  confirmOpen.value = true
}

function handleBan(userId) {
  if (isSelf(userId)) return
  confirmTitle.value = 'Ban User'
  confirmMessage.value = 'This will ban the user from the platform.'
  confirmAction.value = async () => {
    await adminStore.banUser(userId, 'Banned by admin')
    confirmOpen.value = false
  }
  confirmOpen.value = true
}

function handleUnban(userId) {
  confirmTitle.value = 'Unban User'
  confirmMessage.value = "This will restore the user's access."
  confirmAction.value = async () => {
    await adminStore.unbanUser(userId)
    confirmOpen.value = false
  }
  confirmOpen.value = true
}

function handleDelete(userId) {
  if (isSelf(userId)) return
  confirmTitle.value = 'Delete User'
  confirmMessage.value =
    'This will permanently delete the user and all their data. This cannot be undone.'
  confirmAction.value = async () => {
    await adminStore.removeUser(userId)
    confirmOpen.value = false
  }
  confirmOpen.value = true
}

onMounted(() => doSearch())
</script>

<template>
  <div>
    <!-- Search -->
    <div class="mb-4">
      <input
        v-model="search"
        @keyup.enter="doSearch"
        type="text"
        placeholder="Search by email..."
        aria-label="Search users by email"
        class="w-full max-w-md px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
      />
    </div>

    <!-- Loading -->
    <div v-if="adminStore.loading" class="text-center py-8 text-gray-500">Loading...</div>

    <!-- Table -->
    <div v-else class="overflow-x-auto bg-white rounded-xl shadow-sm border border-gray-200">
      <table class="w-full min-w-[600px] text-sm">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="text-left px-4 py-3 font-medium text-gray-600">Name</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Email</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Role</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Banned</th>
            <th class="text-left px-4 py-3 font-medium text-gray-600">Created</th>
            <th class="text-right px-4 py-3 font-medium text-gray-600">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="u in adminStore.users"
            :key="u.id"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td class="px-4 py-3">{{ u.name }}</td>
            <td class="px-4 py-3 text-gray-600">{{ u.email }}</td>
            <td class="px-4 py-3">
              <select
                :value="u.role || 'user'"
                @change="handleSetRole(u.id, $event.target.value)"
                :disabled="isSelf(u.id)"
                class="text-xs border border-gray-300 rounded px-2 py-1 bg-white disabled:opacity-50"
              >
                <option value="user">user</option>
                <option value="admin">admin</option>
              </select>
            </td>
            <td class="px-4 py-3">
              <span
                v-if="u.banned"
                class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700"
              >
                Yes
              </span>
              <span v-else class="text-gray-400 text-xs">No</span>
            </td>
            <td class="px-4 py-3 text-gray-500 text-xs">
              {{ u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-' }}
            </td>
            <td class="px-4 py-3 text-right space-x-2">
              <button
                v-if="!u.banned && !isSelf(u.id)"
                @click="handleBan(u.id)"
                class="text-xs text-orange-600 hover:text-orange-800 font-medium cursor-pointer"
              >
                Ban
              </button>
              <button
                v-if="u.banned"
                @click="handleUnban(u.id)"
                class="text-xs text-green-600 hover:text-green-800 font-medium cursor-pointer"
              >
                Unban
              </button>
              <button
                v-if="!isSelf(u.id)"
                @click="handleDelete(u.id)"
                class="text-xs text-red-600 hover:text-red-800 font-medium cursor-pointer"
              >
                Delete
              </button>
              <span v-if="isSelf(u.id)" class="text-xs text-gray-400 italic">You</span>
            </td>
          </tr>
          <tr v-if="adminStore.users.length === 0">
            <td colspan="6" class="px-4 py-8 text-center text-gray-500">No users found.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="flex items-center justify-between mt-4">
      <button
        @click="prevPage"
        :disabled="page === 0"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50"
      >
        Previous
      </button>
      <span class="text-sm text-gray-500">Page {{ page + 1 }}</span>
      <button
        @click="nextPage"
        :disabled="adminStore.users.length < pageSize"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50"
      >
        Next
      </button>
    </div>

    <ConfirmModal
      :open="confirmOpen"
      :title="confirmTitle"
      :message="confirmMessage"
      confirm-label="Confirm"
      @confirm="confirmAction?.()"
      @cancel="confirmOpen = false"
    />
  </div>
</template>
