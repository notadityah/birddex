<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  // Number of items on the current page — used to detect the last page when
  // the endpoint doesn't return a total count.
  itemCount: { type: Number, required: true },
  // Total row count, when the endpoint provides one (feedback does).
  total: { type: Number, default: null },
})

const emit = defineEmits(['update:page', 'update:pageSize'])

const pageCount = computed(() =>
  props.total !== null ? Math.max(1, Math.ceil(props.total / props.pageSize)) : null,
)

const nextDisabled = computed(() =>
  pageCount.value !== null
    ? props.page + 1 >= pageCount.value
    : props.itemCount < props.pageSize,
)

function onSizeChange(e) {
  emit('update:pageSize', Number(e.target.value))
}
</script>

<template>
  <div class="flex flex-wrap items-center justify-between gap-3 mt-4">
    <label class="flex items-center gap-2 text-sm text-gray-500">
      Rows per page
      <select
        :value="pageSize"
        @change="onSizeChange"
        class="border border-gray-300 rounded-lg px-2 py-1.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-green/50 cursor-pointer"
      >
        <option v-for="size in [5, 10, 15, 25, 50]" :key="size" :value="size">
          {{ size }}
        </option>
      </select>
    </label>

    <div class="flex items-center gap-3">
      <button
        type="button"
        @click="emit('update:page', page - 1)"
        :disabled="page === 0"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 cursor-pointer"
      >
        Previous
      </button>
      <span class="text-sm text-gray-500">
        Page {{ page + 1 }}<template v-if="pageCount !== null"> of {{ pageCount }}</template>
      </span>
      <button
        type="button"
        @click="emit('update:page', page + 1)"
        :disabled="nextDisabled"
        class="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 cursor-pointer"
      >
        Next
      </button>
    </div>
  </div>
</template>
