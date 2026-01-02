<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>角色配置</h2>
      <n-space>
        <n-input
          v-model:value="keyword"
          placeholder="搜索角色编码/名称/描述"
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        />
        <n-button @click="handleSearch">搜索</n-button>
        <n-button type="primary" @click="handleAdd">新增角色</n-button>
      </n-space>
    </n-space>
    <n-data-table
      :columns="columns"
      :data="tableData"
      :pagination="pagination"
      :loading="loading"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />
    <!-- 新增/编辑对话框 -->
    <n-modal v-model:show="showModal" :title="modalTitle" preset="dialog" style="width: 600px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100">
        <n-form-item path="roleCode" label="角色编码">
          <n-input v-model:value="form.roleCode" :disabled="isEdit" placeholder="请输入角色编码" />
        </n-form-item>
        <n-form-item path="roleName" label="角色名称">
          <n-input v-model:value="form.roleName" placeholder="请输入角色名称" />
        </n-form-item>
        <n-form-item path="description" label="描述">
          <n-input v-model:value="form.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </n-form-item>
        <n-form-item path="status" label="状态">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">禁用</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</n-button>
      </template>
    </n-modal>
    <!-- 权限配置对话框 -->
    <n-modal v-model:show="showPermissionModal" title="权限配置" preset="dialog" style="width: 800px">
      <div style="max-height: 500px; overflow-y: auto">
        <n-space vertical>
          <n-input
            v-model:value="permissionKeyword"
            placeholder="搜索权限编码/名称"
            clearable
            @input="filterPermissions"
          />
          <n-checkbox-group v-model:value="selectedPermissionIds">
            <n-space vertical>
              <n-checkbox
                v-for="permission in filteredPermissionList"
                :key="permission.id"
                :value="Number(permission.id)"
                :label="`${permission.permissionCode} - ${permission.permissionName}`"
              />
            </n-space>
          </n-checkbox-group>
          <div v-if="filteredPermissionList.length === 0" style="text-align: center; padding: 20px; color: #999">
            暂无权限数据
          </div>
        </n-space>
      </div>
      <template #action>
        <n-button @click="showPermissionModal = false">取消</n-button>
        <n-button type="primary" @click="handleSavePermissions" :loading="permissionLoading">保存</n-button>
      </template>
    </n-modal>
    <!-- 菜单配置对话框 -->
    <n-modal v-model:show="showMenuModal" title="菜单配置" preset="dialog" style="width: 800px">
      <div style="max-height: 500px; overflow-y: auto">
        <n-tree
          :data="menuTree"
          :checked-keys="selectedMenuIds"
          checkable
          :check-strategy="'child'"
          @update:checked-keys="handleMenuCheck"
        />
      </div>
      <template #action>
        <n-button @click="showMenuModal = false">取消</n-button>
        <n-button type="primary" @click="handleSaveMenus" :loading="menuLoading">保存</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { getRoleList, createRole, updateRole, deleteRole, changeRoleStatus, getRoleMenus, saveRoleMenus, type RoleForm } from '@/api/role'
import { getPermissionList, getRolePermissions, saveRolePermissions, type PermissionVO } from '@/api/permission'
import { getMenuTree, type Menu } from '@/api/menu'
import { useMessage } from 'naive-ui'
import type { DataTableColumns, FormInst } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const submitLoading = ref(false)
const showModal = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInst>()
const keyword = ref('')
const tableData = ref([])
const showPermissionModal = ref(false)
const permissionLoading = ref(false)
const permissionList = ref<PermissionVO[]>([])
const filteredPermissionList = ref<PermissionVO[]>([])
const selectedPermissionIds = ref<number[]>([])
const permissionKeyword = ref('')
const showMenuModal = ref(false)
const menuLoading = ref(false)
const menuTree = ref<any[]>([])
const selectedMenuIds = ref<Array<string | number>>([])
const currentRoleId = ref<number | string>('')
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const form = reactive<RoleForm>({
  roleCode: '',
  roleName: '',
  description: '',
  status: 1
})

const rules = {
  roleCode: { required: true, message: '请输入角色编码', trigger: 'blur' },
  roleName: { required: true, message: '请输入角色名称', trigger: 'blur' }
}

const modalTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')

const columns: DataTableColumns = [
  { title: '角色编码', key: 'roleCode', width: 150 },
  { title: '角色名称', key: 'roleName', width: 150 },
  { title: '描述', key: 'description', width: 250, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row: any) => {
      return h(
        'n-tag',
        { type: row.status === 1 ? 'success' : 'error' },
        { default: () => row.status === 1 ? '启用' : '禁用' }
      )
    }
  },
  { title: '创建时间', key: 'createTime', width: 180, render: (row: any) => formatDateTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    fixed: 'right',
    render: (row: any) => {
      const buttons = []
      buttons.push(
        h('n-button', {
          size: 'small',
          onClick: () => handleEdit(row)
        }, { default: () => '编辑' })
      )
      buttons.push(
        h('n-button', {
          size: 'small',
          type: 'info',
          onClick: () => handlePermissionConfig(row)
        }, { default: () => '权限配置' })
      )
      buttons.push(
        h('n-button', {
          size: 'small',
          type: 'warning',
          onClick: () => handleMenuConfig(row)
        }, { default: () => '菜单配置' })
      )
      buttons.push(
        h('n-button', {
          size: 'small',
          type: row.status === 1 ? 'error' : 'success',
          onClick: () => handleStatusChange(row.id, row.status === 1 ? 0 : 1)
        }, { default: () => row.status === 1 ? '禁用' : '启用' })
      )
      buttons.push(
        h('n-button', {
          size: 'small',
          type: 'error',
          onClick: () => handleDelete(row.id)
        }, { default: () => '删除' })
      )
      return h('div', { style: 'display: flex; gap: 8px' }, buttons)
    }
  }
]

const formatDateTime = (dateTime: string | null | undefined) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoleList({
      current: pagination.page,
      size: pagination.pageSize,
      keyword: keyword.value || undefined
    })
    tableData.value = res.data.records.map((item: any) => ({
      ...item,
      id: String(item.id || '')
    }))
    pagination.itemCount = res.data.total
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadData()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    roleCode: '',
    roleName: '',
    description: '',
    status: 1
  })
  showModal.value = true
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description || '',
    status: row.status
  })
  showModal.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (isEdit.value) {
      await updateRole(form)
      message.success('更新成功')
    } else {
      await createRole(form)
      message.success('创建成功')
    }
    showModal.value = false
    loadData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (id: number | string) => {
  try {
    await deleteRole(id)
    message.success('删除成功')
    if (tableData.value.length === 1 && pagination.page > 1) {
      pagination.page--
    }
    loadData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

const handleStatusChange = async (id: number | string, status: number) => {
  try {
    await changeRoleStatus(id, status)
    message.success('操作成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

const loadPermissions = async () => {
  try {
    const res = await getPermissionList()
    permissionList.value = res.data || []
    filteredPermissionList.value = permissionList.value
  } catch (error: any) {
    message.error('加载权限列表失败')
  }
}

const loadMenuTree = async () => {
  try {
    const res = await getMenuTree()
    menuTree.value = convertToTreeOptions(res.data || [])
  } catch (error: any) {
    message.error('加载菜单树失败')
  }
}

const convertToTreeOptions = (menus: Menu[]): any[] => {
  return menus.map(menu => ({
    key: String(menu.id),
    label: `${menu.menuName} (${menu.menuCode})`,
    menu: menu,
    children: menu.children ? convertToTreeOptions(menu.children) : undefined
  }))
}

const handleMenuCheck = (keys: Array<string | number>) => {
  selectedMenuIds.value = keys
}

const handleMenuConfig = async (row: any) => {
  currentRoleId.value = row.id
  selectedMenuIds.value = []
  showMenuModal.value = true
  
  // 加载菜单树（如果还没有加载）
  if (menuTree.value.length === 0) {
    await loadMenuTree()
  }
  
  // 加载当前角色的菜单
  try {
    const res = await getRoleMenus(row.id)
    selectedMenuIds.value = (res.data || []).map((id: any) => String(id))
  } catch (error: any) {
    message.error('加载角色菜单失败')
  }
}

const handleSaveMenus = async () => {
  try {
    menuLoading.value = true
    const menuIds = selectedMenuIds.value.map(id => Number(id))
    await saveRoleMenus(currentRoleId.value, menuIds)
    message.success('保存成功')
    showMenuModal.value = false
  } catch (error: any) {
    message.error(error.message || '保存失败')
  } finally {
    menuLoading.value = false
  }
}

const filterPermissions = () => {
  if (!permissionKeyword.value) {
    filteredPermissionList.value = permissionList.value
    return
  }
  const keyword = permissionKeyword.value.toLowerCase()
  filteredPermissionList.value = permissionList.value.filter(
    (p) =>
      p.permissionCode?.toLowerCase().includes(keyword) ||
      p.permissionName?.toLowerCase().includes(keyword)
  )
}

const handlePermissionConfig = async (row: any) => {
  currentRoleId.value = row.id
  permissionKeyword.value = ''
  selectedPermissionIds.value = []
  showPermissionModal.value = true
  
  // 加载权限列表（如果还没有加载）
  if (permissionList.value.length === 0) {
    await loadPermissions()
  } else {
    filteredPermissionList.value = permissionList.value
  }
  
  // 加载当前角色的权限
  try {
    const res = await getRolePermissions(row.id)
    selectedPermissionIds.value = (res.data || []).map((id: any) => Number(id))
  } catch (error: any) {
    message.error('加载角色权限失败')
  }
}

const handleSavePermissions = async () => {
  try {
    permissionLoading.value = true
    await saveRolePermissions(currentRoleId.value, selectedPermissionIds.value)
    message.success('保存成功')
    showPermissionModal.value = false
  } catch (error: any) {
    message.error(error.message || '保存失败')
  } finally {
    permissionLoading.value = false
  }
}

onMounted(() => {
  loadData()
  loadPermissions()
  loadMenuTree()
})
</script>

