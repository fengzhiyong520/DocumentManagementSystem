import request from '@/utils/request'

export const login = (username: string, password: string) => {
  return request.post<string>('/auth/login', null, {
    params: { username, password }
  })
}

export const register = (data: any) => {
  return request.post('/auth/register', data)
}

export const logout = () => {
  return request.post('/auth/logout')
}

export const getUserInfo = () => {
  return request.get('/user/info')
}

