import request from '@/utils/request'

// 获取所有运行中的流程实例
export function getRunningProcessInstances() {
  return request.get('/process-monitor/running')
}

// 获取流程实例的当前任务
export function getProcessInstanceTasks(processInstanceId: string) {
  return request.get(`/process-monitor/tasks/${processInstanceId}`)
}

// 获取流程实例的变量
export function getProcessInstanceVariables(processInstanceId: string) {
  return request.get(`/process-monitor/variables/${processInstanceId}`)
}

// 获取流程统计信息
export function getProcessStatistics() {
  return request.get('/process-monitor/statistics')
}

// 根据业务键获取流程实例
export function getProcessInstanceByBusinessKey(businessKey: string) {
  return request.get(`/process-monitor/business-key/${businessKey}`)
}

