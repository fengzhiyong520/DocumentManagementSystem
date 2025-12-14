import request from '@/utils/request'

// 根据流程实例ID查询流程历史
export function getProcessInstanceHistory(processInstanceId: string) {
  return request.get(`/process-history/instance/${processInstanceId}`)
}

// 查询流程实例的所有历史任务
export function getProcessInstanceTasks(processInstanceId: string) {
  return request.get(`/process-history/tasks/${processInstanceId}`)
}

// 查询用户的历史任务
export function getUserHistoryTasks(userId: string) {
  return request.get(`/process-history/user-tasks/${userId}`)
}

// 根据业务键查询流程历史
export function getProcessInstanceByBusinessKey(businessKey: string) {
  return request.get(`/process-history/business-key/${businessKey}`)
}

// 查询所有已完成的流程实例
export function getFinishedProcessInstances() {
  return request.get('/process-history/finished')
}

