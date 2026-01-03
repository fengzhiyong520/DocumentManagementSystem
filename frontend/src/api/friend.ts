import request from '@/utils/request'

export interface FriendForm {
  id?: number | string
  friendId: number | string
  remark?: string
}

export interface FriendVO {
  id: number | string
  userId: number | string
  friendId: number | string
  remark?: string
  friendUsername?: string
  friendNickname?: string
  friendEmail?: string
  friendPhone?: string
  friendAvatar?: string
  friendStatus?: number
  applicantUsername?: string
  applicantNickname?: string
  applicantEmail?: string
  applicantPhone?: string
  applicantAvatar?: string
  applicantStatus?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export const getFriendList = () => {
  return request.get('/friend/list')
}

export const getFriendRequestList = () => {
  return request.get('/friend/requests')
}

export const getSentFriendRequestList = () => {
  return request.get('/friend/sent-requests')
}

export const cancelFriendRequest = (requestId: number | string) => {
  return request.delete(`/friend/request/${requestId}`)
}

export const getFriendById = (id: number | string) => {
  return request.get(`/friend/${id}`)
}

export const requestFriend = (data: FriendForm) => {
  return request.post('/friend/request', data)
}

export const acceptFriendRequest = (requestId: number | string) => {
  return request.put(`/friend/accept/${requestId}`)
}

export const rejectFriendRequest = (requestId: number | string) => {
  return request.put(`/friend/reject/${requestId}`)
}

export const updateFriend = (data: FriendForm) => {
  return request.put('/friend', data)
}

export const deleteFriend = (friendId: number | string) => {
  return request.delete(`/friend/${friendId}`)
}

