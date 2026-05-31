<template>
  <div class="profile-page">
    <div class="profile-header">
      <button class="back-btn" @click="router.push('/chat')">
        <ArrowLeftOutlined />
        <span>返回</span>
      </button>
      <h1>设置</h1>
    </div>
    <div class="profile-body">
      <div class="profile-section">
        <h2>个人信息</h2>
        <a-form
          :model="profileForm"
          @finish="handleUpdateProfile"
          layout="vertical"
          class="profile-form"
        >
          <a-form-item label="用户名">
            <a-input :value="userStore.userInfo?.userName" disabled />
          </a-form-item>
          <a-form-item label="头像链接" name="avatarUrl">
            <a-input v-model:value="profileForm.avatarUrl" placeholder="头像图片链接" />
          </a-form-item>
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="profileForm.email" placeholder="邮箱" />
          </a-form-item>
          <a-form-item label="手机号" name="phone">
            <a-input v-model:value="profileForm.phone" placeholder="手机号" />
          </a-form-item>
          <a-form-item>
            <button type="submit" class="profile-btn" :disabled="profileLoading">
              {{ profileLoading ? '保存中...' : '保存修改' }}
            </button>
          </a-form-item>
        </a-form>
      </div>
      <div class="profile-section">
        <h2>修改密码</h2>
        <a-form
          :model="passwordForm"
          :rules="passwordRules"
          @finish="handleChangePassword"
          layout="vertical"
          class="profile-form"
        >
          <a-form-item label="当前密码" name="oldPassword">
            <a-input-password v-model:value="passwordForm.oldPassword" placeholder="当前密码" />
          </a-form-item>
          <a-form-item label="新密码" name="newPassword">
            <a-input-password v-model:value="passwordForm.newPassword" placeholder="新密码" />
          </a-form-item>
          <a-form-item label="确认新密码" name="confirmPassword">
            <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="确认新密码" />
          </a-form-item>
          <a-form-item>
            <button type="submit" class="profile-btn" :disabled="passwordLoading">
              {{ passwordLoading ? '修改中...' : '修改密码' }}
            </button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const profileLoading = ref(false)
const passwordLoading = ref(false)

const profileForm = reactive({
  avatarUrl: '',
  email: '',
  phone: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (value && value !== passwordForm.newPassword) {
    throw new Error('两次输入的密码不一致')
  }
}

const passwordRules: Record<string, Rule[]> = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

onMounted(() => {
  if (userStore.userInfo) {
    profileForm.avatarUrl = userStore.userInfo.avatarUrl || ''
    profileForm.email = userStore.userInfo.email || ''
    profileForm.phone = userStore.userInfo.phone || ''
  }
})

async function handleUpdateProfile() {
  profileLoading.value = true
  try {
    await userStore.updateProfile({
      avatarUrl: profileForm.avatarUrl || undefined,
      email: profileForm.email || undefined,
      phone: profileForm.phone || undefined,
    })
    message.success('更新成功')
  } catch {
    // handled by interceptor
  } finally {
    profileLoading.value = false
  }
}

async function handleChangePassword() {
  passwordLoading.value = true
  try {
    await userStore.changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    message.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    // handled by interceptor
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--bg-primary);
  max-width: 680px;
  margin: 0 auto;
  padding: 24px 32px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}

.back-btn:hover {
  background: var(--bg-hover);
}

.profile-header h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.profile-section {
  margin-bottom: 40px;
}

.profile-section h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.profile-form :deep(.ant-form-item) {
  margin-bottom: 18px;
}

.profile-form :deep(.ant-form-item-label > label) {
  color: var(--text-secondary) !important;
  font-size: 13px;
}

.profile-form :deep(.ant-input),
.profile-form :deep(.ant-input-password) {
  background: var(--bg-input) !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
}

.profile-form :deep(.ant-input::placeholder) {
  color: var(--text-muted) !important;
}

.profile-form :deep(.ant-input-affix-wrapper) {
  background: var(--bg-input) !important;
  border-color: var(--border-color) !important;
}

.profile-form :deep(.ant-input-affix-wrapper input) {
  background: transparent !important;
  color: var(--text-primary) !important;
}

.profile-form :deep(.ant-input-disabled) {
  opacity: 0.5 !important;
}

.profile-form :deep(.ant-form-item-explain-error) {
  color: var(--danger);
}

.profile-btn {
  padding: 8px 24px;
  background: var(--accent);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.profile-btn:hover:not(:disabled) {
  background: var(--accent-hover);
}

.profile-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
