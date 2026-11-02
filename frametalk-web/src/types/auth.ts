export interface AuthUser {
  id: number
  email: string
  displayName: string
}

export interface LoginInput {
  email: string
  password: string
  rememberMe: boolean
}

export interface RegisterInput {
  email: string
  emailCode: string
  password: string
  confirmPassword: string
}

export interface ChangePasswordInput {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}
