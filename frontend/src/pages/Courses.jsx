import { useEffect, useState } from 'react'
import { api, importRegister } from '../api'

const BLANK = {
  code: '', title: '', description: '', sequence: 1,
  startDate: '', endDate: '', trainerId: '', active: true,
}

export default function Courses({ user }) {
  const [courses, setCourses] = useState(null)
  const [trainers, setTrainers] = useState([])
  const [form, setForm] = useState(BLANK)
  const [editing, setEditing] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const [importResult, setImportResult] = useState(null)
  const [importing, setImporting] = useState(null)

  const canWrite = user.roles.includes('ADMIN') || user.roles.includes('COHORT_LEADER')
  const today = new Date().toISOString().slice(0, 10)

  function load() {
    api.courses().then(setCourses).catch((p) => setError(p.message))
  }

  useEffect(() => {
    load()
    if (canWrite) api.trainers().then(setTrainers).catch(() => setTrainers([]))
  }, [])

  function startEdit(course) {
    setEditing(course.id)
    setForm({
      code: course.code, title: course.title, description: course.description || '',
      sequence: course.sequence, startDate: course.startDate, endDate: course.endDate,
      trainerId: course.trainer?.id || '', active: course.active,
    })
    setShowForm(true); setError(null)
  }

  async function save(event) {
    event.preventDefault()
    setBusy(true); setError(null)
    const body = { ...form, sequence: Number(form.sequence) }
    try {
      if (editing) await api.updateCourse(editing, body)
      else await api.createCourse(body)
      setShowForm(false); setEditing(null); setForm(BLANK); load()
    } catch (problem) {
      setError(problem.message)
    } finally {
      setBusy(false)
    }
  }

  async function onRegister(event, course) {
    const file = event.target.files?.[0]
    if (!file) return
    setImporting(course.id); setError(null); setImportResult(null)
    try {
      setImportResult({ course: course.title, ...(await importRegister(course.id, file)) })
    } catch (problem) {
      setError(problem.message)
    } finally {
      setImporting(null)
      event.target.value = ''
    }
  }

  async function remove(course) {
    if (!window.confirm(`Delete "${course.title}"?`)) return
    try { await api.deleteCourse(course.id); load() } catch (p) { setError(p.message) }
  }

  function stateOf(course) {
    if (course.endDate < today) return { label: 'Finished', pill: 'muted' }
    if (course.startDate > today) return { label: 'Upcoming', pill: 'EXCUSED' }
    return { label: 'Running now', pill: 'PRESENT' }
  }

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Courses</h1>
          <p>The cohort takes these one after another, so their dates may not overlap. Import a register to bring in students, sessions and attendance at once.</p>
        </div>
        {canWrite && (
          <a className="btn ghost" href="/api/courses/import/template">Template</a>
        )}
        {canWrite && (
          <button className="btn" onClick={() => {
            if (showForm) { setShowForm(false); return }
            setEditing(null); setForm(BLANK); setShowForm(true); setError(null)
          }}>
            {showForm ? 'Close' : 'New course'}
          </button>
        )}
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}

        {importResult && (
          <div className={importResult.problems.length ? 'banner error' : 'banner ok'}>
            <strong>Register imported into {importResult.course}.</strong>
            <br />
            {importResult.studentsCreated} student(s) added,
            {' '}{importResult.studentsAlreadyKnown} already known,
            {' '}{importResult.sessionsCreated} session(s) created,
            {' '}{importResult.attendanceCreated} attendance record(s) written.
            {importResult.columnsIgnored.length > 0 && (
              <div style={{ marginTop: 6 }}>
                Columns that were not a date, so carried no attendance:
                {' '}{importResult.columnsIgnored.join(', ')}
              </div>
            )}
            {importResult.problems.length > 0 && (
              <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
                {importResult.problems.map((p, i) => (
                  <li key={i}>{p.sheet} row {p.row}: {p.message}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        {showForm && canWrite && (
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: 18, marginBottom: 14 }}>
              {editing ? 'Edit course' : 'New course'}
            </h2>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="code">Code</label>
                <input id="code" value={form.code} required placeholder="WEBTECH"
                       onChange={(e) => setForm({ ...form, code: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="sequence">Position in the programme</label>
                <input id="sequence" type="number" min="1" value={form.sequence} required
                       onChange={(e) => setForm({ ...form, sequence: e.target.value })} />
                <span className="hint">1 is taught first.</span>
              </div>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label htmlFor="ctitle">Title</label>
                <input id="ctitle" value={form.title} required
                       onChange={(e) => setForm({ ...form, title: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="start">Starts</label>
                <input id="start" type="date" value={form.startDate} required
                       onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="end">Ends</label>
                <input id="end" type="date" value={form.endDate} required
                       onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
                <span className="hint">Must not overlap another course.</span>
              </div>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label htmlFor="ctrainer">Trainer</label>
                <select id="ctrainer" value={form.trainerId} required
                        onChange={(e) => setForm({ ...form, trainerId: e.target.value })}>
                  <option value="">Choose a trainer…</option>
                  {trainers.map((t) => <option key={t.id} value={t.id}>{t.fullName}</option>)}
                </select>
              </div>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label htmlFor="desc">Description</label>
                <input id="desc" value={form.description}
                       onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </div>
            </div>
            <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
              <button className="btn" type="submit" disabled={busy}>
                {busy ? 'Saving…' : editing ? 'Update course' : 'Create course'}
              </button>
              <button className="btn ghost" type="button" onClick={() => setShowForm(false)}>Cancel</button>
            </div>
          </form>
        )}

        {!courses ? <div className="empty">Loading…</div>
          : courses.length === 0 ? <div className="card"><p className="empty">No courses yet.</p></div>
          : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {courses.map((c) => {
                const state = stateOf(c)
                return (
                  <div className="card" key={c.id}>
                    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, flexWrap: 'wrap' }}>
                      <span className="stat-value" style={{ fontSize: 22, width: 34 }}>{c.sequence}</span>
                      <span style={{ flexGrow: 1, minWidth: 0 }}>
                        <strong style={{ fontSize: 16 }}>{c.title}</strong>
                        <span className="stat-note" style={{ marginLeft: 8 }}>{c.code}</span>
                        <br />
                        <span className="stat-note num">{c.startDate} to {c.endDate}</span>
                        <span className="stat-note"> · {c.trainer?.fullName}</span>
                      </span>
                      <span className={`pill ${state.pill}`}>{state.label}</span>
                      {canWrite && (
                        <span style={{ display: 'flex', gap: 8 }}>
                          <label className="btn ghost"
                                 style={{ minHeight: 36, padding: '0 12px', cursor: 'pointer' }}>
                            {importing === c.id ? 'Reading…' : 'Import register'}
                            <input type="file" accept=".xlsx"
                                   onChange={(e) => onRegister(e, c)}
                                   style={{ position: 'absolute', width: 1, height: 1, opacity: 0 }} />
                          </label>
                          <button className="btn ghost" style={{ minHeight: 36, padding: '0 12px' }}
                                  onClick={() => startEdit(c)}>Edit</button>
                          <button className="btn ghost" style={{ minHeight: 36, padding: '0 12px' }}
                                  onClick={() => remove(c)}>Delete</button>
                        </span>
                      )}
                    </div>
                    {c.description && (
                      <p className="stat-note" style={{ margin: '10px 0 0' }}>{c.description}</p>
                    )}
                  </div>
                )
              })}
            </div>
          )}
      </div>
    </>
  )
}
