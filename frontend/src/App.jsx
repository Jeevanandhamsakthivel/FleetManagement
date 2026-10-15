import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Vehicles from './pages/Vehicles'
import Drivers from './pages/Drivers'
import Trips from './pages/Trips'
import Geofences from './pages/Geofences'
import Alerts from './pages/Alerts'
import Maintenance from './pages/Maintenance'
import Fuel from './pages/Fuel'
import Documents from './pages/Documents'
import Customers from './pages/Customers'
import Deliveries from './pages/Deliveries'
import Users from './pages/Users'
import AuditLogs from './pages/AuditLogs'
import TrackDelivery from './pages/TrackDelivery'
import MobileTracker from './pages/MobileTracker'

function Protected({ children }) {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Layout>{children}</Layout>
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/track/:token" element={<TrackDelivery />} />
      <Route path="/" element={<Protected><Dashboard /></Protected>} />
      <Route path="/vehicles" element={<Protected><Vehicles /></Protected>} />
      <Route path="/drivers" element={<Protected><Drivers /></Protected>} />
      <Route path="/trips" element={<Protected><Trips /></Protected>} />
      <Route path="/geofences" element={<Protected><Geofences /></Protected>} />
      <Route path="/alerts" element={<Protected><Alerts /></Protected>} />
      <Route path="/maintenance" element={<Protected><Maintenance /></Protected>} />
      <Route path="/fuel" element={<Protected><Fuel /></Protected>} />
      <Route path="/documents" element={<Protected><Documents /></Protected>} />
      <Route path="/customers" element={<Protected><Customers /></Protected>} />
      <Route path="/deliveries" element={<Protected><Deliveries /></Protected>} />
      <Route path="/users" element={<Protected><Users /></Protected>} />
      <Route path="/audit-logs" element={<Protected><AuditLogs /></Protected>} />
      <Route path="/tracker" element={<Protected><MobileTracker /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
