<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>用户管理</h2>
      <n-space>
        <n-input
          v-model:value="keyword"
          placeholder="搜索用户名/昵称/邮箱"
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        />
        <n-button @click="handleSearch">搜索</n-button>
        <n-button @click="handleExport">导出</n-button>
        <n-upload
          :file-list="fileList"
          :max="1"
          accept=".xlsx,.xls"
          :show-file-list="false"
          @change="handleImport"
        >
          <n-button>导入</n-button>
        </n-upload>
        <n-button type="primary" @click="handleAdd">新增用户</n-button>
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
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="80">
        <n-form-item path="username" label="用户名">
          <n-input v-model:value="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item path="password" label="密码" v-if="!isEdit">
          <n-input v-model:value="form.password" type="password" placeholder="请输入密码" />
        </n-form-item>
        <n-form-item path="password" label="新密码" v-else>
          <n-input v-model:value="form.password" type="password" placeholder="留空则不修改密码" />
        </n-form-item>
        <n-form-item path="nickname" label="昵称">
          <n-input v-model:value="form.nickname" placeholder="请输入昵称" />
        </n-form-item>
        <n-form-item label="头像">
          <n-space vertical>
            <n-avatar
              v-if="form.avatar"
              :src="getAvatarUrl(form.avatar)"
              :size="60"
              round
            />
            <n-avatar
              v-else
              :size="60"
              round
            >
              {{ form.nickname?.[0] || form.username?.[0] || 'U' }}
            </n-avatar>
            <n-upload
              :file-list="avatarFileList"
              :max="1"
              accept="image/*"
              :show-file-list="false"
              @change="handleAvatarChange"
              @preview-file="handleAvatarPreview"
            >
              <n-button size="small">选择头像</n-button>
            </n-upload>
          </n-space>
        </n-form-item>
        <n-form-item path="roleId" label="角色">
          <n-select
            v-model:value="form.roleId"
            placeholder="请选择角色"
            :options="roleOptions"
            clearable
          />
        </n-form-item>
        <n-form-item path="email" label="邮箱">
          <n-input v-model:value="form.email" placeholder="请输入邮箱" />
        </n-form-item>
        <n-form-item path="phone" label="手机号">
          <n-input v-model:value="form.phone" placeholder="请输入手机号" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import {
  getUserList,
  getUserById,
  createUser,
  updateUser,
  deleteUser,
  changeUserStatus,
  importUsers,
  exportUsers,
  getRoleList,
  uploadAvatar
} from '@/api/user'
import { useMessage } from 'naive-ui'
import type { DataTableColumns, UploadFileInfo } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const submitLoading = ref(false)
const showModal = ref(false)
const isEdit = ref(false)
const formRef = ref()
const keyword = ref('')
const fileList = ref<UploadFileInfo[]>([])
const tableData = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const form = reactive({
  id: null as number | null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  status: 1,
  roleId: null as number | null
})

const avatarFileList = ref<UploadFileInfo[]>([])
// 保存原始头像路径，用于预览
const originalAvatar = ref('')

const roleOptions = ref<Array<{ label: string; value: number }>>([])

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  email: { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
}

const modalTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))

const columns: DataTableColumns = [
  {
    title: '头像',
    key: 'avatar',
    width: 80,
    render: (row: any) => {
      return h('n-avatar', {
        src: row.avatar ? getAvatarUrl(row.avatar) : undefined,
        size: 40,
        round: true
      }, {
        default: () => row.nickname?.[0] || row.username?.[0] || 'U'
      })
    }
  },
  { title: '用户名', key: 'username', width: 120 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '邮箱', key: 'email', width: 180 },
  { title: '手机号', key: 'phone', width: 120 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row: any) => {
      return h(
        'n-tag',
        { type: row.status === 1 ? 'success' : 'error' },
        { default: () => (row.status === 1 ? '启用' : '禁用') }
      )
    }
  },
  { title: '角色', key: 'roleName', width: 100 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row: any) => {
      return h('div', { style: 'display: flex; gap: 8px' }, [
        h(
          'n-button',
          {
            size: 'small',
            type: 'primary',
            onClick: () => {
              if (row.id) {
                handleEdit(row)
              } else {
                message.error('用户ID无效')
              }
            }
          },
          { default: () => '编辑' }
        ),
        h(
          'n-button',
          {
            size: 'small',
            type: row.status === 1 ? 'warning' : 'success',
            onClick: () => {
              if (row.id) {
                handleStatusChange(row.id, row.status === 1 ? 0 : 1)
              } else {
                message.error('用户ID无效')
              }
            }
          },
          { default: () => (row.status === 1 ? '禁用' : '启用') }
        ),
        h(
          'n-button',
          {
            size: 'small',
            type: 'error',
            onClick: () => {
              if (row.id) {
                handleDelete(row.id)
              } else {
                message.error('用户ID无效')
              }
            }
          },
          { default: () => '删除' }
        )
      ])
    }
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      current: pagination.page,
      size: pagination.pageSize,
      keyword: keyword.value || undefined
    })
    // 确保ID字段保持字符串格式，避免精度丢失
    tableData.value = res.data.records.map((item: any) => ({
      ...item,
      id: String(item.id || ''),
      roleId: item.roleId ? String(item.roleId) : null
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
    id: null,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    avatar: '',
    status: 1,
    roleId: null
  })
  avatarFileList.value = []
  showModal.value = true
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  try {
    if (!row.id) {
      message.error('用户ID无效')
      return
    }
    const res = await getUserById(row.id)
    // 确保avatar字段是服务器返回的路径，不是data URL
    const avatarPath = res.data.avatar && !res.data.avatar.startsWith('data:') 
      ? res.data.avatar 
      : (res.data.avatar || '')
    Object.assign(form, {
      id: res.data.id,
      username: res.data.username,
      password: '',
      nickname: res.data.nickname,
      email: res.data.email,
      phone: res.data.phone,
      avatar: avatarPath,
      status: res.data.status,
      roleId: res.data.roleId || null
    })
    // 保存原始头像路径
    originalAvatar.value = avatarPath
    avatarFileList.value = []
    showModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取用户信息失败')
  }
}

const handleAvatarChange = (options: { file: UploadFileInfo; fileList: UploadFileInfo[] }) => {
  avatarFileList.value = options.fileList
  // 如果选择了文件，创建预览URL
  if (options.fileList.length > 0 && options.fileList[0].file) {
    const file = options.fileList[0].file as File
    if (file.type.startsWith('image/')) {
      // 创建本地预览URL
      const reader = new FileReader()
      reader.onload = (e) => {
        // 临时设置预览URL，保存时会替换为服务器返回的URL
        form.avatar = e.target?.result as string
      }
      reader.readAsDataURL(file)
    }
  } else {
    // 如果清除了文件选择，恢复原始头像
    if (originalAvatar.value) {
      form.avatar = originalAvatar.value
    }
  }
}

const handleAvatarPreview = (file: UploadFileInfo) => {
  // 预览处理（如果需要）
}

const getAvatarUrl = (avatar: string | null | undefined) => {
  if (!avatar) return ''
  // 如果是data URL（base64），直接返回
  if (avatar.startsWith('data:')) {
    return avatar
  }
  // 如果是完整URL，直接返回
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar
  }
  // 路径格式：avatar/{username}/年/月/日/uuid.扩展名
  // 直接拼接为：/api/user/avatar/avatar/{username}/年/月/日/uuid.扩展名
  return `/api/user/avatar/${avatar}`
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    
    // 如果选择了新头像，先上传头像
    if (avatarFileList.value.length > 0) {
      const file = avatarFileList.value[0].file
      if (file) {
        const res = await uploadAvatar(file as File)
        // 确保使用服务器返回的路径，而不是data URL
        form.avatar = res.data
      }
    }
    
    // 如果是编辑模式且没有选择新头像，但form.avatar是data URL，需要恢复原始头像
    if (isEdit.value && avatarFileList.value.length === 0 && form.avatar && form.avatar.startsWith('data:')) {
      form.avatar = originalAvatar.value
    }
    
    if (isEdit.value) {
      await updateUser(form)
      message.success('更新成功')
    } else {
      await createUser(form)
      message.success('创建成功')
    }
    showModal.value = false
    avatarFileList.value = []
    originalAvatar.value = ''
    loadData()
  } catch (error: any) {
    message.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (id: number | string) => {
  try {
    if (!id) {
      message.error('用户ID无效')
      return
    }
    await deleteUser(String(id))
    message.success('删除成功')
    // 如果当前页没有数据了，返回上一页
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
    if (!id) {
      message.error('用户ID无效')
      return
    }
    await changeUserStatus(String(id), status)
    message.success('操作成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

const handleImport = async (options: { file: UploadFileInfo; fileList: UploadFileInfo[] }) => {
  const file = options.file.file
  if (!file) {
    return
  }
  try {
    loading.value = true
    await importUsers(file as File)
    message.success('导入成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '导入失败')
  } finally {
    loading.value = false
    fileList.value = []
  }
}

const handleExport = async () => {
  try {
    loading.value = true
    const res = await exportUsers()
    // res.data 已经是 Blob 类型
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '用户列表.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error.message || '导出失败')
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const res = await getRoleList()
    roleOptions.value = res.data.map((role: any) => ({
      label: role.roleName,
      value: role.id
    }))
  } catch (error) {
    message.error('加载角色列表失败')
  }
}

onMounted(() => {
  loadData()
  loadRoles()
})
</script>
