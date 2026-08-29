<template>
  <div id="neopick-app">
    <header class="app-header">
      <router-link to="/" class="logo">NEOPick</router-link>
      <nav>
        <template v-if="auth.isLoggedIn">
          <router-link to="/bookings">My Bookings</router-link>
          <router-link to="/chat">Chat</router-link>
          <a href="#" @click.prevent="auth.logout()">Logout</a>
        </template>
        <router-link v-else to="/login">Login</router-link>
      </nav>
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/auth.js'
const auth = useAuthStore()
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #222; }
.app-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; border-bottom: 1px solid #eee; background: #fff;
}
.logo { font-size: 1.4rem; font-weight: 700; color: #333; text-decoration: none; }
nav a { margin-left: 1.5rem; color: #555; text-decoration: none; }
nav a:hover { color: #000; }
main { max-width: 960px; margin: 0 auto; padding: 2rem 1rem; }
</style>
