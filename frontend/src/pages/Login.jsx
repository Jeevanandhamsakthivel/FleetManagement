import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { errorMessage } from '../api'
import { Button, Field, Input } from '../components/ui'

const DEMO = [
  { role: 'Company Admin', username: 'admin', password: 'admin123' },
  { role: 'Fleet Manager', username: 'manager', password: 'manager123' },
  { role: 'Dispatcher', username: 'dispatch', password: 'dispatch123' },
  { role: 'Driver', username: 'driver', password: 'driver123' },
]

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('admin123')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-blue-900 p-4">
      <div className="grid w-full max-w-4xl overflow-hidden rounded-2xl bg-white shadow-2xl md:grid-cols-2">
        <div className="hidden flex-col justify-between bg-slate-900 p-8 text-white md:flex">
          <div>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 font-bold">FM</div>
            <h1 className="mt-6 text-2xl font-semibold">Fleet Management</h1>
            <p className="mt-2 text-sm text-slate-300">
              Real-time tracking, dispatch, safety, maintenance and delivery visibility in one platform.
            </p>
          </div>
          <ul className="space-y-2 text-sm text-slate-300">
            <li>• Live GPS map &amp; vehicle status</li>
            <li>• Trips, geofencing &amp; alerts</li>
            <li>• Maintenance, fuel &amp; compliance</li>
            <li>• Customer delivery tracking &amp; POD</li>
          </ul>
        </div>
        <div className="p-8">
          <h2 className="text-lg font-semibold text-slate-900">Sign in</h2>
          <p className="mb-4 text-sm text-slate-500">Use a demo account below.</p>
          <form onSubmit={submit} className="space-y-3">
            <Field label="Username">
              <Input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
            </Field>
            <Field label="Password">
              <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </Field>
            {error && <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>}
            <Button type="submit" disabled={loading} className="w-full">
              {loading ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
          <div className="mt-5">
            <div className="mb-2 text-xs font-medium uppercase text-slate-400">Demo accounts</div>
            <div className="grid grid-cols-2 gap-2">
              {DEMO.map((d) => (
                <button
                  key={d.username}
                  onClick={() => {
                    setUsername(d.username)
                    setPassword(d.password)
                  }}
                  className="rounded-lg border border-slate-200 px-3 py-2 text-left text-xs hover:bg-slate-50"
                >
                  <div className="font-medium text-slate-700">{d.role}</div>
                  <div className="text-slate-400">
                    {d.username} / {d.password}
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
