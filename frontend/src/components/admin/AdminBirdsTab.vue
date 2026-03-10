<script setup>
import { ref, onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import ConfirmModal from './ConfirmModal.vue'

const formModalRef = ref(null)

const adminStore = useAdminStore()

const search = ref('')
const page = ref(0)
const pageSize = 50

// Form state
const showForm = ref(false)
const editingBird = ref(null)
const formName = ref('')
const formScientific = ref('')
const formSlug = ref('')

const confirmOpen = ref(false)
const confirmAction = ref(null)
const confirmMessage = ref('')

function doSearch() {
  page.value = 0
  adminStore.loadBirds({ query: search.value, limit: pageSize, offset: 0 })
}

function nextPage() {
  page.value++
  adminStore.loadBirds({ query: search.value, limit: pageSize, offset: page.value * pageSize })
}

function prevPage() {
  if (page.value > 0) page.value--
  adminStore.loadBirds({ query: search.value, limit: pageSize, offset: page.value * pageSize })
}

function openAddForm() {
  editingBird.value = null
  formName.value = ''
  formScientific.value = ''
  formSlug.value = ''
  showForm.value = true
}

function openEditForm(bird) {
  editingBird.value = bird
  formName.value = bird.name
  formScientific.value = bird.scientific_name
  formSlug.value = bird.slug
  showForm.value = true
}

async function submitForm() {
  const data = {
    name: formName.value,
    scientificName: formScientific.value,
    slug: formSlug.value || undefined,
  }

  let result
  if (editingBird.value) {
    result = await adminStore.updateBird(editingBird.value.id, data)
  } else {
    result = await adminStore.createBird(data)
  }

  if (result) {
    showForm.value = false
  }
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
    <!-- Search + Add -->
    <div class="flex flex-wrap items-center gap-3 mb-4">
      <input
        v-model="search"
        @keyup.enter="doSearch"
        type="text"
        placeholder="Search birds..."
        aria-label="Search birds"
        class="flex-1 min-w-[200px] max-w-md px-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-green/50 focus:border-primary-green"
      />
      <button
        @click="openAddForm"
        class="px-4 py-2 text-sm font-medium text-white bg-primary-green rounded-lg hover:bg-primary-green/90 transition-colors cursor-pointer"
      >
        Add Bird
      </button>
    </div>

    <!-- Add/Edit Form Modal -->
    <Teleport to="body">
      <div v-if="showForm" class="fixed inset-0 z-[100] flex items-center justify-center" @keydown.escape="showForm = false">
        <div class="fixed inset-0 bg-black/40" @click="showForm = false" aria-hidden="true" />
        <div ref="formModalRef" role="dialog" aria-modal="true" aria-labelledby="bird-form-title" class="relative bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
          <h3 id="bird-form-title" class="text-lg font-semibold text-gray-900 mb-4">
            {{ editingBird ? 'Edit Bird' : 'Add Bird' }}
          </h3>
          <form @submit.prevent="submitForm" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <input
                v-model="formName"
                required
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-green/50"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Scientific Name</label>
              <input
                v-model="formScientific"
                required
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-green/50"
              />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1"
                >Slug (auto-generated if empty)</label
              >
              <input
                v-model="formSlug"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-green/50"
              />
            </div>
            <div class="flex justify-end gap-3 pt-2">
              <button
                type="button"
                @click="showForm = false"
                class="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                class="px-4 py-2 text-sm font-medium text-white bg-primary-green rounded-lg hover:bg-primary-green/90 cursor-pointer"
              >
                {{ editingBird ? 'Save' : 'Create' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

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
                @click="openEditForm(bird)"
                class="text-xs text-blue-600 hover:text-blue-800 font-medium cursor-pointer"
              >
                Edit
              </button>
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
        :disabled="adminStore.birds.length < pageSize"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 cursor-pointer"
      >
        Next
      </button>
    </div>

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
