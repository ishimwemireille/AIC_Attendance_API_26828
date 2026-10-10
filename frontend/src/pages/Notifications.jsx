import { useEffect, useState } from 'react'
import { api } from '../api'

export default function Notifications() {
  const [logs, setLogs] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.notifications().then(setLogs).catch((p) => setError(p.message))
  }, [])

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>Notifications</h1>
          <p>Every alert the system tried to send. Stored in MongoDB, produced through RabbitMQ.</p>
        </div>
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}
        {!logs ? <div className="empty">Loading…</div>
          : logs.length === 0
            ? <div className="card"><p className="empty">
                Nothing sent yet. Mark a student absent and an alert appears here.</p></div>
            : (
              <div className="card">
                <table className="table">
                  <thead>
                    <tr><th>Channel</th><th>To</th><th>Message</th><th>Status</th><th>Sent</th></tr>
                  </thead>
                  <tbody>
                    {logs.map((n) => (
                      <tr key={n.id}>
                        <td data-label="Channel"><span className="pill muted">{n.channel}</span></td>
                        <td data-label="To" className="num">{n.recipient.email || n.recipient.phone}</td>
                        <td data-label="Message">{n.payload.subject || n.payload.text}</td>
                        <td data-label="Status">
                          <span className={n.status === 'SENT' ? 'pill PRESENT' : 'pill ABSENT'}>
                            {n.status}
                          </span>
                        </td>
                        <td data-label="Sent" className="num">{String(n.createdAt).slice(0, 16).replace('T', ' ')}</td>
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
