import { useEffect, useState } from 'react'
import api from '../api'
import { Badge, PageHeader, Table } from '../components/ui'

export default function AuditLogs() {
  const [logs, setLogs] = useState([])

  useEffect(() => {
    api
      .get('/audit-logs')
      .then((r) => setLogs(r.data))
      .catch(() => {})
  }, [])

  return (
    <div>
      <PageHeader title="Audit Logs" subtitle="Traceability of critical changes" />
      <Table
        columns={['Time', 'User', 'Action', 'Entity', 'Details']}
        rows={logs}
        empty="No audit entries"
        renderRow={(l) => (
          <tr key={l.id} className="hover:bg-slate-50">
            <td className="px-4 py-2.5 text-xs text-slate-400">{new Date(l.createdAt).toLocaleString()}</td>
            <td className="px-4 py-2.5 text-slate-600">{l.username || l.userId}</td>
            <td className="px-4 py-2.5">
              <Badge color="blue">{l.action}</Badge>
            </td>
            <td className="px-4 py-2.5 text-slate-600">
              {l.entity} {l.entityId ? `#${l.entityId}` : ''}
            </td>
            <td className="px-4 py-2.5 text-slate-600">{l.details}</td>
          </tr>
        )}
      />
    </div>
  )
}
