import request from '@/utils/request'

export interface PermissionQuery {
  current?: number
  size?: number
  keyword?: string
}

export interface PermissionForm {
  id?: number | string
  permissionCode: string
  permissionName: string
  resource?: string
  method?: string
  description?: string
}

export interface PermissionVO {
  id: number | string
  permissionCode: string
  permissionName: string
  resource?: string
  method?: string
  description?: string
  createTime?: string
}

export const getPermissionPage = (params: PermissionQuery) => {
  return request.get('/permission/page', { params })
}

export const getPermissionList = () => {
  return request.get<PermissionVO[]>('/permission/list')
}

export const getPermissionById = (id: number | string) => {
  return request.get(`/permission/${id}`)
}

export const createPermission = (data: PermissionForm) => {
  return request.post('/permission', data)
}

export const updatePermission = (data: PermissionForm) => {
  return request.put('/permission', data)
}

export const deletePermission = (id: number | string) => {
  return request.delete(`/permission/${id}`)
}

export const getRolePermissions = (roleId: number | string) => {
  return request.get<number[]>('/role/' + roleId + '/permissions')
}

export const saveRolePermissions = (roleId: number | string, permissionIds: number[]) => {
  return request.put('/role/' + roleId + '/permissions', permissionIds)
}

