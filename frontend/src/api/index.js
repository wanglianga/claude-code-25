import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || '网络请求失败'
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (location.pathname !== '/login') location.href = '/login'
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(new Error(msg))
  }
)

export default http
