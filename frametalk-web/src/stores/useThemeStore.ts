import { defineStore } from 'pinia'

import type { ThemePreference } from '@/types/settings'

const STORAGE_KEY = 'frametalk.theme'

function storedPreference(): ThemePreference {
  const value = localStorage.getItem(STORAGE_KEY)
  return value === 'light' || value === 'dark' || value === 'system'
    ? value
    : 'system'
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    preference: storedPreference() as ThemePreference,
    systemDark: false,
    mediaQuery: null as MediaQueryList | null,
  }),
  getters: {
    resolvedTheme(state): 'light' | 'dark' {
      if (state.preference === 'system') {
        return state.systemDark ? 'dark' : 'light'
      }
      return state.preference
    },
  },
  actions: {
    initialize() {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      this.mediaQuery = mediaQuery
      this.systemDark = mediaQuery.matches
      mediaQuery.addEventListener('change', this.syncSystemTheme)
      this.apply()
    },
    setPreference(preference: ThemePreference) {
      this.preference = preference
      localStorage.setItem(STORAGE_KEY, preference)
      this.apply()
    },
    syncSystemTheme(event: MediaQueryListEvent) {
      this.systemDark = event.matches
      this.apply()
    },
    apply() {
      document.documentElement.dataset.theme = this.resolvedTheme
    },
  },
})
