<script setup lang="ts">
import { Search, X } from '@lucide/vue'

withDefaults(
  defineProps<{
    label?: string
    placeholder?: string
    clearable?: boolean
  }>(),
  {
    label: '搜索',
    placeholder: '搜索',
    clearable: false,
  },
)

const model = defineModel<string>({ default: '' })
</script>

<template>
  <label class="search-input">
    <Search :size="14" aria-hidden="true" />
    <input
      v-model="model"
      type="search"
      :aria-label="label"
      :placeholder="placeholder"
      autocomplete="off"
    />
    <button
      v-if="clearable && model"
      type="button"
      aria-label="清空搜索"
      title="清空搜索"
      @click.prevent="model = ''"
    >
      <X :size="13" />
    </button>
  </label>
</template>

<style scoped>
.search-input {
  display: flex;
  height: var(--control-md);
  align-items: center;
  gap: 6px;
  padding: 0 8px;
  color: var(--text-subtle);
  background: var(--field-bg);
  border: 1px solid var(--field-border);
  border-radius: var(--radius-control);
  box-shadow: var(--control-inset-shadow);
}

.search-input:focus-within {
  border-color: var(--system-blue);
  box-shadow: var(--focus-ring);
}

.search-input input {
  width: 100%;
  min-width: 0;
  height: 100%;
  padding: 0;
  font-size: var(--font-body);
  background: transparent;
  border: 0;
}

.search-input input::-webkit-search-cancel-button {
  display: none;
}

.search-input button {
  display: grid;
  width: 20px;
  height: 20px;
  padding: 0;
  flex: 0 0 auto;
  color: var(--text-subtle);
  background: transparent;
  border-radius: var(--radius-badge);
  place-items: center;
}

.search-input button:hover {
  color: var(--text);
  background: var(--hover-bg);
}
</style>
