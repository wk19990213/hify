import { ref, computed } from 'vue'
import { post } from '@/utils/request'

const TOKEN_KEY = 'hify_token'
const USER_KEY = 'hify_user'

interface UserInfo {
  userId: number
  username: string
}

const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
const user = ref<UserInfo | null>(loadUser())

function loadUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function saveAuth(t: string, u: UserInfo) {
  token.value = t
  user.value = u
  localStorage.setItem(TOKEN_KEY, t)
  localStorage.setItem(USER_KEY, JSON.stringify(u))
}

function clearAuth() {
  token.value = null
  user.value = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  const authHeaders = computed(() => {
    if (!token.value) return {}
    return { Authorization: `Bearer ${token.value}` }
  })

  async function login(username: string, password: string) {
    const res: any = await post('/v1/auth/login', { username, password })
    saveAuth(res.token, { userId: res.userId, username: res.username })
  }

  async function register(username: string, password: string, displayName?: string) {
    const res: any = await post('/v1/auth/register', { username, password, displayName })
    saveAuth(res.token, { userId: res.userId, username: res.username })
  }

  function logout() {
    clearAuth()
    window.location.href = '/'
  }

  return { token, user, isLoggedIn, authHeaders, login, register, logout }
}
