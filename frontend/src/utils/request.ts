import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { createDiscreteApi } from 'naive-ui'

const { message } = createDiscreteApi(['message'])

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 如果是 blob 类型响应（如文件下载），直接返回原始响应
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response
    }
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    // 如果是 blob 类型的错误响应，尝试解析错误信息
    if (error.config?.responseType === 'blob' && error.response?.data instanceof Blob) {
      error.response.data.text().then((text: string) => {
        try {
          const errorData = JSON.parse(text)
          message.error(errorData.message || '导出失败')
        } catch {
          message.error('导出失败')
        }
      })
      return Promise.reject(error)
    }
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      message.error('登录已过期，请重新登录')
    } else {
      message.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request

