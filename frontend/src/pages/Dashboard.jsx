import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

export default function Dashboard({ user }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([api.students(), api.sessions(), api.attendances()])
      .then(([students, sessions, attendances]) => setData({ students, sessions, attendances }))
      .catch((problem) => setError(problem.message))
  }, [])

  if (error) return <Shell><div className="banner error">{error}</div></Shell>
  if (!data) return <Shell><div className="empty">Loading…</div></Shell>

  const { students, sessions, attendances } = data
  const active = students.filter((s) => s.active).length
  const counted = attendances.filter((a) => a.status !== 'ABSENT').length
  const rate = attendances.length ? Math.round((counted / attendances.length) * 100) : 0
  const today = new Date().toISOString().slice(0, 10)
  const todays = sessions.filter((s) => s.sessionDate === today)

  return (
    <Shell user={user}>
      <div className="stat-row">
        <Stat label="Active students" value={active} note={`${students.length} registered`} />
        <Stat label="Sessions" value={sessions.length} note={`${todays.length} today`} />
        <Stat label="Attendance rate" value={`${rate}%`} note={`${attendances.length} records`} />
        <Stat label="Marked absent" value={attendances.filter((a) => a.status === 'ABSENT').length}
              note="alerts were sent" />
      </div>

      <div className="card">
        <h2 style={{ fontSize: 18, marginBottom: 12 }}>Recent attendance</h2>
        {attendances.length === 0 ? (
          <p className="empty">Nothing has been marked yet. <Link to="/mark">Mark a session</Link>.</p>
        ) : (
          <table className="table">
            <thead>
              <tr><th>Student</th><th>Session</th><th>Date</th><th>Time in</th><th>Status</th></tr>
            </thead>
            <tbody>
              {attendances.slice(0, 8).map((a) => (
                <tr key={a.id}>
                  <td data-label="Student">{a.student.firstName} {a.student.lastName}</td>
                  <td data-label="Session">{a.session.title}</td>
                  <td data-label="Date" className="num">{a.session.sessionDate}</td>
                  <td data-label="Time in" className="num">{a.timeIn || '—'}</td>
                  <td data-label="Status"><span className={`pill ${a.status}`}>{a.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </Shell>
  )
}

function Stat({ label, value, note }) {
  return (
    <div className="card">
      <div className="stat-label">{label}</div>
      <div className="stat-value" style={{ margin: '8px 0 6px' }}>{value}</div>
      <div className="stat-note">{note}</div>
    </div>
  )
}

function Shell({ user, children }) {
  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>{user ? `Good day, ${user.fullName.split(' ')[0]}` : 'Dashboard'}</h1>
          <p>{new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}</p>
        </div>
        <Link className="btn" to="/mark">Mark attendance</Link>
      </div>
      <div className="content">{children}</div>
    </>
  )
}
