import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

// Attach access token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// On 401, try refresh token once, then redirect to login
let refreshLock = null
api.interceptors.response.use(
  (res) => {
    const payload = res.data
    if (payload && typeof payload === 'object' && 'code' in payload && 'data' in payload) {
      return { ...res, data: payload.data }
    }
    return res
  },
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      if (!refreshLock) {
        refreshLock = refreshAccessToken()
      }
      const newToken = await refreshLock
      refreshLock = null
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      }
      localStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return null
  try {
    const response = await axios.post('/api/v1/auth/refresh', { refresh_token: refreshToken })
    const payload = response.data?.data ?? response.data
    const accessToken = payload.accessToken ?? payload.access_token
    const nextRefreshToken = payload.refreshToken ?? payload.refresh_token
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', nextRefreshToken)
    return accessToken
  } catch {
    return null
  }
}

export default api
