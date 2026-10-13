import axios from 'axios'

const client = axios.create({ baseURL: '/api', withCredentials: true, timeout: 15000 })
client.interceptors.response.use(
  response => {
    const body = response.data
    if (!body || body.code !== 0) throw new Error(body?.message || '服务返回异常，请稍后重试')
    return body.data
  },
  error => {
    const status = error.response?.status
    if (status === 401) window.dispatchEvent(new CustomEvent('session-expired'))
    const message = error.response?.data?.message || (error.code === 'ECONNABORTED'
      ? '请求超时，请稍后重试' : error.response ? '操作失败，请稍后重试' : '无法连接服务，请检查后端是否已启动')
    return Promise.reject(Object.assign(new Error(message), { status }))
  },
)

export const api = {
  me: () => client.get('/auth/me'),
  login: data => client.post('/auth/login', data),
  logout: () => client.post('/auth/logout'),
  profile: data => client.put('/auth/profile', data),
  categories: () => client.get('/categories'),
  saveCategory: (id, data) => id ? client.put('/categories/' + id, data) : client.post('/categories', data),
  deleteCategory: id => client.delete('/categories/' + id),
  equipment: params => client.get('/equipment', { params }),
  saveEquipment: (id, data) => id ? client.put('/equipment/' + id, data) : client.post('/equipment', data),
  deleteEquipment: id => client.delete('/equipment/' + id),
  borrows: params => client.get('/borrows', { params }),
  borrow: equipmentId => client.post('/borrows', { equipmentId }),
  returnBorrow: id => client.post('/borrows/' + id + '/return'),
  users: () => client.get('/users'),
  saveUser: (id, data) => id ? client.put('/users/' + id, data) : client.post('/users', data),
  deleteUser: id => client.delete('/users/' + id),
  stats: () => client.get('/stats'),
}
