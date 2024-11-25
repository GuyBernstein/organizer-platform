import axios from 'axios'

const api = axios.create({
  baseURL: '/api', // This will use the proxy configured in vite.config.js
  withCredentials: true, // Required for OAuth2 session cookies
  headers: {
    'Content-Type': 'application/json'
  }
})

// response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Redirect to login page
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
