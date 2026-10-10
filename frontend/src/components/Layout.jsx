import { useState } from 'react'
import { NavLink } from 'react-router-dom'

const ITEMS = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/mark', label: 'Mark attendance', writers: true },
  { to: '/students', label: 'Students' },
  { to: '/sessions', label: 'Sessions' },
  { to: '/reports', label: 'Reports' },
  { to: '/notifications', label: 'Notifications', writers: true },
  { to: '/users', label: 'Access', admins: true },
]

export default function Layout({ user, onSignOut, children }) {
  const [open, setOpen] = useState(false)
  const canWrite = user.roles.includes('ADMIN') || user.roles.includes('COHORT_LEADER')
  const isAdmin = user.roles.includes('ADMIN')
  const initials = user.fullName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()

  return (
    <div className="shell">
      {open && <button className="scrim" aria-label="Close the menu" onClick={() => setOpen(false)} />}

      <aside className={open ? 'sidebar open' : 'sidebar'}>
        <div className="brand">
          <span className="brand-mark">A</span>
          <span>
            <span className="brand-name">AIC Attendance</span><br />
            <span className="brand-sub">AUCA · Kigali</span>
          </span>
        </div>

        <nav className="nav">
          {ITEMS.filter((item) => (!item.writers || canWrite) && (!item.admins || isAdmin)).map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} onClick={() => setOpen(false)}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="who">
          <div className="who-card">
            <span className="avatar">{initials}</span>
            <span>
              <span className="who-name">{user.fullName}</span><br />
              <span className="who-role">{user.roles.join(', ').replaceAll('_', ' ').toLowerCase()}</span>
            </span>
          </div>
          <button className="signout" onClick={onSignOut}>Sign out</button>
        </div>
      </aside>

      <div className="main">
        <header className="mobile-bar">
          <button className="hamburger" aria-label="Open the menu" onClick={() => setOpen(true)}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="M4 7h16" /><path d="M4 12h16" /><path d="M4 17h16" />
            </svg>
          </button>
          <span className="mobile-title">AIC Attendance</span>
        </header>
        {children}
      </div>
    </div>
  )
}
