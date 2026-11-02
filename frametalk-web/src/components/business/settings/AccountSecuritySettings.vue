<script setup lang="ts">
import { Eye, EyeOff, KeyRound } from '@lucide/vue'
import { ref } from 'vue'

import UiButton from '@/components/ui/UiButton.vue'
import UiInput from '@/components/ui/UiInput.vue'
import SettingsCard from '@/components/ui/SettingsCard.vue'
import SettingsRow from '@/components/ui/SettingsRow.vue'
import { useAuthStore } from '@/stores/useAuthStore'

defineProps<{
  email: string
}>()

const emit = defineEmits<{
  notify: [message: string]
}>()

const auth = useAuthStore()
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const currentVisible = ref(false)
const newVisible = ref(false)
const confirmVisible = ref(false)
const saving = ref(false)
const errorMessage = ref('')

async function submit() {
  if (saving.value) return
  errorMessage.value = ''
  if (!currentPassword.value) {
    errorMessage.value = '请输入当前密码'
    return
  }
  if (Array.from(newPassword.value).length < 6) {
    errorMessage.value = '新密码至少需要 6 个字符'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的新密码不一致'
    return
  }
  if (newPassword.value === currentPassword.value) {
    errorMessage.value = '新密码不能与当前密码相同'
    return
  }

  saving.value = true
  try {
    await auth.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value,
    })
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    currentVisible.value = false
    newVisible.value = false
    confirmVisible.value = false
    emit('notify', '登录密码已更新，其他设备已退出登录')
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '无法修改登录密码'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <SettingsCard
    class="account-security"
    title="登录密码"
    description="修改成功后，其他设备上的登录会话会失效。"
  >
    <form class="account-security__form" @submit.prevent="submit">
      <SettingsRow
        title="登录账号"
        description="当前账号用于恢复项目、素材和渲染记录。"
      >
        <span class="account-security__account">{{ email }}</span>
      </SettingsRow>

      <SettingsRow
        title="当前密码"
        description="输入当前登录密码确认身份。"
        control-width="form"
      >
        <div class="account-security__input">
          <UiInput
            id="account-security-current-password"
            v-model="currentPassword"
            :type="currentVisible ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="输入当前登录密码"
            aria-label="当前密码"
            maxlength="128"
          />
          <button
            type="button"
            :aria-label="currentVisible ? '隐藏当前密码' : '显示当前密码'"
            :title="currentVisible ? '隐藏当前密码' : '显示当前密码'"
            @click="currentVisible = !currentVisible"
          >
            <EyeOff v-if="currentVisible" :size="15" />
            <Eye v-else :size="15" />
          </button>
        </div>
      </SettingsRow>

      <SettingsRow
        title="新密码"
        description="至少 6 个字符，最多 128 个字符。"
        control-width="form"
      >
        <div class="account-security__input">
          <UiInput
            id="account-security-new-password"
            v-model="newPassword"
            :type="newVisible ? 'text' : 'password'"
            autocomplete="new-password"
            placeholder="至少 6 个字符"
            aria-label="新密码"
            minlength="6"
            maxlength="128"
          />
          <button
            type="button"
            :aria-label="newVisible ? '隐藏新密码' : '显示新密码'"
            :title="newVisible ? '隐藏新密码' : '显示新密码'"
            @click="newVisible = !newVisible"
          >
            <EyeOff v-if="newVisible" :size="15" />
            <Eye v-else :size="15" />
          </button>
        </div>
      </SettingsRow>

      <SettingsRow
        title="确认新密码"
        description="再次输入新密码，避免输入错误。"
        control-width="form"
      >
        <div class="account-security__input">
          <UiInput
            id="account-security-confirm-password"
            v-model="confirmPassword"
            :type="confirmVisible ? 'text' : 'password'"
            autocomplete="new-password"
            placeholder="再次输入新密码"
            aria-label="确认新密码"
            minlength="6"
            maxlength="128"
          />
          <button
            type="button"
            :aria-label="
              confirmVisible ? '隐藏确认密码' : '显示确认密码'
            "
            :title="confirmVisible ? '隐藏确认密码' : '显示确认密码'"
            @click="confirmVisible = !confirmVisible"
          >
            <EyeOff v-if="confirmVisible" :size="15" />
            <Eye v-else :size="15" />
          </button>
        </div>
      </SettingsRow>

      <SettingsRow
        title="保存修改"
        description="密码只在 HTTPS 或本机连接中提交，不会写入前端存储。"
      >
        <span v-if="errorMessage" class="account-security__error" role="alert">
          {{ errorMessage }}
        </span>
        <UiButton
          variant="primary"
          size="regular"
          type="submit"
          :loading="saving"
          :disabled="saving"
        >
          <KeyRound v-if="!saving" :size="14" />
          更新密码
        </UiButton>
      </SettingsRow>
    </form>
  </SettingsCard>
</template>

<style scoped>
.account-security {
  width: 100%;
}

.account-security__form {
  display: block;
}

.account-security__account {
  overflow: hidden;
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: var(--font-caption);
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-security__input {
  position: relative;
  width: 100%;
}

.account-security__input :deep(.ui-input) {
  padding-right: 38px;
}

.account-security__input > button {
  position: absolute;
  top: 50%;
  right: 8px;
  display: grid;
  width: 28px;
  height: 28px;
  color: var(--text-subtle);
  background: transparent;
  border: 0;
  border-radius: var(--radius-control);
  transform: translateY(-50%);
  place-items: center;
}

.account-security__input > button:hover {
  color: var(--text);
  background: var(--hover-bg);
}

.account-security__error {
  flex: 1;
  color: var(--system-red);
  font-size: var(--font-caption);
  line-height: var(--line-caption);
}

@media (max-width: 760px) {
  .account-security__account {
    text-align: left;
  }
}
</style>
