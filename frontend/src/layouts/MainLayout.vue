<template>
  <n-layout>
    <n-layout-header bordered>
      <div class="header-content">
        <h2>文件资料管理系统</h2>
        <div class="header-actions">
          <n-dropdown
            :options="userMenuOptions"
            @select="handleUserMenuSelect"
            trigger="click"
          >
            <div class="user-info" style="cursor: pointer;">
              <n-space vertical :size="4" align="center">
                <n-avatar
                  v-if="userStore.userInfo?.avatar"
                  :src="getAvatarUrl(userStore.userInfo.avatar)"
                  :size="40"
                  round
                />
                <n-avatar
                  v-else
                  :size="40"
                  round
                >
                  {{ userStore.userInfo?.nickname?.[0] || userStore.userInfo?.username?.[0] || 'U' }}
                </n-avatar>
                <n-text style="font-size: 12px;">
                  {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
                </n-text>
              </n-space>
            </div>
          </n-dropdown>
        </div>
      </div>
    </n-layout-header>
    <n-layout has-sider>
      <n-layout-sider bordered width="200" show-trigger>
        <n-menu
          v-model:value="activeKey"
          :options="menuOptions"
          @update:value="handleMenuSelect"
        />
      </n-layout-sider>
      <n-layout-content>
        <div class="content-wrapper">
          <router-view />
        </div>
      </n-layout-content>
    </n-layout>

    <!-- 站内好友对话框 -->
    <n-modal v-model:show="showFriendModal" preset="card" title="站内好友" style="width: 1000px">
      <div>
        <n-tabs v-model:value="friendTabValue" type="line">
          <n-tab-pane name="friends" tab="我的好友">
            <n-space justify="space-between" style="margin-bottom: 20px">
              <n-button type="primary" @click="handleRequestFriend">申请添加好友</n-button>
            </n-space>
            <n-data-table
              :columns="friendColumns"
              :data="friendTableData"
              :loading="friendLoading"
            />
          </n-tab-pane>
          <n-tab-pane name="requests" tab="收到的申请">
            <n-data-table
              :columns="friendRequestColumns"
              :data="friendRequestTableData"
              :loading="friendRequestLoading"
            />
          </n-tab-pane>
          <n-tab-pane name="sent-requests" tab="我发送的申请">
            <n-data-table
              :columns="sentFriendRequestColumns"
              :data="sentFriendRequestTableData"
              :loading="sentFriendRequestLoading"
            />
          </n-tab-pane>
        </n-tabs>
        <!-- 申请添加好友对话框 -->
        <n-modal v-model:show="showRequestFriendModal" title="申请添加好友" preset="dialog" style="width: 500px">
          <n-form ref="requestFriendFormRef" :model="requestFriendForm" :rules="requestFriendRules" label-placement="left" label-width="80">
            <n-form-item path="friendId" label="选择用户">
              <n-select
                v-model:value="requestFriendForm.friendId"
                placeholder="请选择要添加的用户"
                :options="userOptions"
                filterable
                :loading="userOptionsLoading"
                @search="handleUserSearch"
              />
            </n-form-item>
            <n-form-item path="remark" label="备注名称">
              <n-input v-model:value="requestFriendForm.remark" placeholder="请输入备注名称（可选）" />
            </n-form-item>
          </n-form>
          <template #action>
            <n-button @click="showRequestFriendModal = false">取消</n-button>
            <n-button type="primary" @click="handleRequestFriendSubmit" :loading="friendSubmitLoading">发送申请</n-button>
          </template>
        </n-modal>
        <!-- 编辑备注对话框 -->
        <n-modal v-model:show="showEditFriendModal" title="编辑备注" preset="dialog" style="width: 400px">
          <n-form ref="editFriendFormRef" :model="editFriendForm" label-placement="left" label-width="80">
            <n-form-item label="备注名称">
              <n-input v-model:value="editFriendForm.remark" placeholder="请输入备注名称" />
            </n-form-item>
          </n-form>
          <template #action>
            <n-button @click="showEditFriendModal = false">取消</n-button>
            <n-button type="primary" @click="handleEditFriendSubmit" :loading="friendSubmitLoading">确定</n-button>
          </template>
        </n-modal>
      </div>
    </n-modal>

    <!-- 修改个人信息对话框 -->
    <n-modal v-model:show="showProfileModal" preset="card" title="修改个人信息" style="width: 500px">
      <n-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-placement="left" label-width="80">
        <n-form-item label="头像">
          <n-space vertical>
            <n-avatar
              v-if="profileForm.avatar"
              :src="getAvatarUrl(profileForm.avatar)"
              :size="80"
              round
            />
            <n-avatar
              v-else
              :size="80"
              round
            >
              {{ profileForm.nickname?.[0] || 'U' }}
            </n-avatar>
            <n-upload
              :file-list="avatarFileList"
              :max="1"
              accept="image/*"
              :show-file-list="false"
              @change="handleAvatarChange"
            >
              <n-button>选择头像</n-button>
            </n-upload>
          </n-space>
        </n-form-item>
        <n-form-item path="nickname" label="昵称">
          <n-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showProfileModal = false">取消</n-button>
          <n-button type="primary" @click="handleUpdateProfile" :loading="profileLoading">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-layout>
</template>

<script setup lang="ts">
import { ref, h, onMounted, computed, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { updateCurrentUserInfo, uploadAvatar, getUserList } from '@/api/user'
import { getCurrentUserMenus, type Menu } from '@/api/menu'
import { getFriendList, getFriendRequestList, getSentFriendRequestList, requestFriend, acceptFriendRequest, rejectFriendRequest, cancelFriendRequest, updateFriend, deleteFriend, type FriendVO, type FriendForm } from '@/api/friend'
import { HomeOutline, PeopleOutline, DocumentTextOutline, LibraryOutline, StorefrontOutline, PersonOutline, LogOutOutline, SettingsOutline, CheckmarkCircleOutline, ShieldCheckmarkOutline, KeyOutline, PeopleCircleOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import type { FormInst, UploadFileInfo, MenuOption, DataTableColumns } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const activeKey = ref(route.name as string)
const showProfileModal = ref(false)
const profileLoading = ref(false)
const profileFormRef = ref<FormInst>()
const avatarFileList = ref<UploadFileInfo[]>([])

// 好友相关状态
const showFriendModal = ref(false)
const friendTabValue = ref('friends')
const friendLoading = ref(false)
const friendRequestLoading = ref(false)
const sentFriendRequestLoading = ref(false)
const friendSubmitLoading = ref(false)
const showRequestFriendModal = ref(false)
const showEditFriendModal = ref(false)
const friendTableData = ref<FriendVO[]>([])
const friendRequestTableData = ref<FriendVO[]>([])
const sentFriendRequestTableData = ref<FriendVO[]>([])
const userOptions = ref<Array<{ label: string; value: number | string; disabled?: boolean }>>([])
const userOptionsLoading = ref(false)
const requestFriendFormRef = ref<FormInst>()
const editFriendFormRef = ref<FormInst>()

const requestFriendForm = reactive<FriendForm>({
  friendId: undefined,
  remark: ''
})

const editFriendForm = reactive<FriendForm>({
  id: undefined,
  remark: ''
})

const requestFriendRules = {
  friendId: { required: true, message: '请选择要添加的用户', trigger: 'blur' }
}

const profileForm = ref({
  nickname: '',
  avatar: ''
})

const profileRules = {
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' }
}

// 获取头像URL（如果是相对路径，需要转换为完整URL）
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

// 用户菜单选项
const userMenuOptions = computed(() => [
  {
    label: '修改个人信息',
    key: 'profile',
    icon: () => h(PersonOutline)
  },
  {
    label: '站内好友',
    key: 'friend',
    icon: () => h(PeopleCircleOutline)
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(LogOutOutline)
  }
])

// 图标映射
const iconMap: Record<string, any> = {
  'HomeOutline': HomeOutline,
  'PeopleOutline': PeopleOutline,
  'DocumentTextOutline': DocumentTextOutline,
  'LibraryOutline': LibraryOutline,
  'StorefrontOutline': StorefrontOutline,
  'PersonOutline': PersonOutline,
  'LogOutOutline': LogOutOutline,
  'SettingsOutline': SettingsOutline,
  'CheckmarkCircleOutline': CheckmarkCircleOutline,
  'ShieldCheckmarkOutline': ShieldCheckmarkOutline,
  'KeyOutline': KeyOutline
}

// 动态菜单选项
const menuOptions = ref<MenuOption[]>([])

// 将菜单转换为 Naive UI 的菜单选项格式
const convertMenuToOptions = (menus: Menu[]): MenuOption[] => {
  return menus
    .filter(menu => menu.menuType === 'menu') // 只显示菜单类型，不显示按钮类型
    .map(menu => {
      const option: MenuOption = {
        label: menu.menuName,
        key: menu.menuCode || String(menu.id),
        path: menu.path
      }
      
      // 如果有图标，设置图标
      if (menu.icon) {
        const IconComponent = iconMap[menu.icon]
        if (IconComponent) {
          option.icon = () => h(IconComponent)
        }
      }
      
      // 如果有子菜单，递归处理
      if (menu.children && menu.children.length > 0) {
        option.children = convertMenuToOptions(menu.children)
      }
      
      return option
    })
}

// 加载用户菜单
const loadUserMenus = async () => {
  try {
    const res = await getCurrentUserMenus()
    menuOptions.value = convertMenuToOptions(res.data || [])
  } catch (error: any) {
    console.error('加载用户菜单失败:', error)
    // 如果加载失败，使用默认菜单
    menuOptions.value = [
      {
        label: '首页统计',
        key: 'Dashboard',
        icon: () => h(HomeOutline)
      }
    ]
  }
}

// 页面加载时获取用户信息和菜单
onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.getUserInfoAction()
  }
  // 加载用户菜单
  if (userStore.token) {
    await loadUserMenus()
  }
})

const handleMenuSelect = (key: string, item?: MenuOption) => {
  // 如果菜单项有 path，使用 path 跳转
  if (item?.path) {
    router.push(item.path)
  } else {
    // 否则使用 key 作为路由名称
    router.push({ name: key })
  }
}

const handleUserMenuSelect = (key: string) => {
  if (key === 'logout') {
    handleLogout()
  } else if (key === 'profile') {
    handleOpenProfile()
  } else if (key === 'friend') {
    handleOpenFriendModal()
  }
}

const handleLogout = () => {
  userStore.logout()
}

const handleOpenProfile = () => {
  if (userStore.userInfo) {
    // 确保avatar是服务器返回的路径，不是data URL
    const avatarPath = userStore.userInfo.avatar && !userStore.userInfo.avatar.startsWith('data:')
      ? userStore.userInfo.avatar
      : (userStore.userInfo.avatar || '')
    profileForm.value = {
      nickname: userStore.userInfo.nickname || '',
      avatar: avatarPath
    }
    avatarFileList.value = []
    showProfileModal.value = true
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
        profileForm.value.avatar = e.target?.result as string
      }
      reader.readAsDataURL(file)
    }
  }
}

const handleUpdateProfile = async () => {
  try {
    await profileFormRef.value?.validate()
    profileLoading.value = true
    
    let avatarPath = profileForm.value.avatar
    
    // 如果选择了新头像，先上传头像
    if (avatarFileList.value.length > 0) {
      const file = avatarFileList.value[0].file
      if (file) {
        const res = await uploadAvatar(file as File)
        // 确保使用服务器返回的路径，而不是data URL
        avatarPath = res.data
      }
    } else if (avatarPath && avatarPath.startsWith('data:')) {
      // 如果没有选择新头像，但avatar是data URL，说明是预览，需要保持原有头像
      // 从userStore中获取原有头像
      avatarPath = userStore.userInfo?.avatar || ''
    }
    
    // 更新用户信息
    await updateCurrentUserInfo({
      nickname: profileForm.value.nickname,
      avatar: avatarPath
    })
    
    message.success('更新成功')
    showProfileModal.value = false
    
    // 刷新用户信息
    await userStore.getUserInfoAction()
  } catch (error: any) {
    message.error(error.message || '更新失败')
  } finally {
    profileLoading.value = false
  }
}

// 好友相关函数
const friendColumns: DataTableColumns<FriendVO> = [
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
            onClick: () => handleEditFriend(row)
          },
          { default: () => '编辑备注' }
        ),
        h(
          'n-button',
          {
            size: 'small',
            type: 'error',
            onClick: () => handleDeleteFriend(row)
          },
          { default: () => '删除' }
        )
      ])
    }
  }
]

const friendRequestColumns: DataTableColumns<FriendVO> = [
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
  { title: '申请时间', key: 'createTime', width: 180 },
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
            onClick: () => handleAcceptFriendRequest(row)
          },
          { default: () => '同意' }
        ),
        h(
          'n-button',
          {
            size: 'small',
            type: 'error',
            onClick: () => handleRejectFriendRequest(row)
          },
          { default: () => '拒绝' }
        )
      ])
    }
  }
]

const sentFriendRequestColumns: DataTableColumns<FriendVO> = [
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
  { title: '申请时间', key: 'createTime', width: 180 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row: FriendVO) => {
      return h(
        'n-tag',
        { type: 'warning' },
        { default: () => '待同意' }
      )
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render: (row: FriendVO) => {
      return h('div', { style: 'display: flex; gap: 8px' }, [
        h(
          'n-button',
          {
            size: 'small',
            type: 'error',
            onClick: () => handleCancelFriendRequest(row)
          },
          { default: () => '取消申请' }
        )
      ])
    }
  }
]

const loadFriendData = async () => {
  friendLoading.value = true
  try {
    const res = await getFriendList()
    friendTableData.value = res.data || []
  } catch (error: any) {
    message.error(error.message || '加载好友列表失败')
  } finally {
    friendLoading.value = false
  }
}

const loadUserOptionsForFriend = async (keyword?: string) => {
  userOptionsLoading.value = true
  try {
    const res = await getUserList({
      current: 1,
      size: 100,
      keyword: keyword || undefined
    })
    const friendIds = new Set(friendTableData.value.map(f => String(f.friendId)))
    const currentUserId = String(userStore.userInfo?.id)
    userOptions.value = (res.data.records || [])
      .filter((user: any) => String(user.id) !== currentUserId) // 排除当前用户
      .map((user: any) => ({
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

const loadFriendRequestData = async () => {
  friendRequestLoading.value = true
  try {
    const res = await getFriendRequestList()
    friendRequestTableData.value = res.data || []
  } catch (error: any) {
    message.error(error.message || '加载好友申请列表失败')
  } finally {
    friendRequestLoading.value = false
  }
}

const loadSentFriendRequestData = async () => {
  sentFriendRequestLoading.value = true
  try {
    const res = await getSentFriendRequestList()
    sentFriendRequestTableData.value = res.data || []
  } catch (error: any) {
    message.error(error.message || '加载发送的申请列表失败')
  } finally {
    sentFriendRequestLoading.value = false
  }
}

const handleOpenFriendModal = async () => {
  showFriendModal.value = true
  await loadFriendData()
  await loadFriendRequestData()
  await loadSentFriendRequestData()
}

const handleUserSearch = (value: string) => {
  if (value) {
    loadUserOptionsForFriend(value)
  }
}

const handleRequestFriend = async () => {
  if (friendTableData.value.length === 0 && !friendLoading.value) {
    await loadFriendData()
  }
  showRequestFriendModal.value = true
  Object.assign(requestFriendForm, {
    friendId: undefined,
    remark: ''
  })
  await loadUserOptionsForFriend()
}

const handleRequestFriendSubmit = async () => {
  if (!requestFriendFormRef.value) return
  try {
    await requestFriendFormRef.value.validate()
    friendSubmitLoading.value = true
    await requestFriend(requestFriendForm)
    message.success('申请已发送')
    showRequestFriendModal.value = false
    await loadSentFriendRequestData()
    await loadUserOptionsForFriend()
  } catch (error: any) {
    if (error.message && !error.message.includes('验证')) {
      message.error(error.message)
    }
  } finally {
    friendSubmitLoading.value = false
  }
}

const handleCancelFriendRequest = async (row: FriendVO) => {
  try {
    await cancelFriendRequest(row.id)
    message.success('已取消申请')
    await loadSentFriendRequestData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

const handleAcceptFriendRequest = async (row: FriendVO) => {
  try {
    await acceptFriendRequest(row.id)
    message.success('已同意')
    await loadFriendRequestData()
    await loadFriendData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

const handleRejectFriendRequest = async (row: FriendVO) => {
  try {
    await rejectFriendRequest(row.id)
    message.success('已拒绝')
    await loadFriendRequestData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

const handleEditFriend = (row: FriendVO) => {
  showEditFriendModal.value = true
  editFriendForm.id = row.id
  editFriendForm.remark = row.remark || ''
}

const handleEditFriendSubmit = async () => {
  friendSubmitLoading.value = true
  try {
    await updateFriend(editFriendForm)
    message.success('更新成功')
    showEditFriendModal.value = false
    await loadFriendData()
  } catch (error: any) {
    message.error(error.message || '更新失败')
  } finally {
    friendSubmitLoading.value = false
  }
}

const handleDeleteFriend = async (row: FriendVO) => {
  try {
    await deleteFriend(row.friendId)
    message.success('删除成功')
    await loadFriendData()
    await loadUserOptionsForFriend()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}
</script>

<style scoped>
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 100%;
}

.user-info {
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.content-wrapper {
  padding: 20px;
  height: 100%;
  overflow: auto;
}
</style>
