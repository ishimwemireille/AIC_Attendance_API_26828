import { useEffect, useState } from 'react'
import { api } from '../api'

const BLANK = {
  regNumber: '', firstName: '', lastName: '', email: '', phone: '',
  gender: 'Female', sector: 'Health', studentGroup: 'GROUP A', yearOfStudy: 1,
}

export default function Students({ user }) {
  const [students, setStudents] = useState(null)
  const [query, setQuery] = useState('')
  const [form, setForm] = useState(BLANK)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const canWrite = user.roles.includes('ADMIN') || user.roles.includes('COHORT_LEADER')

  function load() {
    api.students().then(setStudents).catch((p) => setError(p.message))
  }
  useEffect(load, [])

  async function save(event) {
    event.preventDefault()
    setBusy(true); setError(null)
    try {
      const body = { ...form, yearOfStudy: Number(form.yearOfStudy) }
      if (editing) await api.updateStudent(editing, body)
      else await api.createStudent(body)
      setForm(BLANK); setEditing(null); setShowForm(false); load()
    } catch (problem) {
      setError(problem.message)
    } finally {
      setBusy(false)
    }
  }


  async function remove(student) {
    if (!window.confirm(`Remove ${student.firstName} ${student.lastName}?`)) return
    try { await api.deleteStudent(student.id); load() } catch (p) { setError(p.message) }
  }

  function startEdit(student) {
    setEditing(student.id)
    setForm({
      regNumber: student.regNumber, firstName: student.firstName, lastName: student.lastName,
      email: student.email, phone: student.phone, gender: student.gender,
      sector: student.sector, studentGroup: student.studentGroup || '',
      yearOfStudy: student.yearOfStudy,
    })
    setShowForm(true); setError(null)
  }

  const shown = (students || []).filter((s) => {
    const q = query.trim().toLowerCase()
    return !q || `${s.firstName} ${s.lastName} ${s.regNumber} ${s.sector} ${s.studentGroup || ''}`.toLowerCase().includes(q)
  })

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Students</h1>
          <p>{students ? `${students.filter((s) => s.active).length} active of ${students.length}` : 'Loading…'}</p>
        </div>
        {canWrite && (
          <button className="btn" onClick={() => {
            if (showForm) { setShowForm(false); return }
            setEditing(null); setForm(BLANK); setShowForm(true); setError(null)
          }}>
            {showForm ? 'Close' : 'Add student'}
          </button>
        )}
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}


        {showForm && canWrite && (
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: 18, marginBottom: 14 }}>
              {editing ? 'Edit student' : 'New student'}
            </h2>
            <div className="form-grid">
              <Field label="Registration number" value={form.regNumber} hint="Exactly 5 digits"
                     onChange={(v) => setForm({ ...form, regNumber: v })} />
              <Field label="Email" type="email" value={form.email} optional
                     hint="Optional. Without one, no email alert is sent."
                     onChange={(v) => setForm({ ...form, email: v })} />
              <Field label="First name" value={form.firstName}
                     onChange={(v) => setForm({ ...form, firstName: v })} />
              <Field label="Last name" value={form.lastName}
                     onChange={(v) => setForm({ ...form, lastName: v })} />
              <Field label="Phone" value={form.phone} optional
                     hint="Optional. 10 digits starting 07."
                     onChange={(v) => setForm({ ...form, phone: v })} />
              <div className="field">
                <label htmlFor="gender">Gender</label>
                <select id="gender" value={form.gender}
                        onChange={(e) => setForm({ ...form, gender: e.target.value })}>
                  <option>Female</option><option>Male</option>
                </select>
              </div>
              <div className="field">
                <label htmlFor="sector">Sector</label>
                <select id="sector" value={form.sector}
                        onChange={(e) => setForm({ ...form, sector: e.target.value })}>
                  <option>Agriculture</option>
                  <option>Crime Prevention</option>
                  <option>Education</option>
                  <option>Fintech</option>
                  <option>Health</option>
                </select>
              </div>
              <div className="field">
                <label htmlFor="group">Group</label>
                <select id="group" value={form.studentGroup}
                        onChange={(e) => setForm({ ...form, studentGroup: e.target.value })}>
                  <option>GROUP A</option>
                  <option>GROUP B</option>
                </select>
                <span className="hint">Both groups take the same course at the same time.</span>
              </div>
              <div className="field">
                <label htmlFor="year">Year of study</label>
                <select id="year" value={form.yearOfStudy}
                        onChange={(e) => setForm({ ...form, yearOfStudy: e.target.value })}>
                  {[1, 2, 3, 4, 5].map((y) => <option key={y} value={y}>Year {y}</option>)}
                </select>
              </div>
            </div>
            <button className="btn" type="submit" disabled={busy} style={{ marginTop: 16 }}>
              {busy ? 'Saving…' : editing ? 'Update student' : 'Save student'}
            </button>
          </form>
        )}

        <div className="field" style={{ maxWidth: 420 }}>
          <label htmlFor="search">Search</label>
          <input id="search" type="search" value={query} placeholder="Name, registration number, sector or group"
                 onChange={(e) => setQuery(e.target.value)} />
        </div>

        <div className="card">
          {!students ? <p className="empty">Loading…</p>
            : shown.length === 0 ? <p className="empty">No student matches that search.</p>
            : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Reg no.</th><th>Student</th><th>Sector</th><th>Group</th>
                    <th>Year</th><th>Phone</th><th>Status</th>{canWrite && <th></th>}
                  </tr>
                </thead>
                <tbody>
                  {shown.map((s) => (
                    <tr key={s.id}>
                      <td data-label="Reg no." className="num"><strong>{s.regNumber}</strong></td>
                      <td data-label="Student">{s.firstName} {s.lastName}<br />
                        <span className="stat-note">{s.email}</span></td>
                      <td data-label="Sector">{s.sector}</td>
                      <td data-label="Group">
                        {s.studentGroup
                          ? <span className="pill muted">{s.studentGroup}</span>
                          : <span className="stat-note">—</span>}
                      </td>
                      <td data-label="Year" className="num">{s.yearOfStudy}</td>
                      <td data-label="Phone" className="num">{s.phone}</td>
                      <td data-label="Status">
                        <span className={s.active ? 'pill PRESENT' : 'pill muted'}>
                          {s.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      {canWrite && (
                        <td data-label="Actions">
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button className="btn ghost" style={{ minHeight: 36, padding: '0 12px' }}
                                    onClick={() => startEdit(s)}>Edit</button>
                            <button className="btn ghost" style={{ minHeight: 36, padding: '0 12px' }}
                                    onClick={() => remove(s)}>Remove</button>
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

function Field({ label, value, onChange, type = 'text', hint, optional }) {
  const id = label.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} value={value} required={!optional}
             onChange={(e) => onChange(e.target.value)} />
      {hint && <span className="hint">{hint}</span>}
    </div>
  )
}
