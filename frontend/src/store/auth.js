import { ref } from 'vue'

// 登录态:token + 角色 + 用户 id,统一存 localStorage,刷新页面不丢。
const token = ref(localStorage.getItem('token') || '')
const role = ref(localStorage.getItem('role') || '')
const userId = ref(localStorage.getItem('userId') || '')

// 解码 JWT 的中间段(payload),拿到 role / userId 等声明。
export function decodeToken(t) {
  try {
    const payload = t.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch (e) {
    return null
  }
}

// 登录成功后:存 token,并从 token 里解出角色与用户 id。
export function setAuth(t) {
  token.value = t
  localStorage.setItem('token', t)
  const claims = decodeToken(t)
  role.value = claims?.role || ''
  userId.value = claims?.userId != null ? String(claims.userId) : ''
  localStorage.setItem('role', role.value)
  localStorage.setItem('userId', userId.value)
}

export function getToken() {
  return token.value || localStorage.getItem('token') || ''
}

export function getRole() {
  return role.value || localStorage.getItem('role') || ''
}

export function getUserId() {
  return userId.value || localStorage.getItem('userId') || ''
}

export function clearAuth() {
  token.value = ''
  role.value = ''
  userId.value = ''
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
}
