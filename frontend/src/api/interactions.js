import http from './http'

export const commentApi = {
  list: (broadcastId, params) => http.get(`/broadcasts/${broadcastId}/comments`, { params }).then((r) => r.data),
  write: (broadcastId, content) => http.post(`/broadcasts/${broadcastId}/comments`, { content }).then((r) => r.data),
  edit: (commentId, content) => http.patch(`/comments/${commentId}`, { content }).then((r) => r.data),
  remove: (commentId) => http.delete(`/comments/${commentId}`)
}

export const donationApi = {
  config: () => http.get('/donations/config').then((r) => r.data),
  createOrder: (body) => http.post('/donations/orders', body).then((r) => r.data),
  confirm: (body) => http.post('/donations/confirm', body).then((r) => r.data),
  cancel: (orderId, reason) => http.post(`/donations/orders/${orderId}/cancel`, null, { params: { reason } }),
  list: (broadcastId, params) => http.get(`/broadcasts/${broadcastId}/donations`, { params }).then((r) => r.data),
  summary: (broadcastId) => http.get(`/broadcasts/${broadcastId}/donations/summary`).then((r) => r.data)
}

export const fundApi = {
  list: (targetId, params) => http.get(`/targets/${targetId}/donations`, { params }).then((r) => r.data),
  summary: (targetId) => http.get(`/targets/${targetId}/donations/summary`).then((r) => r.data),
  mine: (params) => http.get('/donations/mine', { params }).then((r) => r.data),
  received: (params) => http.get('/donations/received', { params }).then((r) => r.data)
}

/** 신고와 운영 조치 */
export const reportApi = {
  report: (type, targetId, reason, detail) => http.post('/reports', { type, targetId, reason, detail }).then((r) => r.data),
  list: (params) => http.get('/moderation/reports', { params }).then((r) => r.data),
  resolve: (id, action, note) => http.post(`/moderation/reports/${id}/resolve`, { action, note }).then((r) => r.data),
  hideComment: (commentId) => http.post(`/moderation/comments/${commentId}/hide`),
  endBroadcast: (broadcastId, reason) => http.post(`/moderation/broadcasts/${broadcastId}/end`, { reason }).then((r) => r.data)
}
