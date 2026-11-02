<script setup lang="ts">
import type { SegmentedItem } from '@/types/settings'

defineProps<{
  modelValue: string
  items: SegmentedItem<string>[]
  ariaLabel?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <div class="segmented-control" role="group" :aria-label="ariaLabel">
    <button
      v-for="item in items"
      :key="item.value"
      type="button"
      :aria-pressed="modelValue === item.value"
      :class="{ 'segmented-control__active': modelValue === item.value }"
      @click="emit('update:modelValue', item.value)"
    >
      {{ item.label }}
    </button>
  </div>
</template>

<style scoped>
.segmented-control {
  display: inline-flex;
  gap: 2px;
  padding: 2px;
  background: var(--segmented-track);
  border-radius: var(--radius-control);
}

.segmented-control button {
  height: var(--control-sm);
  padding: 0 9px;
  color: var(--text-muted);
  font-size: var(--font-caption);
  font-weight: var(--weight-regular);
  white-space: nowrap;
  background: transparent;
  border-radius: 4px;
}

.segmented-control button:hover {
  color: var(--text);
}

.segmented-control__active {
  color: var(--text) !important;
  font-weight: var(--weight-emphasis) !important;
  background: var(--canvas-raised) !important;
  box-shadow: var(--shadow-control);
}
</style>
