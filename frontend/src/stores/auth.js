import { defineStore } from 'pinia'
import api from '../api/client.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isLoggedIn: !!localStorage.getItem('accessToken'),
  }),

  actions: {
    async sendSms(phone) {
      await api.post('/auth/send-sms-code', { phone })
    },

    async login(phone, code) {
      const { data } = await api.post('/auth/login', { phone, code })
      const accessToken = data.accessToken ?? data.access_token
      const refreshToken = data.refreshToken ?? data.refresh_token
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      this.isLoggedIn = true
      this.user = data.user
      return data
    },

    async fetchUser() {
      const { data } = await api.get('/user/me')
      this.user = data
    },

    logout() {
      localStorage.clear()
      this.user = null
      this.isLoggedIn = false
      this.$router?.push('/login')
    },
  },
})
