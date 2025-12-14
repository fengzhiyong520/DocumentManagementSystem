<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>权限管理</h2>
      <n-space>
        <n-input
          v-model:value="keyword"
          placeholder="搜索权限编码/名称/资源/描述"
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        />
        <n-button @click="handleSearch">搜索</n-button>
        <n-button type="primary" @click="handleAdd">新增权限</n-button>
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
        <n-form-item path="permissionCode" label="权限编码">
          <n-input v-model:value="form.permissionCode" :disabled="isEdit" placeholder="请输入权限编码" />
        </n-form-item>
        <n-form-item path="permissionName" label="权限名称">
          <n-input v-model:value="form.permissionName" placeholder="请输入权限名称" />
        </n-form-item>
        <n-form-item path="resource" label="资源路径">
          <n-input v-model:value="form.resource" placeholder="请输入资源路径，如：/api/user" />
        </n-form-item>
        <n-form-item path="method" label="请求方法">
          <n-select v-model:value="form.method" placeholder="请选择请求方法" :options="methodOptions" clearable />
        </n-form-item>
        <n-form-item path="description" label="描述">
          <n-input v-model:value="form.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { getPermissionPage, getPermissionById, createPermission, updatePermission, deletePermission, type PermissionForm } from '@/api/permission'
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
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

const form = reactive<PermissionForm>({
  permissionCode: '',
  permissionName: '',
  resource: '',
  method: '',
  description: ''
})

const rules = {
  permissionCode: { required: true, message: '请输入权限编码', trigger: 'blur' },
  permissionName: { required: true, message: '请输入权限名称', trigger: 'blur' }
}

const modalTitle = computed(() => isEdit.value ? '编辑权限' : '新增权限')

const columns: DataTableColumns = [
  { title: '权限编码', key: 'permissionCode', width: 150 },
  { title: '权限名称', key: 'permissionName', width: 150 },
  { title: '资源路径', key: 'resource', width: 200, ellipsis: { tooltip: true } },
  {
    title: '请求方法',
    key: 'method',
    width: 100,
    render: (row: any) => {
      if (!row.method) return '-'
      const typeMap: Record<string, string> = {
        GET: 'info',
        POST: 'success',
        PUT: 'warning',
        DELETE: 'error'
      }
      return h(
        'n-tag',
        { type: typeMap[row.method] || 'default', size: 'small' },
        { default: () => row.method }
      )
    }
  },
  { title: '描述', key: 'description', width: 250, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 180, render: (row: any) => formatDateTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 150,
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
    const res = await getPermissionPage({
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
    permissionCode: '',
    permissionName: '',
    resource: '',
    method: '',
    description: ''
  })
  showModal.value = true
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  try {
    const res = await getPermissionById(row.id)
    Object.assign(form, {
      id: res.data.id,
      permissionCode: res.data.permissionCode,
      permissionName: res.data.permissionName,
      resource: res.data.resource || '',
      method: res.data.method || '',
      description: res.data.description || ''
    })
    showModal.value = true
  } catch (error: any) {
    message.error('加载权限详情失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (isEdit.value) {
      await updatePermission(form)
      message.success('更新成功')
    } else {
      await createPermission(form)
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
    await deletePermission(id)
    message.success('删除成功')
    if (tableData.value.length === 1 && pagination.page > 1) {
      pagination.page--
    }
    loadData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

