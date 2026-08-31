<template>
  <div class="login">
    <h2>Login</h2>
    <p class="hint">Enter your phone number, we'll send a code.</p>

    <template v-if="!codeSent">
      <input v-model="phone" placeholder="Phone number" type="tel" />
      <button @click="sendCode" :disabled="sending">{{ sending ? 'Sending...' : 'Send Code' }}</button>
    </template>

    <template v-else>
      <p class="hint">Code sent to {{ phone }}</p>
      <input v-model="code" placeholder="6-digit code" maxlength="6" />
      <button @click="doLogin" :disabled="loggingIn">{{ loggingIn ? 'Logging in...' : 'Login' }}</button>
      <button class="link" @click="codeSent = false">Change number</button>
    </template>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const phone = ref('')
const code = ref('')
const codeSent = ref(false)
const sending = ref(false)
const loggingIn = ref(false)
const error = ref('')

async function sendCode() {
  error.value = ''
  sending.value = true
  try {
    await auth.sendSms(phone.value)
    codeSent.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to send code'
  } finally {
    sending.value = false
  }
}

async function doLogin() {
  error.value = ''
  loggingIn.value = true
  try {
    await auth.login(phone.value, code.value)
    router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (e) {
    error.value = e.response?.data?.message || 'Invalid code'
  } finally {
    loggingIn.value = false
  }
}
</script>

<style scoped>
.login { max-width: 360px; margin: 4rem auto; text-align: center; }
h2 { margin-bottom: 0.5rem; }
.hint { color: #888; margin-bottom: 1.5rem; font-size: 0.95rem; }
input { width: 100%; padding: 0.75rem; margin-bottom: 0.75rem; border: 1px solid #ddd; border-radius: 4px; font-size: 1rem; }
button { width: 100%; padding: 0.75rem; background: #333; color: #fff; border: none; border-radius: 4px; font-size: 1rem; cursor: pointer; }
button:disabled { opacity: 0.6; }
.link { background: none; color: #666; margin-top: 0.5rem; }
.error { color: #e74c3c; margin-top: 1rem; }
</style>
