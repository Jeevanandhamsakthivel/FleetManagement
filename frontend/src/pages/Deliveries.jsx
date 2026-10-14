import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const STATUSES = ['PENDING', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'FAILED']
const POD_TYPES = ['PHOTO', 'SIGNATURE', 'OTP', 'QR']
const empty = { customerId: '', reference: '', pickupAddress: '', deliveryAddress: '', notes: '' }

export default function Deliveries() {
  const [deliveries, setDeliveries] = useState([])
  const [customers, setCustomers] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [drivers, setDrivers] = useState([])
  const [form, setForm] = useState(null)
  const [assignFor, setAssignFor] = useState(null)
  const [assign, setAssign] = useState({ vehicleId: '', driverId: '' })
  const [podFor, setPodFor] = useState(null)
  const [pod, setPod] = useState({ type: 'PHOTO', photoUrl: '', recipientName: '', otp: '' })
  const [error, setError] = useState('')

  async function load() {
    const [d, c, v, dr] = await Promise.all([
      api.get('/deliveries'),
      api.get('/customers'),
      api.get('/vehicles'),
      api.get('/drivers'),
    ])
    setDeliveries(d.data)
    setCustomers(c.data)
    setVehicles(v.data)
    setDrivers(dr.data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/deliveries', { ...form, customerId: form.customerId ? Number(form.customerId) : null })
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function assignSave(e) {
    e.preventDefault()
    await api.post(`/deliveries/${assignFor.id}/assign`, {
      vehicleId: assign.vehicleId ? Number(assign.vehicleId) : null,
      driverId: assign.driverId ? Number(assign.driverId) : null,
    })
    setAssignFor(null)
    load()
  }

  async function setStatus(d, status) {
    await api.post(`/deliveries/${d.id}/status`, { status })
    load()
  }

  async function podSave(e) {
    e.preventDefault()
    await api.post(`/deliveries/${podFor.id}/proof-of-delivery`, pod)
    setPodFor(null)
    setPod({ type: 'PHOTO', photoUrl: '', recipientName: '', otp: '' })
    load()
  }

  const custName = (id) => customers.find((c) => c.id === id)?.name || '—'
  const statusColor = { DELIVERED: 'green', IN_TRANSIT: 'blue', PENDING: 'slate', FAILED: 'red', ASSIGNED: 'violet' }

  function trackLink(d) {
    return `${window.location.origin}/track/${d.trackingToken}`
  }

  return (
    <div>
      <PageHeader
        title="Deliveries"
        subtitle={`${deliveries.length} deliveries`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ New Delivery</Button>}
      />
      <Table
        columns={['Reference', 'Customer', 'Pickup', 'Drop', 'Status', 'Tracking', '']}
        rows={deliveries}
        renderRow={(d) => (
          <tr key={d.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">{d.reference}</td>
            <td className="px-4 py-2.5 text-slate-600">{custName(d.customerId)}</td>
            <td className="px-4 py-2.5 text-slate-600">{d.pickupAddress}</td>
            <td className="px-4 py-2.5 text-slate-600">{d.deliveryAddress}</td>
            <td className="px-4 py-2.5">
              <Badge color={statusColor[d.status]}>{d.status}</Badge>
            </td>
            <td className="px-4 py-2.5">
              {d.trackingToken && (
                <button
                  className="text-xs text-blue-600 hover:underline"
                  onClick={() => {
                    navigator.clipboard?.writeText(trackLink(d))
                    window.open(trackLink(d), '_blank')
                  }}
                >
                  Open link
                </button>
              )}
            </td>
            <td className="px-4 py-2.5 text-right">
              <div className="flex justify-end gap-1">
                <Button variant="ghost" onClick={() => setAssignFor(d)}>
                  Assign
                </Button>
                <Select
                  className="w-32 rounded-lg border border-slate-300 px-2 py-1 text-xs"
                  value={d.status}
                  onChange={(e) => setStatus(d, e.target.value)}
                >
                  {STATUSES.map((s) => (
                    <option key={s}>{s}</option>
                  ))}
                </Select>
                <Button variant="subtle" onClick={() => setPodFor(d)}>
                  POD
                </Button>
              </div>
            </td>
          </tr>
        )}
      />

      {form && (
        <Modal title="New Delivery" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Reference">
              <Input value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} placeholder="auto if blank" />
            </Field>
            <Field label="Customer">
              <Select value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })}>
                <option value="">None</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Pickup Address">
              <Input value={form.pickupAddress} onChange={(e) => setForm({ ...form, pickupAddress: e.target.value })} />
            </Field>
            <Field label="Delivery Address">
              <Input value={form.deliveryAddress} onChange={(e) => setForm({ ...form, deliveryAddress: e.target.value })} />
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

      {assignFor && (
        <Modal title={`Assign — ${assignFor.reference}`} onClose={() => setAssignFor(null)}>
          <form onSubmit={assignSave} className="space-y-3">
            <Field label="Vehicle">
              <Select value={assign.vehicleId} onChange={(e) => setAssign({ ...assign, vehicleId: e.target.value })}>
                <option value="">None</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.code}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Driver">
              <Select value={assign.driverId} onChange={(e) => setAssign({ ...assign, driverId: e.target.value })}>
                <option value="">None</option>
                {drivers.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.fullName}
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

      {podFor && (
        <Modal title={`Proof of Delivery — ${podFor.reference}`} onClose={() => setPodFor(null)}>
          <form onSubmit={podSave} className="space-y-3">
            <Field label="Type">
              <Select value={pod.type} onChange={(e) => setPod({ ...pod, type: e.target.value })}>
                {POD_TYPES.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </Select>
            </Field>
            <Field label="Recipient Name">
              <Input value={pod.recipientName} onChange={(e) => setPod({ ...pod, recipientName: e.target.value })} />
            </Field>
            {pod.type === 'OTP' ? (
              <Field label="OTP">
                <Input value={pod.otp} onChange={(e) => setPod({ ...pod, otp: e.target.value })} />
              </Field>
            ) : (
              <Field label="Photo URL / Signature reference">
                <Input value={pod.photoUrl} onChange={(e) => setPod({ ...pod, photoUrl: e.target.value })} />
              </Field>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="ghost" type="button" onClick={() => setPodFor(null)}>
                Cancel
              </Button>
              <Button type="submit">Capture POD</Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
