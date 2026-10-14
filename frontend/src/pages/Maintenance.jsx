import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const TYPES = ['OIL_CHANGE', 'TYRE', 'BRAKE', 'ENGINE', 'GENERAL', 'OTHER']
const empty = { vehicleId: '', type: 'GENERAL', description: '', vendor: '', cost: 0, odometer: '', status: 'OPEN', workOrderNumber: '' }

export default function Maintenance() {
  const [records, setRecords] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [form, setForm] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const [m, v] = await Promise.all([api.get('/maintenance'), api.get('/vehicles')])
    setRecords(m.data)
    setVehicles(v.data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/maintenance', {
        ...form,
        vehicleId: Number(form.vehicleId),
        cost: Number(form.cost) || 0,
        odometer: form.odometer ? Number(form.odometer) : null,
        serviceDate: new Date().toISOString(),
      })
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function setStatus(rec, status) {
    await api.put(`/maintenance/${rec.id}`, { ...rec, status })
    load()
  }

  const vcode = (id) => vehicles.find((v) => v.id === id)?.code || id

  return (
    <div>
      <PageHeader
        title="Maintenance & Work Orders"
        subtitle={`${records.length} records`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ New Work Order</Button>}
      />
      <Table
        columns={['Work Order', 'Vehicle', 'Type', 'Description', 'Cost', 'Status', '']}
        rows={records}
        renderRow={(m) => (
          <tr key={m.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">{m.workOrderNumber || `#${m.id}`}</td>
            <td className="px-4 py-2.5 text-slate-600">{vcode(m.vehicleId)}</td>
            <td className="px-4 py-2.5 text-slate-600">{m.type}</td>
            <td className="px-4 py-2.5 text-slate-600">{m.description}</td>
            <td className="px-4 py-2.5 text-slate-600">₹{(m.cost || 0).toLocaleString()}</td>
            <td className="px-4 py-2.5">
              <Badge color={m.status === 'COMPLETED' ? 'green' : m.status === 'IN_PROGRESS' ? 'blue' : 'amber'}>{m.status}</Badge>
            </td>
            <td className="px-4 py-2.5 text-right">
              {m.status !== 'COMPLETED' && (
                <div className="flex justify-end gap-1">
                  {m.status !== 'IN_PROGRESS' && (
                    <Button variant="subtle" onClick={() => setStatus(m, 'IN_PROGRESS')}>
                      Start
                    </Button>
                  )}
                  <Button onClick={() => setStatus(m, 'COMPLETED')}>Complete</Button>
                </div>
              )}
            </td>
          </tr>
        )}
      />

      {form && (
        <Modal title="New Work Order" onClose={() => setForm(null)}>
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
            <Field label="Type">
              <Select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                {TYPES.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </Select>
            </Field>
            <Field label="Description">
              <Input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </Field>
            <Field label="Vendor">
              <Input value={form.vendor} onChange={(e) => setForm({ ...form, vendor: e.target.value })} />
            </Field>
            <Field label="Cost">
              <Input type="number" value={form.cost} onChange={(e) => setForm({ ...form, cost: e.target.value })} />
            </Field>
            <Field label="Odometer">
              <Input type="number" value={form.odometer} onChange={(e) => setForm({ ...form, odometer: e.target.value })} />
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
    </div>
  )
}
