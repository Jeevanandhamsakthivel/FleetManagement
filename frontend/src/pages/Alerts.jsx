import { useEffect, useState } from 'react'
import api from '../api'
import { Badge, Button, PageHeader, Table } from '../components/ui'

const SEVERITY_COLOR = { CRITICAL: 'red', HIGH: 'red', MEDIUM: 'amber', LOW: 'slate' }

export default function Alerts() {
  const [alerts, setAlerts] = useState([])
  const [filter, setFilter] = useState('OPEN')

  async function load() {
    const url = filter === 'OPEN' ? '/alerts/open' : '/alerts'
    const { data } = await api.get(url)
    setAlerts(data)
  }
  useEffect(() => {
    load().catch(() => {})
    const t = setInterval(load, 8000)
    return () => clearInterval(t)
  }, [filter])

  async function ack(id) {
    await api.post(`/alerts/${id}/acknowledge`)
    load()
  }

  return (
    <div>
      <PageHeader
        title="Alerts"
        subtitle="Overspeed, geofence, idle and safety events"
        actions={
          <div className="flex gap-2">
            <Button variant={filter === 'OPEN' ? 'primary' : 'ghost'} onClick={() => setFilter('OPEN')}>
              Open
            </Button>
            <Button variant={filter === 'ALL' ? 'primary' : 'ghost'} onClick={() => setFilter('ALL')}>
              All
            </Button>
          </div>
        }
      />
      <Table
        columns={['Severity', 'Type', 'Message', 'Time', 'Status', '']}
        rows={alerts}
        empty="No alerts"
        renderRow={(a) => (
          <tr key={a.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5">
              <Badge color={SEVERITY_COLOR[a.severity]}>{a.severity}</Badge>
            </td>
            <td className="px-4 py-2.5 text-slate-600">{a.type}</td>
            <td className="px-4 py-2.5 text-slate-800">{a.message}</td>
            <td className="px-4 py-2.5 text-xs text-slate-400">{new Date(a.createdAt).toLocaleString()}</td>
            <td className="px-4 py-2.5">
              {a.acknowledged ? <Badge color="green">Acknowledged</Badge> : <Badge color="amber">Open</Badge>}
            </td>
            <td className="px-4 py-2.5 text-right">
              {!a.acknowledged && <Button onClick={() => ack(a.id)}>Acknowledge</Button>}
            </td>
          </tr>
        )}
      />
    </div>
  )
}
