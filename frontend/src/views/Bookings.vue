<template>
  <main class="bookings-page">
    <header><p>YOUR PRACTICE, IN MOTION</p><h1>My lessons</h1><span>Upcoming requests and past sessions, all in one place.</span></header>
    <div v-if="loading" class="state">Loading your lessons…</div>
    <section v-else-if="bookings.length" class="lesson-list">
      <article v-for="booking in bookings" :key="booking.id">
        <div class="date"><strong>{{ day(booking.scheduledStart) }}</strong><span>{{ month(booking.scheduledStart) }}</span></div>
        <div class="lesson"><span class="status" :class="booking.status">{{ statusLabel(booking.status) }}</span><h2>Lesson with teacher #{{ booking.teacherId }}</h2><p>{{ formatTime(booking.scheduledStart) }} · {{ booking.durationMinutes }} min · {{ booking.address?.label || 'Location to be agreed' }}</p></div>
        <div class="amount"><strong>¥{{ booking.price }}</strong><router-link :to="`/teachers/${booking.teacherId}`">View teacher →</router-link></div>
      </article>
    </section>
    <section v-else class="state empty"><span>♪</span><h2>Your first lesson starts with a good match.</h2><p>Browse by style, location, and a time that actually fits.</p><router-link to="/">Find a teacher →</router-link></section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/client.js'
const bookings = ref([]); const loading = ref(true)
function normalize(raw) { return { ...raw, teacherId: raw.teacherId ?? raw.teacher_id, scheduledStart: raw.scheduledStart ?? raw.scheduled_start, durationMinutes: raw.durationMinutes ?? raw.duration_minutes, studentNote: raw.studentNote ?? raw.student_note } }
function asDate(value) { return value ? new Date(value) : new Date() }
function day(value) { return String(asDate(value).getDate()).padStart(2, '0') }
function month(value) { return asDate(value).toLocaleDateString('en', { month: 'short' }).toUpperCase() }
function formatTime(value) { return asDate(value).toLocaleString('en', { weekday: 'short', hour: '2-digit', minute: '2-digit' }) }
function statusLabel(status) { return ({ PENDING: 'Awaiting teacher', CONFIRMED: 'Confirmed', PAID: 'Paid', COMPLETED: 'Completed', CANCELLED: 'Cancelled', REJECTED: 'Unavailable' })[status] || status }
onMounted(async () => { try { const { data } = await api.get('/bookings'); bookings.value = (data?.content || (Array.isArray(data) ? data : [])).map(normalize) } catch { bookings.value = [] } finally { loading.value = false } })
</script>

<style scoped>
.bookings-page{padding:80px max(24px,calc((100% - 1000px)/2)) 110px}.bookings-page>header{margin-bottom:55px}.bookings-page header>p{color:var(--wood);font:500 .65rem 'IBM Plex Mono';letter-spacing:.15em}.bookings-page h1{font:500 clamp(3.5rem,7vw,6rem)/1 'Fraunces';letter-spacing:-.055em}.bookings-page header>span{display:block;margin-top:13px;color:#707676}.lesson-list{border-top:1px solid var(--line)}.lesson-list article{display:grid;grid-template-columns:80px 1fr auto;gap:28px;align-items:center;padding:28px 0;border-bottom:1px solid var(--line)}.date{display:grid;place-items:center;padding:10px;border:1px solid var(--ink)}.date strong{font:500 1.5rem 'IBM Plex Mono'}.date span{font:.55rem 'IBM Plex Mono';letter-spacing:.12em}.lesson h2{margin:7px 0 5px;font:500 1.4rem 'Fraunces'}.lesson p{color:#6d7373;font-size:.7rem}.status{padding:4px 7px;background:#e4ddcf;color:#646969;font:500 .55rem 'IBM Plex Mono';text-transform:uppercase}.status.CONFIRMED,.status.PAID{background:#d7e6dc;color:#2c6048}.status.CANCELLED,.status.REJECTED{background:#eed8d1;color:#7b3e31}.amount{text-align:right}.amount strong{display:block;font:500 1rem 'IBM Plex Mono'}.amount a{display:block;margin-top:9px;color:var(--green);font-size:.65rem;text-decoration:none}.state{min-height:360px;display:grid;place-content:center;text-align:center;color:#707676}.empty span{color:var(--wood);font-size:2.2rem}.empty h2{max-width:500px;margin:12px auto;font:500 2rem 'Fraunces';color:var(--ink)}.empty a{width:max-content;margin:22px auto 0;padding:12px 18px;background:var(--ink);color:#fff;text-decoration:none;font-size:.72rem}@media(max-width:600px){.bookings-page{padding:55px 18px 80px}.lesson-list article{grid-template-columns:60px 1fr}.amount{grid-column:2;text-align:left;display:flex;gap:15px;align-items:center}.amount a{margin:0}}
</style>
