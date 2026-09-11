import { defineStore } from 'pinia'
import http from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    role: (s) => s.user?.role || '',
    roleName: (s) => {
      const map = {
        PARENT: '家长',
        TEACHER: '老师',
        HEALTH: '保健老师',
        DIRECTOR: '园长',
        FRONTDESK: '前台',
        KITCHEN: '厨房'
      }
      return map[s.user?.role] || ''
    }
  },
  actions: {
    async login(username, password) {
      const data = await http.post('/auth/login', { username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
