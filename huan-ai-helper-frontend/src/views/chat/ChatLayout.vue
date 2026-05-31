<template>
  <div class="chat-layout">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="sidebar-brand">
          <img src="/logo.svg" alt="Hper" class="sidebar-logo" />
          <span v-show="!sidebarCollapsed" class="sidebar-site-name">Hper</span>
        </div>
        <button class="icon-btn" @click="sidebarCollapsed = !sidebarCollapsed" :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'">
          <MenuFoldOutlined v-if="!sidebarCollapsed" />
          <MenuUnfoldOutlined v-else />
        </button>
      </div>
      <div class="sidebar-tabs">
        <button class="tab-item" :class="{ active: activeTab === 'chat' }" @click="handleNewChat">
          <PlusCircleOutlined />
          <span v-show="!sidebarCollapsed">新对话</span>
        </button>
        <button class="tab-item" :class="{ active: activeTab === 'agent' }" @click="handleAgentClick">
          <RobotOutlined />
          <span v-show="!sidebarCollapsed">智能体</span>
        </button>
        <button class="tab-item" :class="{ active: activeTab === 'more' }" @click="activeTab = 'more'">
          <AppstoreOutlined />
          <span v-show="!sidebarCollapsed">更多</span>
        </button>
      </div>
      <div class="sidebar-list">
        <div
          v-for="conv in conversationStore.conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: conversationStore.currentConversation?.id === conv.id }"
          @click="handleConvClick(conv.id)"
        >
          <template v-if="editingId === conv.id">
            <input
              :value="editingTitle"
              class="conv-edit-input"
              @click.stop
              @input="editingTitle = ($event.target as HTMLInputElement).value"
              @keydown.enter.prevent="handleRenameSave"
              @keydown.escape="cancelEdit"
              @blur="handleRenameSave"
            />
          </template>
          <template v-else>
            <span v-show="!sidebarCollapsed" class="conv-title">{{ conv.title }}</span>
            <div v-show="!sidebarCollapsed" class="conv-menu-wrapper">
              <button class="conv-menu-btn" @click.stop="toggleMenu(conv.id)">
                <MoreOutlined />
              </button>
              <div v-if="openMenuId === conv.id" class="conv-menu-dropdown">
                <button class="menu-item" @click.stop="handleRename(conv.id, conv.title)">
                  <EditOutlined /> 重命名
                </button>
                <button class="menu-item danger" @click.stop="handleDeleteClick(conv.id)">
                  <DeleteOutlined /> 删除
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
      <div class="sidebar-bottom">
        <div class="user-info" @click="toggleUserMenu">
          <a-avatar :size="36" :src="userStore.userInfo?.avatarUrl || undefined">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <span v-show="!sidebarCollapsed" class="user-name">{{ userStore.userInfo?.userName }}</span>
        </div>
        <div v-if="userMenuVisible" class="user-menu-dropdown">
          <button class="user-menu-item" @click="handleProfile">
            <UserOutlined /> 个人资料
          </button>
          <button class="user-menu-item" @click="handleSettings">
            <SettingOutlined /> 设置
          </button>
          <button class="user-menu-item danger" @click="handleLogout">
            <LogoutOutlined /> 退出登录
          </button>
        </div>
      </div>
    </aside>
    <main class="chat-main">
      <ChatPanel 
        :initializing="initializing" 
        :activeTab="activeTab"
        @createChatbot="handleCreateChatbotClick"
        @switchToChat="handleSwitchToChat"
      />
    </main>

    <div v-if="agentModalVisible" class="delete-confirm-overlay" @click="agentModalVisible = false">
      <div class="agent-modal" @click.stop>
        <div class="agent-modal-title">创建智能体</div>
        <div class="agent-modal-content">
          <div class="agent-modal-field">
            <label class="agent-modal-label">智能体名称</label>
            <input
              v-model="agentName"
              class="agent-modal-input"
              placeholder="为你的智能体起个名字"
              maxlength="50"
            />
          </div>
          <div class="agent-modal-field">
            <label class="agent-modal-label">设定描述</label>
            <textarea
              v-model="agentPrompt"
              class="agent-modal-textarea"
              placeholder="描述智能体的角色、能力和行为方式..."
              rows="5"
              maxlength="2000"
            ></textarea>
          </div>
        </div>
        <div class="delete-confirm-actions">
          <button class="delete-btn cancel" @click="agentModalVisible = false">取消</button>
          <button class="delete-btn confirm" :disabled="agentCreating" @click="handleCreateAgent">
            {{ agentCreating ? '创建中...' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteConfirmVisible" class="delete-confirm-overlay" @click="deleteConfirmVisible = false">
      <div class="delete-confirm-modal" @click.stop>
        <div class="delete-confirm-title">确认删除此会话？</div>
        <div class="delete-confirm-actions">
          <button class="delete-btn cancel" @click="deleteConfirmVisible = false">取消</button>
          <button class="delete-btn confirm" @click="handleDeleteConfirm">删除</button>
        </div>
      </div>
    </div>

    <div v-if="profileModalVisible" class="delete-confirm-overlay" @click="profileModalVisible = false">
      <div class="profile-modal" @click.stop>
        <div class="profile-title">个人资料</div>
        <div class="profile-content">
          <div class="profile-avatar-wrapper" @click="triggerAvatarUpload">
            <a-avatar :size="80" :src="profileAvatar || userStore.userInfo?.avatarUrl || undefined">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div class="profile-avatar-edit">编辑</div>
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleAvatarChange"
            />
          </div>
          <div class="profile-field">
            <label class="profile-label">昵称</label>
            <input
              v-model="profileName"
              class="profile-input"
              placeholder="请输入昵称"
              maxlength="20"
            />
          </div>
        </div>
        <div class="delete-confirm-actions">
          <button class="delete-btn cancel" @click="profileModalVisible = false">取消</button>
          <button class="delete-btn confirm" :disabled="profileSaving" @click="handleProfileSave">
            {{ profileSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="settingsModalVisible" class="delete-confirm-overlay" @click="settingsModalVisible = false">
      <div class="settings-modal" @click.stop>
        <div class="settings-sidebar">
          <div class="settings-menu">
            <button
              class="settings-menu-item"
              :class="{ active: settingsTab === 'general' }"
              @click="settingsTab = 'general'"
            >
              <SettingOutlined /> 通用设置
            </button>
            <button
              class="settings-menu-item"
              :class="{ active: settingsTab === 'account' }"
              @click="settingsTab = 'account'"
            >
              <UserOutlined /> 账号设置
            </button>
            <button
              class="settings-menu-item"
              :class="{ active: settingsTab === 'about' }"
              @click="settingsTab = 'about'"
            >
              <InfoCircleOutlined /> 关于我们
            </button>
          </div>
        </div>
        <div class="settings-content">
          <template v-if="settingsTab === 'general'">
            <div class="settings-section-title">通用设置</div>
            <div class="settings-item">
              <span class="settings-item-label">主题模式</span>
              <div class="theme-switch">
                <button
                  class="theme-btn"
                  :class="{ active: themeStore.theme === 'dark' }"
                  @click="themeStore.setTheme('dark')"
                >深色</button>
                <button
                  class="theme-btn"
                  :class="{ active: themeStore.theme === 'light' }"
                  @click="themeStore.setTheme('light')"
                >浅色</button>
              </div>
            </div>
            <div class="settings-item">
              <span class="settings-item-label">语言</span>
              <span class="settings-item-value">简体中文</span>
            </div>
          </template>
          <template v-else-if="settingsTab === 'account'">
            <div class="settings-section-title">账号设置</div>
            <div class="settings-item">
              <span class="settings-item-label">邮箱</span>
              <span class="settings-item-value">{{ userStore.userInfo?.email || '未设置' }}</span>
            </div>
            <div class="settings-item">
              <span class="settings-item-label">手机号</span>
              <span class="settings-item-value">{{ userStore.userInfo?.phone || '未绑定' }}</span>
            </div>
            <div class="settings-item">
              <span class="settings-item-label">修改密码</span>
              <button class="settings-item-btn">修改</button>
            </div>
          </template>
          <template v-else-if="settingsTab === 'about'">
            <div class="settings-section-title">关于我们</div>
            <div class="about-logo">
              <img src="/logo.svg" alt="Hper" class="about-logo-img" />
              <div class="about-name">Hper</div>
              <div class="about-version">版本 1.0.0</div>
            </div>
            <div class="about-desc">
              Hper 是一款智能 AI 助手，致力于为用户提供高效、便捷的对话体验。
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  PlusCircleOutlined,
  RobotOutlined,
  AppstoreOutlined,
  DeleteOutlined,
  UserOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MoreOutlined,
  EditOutlined,
  SettingOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'
import { useConversationStore } from '@/stores/conversation'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { uploadAvatar, updateUser } from '@/api/user'
import ChatPanel from '@/components/ChatPanel.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const conversationStore = useConversationStore()
const themeStore = useThemeStore()
const sidebarCollapsed = ref(false)
const activeTab = ref<'chat' | 'agent' | 'more'>('chat')
const initializing = ref(true)

onMounted(async () => {
  await conversationStore.fetchConversations()
  const convId = route.query.c
  if (convId) {
    const id = Number(convId)
    if (!isNaN(id) && conversationStore.conversations.some((c) => c.id === id)) {
      await conversationStore.selectConversation(id)
    }
  }
  const tab = route.query.tab as string
  if (tab === 'agent') {
    activeTab.value = 'agent'
  }
  initializing.value = false
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.conv-menu-wrapper')) {
    openMenuId.value = null
  }
  if (!target.closest('.sidebar-bottom')) {
    userMenuVisible.value = false
  }
}

async function handleNewChat() {
  activeTab.value = 'chat'
  conversationStore.clearCurrent()
}

function handleCreateChatbotClick() {
  agentName.value = ''
  agentPrompt.value = ''
  agentModalVisible.value = true
}

const agentModalVisible = ref(false)
const agentName = ref('')
const agentPrompt = ref('')
const agentCreating = ref(false)

function handleAgentClick() {
  activeTab.value = 'agent'
  router.replace({ query: { tab: 'agent' } })
}

function handleSwitchToChat() {
  activeTab.value = 'chat'
  router.replace({ query: {} })
}

async function handleCreateAgent() {
  if (!agentName.value.trim()) return
  agentCreating.value = true
  try {
    await conversationStore.createConversation(agentName.value.trim(), agentPrompt.value.trim() || undefined, 'CHATBOT')
    agentModalVisible.value = false
    activeTab.value = 'chat'
  } finally {
    agentCreating.value = false
  }
}

const openMenuId = ref<number | null>(null)

function toggleMenu(id: number) {
  openMenuId.value = openMenuId.value === id ? null : id
}

function handleConvClick(id: number) {
  if (editingId.value === id) return
  activeTab.value = 'chat'
  conversationStore.selectConversation(id)
}

const editingId = ref<number | null>(null)
const editingTitle = ref('')
const originalTitle = ref('')
const renameSaving = ref(false)

function handleRename(id: number, title: string) {
  openMenuId.value = null
  editingId.value = id
  editingTitle.value = title
  originalTitle.value = title
  nextTick(() => {
    const input = document.querySelector('.conv-edit-input') as HTMLInputElement
    input?.focus()
    input?.select()
  })
}

function cancelEdit() {
  editingId.value = null
  editingTitle.value = ''
  originalTitle.value = ''
}

async function handleRenameSave() {
  if (renameSaving.value) return
  const id = editingId.value
  const newTitle = editingTitle.value.trim()
  const oldTitle = originalTitle.value
  
  editingId.value = null
  
  if (!id || !newTitle || newTitle === oldTitle) {
    return
  }
  
  renameSaving.value = true
  try {
    await conversationStore.renameConversation(id, newTitle)
  } finally {
    renameSaving.value = false
  }
}

const deleteConfirmVisible = ref(false)
const deleteId = ref<number | null>(null)

function handleDeleteClick(id: number) {
  openMenuId.value = null
  deleteId.value = id
  deleteConfirmVisible.value = true
}

async function handleDeleteConfirm() {
  if (!deleteId.value) return
  await conversationStore.deleteConversation(deleteId.value)
  deleteConfirmVisible.value = false
  deleteId.value = null
}

const userMenuVisible = ref(false)

const profileModalVisible = ref(false)
const profileName = ref('')
const profileAvatar = ref('')
const profileAvatarFile = ref<File | null>(null)
const avatarInputRef = ref<HTMLInputElement | null>(null)
const profileSaving = ref(false)

function toggleUserMenu() {
  userMenuVisible.value = !userMenuVisible.value
}

function handleProfile() {
  userMenuVisible.value = false
  profileName.value = userStore.userInfo?.userName || ''
  profileAvatar.value = ''
  profileAvatarFile.value = null
  profileModalVisible.value = true
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

function handleAvatarChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) {
    profileAvatarFile.value = file
    const reader = new FileReader()
    reader.onload = () => {
      profileAvatar.value = reader.result as string
    }
    reader.readAsDataURL(file)
  }
}

async function handleProfileSave() {
  if (profileSaving.value) return
  profileSaving.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return

    let newAvatarUrl = userStore.userInfo?.avatarUrl
    if (profileAvatarFile.value) {
      const res = await uploadAvatar(userId, profileAvatarFile.value)
      newAvatarUrl = res.data.data
    }

    if (profileName.value !== userStore.userInfo?.userName) {
      await updateUser(userId, { userName: profileName.value })
    }

    userStore.userInfo = {
      ...userStore.userInfo!,
      userName: profileName.value,
      avatarUrl: newAvatarUrl,
    }
    localStorage.setItem('user', JSON.stringify(userStore.userInfo))

    profileModalVisible.value = false
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    profileSaving.value = false
  }
}

const settingsModalVisible = ref(false)
const settingsTab = ref<'general' | 'account' | 'about'>('general')

function handleSettings() {
  userMenuVisible.value = false
  settingsTab.value = 'general'
  settingsModalVisible.value = true
}

function handleLogout() {
  userMenuVisible.value = false
  userStore.logout()
  conversationStore.clearCurrent()
  router.push('/login')
}
</script>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  min-width: 260px;
  background: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  transition: width 0.2s, min-width 0.2s;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 60px;
  min-width: 60px;
}

.sidebar.collapsed .sidebar-header {
  padding: 12px 0;
  justify-content: center;
}

.sidebar.collapsed .sidebar-brand {
  justify-content: center;
}

.sidebar-header {
  padding: 12px 12px 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.sidebar-logo {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.sidebar-site-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

.sidebar.collapsed .sidebar-tabs {
  padding: 8px 4px;
  gap: 2px;
}

.sidebar.collapsed .tab-item {
  justify-content: center;
  padding: 8px;
}

.sidebar.collapsed .sidebar-list {
  display: none;
}

.sidebar.collapsed .sidebar-bottom {
  justify-content: center;
  padding: 12px 8px;
}

.sidebar.collapsed .user-info {
  padding: 6px;
}

.sidebar-tabs {
  padding: 12px 8px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 15px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  white-space: nowrap;
}

.tab-item:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.tab-item.active {
  background: var(--bg-active);
  color: var(--text-primary);
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
  mask-image: linear-gradient(to bottom, transparent 0, black 8px, black calc(100% - 24px), transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, transparent 0, black 8px, black calc(100% - 24px), transparent 100%);
}

.tab-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px 16px;
  color: var(--text-muted);
  font-size: 15px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.conv-item:hover {
  background: var(--bg-hover);
}

.conv-item.active {
  background: var(--bg-active);
}

.conv-title {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-edit-input {
  flex: 1;
  background: var(--bg-primary);
  border: 1px solid var(--accent);
  border-radius: 4px;
  padding: 3px 6px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
}

.conv-menu-wrapper {
  position: relative;
  flex-shrink: 0;
}

.conv-menu-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s;
}

.conv-item:hover .conv-menu-btn {
  opacity: 1;
}

.conv-menu-btn:hover {
  background: var(--bg-hover);
}

.conv-menu-dropdown {
  position: absolute;
  right: -20px;
  top: 100%;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  min-width: 100px;
  z-index: 100;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 8px 12px;
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}

.menu-item:hover {
  background: var(--bg-hover);
}

.menu-item.danger {
  color: var(--danger);
}

.delete-confirm-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: overlay-fade-in 0.2s ease;
}

@keyframes overlay-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.delete-confirm-modal {
  background: var(--bg-primary);
  border-radius: 24px;
  padding: 28px;
  min-width: 300px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  animation: modal-pop-in 0.25s ease;
}

@keyframes modal-pop-in {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.delete-confirm-title {
  font-size: 16px;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 20px;
}

.profile-modal {
  background: var(--bg-primary);
  border-radius: 24px;
  padding: 28px;
  min-width: 320px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  animation: modal-pop-in 0.25s ease;
}

.profile-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 24px;
}

.profile-content {
  margin-bottom: 24px;
}

.profile-avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  margin-bottom: 24px;
  position: relative;
}

.profile-avatar-edit {
  margin-top: 8px;
  font-size: 13px;
  color: var(--accent);
}

.profile-field {
  margin-bottom: 16px;
}

.profile-label {
  display: block;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.profile-input {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  font-size: 15px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.profile-input:focus {
  border-color: var(--accent);
}

.profile-input::placeholder {
  color: var(--text-muted);
}

.settings-modal {
  background: var(--bg-primary);
  border-radius: 24px;
  width: 720px;
  height: 480px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  animation: modal-pop-in 0.25s ease;
  display: flex;
  overflow: hidden;
}

.settings-sidebar {
  width: 160px;
  background: var(--bg-hover);
  padding: 16px 8px;
  flex-shrink: 0;
}

.settings-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.settings-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 14px;
  background: transparent;
  border: none;
  border-radius: 12px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.settings-menu-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.settings-menu-item.active {
  background: var(--bg-primary);
  color: var(--text-primary);
}

.settings-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.settings-section-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-color);
}

.settings-item:last-child {
  border-bottom: none;
}

.settings-item-label {
  font-size: 15px;
  color: var(--text-primary);
}

.settings-item-value {
  font-size: 14px;
  color: var(--text-muted);
}

.settings-item-btn {
  padding: 6px 16px;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}

.settings-item-btn:hover {
  background: var(--bg-active);
}

.about-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.about-logo-img {
  width: 64px;
  height: 64px;
  margin-bottom: 12px;
}

.about-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.about-version {
  font-size: 13px;
  color: var(--text-muted);
}

.about-desc {
  margin-top: 20px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  text-align: center;
}

.theme-switch {
  display: flex;
  gap: 8px;
}

.theme-btn {
  padding: 8px 20px;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.theme-btn:hover {
  background: var(--bg-active);
}

.theme-btn.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.delete-confirm-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.delete-btn {
  padding: 10px 28px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: background 0.15s, transform 0.1s;
}

.delete-btn.cancel {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.delete-btn.cancel:hover {
  background: var(--bg-active);
}

.delete-btn.confirm {
  background: #fff;
  color: black;
}

.delete-btn.confirm:hover {
  background: #c0392b;
}

.delete-btn:active {
  transform: scale(0.96);
}

.sidebar-bottom {
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.user-menu-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: #3a3a3a;
  border-radius: 12px 12px 0 0;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  animation: menu-slide-up 0.2s ease;
}

@keyframes menu-slide-up {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  background: transparent;
  border: none;
  color: #e0e0e0;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.user-menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.user-menu-item.danger {
  color: #ff6b6b;
}

.icon-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 16px;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: background 0.15s, color 0.15s;
}

.icon-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.user-info:hover {
  background: var(--bg-hover);
}

.user-name {
  font-size: 15px;
  color: var(--text-primary);
}

.chat-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.agent-modal {
  background: var(--bg-primary);
  border-radius: 24px;
  padding: 28px;
  min-width: 400px;
  max-width: 480px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  animation: modal-pop-in 0.25s ease;
}

.agent-modal-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 24px;
}

.agent-modal-content {
  margin-bottom: 24px;
}

.agent-modal-field {
  margin-bottom: 16px;
}

.agent-modal-field:last-child {
  margin-bottom: 0;
}

.agent-modal-label {
  display: block;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.agent-modal-input {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  font-size: 15px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.agent-modal-input:focus {
  border-color: var(--accent);
}

.agent-modal-input::placeholder {
  color: var(--text-muted);
}

.agent-modal-textarea {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  font-size: 15px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
}

.agent-modal-textarea:focus {
  border-color: var(--accent);
}

.agent-modal-textarea::placeholder {
  color: var(--text-muted);
}

.agent-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}
</style>
