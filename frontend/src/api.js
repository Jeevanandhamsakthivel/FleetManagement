import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('fleet_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response && error.response.status === 401 && !error.config?.url?.includes('/auth/login')) {
      localStorage.removeItem('fleet_token')
      localStorage.removeItem('fleet_user')
      if (!window.location.pathname.startsWith('/track')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export function errorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    'Request failed'
  )
}

const WS_BASE = (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + window.location.host

export function connectTrackingSocket(onMessage) {
  const socket = new WebSocket(`${WS_BASE}/ws/tracking`)
  socket.onmessage = (event) => {
    try {
      onMessage(JSON.parse(event.data))
    } catch {
      /* ignore malformed messages */
    }
  }
  return socket
}

export default api
