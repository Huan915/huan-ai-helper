import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '@/api/types'
import * as userApi from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<User | null>(null)
  const isLoggedIn = computed(() => !!userInfo.value)

  function initFromStorage() {
    const stored = localStorage.getItem('user')
    if (stored) {
      try {
        userInfo.value = JSON.parse(stored)
      } catch {
        localStorage.removeItem('user')
      }
    }
  }

  async function login(userName: string, password: string) {
    const res = await userApi.login({ userName, password })
    userInfo.value = res.data.data
    localStorage.setItem('user', JSON.stringify(res.data.data))
  }

  async function register(userName: string, password: string, email?: string, phone?: string) {
    const res = await userApi.register({ userName, password, email, phone })
    userInfo.value = res.data.data
    localStorage.setItem('user', JSON.stringify(res.data.data))
  }

  async function fetchUserInfo() {
    if (!userInfo.value?.id) return
    const res = await userApi.getUserInfo(userInfo.value.id)
    userInfo.value = res.data.data
    localStorage.setItem('user', JSON.stringify(res.data.data))
  }

  async function updateProfile(data: { avatarUrl?: string; email?: string; phone?: string }) {
    if (!userInfo.value?.id) return
    const res = await userApi.updateUser(userInfo.value.id, data)
    userInfo.value = res.data.data
    localStorage.setItem('user', JSON.stringify(res.data.data))
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    if (!userInfo.value?.id) return
    await userApi.changePassword(userInfo.value.id, { oldPassword, newPassword })
  }

  function logout() {
    userInfo.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  }

  initFromStorage()

  return {
    userInfo,
    isLoggedIn,
    login,
    register,
    fetchUserInfo,
    updateProfile,
    changePassword,
    logout,
  }
})
