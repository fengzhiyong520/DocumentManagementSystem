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
import { ref, h, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { updateCurrentUserInfo, uploadAvatar } from '@/api/user'
import { HomeOutline, PeopleOutline, DocumentTextOutline, LibraryOutline, StorefrontOutline, PersonOutline, LogOutOutline, SettingsOutline, CheckmarkCircleOutline, ShieldCheckmarkOutline, KeyOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import type { FormInst, UploadFileInfo } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const activeKey = ref(route.name as string)
const showProfileModal = ref(false)
const profileLoading = ref(false)
const profileFormRef = ref<FormInst>()
const avatarFileList = ref<UploadFileInfo[]>([])

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
    label: '退出登录',
    key: 'logout',
    icon: () => h(LogOutOutline)
  }
])

// 页面加载时获取用户信息（如果还没有）
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.getUserInfoAction()
  }
})

const menuOptions = [
  {
    label: '首页统计',
    key: 'Dashboard',
    icon: () => h(HomeOutline)
  },
  {
    label: '角色配置',
    key: 'Role',
    icon: () => h(ShieldCheckmarkOutline),
    children: [
      {
        label: '角色管理',
        key: 'Role',
        icon: () => h(ShieldCheckmarkOutline)
      },
      {
        label: '权限管理',
        key: 'Permission',
        icon: () => h(KeyOutline)
      }
    ]
  },
  {
    label: '用户管理',
    key: 'User',
    icon: () => h(PeopleOutline)
  },
  {
    label: '资料管理',
    key: 'Document',
    icon: () => h(DocumentTextOutline)
  },
  {
    label: '审批管理',
    key: 'Borrow',
    icon: () => h(LibraryOutline)
  },
  {
    label: '审批记录',
    key: 'BorrowHistory',
    icon: () => h(CheckmarkCircleOutline)
  },
  {
    label: '大厅管理',
    key: 'Hall',
    icon: () => h(StorefrontOutline)
  },
  {
    label: '流程配置',
    key: 'ProcessDefinition',
    icon: () => h(SettingsOutline)
  }
]

const handleMenuSelect = (key: string) => {
  router.push({ name: key })
}

const handleUserMenuSelect = (key: string) => {
  if (key === 'logout') {
    handleLogout()
  } else if (key === 'profile') {
    handleOpenProfile()
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
