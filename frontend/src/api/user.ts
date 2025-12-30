import request from '@/utils/request'

export interface UserQuery {
  current?: number
  size?: number
  keyword?: string
}

export interface UserForm {
  id?: number | string
  username: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  roleId?: number | string
}

export const getUserList = (params: UserQuery) => {
  return request.get('/user/page', { params })
}

export const getUserById = (id: number | string) => {
  return request.get(`/user/${id}`)
}

export const createUser = (data: UserForm) => {
  return request.post('/user', data)
}

export const updateUser = (data: UserForm) => {
  return request.put('/user', data)
}

export const deleteUser = (id: number | string) => {
  return request.delete(`/user/${id}`)
}

export const changeUserStatus = (id: number | string, status: number) => {
  return request.put(`/user/status/${id}`, null, {
    params: { status }
  })
}

export const importUsers = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const exportUsers = () => {
  return request.get('/user/export', {
    responseType: 'blob'
  })
}

export const getRoleList = () => {
  return request.get('/role/list')
}

export interface UpdateCurrentUserInfoForm {
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
}

export const updateCurrentUserInfo = (data: UpdateCurrentUserInfoForm) => {
  return request.put('/user/info', data)
}

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

