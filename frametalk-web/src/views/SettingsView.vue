<script setup lang="ts">
import {
  Info,
  LogOut,
  Palette,
  Settings,
  ShieldCheck,
} from '@lucide/vue'
import type { Component } from 'vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AccountSecuritySettings from '@/components/business/settings/AccountSecuritySettings.vue'
import BrandMark from '@/components/ui/BrandMark.vue'
import SettingsCard from '@/components/ui/SettingsCard.vue'
import SettingsRow from '@/components/ui/SettingsRow.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiSearchInput from '@/components/ui/UiSearchInput.vue'
import UiSegmentedControl from '@/components/ui/UiSegmentedControl.vue'
import {
  PRODUCT_ENGLISH_NAME,
  PRODUCT_NAME,
  PRODUCT_TAGLINE,
  PRODUCT_VERSION,
} from '@/brand'
import { useAuthStore } from '@/stores/useAuthStore'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { usePreferencesStore } from '@/stores/usePreferencesStore'
import { useThemeStore } from '@/stores/useThemeStore'
import type { SettingsSection, ThemePreference } from '@/types/settings'

interface SettingsNavigationItem {
  id: SettingsSection
  label: string
  description: string
  icon: Component
}

interface SettingsNavigationGroup {
  id: string
  label: string
  items: SettingsNavigationItem[]
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const preferences = usePreferencesStore()
const notifications = useNotificationStore()
const searchQuery = ref('')
const loggingOut = ref(false)

const sections: SettingsSection[] = [
  'general',
  'security',
  'appearance',
  'about',
]

const navigation: SettingsNavigationGroup[] = [
  {
    id: 'personal',
    label: '个人',
    items: [
      {
        id: 'general',
        label: '通用',
        description: '登录后的默认行为和侧边栏密度',
        icon: Settings,
      },
      {
        id: 'security',
        label: '账号安全',
        description: '登录密码与当前会话',
        icon: ShieldCheck,
      },
      {
        id: 'appearance',
        label: '外观',
        description: '浅色、深色与跟随系统',
        icon: Palette,
      },
    ],
  },
  {
    id: 'system',
    label: '系统',
    items: [
      {
        id: 'about',
        label: '关于',
        description: '产品信息和版本',
        icon: Info,
      },
    ],
  },
]

const sectionMeta: Record<
  SettingsSection,
  { title: string; description: string }
> = {
  general: {
    title: '通用',
    description: '设置登录后的默认行为和侧边栏显示方式。',
  },
  security: {
    title: '账号安全',
    description: '修改登录密码并管理当前浏览器会话。',
  },
  appearance: {
    title: '外观',
    description: '选择 FrameTalk 在当前设备上使用的颜色模式。',
  },
  about: {
    title: '关于',
    description: '查看产品版本、品牌信息和本地运行说明。',
  },
}

const themeItems = [
  { value: 'system', label: '跟随系统' },
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
]

const activeSection = computed<SettingsSection>(() => {
  const value = String(route.params.section ?? 'general') as SettingsSection
  return sections.includes(value) ? value : 'general'
})

const activeMeta = computed(() => sectionMeta[activeSection.value])

const filteredNavigation = computed(() => {
  const keyword = searchQuery.value.trim().toLocaleLowerCase('zh-CN')
  if (!keyword) return navigation
  return navigation
    .map((group) => ({
      ...group,
      items: group.items.filter((item) =>
        `${group.label} ${item.label} ${item.description}`
          .toLocaleLowerCase('zh-CN')
          .includes(keyword),
      ),
    }))
    .filter((group) => group.items.length > 0)
})

function selectSection(section: SettingsSection) {
  void router.push({ name: 'settings', params: { section } })
}

function setTheme(value: string) {
  theme.setPreference(value as ThemePreference)
}

function setCompactSidebar(event: Event) {
  const input = event.target
  if (!(input instanceof HTMLInputElement)) return
  preferences.setCompactSidebar(input.checked)
}

async function logout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await auth.logout()
    await router.replace({ name: 'login' })
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <section class="settings-page">
    <aside class="settings-sidebar" aria-label="设置分类">
      <header class="settings-sidebar__heading">
        <h1>设置</h1>
      </header>
      <UiSearchInput
        v-model="searchQuery"
        label="搜索设置"
        placeholder="搜索设置"
        clearable
      />
      <nav class="settings-sidebar__groups">
        <section
          v-for="group in filteredNavigation"
          :key="group.id"
          class="settings-sidebar__group"
        >
          <h2>{{ group.label }}</h2>
          <button
            v-for="item in group.items"
            :key="item.id"
            type="button"
            :aria-current="activeSection === item.id ? 'page' : undefined"
            :class="{
              'settings-sidebar__item--active': activeSection === item.id,
            }"
            @click="selectSection(item.id)"
          >
            <component :is="item.icon" :size="16" aria-hidden="true" />
            <span>
              <strong>{{ item.label }}</strong>
            </span>
          </button>
        </section>
        <p
          v-if="filteredNavigation.length === 0"
          class="settings-sidebar__empty"
        >
          没有匹配的设置分类
        </p>
      </nav>
    </aside>

    <main class="settings-content">
      <header class="settings-content__heading">
        <h2>{{ activeMeta.title }}</h2>
        <p>{{ activeMeta.description }}</p>
      </header>

      <div v-show="activeSection === 'general'" class="settings-stack">
        <SettingsCard
          title="工作台"
          description="这些偏好保存在当前浏览器中。"
        >
          <SettingsRow
            as="label"
            title="紧凑侧边栏"
            description="压缩导航宽度，同时保留图标和入口名称。"
          >
            <input
              :checked="preferences.compactSidebar"
              data-settings-switch
              type="checkbox"
              @change="setCompactSidebar"
            />
          </SettingsRow>
          <SettingsRow
            title="默认入口"
            description="登录后进入项目管理页。"
          >
            <span class="settings-value">项目</span>
          </SettingsRow>
          <SettingsRow
            title="登录状态"
            description="是否长期保持登录由登录页的“保持登录”控制。"
          >
            <span class="settings-value">本机 Cookie</span>
          </SettingsRow>
        </SettingsCard>
      </div>

      <div v-show="activeSection === 'appearance'" class="settings-stack">
        <SettingsCard
          title="颜色模式"
          description="外观切换会立即生效并保存在当前浏览器。"
        >
          <SettingsRow
            title="颜色模式"
            description="跟随系统时自动响应 macOS 的浅色和深色设置。"
          >
            <UiSegmentedControl
              :model-value="theme.preference"
              :items="themeItems"
              aria-label="颜色模式"
              @update:model-value="setTheme"
            />
          </SettingsRow>
          <SettingsRow
            title="品牌色"
            description="FrameTalk 使用墨绿、暖纸和珊瑚橙作为品牌识别。"
          >
            <span class="brand-swatches" aria-label="品牌颜色">
              <i class="brand-swatches__ink" />
              <i class="brand-swatches__paper" />
              <i class="brand-swatches__coral" />
            </span>
          </SettingsRow>
        </SettingsCard>
      </div>

      <div v-show="activeSection === 'security'" class="settings-stack">
        <AccountSecuritySettings
          :email="auth.email"
          @notify="notifications.show"
        />
        <SettingsCard
          title="当前会话"
          description="退出后需要重新输入邮箱和密码。"
        >
          <SettingsRow
            title="浏览器登录会话"
            description="当前设备通过 HttpOnly Cookie 保持登录。"
          >
            <UiButton
              variant="danger-ghost"
              :loading="loggingOut"
              :disabled="loggingOut"
              @click="logout"
            >
              <LogOut v-if="!loggingOut" :size="14" />
              退出登录
            </UiButton>
          </SettingsRow>
        </SettingsCard>
      </div>

      <div v-show="activeSection === 'about'" class="settings-stack">
        <SettingsCard title="FrameTalk">
          <div class="about-brand">
            <BrandMark :size="64" :decorative="false" />
            <div class="about-brand__copy">
              <h3>{{ PRODUCT_NAME }}</h3>
              <p>{{ PRODUCT_TAGLINE }}</p>
              <span>{{ PRODUCT_ENGLISH_NAME }} {{ PRODUCT_VERSION }}</span>
            </div>
          </div>
          <SettingsRow
            title="产品定位"
            description="面向真人出镜创作者的自媒体成片工作台。"
          >
            <span class="settings-value">本地优先</span>
          </SettingsRow>
          <SettingsRow
            title="数据存储"
            description="账号和业务数据保存在本机 MySQL，媒体文件保存在本机磁盘。"
          >
            <span class="settings-value">MySQL 8</span>
          </SettingsRow>
        </SettingsCard>
      </div>
    </main>
  </section>
</template>

<style scoped>
.settings-page {
  display: grid;
  min-height: 100%;
  grid-template-columns: 232px minmax(0, 1fr);
}

.settings-sidebar {
  padding: var(--space-6) var(--space-4);
  background: var(--surface-secondary);
  border-right: 1px solid var(--line-soft);
}

.settings-sidebar__heading h1 {
  margin: 0 0 var(--space-5);
  color: var(--text);
  font-size: var(--font-metric);
  font-weight: var(--weight-emphasis);
}

.settings-sidebar__groups {
  display: flex;
  margin-top: var(--space-5);
  flex-direction: column;
  gap: var(--space-5);
}

.settings-sidebar__group h2 {
  padding: 0 var(--space-2);
  margin: 0 0 var(--space-2);
  color: var(--text-subtle);
  font-size: var(--font-caption);
  font-weight: var(--weight-emphasis);
}

.settings-sidebar__group button {
  display: flex;
  width: 100%;
  height: 32px;
  align-items: center;
  gap: var(--space-2);
  padding: 0 var(--space-2);
  color: var(--text-muted);
  font-size: var(--font-body);
  text-align: left;
  background: transparent;
  border-radius: var(--radius-control);
}

.settings-sidebar__group button:hover {
  color: var(--text);
  background: var(--hover-bg);
}

.settings-sidebar__group button.settings-sidebar__item--active {
  color: var(--text);
  background: var(--selection-bg);
}

.settings-sidebar__group strong {
  font-weight: var(--weight-regular);
}

.settings-sidebar__empty {
  margin: 0;
  padding: var(--space-3) var(--space-2);
  color: var(--text-subtle);
  font-size: var(--font-caption);
}

.settings-content {
  min-width: 0;
  padding: var(--space-8);
}

.settings-content__heading {
  margin-bottom: var(--space-6);
}

.settings-content__heading h2 {
  margin: 0;
  color: var(--text);
  font-size: var(--font-display);
  font-weight: var(--weight-emphasis);
  line-height: 36px;
}

.settings-content__heading p {
  margin: var(--space-2) 0 0;
  color: var(--text-muted);
  font-size: var(--font-body);
}

.settings-stack {
  display: flex;
  width: min(100%, 820px);
  flex-direction: column;
  gap: var(--space-6);
}

.settings-value {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: var(--font-caption);
}

.brand-swatches {
  display: flex;
  gap: var(--space-2);
}

.brand-swatches i {
  display: block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgb(0 0 0 / 10%);
}

.brand-swatches__ink {
  background: var(--brand-ink);
}

.brand-swatches__paper {
  background: var(--brand-paper);
}

.brand-swatches__coral {
  background: var(--brand-coral);
}

.about-brand {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-6);
}

.about-brand__copy h3 {
  margin: 0;
  color: var(--text);
  font-size: 20px;
  font-weight: var(--weight-emphasis);
}

.about-brand__copy p {
  margin: var(--space-1) 0;
  color: var(--text-muted);
  font-size: var(--font-body);
}

.about-brand__copy span {
  color: var(--text-subtle);
  font-family: var(--font-mono);
  font-size: var(--font-caption);
}

@media (max-width: 900px) {
  .settings-page {
    grid-template-columns: 1fr;
  }

  .settings-sidebar {
    padding: var(--space-4);
    border-right: 0;
    border-bottom: 1px solid var(--line-soft);
  }

  .settings-sidebar__heading h1 {
    margin-bottom: var(--space-4);
  }

  .settings-sidebar__groups {
    margin-top: var(--space-4);
    flex-direction: row;
    gap: var(--space-4);
    overflow-x: auto;
  }

  .settings-sidebar__group {
    min-width: max-content;
  }

  .settings-sidebar__group button {
    display: inline-flex;
    width: auto;
  }

  .settings-content {
    padding: var(--space-6) var(--space-5);
  }
}

@media (max-width: 560px) {
  .settings-content__heading h2 {
    font-size: var(--font-metric);
    line-height: 28px;
  }

  .settings-sidebar__group h2 {
    display: none;
  }

  .settings-sidebar__groups {
    gap: var(--space-2);
  }

  .settings-sidebar__group {
    display: contents;
  }
}
</style>
