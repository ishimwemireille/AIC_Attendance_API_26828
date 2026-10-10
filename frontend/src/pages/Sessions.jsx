import { useEffect, useState } from 'react'
import { api } from '../api'

const BLANK = {
  title: '', sessionDate: '', startTime: '08:00', endTime: '10:00',
  room: '', trainerId: '', capacity: 40, active: true,
}

export default function Sessions({ user }) {
  const [sessions, setSessions] = useState(null)
  const [trainers, setTrainers] = useState([])
  const [form, setForm] = useState(BLANK)
  const [editing, setEditing] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const canWrite = user.roles.includes('ADMIN') || user.roles.includes('COHORT_LEADER')
  const trainerOnly = user.roles.length === 1 && user.roles[0] === 'TRAINER'

  function load() {
    api.sessions().then(setSessions).catch((p) => setError(p.message))
  }

  useEffect(() => {
    load()
    if (canWrite) api.trainers().then(setTrainers).catch(() => setTrainers([]))
  }, [])

  function startEdit(session) {
    setEditing(session.id)
    setForm({
      title: session.title,
      sessionDate: session.sessionDate,
      startTime: session.startTime,
      endTime: session.endTime,
      room: session.room,
      trainerId: session.trainer?.id || '',
      capacity: session.capacity,
      active: session.active,
    })
    setShowForm(true)
    setError(null)
  }

  function startCreate() {
    setEditing(null)
    setForm({ ...BLANK, sessionDate: new Date().toISOString().slice(0, 10) })
    setShowForm(true)
    setError(null)
  }

  async function save(event) {
    event.preventDefault()
    setBusy(true); setError(null)
    const body = { ...form, capacity: Number(form.capacity) }
    try {
      if (editing) await api.updateSession(editing, body)
      else await api.createSession(body)
      setShowForm(false); setEditing(null); load()
    } catch (problem) {
      setError(problem.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(session) {
    if (!window.confirm(`Delete "${session.title}"?`)) return
    try { await api.deleteSession(session.id); load() } catch (p) { setError(p.message) }
  }

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Sessions</h1>
          <p>{trainerOnly
            ? 'Only the sessions you teach are shown.'
            : 'A room cannot hold two sessions at the same date and time.'}</p>
        </div>
        {canWrite && (
          <button className="btn" onClick={showForm ? () => setShowForm(false) : startCreate}>
            {showForm ? 'Close' : 'New session'}
          </button>
        )}
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}

        {showForm && canWrite && (
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: 18, marginBottom: 14 }}>
              {editing ? 'Edit session' : 'New session'}
            </h2>
            <div className="form-grid">
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label htmlFor="title">Title</label>
                <input id="title" value={form.title} required
                       onChange={(e) => setForm({ ...form, title: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="date">Date</label>
                <input id="date" type="date" value={form.sessionDate} required
                       onChange={(e) => setForm({ ...form, sessionDate: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="room">Room</label>
                <input id="room" value={form.room} required
                       onChange={(e) => setForm({ ...form, room: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="start">Start time</label>
                <input id="start" type="time" value={form.startTime} required
                       onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="end">End time</label>
                <input id="end" type="time" value={form.endTime} required
                       onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
                <span className="hint">Must be after the start time.</span>
              </div>
              <div className="field">
                <label htmlFor="trainer">Trainer</label>
                <select id="trainer" value={form.trainerId} required
                        onChange={(e) => setForm({ ...form, trainerId: e.target.value })}>
                  <option value="">Choose a trainer…</option>
                  {trainers.map((t) => (
                    <option key={t.id} value={t.id}>{t.fullName}</option>
                  ))}
                </select>
                <span className="hint">Only users holding the TRAINER role appear.</span>
              </div>
              <div className="field">
                <label htmlFor="capacity">Capacity</label>
                <input id="capacity" type="number" min="1" value={form.capacity} required
                       onChange={(e) => setForm({ ...form, capacity: e.target.value })} />
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
              <button className="btn" type="submit" disabled={busy}>
                {busy ? 'Saving…' : editing ? 'Update session' : 'Create session'}
              </button>
              <button className="btn ghost" type="button" onClick={() => setShowForm(false)}>Cancel</button>
            </div>
          </form>
        )}

        <div className="card">
          {!sessions ? <p className="empty">Loading…</p>
            : sessions.length === 0 ? <p className="empty">No sessions yet.</p>
            : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Title</th><th>Date</th><th>Time</th><th>Room</th>
                    <th>Trainer</th><th>Seats</th>{canWrite && <th></th>}
                  </tr>
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
                      {canWrite && (
                        <td data-label="Actions">
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button className="btn ghost" style={{ minHeight: 36, padding: '0 12px' }}
                                    onClick={() => startEdit(s)}>Edit</button>
                            <button className="btn ghost" style={{ minHeight: 36, padding: '0 12px' }}
                                    onClick={() => remove(s)}>Delete</button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
        </div>
      </div>
    </>
  )
}
