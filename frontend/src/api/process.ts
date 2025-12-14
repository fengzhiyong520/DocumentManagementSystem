import request from '@/utils/request'

export interface ProcessDefinition {
  id?: number
  name: string
  key: string
  description?: string
  status: number
  category?: string
  deploymentId?: string
  flowableDefinitionId?: string
}

export interface ProcessNode {
  id?: number
  processDefinitionId?: number
  nodeId: string
  nodeName: string
  nodeType: string
  sortOrder: number
  assignee?: string
  candidateUsers?: string
  candidateGroups?: string
  formKey?: string
  description?: string
}

// 获取启用的流程定义列表
export function getEnabledProcessDefinitions() {
  return request.get<ProcessDefinition[]>('/process-definition/enabled')
}

// 分页查询流程定义列表
export function pageProcessDefinition(params: { current?: number; size?: number; keyword?: string }) {
  return request.get('/process-definition/page', { params })
}

// 获取流程定义详情
export function getProcessDefinition(id: number) {
  return request.get<ProcessDefinition>(`/process-definition/${id}`)
}

export interface ProcessDefinitionWithNodes {
  processDefinition: ProcessDefinition
  nodes?: ProcessNode[]
}

// 保存流程定义
export function saveProcessDefinition(processDefinition: ProcessDefinition) {
  return request.post('/process-definition', processDefinition)
}

// 更新流程定义
export function updateProcessDefinition(processDefinition: ProcessDefinition) {
  return request.put('/process-definition', processDefinition)
}

// 获取流程定义详情（含节点）
export function getProcessDefinitionWithNodes(id: number) {
  return request.get<ProcessDefinitionWithNodes>(`/process-definition/${id}/with-nodes`)
}

// 删除流程定义
export function deleteProcessDefinition(id: number) {
  return request.delete(`/process-definition/${id}`)
}

// 部署流程定义
export function deployProcessDefinition(id: number) {
  return request.post(`/process-definition/${id}/deploy`)
}

// ========== 流程节点相关 API ==========

// 根据流程定义ID获取节点列表
export function getNodesByProcessDefinitionId(processDefinitionId: number) {
  return request.get<ProcessNode[]>(`/process-node/process-definition/${processDefinitionId}`)
}

// 保存流程节点列表
export function saveNodes(processDefinitionId: number, nodes: ProcessNode[]) {
  return request.post(`/process-node/process-definition/${processDefinitionId}`, nodes)
}

// 更新流程节点列表
export function updateNodes(processDefinitionId: number, nodes: ProcessNode[]) {
  return request.put(`/process-node/process-definition/${processDefinitionId}`, nodes)
}

// 删除流程节点
export function deleteNode(id: number) {
  return request.delete(`/process-node/${id}`)
}

// 删除流程定义的所有节点
export function deleteNodesByProcessDefinitionId(processDefinitionId: number) {
  return request.delete(`/process-node/process-definition/${processDefinitionId}`)
}

