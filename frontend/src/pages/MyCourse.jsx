import { useEffect, useState } from 'react'
import { api } from '../api'

export default function MyCourse() {
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState('')
  const [progress, setProgress] = useState(null)
  const [activities, setActivities] = useState([])
  const [entry, setEntry] = useState({ activityDate: '', title: '', notes: '' })
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.myCourses()
      .then((mine) => {
        setCourses(mine)
        if (mine.length) setCourseId(mine[0].id)
      })
      .catch((p) => setError(p.message))
  }, [])

  function load(id) {
    if (!id) return
    api.courseProgress(id).then(setProgress).catch((p) => setError(p.message))
    api.courseActivities(id).then(setActivities).catch(() => setActivities([]))
  }

  useEffect(() => { load(courseId) }, [courseId])

  async function save(event) {
    event.preventDefault()
    setBusy(true); setError(null)
    try {
      await api.logActivity(courseId, entry)
      setEntry({ activityDate: '', title: '', notes: '' })
      setShowForm(false)
      load(courseId)
    } catch (problem) {
      setError(problem.message)
    } finally {
      setBusy(false)
    }
  }

  if (courses.length === 0) {
    return (
      <>
        <Top />
        <div className="content">
          {error && <div className="banner error">{error}</div>}
          <div className="card">
            <p className="empty">You do not lead any course yet.</p>
          </div>
        </div>
      </>
    )
  }

  return (
    <>
      <Top onLog={() => {
        setShowForm((v) => !v)
        setEntry({ activityDate: new Date().toISOString().slice(0, 10), title: '', notes: '' })
      }} showForm={showForm} />

      <div className="content">
        {error && <div className="banner error">{error}</div>}

        {courses.length > 1 && (
          <div className="field" style={{ maxWidth: 460 }}>
            <label htmlFor="course">Course</label>
            <select id="course" value={courseId} onChange={(e) => setCourseId(e.target.value)}>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>{c.sequence}. {c.title}</option>
              ))}
            </select>
          </div>
        )}

        {showForm && (
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: 18, marginBottom: 14 }}>Log what you covered</h2>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="adate">Date</label>
                <input id="adate" type="date" value={entry.activityDate} required
                       onChange={(e) => setEntry({ ...entry, activityDate: e.target.value })} />
                <span className="hint">Must fall inside the course dates.</span>
              </div>
              <div className="field">
                <label htmlFor="atitle">What you covered</label>
                <input id="atitle" value={entry.title} required
                       onChange={(e) => setEntry({ ...entry, title: e.target.value })} />
              </div>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label htmlFor="anotes">Notes</label>
                <input id="anotes" value={entry.notes}
                       placeholder="How it went, who needs help, what is next"
                       onChange={(e) => setEntry({ ...entry, notes: e.target.value })} />
              </div>
            </div>
            <button className="btn" type="submit" disabled={busy} style={{ marginTop: 16 }}>
              {busy ? 'Saving…' : 'Save entry'}
            </button>
          </form>
        )}

        {!progress ? <div className="empty">Loading…</div> : (
          <>
            <div className="stat-row">
              <Stat label="Sessions marked" value={`${progress.sessionsDelivered} / ${progress.sessionsPlanned}`}
                    note="attendance taken" />
              <Stat label="Attendance rate" value={`${progress.attendanceRate}%`}
                    note={`${progress.recordsWritten} records`} />
              <Stat label="Course elapsed" value={`${progress.elapsedPercent}%`}
                    note={`${progress.course.startDate} to ${progress.course.endDate}`} />
              <Stat label="Students at risk" value={progress.atRisk.length} note="below 75%" />
            </div>

            <div className="card">
              <h2 style={{ fontSize: 18, marginBottom: 10 }}>Progress</h2>
              <Bar label="Sessions marked" value={progress.sessionsPlanned
                ? Math.round((progress.sessionsDelivered / progress.sessionsPlanned) * 100) : 0} />
              <Bar label="Course elapsed" value={progress.elapsedPercent} />
              <p className="stat-note" style={{ marginTop: 10 }}>
                If the course has elapsed further than the sessions marked, attendance is behind.
              </p>
            </div>

            <div className="card">
              <h2 style={{ fontSize: 18, marginBottom: 10 }}>Students below 75%</h2>
              {progress.atRisk.length === 0
                ? <p className="empty">Nobody on this course is below the threshold.</p>
                : (
                  <table className="table">
                    <thead>
                      <tr><th>Student</th><th>Reg no.</th><th>Attended</th><th>Rate</th></tr>
                    </thead>
                    <tbody>
                      {progress.atRisk.map((s) => (
                        <tr key={s.studentId}>
                          <td data-label="Student">{s.fullName}</td>
                          <td data-label="Reg no." className="num">{s.regNumber}</td>
                          <td data-label="Attended" className="num">{s.attended} of {s.total}</td>
                          <td data-label="Rate"><span className="pill ABSENT">{s.rate}%</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
            </div>

            <div className="card">
              <h2 style={{ fontSize: 18, marginBottom: 10 }}>Activity log</h2>
              {activities.length === 0
                ? <p className="empty">Nothing logged yet. Attendance says who was there; this says what happened.</p>
                : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {activities.map((a) => (
                      <div key={a.id} style={{ borderLeft: '3px solid var(--border)', paddingLeft: 12 }}>
                        <span className="stat-note num">{a.activityDate}</span>
                        <br />
                        <strong>{a.title}</strong>
                        {a.notes && <p style={{ margin: '4px 0 0', fontSize: 14 }}>{a.notes}</p>}
                      </div>
                    ))}
                  </div>
                )}
            </div>
          </>
        )}
      </div>
    </>
  )
}

function Bar({ label, value }) {
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', fontSize: 13, marginBottom: 5 }}>
        <span style={{ flexGrow: 1 }}>{label}</span>
        <strong className="num">{value}%</strong>
      </div>
      <div style={{ height: 10, borderRadius: 999, background: 'var(--border-soft)' }}>
        <div style={{ height: 10, width: `${value}%`, borderRadius: 999, background: 'var(--ink-soft)' }} />
      </div>
    </div>
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

function Top({ onLog, showForm }) {
  return (
    <div className="topbar">
      <div className="grow">
        <h1>My course</h1>
        <p>Progress through the course you lead, and the record of what you covered.</p>
      </div>
      {onLog && <button className="btn" onClick={onLog}>{showForm ? 'Close' : 'Log activity'}</button>}
    </div>
  )
}
