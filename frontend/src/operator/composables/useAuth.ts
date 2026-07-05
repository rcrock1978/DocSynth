import { ref, computed } from 'vue'

export interface AuthenticatedUser {
  email: string
  sub: string
  tenantId: string
  roles: Record<string, 'owner' | 'editor' | 'viewer'>
}

const user = ref<AuthenticatedUser | null>(null)

export function useAuth() {
  const isAuthenticated = computed(() => user.value !== null)

  function signIn() {
    // OIDC PKCE flow. v1 uses auth code + PKCE; the IdP issuer URL is
    // configured at build time via VITE_OIDC_ISSUER.
    const issuer = import.meta.env.VITE_OIDC_ISSUER as string
    const clientId = import.meta.env.VITE_OIDC_CLIENT_ID as string
    const redirectUri = `${window.location.origin}/auth/callback`
    const state = crypto.randomUUID()
    sessionStorage.setItem('oidc_state', state)
    const url = new URL(`${issuer}/authorize`)
    url.searchParams.set('response_type', 'code')
    url.searchParams.set('client_id', clientId)
    url.searchParams.set('redirect_uri', redirectUri)
    url.searchParams.set('scope', 'openid email profile')
    url.searchParams.set('state', state)
    window.location.href = url.toString()
  }

  function signOut() {
    user.value = null
    localStorage.removeItem('access_token')
  }

  return { user, isAuthenticated, signIn, signOut }
}
