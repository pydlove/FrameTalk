<script setup lang="ts">
import {
  ArrowRight,
  Eye,
  EyeOff,
  LockKeyhole,
  Mail,
  ShieldCheck,
} from '@lucide/vue'
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue'
import { useRouter } from 'vue-router'

import { ApiError } from '@/api/http'
import loginImage from '@/assets/auth/frametalk-slate.webp'
import {
  PRODUCT_ENGLISH_NAME,
  PRODUCT_NAME,
  PRODUCT_TAGLINE,
} from '@/brand'
import BrandMark from '@/components/ui/BrandMark.vue'
import { useAuthStore } from '@/stores/useAuthStore'

type AuthMode = 'login' | 'register'

const props = withDefaults(
  defineProps<{
    initialMode?: AuthMode
  }>(),
  {
    initialMode: 'login',
  },
)

const router = useRouter()
const auth = useAuthStore()
const loginImageUrl = `url("${loginImage}")`

const authMode = ref<AuthMode>(props.initialMode)
const email = ref('')
const emailCode = ref('')
const password = ref('')
const confirmPassword = ref('')
const remember = ref(true)
const passwordVisible = ref(false)
const confirmPasswordVisible = ref(false)
const submitting = ref(false)
const sendingCode = ref(false)
const resendSeconds = ref(0)
const errorMessage = ref('')
const noticeMessage = ref('')
const emailInput = ref<HTMLInputElement | null>(null)
const passwordInput = ref<HTMLInputElement | null>(null)

let countdownTimer: number | null = null

const isRegisterMode = computed(() => authMode.value === 'register')
const codeButtonLabel = computed(() => {
  if (resendSeconds.value > 0) return `${resendSeconds.value}s 后重发`
  return sendingCode.value ? '发送中…' : '获取验证码'
})

function normalizeEmail(value: string) {
  return value.trim().toLocaleLowerCase('en-US')
}

function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

function errorText(error: unknown) {
  if (error instanceof ApiError) return error.message
  if (error instanceof TypeError) {
    return '无法连接账号服务，请确认 Java 后端已启动'
  }
  return '账号操作失败，请稍后重试'
}

function stopCountdown() {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
}

function startCountdown() {
  stopCountdown()
  resendSeconds.value = 60
  countdownTimer = window.setInterval(() => {
    resendSeconds.value -= 1
    if (resendSeconds.value <= 0) stopCountdown()
  }, 1000)
}

async function sendCode() {
  const normalizedEmail = normalizeEmail(email.value)
  errorMessage.value = ''
  noticeMessage.value = ''

  if (!isValidEmail(normalizedEmail)) {
    errorMessage.value = '请输入有效的邮箱地址'
    await nextTick()
    emailInput.value?.focus()
    return
  }

  sendingCode.value = true
  try {
    await auth.sendRegistrationCode(normalizedEmail)
    startCountdown()
    noticeMessage.value =
      `验证码已发送至 ${normalizedEmail}，5 分钟内有效。本地开发模式请查看 Java 后端日志。`
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    sendingCode.value = false
  }
}

async function handleSubmit() {
  const normalizedEmail = normalizeEmail(email.value)
  errorMessage.value = ''
  noticeMessage.value = ''

  if (!normalizedEmail) {
    errorMessage.value = '请输入邮箱'
    await nextTick()
    emailInput.value?.focus()
    return
  }
  if (!isValidEmail(normalizedEmail)) {
    errorMessage.value = '请输入有效的邮箱地址'
    await nextTick()
    emailInput.value?.focus()
    return
  }
  if (!password.value) {
    errorMessage.value = '请输入密码'
    return
  }
  if (Array.from(password.value).length < 6) {
    errorMessage.value = '密码至少需要 6 个字符'
    return
  }
  if (isRegisterMode.value && !/^\d{6}$/.test(emailCode.value.trim())) {
    errorMessage.value = '请输入 6 位邮箱验证码'
    return
  }
  if (isRegisterMode.value && password.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }

  submitting.value = true
  try {
    if (isRegisterMode.value) {
      await auth.register({
        email: normalizedEmail,
        emailCode: emailCode.value.trim(),
        password: password.value,
        confirmPassword: confirmPassword.value,
      })
      stopCountdown()
      resendSeconds.value = 0
      authMode.value = 'login'
      emailCode.value = ''
      password.value = ''
      confirmPassword.value = ''
      passwordVisible.value = false
      confirmPasswordVisible.value = false
      noticeMessage.value = '账号已创建，请使用邮箱和密码登录。'
      await nextTick()
      passwordInput.value?.focus()
      return
    }

    await auth.login(normalizedEmail, password.value, remember.value)
    await router.push({ name: 'projects' })
  } catch (error) {
    errorMessage.value = errorText(error)
  } finally {
    submitting.value = false
  }
}

async function switchMode(mode: AuthMode) {
  stopCountdown()
  authMode.value = mode
  emailCode.value = ''
  password.value = ''
  confirmPassword.value = ''
  passwordVisible.value = false
  confirmPasswordVisible.value = false
  resendSeconds.value = 0
  errorMessage.value = ''
  noticeMessage.value = ''
  await nextTick()
  emailInput.value?.focus()
}

onMounted(() => {
  emailInput.value?.focus()
})

onBeforeUnmount(stopCountdown)
</script>

<template>
  <main class="login-stage">
    <section class="login-window" aria-labelledby="login-title">
      <header class="auth-window-bar">
        <div class="auth-window-bar__brand">
          <BrandMark :size="22" />
          <strong>{{ PRODUCT_NAME }}</strong>
        </div>
        <span>{{ PRODUCT_ENGLISH_NAME }} · 本地创作工作台</span>
      </header>

      <div class="login-window__body">
        <aside class="login-story">
          <div class="story-brand">
            <BrandMark :size="36" />
            <div>
              <p class="story-brand__name">{{ PRODUCT_NAME }}</p>
              <p class="story-brand__meta">{{ PRODUCT_ENGLISH_NAME }}</p>
            </div>
          </div>

          <div class="story-copy">
            <p class="story-kicker">自媒体成片工作台</p>
            <h1>让每一帧，<br />都有话可说。</h1>
            <p class="story-intro">
              从脚本、真人口播到素材与成片，把自媒体创作留在同一张工作台。
            </p>
          </div>

          <div class="story-features">
            <div>
              <strong>脚本与分镜</strong>
              <span>先把表达想清楚</span>
            </div>
            <div>
              <strong>真人出镜剪辑</strong>
              <span>围绕口播完成粗剪</span>
            </div>
            <div>
              <strong>本机素材</strong>
              <span>文件留在自己电脑</span>
            </div>
          </div>
        </aside>

        <section class="login-panel">
          <div class="login-panel__inner">
            <div class="login-heading">
              <div class="login-heading__top">
                <div
                  class="auth-mode-tabs"
                  role="tablist"
                  aria-label="账号操作"
                >
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="authMode === 'login'"
                    :class="{ 'auth-mode-tabs__active': authMode === 'login' }"
                    @click="switchMode('login')"
                  >
                    登录
                  </button>
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="authMode === 'register'"
                    :class="{
                      'auth-mode-tabs__active': authMode === 'register',
                    }"
                    @click="switchMode('register')"
                  >
                    注册
                  </button>
                </div>
              </div>
              <h2 id="login-title">
                {{ isRegisterMode ? '创建账号' : '欢迎回来' }}
              </h2>
              <p>
                {{
                  isRegisterMode
                    ? `使用邮箱验证码创建 ${PRODUCT_NAME}账号。`
                    : '登录后继续整理今天的创作。'
                }}
              </p>
            </div>

            <form
              class="login-form"
              :class="{ 'login-form--register': isRegisterMode }"
              novalidate
              @submit.prevent="handleSubmit"
            >
              <label class="form-field">
                <span class="form-field__label">邮箱</span>
                <span class="form-field__control">
                  <Mail :size="16" aria-hidden="true" />
                  <input
                    ref="emailInput"
                    v-model="email"
                    type="email"
                    name="email"
                    autocomplete="email"
                    inputmode="email"
                    placeholder="you@example.com"
                    spellcheck="false"
                    aria-label="邮箱"
                  />
                </span>
              </label>

              <label v-if="isRegisterMode" class="form-field">
                <span class="form-field__label">邮箱验证码</span>
                <span class="form-field__control form-field__control--code">
                  <ShieldCheck :size="16" aria-hidden="true" />
                  <input
                    v-model="emailCode"
                    type="text"
                    name="email-code"
                    inputmode="numeric"
                    maxlength="6"
                    autocomplete="one-time-code"
                    aria-label="邮箱验证码"
                    placeholder="6 位数字"
                  />
                  <button
                    class="code-send"
                    type="button"
                    :disabled="sendingCode || resendSeconds > 0 || submitting"
                    @click="sendCode"
                  >
                    {{ codeButtonLabel }}
                  </button>
                </span>
              </label>

              <label class="form-field">
                <span class="form-field__label">密码</span>
                <span class="form-field__control">
                  <LockKeyhole :size="16" aria-hidden="true" />
                  <input
                    ref="passwordInput"
                    v-model="password"
                    :type="passwordVisible ? 'text' : 'password'"
                    name="password"
                    minlength="6"
                    maxlength="128"
                    :autocomplete="
                      isRegisterMode ? 'new-password' : 'current-password'
                    "
                    aria-label="密码"
                    :placeholder="
                      isRegisterMode ? '至少 6 个字符' : '输入密码'
                    "
                  />
                  <button
                    class="password-toggle"
                    type="button"
                    :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                    @click="passwordVisible = !passwordVisible"
                  >
                    <EyeOff v-if="passwordVisible" :size="16" />
                    <Eye v-else :size="16" />
                  </button>
                </span>
              </label>

              <label v-if="isRegisterMode" class="form-field">
                <span class="form-field__label">确认密码</span>
                <span class="form-field__control">
                  <LockKeyhole :size="16" aria-hidden="true" />
                  <input
                    v-model="confirmPassword"
                    :type="confirmPasswordVisible ? 'text' : 'password'"
                    name="confirm-password"
                    autocomplete="new-password"
                    aria-label="确认密码"
                    placeholder="再次输入密码"
                  />
                  <button
                    class="password-toggle"
                    type="button"
                    :aria-label="
                      confirmPasswordVisible
                        ? '隐藏确认密码'
                        : '显示确认密码'
                    "
                    @click="
                      confirmPasswordVisible = !confirmPasswordVisible
                    "
                  >
                    <EyeOff v-if="confirmPasswordVisible" :size="16" />
                    <Eye v-else :size="16" />
                  </button>
                </span>
              </label>

              <div v-if="!isRegisterMode" class="form-options">
                <label class="remember-control">
                  <input v-model="remember" type="checkbox" />
                  <span class="remember-control__box"><span /></span>
                  <span>保持登录</span>
                </label>
                <span class="secure-hint">安全会话</span>
              </div>

              <p v-if="isRegisterMode" class="local-account-note">
                验证码 5 分钟内有效，同一邮箱 60 秒后可重发。
              </p>

              <p v-if="noticeMessage" class="form-notice" role="status">
                {{ noticeMessage }}
              </p>
              <p v-if="errorMessage" class="form-error" role="alert">
                {{ errorMessage }}
              </p>

              <button
                class="login-submit"
                type="submit"
                :disabled="submitting"
              >
                <span v-if="submitting">
                  {{ isRegisterMode ? '正在创建…' : '正在进入…' }}
                </span>
                <span v-else>
                  {{ isRegisterMode ? '创建账号' : '进入工作台' }}
                </span>
                <ArrowRight :size="17" :stroke-width="2" />
              </button>
            </form>
          </div>

          <p class="login-footnote">
            {{ PRODUCT_TAGLINE }} · 数据保存在本机 MySQL
          </p>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-stage {
  display: grid;
  width: 100%;
  min-height: 100%;
  overflow: auto;
  padding: 42px;
  place-items: center;
  background:
    linear-gradient(128deg, rgb(23 37 31 / 6%), transparent 48%),
    linear-gradient(315deg, rgb(255 91 53 / 5%), transparent 42%),
    #eef0eb;
}

.login-window {
  display: flex;
  width: min(100%, 1120px);
  min-height: 700px;
  overflow: hidden;
  flex-direction: column;
  background: var(--canvas-raised);
  border: 1px solid var(--line);
  border-radius: 15px;
  box-shadow:
    0 30px 80px rgb(23 37 31 / 16%),
    0 4px 18px rgb(23 37 31 / 9%),
    inset 0 0 0 1px rgb(255 255 255 / 65%);
  animation: login-window-in 420ms cubic-bezier(0.2, 0.75, 0.3, 1) both;
}

.auth-window-bar {
  display: flex;
  height: 42px;
  flex: 0 0 42px;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-caption);
  background: var(--material);
  border-bottom: 1px solid var(--line-soft);
  backdrop-filter: blur(24px) saturate(1.4);
}

.auth-window-bar__brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text);
}

.auth-window-bar__brand strong {
  font-size: var(--font-body);
  font-weight: var(--weight-emphasis);
}

.login-window__body {
  display: grid;
  min-height: 658px;
  flex: 1;
  grid-template-columns: minmax(0, 1.06fr) minmax(390px, 0.94fr);
}

.login-story {
  position: relative;
  display: flex;
  overflow: hidden;
  flex-direction: column;
  justify-content: space-between;
  padding: 42px 44px 34px;
  background:
    linear-gradient(
      180deg,
      rgb(7 15 11 / 30%) 0%,
      rgb(7 15 11 / 14%) 38%,
      rgb(7 15 11 / 84%) 100%
    ),
    v-bind(loginImageUrl) center / cover no-repeat,
    #07111c;
  border-right: 1px solid rgb(255 255 255 / 7%);
}

.story-brand,
.story-copy,
.story-features {
  position: relative;
  z-index: 1;
}

.story-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.story-brand p {
  margin: 0;
}

.story-brand__name {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.story-brand__meta {
  margin-top: 2px !important;
  color: rgb(255 255 255 / 56%);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.story-copy {
  max-width: 390px;
  margin-top: auto;
  padding-top: 48px;
}

.story-kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 20px;
  color: rgb(255 255 255 / 65%);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.13em;
}

.story-kicker::before {
  width: 28px;
  height: 1px;
  content: "";
  background: rgb(255 255 255 / 38%);
}

.story-copy h1 {
  margin: 0;
  color: #fff;
  font-size: clamp(38px, 4.2vw, 52px);
  font-weight: 630;
  line-height: 1.16;
  white-space: nowrap;
  text-shadow: 0 2px 24px rgb(0 0 0 / 20%);
}

.story-intro {
  max-width: 340px;
  margin: 23px 0 0;
  color: rgb(255 255 255 / 70%);
  font-size: 14px;
  line-height: 1.75;
}

.story-features {
  display: grid;
  padding-top: 20px;
  margin-top: 34px;
  border-top: 1px solid rgb(255 255 255 / 16%);
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.story-features > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.story-features strong {
  color: rgb(255 255 255 / 90%);
  font-size: 11px;
  font-weight: 620;
}

.story-features span {
  overflow: hidden;
  color: rgb(255 255 255 / 48%);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 58px 28px;
  background:
    linear-gradient(145deg, rgb(255 91 53 / 3%), transparent 42%),
    var(--canvas-raised);
}

.login-panel__inner {
  width: min(100%, 352px);
  margin: auto;
}

.login-heading__top {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 16px;
  margin-bottom: 12px;
}

.auth-mode-tabs {
  display: flex;
  flex: 0 0 auto;
  gap: 2px;
  padding: 2px;
  margin-top: -4px;
  background: var(--segmented-track);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-control);
}

.auth-mode-tabs button {
  height: 24px;
  padding: 0 9px;
  color: var(--text-subtle);
  font-size: 11px;
  font-weight: 550;
  background: transparent;
  border-radius: 4px;
}

.auth-mode-tabs .auth-mode-tabs__active {
  color: var(--text);
  font-weight: 620;
  background: var(--canvas-raised);
  box-shadow: var(--shadow-control);
}

.login-heading h2 {
  margin: 0;
  color: var(--text);
  font-size: 32px;
  font-weight: 630;
  line-height: 1.2;
}

.login-heading > p:last-child {
  margin: 10px 0 0;
  color: var(--text-muted);
  font-size: var(--font-body);
}

.login-form {
  display: flex;
  margin-top: 34px;
  flex-direction: column;
  gap: 18px;
}

.login-form--register {
  margin-top: 26px;
  gap: 14px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-field__label {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
}

.form-field__control {
  display: flex;
  height: 43px;
  align-items: center;
  gap: 10px;
  padding: 0 13px;
  color: var(--text-subtle);
  background: var(--field-bg);
  border: 1px solid var(--field-border);
  border-radius: 8px;
  box-shadow: var(--control-inset-shadow);
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease,
    background 150ms ease;
}

.form-field__control:focus-within {
  border-color: var(--system-blue);
  box-shadow: var(--focus-ring);
}

.form-field__control input {
  width: 100%;
  min-width: 0;
  height: 100%;
  padding: 0;
  color: var(--text);
  font-size: var(--font-body);
  background: transparent;
  border: 0;
}

.form-field__control input:focus-visible {
  outline: none;
}

.form-field__control--code {
  padding-right: 5px;
}

.code-send {
  height: 29px;
  padding: 0 9px;
  flex: 0 0 auto;
  color: var(--system-blue);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  background: var(--accent-soft);
  border-radius: 5px;
}

.code-send:hover:not(:disabled) {
  color: #fff;
  background: var(--system-blue);
}

.code-send:disabled {
  color: var(--text-subtle);
  background: var(--hover-bg);
}

.password-toggle {
  display: grid;
  width: 28px;
  height: 28px;
  padding: 0;
  flex: 0 0 auto;
  color: var(--text-subtle);
  background: transparent;
  border-radius: var(--radius-control);
  place-items: center;
}

.password-toggle:hover {
  color: var(--text);
  background: var(--hover-bg);
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: -2px;
}

.remember-control {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 11px;
  user-select: none;
}

.remember-control input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.remember-control__box {
  display: grid;
  width: 15px;
  height: 15px;
  background: var(--control-bg);
  border: 1px solid var(--control-border);
  border-radius: 4px;
  place-items: center;
}

.remember-control__box span {
  width: 5px;
  height: 8px;
  border-right: 1.5px solid white;
  border-bottom: 1.5px solid white;
  opacity: 0;
  transform: translateY(-1px) rotate(43deg);
}

.remember-control input:checked + .remember-control__box {
  background: var(--system-blue);
  border-color: var(--system-blue);
}

.remember-control input:checked + .remember-control__box span {
  opacity: 1;
}

.remember-control input:focus-visible + .remember-control__box {
  box-shadow: var(--focus-ring);
}

.secure-hint {
  color: var(--text-subtle);
  font-size: 11px;
}

.form-error {
  margin: -5px 0;
  color: var(--system-red);
  font-size: 11px;
}

.form-notice {
  padding: 8px 10px;
  margin: -5px 0;
  color: #1a7f37;
  font-size: 11px;
  line-height: 1.5;
  background: rgb(26 127 55 / 7%);
  border-radius: 6px;
}

.local-account-note {
  margin: -6px 0 -2px;
  color: var(--text-subtle);
  font-size: 11px;
  line-height: 1.55;
}

.login-submit {
  display: flex;
  width: 100%;
  height: 44px;
  align-items: center;
  justify-content: space-between;
  margin-top: 3px;
  padding: 0 16px 0 18px;
  color: var(--brand-paper);
  font-size: 12px;
  font-weight: 620;
  background: var(--brand-ink);
  border-radius: 8px;
  box-shadow:
    0 1px 2px rgb(23 37 31 / 24%),
    inset 0 1px 0 rgb(255 255 255 / 8%);
  transition:
    background 150ms ease,
    transform 150ms ease,
    box-shadow 150ms ease;
}

.login-submit:hover:not(:disabled) {
  background: #253a31;
  box-shadow:
    0 5px 18px rgb(23 37 31 / 22%),
    inset 0 1px 0 rgb(255 255 255 / 8%);
  transform: translateY(-1px);
}

.login-submit:active:not(:disabled) {
  transform: translateY(0);
}

.login-submit:disabled {
  opacity: 0.6;
}

.login-submit svg {
  color: var(--brand-coral);
  transition: transform 150ms ease;
}

.login-submit:hover svg {
  transform: translateX(2px);
}

.login-footnote {
  margin: 22px 0 0;
  color: var(--text-subtle);
  font-size: 11px;
  text-align: center;
}

@keyframes login-window-in {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.992);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 820px) {
  .login-stage {
    padding: 24px;
  }

  .login-window {
    min-height: 620px;
  }

  .login-window__body {
    grid-template-columns: minmax(280px, 0.8fr) minmax(360px, 1.2fr);
  }

  .login-story {
    padding: 36px 32px;
  }

  .story-copy h1 {
    font-size: 35px;
  }

  .login-panel {
    padding: 42px 42px 24px;
  }
}

@media (max-width: 700px) {
  .login-stage {
    min-height: 100dvh;
    padding: 0;
    place-items: stretch;
  }

  .login-window {
    width: 100%;
    min-height: 100dvh;
    border: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .login-window__body {
    min-height: calc(100dvh - 42px);
    grid-template-columns: 1fr;
  }

  .login-story {
    min-height: 270px;
    padding: 30px 28px;
    border-right: 0;
    border-bottom: 1px solid var(--line-soft);
  }

  .story-copy {
    margin-top: 40px;
    padding-top: 0;
  }

  .story-kicker,
  .story-intro,
  .story-features {
    display: none;
  }

  .story-copy h1 {
    font-size: 34px;
    line-height: 1.16;
  }

  .login-panel {
    padding: 38px 28px 24px;
  }

  .login-panel__inner {
    margin: 0 auto auto;
  }
}

html[data-theme="dark"] .login-stage {
  background:
    linear-gradient(128deg, rgb(6 12 9 / 30%), transparent 48%),
    #111513;
}

html[data-theme="dark"] .login-window {
  border-color: rgb(255 255 255 / 12%);
  box-shadow:
    0 34px 90px rgb(0 0 0 / 55%),
    0 4px 18px rgb(0 0 0 / 35%),
    inset 0 0 0 1px rgb(255 255 255 / 5%);
}

html[data-theme="dark"] .login-panel {
  background:
    linear-gradient(145deg, rgb(255 91 53 / 4%), transparent 42%),
    #191c1a;
}

html[data-theme="dark"] .form-notice {
  color: #7ee787;
  background: rgb(63 185 80 / 10%);
}

@media (prefers-reduced-motion: reduce) {
  .login-window {
    animation: none;
  }
}
</style>
