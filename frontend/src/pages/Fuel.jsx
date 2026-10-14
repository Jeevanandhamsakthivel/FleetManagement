import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import { Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const FUELS = ['PETROL', 'DIESEL', 'CNG', 'ELECTRIC', 'HYBRID']
const empty = { vehicleId: '', fuelType: 'DIESEL', quantity: 0, cost: 0, odometer: '', station: '' }

export default function Fuel() {
  const [records, setRecords] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [form, setForm] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const [f, v] = await Promise.all([api.get('/fuel'), api.get('/vehicles')])
    setRecords(f.data)
    setVehicles(v.data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/fuel', {
        ...form,
        vehicleId: Number(form.vehicleId),
        quantity: Number(form.quantity) || 0,
        cost: Number(form.cost) || 0,
        odometer: form.odometer ? Number(form.odometer) : null,
        filledAt: new Date().toISOString(),
      })
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const vcode = (id) => vehicles.find((v) => v.id === id)?.code || id
  const total = records.reduce((s, r) => s + (r.cost || 0), 0)

  return (
    <div>
      <PageHeader
        title="Fuel / Energy"
        subtitle={`Total spend ₹${total.toLocaleString()}`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ Log Fuel</Button>}
      />
      <Table
        columns={['Vehicle', 'Fuel', 'Quantity', 'Cost', 'Odometer', 'Station', 'Date']}
        rows={records}
        renderRow={(r) => (
          <tr key={r.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">{vcode(r.vehicleId)}</td>
            <td className="px-4 py-2.5 text-slate-600">{r.fuelType}</td>
            <td className="px-4 py-2.5 text-slate-600">
              {r.quantity} {r.fuelType === 'ELECTRIC' ? 'kWh' : 'L'}
            </td>
            <td className="px-4 py-2.5 text-slate-600">₹{(r.cost || 0).toLocaleString()}</td>
            <td className="px-4 py-2.5 text-slate-600">{r.odometer || '—'}</td>
            <td className="px-4 py-2.5 text-slate-600">{r.station}</td>
            <td className="px-4 py-2.5 text-xs text-slate-400">{r.filledAt ? new Date(r.filledAt).toLocaleDateString() : '—'}</td>
          </tr>
        )}
      />
      {form && (
        <Modal title="Log Fuel / Energy" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Vehicle *">
              <Select value={form.vehicleId} onChange={(e) => setForm({ ...form, vehicleId: e.target.value })} required>
                <option value="">Select…</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.code}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Fuel Type">
              <Select value={form.fuelType} onChange={(e) => setForm({ ...form, fuelType: e.target.value })}>
                {FUELS.map((f) => (
                  <option key={f}>{f}</option>
                ))}
              </Select>
            </Field>
            <Field label="Quantity">
              <Input type="number" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} />
            </Field>
            <Field label="Cost">
              <Input type="number" value={form.cost} onChange={(e) => setForm({ ...form, cost: e.target.value })} />
            </Field>
            <Field label="Odometer">
              <Input type="number" value={form.odometer} onChange={(e) => setForm({ ...form, odometer: e.target.value })} />
            </Field>
            <Field label="Station">
              <Input value={form.station} onChange={(e) => setForm({ ...form, station: e.target.value })} />
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
    </div>
  )
}
