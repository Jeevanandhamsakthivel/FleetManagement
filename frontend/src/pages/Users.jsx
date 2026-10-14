import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import { Badge, Button, Field, Input, Modal, PageHeader, Select, Table } from '../components/ui'

const ROLES = ['COMPANY_ADMIN', 'FLEET_MANAGER', 'DISPATCHER', 'DRIVER', 'MECHANIC', 'FINANCE', 'CUSTOMER']
const empty = { username: '', password: '', fullName: '', email: '', role: 'FLEET_MANAGER', enabled: true }

export default function Users() {
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const { data } = await api.get('/users')
    setUsers(data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/users', form)
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function remove(id) {
    if (!confirm('Delete this user?')) return
    await api.delete(`/users/${id}`)
    load()
  }

  return (
    <div>
      <PageHeader
        title="Users & Roles"
        subtitle={`${users.length} users`}
        actions={<Button onClick={() => setForm({ ...empty })}>+ Add User</Button>}
      />
      <Table
        columns={['Username', 'Full Name', 'Email', 'Role', 'Status', '']}
        rows={users}
        renderRow={(u) => (
          <tr key={u.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 font-medium text-slate-800">{u.username}</td>
            <td className="px-4 py-2.5 text-slate-600">{u.fullName}</td>
            <td className="px-4 py-2.5 text-slate-600">{u.email}</td>
            <td className="px-4 py-2.5">
              <Badge color="blue">{u.role}</Badge>
            </td>
            <td className="px-4 py-2.5">
              <Badge color={u.enabled ? 'green' : 'red'}>{u.enabled ? 'Active' : 'Disabled'}</Badge>
            </td>
            <td className="px-4 py-2.5 text-right">
              <Button variant="danger" onClick={() => remove(u.id)}>
                Delete
              </Button>
            </td>
          </tr>
        )}
      />
      {form && (
        <Modal title="Add User" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Username *">
              <Input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
            </Field>
            <Field label="Password *">
              <Input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            </Field>
            <Field label="Full Name">
              <Input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
            </Field>
            <Field label="Email">
              <Input value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Field>
            <Field label="Role">
              <Select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                {ROLES.map((r) => (
                  <option key={r}>{r}</option>
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
