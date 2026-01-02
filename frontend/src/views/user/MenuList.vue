<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>菜单管理</h2>
      <n-space>
        <n-button type="primary" @click="handleAdd">新增菜单</n-button>
      </n-space>
    </n-space>
    
    <n-tree
      :data="menuTree"
      :render-label="renderLabel"
      :render-switcher-icon="renderSwitcherIcon"
      block-line
      :default-expand-all="true"
    />

    <!-- 新增/编辑对话框 -->
    <n-modal v-model:show="showModal" preset="card" :title="modalTitle" style="width: 600px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100">
        <n-form-item path="menuCode" label="菜单编码">
          <n-input v-model:value="form.menuCode" :disabled="isEdit" placeholder="请输入菜单编码（唯一标识）" />
        </n-form-item>
        <n-form-item path="menuName" label="菜单名称">
          <n-input v-model:value="form.menuName" placeholder="请输入菜单名称" />
        </n-form-item>
        <n-form-item path="menuType" label="菜单类型">
          <n-select v-model:value="form.menuType" :options="menuTypeOptions" placeholder="请选择菜单类型" />
        </n-form-item>
        <n-form-item path="parentId" label="父菜单">
          <n-tree-select
            v-model:value="form.parentId"
            :options="parentMenuOptions"
            key-field="id"
            label-field="menuName"
            children-field="children"
            placeholder="请选择父菜单（不选则为顶级菜单）"
            clearable
            :filterable="true"
          />
        </n-form-item>
        <n-form-item path="path" label="路由路径">
          <n-input v-model:value="form.path" placeholder="请输入路由路径（如：/user）" />
        </n-form-item>
        <n-form-item path="component" label="组件路径">
          <n-input v-model:value="form.component" placeholder="请输入组件路径（如：views/user/UserList.vue）" />
        </n-form-item>
        <n-form-item path="icon" label="图标">
          <n-input v-model:value="form.icon" placeholder="请输入图标名称（如：HomeOutline）" />
        </n-form-item>
        <n-form-item path="sortOrder" label="排序">
          <n-input-number v-model:value="form.sortOrder" placeholder="请输入排序" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item path="status" label="状态">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">禁用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item path="description" label="描述">
          <n-input v-model:value="form.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { getMenuTree, createMenu, updateMenu, deleteMenu, type Menu } from '@/api/menu'
import { useMessage } from 'naive-ui'
import type { FormInst, TreeOption } from 'naive-ui'
import { 
  CreateOutline, 
  TrashOutline, 
  PencilOutline,
  ChevronForwardOutline,
  ChevronDownOutline
} from '@vicons/ionicons5'

const message = useMessage()
const loading = ref(false)
const submitLoading = ref(false)
const showModal = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInst>()
const menuTree = ref<TreeOption[]>([])

const menuTypeOptions = [
  { label: '菜单', value: 'menu' },
  { label: '按钮', value: 'button' }
]

const form = reactive<Menu>({
  menuCode: '',
  menuName: '',
  menuType: 'menu',
  path: '',
  component: '',
  icon: '',
  parentId: 0,
  sortOrder: 0,
  status: 1,
  description: ''
})

const rules = {
  menuCode: { required: true, message: '请输入菜单编码', trigger: 'blur' },
  menuName: { required: true, message: '请输入菜单名称', trigger: 'blur' }
}

const modalTitle = computed(() => (isEdit.value ? '编辑菜单' : '新增菜单'))

const parentMenuOptions = computed(() => {
  const buildOptions = (treeOptions: TreeOption[], excludeId?: number): any[] => {
    return treeOptions
      .filter(option => {
        const menu = (option as any).menu as Menu
        // 编辑时排除当前菜单及其子菜单
        return !excludeId || menu.id !== excludeId
      })
      .map(option => {
        const menu = (option as any).menu as Menu
        return {
          id: menu.id,
          menuName: menu.menuName,
          children: option.children ? buildOptions(option.children as TreeOption[], excludeId) : undefined
        }
      })
  }
  // 编辑时排除当前菜单
  const excludeId = isEdit.value && form.id ? Number(form.id) : undefined
  return buildOptions(menuTree.value, excludeId)
})

const loadMenuTree = async () => {
  loading.value = true
  try {
    const res = await getMenuTree()
    menuTree.value = convertToTreeOptions(res.data || [])
  } catch (error: any) {
    message.error(error.message || '加载菜单失败')
  } finally {
    loading.value = false
  }
}

const convertToTreeOptions = (menus: Menu[]): TreeOption[] => {
  return menus.map(menu => ({
    key: String(menu.id),
    label: menu.menuName,
    menu: menu,
    children: menu.children ? convertToTreeOptions(menu.children) : undefined
  }))
}

const renderLabel = ({ option }: { option: TreeOption }) => {
  const menu = (option as any).menu as Menu
  return h('div', { style: 'display: flex; justify-content: space-between; align-items: center; width: 100%' }, [
    h('span', `${menu.menuName} (${menu.menuCode})`),
    h('div', { style: 'display: flex; gap: 8px' }, [
      h('n-button', {
        size: 'small',
        type: 'primary',
        onClick: (e: Event) => {
          e.stopPropagation()
          handleEdit(menu)
        }
      }, { default: () => '编辑' }),
      h('n-button', {
        size: 'small',
        type: 'error',
        onClick: (e: Event) => {
          e.stopPropagation()
          handleDelete(menu.id!)
        }
      }, { default: () => '删除' })
    ])
  ])
}

const renderSwitcherIcon = ({ expanded }: { expanded: boolean }) => {
  return h(expanded ? ChevronDownOutline : ChevronForwardOutline)
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    menuCode: '',
    menuName: '',
    menuType: 'menu',
    path: '',
    component: '',
    icon: '',
    parentId: 0,
    sortOrder: 0,
    status: 1,
    description: ''
  })
  showModal.value = true
}

const handleEdit = (menu: Menu) => {
  isEdit.value = true
  Object.assign(form, {
    id: menu.id,
    menuCode: menu.menuCode,
    menuName: menu.menuName,
    menuType: menu.menuType || 'menu',
    path: menu.path || '',
    component: menu.component || '',
    icon: menu.icon || '',
    parentId: menu.parentId || 0,
    sortOrder: menu.sortOrder || 0,
    status: menu.status ?? 1,
    description: menu.description || ''
  })
  showModal.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    
    if (isEdit.value) {
      await updateMenu(form)
      message.success('更新成功')
    } else {
      await createMenu(form)
      message.success('创建成功')
    }
    
    showModal.value = false
    loadMenuTree()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteMenu(id)
    message.success('删除成功')
    loadMenuTree()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

onMounted(() => {
  loadMenuTree()
})
</script>

