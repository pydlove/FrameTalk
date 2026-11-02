<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="settings-card">
    <header class="settings-card__header">
      <slot name="leading" />
      <div class="settings-card__heading">
        <h2>{{ title }}</h2>
        <p v-if="description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="settings-card__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="settings-card__body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.settings-card {
  overflow: hidden;
  background: var(--canvas-raised);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-panel);
}

.settings-card__header {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--surface-secondary);
  border-bottom: 1px solid var(--line-soft);
}

.settings-card__heading {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.settings-card__heading h2 {
  margin: 0;
  color: var(--text);
  font-size: var(--font-body);
  font-weight: var(--weight-emphasis);
  line-height: 1.3;
}

.settings-card__heading p {
  margin: 0;
  color: var(--text-subtle);
  font-size: var(--font-caption);
  line-height: var(--line-caption);
}

.settings-card__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--space-2);
}

.settings-card__body {
  background: var(--canvas-raised);
}
</style>
