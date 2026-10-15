import { useEffect, useRef, useState } from 'react'
import api, { connectTrackingSocket, errorMessage } from '../api'
import LiveMap, { STATUS_COLORS } from '../components/LiveMap'
import StatCard from '../components/StatCard'
import { Badge, Card } from '../components/ui'

const SEVERITY_COLOR = { CRITICAL: 'red', HIGH: 'red', MEDIUM: 'amber', LOW: 'slate' }

function WsIndicator({ status }) {
  const map = {
    connected: { color: 'text-green-600', dot: 'bg-green-500', label: 'Live', pulse: true },
    connecting: { color: 'text-amber-600', dot: 'bg-amber-500', label: 'Connecting…', pulse: true },
    error: { color: 'text-red-600', dot: 'bg-red-500', label: 'Reconnecting…', pulse: false },
    disconnected: { color: 'text-red-600', dot: 'bg-red-500', label: 'Reconnecting…', pulse: false },
  }
  const s = map[status] || map.connecting
  return (
    <span className={`flex items-center gap-1.5 text-xs font-medium ${s.color}`}>
      <span className={`h-2 w-2 rounded-full ${s.dot} ${s.pulse ? 'animate-pulse' : ''}`} /> {s.label}
    </span>
  )
}


export default function Dashboard() {
  const [kpis, setKpis] = useState(null)
  const [vehicles, setVehicles] = useState([])
  const [geofences, setGeofences] = useState([])
  const [alerts, setAlerts] = useState([])
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [wsStatus, setWsStatus] = useState('connecting')
  const socketRef = useRef(null)

  async function loadAll() {
    const [k, v, g, a] = await Promise.all([
      api.get('/dashboard/kpis'),
      api.get('/vehicles'),
      api.get('/geofences'),
      api.get('/alerts/open'),
    ])
    setKpis(k.data)
    setVehicles(v.data)
    setGeofences(g.data)
    setAlerts(a.data.slice(0, 12))
  }

  useEffect(() => {
    loadAll()
      .then(() => {
        setLoadError('')
      })
      .catch((err) => {
        setLoadError(errorMessage(err))
      })
      .finally(() => setLoading(false))
    const kpiTimer = setInterval(() => api.get('/dashboard/kpis').then((r) => setKpis(r.data)).catch(() => {}), 8000)
    const alertTimer = setInterval(
      () => api.get('/alerts/open').then((r) => setAlerts(r.data.slice(0, 12))).catch(() => {}),
      10000,
    )
    socketRef.current = connectTrackingSocket(
      (msg) => {
        if (msg.type !== 'position') return
        setVehicles((prev) =>
          prev.map((v) =>
            v.id === msg.vehicleId
              ? {
                  ...v,
                  lastLat: msg.lat,
                  lastLng: msg.lng,
                  lastSpeed: msg.speed,
                  lastHeading: msg.heading,
                  status: msg.status,
                  lastSource: msg.source,
                  lastUpdate: msg.ts,
                }
              : v,
          ),
        )
      },
      { onStatus: setWsStatus },
    )
    return () => {
      clearInterval(kpiTimer)
      clearInterval(alertTimer)
      socketRef.current?.close()
    }
  }, [])

  const breakdown = kpis
    ? [
        { label: 'Moving', value: kpis.moving, color: '#16a34a' },
        { label: 'Idle', value: kpis.idle, color: '#f59e0b' },
        { label: 'Stopped', value: kpis.stopped, color: '#64748b' },
        { label: 'Offline', value: kpis.offline, color: '#94a3b8' },
        { label: 'Maintenance', value: kpis.maintenance, color: '#dc2626' },
      ]
    : []
  const total = breakdown.reduce((s, b) => s + b.value, 0)

  return (
    <div>
      <div className="mb-5 flex items-end justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Fleet Overview</h1>
          <p className="mt-0.5 text-sm text-slate-500">Live positions, utilization and open alerts</p>
        </div>
        <WsIndicator status={wsStatus} />
      </div>

      {loadError && (
        <div className="mb-4 flex items-center justify-between rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
          <span>Could not load fleet data: {loadError}</span>
          <button
            onClick={() => {
              setLoading(true)
              loadAll()
                .then(() => setLoadError(''))
                .catch((err) => setLoadError(errorMessage(err)))
                .finally(() => setLoading(false))
            }}
            className="rounded-lg border border-red-200 px-3 py-1 text-xs font-medium hover:bg-red-100"
          >
            Retry
          </button>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-6">
        <StatCard label="Vehicles" value={kpis?.totalVehicles} accent="blue" />
        <StatCard label="Moving" value={kpis?.moving} accent="green" hint={`${kpis?.utilizationPct ?? 0}% utilization`} />
        <StatCard label="Active Trips" value={kpis?.activeTrips} accent="violet" />
        <StatCard label="Open Alerts" value={kpis?.openAlerts} accent="red" />
        <StatCard label="Drivers" value={kpis?.totalDrivers} accent="slate" hint={`${kpis?.driversOnTrip ?? 0} on trip`} />
        <StatCard label="Cost / km" value={kpis ? `₹${kpis.costPerKm}` : undefined} accent="amber" />
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Card className="p-0">
            <div className="flex items-center justify-between px-4 py-3">
              <div className="text-sm font-semibold text-slate-700">Live Fleet Map</div>
              <div className="flex flex-wrap items-center gap-2">
                {Object.entries(STATUS_COLORS).map(([s, c]) => (
                  <span key={s} className="flex items-center gap-1 text-xs text-slate-500">
                    <span className="h-2 w-2 rounded-full" style={{ background: c }} />
                    {s}
                  </span>
                ))}
                <span className="flex items-center gap-1 text-xs text-amber-600">
                  <span className="h-2 w-2 rounded-full border-2 border-dashed border-amber-500" />
                  Simulated
                </span>
              </div>
            </div>
            <p className="px-4 pb-2 text-xs text-slate-400">
              Solid markers are real GPS; dashed markers are simulated (dev). Red rings mark stale signals.
            </p>
            <LiveMap
              vehicles={vehicles}
              geofences={geofences}
              height={440}
              onSelect={setSelected}
              selectedId={selected?.id}
              loading={loading}
            />
          </Card>
        </div>

        <div className="space-y-4">
          <Card>
            <div className="mb-3 text-sm font-semibold text-slate-700">Fleet Status</div>
            <div className="flex h-3 overflow-hidden rounded-full bg-slate-100">
              {breakdown.map((b) =>
                b.value > 0 ? (
                  <div key={b.label} style={{ width: `${(b.value / total) * 100}%`, background: b.color }} title={`${b.label}: ${b.value}`} />
                ) : null,
              )}
            </div>
            <div className="mt-3 space-y-1">
              {breakdown.map((b) => (
                <div key={b.label} className="flex items-center justify-between text-sm">
                  <span className="flex items-center gap-2 text-slate-600">
                    <span className="h-2 w-2 rounded-full" style={{ background: b.color }} />
                    {b.label}
                  </span>
                  <span className="font-medium">{b.value}</span>
                </div>
              ))}
            </div>
          </Card>

          <Card className="p-0">
            <div className="border-b border-slate-100 px-4 py-3 text-sm font-semibold text-slate-700">Open Alerts</div>
            <div className="max-h-72 overflow-y-auto">
              {alerts.length === 0 ? (
                <div className="px-4 py-6 text-center text-sm text-slate-400">No open alerts</div>
              ) : (
                alerts.map((a) => (
                  <div key={a.id} className="border-b border-slate-50 px-4 py-2.5">
                    <div className="flex items-center justify-between gap-2">
                      <Badge color={SEVERITY_COLOR[a.severity]}>{a.type}</Badge>
                      <span className="text-xs text-slate-400">{new Date(a.createdAt).toLocaleTimeString()}</span>
                    </div>
                    <div className="mt-1 text-sm text-slate-700">{a.message}</div>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatCard label="Distance (all time)" value={kpis ? `${Math.round(kpis.totalDistanceKm).toLocaleString()} km` : undefined} accent="blue" />
        <StatCard label="Fuel Cost" value={kpis ? `₹${kpis.fuelCost.toLocaleString()}` : undefined} accent="amber" />
        <StatCard label="Maintenance Cost" value={kpis ? `₹${kpis.maintenanceCost.toLocaleString()}` : undefined} accent="red" />
        <StatCard label="Deliveries In Transit" value={kpis?.deliveriesInTransit} accent="green" />
      </div>
    </div>
  )
}
