<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>审批记录</h2>
      <n-space>
        <n-select
          v-model:value="statusFilter"
          placeholder="筛选状态"
          clearable
          style="width: 150px"
          :options="statusOptions"
          @update:value="handleSearch"
        />
        <n-button @click="handleSearch">搜索</n-button>
      </n-space>
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
import { getApprovedRecords } from '@/api/borrow'
import { getProcessInstanceTasks } from '@/api/processHistory'
import { useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const statusFilter = ref<number | null>(null)
const tableData = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const statusOptions = [
  { label: '已批准', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '已归还', value: 3 },
  { label: '已逾期', value: 4 }
]

const emptyDescription = computed(() => {
  return loading.value ? '加载中...' : '暂无数据'
})

// 获取状态文本（优先使用 processStatus，如果没有则使用 status）
const getStatusText = (row: any) => {
  // 如果有流程状态，优先显示流程状态
  if (row.processStatus) {
    return row.processStatus
  }
  // 否则使用传统状态
  const statusMap: Record<number, string> = {
    0: '待审批',
    1: '已批准',
    2: '已拒绝',
    3: '已归还',
    4: '已逾期'
  }
  return statusMap[row.status] || '未知'
}

// 获取状态标签类型
const getStatusTagType = (row: any) => {
  // 如果有流程状态，根据流程状态返回类型
  if (row.processStatus) {
    if (row.processStatus === '待审批') {
      return 'warning'
    } else if (row.processStatus === '审批中') {
      return 'info'
    } else if (row.processStatus === '审批完成') {
      return 'success'
    }
  }
  // 否则使用传统状态类型
  const typeMap: Record<number, 'success' | 'error' | 'warning' | 'info'> = {
    0: 'warning',
    1: 'success',
    2: 'error',
    3: 'info',
    4: 'error'
  }
  return typeMap[row.status] || 'default'
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
        { type: getStatusTagType(row) },
        { default: () => getStatusText(row) }
      )
    }
  },
  { title: '申请时间', key: 'borrowTime', width: 180, render: (row: any) => formatDateTime(row.borrowTime) },
  { title: '审批备注', key: 'remark', width: 200, ellipsis: { tooltip: true }, render: (row: any) => row.remark || '-' },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render: (row: any) => {
      const buttons = []
      
      if (row.processInstanceId) {
        buttons.push(
          h('n-button', {
            size: 'small',
            type: 'info',
            onClick: () => handleViewHistory(row)
          }, { default: () => '查看历史' })
        )
      }
      
      if (buttons.length > 0) {
        return h('div', { style: 'display: flex; gap: 8px' }, buttons)
      }
      
      return '-'
    }
  }
]

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

const formatDateTime = (dateTime: string | null | undefined) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getApprovedRecords({
      current: pagination.page,
      size: pagination.pageSize,
      status: statusFilter.value !== null ? statusFilter.value : undefined
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

