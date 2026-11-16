import http from './http'
import { deviceHeaders } from '../lib/device'

/** fandom-service: 종목과 응원 대상, 팬 등록, 하트, 랭킹, 경기, 신청, 운영 */
export const fandomApi = {
  config: () => http.get('/fandom/config').then((r) => r.data),
  sports: () => http.get('/sports').then((r) => r.data),
  leagueTargets: (leagueCode, sort = 'fans') => http.get(`/leagues/${leagueCode}/targets`, { params: { sort } }).then((r) => r.data),
  searchTargets: (q) => http.get('/targets', { params: { q } }).then((r) => r.data),
  target: (id) => http.get(`/targets/${id}`).then((r) => r.data),
  supporters: (id, period = 'SEASON', limit = 10) => http.get(`/targets/${id}/supporters`, { params: { period, limit } }).then((r) => r.data),
  rankHistory: (id, limit = 8) => http.get(`/targets/${id}/rank-history`, { params: { limit } }).then((r) => r.data),

  joinFan: (targetId, primary) => http.put(`/fans/${targetId}`, { primary }).then((r) => r.data),
  leaveFan: (targetId) => http.delete(`/fans/${targetId}`),
  myFans: () => http.get('/fans/me').then((r) => r.data),
  badges: (userIds) => http.get('/fans/badges', { params: { userIds: userIds.join(',') } }).then((r) => r.data),

  giveHeart: (body) => http.post('/hearts', body, { headers: deviceHeaders() }).then((r) => r.data),
  myHearts: () => http.get('/hearts/me').then((r) => r.data),

  ranking: (league = 'KBO', period = 'WEEKLY', sort = 'SCORE') => http.get('/rankings', { params: { league, period, sort } }).then((r) => r.data),
  overall: (limit = 10) => http.get('/rankings/overall', { params: { limit } }).then((r) => r.data),

  matches: (params) => http.get('/matches', { params }).then((r) => r.data),
  upcoming: (limit = 6) => http.get('/matches/upcoming', { params: { limit } }).then((r) => r.data),
  match: (id) => http.get(`/matches/${id}`).then((r) => r.data),

  requestTarget: (body) => http.post('/target-requests', body),
  voteRequest: (id) => http.post(`/target-requests/${id}/votes`).then((r) => r.data),
  openRequests: (params) => http.get('/target-requests', { params }).then((r) => r.data),
  myRequests: () => http.get('/target-requests/mine').then((r) => r.data)
}

export const adminApi = {
  requests: (params) => http.get('/admin/target-requests', { params }).then((r) => r.data),
  approve: (id) => http.post(`/admin/target-requests/${id}/approve`).then((r) => r.data),
  reject: (id, reason) => http.post(`/admin/target-requests/${id}/reject`, { reason }).then((r) => r.data),
  setVisible: (targetId, visible) => http.patch(`/admin/targets/${targetId}`, { visible }).then((r) => r.data),
  createMatch: (body) => http.post('/admin/matches', body).then((r) => r.data),
  updateMatch: (id, body) => http.patch(`/admin/matches/${id}`, body).then((r) => r.data),
  ops: () => http.get('/admin/ops').then((r) => r.data)
}

/** interaction-service: 시청자 수 */
export const viewerApi = {
  stats: (ids) => (ids.length ? http.get('/viewers', { params: { ids: ids.join(',') } }).then((r) => r.data) : Promise.resolve({})),
  target: (targetId) => http.get(`/viewers/targets/${targetId}`).then((r) => r.data)
}
