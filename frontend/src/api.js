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

/**
 * Connects to the live tracking WebSocket. The JWT is passed as a query parameter so
 * the backend can authenticate the session and only deliver updates for the caller's
 * company. Automatically reconnects with backoff until close() is called.
 */
export function connectTrackingSocket(onMessage, { onStatus } = {}) {
  let socket = null
  let closedByUser = false
  let retry = 0
  let timer = null

  function connect() {
    const token = localStorage.getItem('fleet_token')
    const url = `${WS_BASE}/ws/tracking${token ? `?token=${encodeURIComponent(token)}` : ''}`
    try {
      socket = new WebSocket(url)
    } catch {
      scheduleReconnect()
      return
    }
    onStatus?.('connecting')
    socket.onopen = () => {
      retry = 0
      onStatus?.('connected')
    }
    socket.onmessage = (event) => {
      try {
        onMessage(JSON.parse(event.data))
      } catch {
        /* ignore malformed messages */
      }
    }
    socket.onerror = () => onStatus?.('error')
    socket.onclose = () => {
      onStatus?.('disconnected')
      if (!closedByUser) scheduleReconnect()
    }
  }

  function scheduleReconnect() {
    const delay = Math.min(30000, 1000 * 2 ** retry)
    retry += 1
    clearTimeout(timer)
    timer = setTimeout(connect, delay)
  }

  connect()

  return {
    close() {
      closedByUser = true
      clearTimeout(timer)
      try {
        socket?.close()
      } catch {
        /* ignore */
      }
    },
  }
}

export default api
