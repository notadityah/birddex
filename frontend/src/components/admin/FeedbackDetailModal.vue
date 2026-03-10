<script setup>
import { ref } from 'vue'
import { useFocusTrap } from '@/composables/useFocusTrap'
import { useModalLifecycle } from '@/composables/useModalLifecycle'
import { useAdminStore } from '@/stores/admin'

const props = defineProps({
  feedback: { type: Object, required: true },
})

const emit = defineEmits(['close', 'updated'])
const adminStore = useAdminStore()

const dialogRef = ref(null)
const adminNotes = ref(props.feedback.admin_notes ?? '')
const saving = ref(false)

useFocusTrap(dialogRef)
useModalLifecycle(() => emit('close'))

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString()
}

async function updateStatus(status) {
  saving.value = true
  const ok = await adminStore.updateFeedback(props.feedback.id, { status })
  if (ok) {
    props.feedback.status = status
    emit('updated')
  }
  saving.value = false
}

async function saveNotes() {
  saving.value = true
  const ok = await adminStore.updateFeedback(props.feedback.id, { adminNotes: adminNotes.value })
  if (ok) {
    props.feedback.admin_notes = adminNotes.value
    emit('updated')
  }
  saving.value = false
}

const statusColor = {
  open: 'bg-yellow-100 text-yellow-800',
  reviewed: 'bg-blue-100 text-blue-800',
  closed: 'bg-gray-100 text-gray-600',
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-[100] flex items-center justify-center">
      <div class="fixed inset-0 bg-black/40" @click="emit('close')" aria-hidden="true" />
      <div ref="dialogRef" role="dialog" aria-modal="true" aria-labelledby="feedback-detail-title" class="relative bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6 max-h-[90vh] overflow-y-auto">
        <h3 id="feedback-detail-title" class="text-lg font-semibold text-gray-900 mb-4">Feedback Details</h3>

        <!-- Meta -->
        <div class="space-y-3 mb-5 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-500">Status</span>
            <span class="inline-block px-2 py-0.5 rounded-full text-xs font-medium" :class="statusColor[feedback.status]">{{ feedback.status }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">Category</span>
            <span class="font-medium text-gray-900 capitalize">{{ feedback.category }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">User</span>
            <span class="font-medium text-gray-900">{{ feedback.user_name }} ({{ feedback.user_email }})</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">Page</span>
            <span class="font-mono text-xs text-gray-700">{{ feedback.page_url || '-' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">Date</span>
            <span class="text-gray-700">{{ formatDate(feedback.created_at) }}</span>
          </div>
        </div>

        <!-- Message -->
        <div class="mb-5">
          <label class="block text-sm font-medium text-gray-700 mb-1">Message</label>
          <div class="p-3 bg-gray-50 rounded-lg text-sm text-gray-800 whitespace-pre-wrap break-words">{{ feedback.message }}</div>
        </div>

        <!-- Admin Notes -->
        <div class="mb-5">
          <label class="block text-sm font-medium text-gray-700 mb-1">Admin Notes</label>
          <textarea
            v-model="adminNotes"
            rows="3"
            maxlength="2000"
            placeholder="Add notes..."
            class="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none focus:outline-none focus:ring-2 focus:ring-forest-green/50 focus:border-forest-green"
          />
          <div class="flex justify-end mt-1">
            <button
              @click="saveNotes"
              :disabled="saving"
              class="text-xs text-forest-green hover:text-forest-green/80 font-medium disabled:opacity-50"
            >
              Save Notes
            </button>
          </div>
        </div>

        <!-- Status Actions -->
        <div class="flex flex-wrap gap-2 mb-5">
          <button
            v-if="feedback.status !== 'reviewed'"
            @click="updateStatus('reviewed')"
            :disabled="saving"
            class="px-3 py-1.5 text-xs font-medium text-blue-700 bg-blue-100 rounded-lg hover:bg-blue-200 transition-colors disabled:opacity-50 cursor-pointer"
          >
            Mark Reviewed
          </button>
          <button
            v-if="feedback.status !== 'closed'"
            @click="updateStatus('closed')"
            :disabled="saving"
            class="px-3 py-1.5 text-xs font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors disabled:opacity-50 cursor-pointer"
          >
            Close
          </button>
          <button
            v-if="feedback.status !== 'open'"
            @click="updateStatus('open')"
            :disabled="saving"
            class="px-3 py-1.5 text-xs font-medium text-yellow-700 bg-yellow-100 rounded-lg hover:bg-yellow-200 transition-colors disabled:opacity-50 cursor-pointer"
          >
            Reopen
          </button>
        </div>

        <!-- Close Button -->
        <div class="flex justify-end">
          <button
            @click="emit('close')"
            class="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors cursor-pointer"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
