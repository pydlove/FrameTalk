import { defineStore } from 'pinia'

const COMPACT_SIDEBAR_KEY = 'frametalk.compact-sidebar'

export const usePreferencesStore = defineStore('preferences', {
  state: () => ({
    compactSidebar: localStorage.getItem(COMPACT_SIDEBAR_KEY) === 'true',
  }),
  actions: {
    setCompactSidebar(value: boolean) {
      this.compactSidebar = value
      localStorage.setItem(COMPACT_SIDEBAR_KEY, String(value))
    },
  },
})
