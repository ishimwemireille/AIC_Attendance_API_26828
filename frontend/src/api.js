// One place that talks to the Spring Boot API.
//
// In development the API accepts HTTP Basic (the "dev" profile). In production
// the browser is redirected to Google and carries a session cookie instead, so
// this module sends credentials either way.

const KEY = 'aic.auth'

export function saveAuth(email, password) {
  sessionStorage.setItem(KEY, btoa(`${email}:${password}`))
}

export function clearAuth() {
  sessionStorage.removeItem(KEY)
}

export function hasAuth() {
  return Boolean(sessionStorage.getItem(KEY))
}

async function request(path, options = {}) {
  const token = sessionStorage.getItem(KEY)
  const response = await fetch(`/api${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Basic ${token}` } : {}),
      ...(options.headers || {}),
    },
  })

  if (response.status === 401) {
    clearAuth()
    throw new ApiError('Your session has ended. Sign in again.', 401)
  }
  if (response.status === 204) return null

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiError(messageFrom(body, response.status), response.status, body)
  }
  return body
}

function messageFrom(body, status) {
  if (!body) return `The request failed (${status}).`
  if (body.message) return body.message
  if (body.fieldErrors) {
    return Object.entries(body.fieldErrors)
      .map(([field, message]) => `${field}: ${message}`)
      .join('\n')
  }
  return `The request failed (${status}).`
}

export async function importRegister(courseId, file) {
  const token = sessionStorage.getItem(KEY)
  const body = new FormData()
  body.append('file', file)
  // No Content-Type here: the browser must set it so the multipart boundary is right.
  const response = await fetch(`/api/courses/${courseId}/import`, {
    method: 'POST',
    credentials: 'include',
    headers: token ? { Authorization: `Basic ${token}` } : {},
    body,
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiError(messageFrom(payload, response.status), response.status, payload)
  }
  return payload
}

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message)
    this.status = status
    this.body = body
  }
}

export const api = {
  me: () => request('/users/me'),
  students: () => request('/students'),
  createStudent: (student) => request('/students', { method: 'POST', body: JSON.stringify(student) }),
  updateStudent: (id, student) => request(`/students/${id}`, { method: 'PUT', body: JSON.stringify(student) }),
  deleteStudent: (id) => request(`/students/${id}`, { method: 'DELETE' }),
  courses: () => request('/courses'),
  myCourses: () => request('/courses/mine'),
  createCourse: (course) => request('/courses', { method: 'POST', body: JSON.stringify(course) }),
  updateCourse: (id, course) => request(`/courses/${id}`, { method: 'PUT', body: JSON.stringify(course) }),
  deleteCourse: (id) => request(`/courses/${id}`, { method: 'DELETE' }),
  courseProgress: (id) => request(`/courses/${id}/progress`),
  courseActivities: (id) => request(`/courses/${id}/activities`),
  logActivity: (id, entry) => request(`/courses/${id}/activities`, { method: 'POST', body: JSON.stringify(entry) }),
  sessions: () => request('/sessions'),
  createSession: (session) => request('/sessions', { method: 'POST', body: JSON.stringify(session) }),
  updateSession: (id, session) => request(`/sessions/${id}`, { method: 'PUT', body: JSON.stringify(session) }),
  deleteSession: (id) => request(`/sessions/${id}`, { method: 'DELETE' }),
  users: () => request('/users'),
  trainers: () => request('/users/trainers'),
  createUser: (user) => request('/users', { method: 'POST', body: JSON.stringify(user) }),
  setUserActive: (id, value) => request(`/users/${id}/active?value=${value}`, { method: 'PUT' }),
  setUserRoles: (id, roles) => request(`/users/${id}/roles`, { method: 'PUT', body: JSON.stringify(roles) }),
  attendances: () => request('/attendances'),
  attendanceBySession: (id) => request(`/attendances/session/${id}`),
  mark: (record) => request('/attendances', { method: 'POST', body: JSON.stringify(record) }),
  notifications: () => request('/notifications'),
  audit: (limit = 50) => request(`/audit?limit=${limit}`),
  systemStores: () => request('/system/stores'),
  systemOutbox: () => request('/system/outbox'),
  retryOutbox: (id) => request(`/system/outbox/${id}/retry`, { method: 'POST' }),
}
