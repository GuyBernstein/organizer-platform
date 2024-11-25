<!-- DashboardView.vue -->
<template>
  <div class="dashboard-container">
    <header class="dashboard-header">
      <h1>Dashboard</h1>
      <div class="user-info">
        <span v-if="userDetails" class="email">{{ userDetails.email }}</span>
        <button @click="handleLogout" :disabled="loading">Logout</button>
      </div>
    </header>

    <div class="dashboard-content">
      <div v-if="loading" class="loading">
        Loading...
      </div>

      <div v-else-if="error" class="error-message">
        {{ error }}
      </div>

      <div v-else-if="userDetails" class="dashboard-stats">
        <div class="stat-card">
          <h3>User Details</h3>
          <div class="detail-item">
            <strong>ID:</strong> {{ userDetails.id }}
          </div>
          <div class="detail-item">
            <strong>Email:</strong> {{ userDetails.email }}
          </div>
          <div class="detail-item">
            <strong>Role:</strong> {{ userDetails.role }}
          </div>
          <div class="detail-item">
            <strong>Status:</strong>
            <span :class="['status', userDetails.authorized ? 'authorized' : 'unauthorized']">
              {{ userDetails.authorized ? 'Authorized' : 'Unauthorized' }}
            </span>
          </div>
        </div>

        <!-- Add additional dashboard cards here based on your needs -->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authService, type AppUserDetails } from '@/services/authService'

const router = useRouter()
const userDetails = ref<AppUserDetails | null>(null)
const loading = ref(true)
const error = ref('')

const handleLogout = async () => {
  try {
    await authService.logout()
  } catch (err) {
    error.value = 'Error logging out'
  }
}

onMounted(async () => {
  try {
    const authStatus = await authService.checkAuthStatus()

    if (!authStatus.app_user_details) {
      router.push('/login')
      return
    }

    userDetails.value = authStatus.app_user_details
  } catch (err) {
    error.value = 'Error loading user data'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.email {
  color: #666;
}

.dashboard-content {
  margin-top: 20px;
}

.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.stat-card {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.detail-item {
  margin: 10px 0;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.detail-item:last-child {
  border-bottom: none;
}

.status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.9em;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error-message {
  color: #dc3545;
  text-align: center;
  padding: 20px;
  background-color: #fce8e8;
  border-radius: 4px;
}

button {
  padding: 8px 16px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

button:hover {
  background-color: #c82333;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
