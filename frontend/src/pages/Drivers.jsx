import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const STATUSES = ['AVAILABLE', 'ON_TRIP', 'OFF_DUTY', 'SUSPENDED']
const empty = { fullName: '', phone: '', email: '', licenseNumber: '', licenseExpiry: '', status: 'AVAILABLE' }

export default function Drivers() {
  const [drivers, setDrivers] = useState([])
  const [form, setForm] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const { data } = await api.get('/drivers')
    setDrivers(data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/drivers', form)
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function remove(id) {
    if (!confirm('Delete this driver?')) return
    await api.delete(`/drivers/${id}`)
    load()
  }

  return (
    <div>
      <PageHeader
        title="Drivers"
        subtitle={`${drivers.length} drivers`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ Add Driver</Button>}
      />
      <Table
        columns={['Name', 'Phone', 'License', 'Expiry', 'Status', 'Safety', '']}
        rows={drivers}
        renderRow={(d) => (
          <tr key={d.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">{d.fullName}</td>
            <td className="px-4 py-2.5 text-slate-600">{d.phone}</td>
            <td className="px-4 py-2.5 text-slate-600">{d.licenseNumber}</td>
            <td className="px-4 py-2.5 text-slate-600">{d.licenseExpiry || '—'}</td>
            <td className="px-4 py-2.5">
              <Badge color={d.status === 'AVAILABLE' ? 'green' : d.status === 'ON_TRIP' ? 'blue' : 'slate'}>
                {d.status}
              </Badge>
            </td>
            <td className="px-4 py-2.5 text-slate-600">{d.safetyScore}</td>
            <td className="px-4 py-2.5 text-right">
              <Button variant="danger" onClick={() => remove(d.id)}>
                Delete
              </Button>
            </td>
          </tr>
        )}
      />
      {form && (
        <Modal title="Add Driver" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Full Name *">
              <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
            </Field>
            <Field label="Phone">
              <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </Field>
            <Field label="Email">
              <Input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Field>
            <Field label="License Number">
              <Input value={form.licenseNumber} onChange={(e) => setForm({ ...form, licenseNumber: e.target.value })} />
            </Field>
            <Field label="License Expiry">
              <Input type="date" value={form.licenseExpiry} onChange={(e) => setForm({ ...form, licenseExpiry: e.target.value })} />
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
    </div>
  )
}
