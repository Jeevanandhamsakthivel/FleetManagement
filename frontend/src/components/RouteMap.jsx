import { useEffect, useRef } from 'react'
import L from 'leaflet'

export default function RouteMap({ points = [], height = 360 }) {
  const containerRef = useRef(null)
  const mapRef = useRef(null)
  const layerRef = useRef(null)

  useEffect(() => {
    if (mapRef.current || !containerRef.current) return
    const map = L.map(containerRef.current).setView([12.9716, 77.5946], 11)
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
    const latlngs = points
      .filter((p) => p.latitude != null && p.longitude != null)
      .map((p) => [p.latitude, p.longitude])
    if (latlngs.length === 0) return
    L.polyline(latlngs, { color: '#2563eb', weight: 3 }).addTo(layer)
    L.circleMarker(latlngs[0], { radius: 6, color: '#fff', weight: 2, fillColor: '#16a34a', fillOpacity: 1 })
      .bindPopup('Start')
      .addTo(layer)
    L.circleMarker(latlngs[latlngs.length - 1], { radius: 6, color: '#fff', weight: 2, fillColor: '#dc2626', fillOpacity: 1 })
      .bindPopup('Latest')
      .addTo(layer)
    map.fitBounds(L.latLngBounds(latlngs).pad(0.2))
  }, [points])

  return <div ref={containerRef} style={{ height }} className="overflow-hidden rounded-xl" />
}
