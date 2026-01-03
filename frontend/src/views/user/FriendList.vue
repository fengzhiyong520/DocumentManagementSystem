<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>站内好友</h2>
      <n-button type="primary" @click="handleAdd">添加好友</n-button>
    </n-space>
    <n-data-table
      :columns="columns"
      :data="tableData"
      :loading="loading"
    />
    <!-- 添加好友对话框 -->
    <n-modal v-model:show="showAddModal" title="添加好友" preset="dialog" style="width: 500px">
      <n-form ref="addFormRef" :model="addForm" :rules="addRules" label-placement="left" label-width="80">
        <n-form-item path="friendId" label="选择用户">
          <n-select
            v-model:value="addForm.friendId"
            placeholder="请选择要添加的用户"
            :options="userOptions"
            filterable
            :loading="userOptionsLoading"
            @search="handleUserSearch"
          />
        </n-form-item>
        <n-form-item path="remark" label="备注名称">
          <n-input v-model:value="addForm.remark" placeholder="请输入备注名称（可选）" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showAddModal = false">取消</n-button>
        <n-button type="primary" @click="handleAddSubmit" :loading="submitLoading">确定</n-button>
      </template>
    </n-modal>
    <!-- 编辑备注对话框 -->
    <n-modal v-model:show="showEditModal" title="编辑备注" preset="dialog" style="width: 400px">
      <n-form ref="editFormRef" :model="editForm" label-placement="left" label-width="80">
        <n-form-item label="备注名称">
          <n-input v-model:value="editForm.remark" placeholder="请输入备注名称" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showEditModal = false">取消</n-button>
        <n-button type="primary" @click="handleEditSubmit" :loading="submitLoading">确定</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { getFriendList, addFriend, updateFriend, deleteFriend, type FriendVO, type FriendForm } from '@/api/friend'
import { getUserList, type UserForm } from '@/api/user'
import { useMessage } from 'naive-ui'
import type { DataTableColumns, FormInst } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const submitLoading = ref(false)
const showAddModal = ref(false)
const showEditModal = ref(false)
const addFormRef = ref<FormInst>()
const editFormRef = ref<FormInst>()
const tableData = ref<FriendVO[]>([])
const userOptions = ref<Array<{ label: string; value: number | string }>>([])
const userOptionsLoading = ref(false)

const addForm = reactive<FriendForm>({
  friendId: undefined,
  remark: ''
})

const editForm = reactive<FriendForm>({
  id: undefined,
  remark: ''
})

const addRules = {
  friendId: { required: true, message: '请选择要添加的用户', trigger: 'blur' }
}

// 获取头像URL
const getAvatarUrl = (avatar: string | null | undefined) => {
  if (!avatar) return ''
  if (avatar.startsWith('data:')) return avatar
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  return `/api/user/avatar/${avatar}`
}

const columns: DataTableColumns<FriendVO> = [
  {
    title: '头像',
    key: 'friendAvatar',
    width: 80,
    render: (row: FriendVO) => {
      return h('n-avatar', {
        src: row.friendAvatar ? getAvatarUrl(row.friendAvatar) : undefined,
        size: 40,
        round: true
      }, {
        default: () => row.friendNickname?.[0] || row.friendUsername?.[0] || 'U'
      })
    }
  },
  { title: '用户名', key: 'friendUsername', width: 120 },
  { title: '昵称', key: 'friendNickname', width: 120 },
  { title: '备注', key: 'remark', width: 120 },
  { title: '邮箱', key: 'friendEmail', width: 180 },
  { title: '手机号', key: 'friendPhone', width: 120 },
  {
    title: '状态',
    key: 'friendStatus',
    width: 100,
    render: (row: FriendVO) => {
      return h(
        'n-tag',
        { type: row.friendStatus === 1 ? 'success' : 'error' },
        { default: () => (row.friendStatus === 1 ? '启用' : '禁用') }
      )
    }
  },
  { title: '添加时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row: FriendVO) => {
      return h('div', { style: 'display: flex; gap: 8px' }, [
        h(
          'n-button',
          {
            size: 'small',
            type: 'primary',
            onClick: () => handleEdit(row)
          },
          { default: () => '编辑备注' }
        ),
        h(
          'n-button',
          {
            size: 'small',
            type: 'error',
            onClick: () => handleDelete(row)
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
    const res = await getFriendList()
    tableData.value = res.data || []
  } catch (error: any) {
    message.error(error.message || '加载好友列表失败')
  } finally {
    loading.value = false
  }
}

const loadUserOptions = async (keyword?: string) => {
  userOptionsLoading.value = true
  try {
    const res = await getUserList({
      current: 1,
      size: 100,
      keyword: keyword || undefined
    })
    // 过滤掉已经是好友的用户（这里简化处理，实际可以优化）
    const friendIds = new Set(tableData.value.map(f => String(f.friendId)))
    userOptions.value = (res.data.records || []).map((user: any) => ({
      label: `${user.nickname || user.username} (${user.username})`,
      value: user.id,
      disabled: friendIds.has(String(user.id))
    }))
  } catch (error: any) {
    message.error(error.message || '加载用户列表失败')
  } finally {
    userOptionsLoading.value = false
  }
}

const handleUserSearch = (value: string) => {
  if (value) {
    loadUserOptions(value)
  }
}

const handleAdd = async () => {
  // 确保好友列表已加载
  if (tableData.value.length === 0 && !loading.value) {
    await loadData()
  }
  showAddModal.value = true
  Object.assign(addForm, {
    friendId: undefined,
    remark: ''
  })
  await loadUserOptions()
}

const handleAddSubmit = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    submitLoading.value = true
    await addFriend(addForm)
    message.success('添加成功')
    showAddModal.value = false
    await loadData()
    await loadUserOptions() // 刷新用户选项
  } catch (error: any) {
    if (error.message && !error.message.includes('验证')) {
      message.error(error.message)
    }
  } finally {
    submitLoading.value = false
  }
}

const handleEdit = (row: FriendVO) => {
  showEditModal.value = true
  editForm.id = row.id
  editForm.remark = row.remark || ''
}

const handleEditSubmit = async () => {
  submitLoading.value = true
  try {
    await updateFriend(editForm)
    message.success('更新成功')
    showEditModal.value = false
    await loadData()
  } catch (error: any) {
    message.error(error.message || '更新失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: FriendVO) => {
  try {
    await deleteFriend(row.friendId)
    message.success('删除成功')
    await loadData()
    await loadUserOptions() // 刷新用户选项
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

