import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import LiveMap, { STATUS_COLORS } from '../components/LiveMap'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const STATUSES = ['MOVING', 'IDLE', 'STOPPED', 'OFFLINE', 'MAINTENANCE']
const FUELS = ['PETROL', 'DIESEL', 'CNG', 'ELECTRIC', 'HYBRID']
const empty = { code: '', registrationNumber: '', type: 'Truck', make: '', model: '', year: 2023, fuelType: 'DIESEL', status: 'OFFLINE' }

export default function Vehicles() {
  const [vehicles, setVehicles] = useState([])
  const [drivers, setDrivers] = useState([])
  const [form, setForm] = useState(null)
  const [assignFor, setAssignFor] = useState(null)
  const [driverId, setDriverId] = useState('')
  const [history, setHistory] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const [v, d] = await Promise.all([api.get('/vehicles'), api.get('/drivers')])
    setVehicles(v.data)
    setDrivers(d.data)
  }

  useEffect(() => {
    load().catch(() => {})
    const t = setInterval(() => api.get('/vehicles').then((r) => setVehicles(r.data)).catch(() => {}), 5000)
    return () => clearInterval(t)
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/vehicles', { ...form, year: Number(form.year) || null })
      setForm(null)
      await load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function remove(id) {
    if (!confirm('Delete this vehicle?')) return
    await api.delete(`/vehicles/${id}`)
    load()
  }

  async function assign(e) {
    e.preventDefault()
    await api.post(`/vehicles/${assignFor.id}/assign`, { driverId: Number(driverId) })
    setAssignFor(null)
    setDriverId('')
    load()
  }

  async function openHistory(v) {
    const { data } = await api.get(`/tracking/vehicle/${v.id}/history`)
    setHistory({ vehicle: v, points: data })
  }

  return (
    <div>
      <PageHeader
        title="Vehicles"
        subtitle={`${vehicles.length} vehicles in your fleet`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ Add Vehicle</Button>}
      />

      <Table
        columns={['Code', 'Registration', 'Type', 'Status', 'Speed', 'Last Update', '']}
        rows={vehicles}
        renderRow={(v) => (
          <tr key={v.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">{v.code}</td>
            <td className="px-4 py-2.5 text-slate-600">{v.registrationNumber}</td>
            <td className="px-4 py-2.5 text-slate-600">
              {v.make} {v.model}
            </td>
            <td className="px-4 py-2.5">
              <span className="inline-flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full" style={{ background: STATUS_COLORS[v.status] || '#64748b' }} />
                <Badge color={v.status === 'MOVING' ? 'green' : v.status === 'MAINTENANCE' ? 'red' : 'slate'}>
                  {v.status}
                </Badge>
              </span>
            </td>
            <td className="px-4 py-2.5 text-slate-600">{Math.round(v.lastSpeed || 0)} km/h</td>
            <td className="px-4 py-2.5 text-xs text-slate-400">
              {v.lastUpdate ? new Date(v.lastUpdate).toLocaleTimeString() : '—'}
            </td>
            <td className="px-4 py-2.5 text-right">
              <div className="flex justify-end gap-1">
                <Button variant="subtle" onClick={() => openHistory(v)}>
                  Track
                </Button>
                <Button variant="ghost" onClick={() => setAssignFor(v)}>
                  Assign
                </Button>
                <Button variant="danger" onClick={() => remove(v.id)}>
                  Delete
                </Button>
              </div>
            </td>
          </tr>
        )}
      />

      {form && (
        <Modal title="Add Vehicle" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Code *">
              <Input value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} required />
            </Field>
            <Field label="Registration No.">
              <Input value={form.registrationNumber} onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })} />
            </Field>
            <Field label="Type">
              <Input value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })} />
            </Field>
            <Field label="Make">
              <Input value={form.make} onChange={(e) => setForm({ ...form, make: e.target.value })} />
            </Field>
            <Field label="Model">
              <Input value={form.model} onChange={(e) => setForm({ ...form, model: e.target.value })} />
            </Field>
            <Field label="Year">
              <Input type="number" value={form.year} onChange={(e) => setForm({ ...form, year: e.target.value })} />
            </Field>
            <Field label="Fuel Type">
              <Select value={form.fuelType} onChange={(e) => setForm({ ...form, fuelType: e.target.value })}>
                {FUELS.map((f) => (
                  <option key={f}>{f}</option>
                ))}
              </Select>
            </Field>
            <Field label="Status">
              <Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                {STATUSES.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </Select>
            </Field>
            {error && <div className="col-span-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>}
            <div className="col-span-2 flex justify-end gap-2 pt-1">
              <Button variant="ghost" type="button" onClick={() => setForm(null)}>
                Cancel
              </Button>
              <Button type="submit">Save</Button>
            </div>
          </form>
        </Modal>
      )}

      {assignFor && (
        <Modal title={`Assign driver to ${assignFor.code}`} onClose={() => setAssignFor(null)}>
          <form onSubmit={assign} className="space-y-3">
            <Field label="Driver">
              <Select value={driverId} onChange={(e) => setDriverId(e.target.value)} required>
                <option value="">Select driver…</option>
                {drivers.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.fullName} ({d.status})
                  </option>
                ))}
              </Select>
            </Field>
            <div className="flex justify-end gap-2">
              <Button variant="ghost" type="button" onClick={() => setAssignFor(null)}>
                Cancel
              </Button>
              <Button type="submit">Assign</Button>
            </div>
          </form>
        </Modal>
      )}

      {history && (
        <Modal title={`Route history — ${history.vehicle.code}`} onClose={() => setHistory(null)} wide>
          <LiveMap vehicles={[history.vehicle]} height={420} />
          <p className="mt-2 text-xs text-slate-400">{history.points.length} location points in the last 6 hours</p>
        </Modal>
      )}
    </div>
  )
}
