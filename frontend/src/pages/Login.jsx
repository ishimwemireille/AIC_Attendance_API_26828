import { useState } from 'react'
import { api, saveAuth, clearAuth } from '../api'

// What the API sends back when Google vouched for someone the system will
// not let in. Google accepted the password; the refusal is ours, so saying
// "invalid credentials" would be a lie.
const REFUSALS = {
  invalid_domain: 'That Google account is not an AUCA address. Sign in with your @auca.ac.rw account.',
  no_access: 'That account has not been granted access yet. Ask an administrator to add it on the Access screen.',
  inactive: 'That account has been deactivated. Ask an administrator to reactivate it.',
  failed: 'Signing in with Google did not complete. Try again.',
}

function refusalFromUrl() {
  const code = new URLSearchParams(window.location.search).get('error')
  if (!code) return null
  // Keep the address bar clean once the message has been read off it.
  window.history.replaceState({}, '', window.location.pathname)
  return REFUSALS[code] || REFUSALS.failed
}

export default function Login({ onSignedIn }) {
  const [email, setEmail] = useState('cohort.leader@auca.ac.rw')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(refusalFromUrl)
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    saveAuth(email, password)
    try {
      onSignedIn(await api.me())
    } catch (problem) {
      clearAuth()
      setError(problem.status === 401
        ? 'That email and password were not accepted.'
        : problem.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <div className="login-side">
        <div className="brand">
          <span className="brand-mark">A</span>
          <span>
            <span className="brand-name">AIC Attendance</span><br />
            <span className="brand-sub">AUCA · Kigali</span>
          </span>
        </div>
        <h2>Attendance,<br />without the<br />spreadsheet.</h2>
        <p>One reliable place to plan sessions, take attendance and spot students who need support.</p>
      </div>

      <div className="login-form">
        <form className="login-form-inner" onSubmit={submit}>
          <div>
            <p className="eyebrow" style={{ margin: '0 0 8px' }}>Staff access</p>
            <h1 style={{ fontSize: 32 }}>Sign in</h1>
          </div>

          {error && <div className="banner error">{error}</div>}

          <a className="btn ghost" href="/oauth2/authorization/google" style={{ width: '100%' }}>
            Continue with your AUCA Google account
          </a>
          <p className="hint" style={{ textAlign: 'center', margin: 0 }}>
            The usual way in. Needs the <code>google</code> profile and Google credentials configured.
          </p>

          <hr style={{ border: 'none', borderTop: '1px solid #e6e1d6', margin: '4px 0' }} />

          <div className="field">
            <label htmlFor="email">Staff email</label>
            <input id="email" type="email" value={email} required
                   onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" type="password" value={password} required
                   onChange={(e) => setPassword(e.target.value)} />
          </div>
          <button className="btn" type="submit" disabled={busy}>
            {busy ? 'Checking…' : 'Sign in'}
          </button>

        </form>
      </div>
    </div>
  )
}
