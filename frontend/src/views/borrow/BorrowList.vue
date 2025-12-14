<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>审批管理</h2>
    </n-space>
    
    <n-data-table
      :columns="columns"
      :data="tableData"
      :pagination="pagination"
      :loading="loading"
      :empty-description="emptyDescription"
      :scroll-x="1400"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <!-- 审批对话框 -->
    <n-modal v-model:show="showApproveModal" preset="dialog" :title="approveModalTitle" style="width: 500px">
      <n-form ref="approveFormRef" :model="approveForm" :rules="approveRules" label-placement="left" label-width="80">
        <n-form-item path="remark" label="审批备注">
          <n-input
            v-model:value="approveForm.remark"
            type="textarea"
            placeholder="请输入审批备注（可选）"
            :rows="4"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showApproveModal = false" :disabled="approveLoading || rejectLoading">取消</n-button>
        <n-button type="error" @click="handleReject" :loading="rejectLoading" :disabled="approveLoading || rejectLoading" style="margin-right: 8px">拒绝</n-button>
        <n-button type="primary" @click="handleApprove" :loading="approveLoading" :disabled="approveLoading || rejectLoading">批准</n-button>
      </template>
    </n-modal>

    <!-- 流程历史对话框 -->
    <n-modal 
      v-model:show="showHistoryModal" 
      preset="card" 
      title="流程执行历史" 
      style="width: 1500px; max-width: 95vw"
    >
      <n-spin :show="historyLoading">
        <div style="max-height: 70vh; overflow: auto;">
          <n-data-table
            :columns="historyColumns"
            :data="historyTasks"
            :pagination="false"
            :scroll-x="1400"
            :max-height="600"
          />
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showHistoryModal = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import { getBorrowList, approveBorrow } from '@/api/borrow'
import { getProcessInstanceTasks } from '@/api/processHistory'
import { useUserStore } from '@/stores/user'
import { useMessage } from 'naive-ui'
import type { DataTableColumns, FormInst } from 'naive-ui'

const message = useMessage()
const userStore = useUserStore()
const loading = ref(false)
const approveLoading = ref(false)
const rejectLoading = ref(false)
const showApproveModal = ref(false)
const approveFormRef = ref<FormInst>()
const statusFilter = ref<number | null>(null)
const currentApproveItem = ref<any>(null)
const approveAction = ref<'approve' | 'reject'>('approve')
const tableData = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const statusOptions = [
  { label: '待审批', value: 0 },
  { label: '已批准', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '已归还', value: 3 },
  { label: '已逾期', value: 4 }
]

const approveForm = reactive({
  remark: ''
})

const approveRules = {}

const approveModalTitle = computed(() => {
  if (!currentApproveItem.value) return '审批申请'
  return `审批申请 - ${currentApproveItem.value.documentTitle}`
})

const emptyDescription = computed(() => {
  if (statusFilter.value !== null) {
    return '暂无符合条件的申请记录'
  }
  return '暂无申请记录，当有人申请您创建的资料时会显示在这里'
})

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '待审批',
    1: '已批准',
    2: '已拒绝',
    3: '已归还',
    4: '已逾期'
  }
  return statusMap[status] || '未知'
}

// 获取状态标签类型
const getStatusTagType = (status: number): 'default' | 'info' | 'success' | 'warning' | 'error' => {
  const typeMap: Record<number, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    0: 'info',      // 待审批 - 蓝色
    1: 'success',   // 已批准 - 绿色
    2: 'error',     // 已拒绝 - 红色
    3: 'default',   // 已归还 - 灰色
    4: 'warning'    // 已逾期 - 橙色
  }
  return typeMap[status] || 'default'
}

// 格式化日期时间
const formatDateTime = (dateTime: string | null) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 19)
}

const columns: DataTableColumns = [
  { title: '资料标题', key: 'documentTitle', width: 200, ellipsis: { tooltip: true } },
  { title: '申请人', key: 'userName', width: 120 },
  { title: '申请原因', key: 'reason', width: 250, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row: any) => {
      return h(
        'n-tag',
        { type: getStatusTagType(row.status) },
        { default: () => getStatusText(row.status) }
      )
    }
  },
  { title: '申请时间', key: 'borrowTime', width: 180, render: (row: any) => formatDateTime(row.borrowTime) },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row: any) => {
      // 使用后端返回的 canApprove 字段，如果不存在则使用前端判断
      let canApprove = row.canApprove !== undefined ? row.canApprove : false
      
      // 如果后端没有返回 canApprove，使用前端判断（兼容旧逻辑）
      if (row.canApprove === undefined) {
        const currentUserId = userStore.userInfo?.id
        const currentUserIdStr = currentUserId ? String(currentUserId) : null
        
        if (row.status === 0) {
          if (!row.taskId) {
            // 没有流程任务，使用传统审批方式
            canApprove = true
          } else if (row.taskAssignee) {
            // 有流程任务且有分配人，检查是否是当前用户
            const assigneeStr = String(row.taskAssignee)
            canApprove = assigneeStr === currentUserIdStr
          }
        }
      }
      
      const buttons: any[] = []
      
      if (canApprove) {
        buttons.push(
          h(
            'n-button',
            {
              size: 'small',
              type: 'primary',
              onClick: () => {
                handleOpenApproveModal(row, 'approve')
              }
            },
            { default: () => '审批' }
          )
        )
      }
      
      // 如果有流程实例，显示查看历史按钮
      if (row.processInstanceId) {
        buttons.push(
          h(
            'n-button',
            {
              size: 'small',
              type: 'info',
              onClick: () => {
                handleViewHistory(row)
              }
            },
            { default: () => '查看历史' }
          )
        )
      }
      
      if (buttons.length > 0) {
        return h('div', { style: 'display: flex; gap: 8px' }, buttons)
      }
      
      return '-'
    }
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getBorrowList({
      current: pagination.page,
      size: pagination.pageSize,
      status: 0 // 审批管理只显示待审批的数据
    })
    tableData.value = res.data.records.map((item: any) => ({
      ...item,
      id: String(item.id || ''),
      // 确保这些字段存在
      taskName: item.taskName || null,
      taskAssigneeName: item.taskAssigneeName || item.taskAssignee || null,
      approverName: item.approverName || null,
      remark: item.remark || null
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

const handleOpenApproveModal = (item: any, action: 'approve' | 'reject') => {
  currentApproveItem.value = item
  approveAction.value = action
  approveForm.remark = ''
  showApproveModal.value = true
}

const handleApprove = async () => {
  try {
    approveLoading.value = true
    await approveBorrow(
      currentApproveItem.value.id,
      1, // 已批准
      approveForm.remark || undefined
    )
    message.success('审批成功')
    showApproveModal.value = false
    loadData()
  } catch (error: any) {
    message.error(error.message || '审批失败')
  } finally {
    approveLoading.value = false
  }
}

const handleReject = async () => {
  try {
    rejectLoading.value = true
    await approveBorrow(
      currentApproveItem.value.id,
      2, // 已拒绝
      approveForm.remark || undefined
    )
    message.success('已拒绝申请')
    showApproveModal.value = false
    loadData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  } finally {
    rejectLoading.value = false
  }
}

const showHistoryModal = ref(false)
const historyTasks = ref<any[]>([])
const historyLoading = ref(false)

const historyColumns: DataTableColumns = [
  { title: '任务名称', key: 'name', width: 200 },
  { 
    title: '审批人', 
    key: 'assigneeName', 
    width: 120,
    render: (row: any) => row.assigneeName || row.assignee || '-'
  },
  { title: '开始时间', key: 'startTime', width: 180, render: (row: any) => formatDateTime(row.startTime) },
  { title: '结束时间', key: 'endTime', width: 180, render: (row: any) => formatDateTime(row.endTime) },
  { title: '持续时间', key: 'durationInMillis', width: 150, render: (row: any) => {
    if (row.durationInMillis) {
      const seconds = Math.floor(row.durationInMillis / 1000)
      const minutes = Math.floor(seconds / 60)
      const hours = Math.floor(minutes / 60)
      if (hours > 0) {
        return `${hours}小时${minutes % 60}分钟`
      } else if (minutes > 0) {
        return `${minutes}分钟${seconds % 60}秒`
      } else {
        return `${seconds}秒`
      }
    }
    return '-'
  }},
  {
    title: '审批结果',
    key: 'approved',
    width: 100,
    render: (row: any) => {
      if (row.approved === true) {
        return h('n-tag', { type: 'success' }, { default: () => '已批准' })
      } else if (row.approved === false) {
        return h('n-tag', { type: 'error' }, { default: () => '已拒绝' })
      }
      return h('n-tag', { type: 'warning' }, { default: () => '待审批' })
    }
  },
  { 
    title: '审批备注', 
    key: 'remark', 
    width: 200, 
    ellipsis: { tooltip: true },
    render: (row: any) => row.remark || '-'
  },
  {
    title: '状态',
    key: 'deleteReason',
    width: 100,
    render: (row: any) => {
      if (row.endTime) {
        return h('n-tag', { type: 'success' }, { default: () => '已完成' })
      }
      return h('n-tag', { type: 'info' }, { default: () => '进行中' })
    }
  }
]

const handleViewHistory = async (row: any) => {
  if (!row.processInstanceId) {
    message.warning('该申请未使用流程审批')
    return
  }
  try {
    historyLoading.value = true
    const res = await getProcessInstanceTasks(row.processInstanceId)
    // 确保数据格式正确
    historyTasks.value = (res.data || []).map((task: any) => ({
      ...task,
      // 确保 approved 是布尔值或 null
      approved: task.approved === true ? true : task.approved === false ? false : null,
      // 确保 remark 是字符串或 null
      remark: task.remark || null,
      // 确保 endTime 存在时显示状态
      endTime: task.endTime || null
    }))
    showHistoryModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取流程历史失败')
  } finally {
    historyLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
