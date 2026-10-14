import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api from '../api'
import LiveMap from '../components/LiveMap'
import { Badge } from '../components/ui'

export default function TrackDelivery() {
  const { token } = useParams()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const res = await api.get(`/public/track/${token}`)
        if (active) setData(res.data)
      } catch {
        if (active) setError('Tracking link not found or has expired.')
      }
    }
    load()
    const t = setInterval(load, 6000)
    return () => {
      active = false
      clearInterval(t)
    }
  }, [token])

  const vehicle = data?.vehicle
    ? [
        {
          id: 'live',
          code: data.vehicle.code,
          lastLat: data.vehicle.lat,
          lastLng: data.vehicle.lng,
          lastSpeed: data.vehicle.speed,
          status: data.vehicle.status,
        },
      ]
    : []

  return (
    <div className="mx-auto max-w-3xl p-6">
      <div className="mb-4 flex items-center gap-2">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-sm font-bold text-white">FM</div>
        <div>
          <div className="font-semibold text-slate-900">Delivery Tracking</div>
          <div className="text-xs text-slate-500">{data?.reference || token}</div>
        </div>
      </div>

      {error && <div className="rounded-xl bg-red-50 p-4 text-red-600">{error}</div>}

      {data && (
        <div className="space-y-4">
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <Badge color={data.status === 'DELIVERED' ? 'green' : 'blue'}>{data.status}</Badge>
              {data.eta && <span className="text-sm text-slate-500">ETA {new Date(data.eta).toLocaleTimeString()}</span>}
            </div>
            <div className="mt-4 grid gap-3 text-sm sm:grid-cols-2">
              <div>
                <div className="text-xs uppercase text-slate-400">Pickup</div>
                <div className="text-slate-700">{data.pickupAddress || '—'}</div>
              </div>
              <div>
                <div className="text-xs uppercase text-slate-400">Delivery</div>
                <div className="text-slate-700">{data.deliveryAddress || '—'}</div>
              </div>
              {vehicle[0] && (
                <div>
                  <div className="text-xs uppercase text-slate-400">Vehicle</div>
                  <div className="text-slate-700">
                    {data.vehicle.code} · {Math.round(data.vehicle.speed || 0)} km/h
                  </div>
                </div>
              )}
              {data.deliveredAt && (
                <div>
                  <div className="text-xs uppercase text-slate-400">Delivered At</div>
                  <div className="text-slate-700">{new Date(data.deliveredAt).toLocaleString()}</div>
                </div>
              )}
            </div>
          </div>

          {vehicle[0] && (
            <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
              <LiveMap vehicles={vehicle} height={360} zoom={12} />
            </div>
          )}
        </div>
      )}
    </div>
  )
}
