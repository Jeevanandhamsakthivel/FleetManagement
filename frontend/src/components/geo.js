// Small geodesy helpers used by the map components.

const M_PER_DEG_LAT = 111320

/**
 * Builds a closed polygon approximating a buffer of `meters` on both sides of a polyline.
 * `latlngs` is an array of [lat,lng]. The offsets are computed in a local metric plane and
 * converted back to degrees, which is accurate over city-scale distances.
 */
export function bufferPolyline(latlngs, meters) {
  if (!Array.isArray(latlngs) || latlngs.length < 2 || !meters || meters <= 0) return []
  const latRef = latlngs[0][0]
  const mPerDegLng = M_PER_DEG_LAT * Math.cos((latRef * Math.PI) / 180)
  const pts = latlngs.map(([lat, lng]) => [lng * mPerDegLng, lat * M_PER_DEG_LAT])
  const left = []
  const right = []
  for (let i = 0; i < pts.length; i++) {
    let nx = 0
    let ny = 0
    for (const [a, b] of [
      [i - 1, i],
      [i, i + 1],
    ]) {
      if (a < 0 || b >= pts.length) continue
      const dx = pts[b][0] - pts[a][0]
      const dy = pts[b][1] - pts[a][1]
      const len = Math.hypot(dx, dy) || 1
      nx += -dy / len
      ny += dx / len
    }
    const nl = Math.hypot(nx, ny) || 1
    nx /= nl
    ny /= nl
    left.push([pts[i][0] + nx * meters, pts[i][1] + ny * meters])
    right.push([pts[i][0] - nx * meters, pts[i][1] - ny * meters])
  }
  const ring = [...left, ...right.reverse()]
  return ring.map(([x, y]) => [y / M_PER_DEG_LAT, x / mPerDegLng])
}

export function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLng = ((lng2 - lng1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

export const ROUTE_STATE = {
  ON_ROUTE: { label: 'On route', color: '#16a34a' },
  DEVIATED: { label: 'Route deviation', color: '#dc2626' },
  GPS_LOST: { label: 'GPS signal lost', color: '#f59e0b' },
  COMPLETED: { label: 'Trip completed', color: '#64748b' },
}

export function routeStateMeta(state) {
  return ROUTE_STATE[state] || { label: 'No route', color: '#94a3b8' }
}
