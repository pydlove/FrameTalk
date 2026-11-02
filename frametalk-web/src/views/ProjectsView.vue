<script setup lang="ts">
import { FolderKanban } from '@lucide/vue'

import { PRODUCT_NAME } from '@/brand'
import { useAuthStore } from '@/stores/useAuthStore'

const auth = useAuthStore()
</script>

<template>
  <section class="projects-view">
    <header class="projects-view__header">
      <div>
        <p class="projects-view__eyebrow">{{ PRODUCT_NAME }} · 创作</p>
        <h1>项目</h1>
        <p>整理脚本、真人口播和素材，把内容推进到成片。</p>
      </div>
      <span class="projects-view__account">{{ auth.email }}</span>
    </header>

    <div class="projects-view__empty">
      <span class="projects-view__empty-icon" aria-hidden="true">
        <FolderKanban :size="28" />
      </span>
      <div>
        <h2>还没有项目</h2>
        <p>创建项目后，脚本、素材和成片会显示在这里。</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.projects-view {
  min-height: 100%;
  padding: var(--space-8);
}

.projects-view__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
}

.projects-view__eyebrow {
  margin: 0 0 var(--space-2);
  color: var(--text-subtle);
  font-family: var(--font-mono);
  font-size: var(--font-caption);
  font-weight: var(--weight-emphasis);
}

.projects-view__header h1 {
  margin: 0;
  color: var(--text);
  font-size: var(--font-display);
  font-weight: var(--weight-emphasis);
  line-height: 36px;
}

.projects-view__header p:last-child {
  margin: var(--space-2) 0 0;
  color: var(--text-muted);
  font-size: var(--font-body);
}

.projects-view__account {
  overflow: hidden;
  color: var(--text-subtle);
  font-family: var(--font-mono);
  font-size: var(--font-caption);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.projects-view__empty {
  display: flex;
  min-height: 240px;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-6);
  background: var(--canvas-raised);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-panel);
}

.projects-view__empty-icon {
  display: grid;
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  color: var(--text-muted);
  background: var(--surface-inset);
  border-radius: var(--radius-panel);
  place-items: center;
}

.projects-view__empty > div {
  min-width: 0;
  flex: 1;
}

.projects-view__empty h2 {
  margin: 0;
  color: var(--text);
  font-size: var(--font-title);
  font-weight: var(--weight-emphasis);
}

.projects-view__empty p {
  margin: var(--space-1) 0 0;
  color: var(--text-subtle);
  font-size: var(--font-caption);
}

@media (max-width: 760px) {
  .projects-view {
    padding: var(--space-5);
  }

  .projects-view__header {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-2);
  }

  .projects-view__empty {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
