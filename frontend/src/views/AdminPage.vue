<script setup>
import { ref, onMounted } from 'vue'
import { generateClient } from 'aws-amplify/data'
import { useAuth } from '@/composables/useAuth'
import Header from '@/components/landingpage/HeaderComponent.vue'

const { user } = useAuth()
const client = generateClient()

const users = ref([])
const loading = ref(true)
const actionLoading = ref('')
const error = ref('')
const successMessage = ref('')

onMounted(async () => {
  await fetchUsers()
})

async function fetchUsers() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await client.queries.listAllUsers()
    users.value = data || []
  } catch (err) {
    error.value = err?.message || 'Failed to load users.'
  } finally {
    loading.value = false
  }
}

async function promoteUser(username) {
  actionLoading.value = username
  error.value = ''
  successMessage.value = ''
  try {
    const { data } = await client.mutations.addUserToGroup({
      username,
      groupName: 'ADMINS',
    })
    if (data?.success) {
      successMessage.value = `User promoted to admin.`
      await fetchUsers()
    } else {
      error.value = data?.message || 'Failed to promote user.'
    }
  } catch (err) {
    error.value = err?.message || 'Failed to promote user.'
  } finally {
    actionLoading.value = ''
  }
}

async function demoteUser(username) {
  if (username === user.value?.username) {
    error.value = 'You cannot remove your own admin access.'
    return
  }
  actionLoading.value = username
  error.value = ''
  successMessage.value = ''
  try {
    const { data } = await client.mutations.removeUserFromGroup({
      username,
      groupName: 'ADMINS',
    })
    if (data?.success) {
      successMessage.value = `Admin access removed.`
      await fetchUsers()
    } else {
      error.value = data?.message || 'Failed to demote user.'
    }
  } catch (err) {
    error.value = err?.message || 'Failed to demote user.'
  } finally {
    actionLoading.value = ''
  }
}

function isUserAdmin(userItem) {
  return userItem.groups?.includes('ADMINS')
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <Header />

    <main class="flex-1 px-4 py-12 bg-white/90 backdrop-blur-none">
      <div class="max-w-4xl mx-auto">
        <!-- Page heading -->
        <div class="mb-8">
          <h1 class="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
          <p class="text-sm text-gray-500 mt-1">Manage users and assign roles.</p>
        </div>

        <!-- Success message -->
        <div
          v-if="successMessage"
          class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700"
        >
          {{ successMessage }}
        </div>

        <!-- Error banner -->
        <div
          v-if="error"
          class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700"
        >
          {{ error }}
        </div>

        <!-- Loading state -->
        <div v-if="loading" class="text-center py-16">
          <div
            class="inline-block w-8 h-8 border-4 border-forest-green/20 border-t-forest-green rounded-full animate-spin"
          ></div>
          <p class="text-sm text-gray-500 mt-3">Loading users...</p>
        </div>

        <!-- Users table -->
        <div v-else class="bg-white rounded-2xl shadow-xl overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="bg-forest-green text-white text-left text-sm">
                  <th class="px-6 py-3 font-semibold">Name</th>
                  <th class="px-6 py-3 font-semibold">Email</th>
                  <th class="px-6 py-3 font-semibold">Status</th>
                  <th class="px-6 py-3 font-semibold">Role</th>
                  <th class="px-6 py-3 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="u in users" :key="u.username" class="hover:bg-gray-50 transition-colors">
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div
                        class="w-8 h-8 bg-forest-green/10 rounded-full flex items-center justify-center text-forest-green text-sm font-bold shrink-0"
                      >
                        {{ u.name?.charAt(0)?.toUpperCase() || '?' }}
                      </div>
                      <span class="text-sm font-medium text-gray-900">{{ u.name || '—' }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-sm text-gray-600">{{ u.email }}</td>
                  <td class="px-6 py-4">
                    <span
                      class="text-xs font-medium px-2.5 py-0.5 rounded-full"
                      :class="
                        u.status === 'CONFIRMED'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-yellow-100 text-yellow-700'
                      "
                    >
                      {{ u.status }}
                    </span>
                  </td>
                  <td class="px-6 py-4">
                    <span
                      class="text-xs font-bold px-2.5 py-0.5 rounded-full"
                      :class="
                        isUserAdmin(u)
                          ? 'bg-primary-green/10 text-forest-green'
                          : 'bg-gray-100 text-gray-600'
                      "
                    >
                      {{ isUserAdmin(u) ? 'Admin' : 'User' }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-right">
                    <button
                      v-if="!isUserAdmin(u)"
                      @click="promoteUser(u.username)"
                      :disabled="actionLoading === u.username"
                      class="text-xs font-medium text-primary-green hover:text-forest-green transition-colors cursor-pointer disabled:opacity-50"
                    >
                      {{ actionLoading === u.username ? 'Processing...' : 'Make Admin' }}
                    </button>
                    <button
                      v-else-if="u.username !== user?.username"
                      @click="demoteUser(u.username)"
                      :disabled="actionLoading === u.username"
                      class="text-xs font-medium text-red-600 hover:text-red-800 transition-colors cursor-pointer disabled:opacity-50"
                    >
                      {{ actionLoading === u.username ? 'Processing...' : 'Remove Admin' }}
                    </button>
                    <span v-else class="text-xs text-gray-400">You</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Empty state -->
          <div v-if="users.length === 0" class="text-center py-12">
            <p class="text-gray-500 text-sm">No users found.</p>
          </div>
        </div>

        <!-- Back to dashboard -->
        <div class="mt-6 text-center">
          <router-link
            to="/dashboard"
            class="text-sm text-primary-green hover:text-forest-green font-medium transition-colors"
          >
            &larr; Back to dashboard
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>
