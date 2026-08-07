<template>
  <div id="neopick-app">
    <header class="app-header">
      <router-link to="/" class="logo">NEOPick</router-link>
      <nav>
        <router-link to="/login">Login</router-link>
      </nav>
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
// App shell — router-view handles the rest
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
.app-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; border-bottom: 1px solid #eee;
}
.logo { font-size: 1.5rem; font-weight: 700; color: #333; text-decoration: none; }
</style>
