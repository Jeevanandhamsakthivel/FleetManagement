import { useEffect, useRef } from 'react'
import L from 'leaflet'

export const STATUS_COLORS = {
  MOVING: '#16a34a',
  IDLE: '#f59e0b',
  STOPPED: '#64748b',
  OFFLINE: '#94a3b8',
  MAINTENANCE: '#dc2626',
}

export default function LiveMap({
  vehicles = [],
  geofences = [],
  center = [12.9716, 77.5946],
  zoom = 11,
  height = 460,
  onSelect,
  selectedId,
}) {
  const containerRef = useRef(null)
  const mapRef = useRef(null)
  const markerLayerRef = useRef(null)
  const geofenceLayerRef = useRef(null)
  const markersRef = useRef(new Map())

  useEffect(() => {
    if (mapRef.current || !containerRef.current) return
    const map = L.map(containerRef.current, { zoomControl: true }).setView(center, zoom)
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
    if (!layer) return
    const seen = new Set()
    for (const v of vehicles) {
      if (v.lastLat == null || v.lastLng == null) continue
      seen.add(v.id)
      const color = STATUS_COLORS[v.status] || '#64748b'
      const existing = markersRef.current.get(v.id)
      if (existing) {
        existing.setLatLng([v.lastLat, v.lastLng])
        existing.setStyle({ fillColor: color, radius: selectedId === v.id ? 11 : 8 })
        existing.setPopupContent(
          `<b>${v.code}</b><br/>${v.status}<br/>${(v.lastSpeed || 0).toFixed(0)} km/h`,
        )
      } else {
        const marker = L.circleMarker([v.lastLat, v.lastLng], {
          radius: selectedId === v.id ? 11 : 8,
          color: '#fff',
          weight: 2,
          fillColor: color,
          fillOpacity: 1,
        })
        marker.bindPopup(`<b>${v.code}</b><br/>${v.status}<br/>${(v.lastSpeed || 0).toFixed(0)} km/h`)
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
  }, [vehicles, selectedId, onSelect])

  return <div ref={containerRef} style={{ height }} className="overflow-hidden rounded-xl" />
}
