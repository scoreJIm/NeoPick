<template>
  <div id="neopick-app">
    <header class="app-header">
      <router-link to="/" class="logo" aria-label="NeoPick home">
        <span class="logo-mark">N</span>
        <span>NEO<strong>PICK</strong></span>
      </router-link>
      <nav aria-label="Main navigation">
        <router-link to="/">Find a teacher</router-link>
        <template v-if="auth.isLoggedIn">
          <router-link to="/bookings">My lessons</router-link>
          <router-link to="/chat">Messages</router-link>
          <button class="nav-link" @click="auth.logout()">Log out</button>
        </template>
        <router-link v-else class="login-link" to="/login">Log in</router-link>
      </nav>
    </header>
    <main>
      <router-view />
    </main>
    <footer class="app-footer">
      <div>
        <span class="logo-footer">NEOPICK</span>
        <p>One-to-one guitar lessons that fit real schedules.</p>
      </div>
      <p>Product preview · Shanghai / Nanjing / Hangzhou</p>
    </footer>
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/auth.js'
const auth = useAuthStore()
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600&family=IBM+Plex+Mono:wght@400;500&family=Manrope:wght@400;500;600;700&display=swap');

:root {
  font-family: 'Manrope', sans-serif;
  color: #17212b;
  background: #f7f2e8;
  font-synthesis: none;
  --ink: #17212b;
  --paper: #f7f2e8;
  --wood: #a85f38;
  --pick: #f2c14e;
  --green: #204e4a;
  --line: rgba(23, 33, 43, .16);
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html { scroll-behavior: smooth; }
body { min-width: 320px; background: var(--paper); }
button, input, select, textarea { font: inherit; }
button, a { -webkit-tap-highlight-color: transparent; }
a { color: inherit; }
:focus-visible { outline: 3px solid var(--pick); outline-offset: 3px; }

.app-header {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 72px;
  padding: 0 max(24px, calc((100% - 1180px) / 2));
  border-bottom: 1px solid var(--line);
  background: rgba(247, 242, 232, .9);
  backdrop-filter: blur(16px);
}

.logo { display: flex; align-items: center; gap: 10px; text-decoration: none; font: 600 .85rem 'IBM Plex Mono'; letter-spacing: .09em; }
.logo strong { color: var(--wood); }
.logo-mark { display: grid; place-items: center; width: 31px; height: 31px; border-radius: 50% 50% 45% 55%; background: var(--pick); color: var(--ink); font-family: 'Fraunces'; transform: rotate(-8deg); }
nav { display: flex; align-items: center; gap: 26px; }
nav a, .nav-link { border: 0; background: none; text-decoration: none; color: #58616a; font-size: .82rem; cursor: pointer; }
nav a:hover, .nav-link:hover, nav .router-link-active { color: var(--ink); }
.login-link { padding: 10px 15px; border: 1px solid var(--ink); color: var(--ink); }
main { min-height: calc(100vh - 72px); }
.app-footer { display: flex; justify-content: space-between; gap: 30px; padding: 45px max(24px, calc((100% - 1180px) / 2)); border-top: 1px solid var(--line); color: #747b80; font-size: .75rem; }
.app-footer div p { margin-top: 5px; }
.logo-footer { color: var(--ink); font: 600 .75rem 'IBM Plex Mono'; letter-spacing: .1em; }

@media (max-width: 720px) {
  .app-header { min-height: 64px; padding: 0 16px; }
  nav { gap: 10px; }
  nav > a:first-child { display: none; }
  nav > a:nth-child(3) { display: none; }
  nav a, .nav-link { font-size: .73rem; }
  .app-footer { flex-direction: column; padding: 36px 18px; }
}

@media (prefers-reduced-motion: reduce) {
  html { scroll-behavior: auto; }
  *, *::before, *::after { transition-duration: .01ms !important; }
}
</style>
