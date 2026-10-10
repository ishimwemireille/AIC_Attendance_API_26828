import { useEffect, useState } from 'react'
import { api } from '../api'

export default function Sessions({ user }) {
  const [sessions, setSessions] = useState(null)
  const [error, setError] = useState(null)
  const trainerOnly = user.roles.length === 1 && user.roles[0] === 'TRAINER'

  useEffect(() => {
    api.sessions().then(setSessions).catch((p) => setError(p.message))
  }, [])

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Sessions</h1>
          <p>{trainerOnly
            ? 'Only the sessions you teach are shown.'
            : 'A room cannot hold two sessions at the same date and time.'}</p>
        </div>
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}
        {!sessions ? <div className="empty">Loading…</div>
          : sessions.length === 0 ? <div className="card"><p className="empty">No sessions yet.</p></div>
          : (
            <div className="card">
              <table className="table">
                <thead>
                  <tr><th>Title</th><th>Date</th><th>Time</th><th>Room</th><th>Trainer</th><th>Seats</th></tr>
                </thead>
                <tbody>
                  {sessions.map((s) => (
                    <tr key={s.id}>
                      <td data-label="Title"><strong>{s.title}</strong></td>
                      <td data-label="Date" className="num">{s.sessionDate}</td>
                      <td data-label="Time" className="num">{s.startTime} – {s.endTime}</td>
                      <td data-label="Room">{s.room}</td>
                      <td data-label="Trainer">{s.trainer?.fullName || '—'}</td>
                      <td data-label="Seats" className="num">{s.capacity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
      </div>
    </>
  )
}
