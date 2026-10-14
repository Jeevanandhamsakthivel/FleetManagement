import { createContext, useContext, useMemo, useState } from 'react'
import api from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('fleet_user')
    return raw ? JSON.parse(raw) : null
  })
  const [token, setToken] = useState(() => localStorage.getItem('fleet_token'))

  async function login(username, password) {
    const { data } = await api.post('/auth/login', { username, password })
    localStorage.setItem('fleet_token', data.token)
    localStorage.setItem('fleet_user', JSON.stringify(data))
    setToken(data.token)
    setUser(data)
    return data
  }

  function logout() {
    localStorage.removeItem('fleet_token')
    localStorage.removeItem('fleet_user')
    setToken(null)
    setUser(null)
  }

  const value = useMemo(() => ({ user, token, login, logout, isAuthenticated: !!token }), [user, token])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
