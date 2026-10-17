import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import RouteMap from '../components/RouteMap'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const empty = { vehicleId: '', driverId: '', origin: '', destination: '', plannedDistanceKm: 10, deviationThresholdM: 500 }

export default function Trips() {
  const [trips, setTrips] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [drivers, setDrivers] = useState([])
  const [form, setForm] = useState(null)
  const [route, setRoute] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const [t, v, d] = await Promise.all([api.get('/trips'), api.get('/vehicles'), api.get('/drivers')])
    setTrips(t.data)
    setVehicles(v.data)
    setDrivers(d.data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  function name(id, list, field = 'code') {
    const item = list.find((x) => x.id === id)
    return item ? item[field] : '—'
  }

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/trips', {
        ...form,
        vehicleId: form.vehicleId ? Number(form.vehicleId) : null,
        driverId: form.driverId ? Number(form.driverId) : null,
        plannedDistanceKm: Number(form.plannedDistanceKm) || null,
        deviationThresholdM: Number(form.deviationThresholdM) || 500,
      })
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function action(id, verb) {
    await api.post(`/trips/${id}/${verb}`)
    load()
  }

  async function remove(id) {
    if (!confirm('Delete this trip?')) return
    await api.delete(`/trips/${id}`)
    load()
  }

  async function viewRoute(t) {
    const [loc, route] = await Promise.all([
      api.get(`/trips/${t.id}/locations`),
      api.get(`/trips/${t.id}/route`).catch(() => ({ data: null })),
    ])
    setRoute({ trip: t, points: loc.data, planned: route.data })
  }

  async function replanRoute(id) {
    setError('')
    try {
      const { data } = await api.post(`/trips/${id}/route`)
      setRoute((r) => (r ? { ...r, planned: data } : r))
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function saveCorridor(id, deviationThresholdM) {
    setError('')
    try {
      const { data } = await api.put(`/trips/${id}/corridor`, { deviationThresholdM })
      setRoute((r) => (r ? { ...r, planned: data } : r))
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const statusColor = { IN_PROGRESS: 'blue', COMPLETED: 'green', PLANNED: 'slate', CANCELLED: 'red' }
  const routeStateColor = { ON_ROUTE: 'green', DEVIATED: 'red', GPS_LOST: 'amber', COMPLETED: 'slate' }

  return (
    <div>
      <PageHeader
        title="Trips"
        subtitle={`${trips.length} trips`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ New Trip</Button>}
      />
      <Table
        columns={['Route', 'Vehicle', 'Driver', 'Status', 'Distance', 'Started', '']}
        rows={trips}
        renderRow={(t) => (
          <tr key={t.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">
              {t.origin} → {t.destination}
            </td>
            <td className="px-4 py-2.5 text-slate-600">{name(t.vehicleId, vehicles)}</td>
            <td className="px-4 py-2.5 text-slate-600">{name(t.driverId, drivers, 'fullName')}</td>
            <td className="px-4 py-2.5">
              <Badge color={statusColor[t.status]}>{t.status}</Badge>
              {t.status === 'IN_PROGRESS' && t.routeState && (
                <Badge color={routeStateColor[t.routeState]}>{t.routeState.replace('_', ' ')}</Badge>
              )}
            </td>
            <td className="px-4 py-2.5 text-slate-600">{(t.distanceKm || 0).toFixed(1)} km</td>
            <td className="px-4 py-2.5 text-xs text-slate-400">
              {t.actualStart ? new Date(t.actualStart).toLocaleString() : '—'}
            </td>
            <td className="px-4 py-2.5 text-right">
              <div className="flex justify-end gap-1">
                <Button variant="subtle" onClick={() => viewRoute(t)}>
                  Route
                </Button>
                {t.status === 'PLANNED' && (
                  <Button onClick={() => action(t.id, 'start')}>Start</Button>
                )}
                {t.status === 'IN_PROGRESS' && (
                  <Button onClick={() => action(t.id, 'complete')}>Complete</Button>
                )}
                <Button variant="danger" onClick={() => remove(t.id)}>
                  Delete
                </Button>
              </div>
            </td>
          </tr>
        )}
      />

      {form && (
        <Modal title="New Trip" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Origin *">
              <Input value={form.origin} onChange={(e) => setForm({ ...form, origin: e.target.value })} required />
            </Field>
            <Field label="Destination *">
              <Input value={form.destination} onChange={(e) => setForm({ ...form, destination: e.target.value })} required />
            </Field>
            <Field label="Vehicle">
              <Select value={form.vehicleId} onChange={(e) => setForm({ ...form, vehicleId: e.target.value })}>
                <option value="">None</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.code}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Driver">
              <Select value={form.driverId} onChange={(e) => setForm({ ...form, driverId: e.target.value })}>
                <option value="">None</option>
                {drivers.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.fullName}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Planned Distance (km)">
              <Input
                type="number"
                value={form.plannedDistanceKm}
                onChange={(e) => setForm({ ...form, plannedDistanceKm: e.target.value })}
              />
            </Field>
            <Field label="Route corridor (± metres)">
              <Input
                type="number"
                value={form.deviationThresholdM}
                onChange={(e) => setForm({ ...form, deviationThresholdM: e.target.value })}
              />
            </Field>
            {error && <div className="col-span-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>}
            <div className="col-span-2 flex justify-end gap-2 pt-1">
              <Button variant="ghost" type="button" onClick={() => setForm(null)}>
                Cancel
              </Button>
              <Button type="submit">Create</Button>
            </div>
          </form>
        </Modal>
      )}

      {route && (
        <Modal title={`Route — ${route.trip.origin} → ${route.trip.destination}`} onClose={() => setRoute(null)} wide>
          <RouteMap
            points={route.points}
            route={route.planned?.geometry}
            corridorM={route.planned?.corridorM}
            state={route.planned?.state}
            height={420}
          />
          {route.planned?.geometry?.length > 1 ? (
            <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
              <span>
                Planned road route via <b>{route.planned.provider}</b> ·{' '}
                {route.planned.distanceKm != null ? `${route.planned.distanceKm.toFixed(1)} km` : '—'} ·{' '}
                {route.points.length} GPS points recorded
              </span>
              <span className="flex items-center gap-2">
                Corridor
                <select
                  defaultValue={route.planned.corridorM || 500}
                  onChange={(e) => saveCorridor(route.trip.id, Number(e.target.value))}
                  className="rounded-md border border-slate-300 px-2 py-1"
                >
                  {[200, 300, 500, 1000, 2000].map((m) => (
                    <option key={m} value={m}>
                      ±{m} m
                    </option>
                  ))}
                </select>
              </span>
            </div>
          ) : (
            <div className="mt-2 flex items-center justify-between gap-2 text-xs text-slate-500">
              <span>No planned road route yet (routing unavailable or origin/destination not resolvable).</span>
              <Button variant="subtle" onClick={() => replanRoute(route.trip.id)}>
                Plan route
              </Button>
            </div>
          )}
        </Modal>
      )}
    </div>
  )
}
