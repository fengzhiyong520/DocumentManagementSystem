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

