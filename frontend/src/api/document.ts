import request from '@/utils/request'

export interface DocumentQuery {
  current?: number
  size?: number
  keyword?: string
  category?: string
  isPublic?: number
}

export const getDocumentList = (params: DocumentQuery) => {
  return request.get('/document/page', { params })
}

export const getDocumentById = (id: number | string) => {
  return request.get(`/document/${id}`)
}

export const uploadDocument = (data: FormData) => {
  return request.post('/document/upload', data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updateDocument = (data: any) => {
  return request.put('/document', data)
}

export const deleteDocument = (id: number | string) => {
  return request.delete(`/document/${id}`)
}

export const changeDocumentStatus = (id: number | string, status: number) => {
  return request.put(`/document/status/${id}`, null, {
    params: { status }
  })
}

export const importDocuments = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/document/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const exportDocuments = () => {
  return request.get('/document/export', {
    responseType: 'blob'
  })
}

export const getPublicDocuments = (params: DocumentQuery & { fileName?: string; uploadUserName?: string }) => {
  return request.get('/hall/documents', { params })
}

export const downloadDocument = (id: number | string) => {
  return request.get(`/document/download/${id}`, {
    responseType: 'blob'
  })
}

export const previewDocument = (id: number | string) => {
  return request.get(`/document/preview/${id}`, {
    responseType: 'blob'
  })
}

