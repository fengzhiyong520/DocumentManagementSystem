<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 20px">
      <h2>资料管理</h2>
      <n-space>
        <n-input
          v-model:value="keyword"
          placeholder="搜索标题/描述"
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        />
        <n-select
          v-model:value="category"
          placeholder="分类"
          clearable
          style="width: 150px"
          :options="categoryOptions"
          @update:value="handleSearch"
        />
        <n-select
          v-model:value="isPublic"
          placeholder="公开性"
          clearable
          style="width: 120px"
          :options="publicOptions"
          @update:value="handleSearch"
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
        <n-button type="primary" @click="handleAdd">上传资料</n-button>
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
        <n-form-item path="title" label="标题">
          <n-input v-model:value="form.title" placeholder="请输入标题" />
        </n-form-item>
        <n-form-item path="description" label="描述">
          <n-input v-model:value="form.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </n-form-item>
        <n-form-item path="category" label="分类">
          <n-select
            v-model:value="form.category"
            placeholder="请选择分类"
            :options="categoryOptions"
            clearable
          />
        </n-form-item>
        <n-form-item path="isPublic" label="是否公开">
          <n-radio-group v-model:value="form.isPublic">
            <n-radio :value="1">公开</n-radio>
            <n-radio :value="0">私有</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item path="status" label="状态">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">禁用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="!isEdit" path="file" label="文件">
          <n-upload
            v-model:file-list="uploadFileList"
            :max="1"
            accept="*/*"
            :show-file-list="true"
          >
            <n-button>选择文件</n-button>
          </n-upload>
        </n-form-item>
        <n-form-item v-if="isEdit && form.fileUrl" label="文件">
          <n-text>{{ form.fileName || '无文件名' }}</n-text>
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</n-button>
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
    <n-modal v-model:show="showDetailModal" preset="card" title="资料详情" style="width: 600px; max-height: 80vh">
      <div style="max-height: calc(80vh - 180px); overflow-y: auto; padding-bottom: 10px">
        <n-descriptions :column="1" bordered v-if="currentDocument">
        <n-descriptions-item label="标题">
          {{ currentDocument.title }}
        </n-descriptions-item>
        <n-descriptions-item label="描述">
          {{ currentDocument.description || '无描述' }}
        </n-descriptions-item>
        <n-descriptions-item label="分类">
          {{ categoryMap[currentDocument.category] || currentDocument.category || '-' }}
        </n-descriptions-item>
        <n-descriptions-item label="文件名" v-if="currentDocument.fileName">
          {{ currentDocument.fileName }}
        </n-descriptions-item>
        <n-descriptions-item label="文件大小" v-if="currentDocument.fileSize">
          {{ formatFileSize(currentDocument.fileSize) }}
        </n-descriptions-item>
        <n-descriptions-item label="是否公开">
          <n-tag :type="currentDocument.isPublic === 1 ? 'success' : 'default'">
            {{ currentDocument.isPublic === 1 ? '公开' : '私有' }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="状态">
          <n-tag :type="currentDocument.status === 1 ? 'success' : 'error'">
            {{ currentDocument.status === 1 ? '启用' : '禁用' }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="上传人">
          {{ currentDocument.uploadUserName || '未知' }}
        </n-descriptions-item>
        <n-descriptions-item label="创建时间" v-if="currentDocument.createTime">
          {{ currentDocument.createTime }}
        </n-descriptions-item>
      </n-descriptions>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showDetailModal = false">关闭</n-button>
          <n-button
            v-if="currentDocument && currentDocument.fileUrl && isImageFile(currentDocument.fileName || '')"
            type="info"
            @click="handleDetailPreview(currentDocument)"
          >
            预览文件
          </n-button>
          <n-button
            v-if="currentDocument && currentDocument.fileUrl && !isImageFile(currentDocument.fileName || '')"
            type="primary"
            @click="handleDetailDownload(currentDocument)"
          >
            下载文件
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, computed } from 'vue'
import {
  getDocumentList,
  getDocumentById,
  uploadDocument,
  updateDocument,
  deleteDocument,
  changeDocumentStatus,
  importDocuments,
  exportDocuments,
  downloadDocument,
  previewDocument
} from '@/api/document'
import { useMessage } from 'naive-ui'
import type { DataTableColumns, UploadFileInfo } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const submitLoading = ref(false)
const showModal = ref(false)
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
const isEdit = ref(false)
const formRef = ref()
const keyword = ref('')
const category = ref<string | null>(null)
const isPublic = ref<number | null>(null)
const fileList = ref<UploadFileInfo[]>([])
const uploadFileList = ref<UploadFileInfo[]>([])
const tableData = ref([])
const currentDocument = ref<any>(null)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const form = reactive({
  id: null as number | string | null,
  title: '',
  description: '',
  category: '',
  isPublic: 1,
  status: 1,
  fileUrl: ''
})

// 分类选项：接口传英文，界面显示中文
const categoryOptions = [
  { label: '图片', value: 'image' },
  { label: '文本', value: 'text' },
  { label: '文件', value: 'file' }
]
const publicOptions = [
  { label: '公开', value: 1 },
  { label: '私有', value: 0 }
]

// 分类映射：英文转中文
const categoryMap: Record<string, string> = {
  'image': '图片',
  'text': '文本',
  'file': '文件'
}

const rules = {
  title: { required: true, message: '请输入标题', trigger: 'blur' }
}

const modalTitle = computed(() => (isEdit.value ? '编辑资料' : '上传资料'))

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

const columns: DataTableColumns = [
  { title: '标题', key: 'title', width: 200 },
  {
    title: '分类',
    key: 'category',
    width: 120,
    render: (row: any) => categoryMap[row.category] || row.category || '-'
  },
  {
    title: '文件大小',
    key: 'fileSize',
    width: 120,
    render: (row: any) => formatFileSize(row.fileSize)
  },
  {
    title: '是否公开',
    key: 'isPublic',
    width: 100,
    render: (row: any) => {
      return h(
        'n-tag',
        { type: row.isPublic === 1 ? 'success' : 'default' },
        { default: () => (row.isPublic === 1 ? '公开' : '私有') }
      )
    }
  },
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
  { title: '上传人', key: 'uploadUserName', width: 120 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row: any) => {
      const buttons: any[] = [
        h(
          'n-button',
          {
            size: 'small',
            type: 'info',
            onClick: () => {
              if (row.id) {
                handleViewDetail(row)
              } else {
                message.error('资料ID无效')
              }
            }
          },
          { default: () => '详情' }
        ),
        h(
          'n-button',
          {
            size: 'small',
            type: 'primary',
            onClick: () => {
              if (row.id) {
                handleEdit(row)
              } else {
                message.error('资料ID无效')
              }
            }
          },
          { default: () => '编辑' }
        )
      ]
      
      // 如果类型不是文本，显示下载和预览按钮
      if (row.fileType !== 'text') {
        buttons.push(
          h(
            'n-button',
            {
              size: 'small',
              onClick: () => {
                if (row.id) {
                  handleDownload(row)
                } else {
                  message.error('资料ID无效')
                }
              }
            },
            { default: () => '下载' }
          ),
          h(
            'n-button',
            {
              size: 'small',
              type: 'info',
              onClick: () => {
                if (row.id) {
                  handlePreview(row)
                } else {
                  message.error('资料ID无效')
                }
              }
            },
            { default: () => '预览' }
          )
        )
      }
      
      buttons.push(
        h(
          'n-button',
          {
            size: 'small',
            type: row.status === 1 ? 'warning' : 'success',
            onClick: () => {
              if (row.id) {
                handleStatusChange(row.id, row.status === 1 ? 0 : 1)
              } else {
                message.error('资料ID无效')
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
                message.error('资料ID无效')
              }
            }
          },
          { default: () => '删除' }
        )
      )
      
      return h('div', { style: 'display: flex; gap: 8px' }, buttons)
    }
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDocumentList({
      current: pagination.page,
      size: pagination.pageSize,
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      isPublic: isPublic.value !== null ? isPublic.value : undefined
    })
    // 确保ID字段保持字符串格式，避免精度丢失
    tableData.value = res.data.records.map((item: any) => ({
      ...item,
      id: String(item.id || '')
    }))
    pagination.itemCount = res.data.total
    
    // 分类选项已固定，不需要从数据中提取
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
    title: '',
    description: '',
    category: '',
    isPublic: 1,
    status: 1,
    fileUrl: ''
  })
  uploadFileList.value = []
  showModal.value = true
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  try {
    if (!row.id) {
      message.error('资料ID无效')
      return
    }
    const res = await getDocumentById(row.id)
    Object.assign(form, {
      id: res.data.id,
      title: res.data.title,
      description: res.data.description || '',
      category: res.data.category || '',
      isPublic: res.data.isPublic,
      status: res.data.status,
      fileUrl: res.data.fileUrl || '',
      fileName: res.data.fileName || ''
    })
    uploadFileList.value = []
    showModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取资料信息失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    
    if (isEdit.value) {
      // 编辑模式
      await updateDocument(form)
      message.success('更新成功')
    } else {
      // 新增模式
      const formData = new FormData()
      // 文件非必传
      if (uploadFileList.value.length > 0) {
        formData.append('file', uploadFileList.value[0].file as File)
      }
      formData.append('title', form.title)
      formData.append('description', form.description || '')
      formData.append('category', form.category || '')
      formData.append('isPublic', form.isPublic.toString())
      await uploadDocument(formData)
      message.success('保存成功')
    }
    showModal.value = false
    loadData()
  } catch (error: any) {
    message.error(error.message || (isEdit.value ? '更新失败' : '上传失败'))
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (id: number | string) => {
  try {
    if (!id) {
      message.error('资料ID无效')
      return
    }
    await deleteDocument(String(id))
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
      message.error('资料ID无效')
      return
    }
    await changeDocumentStatus(String(id), status)
    message.success('操作成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

const handleDownload = async (row: any) => {
  try {
    if (!row.id) {
      message.error('资料ID无效')
      return
    }
    const res = await downloadDocument(row.id)
    // res.data 已经是 Blob 类型
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', row.fileName || 'file')
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

const handlePreview = async (row: any) => {
  try {
    if (!row.id) {
      message.error('资料ID无效')
      return
    }
    previewLoading.value = true
    previewDocumentId.value = row.id
    previewFileName.value = row.fileName || ''
    previewTitle.value = `文件预览 - ${row.title || row.fileName || '未知文件'}`
    zoomLevel.value = 1 // 重置缩放
    showPreviewModal.value = true
    
    const res = await previewDocument(row.id)
    // res.data 已经是 Blob 类型
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    previewUrl.value = url
    
    // 如果是文本文件，读取内容
    if (isTextFile(row.fileName || '')) {
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

const handleViewDetail = async (row: any) => {
  try {
    if (!row.id) {
      message.error('资料ID无效')
      return
    }
    const res = await getDocumentById(row.id)
    currentDocument.value = res.data
    showDetailModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取资料详情失败')
  }
}

const handleDetailDownload = async (item: any) => {
  try {
    if (!item.id) {
      message.error('资料ID无效')
      return
    }
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

const handleImport = async (options: { file: UploadFileInfo; fileList: UploadFileInfo[] }) => {
  const file = options.file.file
  if (!file) {
    return
  }
  try {
    loading.value = true
    await importDocuments(file as File)
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
    const res = await exportDocuments()
    // res.data 已经是 Blob 类型
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '资料列表.xlsx')
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

onMounted(() => {
  loadData()
})
</script>
