import api from '@/api/axios'

export interface AppUserDetails {
  id: string
  email: string
  role: string
  authorized: boolean
}

export interface OAuth2Details {
  email: string
  [key: string]: any
}

export interface AuthStatus {
  status?: string
  oauth2_details?: OAuth2Details
  user_exists?: boolean
  app_user_details?: AppUserDetails
}

export const authService = {
  async checkAuthStatus(): Promise<AuthStatus> {
    try {
      const response = await api.get<AuthStatus>('/auth-status')
      return response.data
    } catch (error) {
      return {
        status: 'Not authenticated',
        user_exists: false
      }
    }
  },

  loginWithGoogle(): void {
    window.location.href = '/oauth2/authorization/google'
  },

  async logout(): Promise<void> {
    await api.post('/logout')
    window.location.href = '/'
  }
}
