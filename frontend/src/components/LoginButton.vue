<script setup lang="ts">
import { authService } from '@/services/authService'

const login = () => {
  authService.loginWithGoogle()
}
</script>

<template>
  import { authService } from '@/services/authService'

  <button @click="login">Login with Google</button>
</template>

<style scoped>

</style>
