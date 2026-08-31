<template>
  <div v-if="teacher" class="detail">
    <section class="profile-hero" :style="{ '--accent': teacher.accent || '#a85f38' }">
      <button class="back" @click="router.push('/')">← All teachers</button>
      <div class="portrait"><span>{{ initials(teacher.realName) }}</span><b v-if="teacher.isPreview">SAMPLE PROFILE</b></div>
      <div class="profile-copy">
        <p class="eyebrow">{{ teacher.city?.name }} · {{ teacher.district }}</p>
        <h1>{{ teacher.realName }}</h1>
        <p class="bio">{{ teacher.bio }}</p>
        <div class="tags"><span v-for="tag in teacher.tags" :key="tag">{{ tag }}</span></div>
        <div class="proof"><span><strong>★ {{ teacher.rating || 'New' }}</strong>{{ teacher.reviewCount }} reviews</span><span><strong>{{ teacher.bookingCount || '—' }}</strong>lessons taught</span><span><strong>¥{{ teacher.basePrice }}</strong>per lesson</span></div>
      </div>
    </section>

    <main class="booking-layout">
      <div class="lesson-story">
        <p class="eyebrow">START WITH ONE LESSON</p>
        <h2>Pick a time. See if it clicks.</h2>
        <p>Choose a slot like choosing a chord: simple, visible, and easy to change.</p>
        <div class="fretboard" aria-label="Available lesson times">
          <button v-for="slot in slots" :key="slot.value" :class="{ selected: form.time === slot.value }" @click="form.time = slot.value"><small>{{ slot.day }}</small><strong>{{ slot.time }}</strong></button>
        </div>
        <article class="what-to-expect"><span>♪</span><div><h3>Your first session</h3><p>Bring one song you love and one thing you find difficult. The teacher will turn both into a practical next-step plan.</p></div></article>
      </div>

      <aside class="booking-card">
        <p class="eyebrow">LESSON REQUEST</p>
        <div class="price"><strong>¥{{ totalPrice }}</strong><span>{{ form.duration }} minutes</span></div>
        <label><span>Date</span><input v-model="form.date" type="date" :min="minDate" /></label>
        <label><span>Start time</span><input v-model="form.time" type="time" /></label>
        <label><span>Lesson length</span><select v-model.number="form.duration"><option :value="60">60 minutes</option><option :value="90">90 minutes</option></select></label>
        <label><span>Where</span><select v-model="form.addressLabel"><option>Teacher's studio</option><option>My home</option><option>Agree after booking</option></select></label>
        <label v-if="form.addressLabel === 'My home'"><span>Address</span><input v-model="form.addressDetail" placeholder="Street and room number" /></label>
        <label><span>Note (optional)</span><textarea v-model="form.note" rows="3" placeholder="What would you like to learn?"></textarea></label>
        <button class="book" :disabled="submitting" @click="startBooking">{{ submitting ? 'Sending…' : 'Request this lesson →' }}</button>
        <button class="message" @click="startChat">Message teacher</button>
        <p v-if="message" class="notice" :class="{ success }">{{ message }}</p>
        <p class="fine-print">No charge until the teacher confirms.</p>
      </aside>
    </main>
  </div>
  <div v-else-if="error" class="state"><span>♪</span><h2>Teacher not found</h2><button @click="router.push('/')">Browse teachers</button></div>
  <div v-else class="state">Loading the profile…</div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import api from '../api/client.js'
import { findDemoTeacher } from '../data/demoTeachers.js'

const route = useRoute(); const router = useRouter(); const auth = useAuthStore()
const teacher = ref(null); const error = ref(false); const submitting = ref(false); const message = ref(''); const success = ref(false)
const tomorrow = new Date(Date.now() + 86400000)
const minDate = tomorrow.toISOString().slice(0, 10)
const form = reactive({ date: minDate, time: '19:00', duration: 60, addressLabel: "Teacher's studio", addressDetail: '', note: '' })
const slots = [{ day: 'TOMORROW', time: '19:00', value: '19:00' }, { day: 'SATURDAY', time: '14:30', value: '14:30' }, { day: 'SUNDAY', time: '10:00', value: '10:00' }]
const totalPrice = computed(() => Math.round((teacher.value?.basePrice || 0) * form.duration / 60))

function initials(name = '') { return name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase() }
function normalize(raw) {
  return { ...raw, realName: raw.realName ?? raw.real_name ?? raw.name, basePrice: raw.basePrice ?? raw.base_price ?? raw.hourly_rate, reviewCount: raw.reviewCount ?? raw.review_count ?? 0, bookingCount: raw.bookingCount ?? raw.booking_count ?? 0, accent: raw.accent || '#a85f38' }
}
onMounted(async () => {
  const preview = findDemoTeacher(route.params.id)
  if (preview) { teacher.value = preview; return }
  try { const { data } = await api.get(`/teachers/${route.params.id}`); teacher.value = normalize(data) }
  catch { error.value = true }
})

async function startBooking() {
  message.value = ''; success.value = false
  if (!form.date || !form.time) { message.value = 'Choose a date and start time first.'; return }
  if (teacher.value.isPreview) { success.value = true; message.value = 'Preview complete—no booking was submitted. Real teacher onboarding is the next release step.'; return }
  if (!auth.isLoggedIn) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return }
  submitting.value = true
  try {
    await api.post('/bookings', { teacher_id: Number(teacher.value.id), scheduled_start: `${form.date}T${form.time}:00`, duration_minutes: form.duration, price: totalPrice.value, address_label: form.addressLabel, address_detail: form.addressDetail, latitude: 0, longitude: 0, note: form.note })
    success.value = true; message.value = 'Lesson request sent. You can follow its status in My lessons.'
  } catch (e) { message.value = e.response?.data?.message || 'We could not send this request. Please try again.' }
  finally { submitting.value = false }
}
function startChat() {
  if (teacher.value.isPreview) { success.value = true; message.value = 'Messaging is disabled for preview profiles.'; return }
  if (!auth.isLoggedIn) { router.push({ path: '/login', query: { redirect: route.fullPath } }); return }
  router.push({ path: '/chat', query: { teacherId: teacher.value.id } })
}
</script>

<style scoped>
.profile-hero{min-height:470px;padding:42px max(24px,calc((100% - 1180px)/2)) 58px;display:grid;grid-template-columns:340px 1fr;gap:68px;align-items:end;background:var(--ink);color:var(--paper);position:relative}.back{position:absolute;top:34px;left:max(24px,calc((100% - 1180px)/2));border:0;background:none;color:#9ca4aa;cursor:pointer}.portrait{height:330px;display:grid;place-items:center;position:relative;background:var(--accent);overflow:hidden}.portrait::before{content:'';position:absolute;width:260px;height:260px;border:1px solid rgba(255,255,255,.45);border-radius:46% 54% 55% 45%;transform:rotate(-10deg)}.portrait span{font:500 6rem 'Fraunces';letter-spacing:-.08em}.portrait b{position:absolute;left:14px;top:14px;padding:6px 8px;background:rgba(23,33,43,.8);font:500 .55rem 'IBM Plex Mono';letter-spacing:.1em}.eyebrow{font:500 .65rem 'IBM Plex Mono';letter-spacing:.15em;color:var(--pick)}h1,h2,h3{font-family:'Fraunces';font-weight:500}h1{font-size:clamp(3.3rem,6vw,5.6rem);line-height:.95;letter-spacing:-.05em}.profile-copy .bio{max-width:650px;margin:22px 0;color:#c4c8c7;line-height:1.75}.tags{display:flex;gap:8px;flex-wrap:wrap}.tags span{padding:6px 10px;border:1px solid rgba(247,242,232,.25);font-size:.65rem}.proof{display:flex;gap:38px;margin-top:38px}.proof span{color:#858e94;font-size:.62rem}.proof strong{display:block;margin-bottom:4px;color:var(--paper);font:500 .95rem 'IBM Plex Mono'}.booking-layout{display:grid;grid-template-columns:1fr 380px;gap:85px;padding:85px max(24px,calc((100% - 1180px)/2)) 110px}.lesson-story h2{margin:10px 0 14px;font-size:clamp(2rem,4vw,3.5rem);letter-spacing:-.04em}.lesson-story>p:not(.eyebrow){color:#656b6d}.fretboard{display:grid;grid-template-columns:repeat(3,1fr);margin-top:50px;border-top:6px solid var(--wood);border-bottom:6px solid var(--wood);background:repeating-linear-gradient(0deg,transparent 0 31px,rgba(23,33,43,.16) 32px 33px)}.fretboard button{min-height:110px;border:0;border-right:2px solid rgba(23,33,43,.3);background:transparent;cursor:pointer}.fretboard button:last-child{border-right:0}.fretboard button.selected{background:var(--pick)}.fretboard small,.fretboard strong{display:block}.fretboard small{color:#676d6e;font:500 .54rem 'IBM Plex Mono';letter-spacing:.08em}.fretboard strong{margin-top:9px;font:500 1.05rem 'IBM Plex Mono'}.what-to-expect{display:flex;gap:22px;margin-top:55px;padding:26px;border:1px solid var(--line);background:#fbf8f1}.what-to-expect>span{color:var(--wood);font-size:2rem}.what-to-expect h3{font-size:1.25rem}.what-to-expect p{margin-top:7px;color:#676d6e;font-size:.72rem;line-height:1.7}.booking-card{align-self:start;padding:28px;background:#e9dfce;border-top:5px solid var(--wood);box-shadow:0 18px 45px rgba(23,33,43,.12)}.booking-card .eyebrow{color:var(--wood)}.price{display:flex;justify-content:space-between;align-items:end;margin:15px 0 24px;padding-bottom:18px;border-bottom:1px solid var(--line)}.price strong{font:500 2rem 'IBM Plex Mono'}.price span{color:#747a79;font-size:.65rem}.booking-card label{display:block;margin:13px 0}.booking-card label span{display:block;margin-bottom:6px;color:#676d6d;font:500 .58rem 'IBM Plex Mono';text-transform:uppercase;letter-spacing:.09em}.booking-card input,.booking-card select,.booking-card textarea{width:100%;padding:11px;border:1px solid rgba(23,33,43,.22);background:#f8f3e9}.book,.message{width:100%;padding:14px;border:0;cursor:pointer;font-weight:700}.book{margin-top:8px;background:var(--ink);color:white}.book:disabled{opacity:.55}.message{margin-top:8px;border:1px solid var(--ink);background:transparent;color:var(--ink)}.notice{margin-top:12px;padding:10px;background:#f7d9d1;color:#7a3027;font-size:.68rem;line-height:1.5}.notice.success{background:#dbe8df;color:#24553e}.fine-print{text-align:center;margin-top:12px;color:#777c79;font-size:.58rem}.state{min-height:70vh;display:grid;place-content:center;text-align:center;gap:12px}.state span{color:var(--wood);font-size:2rem}.state button{padding:11px 16px;border:0;background:var(--ink);color:#fff;cursor:pointer}
@media(max-width:800px){.profile-hero{grid-template-columns:250px 1fr;gap:32px}.portrait{height:270px}.booking-layout{grid-template-columns:1fr;gap:45px}.booking-card{width:min(100%,480px)}}@media(max-width:580px){.profile-hero{padding:78px 18px 42px;grid-template-columns:1fr;gap:25px}.back{left:18px}.portrait{height:230px}.proof{gap:20px;flex-wrap:wrap}.booking-layout{padding:60px 18px 75px}.fretboard{grid-template-columns:1fr}.fretboard button{border-right:0;border-bottom:1px solid var(--line)}}
.profile-hero{overflow:hidden}.profile-copy{min-width:0}.profile-copy .bio{overflow-wrap:anywhere}
</style>
