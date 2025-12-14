import request from '@/utils/request'

export const getStatistics = () => {
  return request.get('/dashboard/statistics')
}

export const getStatisticsByCategory = () => {
  return request.get('/dashboard/statistics/by-category')
}

export const getStatisticsByDate = () => {
  return request.get('/dashboard/statistics/by-date')
}

export const getStatisticsByBorrowStatus = () => {
  return request.get('/dashboard/statistics/by-borrow-status')
}

