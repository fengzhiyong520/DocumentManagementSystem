<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>流程配置管理</h2>
      <n-space>
        <n-input
          v-model:value="keyword"
          placeholder="搜索流程名称、Key或描述"
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        />
        <n-button @click="handleSearch">搜索</n-button>
        <n-button type="primary" @click="handleAdd">新增流程</n-button>
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

    <!-- 流程配置对话框 -->
    <n-modal v-model:show="showModal" preset="card" :title="modalTitle" style="width: 600px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100">
        <n-form-item path="name" label="流程名称">
          <n-input v-model:value="form.name" placeholder="请输入流程名称" />
        </n-form-item>
        <n-form-item path="key" label="流程Key">
          <n-input v-model:value="form.key" placeholder="请输入流程Key（唯一标识）" :disabled="isEdit" />
        </n-form-item>
        <n-form-item path="description" label="流程描述">
          <n-input v-model:value="form.description" type="textarea" placeholder="请输入流程描述" :rows="3" />
        </n-form-item>
        <n-form-item path="status" label="状态">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">禁用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item path="category" label="流程分类">
          <n-input v-model:value="form.category" placeholder="请输入流程分类" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 节点配置对话框 -->
    <n-modal v-model:show="showNodeModal" preset="card" title="配置流程节点" style="width: 1000px">
      <n-space vertical style="width: 100%">
        <n-space justify="space-between">
          <n-button type="primary" @click="handleAddNode">新增节点</n-button>
        </n-space>
        <n-data-table
          :columns="nodeColumns"
          :data="nodeForm"
          :pagination="false"
          :bordered="true"
          max-height="500px"
        />
      </n-space>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showNodeModal = false">取消</n-button>
          <n-button type="primary" @click="handleSaveNodes" :loading="nodeSaveLoading">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 节点编辑对话框 -->
    <n-modal v-model:show="showNodeEditModal" preset="card" :title="nodeEditTitle" style="width: 600px">
      <n-form ref="nodeFormRef" :model="currentNode" :rules="nodeRules" label-placement="left" label-width="120">
        <n-form-item path="nodeId" label="节点ID">
          <n-input v-model:value="currentNode.nodeId" placeholder="请输入节点ID（唯一标识，建议使用英文）" />
        </n-form-item>
        <n-form-item path="nodeName" label="节点名称">
          <n-input v-model:value="currentNode.nodeName" placeholder="请输入节点名称" />
        </n-form-item>
        <n-form-item path="nodeType" label="节点类型">
          <n-select v-model:value="currentNode.nodeType" :options="nodeTypeOptions" placeholder="请选择节点类型" />
        </n-form-item>
        <n-form-item path="sortOrder" label="排序">
          <n-input-number v-model:value="currentNode.sortOrder" placeholder="请输入排序" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item path="assignee" label="审批人">
          <n-select
            v-model:value="currentNode.assignee"
            :options="userOptions"
            placeholder="请选择审批人（单选）"
            clearable
            filterable
            :loading="userLoading"
            @search="handleUserSearch"
          />
        </n-form-item>
        <n-form-item path="candidateUsers" label="候选审批人">
          <n-select
            v-model:value="currentNode.candidateUsers"
            :options="userOptions"
            placeholder="请选择候选审批人（可多选）"
            multiple
            clearable
            filterable
            :loading="userLoading"
            @search="handleUserSearch"
            tag
          />
        </n-form-item>
        <n-form-item path="candidateGroups" label="候选审批组">
          <n-input v-model:value="currentNode.candidateGroups" placeholder="候选审批组（角色ID，多个用逗号分隔）" />
        </n-form-item>
        <n-form-item path="description" label="节点描述">
          <n-input v-model:value="currentNode.description" type="textarea" placeholder="请输入节点描述" :rows="3" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showNodeEditModal = false">取消</n-button>
          <n-button type="primary" @click="handleNodeSubmit" :loading="nodeSubmitLoading">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { 
  saveProcessDefinition, 
  updateProcessDefinition, 
  getProcessDefinition, 
  pageProcessDefinition, 
  deleteProcessDefinition,
  deployProcessDefinition,
  getNodesByProcessDefinitionId,
  saveNodes,
  updateNodes,
  deleteNode,
  type ProcessDefinition, 
  type ProcessNode 
} from '@/api/process'
import { getUserList } from '@/api/user'
import { useMessage } from 'naive-ui'
import type { DataTableColumns, FormInst } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const submitLoading = ref(false)
const showModal = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInst>()
const tableData = ref<ProcessDefinition[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const form = reactive<ProcessDefinition>({
  id: null,
  name: '',
  key: '',
  description: '',
  status: 1,
  category: ''
})

// 节点管理相关
const showNodeModal = ref(false)
const showNodeEditModal = ref(false)
const nodeSaveLoading = ref(false)
const nodeSubmitLoading = ref(false)
const currentNodeDefinitionId = ref<number | null>(null)
const nodeForm = ref<ProcessNode[]>([])
const currentNode = reactive<ProcessNode>({
  id: undefined,
  processDefinitionId: undefined,
  nodeId: '',
  nodeName: '',
  nodeType: 'userTask',
  sortOrder: 0,
  assignee: undefined,
  candidateUsers: undefined,
  candidateGroups: undefined,
  description: undefined
})
const currentNodeIndex = ref<number>(-1)
const nodeFormRef = ref<FormInst>()
const userOptions = ref<Array<{ label: string; value: string }>>([])
const userLoading = ref(false)

const nodeTypeOptions = [
  { label: '开始事件', value: 'startEvent' },
  { label: '用户任务', value: 'userTask' },
  { label: '结束事件', value: 'endEvent' }
]

const nodeRules = {
  nodeId: { required: true, message: '请输入节点ID', trigger: 'blur' },
  nodeName: { required: true, message: '请输入节点名称', trigger: 'blur' },
  nodeType: { required: true, message: '请选择节点类型', trigger: 'change' },
  sortOrder: { required: true, type: 'number', message: '请输入排序', trigger: 'blur' }
}

const nodeEditTitle = computed(() => {
  if (currentNodeIndex.value >= 0 && currentNode.id) {
    return '编辑节点'
  }
  return '新增节点'
})

const nodeColumns: DataTableColumns<ProcessNode> = [
  { title: '节点ID', key: 'nodeId', width: 150 },
  { title: '节点名称', key: 'nodeName', width: 150 },
  {
    title: '节点类型',
    key: 'nodeType',
    width: 120,
    render: (row) => {
      const typeMap: Record<string, string> = {
        startEvent: '开始事件',
        userTask: '用户任务',
        endEvent: '结束事件'
      }
      return typeMap[row.nodeType] || row.nodeType
    }
  },
  { title: '排序', key: 'sortOrder', width: 80 },
  {
    title: '审批人',
    key: 'assignee',
    width: 120,
    render: (row) => {
      if (row.assignee) {
        const user = userOptions.value.find(u => u.value === String(row.assignee))
        return user ? user.label : row.assignee
      }
      return '-'
    }
  },
  {
    title: '候选审批人',
    key: 'candidateUsers',
    width: 150,
    ellipsis: { tooltip: true },
    render: (row) => {
      if (row.candidateUsers) {
        const userIds = String(row.candidateUsers).split(',').filter(Boolean)
        const users = userIds.map(id => {
          const user = userOptions.value.find(u => u.value === id.trim())
          return user ? user.label : id.trim()
        })
        return users.join(', ') || '-'
      }
      return '-'
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: (row, index) => {
      return h('div', { style: 'display: flex; gap: 8px' }, [
        h('n-button', {
          size: 'small',
          type: 'primary',
          onClick: () => handleEditNode(row, index)
        }, { default: () => '编辑' }),
        h('n-button', {
          size: 'small',
          type: 'error',
          onClick: () => handleRemoveNode(index)
        }, { default: () => '删除' })
      ])
    }
  }
]

const rules = {
  name: { required: true, message: '请输入流程名称', trigger: 'blur' },
  key: { required: true, message: '请输入流程Key', trigger: 'blur' }
}

const modalTitle = computed(() => (isEdit.value ? '编辑流程' : '新增流程'))

const columns: DataTableColumns<ProcessDefinition> = [
  { title: '流程名称', key: 'name', width: 200 },
  { title: '流程Key', key: 'key', width: 150 },
  { title: '描述', key: 'description', width: 250, ellipsis: { tooltip: true } },
  { title: '分类', key: 'category', width: 120 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => {
      return h('n-tag', { type: row.status === 1 ? 'success' : 'default' }, {
        default: () => row.status === 1 ? '启用' : '禁用'
      })
    }
  },
  {
    title: '发布状态',
    key: 'deploymentStatus',
    width: 120,
    render: (row) => {
      const isDeployed = !!(row.deploymentId || row.flowableDefinitionId)
      return h('n-tag', { type: isDeployed ? 'success' : 'warning' }, {
        default: () => isDeployed ? '已发布' : '未发布'
      })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 250,
    render: (row) => {
      return h('div', { style: 'display: flex; gap: 8px' }, [
        h('n-button', {
          size: 'small',
          type: 'info',
          onClick: () => handleManageNodes(row)
        }, { default: () => '配置节点' }),
        h('n-button', {
          size: 'small',
          type: 'success',
          onClick: () => handleDeploy(row)
        }, { default: () => '部署' }),
        h('n-button', {
          size: 'small',
          type: 'primary',
          onClick: () => handleEdit(row)
        }, { default: () => '编辑' }),
        h('n-button', {
          size: 'small',
          type: 'error',
          onClick: () => handleDelete(row)
        }, { default: () => '删除' })
      ])
    }
  }
]

const keyword = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageProcessDefinition({
      current: pagination.page,
      size: pagination.pageSize,
      keyword: keyword.value || undefined
    })
    tableData.value = res.data.records
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

const handleDelete = async (row: ProcessDefinition) => {
  try {
    await deleteProcessDefinition(row.id!)
    message.success('删除成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    key: '',
    description: '',
    status: 1,
    category: ''
  })
  showModal.value = true
}

const handleEdit = async (row: ProcessDefinition) => {
  isEdit.value = true
  try {
    const res = await getProcessDefinition(row.id!)
    if (res.data) {
      Object.assign(form, {
        id: res.data.id,
        name: res.data.name,
        key: res.data.key,
        description: res.data.description || '',
        status: res.data.status,
        category: res.data.category || ''
      })
    } else {
      Object.assign(form, {
        id: row.id,
        name: row.name,
        key: row.key,
        description: row.description || '',
        status: row.status,
        category: row.category || ''
      })
    }
    showModal.value = true
  } catch (error: any) {
    message.error(error.message || '加载流程定义失败')
  }
}

const handleManageNodes = async (row: ProcessDefinition) => {
  currentNodeDefinitionId.value = row.id!
  try {
    const res = await getNodesByProcessDefinitionId(row.id!)
    nodeForm.value = (res.data || []).map((node: ProcessNode) => ({
      ...node,
      assignee: node.assignee ? String(node.assignee) : undefined,
      candidateUsers: node.candidateUsers ? String(node.candidateUsers) : undefined
    }))
    // 加载用户列表
    await loadUserOptions()
    showNodeModal.value = true
  } catch (error: any) {
    message.error(error.message || '加载节点失败')
  }
}

const loadUserOptions = async (keyword?: string) => {
  try {
    userLoading.value = true
    const res = await getUserList({
      current: 1,
      size: 1000, // 获取所有用户
      keyword: keyword || undefined
    })
    userOptions.value = (res.data.records || []).map((user: any) => ({
      label: `${user.nickname || user.username} (${user.username})`,
      value: String(user.id)
    }))
  } catch (error: any) {
    message.error('加载用户列表失败')
  } finally {
    userLoading.value = false
  }
}

const handleUserSearch = (query: string) => {
  if (query) {
    loadUserOptions(query)
  } else {
    loadUserOptions()
  }
}

const handleAddNode = () => {
  currentNodeIndex.value = -1
  Object.assign(currentNode, {
    id: undefined,
    processDefinitionId: currentNodeDefinitionId.value,
    nodeId: '',
    nodeName: '',
    nodeType: 'userTask',
    sortOrder: nodeForm.value.length,
    assignee: undefined,
    candidateUsers: undefined,
    candidateGroups: undefined,
    description: undefined
  })
  showNodeEditModal.value = true
}

const handleEditNode = (row: ProcessNode, index: number) => {
  currentNodeIndex.value = index
  // 处理候选审批人：如果是逗号分隔的字符串，转换为数组
  let candidateUsersValue: string[] | undefined = undefined
  if (row.candidateUsers) {
    const candidateUsersStr = Array.isArray(row.candidateUsers) 
      ? row.candidateUsers.join(',') 
      : String(row.candidateUsers)
    candidateUsersValue = candidateUsersStr.split(',').filter(Boolean).map(s => s.trim())
  }
  
  Object.assign(currentNode, {
    id: row.id,
    processDefinitionId: row.processDefinitionId || currentNodeDefinitionId.value,
    nodeId: row.nodeId || '',
    nodeName: row.nodeName || '',
    nodeType: row.nodeType || 'userTask',
    sortOrder: row.sortOrder || 0,
    assignee: row.assignee ? String(row.assignee) : undefined,
    candidateUsers: candidateUsersValue,
    candidateGroups: row.candidateGroups || undefined,
    description: row.description || undefined
  })
  showNodeEditModal.value = true
}

const handleNodeSubmit = async () => {
  try {
    await nodeFormRef.value?.validate()
    nodeSubmitLoading.value = true
    
    // 处理审批人：转换为字符串
    const assignee = currentNode.assignee
      ? String(currentNode.assignee)
      : undefined
    
    // 处理候选审批人：如果是数组，转换为逗号分隔的字符串
    const candidateUsers = currentNode.candidateUsers
      ? (Array.isArray(currentNode.candidateUsers) 
          ? currentNode.candidateUsers.join(',') 
          : String(currentNode.candidateUsers))
      : undefined
    
    const nodeData: ProcessNode = {
      ...currentNode,
      processDefinitionId: currentNodeDefinitionId.value,
      assignee: assignee,
      candidateUsers: candidateUsers
    }
    
    if (currentNodeIndex.value >= 0 && currentNode.id) {
      // 编辑已有节点
      nodeForm.value[currentNodeIndex.value] = { ...nodeData }
    } else {
      // 新增节点
      nodeForm.value.push({ ...nodeData })
    }
    
    showNodeEditModal.value = false
    message.success(currentNodeIndex.value >= 0 ? '编辑成功' : '新增成功')
  } catch (error: any) {
    message.error(error.message || '操作失败')
  } finally {
    nodeSubmitLoading.value = false
  }
}

const handleRemoveNode = (index: number) => {
  const node = nodeForm.value[index]
  if (node.id) {
    // 已有节点，标记为删除（实际删除在保存时处理）
    nodeForm.value.splice(index, 1)
  } else {
    // 新增节点，直接删除
    nodeForm.value.splice(index, 1)
  }
  message.success('删除成功')
}

const handleSaveNodes = async () => {
  try {
    if (!currentNodeDefinitionId.value) {
      message.error('流程定义ID不能为空')
      return
    }
    
    // 验证节点配置
    if (nodeForm.value.length === 0) {
      message.error('至少需要配置一个节点')
      return
    }
    
    // 检查是否有开始事件和结束事件
    const hasStartEvent = nodeForm.value.some(n => n.nodeType === 'startEvent')
    const hasEndEvent = nodeForm.value.some(n => n.nodeType === 'endEvent')
    const hasUserTask = nodeForm.value.some(n => n.nodeType === 'userTask')
    
    if (!hasStartEvent) {
      message.error('至少需要配置一个开始事件')
      return
    }
    
    if (!hasEndEvent) {
      message.error('至少需要配置一个结束事件')
      return
    }
    
    if (!hasUserTask) {
      message.error('至少需要配置一个用户任务（审批节点）')
      return
    }
    
    nodeSaveLoading.value = true
    
    // 准备保存的数据：区分新增和编辑
    const nodesToSave = nodeForm.value.map(node => ({
      ...node,
      processDefinitionId: currentNodeDefinitionId.value,
      // 转换用户ID为字符串
      assignee: node.assignee ? String(node.assignee) : undefined,
      candidateUsers: node.candidateUsers 
        ? (Array.isArray(node.candidateUsers) 
            ? node.candidateUsers.join(',') 
            : String(node.candidateUsers))
        : undefined
    }))
    
    await updateNodes(currentNodeDefinitionId.value, nodesToSave)
    message.success('保存成功')
    showNodeModal.value = false
  } catch (error: any) {
    message.error(error.message || '保存失败')
  } finally {
    nodeSaveLoading.value = false
  }
}

const handleDeploy = async (row: ProcessDefinition) => {
  try {
    await deployProcessDefinition(row.id!)
    message.success('部署成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '部署失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    
    // 构建提交数据，确保必填字段有值
    const submitData: ProcessDefinition = {
      id: form.id || undefined,
      name: form.name?.trim() || '',
      key: form.key?.trim() || '',
      description: form.description?.trim() || '',
      status: form.status ?? 1,
      category: form.category?.trim() || ''
    }
    
    if (isEdit.value) {
      await updateProcessDefinition(submitData)
      message.success('更新成功')
    } else {
      // 新增时移除 id
      delete submitData.id
      await saveProcessDefinition(submitData)
      message.success('保存成功')
    }
    
    showModal.value = false
    loadData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

