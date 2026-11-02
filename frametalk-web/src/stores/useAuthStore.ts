import { defineStore } from 'pinia'

import {
  changePassword as changePasswordRequest,
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  sendRegistrationCode,
} from '@/api/auth'
import type {
  AuthUser,
  ChangePasswordInput,
  RegisterInput,
} from '@/types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    authenticated: false,
    initialized: false,
    user: null as AuthUser | null,
  }),
  getters: {
    displayName: (state) => state.user?.displayName ?? '',
    email: (state) => state.user?.email ?? '',
    avatarLabel(): string {
      return this.displayName.trim().slice(0, 1).toUpperCase() || 'F'
    },
  },
  actions: {
    async initialize() {
      if (this.initialized) return this.authenticated
      try {
        const user = await getCurrentUser()
        this.applyUser(user)
      } catch {
        this.clearAuth()
      } finally {
        this.initialized = true
      }
      return this.authenticated
    },
    async login(email: string, password: string, rememberMe: boolean) {
      const user = await loginRequest({ email, password, rememberMe })
      this.applyUser(user)
      this.initialized = true
    },
    async register(input: RegisterInput) {
      return registerRequest(input)
    },
    async sendRegistrationCode(email: string) {
      await sendRegistrationCode(email)
    },
    async changePassword(input: ChangePasswordInput) {
      await changePasswordRequest(input)
    },
    async logout() {
      try {
        await logoutRequest()
      } finally {
        this.clearAuth()
      }
    },
    applyUser(user: AuthUser) {
      this.authenticated = true
      this.user = user
    },
    clearAuth() {
      this.authenticated = false
      this.user = null
    },
  },
})
