import { useEffect, useState } from 'react'
import { api, uploadStudents } from '../api'

const BLANK = {
  regNumber: '', firstName: '', lastName: '', email: '', phone: '',
  gender: 'Female', program: 'Information Technology', yearOfStudy: 1,
}

export default function Students({ user }) {
  const [students, setStudents] = useState(null)
  const [query, setQuery] = useState('')
  const [form, setForm] = useState(BLANK)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)
  const [importResult, setImportResult] = useState(null)
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

  async function onFile(event) {
    const file = event.target.files?.[0]
    if (!file) return
    setBusy(true); setError(null); setImportResult(null)
    try {
      setImportResult(await uploadStudents(file))
      load()
    } catch (problem) {
      setError(problem.message)
    } finally {
      setBusy(false)
      event.target.value = ''
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
      program: student.program, yearOfStudy: student.yearOfStudy,
    })
    setShowForm(true); setError(null)
  }

  const shown = (students || []).filter((s) => {
    const q = query.trim().toLowerCase()
    return !q || `${s.firstName} ${s.lastName} ${s.regNumber} ${s.program}`.toLowerCase().includes(q)
  })

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Students</h1>
          <p>{students ? `${students.filter((s) => s.active).length} active of ${students.length}` : 'Loading…'}</p>
        </div>
        {canWrite && (
          <a className="btn ghost" href="/api/students/import/template">Template</a>
        )}
        {canWrite && (
          <label className="btn ghost" style={{ cursor: 'pointer' }}>
            {busy ? 'Reading…' : 'Import Excel'}
            <input type="file" accept=".xlsx" onChange={onFile}
                   style={{ position: 'absolute', width: 1, height: 1, opacity: 0 }} />
          </label>
        )}
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

        {importResult && (
          <div className={importResult.failed ? 'banner error' : 'banner ok'}>
            <strong>{importResult.imported} student(s) imported, {importResult.failed} rejected.</strong>
            {importResult.errors.length > 0 && (
              <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
                {importResult.errors.map((e) => (
                  <li key={e.row}>Row {e.row}: {e.message}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        {showForm && canWrite && (
          <form className="card" onSubmit={save}>
            <h2 style={{ fontSize: 18, marginBottom: 14 }}>
              {editing ? 'Edit student' : 'New student'}
            </h2>
            <div className="form-grid">
              <Field label="Registration number" value={form.regNumber} hint="Exactly 5 digits"
                     onChange={(v) => setForm({ ...form, regNumber: v })} />
              <Field label="Email" type="email" value={form.email}
                     onChange={(v) => setForm({ ...form, email: v })} />
              <Field label="First name" value={form.firstName}
                     onChange={(v) => setForm({ ...form, firstName: v })} />
              <Field label="Last name" value={form.lastName}
                     onChange={(v) => setForm({ ...form, lastName: v })} />
              <Field label="Phone" value={form.phone} hint="10 digits starting 07"
                     onChange={(v) => setForm({ ...form, phone: v })} />
              <div className="field">
                <label htmlFor="gender">Gender</label>
                <select id="gender" value={form.gender}
                        onChange={(e) => setForm({ ...form, gender: e.target.value })}>
                  <option>Female</option><option>Male</option>
                </select>
              </div>
              <div className="field">
                <label htmlFor="program">Program</label>
                <select id="program" value={form.program}
                        onChange={(e) => setForm({ ...form, program: e.target.value })}>
                  <option>Information Technology</option>
                  <option>Software Engineering</option>
                  <option>Business IT</option>
                </select>
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
          <input id="search" type="search" value={query} placeholder="Name, registration number or program"
                 onChange={(e) => setQuery(e.target.value)} />
        </div>

        <div className="card">
          {!students ? <p className="empty">Loading…</p>
            : shown.length === 0 ? <p className="empty">No student matches that search.</p>
            : (
              <table className="table">
                <thead>
                  <tr>
                    <th>Reg no.</th><th>Student</th><th>Program</th>
                    <th>Year</th><th>Phone</th><th>Status</th>{canWrite && <th></th>}
                  </tr>
                </thead>
                <tbody>
                  {shown.map((s) => (
                    <tr key={s.id}>
                      <td data-label="Reg no." className="num"><strong>{s.regNumber}</strong></td>
                      <td data-label="Student">{s.firstName} {s.lastName}<br />
                        <span className="stat-note">{s.email}</span></td>
                      <td data-label="Program">{s.program}</td>
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

function Field({ label, value, onChange, type = 'text', hint }) {
  const id = label.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} value={value} required onChange={(e) => onChange(e.target.value)} />
      {hint && <span className="hint">{hint}</span>}
    </div>
  )
}
