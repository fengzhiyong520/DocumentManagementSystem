import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getUserInfo } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
  }

  const loginAction = async (username: string, password: string) => {
    const res = await login(username, password)
    setToken(res.data)
    await getUserInfoAction()
    router.push('/dashboard')
  }

  const getUserInfoAction = async () => {
    try {
      const res = await getUserInfo()
      setUserInfo(res.data)
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    router.push('/login')
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    loginAction,
    getUserInfoAction,
    logout
  }
})

