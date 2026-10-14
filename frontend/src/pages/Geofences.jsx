import { useEffect, useState } from 'react'
import api, { errorMessage } from '../api'
import LiveMap from '../components/LiveMap'
import { Button, Card, Field, Input, Modal, PageHeader, Table } from '../components/ui'

const empty = { name: '', type: 'CIRCLE', centerLat: 12.9716, centerLng: 77.5946, radiusM: 2000, alertOnEnter: true, alertOnExit: true }

export default function Geofences() {
  const [geofences, setGeofences] = useState([])
  const [form, setForm] = useState(null)
  const [error, setError] = useState('')

  async function load() {
    const { data } = await api.get('/geofences')
    setGeofences(data)
  }
  useEffect(() => {
    load().catch(() => {})
  }, [])

  async function save(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/geofences', {
        ...form,
        centerLat: Number(form.centerLat),
        centerLng: Number(form.centerLng),
        radiusM: Number(form.radiusM),
      })
      setForm(null)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function remove(id) {
    if (!confirm('Delete this geofence?')) return
    await api.delete(`/geofences/${id}`)
    load()
  }

  return (
    <div>
      <PageHeader
        title="Geofences"
        subtitle="Draw zones and get entry/exit alerts"
        actions={<Button onClick={() => setForm({ ...empty })}>+ Add Geofence</Button>}
      />
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card className="p-0">
          <div className="px-4 py-3 text-sm font-semibold text-slate-700">Map</div>
          <LiveMap vehicles={[]} geofences={geofences} height={420} />
        </Card>
        <div>
          <Table
            columns={['Name', 'Type', 'Radius', 'Enter', 'Exit', '']}
            rows={geofences}
            renderRow={(g) => (
              <tr key={g.id} className="hover:bg-slate-50">
                <td className="px-4 py-2.5 font-medium text-slate-800">{g.name}</td>
                <td className="px-4 py-2.5 text-slate-600">{g.type}</td>
                <td className="px-4 py-2.5 text-slate-600">{g.radiusM ? `${g.radiusM} m` : '—'}</td>
                <td className="px-4 py-2.5 text-slate-600">{g.alertOnEnter ? 'Yes' : 'No'}</td>
                <td className="px-4 py-2.5 text-slate-600">{g.alertOnExit ? 'Yes' : 'No'}</td>
                <td className="px-4 py-2.5 text-right">
                  <Button variant="danger" onClick={() => remove(g.id)}>
                    Delete
                  </Button>
                </td>
              </tr>
            )}
          />
        </div>
      </div>

      {form && (
        <Modal title="Add Geofence" onClose={() => setForm(null)}>
          <form onSubmit={save} className="grid grid-cols-2 gap-3">
            <Field label="Name *">
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            </Field>
            <Field label="Radius (m)">
              <Input type="number" value={form.radiusM} onChange={(e) => setForm({ ...form, radiusM: e.target.value })} />
            </Field>
            <Field label="Center Latitude">
              <Input type="number" step="0.0001" value={form.centerLat} onChange={(e) => setForm({ ...form, centerLat: e.target.value })} />
            </Field>
            <Field label="Center Longitude">
              <Input type="number" step="0.0001" value={form.centerLng} onChange={(e) => setForm({ ...form, centerLng: e.target.value })} />
            </Field>
            <label className="col-span-2 flex items-center gap-2 text-sm text-slate-600">
              <input type="checkbox" checked={form.alertOnEnter} onChange={(e) => setForm({ ...form, alertOnEnter: e.target.checked })} />
              Alert on entry
            </label>
            <label className="col-span-2 flex items-center gap-2 text-sm text-slate-600">
              <input type="checkbox" checked={form.alertOnExit} onChange={(e) => setForm({ ...form, alertOnExit: e.target.checked })} />
              Alert on exit
            </label>
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
