<script setup>
import { computed } from 'vue'
import { birdImageUrl, birdImageAttribution } from '@/data/birdImages'

const props = defineProps({
  bird: { type: Object, default: null },
  // Reference photo is optional so the card can be reused in contexts that
  // already show their own image (e.g. detail modals).
  showPhoto: { type: Boolean, default: true },
})

const attribution = computed(() =>
  props.bird ? birdImageAttribution(props.bird.slug) : null,
)

const rarityClasses = {
  common: 'bg-gray-100 text-gray-700',
  uncommon: 'bg-green-100 text-green-800',
  rare: 'bg-amber-100 text-amber-800',
}

const stats = computed(() => {
  if (!props.bird) return []
  return [
    { label: 'Wingspan', value: props.bird.wingspan },
    { label: 'Length', value: props.bird.length },
    { label: 'Weight', value: props.bird.weight },
    { label: 'Diet', value: props.bird.diet },
  ].filter((s) => s.value)
})

const detailRows = computed(() => {
  if (!props.bird) return []
  return [
    { label: 'Appearance', value: props.bird.appearance },
    { label: 'Habitat', value: props.bird.habitat },
    { label: 'Call', value: props.bird.callDescription },
  ].filter((r) => r.value)
})

function onImageError(e) {
  e.target.closest('[data-photo-frame]').style.display = 'none'
}
</script>

<template>
  <div v-if="bird" class="bg-white border border-gray-200 rounded-xl overflow-hidden">
    <!-- Reference photo -->
    <div v-if="showPhoto" data-photo-frame class="bg-gray-100">
      <img
        :src="birdImageUrl(bird.slug)"
        :alt="`Reference photo of ${bird.name}`"
        loading="lazy"
        class="w-full max-h-56 object-cover"
        @error="onImageError"
      />
      <p v-if="attribution" class="px-3 py-1 text-[11px] text-gray-500 bg-gray-50">
        Photo:
        <a
          :href="attribution.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="underline hover:text-gray-700"
          >{{ attribution.author }}</a
        >
        ({{ attribution.license }})
      </p>
    </div>

    <!-- Header band -->
    <div class="bg-primary-green px-5 py-3">
      <div class="flex items-center justify-between gap-2 flex-wrap">
        <div>
          <h3 class="text-lg font-semibold text-white leading-tight">{{ bird.name }}</h3>
          <p class="text-sm italic text-green-100">{{ bird.scientificName }}</p>
        </div>
        <div class="flex gap-2">
          <span
            v-if="bird.rarity"
            class="px-2.5 py-0.5 text-xs font-semibold rounded-full capitalize"
            :class="rarityClasses[bird.rarity] ?? rarityClasses.common"
          >
            {{ bird.rarity }}
          </span>
          <span
            v-if="bird.conservationStatus"
            class="px-2.5 py-0.5 text-xs font-medium rounded-full bg-white/20 text-white"
          >
            {{ bird.conservationStatus }}
          </span>
        </div>
      </div>
    </div>

    <div class="p-5">
      <!-- Stat grid -->
      <dl v-if="stats.length" class="grid grid-cols-2 gap-x-4 gap-y-3 mb-4">
        <div v-for="stat in stats" :key="stat.label">
          <dt class="text-xs font-medium text-gray-500 uppercase tracking-wide">
            {{ stat.label }}
          </dt>
          <dd class="text-sm text-gray-800 mt-0.5">{{ stat.value }}</dd>
        </div>
      </dl>

      <!-- Detail rows -->
      <dl v-if="detailRows.length" class="space-y-2 mb-4">
        <div v-for="row in detailRows" :key="row.label">
          <dt class="text-xs font-medium text-gray-500 uppercase tracking-wide">
            {{ row.label }}
          </dt>
          <dd class="text-sm text-gray-700 mt-0.5">{{ row.value }}</dd>
        </div>
      </dl>

      <!-- Fun fact -->
      <div
        v-if="bird.funFact"
        class="bg-green-50 border border-green-100 rounded-lg px-4 py-3"
      >
        <p class="text-xs font-semibold text-green-800 uppercase tracking-wide mb-1">
          Fun fact
        </p>
        <p class="text-sm text-green-900">{{ bird.funFact }}</p>
      </div>
    </div>
  </div>
</template>
