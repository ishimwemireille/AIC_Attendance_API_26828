import { useEffect, useState } from 'react'
import { api } from '../api'

const ROLES = ['ADMIN', 'COHORT_LEADER', 'TRAINER']
const BLANK = { email: '', fullName: '', roles: ['TRAINER'], courseId: '' }

export default function Users() {
  const [users, setUsers] = useState(null)
  const [courses, setCourses] = useState([])
  const [form, setForm] = useState(BLANK)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  function load() {
    api.users().then(setUsers).catch((p) => setError(p.message))
    api.courses().then(setCourses).catch(() => setCourses([]))
  }
  useEffect(load, [])

  function toggleRole(role) {
    setForm((current) => ({
      ...current,
      roles: current.roles.includes(role)
        ? current.roles.filter((r) => r !== role)
        : [...current.roles, role],
    }))
  }

  async function save(event) {
    event.preventDefault()
    setBusy(true); setError(null)
    try {
      await api.createUser({ ...form, courseId: form.courseId || null })
      setForm(BLANK); setShowForm(false); load()
    } catch (problem) {
      setError(problem.message)
    } finally {
      setBusy(false)
    }
  }

  function leadsOf(user) {
    return courses.filter((course) => course.trainer?.id === user.id)
  }

  async function setActive(user, value) {
    try { await api.setUserActive(user.id, value); load() } catch (p) { setError(p.message) }
  }

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Access</h1>
          <p>Signing in with Google is not the same as being allowed in. An account must appear here first.</p>
        </div>
        <button className="btn" onClick={() => setShowForm((v) => !v)}>
          {showForm ? 'Close' : 'Grant access'}
        </button>
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}

        {showForm && (
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: 18, marginBottom: 6 }}>Grant access</h2>
            <p className="hint" style={{ display: 'block', marginBottom: 14 }}>
              No password is set here. The person signs in with the Google account
              behind this address, so the system never holds a password of its own.
            </p>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="email">AUCA email</label>
                <input id="email" type="email" value={form.email} required
                       placeholder="name@auca.ac.rw"
                       onChange={(e) => setForm({ ...form, email: e.target.value })} />
                <span className="hint">Only the permitted domain is accepted.</span>
              </div>
              <div className="field">
                <label htmlFor="fullName">Full name</label>
                <input id="fullName" value={form.fullName} required
                       onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
              </div>
            </div>

            <fieldset style={{ border: 'none', padding: 0, margin: '14px 0 0' }}>
              <legend style={{ padding: 0, fontSize: 13, fontWeight: 600, marginBottom: 8 }}>Roles</legend>
              <div className="status-group">
                {ROLES.map((role) => (
                  <button key={role} type="button" className="status-btn"
                          aria-pressed={form.roles.includes(role)}
                          style={form.roles.includes(role)
                            ? { background: 'var(--ink)', color: '#fff', borderColor: 'var(--ink)', fontWeight: 700 }
                            : undefined}
                          onClick={() => toggleRole(role)}>
                    {role.replace('_', ' ').toLowerCase()}
                  </button>
                ))}
              </div>
              <span className="hint" style={{ display: 'block', marginTop: 8 }}>
                At least one is required. A user may hold more than one.
              </span>
            </fieldset>

            {form.roles.includes('TRAINER') && (
              <div className="field" style={{ marginTop: 14 }}>
                <label htmlFor="courseId">Course to lead</label>
                <select id="courseId" value={form.courseId}
                        onChange={(e) => setForm({ ...form, courseId: e.target.value })}>
                  <option value="">Decide later</option>
                  {courses.map((course) => (
                    <option key={course.id} value={course.id}>
                      {course.sequence}. {course.title}
                      {course.trainer ? ` — currently ${course.trainer.fullName}` : ''}
                    </option>
                  ))}
                </select>
                <span className="hint">
                  A trainer with no course signs in to an empty screen. Choosing one here
                  hands it over straight away; it can also be changed on the Courses page.
                </span>
              </div>
            )}

            <button className="btn" type="submit" disabled={busy || form.roles.length === 0}
                    style={{ marginTop: 16 }}>
              {busy ? 'Saving…' : 'Grant access'}
            </button>
          </form>
        )}

        <div className="card">
          {!users ? <p className="empty">Loading…</p> : (
            <table className="table">
              <thead>
                <tr><th>Name</th><th>Email</th><th>Roles</th><th>Leads</th><th>Status</th><th></th></tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td data-label="Name"><strong>{u.fullName}</strong></td>
                    <td data-label="Email">{u.email}</td>
                    <td data-label="Roles">
                      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        {u.roles.map((r) => (
                          <span key={r.id} className="pill muted">
                            {r.name.replace('_', ' ').toLowerCase()}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td data-label="Leads">
                      {leadsOf(u).length === 0
                        ? <span className="stat-note">—</span>
                        : <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                            {leadsOf(u).map((c) => (
                              <span key={c.id} className="pill muted">{c.title}</span>
                            ))}
                          </div>}
                    </td>
                    <td data-label="Status">
                      <span className={u.active ? 'pill PRESENT' : 'pill muted'}>
                        {u.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td data-label="Actions">
                      <button className="btn ghost" style={{ minHeight: 36, padding: '0 12px' }}
                              onClick={() => setActive(u, !u.active)}>
                        {u.active ? 'Deactivate' : 'Reactivate'}
                      </button>
                    </td>
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
