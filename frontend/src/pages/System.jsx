import { useEffect, useState } from 'react'
import { api } from '../api'

/**
 * The administrator's technical view. Everyone else works with students and
 * attendance; this is the only screen that shows the machinery underneath.
 */
export default function System() {
  const [stores, setStores] = useState(null)
  const [outbox, setOutbox] = useState(null)
  const [audit, setAudit] = useState(null)
  const [error, setError] = useState(null)
  const [checkedAt, setCheckedAt] = useState(null)

  function load() {
    setError(null)
    Promise.all([api.systemStores(), api.systemOutbox(), api.audit(25)])
      .then(([s, o, a]) => { setStores(s); setOutbox(o); setAudit(a) })
      .catch((problem) => setError(problem.message))
      .finally(() => setCheckedAt(new Date()))
  }

  useEffect(() => {
    load()
    // The point of this screen is that it is live: stopping a container should
    // show up here without anyone reaching for the keyboard.
    const timer = setInterval(load, 5000)
    return () => clearInterval(timer)
  }, [])

  async function retry(id) {
    try { await api.retryOutbox(id); load() } catch (problem) { setError(problem.message) }
  }

  return (
    <>
      <div className="topbar">
        <div className="grow">
          <h1>System</h1>
          <p>
            The state of the running system. Refreshes every five seconds
            {checkedAt && ` · last checked ${checkedAt.toLocaleTimeString()}`}.
          </p>
        </div>
        <button className="btn ghost" onClick={load}>Refresh now</button>
      </div>

      <div className="content">
        {error && <div className="banner error">{error}</div>}

        <section>
          <h2 style={{ fontSize: 16, margin: '0 0 12px' }}>Stores</h2>
          <div className="stat-row" style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))' }}>
            {(stores || []).map((store) => (
              <div className="card" key={store.name}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <strong style={{ fontSize: 15 }}>{store.name}</strong>
                  <span className={store.up ? 'pill PRESENT' : 'pill ABSENT'}>
                    {store.up ? 'Up' : 'Down'}
                  </span>
                </div>
                <p className="stat-note" style={{ margin: '6px 0 10px' }}>{store.holds}</p>
                <p className="stat-note" style={{ marginBottom: 10 }}>{store.detail}</p>
                <table className="table">
                  <tbody>
                    {Object.entries(store.counts || {}).map(([key, value]) => (
                      <tr key={key}>
                        <td>{key}</td>
                        <td className="num"><strong>{String(value)}</strong></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
            {!stores && <p className="empty">Checking…</p>}
          </div>
        </section>

        <section>
          <h2 style={{ fontSize: 16, margin: '0 0 12px' }}>Outbox</h2>
          <div className="card">
            <p className="stat-note" style={{ marginBottom: 14 }}>
              An absence alert is written here in the same transaction as the attendance
              record, then handed to RabbitMQ. If the broker is down the alert waits
              rather than being lost.
            </p>
            <div className="stat-row" style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', marginBottom: 16 }}>
              {Object.entries(outbox?.counts || {}).map(([status, count]) => (
                <div key={status}>
                  <div className="stat-value">{count}</div>
                  <div className="stat-label">{status}</div>
                </div>
              ))}
            </div>

            {!outbox ? <p className="empty">Loading…</p>
              : outbox.recent.length === 0 ? <p className="empty">Nothing has been queued yet.</p> : (
              <table className="table">
                <thead>
                  <tr><th>Created</th><th>Routing key</th><th>Status</th>
                      <th className="num">Attempts</th><th>Last error</th><th></th></tr>
                </thead>
                <tbody>
                  {outbox.recent.map((event) => (
                    <tr key={event.id}>
                      <td data-label="Created">{event.createdOn?.replace('T', ' ').slice(0, 19)}</td>
                      <td data-label="Routing key"><code>{event.routingKey}</code></td>
                      <td data-label="Status">
                        <span className={event.status === 'SENT' ? 'pill PRESENT'
                          : event.status === 'FAILED' ? 'pill ABSENT' : 'pill LATE'}>
                          {event.status.toLowerCase()}
                        </span>
                      </td>
                      <td data-label="Attempts" className="num">{event.attempts}</td>
                      <td data-label="Last error" style={{ maxWidth: 260 }}>
                        <span className="stat-note">{event.lastError || '—'}</span>
                      </td>
                      <td data-label="Actions">
                        {event.status !== 'SENT' && (
                          <button className="btn ghost" style={{ minHeight: 34, padding: '0 12px' }}
                                  onClick={() => retry(event.id)}>
                            Send again
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </section>

        <section>
          <h2 style={{ fontSize: 16, margin: '0 0 12px' }}>Audit trail</h2>
          <div className="card">
            <p className="stat-note" style={{ marginBottom: 14 }}>
              Every change to a student, course, session or account, with who made it.
              Kept in MongoDB because the shape differs per record.
            </p>
            {!audit ? <p className="empty">Loading…</p>
              : audit.length === 0 ? <p className="empty">No changes recorded yet.</p> : (
              <table className="table">
                <thead>
                  <tr><th>When</th><th>Entity</th><th>Action</th><th>Who</th></tr>
                </thead>
                <tbody>
                  {audit.map((entry) => (
                    <tr key={entry.id}>
                      <td data-label="When">{entry.at?.replace('T', ' ').slice(0, 19)}</td>
                      <td data-label="Entity"><strong>{entry.entity}</strong></td>
                      <td data-label="Action">
                        <span className="pill muted">{entry.action.toLowerCase()}</span>
                      </td>
                      <td data-label="Who">{entry.actor?.email || 'system'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </section>
      </div>
    </>
  )
}
