<template>
  <div style="padding: 20px">
    <h2>首页统计</h2>
    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="20" style="margin-top: 20px">
      <n-gi>
        <n-statistic label="用户总数" :value="statistics.userCount">
          <template #prefix>
            <n-icon :component="PeopleOutline" />
          </template>
        </n-statistic>
      </n-gi>
      <n-gi>
        <n-statistic label="资料总数" :value="statistics.documentCount">
          <template #prefix>
            <n-icon :component="DocumentTextOutline" />
          </template>
        </n-statistic>
      </n-gi>
      <n-gi>
        <n-statistic label="待审批借阅" :value="statistics.pendingBorrowCount">
          <template #prefix>
            <n-icon :component="LibraryOutline" />
          </template>
        </n-statistic>
      </n-gi>
      <n-gi>
        <n-statistic label="今日新增资料" :value="statistics.todayDocumentCount">
          <template #prefix>
            <n-icon :component="AddCircleOutline" />
          </template>
        </n-statistic>
      </n-gi>
    </n-grid>

    <!-- 图表区域 -->
    <n-grid :cols="2" :x-gap="20" style="margin-top: 30px">
      <n-gi>
        <n-card title="最近7天资料新增趋势">
          <v-chart
            :option="dateChartOption"
            style="height: 300px"
            :loading="loading"
          />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="文件类型统计">
          <v-chart
            :option="categoryChartOption"
            style="height: 300px"
            :loading="loading"
          />
        </n-card>
      </n-gi>
    </n-grid>

    <n-grid :cols="1" style="margin-top: 20px">
      <n-gi>
        <n-card title="借阅状态统计">
          <v-chart
            :option="borrowStatusChartOption"
            style="height: 300px"
            :loading="loading"
          />
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import {
  getStatistics,
  getStatisticsByCategory,
  getStatisticsByDate,
  getStatisticsByBorrowStatus
} from '@/api/dashboard'
import { PeopleOutline, DocumentTextOutline, LibraryOutline, AddCircleOutline } from '@vicons/ionicons5'

// 注册 ECharts 组件
use([
  CanvasRenderer,
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const loading = ref(false)
const statistics = ref({
  userCount: 0,
  documentCount: 0,
  pendingBorrowCount: 0,
  todayDocumentCount: 0
})

const categoryData = ref<{ categories: string[]; counts: number[] }>({
  categories: [],
  counts: []
})

const dateData = ref<{ dates: string[]; counts: number[] }>({
  dates: [],
  counts: []
})

const borrowStatusData = ref<{ statusNames: string[]; counts: number[] }>({
  statusNames: [],
  counts: []
})

// 最近7天资料新增趋势图
const dateChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '15%',
    top: '10%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: dateData.value.dates,
    axisLabel: {
      rotate: 45
    }
  },
  yAxis: {
    type: 'value',
    name: '数量'
  },
  series: [
    {
      name: '新增资料数',
      type: 'bar',
      data: dateData.value.counts,
      barWidth: 40,
      itemStyle: {
        color: '#18a058'
      }
    }
  ]
}))

// 资料分类统计图
const categoryChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '15%',
    top: '10%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: categoryData.value.categories,
    axisLabel: {
      rotate: 45
    }
  },
  yAxis: {
    type: 'value',
    name: '数量'
  },
  series: [
    {
      name: '资料数量',
      type: 'bar',
      data: categoryData.value.counts,
      barWidth: 40,
      itemStyle: {
        color: '#2080f0'
      }
    }
  ]
}))

// 借阅状态统计图
const borrowStatusChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'shadow'
    }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '10%',
    top: '10%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: borrowStatusData.value.statusNames
  },
  yAxis: {
    type: 'value',
    name: '数量'
  },
  series: [
    {
      name: '借阅数量',
      type: 'bar',
      data: borrowStatusData.value.counts,
      barWidth: 40,
      itemStyle: {
        color: function (params: any) {
          const colors = ['#f0a020', '#18a058', '#d03050', '#2080f0', '#f56c6c']
          return colors[params.dataIndex] || '#2080f0'
        }
      }
    }
  ]
}))

const loadStatistics = async () => {
  try {
    loading.value = true
    const res = await getStatistics()
    statistics.value = res.data
  } catch (error) {
    console.error('加载统计数据失败', error)
  } finally {
    loading.value = false
  }
}

const loadCategoryStatistics = async () => {
  try {
    const res = await getStatisticsByCategory()
    categoryData.value = res.data
  } catch (error) {
    console.error('加载分类统计失败', error)
  }
}

const loadDateStatistics = async () => {
  try {
    const res = await getStatisticsByDate()
    dateData.value = res.data
  } catch (error) {
    console.error('加载日期统计失败', error)
  }
}

const loadBorrowStatusStatistics = async () => {
  try {
    const res = await getStatisticsByBorrowStatus()
    borrowStatusData.value = res.data
  } catch (error) {
    console.error('加载借阅状态统计失败', error)
  }
}

onMounted(() => {
  loadStatistics()
  loadCategoryStatistics()
  loadDateStatistics()
  loadBorrowStatusStatistics()
})
</script>
