<script setup lang="ts">
import {
  ChevronDown,
  FolderKanban,
  LogOut,
  Menu,
  Settings,
  SlidersHorizontal,
  X,
} from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { PRODUCT_NAME } from '@/brand'
import BrandMark from '@/components/ui/BrandMark.vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { usePreferencesStore } from '@/stores/usePreferencesStore'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const preferences = usePreferencesStore()
const notifications = useNotificationStore()

const accountMenuOpen = ref(false)
const mobileNavOpen = ref(false)
const loggingOut = ref(false)
const profileArea = ref<HTMLElement | null>(null)

const pageTitle = computed(() => String(route.meta.title ?? PRODUCT_NAME))
const shellClasses = computed(() => ({
  'app-shell--compact': preferences.compactSidebar,
  'app-shell--mobile-nav': mobileNavOpen.value,
}))

function closeMenus() {
  accountMenuOpen.value = false
  mobileNavOpen.value = false
}

function goToProjects() {
  closeMenus()
  void router.push({ name: 'projects' })
}

function goToSettings(section = 'general') {
  closeMenus()
  void router.push({ name: 'settings', params: { section } })
}

async function logout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await auth.logout()
    closeMenus()
    await router.replace({ name: 'login' })
  } finally {
    loggingOut.value = false
  }
}

function handleOutsidePointer(event: PointerEvent) {
  const target = event.target
  if (
    target instanceof Node
    && profileArea.value
    && !profileArea.value.contains(target)
  ) {
    accountMenuOpen.value = false
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') closeMenus()
}

watch(
  () => route.fullPath,
  () => closeMenus(),
)

onMounted(() => {
  document.addEventListener('pointerdown', handleOutsidePointer)
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleOutsidePointer)
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="app-shell" :class="shellClasses">
    <header class="app-titlebar">
      <div class="app-titlebar__leading">
        <button
          class="titlebar-icon-button titlebar-menu"
          type="button"
          :aria-expanded="mobileNavOpen"
          aria-label="打开主导航"
          title="主导航"
          @click="mobileNavOpen = !mobileNavOpen"
        >
          <X v-if="mobileNavOpen" :size="17" />
          <Menu v-else :size="17" />
        </button>
        <div class="app-titlebar__brand">
          <BrandMark :size="24" />
          <span>{{ PRODUCT_NAME }}</span>
        </div>
      </div>

      <span class="app-titlebar__title">{{ pageTitle }}</span>

      <div class="app-titlebar__trailing">
        <span class="app-titlebar__local">本地工作区</span>
        <button
          class="titlebar-icon-button"
          type="button"
          aria-label="打开设置"
          title="设置"
          @click="goToSettings()"
        >
          <Settings :size="16" />
        </button>
      </div>
    </header>

    <div class="app-body">
      <aside class="app-sidebar" aria-label="主导航">
        <nav class="app-sidebar__nav">
          <section class="app-sidebar__group">
            <p>创作</p>
            <RouterLink
              class="app-sidebar__item"
              :class="{ 'app-sidebar__item--active': route.name === 'projects' }"
              :to="{ name: 'projects' }"
              title="项目"
            >
              <FolderKanban :size="16" aria-hidden="true" />
              <span>项目</span>
            </RouterLink>
          </section>

          <section class="app-sidebar__group">
            <p>系统</p>
            <button
              class="app-sidebar__item"
              type="button"
              :class="{ 'app-sidebar__item--active': route.name === 'settings' }"
              title="设置"
              @click="goToSettings()"
            >
              <SlidersHorizontal :size="16" aria-hidden="true" />
              <span>设置</span>
            </button>
          </section>
        </nav>

        <div ref="profileArea" class="profile-area">
          <button
            class="profile-trigger"
            type="button"
            :aria-expanded="accountMenuOpen"
            aria-label="账户菜单"
            title="账户菜单"
            @click="accountMenuOpen = !accountMenuOpen"
          >
            <span class="profile-avatar">{{ auth.avatarLabel }}</span>
            <span class="profile-copy">
              <strong>{{ auth.displayName }}</strong>
              <small>{{ auth.email }}</small>
            </span>
            <ChevronDown :size="14" class="profile-chevron" />
          </button>

          <div v-if="accountMenuOpen" class="profile-popover" role="menu">
            <header class="profile-popover__heading">
              <span class="profile-avatar profile-avatar--large">
                {{ auth.avatarLabel }}
              </span>
              <span>
                <strong>{{ auth.displayName }}</strong>
                <small>{{ auth.email }}</small>
              </span>
            </header>
            <button
              type="button"
              role="menuitem"
              @click="goToSettings('security')"
            >
              <Settings :size="15" />
              账户设置
            </button>
            <button
              class="profile-popover__danger"
              type="button"
              role="menuitem"
              :disabled="loggingOut"
              @click="logout"
            >
              <LogOut :size="15" />
              {{ loggingOut ? '正在退出…' : '退出登录' }}
            </button>
          </div>
        </div>
      </aside>

      <button
        v-if="mobileNavOpen"
        class="mobile-nav-backdrop"
        type="button"
        aria-label="关闭主导航"
        @click="mobileNavOpen = false"
      />

      <main class="app-content">
        <RouterView />
      </main>
    </div>

    <div
      v-if="notifications.message"
      class="app-status-bar"
      :class="`app-status-bar--${notifications.tone}`"
      role="status"
    >
      <span>{{ notifications.message }}</span>
      <button
        type="button"
        aria-label="关闭提示"
        title="关闭提示"
        @click="notifications.clear"
      >
        <X :size="14" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  background: var(--canvas);
}

.app-titlebar {
  position: relative;
  z-index: var(--z-titlebar);
  display: grid;
  height: var(--titlebar-height);
  flex: 0 0 var(--titlebar-height);
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 var(--space-4);
  background: var(--material);
  border-bottom: 1px solid var(--line-soft);
  backdrop-filter: blur(30px) saturate(1.5);
}

.app-titlebar__leading,
.app-titlebar__trailing {
  display: flex;
  min-width: 0;
  align-items: center;
}

.app-titlebar__leading {
  gap: var(--space-2);
}

.app-titlebar__trailing {
  justify-content: flex-end;
  gap: var(--space-2);
}

.app-titlebar__brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text);
  font-size: var(--font-body);
  font-weight: var(--weight-emphasis);
}

.app-titlebar__title {
  overflow: hidden;
  color: var(--text-muted);
  font-size: var(--font-caption);
  font-weight: var(--weight-emphasis);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-titlebar__local {
  color: var(--text-subtle);
  font-size: var(--font-caption);
}

.titlebar-icon-button {
  display: grid;
  width: var(--control-md);
  height: var(--control-md);
  padding: 0;
  color: var(--icon);
  background: transparent;
  border-radius: var(--radius-control);
  place-items: center;
}

.titlebar-icon-button:hover {
  color: var(--icon-hover);
  background: var(--hover-bg);
}

.titlebar-menu {
  display: none;
}

.app-body {
  display: flex;
  min-height: 0;
  flex: 1;
}

.app-sidebar {
  display: flex;
  width: var(--sidebar-width);
  flex: 0 0 var(--sidebar-width);
  flex-direction: column;
  padding: var(--space-3);
  background: var(--sidebar);
  border-right: 1px solid var(--line-soft);
  transition: width var(--duration-fast) var(--ease-apple);
  backdrop-filter: blur(30px) saturate(1.5);
}

.app-sidebar__nav {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: var(--space-5);
}

.app-sidebar__group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.app-sidebar__group > p {
  padding: 0 var(--space-2);
  margin: 0 0 var(--space-1);
  color: var(--text-subtle);
  font-size: var(--font-caption);
  font-weight: var(--weight-emphasis);
}

.app-sidebar__item {
  display: flex;
  width: 100%;
  height: 30px;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-2);
  color: var(--text-muted);
  font-size: var(--font-body);
  text-align: left;
  text-decoration: none;
  background: transparent;
  border-radius: var(--radius-control);
}

.app-sidebar__item:hover {
  color: var(--text);
  background: var(--hover-bg);
}

.app-sidebar__item--active {
  color: var(--text);
  background: var(--selection-bg);
}

.profile-area {
  position: relative;
  flex: 0 0 auto;
  padding-top: var(--space-3);
  margin-top: var(--space-3);
  border-top: 1px solid var(--line-soft);
}

.profile-trigger {
  display: flex;
  width: 100%;
  min-width: 0;
  height: 44px;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-2);
  color: var(--text);
  text-align: left;
  background: transparent;
  border-radius: var(--radius-control);
}

.profile-trigger:hover,
.profile-trigger[aria-expanded="true"] {
  background: var(--hover-bg);
}

.profile-avatar {
  display: grid;
  width: 27px;
  height: 27px;
  flex: 0 0 auto;
  color: var(--brand-paper);
  font-size: var(--font-caption);
  font-weight: var(--weight-strong);
  background: var(--brand-ink);
  border-radius: 8px;
  place-items: center;
}

.profile-avatar--large {
  width: 36px;
  height: 36px;
  border-radius: 10px;
}

.profile-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 1px;
}

.profile-copy strong,
.profile-popover__heading strong {
  overflow: hidden;
  color: var(--text);
  font-size: var(--font-caption);
  font-weight: var(--weight-emphasis);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-copy small,
.profile-popover__heading small {
  overflow: hidden;
  color: var(--text-subtle);
  font-size: var(--font-caption);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-chevron {
  flex: 0 0 auto;
  color: var(--text-subtle);
}

.profile-popover {
  position: absolute;
  bottom: calc(100% + var(--space-2));
  left: 0;
  z-index: var(--z-popover);
  width: var(--action-menu-width);
  padding: var(--space-1);
  background: var(--dropdown-bg, var(--canvas-raised));
  border: 1px solid var(--line);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-popover);
}

.profile-popover__heading {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  margin-bottom: var(--space-1);
  border-bottom: 1px solid var(--line-soft);
}

.profile-popover__heading > span:last-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.profile-popover > button {
  display: flex;
  width: 100%;
  height: 30px;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-2);
  color: var(--text);
  font-size: var(--font-body);
  text-align: left;
  background: transparent;
  border-radius: var(--radius-control);
}

.profile-popover > button:hover {
  background: var(--hover-bg);
}

.profile-popover > button.profile-popover__danger {
  color: var(--system-red);
}

.profile-popover > button.profile-popover__danger:hover {
  background: color-mix(in srgb, var(--system-red) 8%, transparent);
}

.app-content {
  min-width: 0;
  flex: 1;
  overflow: auto;
}

.app-shell--compact .app-sidebar {
  width: 72px;
  flex-basis: 72px;
}

.app-shell--compact .app-sidebar__group > p,
.app-shell--compact .app-sidebar__item span,
.app-shell--compact .profile-copy,
.app-shell--compact .profile-chevron {
  display: none;
}

.app-shell--compact .app-sidebar__item,
.app-shell--compact .profile-trigger {
  justify-content: center;
  padding: 0;
}

.app-shell--compact .profile-popover {
  left: calc(100% + var(--space-2));
  bottom: 0;
}

.mobile-nav-backdrop {
  display: none;
}

.app-status-bar {
  display: flex;
  min-height: 36px;
  flex: 0 0 36px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 0 var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-body);
  background: var(--canvas-raised);
  border-top: 1px solid var(--line-soft);
  box-shadow: 0 -1px 3px rgb(0 0 0 / 4%);
}

.app-status-bar--success {
  color: #24713a;
}

.app-status-bar--error {
  color: var(--system-red);
}

.app-status-bar button {
  display: grid;
  width: 24px;
  height: 24px;
  padding: 0;
  color: var(--text-subtle);
  background: transparent;
  border-radius: var(--radius-control);
  place-items: center;
}

.app-status-bar button:hover {
  color: var(--text);
  background: var(--hover-bg);
}

html[data-theme="dark"] .app-status-bar--success {
  color: #7ee787;
}

@media (max-width: 1023px) {
  .titlebar-menu {
    display: grid;
  }

  .app-titlebar {
    padding: 0 var(--space-3);
  }

  .app-titlebar__local {
    display: none;
  }

  .app-sidebar {
    position: fixed;
    top: var(--titlebar-height);
    bottom: 0;
    left: 0;
    z-index: var(--z-overlay);
    width: min(82vw, var(--sidebar-width));
    flex-basis: auto;
    transform: translateX(-100%);
    transition: transform var(--duration-window) var(--ease-apple);
  }

  .app-shell--mobile-nav .app-sidebar {
    transform: translateX(0);
  }

  .mobile-nav-backdrop {
    position: fixed;
    inset: var(--titlebar-height) 0 0;
    z-index: calc(var(--z-overlay) - 1);
    display: block;
    padding: 0;
    background: rgb(0 0 0 / 24%);
  }

  .app-shell--compact .app-sidebar {
    width: min(82vw, var(--sidebar-width));
  }

  .app-shell--compact .app-sidebar__group > p,
  .app-shell--compact .app-sidebar__item span,
  .app-shell--compact .profile-copy,
  .app-shell--compact .profile-chevron {
    display: initial;
  }

  .app-shell--compact .app-sidebar__group > p {
    display: block;
  }

  .app-shell--compact .app-sidebar__item,
  .app-shell--compact .profile-trigger {
    justify-content: flex-start;
    padding: 0 var(--space-2);
  }

  .app-shell--compact .profile-popover {
    left: 0;
    bottom: calc(100% + var(--space-2));
  }
}

@media (max-width: 700px) {
  .app-titlebar__brand span {
    display: none;
  }

  .profile-popover {
    width: min(78vw, var(--action-menu-width));
  }
}
</style>
