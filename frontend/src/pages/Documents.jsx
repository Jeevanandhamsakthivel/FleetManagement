import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const empty = { vehicleId: '', type: 'Insurance', number: '', issuedDate: '', expiryDate: '' }

export default function Documents() {
  const [docs, setDocs] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [form, setForm] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const [d, v] = await Promise.all([api.get('/documents'), api.get('/vehicles')])
    setDocs(d.data)
    setVehicles(v.data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/documents', { ...form, vehicleId: form.vehicleId ? Number(form.vehicleId) : null })
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function remove(id) {
    if (!confirm('Delete this document?')) return
    await api.delete(`/documents/${id}`)
    load()
  }

  const vcode = (id) => vehicles.find((v) => v.id === id)?.code || '—'
  const daysLeft = (d) => Math.ceil((new Date(d) - new Date()) / 86400000)

  return (
    <div>
      <PageHeader
        title="Documents & Compliance"
        subtitle="Insurance, permits, fitness and expiry tracking"
        actions={<Button onClick={() => setForm({ ...empty })}>+ Add Document</Button>}
      />
      <Table
        columns={['Vehicle', 'Type', 'Number', 'Issued', 'Expiry', 'Status', '']}
        rows={docs}
        renderRow={(d) => {
          const left = d.expiryDate ? daysLeft(d.expiryDate) : null
          return (
            <tr key={d.id} className="hover:bg-slate-50">
              <td className="px-4 py-2.5 font-medium text-slate-800">{vcode(d.vehicleId)}</td>
              <td className="px-4 py-2.5 text-slate-600">{d.type}</td>
              <td className="px-4 py-2.5 text-slate-600">{d.number}</td>
              <td className="px-4 py-2.5 text-slate-600">{d.issuedDate || '—'}</td>
              <td className="px-4 py-2.5 text-slate-600">{d.expiryDate || '—'}</td>
              <td className="px-4 py-2.5">
                {left == null ? (
                  <Badge>—</Badge>
                ) : left < 0 ? (
                  <Badge color="red">Expired</Badge>
                ) : left < 30 ? (
                  <Badge color="amber">{left}d left</Badge>
                ) : (
                  <Badge color="green">Valid</Badge>
                )}
              </td>
              <td className="px-4 py-2.5 text-right">
                <Button variant="danger" onClick={() => remove(d.id)}>
                  Delete
                </Button>
              </td>
            </tr>
          )
        }}
      />
      {form && (
        <Modal title="Add Document" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
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
            <Field label="Type">
              <Input value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })} />
            </Field>
            <Field label="Number">
              <Input value={form.number} onChange={(e) => setForm({ ...form, number: e.target.value })} />
            </Field>
            <Field label="Issued Date">
              <Input type="date" value={form.issuedDate} onChange={(e) => setForm({ ...form, issuedDate: e.target.value })} />
            </Field>
            <Field label="Expiry Date">
              <Input type="date" value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} />
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
