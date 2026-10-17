import { useEffect, useRef } from 'react'
import L from 'leaflet'
import { bufferPolyline, routeStateMeta } from './geo'

/**
 * Shows the planned road route (blue line), its permitted corridor (shaded area),
 * and the actual travelled GPS track (orange dashed). The corridor is computed as a
 * real polygon around the route geometry - not a circle around the origin.
 */
export default function RouteMap({ points = [], route = null, corridorM = null, accuracy = null, state = null, height = 360 }) {
  const containerRef = useRef(null)
  const mapRef = useRef(null)
  const layerRef = useRef(null)

  useEffect(() => {
    if (mapRef.current || !containerRef.current) return
    const map = L.map(containerRef.current).setView([0, 0], 2)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map)
    layerRef.current = L.layerGroup().addTo(map)
    mapRef.current = map
    return () => {
      map.remove()
      mapRef.current = null
    }
  }, [])

  useEffect(() => {
    const layer = layerRef.current
    const map = mapRef.current
    if (!layer || !map) return
    layer.clearLayers()

    const bounds = []
    const corridor = corridorM || 500

    // Planned road route + corridor (real geometry from the routing provider).
    if (Array.isArray(route) && route.length > 1) {
      const ring = bufferPolyline(route, corridor)
      if (ring.length > 2) {
        L.polygon(ring, { color: '#3b82f6', weight: 1, fillColor: '#3b82f6', fillOpacity: 0.12 })
          .bindPopup(`Permitted corridor &plusmn;${corridor} m`)
          .addTo(layer)
      }
      L.polyline(route, { color: '#1d4ed8', weight: 4, opacity: 0.9 })
        .bindPopup('Planned road route')
        .addTo(layer)
      bounds.push(...route)
    }

    // Actual travelled track.
    const latlngs = points
      .filter((p) => p.latitude != null && p.longitude != null)
      .map((p) => [p.latitude, p.longitude])
    if (latlngs.length > 1) {
      L.polyline(latlngs, { color: '#f97316', weight: 3, dashArray: '6 6' })
        .bindPopup('Actual GPS track')
        .addTo(layer)
    }
    if (latlngs.length > 0) {
      L.circleMarker(latlngs[0], { radius: 6, color: '#fff', weight: 2, fillColor: '#16a34a', fillOpacity: 1 })
        .bindPopup('Start')
        .addTo(layer)
      const meta = routeStateMeta(state)
      L.circleMarker(latlngs[latlngs.length - 1], {
        radius: 7,
        color: '#fff',
        weight: 2,
        fillColor: meta.color,
        fillOpacity: 1,
      })
        .bindPopup(`Latest &mdash; ${meta.label}`)
        .addTo(layer)
      if (accuracy != null && accuracy > 0) {
        L.circle(latlngs[latlngs.length - 1], {
          radius: accuracy,
          color: '#0ea5e9',
          weight: 1,
          fillColor: '#0ea5e9',
          fillOpacity: 0.08,
        })
          .bindPopup(`GPS accuracy &plusmn;${Math.round(accuracy)} m`)
          .addTo(layer)
      }
      bounds.push(...latlngs)
    }

    if (bounds.length > 0) {
      map.fitBounds(L.latLngBounds(bounds).pad(0.2))
    }
  }, [points, route, corridorM, accuracy, state])

  return <div ref={containerRef} style={{ height }} className="overflow-hidden rounded-xl" />
}
