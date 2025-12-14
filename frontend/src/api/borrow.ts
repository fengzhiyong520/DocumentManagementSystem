import request from '@/utils/request'

export interface BorrowQuery {
  current?: number
  size?: number
  status?: number
}

export const applyBorrow = (data: any) => {
  return request.post('/borrow/apply', data)
}

export const approveBorrow = (id: number, status: number, remark?: string) => {
  return request.put(`/borrow/approve/${id}`, null, {
    params: { status, remark }
  })
}

export const returnDocument = (id: number) => {
  return request.put(`/borrow/return/${id}`)
}

export const getBorrowList = (params: BorrowQuery) => {
  return request.get('/borrow/page', { params })
}

export const checkApplied = (documentId: number | string) => {
  return request.get(`/borrow/check/${documentId}`)
}

export const getApprovedRecords = (params: BorrowQuery) => {
  return request.get('/borrow/approved-records', { params })
}

