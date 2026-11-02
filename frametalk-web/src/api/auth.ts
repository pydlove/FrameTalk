import { get, postJson, postNoContent, putNoContent } from '@/api/http'
import type {
  AuthUser,
  ChangePasswordInput,
  LoginInput,
  RegisterInput,
} from '@/types/auth'

export const getCurrentUser = () => get<AuthUser>('/auth/me')

export const sendRegistrationCode = (email: string) =>
  postNoContent('/auth/email-codes', { email })

export const register = (input: RegisterInput) =>
  postJson<AuthUser>('/auth/register', input)

export const login = (input: LoginInput) =>
  postJson<AuthUser>('/auth/login', input)

export const logout = () => postNoContent('/auth/logout')

export const changePassword = (input: ChangePasswordInput) =>
  putNoContent('/auth/password', input)
