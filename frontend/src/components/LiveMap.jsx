import { useEffect, useMemo, useRef } from 'react'
import L from 'leaflet'

export const STATUS_COLORS = {
  MOVING: '#16a34a',
  IDLE: '#f59e0b',
  STOPPED: '#64748b',
  OFFLINE: '#94a3b8',
  MAINTENANCE: '#dc2626',
}

// A reading older than this is considered stale (device likely offline).
const STALE_MS = 2 * 60 * 1000

export function isStale(lastUpdate) {
  if (!lastUpdate) return false
  const t = new Date(lastUpdate).getTime()
  if (Number.isNaN(t)) return false
  return Date.now() - t > STALE_MS
}

export function locationAge(lastUpdate) {
  if (!lastUpdate) return null
  const t = new Date(lastUpdate).getTime()
  if (Number.isNaN(t)) return null
  const diff = Date.now() - t
  if (diff < 5000) return 'just now'
  const mins = Math.round(diff / 60000)
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  return `${hrs}h ${mins % 60}m ago`
}

function popupHtml(v) {
  const reg = v.registrationNumber || v.code
  const speed = v.lastSpeed != null ? `${Math.round(v.lastSpeed)} km/h` : 'n/a'
  const age = locationAge(v.lastUpdate)
  const exact = v.lastUpdate ? new Date(v.lastUpdate).toLocaleString() : 'unknown'
  const source = v.lastSource || 'DEVICE'
  const sourceLabel = source === 'SIMULATED' ? 'SIMULATED' : source === 'MOBILE' ? 'GPS' : 'GPS'
  const stale = isStale(v.lastUpdate)
  return (
    `<b>${reg}</b>` +
    (v.code !== reg ? `<br/>${v.code}` : '') +
    `<br/>${v.status}` +
    `<br/>${speed}` +
    `<br/>Updated ${age || 'never'}` +
    `<br/><span style="font-size:11px;color:#64748b">${exact}</span>` +
    `<br/><span style="font-size:11px;font-weight:600;color:${source === 'SIMULATED' ? '#d97706' : '#16a34a'}">${sourceLabel}</span>` +
    (stale ? '<br/><span style="font-size:11px;color:#dc2626">Stale (no recent signal)</span>' : '')
  )
}

/**
 * Live vehicle map. Has no hardcoded city: it starts from a neutral world view and
 * auto-fits to the actual latest GPS coordinates of vehicles once they arrive.
 * Simulated positions are visually distinguished (dashed) from real GPS positions.
 */
export default function LiveMap({
  vehicles = [],
  geofences = [],
  center = null,
  zoom = null,
  height = 460,
  onSelect,
  selectedId,
  loading = false,
}) {
  const containerRef = useRef(null)
  const mapRef = useRef(null)
  const markerLayerRef = useRef(null)
  const geofenceLayerRef = useRef(null)
  const markersRef = useRef(new Map())
  const fittedKeysRef = useRef(null)

  // Latest valid GPS coordinates only.
  const located = useMemo(
    () => vehicles.filter((v) => v.lastLat != null && v.lastLng != null),
    [vehicles],
  )

  useEffect(() => {
    if (mapRef.current || !containerRef.current) return
    // Neutral starting view (not tied to any specific city); the view is replaced by
    // the first real coordinates.
    const opts = { zoomControl: true }
    const map = L.map(containerRef.current, opts)
    if (center && center.length === 2) {
      map.setView(center, zoom ?? 11)
    } else {
      map.setView([0, 0], 2)
    }
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map)
    markerLayerRef.current = L.layerGroup().addTo(map)
    geofenceLayerRef.current = L.layerGroup().addTo(map)
    mapRef.current = map
    return () => {
      map.remove()
      mapRef.current = null
      markersRef.current.clear()
      fittedKeysRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    const layer = geofenceLayerRef.current
    if (!layer) return
    layer.clearLayers()
    for (const g of geofences) {
      if (g.type === 'CIRCLE' && g.centerLat != null && g.centerLng != null) {
        L.circle([g.centerLat, g.centerLng], {
          radius: g.radiusM || 1000,
          color: g.color || '#2563eb',
          weight: 2,
          fillOpacity: 0.06,
        })
          .bindPopup(`<b>${g.name}</b><br/>Geofence`)
          .addTo(layer)
      }
    }
  }, [geofences])

  useEffect(() => {
    const layer = markerLayerRef.current
    const map = mapRef.current
    if (!layer || !map) return
    const seen = new Set()
    for (const v of located) {
      seen.add(v.id)
      const color = STATUS_COLORS[v.status] || '#64748b'
      const simulated = v.lastSource === 'SIMULATED'
      const stale = isStale(v.lastUpdate)
      const radius = selectedId === v.id ? 11 : 8
      const style = {
        fillColor: color,
        radius,
        dashArray: simulated ? '4 4' : undefined,
        fillOpacity: stale ? 0.35 : 1,
        color: stale ? '#dc2626' : '#fff',
        weight: 2,
      }
      const existing = markersRef.current.get(v.id)
      if (existing) {
        existing.setLatLng([v.lastLat, v.lastLng])
        existing.setStyle(style)
        existing.setRadius(radius)
        existing.setPopupContent(popupHtml(v))
      } else {
        const marker = L.circleMarker([v.lastLat, v.lastLng], { color: '#fff', weight: 2, ...style })
        marker.bindPopup(popupHtml(v))
        marker.on('click', () => onSelect && onSelect(v))
        marker.addTo(layer)
        markersRef.current.set(v.id, marker)
      }
    }
    for (const [id, marker] of markersRef.current.entries()) {
      if (!seen.has(id)) {
        layer.removeLayer(marker)
        markersRef.current.delete(id)
      }
    }

    // Fit to the actual coordinates once the set of located vehicles changes
    // (vehicles appear/disappear) without fighting the user mid-move.
    const key = Array.from(seen).sort().join(',')
    if (key && key !== fittedKeysRef.current) {
      fittedKeysRef.current = key
      map.fitBounds(L.latLngBounds(located.map((v) => [v.lastLat, v.lastLng])).pad(0.3), {
        maxZoom: 13,
        animate: false,
      })
    }
  }, [located, selectedId, onSelect])

  const empty = !loading && located.length === 0

  return (
    <div className="relative" style={{ height }}>
      <div ref={containerRef} className="h-full w-full overflow-hidden rounded-xl" />
      {loading && (
        <div className="pointer-events-none absolute inset-0 flex items-center justify-center rounded-xl bg-white/70 text-sm font-medium text-slate-500">
          Loading vehicle positions…
        </div>
      )}
      {empty && (
        <div className="pointer-events-none absolute inset-0 flex items-center justify-center rounded-xl bg-white/70">
          <div className="text-center">
            <div className="text-sm font-medium text-slate-600">No GPS coordinates yet</div>
            <div className="mt-1 text-xs text-slate-400">
              Markers appear once vehicles report a position.
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
