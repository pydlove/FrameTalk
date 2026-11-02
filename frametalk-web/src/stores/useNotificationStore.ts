import { defineStore } from 'pinia'

type NotificationTone = 'success' | 'error' | 'info'

let timer: number | null = null

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    message: '',
    tone: 'info' as NotificationTone,
  }),
  actions: {
    show(message: string, tone: NotificationTone = 'success') {
      if (timer !== null) window.clearTimeout(timer)
      this.message = message
      this.tone = tone
      timer = window.setTimeout(() => {
        this.clear()
      }, 3600)
    },
    clear() {
      if (timer !== null) {
        window.clearTimeout(timer)
        timer = null
      }
      this.message = ''
    },
  },
})
