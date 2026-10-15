import { useEffect, useRef, useState } from 'react'
import api, { errorMessage } from '../api'
import { Badge, Button, Card, Field, PageHeader, Select } from '../components/ui'

// Minimum time between network sends; positions arriving faster are throttled.
const MIN_SEND_INTERVAL_MS = 5000
// ...unless the device has moved at least this far (metres) — keeps turns responsive.
const MIN_SEND_DISTANCE_M = 25

function distanceMeters(lat1, lng1, lat2, lng2) {
  const R = 6371000
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

export default function MobileTracker() {
  const [vehicles, setVehicles] = useState([])
  const [vehicleId, setVehicleId] = useState('')
  const [status, setStatus] = useState('idle') // idle | tracking | stopped | error
  const [geoError, setGeoError] = useState('')
  const [sendError, setSendError] = useState('')
  const [last, setLast] = useState(null)
  const [sentCount, setSentCount] = useState(0)

  const watchIdRef = useRef(null)
  const lastSentRef = useRef(null) // { lat, lng, at }

  const secure = typeof window !== 'undefined' ? window.isSecureContext : false
  const isLocalhost = ['localhost', '127.0.0.1', '::1'].includes(window.location.hostname)

  useEffect(() => {
    api
      .get('/vehicles')
      .then((r) => setVehicles(r.data))
      .catch((err) => setSendError(errorMessage(err)))
    return () => stopTracking()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function stopTracking() {
    if (watchIdRef.current != null && navigator.geolocation) {
      navigator.geolocation.clearWatch(watchIdRef.current)
    }
    watchIdRef.current = null
    setStatus((s) => (s === 'tracking' ? 'stopped' : s))
  }

  function onPosition(pos) {
    const c = pos.coords
    const reading = {
      latitude: c.latitude,
      longitude: c.longitude,
      accuracy: c.accuracy != null ? Math.round(c.accuracy) : null,
      speedKmh: c.speed != null && !Number.isNaN(c.speed) ? Math.round(c.speed * 3.6 * 10) / 10 : null,
      heading: c.heading != null && !Number.isNaN(c.heading) ? Math.round(c.heading) : null,
      timestamp: new Date(c.timestamp || pos.timestamp || Date.now()).toISOString(),
    }
    setLast(reading)

    const now = Date.now()
    const prev = lastSentRef.current
    const elapsed = prev ? now - prev.at : Infinity
    const moved = prev ? distanceMeters(prev.lat, prev.lng, reading.latitude, reading.longitude) : Infinity
    if (elapsed < MIN_SEND_INTERVAL_MS && moved < MIN_SEND_DISTANCE_M) {
      return
    }

    if (!vehicleId) return
    lastSentRef.current = { lat: reading.latitude, lng: reading.longitude, at: now }
    setSendError('')
    api
      .post(`/vehicles/${vehicleId}/location`, reading)
      .then(() => {
        setGeoError('')
        setSentCount((n) => n + 1)
      })
      .catch((err) => {
        // Allow an immediate retry on the next reading after a failure.
        lastSentRef.current = prev
        setSendError(errorMessage(err))
      })
  }

  function onGeoError(err) {
    const msgs = {
      1: 'Location permission denied. Enable location access for this site to track.',
      2: 'GPS position unavailable. Move to an area with a clearer sky view and try again.',
      3: 'Timed out waiting for a GPS fix. Retrying…',
    }
    if (err.code === 1) {
      setStatus('error')
      stopTracking()
    }
    setGeoError(msgs[err.code] || `Location error (${err.code}).`)
  }

  function startTracking() {
    setGeoError('')
    setSendError('')
    if (!vehicleId) {
      setSendError('Select a vehicle before starting.')
      return
    }
    if (!navigator.geolocation) {
      setStatus('error')
      setGeoError('This browser does not support the Geolocation API.')
      return
    }
    setStatus('tracking')
    lastSentRef.current = null
    watchIdRef.current = navigator.geolocation.watchPosition(onPosition, onGeoError, {
      enableHighAccuracy: true,
      maximumAge: 5000,
      timeout: 20000,
    })
  }

  function handleStop() {
    stopTracking()
    setStatus('stopped')
  }

  const selected = vehicles.find((v) => String(v.id) === String(vehicleId))

  return (
    <div className="mx-auto max-w-lg">
      <PageHeader title="Mobile GPS Tracker" subtitle="Use this phone as a live GPS device" />

      {!secure && !isLocalhost && (
        <div className="mb-4 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
          This page is not served over HTTPS. Mobile browsers block GPS on insecure origins. Use
          HTTPS (a development tunnel) or access via <b>localhost</b>.
        </div>
      )}

      <Card className="mb-4">
        <Field label="Vehicle to track">
          <Select value={vehicleId} onChange={(e) => setVehicleId(e.target.value)} disabled={status === 'tracking'}>
            <option value="">Select a vehicle…</option>
            {vehicles.map((v) => (
              <option key={v.id} value={v.id}>
                {v.registrationNumber || v.code} ({v.code})
              </option>
            ))}
          </Select>
        </Field>

        <div className="mt-4 flex items-center gap-3">
          {status === 'tracking' ? (
            <Button variant="danger" onClick={handleStop}>
              Stop tracking
            </Button>
          ) : (
            <Button onClick={startTracking}>Start tracking</Button>
          )}
          <StatusBadge status={status} />
        </div>

        {status === 'idle' && (
          <p className="mt-3 text-xs text-slate-500">
            Grant location permission when prompted. Your position is sent only while tracking is on.
          </p>
        )}
        {status === 'stopped' && sentCount > 0 && (
          <p className="mt-3 text-xs text-slate-500">
            Tracking stopped. This vehicle is no longer reporting from this device.
          </p>
        )}
      </Card>

      {geoError && <Banner tone="red" text={geoError} />}
      {sendError && <Banner tone="red" text={`Could not send update: ${sendError}`} />}

      {last && (
        <Card>
          <div className="mb-3 flex items-center justify-between">
            <div className="text-sm font-semibold text-slate-700">
              {selected ? selected.registrationNumber || selected.code : 'Latest reading'}
            </div>
            <span className="text-xs text-slate-400">{sentCount} sent</span>
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <Metric label="Latitude" value={last.latitude.toFixed(6)} />
            <Metric label="Longitude" value={last.longitude.toFixed(6)} />
            <Metric label="Accuracy" value={last.accuracy != null ? `±${last.accuracy} m` : 'n/a'} />
            <Metric label="Speed" value={last.speedKmh != null ? `${last.speedKmh} km/h` : 'n/a'} />
            <Metric label="Heading" value={last.heading != null ? `${last.heading}°` : 'n/a'} />
            <Metric label="Time" value={new Date(last.timestamp).toLocaleTimeString()} />
          </div>
          {status === 'tracking' && (
            <p className="mt-3 text-xs text-slate-400">
              Updates are throttled to every {MIN_SEND_INTERVAL_MS / 1000}s (or {MIN_SEND_DISTANCE_M}m of
              movement). Keep this tab open and the screen awake; mobile browsers may pause GPS when
              backgrounded or locked.
            </p>
          )}
        </Card>
      )}
    </div>
  )
}

function StatusBadge({ status }) {
  if (status === 'tracking') return <Badge color="green">Tracking</Badge>
  if (status === 'stopped') return <Badge color="slate">Stopped</Badge>
  if (status === 'error') return <Badge color="red">Error</Badge>
  return <Badge color="slate">Idle</Badge>
}

function Banner({ tone, text }) {
  const cls = tone === 'red' ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-800'
  return <div className={`mb-4 rounded-xl px-4 py-3 text-sm ${cls}`}>{text}</div>
}

function Metric({ label, value }) {
  return (
    <div>
      <div className="text-xs uppercase text-slate-400">{label}</div>
      <div className="text-slate-700">{value}</div>
    </div>
  )
}
