import request from '@/utils/request'

export interface RoleQuery {
  current?: number
  size?: number
  keyword?: string
}

export interface RoleForm {
  id?: number | string
  roleCode: string
  roleName: string
  description?: string
  status?: number
}

export const getRoleList = (params: RoleQuery) => {
  return request.get('/role/page', { params })
}

export const getRoleById = (id: number | string) => {
  return request.get(`/role/${id}`)
}

export const createRole = (data: RoleForm) => {
  return request.post('/role', data)
}

export const updateRole = (data: RoleForm) => {
  return request.put('/role', data)
}

export const deleteRole = (id: number | string) => {
  return request.delete(`/role/${id}`)
}

export const changeRoleStatus = (id: number | string, status: number) => {
  return request.put(`/role/status/${id}`, null, {
    params: { status }
  })
}

export const getAllRoles = () => {
  return request.get('/role/list')
}

// 获取角色的菜单ID列表
export const getRoleMenus = (id: number | string) => {
  return request.get<number[]>(`/role/${id}/menus`)
}

// 保存角色的菜单配置
export const saveRoleMenus = (id: number | string, menuIds: number[]) => {
  return request.put(`/role/${id}/menus`, menuIds)
}

