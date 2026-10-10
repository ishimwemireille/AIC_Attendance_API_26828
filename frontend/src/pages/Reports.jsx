import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'

const THRESHOLD = 75

export default function Reports() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([api.attendances(), api.sessions(), api.students()])
      .then(([attendances, sessions, students]) => setData({ attendances, sessions, students }))
      .catch((problem) => setError(problem.message))
  }, [])

  const report = useMemo(() => {
    if (!data) return null
    const { attendances, sessions, students } = data

    const counted = (list) => list.filter((a) => a.status !== 'ABSENT').length
    const rate = (list) => (list.length ? Math.round((counted(list) / list.length) * 100) : 0)

    const bySession = sessions.map((session) => {
      const rows = attendances.filter((a) => a.session.id === session.id)
      const tally = ['PRESENT', 'LATE', 'EXCUSED', 'ABSENT'].reduce((acc, status) => {
        acc[status] = rows.filter((a) => a.status === status).length
        return acc
      }, {})
      return { session, rows, tally, rate: rate(rows) }
    }).filter((s) => s.rows.length > 0)

    const byStudent = students.map((student) => {
      const rows = attendances.filter((a) => a.student.id === student.id)
      return { student, rows, rate: rate(rows) }
    }).filter((s) => s.rows.length > 0)

    return {
      overall: rate(attendances),
      sessionsHeld: bySession.length,
      records: attendances.length,
      bySession,
      atRisk: byStudent.filter((s) => s.rate < THRESHOLD).sort((a, b) => a.rate - b.rate),
    }
  }, [data])

  function exportCsv() {
    const header = 'Student,Registration number,Sessions,Attended,Rate %\n'
    const rows = data.students.map((student) => {
      const mine = data.attendances.filter((a) => a.student.id === student.id)
      const attended = mine.filter((a) => a.status !== 'ABSENT').length
      const pct = mine.length ? Math.round((attended / mine.length) * 100) : 0
      return `"${student.firstName} ${student.lastName}",${student.regNumber},${mine.length},${attended},${pct}`
    }).join('\n')

    const blob = new Blob([header + rows], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `attendance-report-${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
  }

  if (error) return <Shell><div className="banner error">{error}</div></Shell>
  if (!report) return <Shell><div className="empty">Loading…</div></Shell>

  return (
    <Shell onExport={exportCsv}>
      <div className="stat-row">
        <Stat label="Overall attendance rate" value={`${report.overall}%`}
              note={`${report.records} records`} />
        <Stat label="Sessions with records" value={report.sessionsHeld} note="marked so far" />
        <Stat label="Records written" value={report.records} note="no duplicates possible" />
        <Stat label={`Below ${THRESHOLD}%`} value={report.atRisk.length} note="need follow-up" />
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, flexWrap: 'wrap' }}>
          <h2 style={{ fontSize: 18, flexGrow: 1 }}>Attendance by session</h2>
          <Legend />
        </div>
        {report.bySession.length === 0 ? (
          <p className="empty">Nothing has been marked yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 14 }}>
            {report.bySession.map(({ session, rows, tally, rate }) => (
              <div key={session.id} style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
                  <strong style={{ flexGrow: 1, minWidth: 0 }}>{session.title}</strong>
                  <span className="stat-note num">{session.sessionDate}</span>
                  <span className="num" style={{ fontWeight: 700, width: 52, textAlign: 'right',
                        color: rate >= THRESHOLD ? 'var(--present-fg)' : 'var(--absent-fg)' }}>
                    {rate}%
                  </span>
                </div>
                <div style={{ display: 'flex', height: 11, borderRadius: 999, overflow: 'hidden',
                              background: 'var(--border-soft)' }}>
                  {[['PRESENT', '#1f6b4a'], ['LATE', '#b4552d'],
                    ['EXCUSED', '#3b4e8c'], ['ABSENT', '#8e2f2a']].map(([status, colour]) => (
                    <span key={status} style={{
                      width: `${(tally[status] / rows.length) * 100}%`, background: colour,
                    }} />
                  ))}
                </div>
                <span className="stat-note">
                  {tally.PRESENT} present · {tally.LATE} late · {tally.EXCUSED} excused · {tally.ABSENT} absent
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <h2 style={{ fontSize: 18, marginBottom: 6 }}>Below {THRESHOLD}%</h2>
        <p className="stat-note" style={{ marginBottom: 12 }}>
          A student under {THRESHOLD}% is at risk of losing exam eligibility.
        </p>
        {report.atRisk.length === 0 ? (
          <p className="empty">Every student with records is at or above {THRESHOLD}%.</p>
        ) : (
          <table className="table">
            <thead>
              <tr><th>Student</th><th>Reg no.</th><th>Sessions</th><th>Missed</th><th>Rate</th></tr>
            </thead>
            <tbody>
              {report.atRisk.map(({ student, rows, rate }) => (
                <tr key={student.id}>
                  <td data-label="Student">{student.firstName} {student.lastName}</td>
                  <td data-label="Reg no." className="num">{student.regNumber}</td>
                  <td data-label="Sessions" className="num">{rows.length}</td>
                  <td data-label="Missed" className="num">
                    {rows.filter((a) => a.status === 'ABSENT').length}
                  </td>
                  <td data-label="Rate">
                    <span className="pill ABSENT">{rate}%</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </Shell>
  )
}

function Legend() {
  return (
    <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
      {[['Present', '#1f6b4a'], ['Late', '#b4552d'],
        ['Excused', '#3b4e8c'], ['Absent', '#8e2f2a']].map(([label, colour]) => (
        <span key={label} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
          <span style={{ width: 10, height: 10, borderRadius: 3, background: colour }} />
          {label}
        </span>
      ))}
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

function Shell({ onExport, children }) {
  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Reports</h1>
          <p>Attendance rate counts present, late and excused against every record.</p>
        </div>
        {onExport && <button className="btn" onClick={onExport}>Export CSV</button>}
      </div>
      <div className="content">{children}</div>
    </>
  )
}
