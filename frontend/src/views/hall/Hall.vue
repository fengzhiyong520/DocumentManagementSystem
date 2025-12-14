<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>大厅管理</h2>
      <n-space>
        <n-input
          v-model:value="fileName"
          placeholder="搜索文件名"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <n-input
          v-model:value="uploadUserName"
          placeholder="搜索创建人"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <n-button @click="handleSearch">搜索</n-button>
      </n-space>
    </n-space>
    
    <n-grid :cols="4" :x-gap="20" :y-gap="20" style="margin-top: 20px">
      <n-gi v-for="item in documentList" :key="item.id">
        <n-card :title="item.title" hoverable style="height: 100%; display: flex; flex-direction: column;">
          <template #header-extra>
            <n-tag :type="getStatusTagType(item.borrowStatus)" size="small">
              {{ getStatusText(item.borrowStatus) }}
            </n-tag>
          </template>
          <div style="flex: 1;">
            <n-space vertical :size="12">
              <div>
                <n-text strong>类型：</n-text>
                <n-text>{{ getCategoryName(item.category) }}</n-text>
              </div>
              <div>
                <n-text strong>创建人：</n-text>
                <n-text>{{ item.uploadUserName || '未知' }}</n-text>
              </div>
              <div v-if="item.category === 'image' && item.fileName">
                <n-text strong>图片名：</n-text>
                <n-text>{{ item.fileName }}</n-text>
              </div>
              <div v-if="item.category === 'file' && item.fileName">
                <n-text strong>文件名：</n-text>
                <n-text>{{ item.fileName }}</n-text>
              </div>
            </n-space>
          </div>
          <template #footer>
            <n-space style="min-height: 32px; display: flex; align-items: center;">
              <n-button
                v-if="item.borrowStatus === -1 || item.borrowStatus === null || item.borrowStatus === 2"
                @click="handleApply(item)"
              >
                申请查看
              </n-button>
              <n-button
                v-else-if="item.borrowStatus === 1"
                @click="handleViewDetail(item)"
              >
                查看详情
              </n-button>
              <span v-else style="visibility: hidden; display: inline-block; width: 1px;">占位</span>
            </n-space>
          </template>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 分页 -->
    <n-pagination
      v-model:page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :item-count="pagination.itemCount"
      :page-sizes="[12, 24, 48, 96]"
      show-size-picker
      style="margin-top: 20px; justify-content: center"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <!-- 申请对话框 -->
    <n-modal v-model:show="showApplyModal" preset="dialog" title="申请借阅" style="width: 500px">
      <n-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-placement="left" label-width="100">
        <n-form-item label="审批流程">
          <n-select
            v-model:value="applyForm.processDefinitionId"
            :options="processDefinitionOptions"
            placeholder="请选择审批流程（可选）"
            clearable
          />
        </n-form-item>
        <n-form-item path="reason" label="申请原因">
          <n-input
            v-model:value="applyForm.reason"
            type="textarea"
            placeholder="请输入申请原因"
            :rows="4"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showApplyModal = false">取消</n-button>
        <n-button type="primary" @click="handleSubmitApply" :loading="applyLoading">提交</n-button>
      </template>
    </n-modal>

    <!-- 文件预览对话框 -->
    <n-modal v-model:show="showPreviewModal" preset="card" :title="previewTitle" style="width: 90%; max-width: 1200px">
      <div 
        style="text-align: center; min-height: 400px; display: flex; align-items: center; justify-content: center; overflow: auto; position: relative;"
        @wheel.prevent="handleWheelZoom"
        @mouseenter="onPreviewMouseEnter"
        @mouseleave="onPreviewMouseLeave"
      >
        <div v-if="previewLoading" style="padding: 40px;">
          <n-spin size="large" />
        </div>
        <div v-else-if="previewUrl" style="position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;">
          <!-- 图片预览 -->
          <img
            v-if="isImageFile(previewFileName)"
            :src="previewUrl"
            :style="{
              maxWidth: '100%',
              maxHeight: '100%',
              objectFit: 'contain',
              transform: `scale(${zoomLevel})`,
              transition: 'transform 0.1s ease-out',
              cursor: zoomLevel > 1 ? 'grab' : 'default'
            }"
            alt="预览图片"
            draggable="false"
          />
          <!-- PDF预览 -->
          <div
            v-else-if="isPdfFile(previewFileName)"
            :style="{
              transform: `scale(${zoomLevel})`,
              transformOrigin: 'center center',
              transition: 'transform 0.1s ease-out',
              width: `${100 / zoomLevel}%`,
              height: `${70 / zoomLevel}vh`
            }"
          >
            <iframe
              :src="previewUrl"
              style="width: 100%; height: 100%; border: none;"
              frameborder="0"
            ></iframe>
          </div>
          <!-- 文本文件预览 -->
          <n-scrollbar v-else-if="isTextFile(previewFileName)" style="max-height: 70vh; width: 100%;">
            <pre 
              :style="{
                padding: '20px',
                whiteSpace: 'pre-wrap',
                wordWrap: 'break-word',
                fontFamily: 'monospace',
                fontSize: `${14 * zoomLevel}px`,
                transform: `scale(${zoomLevel})`,
                transformOrigin: 'top left',
                transition: 'transform 0.1s ease-out, font-size 0.1s ease-out'
              }"
            >{{ previewTextContent }}</pre>
          </n-scrollbar>
          <!-- 其他文件类型提示 -->
          <div v-else style="padding: 40px;">
            <n-result status="info" title="无法预览此文件类型" description="请下载文件后使用相应的应用程序打开">
              <template #footer>
                <n-button type="primary" @click="handlePreviewDownload">下载文件</n-button>
              </template>
            </n-result>
          </div>
        </div>
      </div>
      <template #footer>
        <n-space justify="space-between" style="width: 100%;">
          <n-space>
            <n-button size="small" @click="resetZoom">重置</n-button>
            <n-button size="small" @click="zoomOut">缩小</n-button>
            <n-button size="small" @click="zoomIn">放大</n-button>
            <n-text style="line-height: 32px; margin-left: 8px;">{{ Math.round(zoomLevel * 100) }}%</n-text>
          </n-space>
          <n-space>
            <n-button @click="closePreview">关闭</n-button>
            <n-button v-if="previewUrl && !isImageFile(previewFileName) && !isPdfFile(previewFileName) && !isTextFile(previewFileName)" type="primary" @click="handlePreviewDownload">下载文件</n-button>
          </n-space>
        </n-space>
      </template>
    </n-modal>

    <!-- 资料详情对话框 -->
    <n-modal v-model:show="showDetailModal" preset="card" title="资料详情" style="width: 600px">
      <n-descriptions :column="1" bordered v-if="currentDocument">
        <n-descriptions-item label="标题">
          {{ currentDocument.title }}
        </n-descriptions-item>
        <n-descriptions-item label="描述">
          {{ currentDocument.description || '无描述' }}
        </n-descriptions-item>
        <n-descriptions-item label="类型">
          {{ getCategoryName(currentDocument.category) }}
        </n-descriptions-item>
        <n-descriptions-item label="文件名" v-if="currentDocument.fileName">
          {{ currentDocument.fileName }}
        </n-descriptions-item>
        <n-descriptions-item label="文件大小" v-if="currentDocument.fileSize">
          {{ formatFileSize(currentDocument.fileSize) }}
        </n-descriptions-item>
        <n-descriptions-item label="创建人">
          {{ currentDocument.uploadUserName || '未知' }}
        </n-descriptions-item>
        <n-descriptions-item label="创建时间" v-if="currentDocument.createTime">
          {{ currentDocument.createTime }}
        </n-descriptions-item>
      </n-descriptions>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showDetailModal = false">关闭</n-button>
          <n-button
            v-if="currentDocument && currentDocument.fileUrl && currentDocument.fileType !== 'text'"
            type="info"
            @click="handleDetailPreview(currentDocument)"
          >
            预览文件
          </n-button>
          <n-button
            v-if="currentDocument && currentDocument.fileUrl"
            type="primary"
            @click="handleDownload(currentDocument)"
          >
            下载文件
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getPublicDocuments } from '@/api/document'
import { applyBorrow, checkApplied } from '@/api/borrow'
import { getDocumentById, downloadDocument, previewDocument } from '@/api/document'
import { getEnabledProcessDefinitions, type ProcessDefinition } from '@/api/process'
import { useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const applyLoading = ref(false)
const showApplyModal = ref(false)
const showDetailModal = ref(false)
const showPreviewModal = ref(false)
const previewLoading = ref(false)
const previewUrl = ref<string | null>(null)
const previewFileName = ref<string>('')
const previewTextContent = ref<string>('')
const previewTitle = ref('文件预览')
const previewDocumentId = ref<string | number | null>(null)
const zoomLevel = ref(1)
const minZoom = 0.5
const maxZoom = 3
const zoomStep = 0.1
const applyFormRef = ref<FormInst>()
const fileName = ref('')
const uploadUserName = ref('')
const currentDocument = ref<any>(null)
const currentApplyDocument = ref<any>(null)
const documentList = ref<any[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 12,
  itemCount: 0
})

const applyForm = reactive({
  processDefinitionId: null as number | null,
  reason: ''
})

const processDefinitionOptions = ref<Array<{ label: string; value: number }>>([])

const applyRules = {
  reason: { required: true, message: '请输入申请原因', trigger: 'blur' }
}

// 分类映射：英文转中文
const categoryMap: Record<string, string> = {
  'image': '图片',
  'text': '文本',
  'file': '文件'
}

const getCategoryName = (category: string) => {
  return categoryMap[category] || category || '-'
}

// 获取状态文本
const getStatusText = (status: number | null) => {
  if (status === null || status === -1) {
    return '待申请'
  }
  const statusMap: Record<number, string> = {
    0: '审批中',
    1: '申请通过',
    2: '申请未通过',
    3: '已归还',
    4: '已逾期'
  }
  return statusMap[status] || '未知'
}

// 获取状态标签类型
const getStatusTagType = (status: number | null): 'default' | 'info' | 'success' | 'warning' | 'error' => {
  if (status === null || status === -1) {
    return 'default'
  }
  const typeMap: Record<number, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    0: 'info',      // 审批中 - 蓝色
    1: 'success',   // 申请通过 - 绿色
    2: 'error',     // 申请未通过 - 红色
    3: 'default',   // 已归还 - 灰色
    4: 'warning'    // 已逾期 - 橙色
  }
  return typeMap[status] || 'default'
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPublicDocuments({
      current: pagination.page,
      size: pagination.pageSize,
      fileName: fileName.value || undefined,
      uploadUserName: uploadUserName.value || undefined
    })
    documentList.value = res.data.records.map((item: any) => ({
      ...item,
      id: String(item.id || ''),
      borrowStatus: null as number | null // 初始值，后续会检查
    }))
    pagination.itemCount = res.data.total
    
    // 检查每个资料的借阅状态
    for (const item of documentList.value) {
      try {
        const checkRes = await checkApplied(item.id)
        // 返回-1表示未申请，其他值表示状态（0-待审批，1-已批准，2-已拒绝，3-已归还，4-已逾期）
        item.borrowStatus = checkRes.data === -1 ? null : checkRes.data
      } catch (error) {
        console.error('检查申请状态失败', error)
        item.borrowStatus = null
      }
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
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

const handleApply = (item: any) => {
  currentApplyDocument.value = item
  applyForm.reason = ''
  applyForm.processDefinitionId = null
  showApplyModal.value = true
}

const handleSubmitApply = async () => {
  try {
    await applyFormRef.value?.validate()
    applyLoading.value = true
    
    await applyBorrow({
      documentId: currentApplyDocument.value.id,
      processDefinitionId: applyForm.processDefinitionId,
      reason: applyForm.reason
    })
    
    message.success('申请成功')
    showApplyModal.value = false
    // 刷新数据，更新申请状态
    loadData()
  } catch (error: any) {
    message.error(error.message || '申请失败')
  } finally {
    applyLoading.value = false
  }
}

const handleViewDetail = async (item: any) => {
  try {
    const res = await getDocumentById(item.id)
    currentDocument.value = res.data
    showDetailModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取资料详情失败')
  }
}

const handleDownload = async (item: any) => {
  try {
    const res = await downloadDocument(item.id)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', item.fileName || 'file')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('下载成功')
  } catch (error: any) {
    message.error(error.message || '下载失败')
  }
}

// 判断是否为图片文件
const isImageFile = (fileName: string) => {
  if (!fileName) return false
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return imageExtensions.includes(ext)
}

// 判断是否为PDF文件
const isPdfFile = (fileName: string) => {
  if (!fileName) return false
  return fileName.toLowerCase().endsWith('.pdf')
}

// 判断是否为文本文件
const isTextFile = (fileName: string) => {
  if (!fileName) return false
  const textExtensions = ['.txt', '.md', '.json', '.xml', '.csv', '.log', '.js', '.ts', '.vue', '.html', '.css']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return textExtensions.includes(ext)
}

// 鼠标进入预览区域
const onPreviewMouseEnter = () => {
  // 可以在这里添加一些逻辑
}

// 鼠标离开预览区域
const onPreviewMouseLeave = () => {
  // 可以在这里添加一些逻辑
}

// 处理鼠标滚轮缩放
const handleWheelZoom = (event: WheelEvent) => {
  // 按住 Ctrl 键时缩放（Windows/Linux）或直接滚轮缩放（Mac）
  if (event.ctrlKey || event.metaKey) {
    event.preventDefault()
    const delta = event.deltaY > 0 ? -zoomStep : zoomStep
    const newZoom = Math.max(minZoom, Math.min(maxZoom, zoomLevel.value + delta))
    zoomLevel.value = newZoom
  }
}

// 放大
const zoomIn = () => {
  const newZoom = Math.min(maxZoom, zoomLevel.value + zoomStep)
  zoomLevel.value = newZoom
}

// 缩小
const zoomOut = () => {
  const newZoom = Math.max(minZoom, zoomLevel.value - zoomStep)
  zoomLevel.value = newZoom
}

// 重置缩放
const resetZoom = () => {
  zoomLevel.value = 1
}

// 关闭预览
const closePreview = () => {
  showPreviewModal.value = false
  // 延迟释放URL，确保模态框关闭后再释放
  setTimeout(() => {
    if (previewUrl.value) {
      window.URL.revokeObjectURL(previewUrl.value)
      previewUrl.value = null
    }
    previewFileName.value = ''
    previewTextContent.value = ''
    previewDocumentId.value = null
    zoomLevel.value = 1 // 重置缩放
  }, 300)
}

// 预览文件下载
const handlePreviewDownload = async () => {
  if (!previewDocumentId.value) {
    message.error('资料ID无效')
    return
  }
  try {
    const res = await downloadDocument(previewDocumentId.value)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', previewFileName.value || 'file')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('下载成功')
  } catch (error: any) {
    message.error(error.message || '下载失败')
  }
}

// 详情模态框中的预览按钮
const handleDetailPreview = async (item: any) => {
  try {
    if (!item.id) {
      message.error('资料ID无效')
      return
    }
    previewLoading.value = true
    previewDocumentId.value = item.id
    previewFileName.value = item.fileName || ''
    previewTitle.value = `文件预览 - ${item.title || item.fileName || '未知文件'}`
    zoomLevel.value = 1 // 重置缩放
    showPreviewModal.value = true
    
    const res = await previewDocument(item.id)
    // res.data 已经是 Blob 类型
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    previewUrl.value = url
    
    // 如果是文本文件，读取内容
    if (isTextFile(item.fileName || '')) {
      try {
        const text = await blob.text()
        previewTextContent.value = text
      } catch (error) {
        console.error('读取文本文件失败', error)
        previewTextContent.value = '无法读取文件内容'
      }
    }
    
    previewLoading.value = false
  } catch (error: any) {
    previewLoading.value = false
    message.error(error.message || '预览失败')
    closePreview()
  }
}

const loadProcessDefinitions = async () => {
  try {
    const res = await getEnabledProcessDefinitions()
    processDefinitionOptions.value = res.data.map(item => ({
      label: item.name,
      value: item.id!
    }))
  } catch (error) {
    console.error('加载流程定义失败', error)
  }
}

onMounted(() => {
  loadData()
  loadProcessDefinitions()
})
</script>
