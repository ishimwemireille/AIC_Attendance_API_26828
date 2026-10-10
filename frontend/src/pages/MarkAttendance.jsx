import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'

const STATUSES = ['PRESENT', 'LATE', 'ABSENT', 'EXCUSED']

export default function MarkAttendance() {
  const [sessions, setSessions] = useState([])
  const [students, setStudents] = useState([])
  const [sessionId, setSessionId] = useState('')
  const [already, setAlready] = useState([])
  const [marks, setMarks] = useState({})
  const [error, setError] = useState(null)
  const [done, setDone] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    Promise.all([api.sessions(), api.students()])
      .then(([s, st]) => {
        setSessions(s)
        setStudents(st.filter((x) => x.active))
        if (s.length) setSessionId(s[0].id)
      })
      .catch((p) => setError(p.message))
  }, [])

  useEffect(() => {
    if (!sessionId) return
    setMarks({}); setDone(null)
    api.attendanceBySession(sessionId).then(setAlready).catch(() => setAlready([]))
  }, [sessionId])

  const session = useMemo(() => sessions.find((s) => s.id === sessionId), [sessions, sessionId])
  const markedIds = new Set(already.map((a) => a.student.id))
  const roster = students.filter((s) => !markedIds.has(s.id))

  const tally = STATUSES.reduce((acc, status) => {
    acc[status] = Object.values(marks).filter((m) => m.status === status).length
    return acc
  }, {})
  const chosen = Object.keys(marks).length

  function choose(student, status) {
    setDone(null)
    setMarks((current) => ({
      ...current,
      [student.id]: {
        status,
        timeIn: status === 'PRESENT' ? session?.startTime
          : status === 'LATE' ? addMinutes(session?.startTime, 20) : null,
        remarks: status === 'ABSENT' ? 'Did not attend the session'
          : status === 'EXCUSED' ? 'Excused in advance' : null,
      },
    }))
  }

  async function save() {
    setBusy(true); setError(null); setDone(null)
    const failures = []
    for (const [studentId, mark] of Object.entries(marks)) {
      try {
        await api.mark({ studentId, sessionId, ...mark })
      } catch (problem) {
        const who = students.find((s) => s.id === studentId)
        failures.push(`${who.firstName} ${who.lastName}: ${problem.message}`)
      }
    }
    const saved = Object.keys(marks).length - failures.length
    if (failures.length) setError(failures.join('\n'))
    if (saved > 0) setDone(`${saved} record${saved === 1 ? '' : 's'} saved. Absences trigger an alert.`)
    setMarks({})
    api.attendanceBySession(sessionId).then(setAlready).catch(() => {})
    setBusy(false)
  }

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Mark attendance</h1>
          <p>Each student can be recorded once per session.</p>
        </div>
        <button className="btn" onClick={save} disabled={busy || chosen === 0}>
          {busy ? 'Saving…' : `Save ${chosen || ''}`}
        </button>
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}
        {done && <div className="banner ok">{done}</div>}

        <div className="field" style={{ maxWidth: 460 }}>
          <label htmlFor="session">Session</label>
          <select id="session" value={sessionId} onChange={(e) => setSessionId(e.target.value)}>
            {sessions.length === 0 && <option value="">No sessions available</option>}
            {sessions.map((s) => (
              <option key={s.id} value={s.id}>
                {s.title} — {s.sessionDate} {s.startTime} — {s.room}
              </option>
            ))}
          </select>
        </div>

        <div className="stat-row">
          {STATUSES.map((status) => (
            <div className="card" key={status}>
              <div className="stat-label">{status[0] + status.slice(1).toLowerCase()}</div>
              <div className="stat-value" style={{ margin: '6px 0 0' }}>{tally[status]}</div>
            </div>
          ))}
        </div>

        <div className="card">
          {roster.length === 0 ? (
            <p className="empty">Every active student has already been marked for this session.</p>
          ) : (
            <table className="table">
              <thead><tr><th>Student</th><th>Status</th></tr></thead>
              <tbody>
                {roster.map((student) => (
                  <tr key={student.id}>
                    <td data-label="Student">
                      <strong>{student.firstName} {student.lastName}</strong><br />
                      <span className="stat-note num">{student.regNumber} · Year {student.yearOfStudy}</span>
                    </td>
                    <td data-label="Status" className="stack">
                      <div className="status-group">
                        {STATUSES.map((status) => (
                          <button key={status} type="button" className={`status-btn ${status}`}
                                  aria-pressed={marks[student.id]?.status === status}
                                  onClick={() => choose(student, status)}>
                            {status[0] + status.slice(1).toLowerCase()}
                          </button>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {already.length > 0 && (
          <div className="card">
            <h2 style={{ fontSize: 18, marginBottom: 12 }}>Already recorded</h2>
            <table className="table">
              <thead><tr><th>Student</th><th>Time in</th><th>Status</th><th>Taken by</th></tr></thead>
              <tbody>
                {already.map((a) => (
                  <tr key={a.id}>
                    <td data-label="Student">{a.student.firstName} {a.student.lastName}</td>
                    <td data-label="Time in" className="num">{a.timeIn || '—'}</td>
                    <td data-label="Status"><span className={`pill ${a.status}`}>{a.status}</span></td>
                    <td data-label="Taken by">{a.recordedBy?.fullName || '—'}</td>
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

function addMinutes(time, minutes) {
  if (!time) return null
  const [h, m] = time.split(':').map(Number)
  const total = h * 60 + m + minutes
  return `${String(Math.floor(total / 60) % 24).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}
