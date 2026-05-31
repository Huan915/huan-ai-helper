<template>
  <div class="register-page">
    <div class="register-box">
      <div class="brand">
        <img :src="logoUrl" alt="Hper" class="brand-logo" />
        <span class="brand-name">Hper</span>
      </div>
      <a-form
        :model="formState"
        :rules="rules"
        @finish="handleRegister"
        layout="vertical"
        class="register-form"
      >
        <a-form-item name="userName">
          <a-input
            v-model:value="formState.userName"
            placeholder="用户名"
            size="large"
          />
        </a-form-item>
        <a-form-item name="password">
          <a-input-password
            v-model:value="formState.password"
            placeholder="密码"
            size="large"
          />
        </a-form-item>
        <a-form-item name="confirmPassword">
          <a-input-password
            v-model:value="formState.confirmPassword"
            placeholder="确认密码"
            size="large"
          />
        </a-form-item>
        <a-form-item name="email">
          <a-input
            v-model:value="formState.email"
            placeholder="邮箱（选填）"
            size="large"
          />
        </a-form-item>
        <a-form-item>
          <button
            type="submit"
            class="register-btn"
            :disabled="loading"
          >
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </a-form-item>
      </a-form>
      <div class="register-footer">
        已有账号？<router-link to="/login">登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { useUserStore } from '@/stores/user'
import logoUrl from '@/assets/images/hper-logo.svg'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const formState = reactive({
  userName: '',
  password: '',
  confirmPassword: '',
  email: '',
})

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (value && value !== formState.password) {
    throw new Error('两次输入的密码不一致')
  }
}

const rules: Record<string, Rule[]> = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
}

async function handleRegister() {
  loading.value = true
  try {
    await userStore.register(
      formState.userName,
      formState.password,
      formState.email || undefined,
    )
    message.success('注册成功')
    router.push('/chat')
  } catch {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
}

.register-box {
  width: 100%;
  max-width: 360px;
  padding: 0 20px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 36px;
}

.brand-logo {
  width: 40px;
  height: 40px;
}

.brand-name {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.register-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.register-form :deep(.ant-input) {
  background: var(--bg-input) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 12px !important;
  color: var(--text-primary) !important;
  height: 44px !important;
  font-size: 14px !important;
  padding: 0 14px !important;
  transition: border-color 0.15s !important;
}

.register-form :deep(.ant-input:hover) {
  border-color: var(--text-muted) !important;
}

.register-form :deep(.ant-input:focus) {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 2px rgba(12, 114, 255, 0.1) !important;
}

.register-form :deep(.ant-input::placeholder) {
  color: var(--text-muted) !important;
}

.register-form :deep(.ant-input-affix-wrapper) {
  background: var(--bg-input) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 12px !important;
  height: 44px !important;
  padding: 0 14px !important;
  transition: border-color 0.15s !important;
}

.register-form :deep(.ant-input-affix-wrapper:hover) {
  border-color: var(--text-muted) !important;
}

.register-form :deep(.ant-input-affix-wrapper:focus-within) {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 2px rgba(12, 114, 255, 0.1) !important;
}

.register-form :deep(.ant-input-affix-wrapper .ant-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  height: auto !important;
  padding: 0 !important;
}

.register-form :deep(.ant-input-affix-wrapper .ant-input-suffix) {
  color: var(--text-muted) !important;
}

.register-form :deep(.ant-form-item-explain-error) {
  color: var(--danger);
  font-size: 12px;
  margin-top: 4px;
}

.register-btn {
  width: 100%;
  height: 44px;
  background: var(--accent);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.register-btn:hover:not(:disabled) {
  background: var(--accent-hover);
}

.register-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.register-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.register-footer {
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

.register-footer a {
  color: var(--accent);
}

.register-footer a:hover {
  color: var(--accent-hover);
}
</style>
