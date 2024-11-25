// src/stores/auth.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authService } from '@/services/authService'

interface AppUserDetails {
  id: string
  email: string
  role: string
  authorized: boolean
}

interface OAuth2Details {
  email: string
  // Add other OAuth2 attributes as needed
  [key: string]: any  // For other dynamic OAuth2 attributes
}

interface AuthStatus {
  status?: string
  oauth2_details?: OAuth2Details
  user_exists?: boolean
  app_user_details?: AppUserDetails
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AppUserDetails | null>(null)
  const isAuthenticated = ref<boolean>(false)

  async function checkAuth(): Promise<void> {
    try {
      const response: AuthStatus = await authService.checkAuthStatus()

      // User is authenticated if we have app_user_details and user_exists is true
      const authenticated = !!response.app_user_details && response.user_exists === true
      isAuthenticated.value = authenticated

      if (authenticated && response.app_user_details) {
        user.value = response.app_user_details
      } else {
        user.value = null
      }
    } catch (error) {
      isAuthenticated.value = false
      user.value = null
    }
  }

  return {
    user,
    isAuthenticated,
    checkAuth
  }
})

export type AuthStore = ReturnType<typeof useAuthStore>
