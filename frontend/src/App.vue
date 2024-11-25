<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import HelloWorld from './components/HelloWorld.vue'
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

onMounted(async () => {
  await authStore.checkAuth()
})
</script>

<template>
  <div class="app-container">
    <header>
      <img alt="Vue logo" class="logo" src="@/assets/logo.svg" width="125" height="125" />

      <div class="wrapper">
        <HelloWorld msg="You did it!" />

        <nav>
          <RouterLink to="/">Home</RouterLink>
          <RouterLink to="/about">About</RouterLink>
          <RouterLink to="/login">Login</RouterLink>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}

header {
  padding: 1rem;
  background-color: var(--color-background);
  width: 100%;
}

.logo {
  display: block;
  margin: 0 auto 1rem;
  max-width: 125px;
  height: auto;
}

.wrapper {
  width: 100%;
}

nav {
  width: 100%;
  font-size: 1rem;
  text-align: center;
  margin-top: 1rem;
  padding: 0.5rem 0;
}

nav a {
  display: inline-block;
  padding: 0.5rem 1rem;
  text-decoration: none;
  color: var(--color-text);
}

nav a.router-link-exact-active {
  color: var(--color-text);
  font-weight: bold;
}

nav a.router-link-exact-active:hover {
  background-color: transparent;
}

.main-content {
  flex: 1;
  width: 100%;
  padding: 1rem;
}

@media (min-width: 768px) {
  header {
    padding: 2rem;
  }

  .logo {
    margin: 0 2rem 0 0;
  }

  nav {
    text-align: center;
    margin-top: 2rem;
  }

  nav a {
    padding: 0 1.5rem;
    border-left: 1px solid var(--color-border);
  }

  nav a:first-of-type {
    border: 0;
  }
}

@media (min-width: 1024px) {
  header {
    display: flex;
    align-items: center;
    padding-right: var(--section-gap);
  }

  .wrapper {
    display: flex;
    place-items: flex-start;
    flex-wrap: wrap;
  }

  nav {
    text-align: left;
    margin-left: -1rem;
  }
}
</style>
