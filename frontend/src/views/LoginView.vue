<!-- LoginView.vue -->
<template>
  <div class="login-container">
    <div class="login-card">
      <h2>Welcome</h2>
      <p class="subtitle">Please sign in to continue</p>

      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <button
        @click="handleGoogleLogin"
        class="google-login-button"
        :disabled="loading"
      >
        <img
          src="@/assets/google-icon.svg"
          alt="Google"
          class="google-icon"
        >
        {{ loading ? 'Loading...' : 'Sign in with Google' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { authService } from '@/services/authService'

const loading = ref(false)
const error = ref('')

const handleGoogleLogin = () => {
  try {
    loading.value = true
    authService.loginWithGoogle()
  } catch (err) {
    error.value = 'Failed to initialize Google login'
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background-color: #f5f5f5;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  background-color: white;
  text-align: center;
}

h2 {
  margin-bottom: 10px;
  color: #333;
}

.subtitle {
  color: #666;
  margin-bottom: 25px;
}

.google-login-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 12px;
  background-color: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s;
}

.google-login-button:hover {
  background-color: #f8f8f8;
}

.google-login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.google-icon {
  width: 20px;
  height: 20px;
  margin-right: 10px;
}

.error-message {
  color: #dc3545;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #fce8e8;
  border-radius: 4px;
}
</style>
