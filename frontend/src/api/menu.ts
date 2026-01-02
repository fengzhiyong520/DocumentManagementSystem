import request from '@/utils/request'

export interface Menu {
  id?: number
  menuCode: string
  menuName: string
  menuType?: string
  path?: string
  component?: string
  icon?: string
  parentId?: number
  sortOrder?: number
  status?: number
  description?: string
  children?: Menu[]
}

// 获取菜单树
export function getMenuTree() {
  return request.get<Menu[]>('/menu/tree')
}

// 根据ID获取菜单
export function getMenuById(id: number) {
  return request.get<Menu>(`/menu/${id}`)
}

// 创建菜单
export function createMenu(menu: Menu) {
  return request.post('/menu', menu)
}

// 更新菜单
export function updateMenu(menu: Menu) {
  return request.put('/menu', menu)
}

// 删除菜单
export function deleteMenu(id: number) {
  return request.delete(`/menu/${id}`)
}

// 获取当前用户的菜单列表
export function getCurrentUserMenus() {
  return request.get<Menu[]>('/user/my/menus')
}

