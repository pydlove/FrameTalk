<script setup lang="ts">
import { computed } from 'vue'

type ControlWidth = 'auto' | 'form' | 'wide'

const props = withDefaults(
  defineProps<{
    title?: string
    description?: string
    controlWidth?: ControlWidth
    as?: 'div' | 'label'
  }>(),
  {
    title: '',
    description: '',
    controlWidth: 'auto',
    as: 'div',
  },
)

const classes = computed(() => [
  'settings-row',
  `settings-row--control-${props.controlWidth}`,
])
</script>

<template>
  <component :is="as" :class="classes">
    <div v-if="title || description || $slots.copy" class="settings-row__copy">
      <slot name="copy">
        <strong v-if="title">{{ title }}</strong>
        <span v-if="description">{{ description }}</span>
      </slot>
    </div>
    <div class="settings-row__control">
      <slot />
    </div>
  </component>
</template>

<style scoped>
.settings-row {
  display: flex;
  min-height: 72px;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-3) var(--space-4);
}

label.settings-row {
  cursor: pointer;
}

.settings-row__copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: var(--space-1);
}

.settings-row__copy > strong {
  color: var(--text);
  font-size: var(--font-body);
  font-weight: var(--weight-emphasis);
  line-height: var(--line-body);
}

.settings-row__copy > span {
  overflow: hidden;
  color: var(--text-subtle);
  font-size: var(--font-caption);
  line-height: var(--line-caption);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-row__control {
  display: flex;
  min-width: 0;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

.settings-row--control-form .settings-row__control {
  width: min(320px, 42%);
}

.settings-row--control-wide .settings-row__control {
  width: min(420px, 50%);
}

.settings-row--control-auto .settings-row__control {
  flex: 0 1 auto;
  max-width: 55%;
}

.settings-row__control :deep(input[data-settings-switch]) {
  width: 32px;
  height: 18px;
  padding: 0;
  flex: 0 0 auto;
  margin: 0;
  appearance: none;
  cursor: pointer;
  background: var(--segmented-track);
  border: 0;
  border-radius: 999px;
  box-shadow: inset 0 1px 2px rgb(0 0 0 / 10%);
  transition:
    background var(--duration-fast) var(--ease-apple),
    box-shadow var(--duration-fast) var(--ease-apple);
}

.settings-row__control :deep(input[data-settings-switch])::after {
  display: block;
  width: 14px;
  height: 14px;
  margin: 2px;
  content: "";
  background: var(--control-bg);
  border-radius: 50%;
  box-shadow: var(--shadow-control);
  transition: transform var(--duration-fast) var(--ease-apple);
}

.settings-row__control :deep(input[data-settings-switch]:checked) {
  background: var(--system-blue);
}

.settings-row__control :deep(input[data-settings-switch]:checked)::after {
  transform: translateX(14px);
}

.settings-row__control :deep(input[data-settings-switch]:focus-visible) {
  outline: none;
  box-shadow: var(--focus-ring);
}

@media (max-width: 760px) {
  .settings-row {
    min-height: 0;
    align-items: stretch;
    flex-direction: column;
    gap: var(--space-2);
  }

  .settings-row--control-auto .settings-row__control,
  .settings-row--control-form .settings-row__control,
  .settings-row--control-wide .settings-row__control {
    width: 100%;
    max-width: none;
    justify-content: flex-start;
  }
}
</style>
