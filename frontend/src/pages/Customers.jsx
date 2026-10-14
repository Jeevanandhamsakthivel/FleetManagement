import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import { Button, Field, Input, Modal, PageHeader, Table } from '../components/ui'

const empty = { name: '', contactName: '', email: '', phone: '', address: '' }

export default function Customers() {
  const [customers, setCustomers] = useState([])
  const [form, setForm] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const { data } = await api.get('/customers')
    setCustomers(data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/customers', form)
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function remove(id) {
    if (!confirm('Delete this customer?')) return
    await api.delete(`/customers/${id}`)
    load()
  }

  return (
    <div>
      <PageHeader
        title="Customers"
        subtitle={`${customers.length} customers`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ Add Customer</Button>}
      />
      <Table
        columns={['Name', 'Contact', 'Phone', 'Email', 'Address', '']}
        rows={customers}
        renderRow={(c) => (
          <tr key={c.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">{c.name}</td>
            <td className="px-4 py-2.5 text-slate-600">{c.contactName}</td>
            <td className="px-4 py-2.5 text-slate-600">{c.phone}</td>
            <td className="px-4 py-2.5 text-slate-600">{c.email}</td>
            <td className="px-4 py-2.5 text-slate-600">{c.address}</td>
            <td className="px-4 py-2.5 text-right">
              <Button variant="danger" onClick={() => remove(c.id)}>
                Delete
              </Button>
            </td>
          </tr>
        )}
      />
      {form && (
        <Modal title="Add Customer" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Name *">
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            </Field>
            <Field label="Contact Name">
              <Input value={form.contactName} onChange={(e) => setForm({ ...form, contactName: e.target.value })} />
            </Field>
            <Field label="Email">
              <Input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Field>
            <Field label="Phone">
              <Input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </Field>
            <Field label="Address">
              <Input value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
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
