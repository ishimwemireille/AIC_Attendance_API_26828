import { useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { api, clearAuth, hasAuth } from './api'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Students from './pages/Students'
import Sessions from './pages/Sessions'
import MarkAttendance from './pages/MarkAttendance'
import Notifications from './pages/Notifications'

export default function App() {
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    if (!hasAuth()) {
      setChecking(false)
      return
    }
    api.me()
      .then(setUser)
      .catch(() => clearAuth())
      .finally(() => setChecking(false))
  }, [])

  function signOut() {
    clearAuth()
    setUser(null)
  }

  if (checking) {
    return <div className="empty">Loading…</div>
  }

  if (!user) {
    return (
      <Routes>
        <Route path="*" element={<Login onSignedIn={setUser} />} />
      </Routes>
    )
  }

  return (
    <Layout user={user} onSignOut={signOut}>
      <Routes>
        <Route path="/" element={<Dashboard user={user} />} />
        <Route path="/students" element={<Students user={user} />} />
        <Route path="/sessions" element={<Sessions user={user} />} />
        <Route path="/mark" element={<MarkAttendance user={user} />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}
