<template>
  <div class="home">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">YOUR CITY · YOUR SOUND</p>
        <h1>Find the teacher who gets <em>your</em> music.</h1>
        <p class="intro">Book one-to-one guitar lessons around your life—not the other way around.</p>
      </div>

      <form class="finder" @submit.prevent="search">
        <p class="finder-label">BUILD YOUR FIRST LESSON</p>
        <label><span>Where?</span><select v-model="city"><option value="">Any city</option><option v-for="c in cities" :key="c.code || c.id" :value="c.code || c.id">{{ c.name }}</option></select></label>
        <label><span>When?</span><select v-model="timing"><option value="">Any time</option><option>Weekday evenings</option><option>Weekends</option><option>Daytime</option></select></label>
        <label><span>Your level</span><select v-model="level"><option value="">Any level</option><option value="BEGINNER">Just starting</option><option value="INTERMEDIATE">I know the basics</option><option value="ADVANCED">Advanced</option></select></label>
        <button type="submit">Show my matches <span>→</span></button>
      </form>
      <div class="hero-note"><span class="pulse"></span><span>Real teachers · Clear pricing · No long-term contract</span></div>
    </section>

    <section class="results" id="teachers">
      <div class="results-heading">
        <div><p class="eyebrow">HANDPICKED MATCHES</p><h2>Teachers worth meeting</h2></div>
        <label class="keyword"><span>Search</span><input v-model="keyword" placeholder="style, teacher, technique…" @keyup.enter="search" /></label>
      </div>
      <div v-if="previewMode" class="preview-note"><strong>Preview profiles</strong><span>The live teacher directory is being onboarded. These sample profiles show the intended booking experience.</span></div>
      <div v-if="loading" class="loading">Tuning the shortlist…</div>
      <div v-else-if="teachers.length" class="teacher-grid">
        <article v-for="(teacher, index) in teachers" :key="teacher.id" class="teacher-card" tabindex="0" @click="$router.push(`/teachers/${teacher.id}`)" @keyup.enter="$router.push(`/teachers/${teacher.id}`)">
          <div class="portrait" :style="{ '--accent': teacher.accent || accents[index % accents.length] }">
            <span class="portrait-initials">{{ initials(teacher.realName) }}</span><span v-if="teacher.isPreview" class="sample-label">SAMPLE</span><span class="rating">★ {{ teacher.rating || 'New' }}</span>
          </div>
          <div class="teacher-body">
            <div class="teacher-title"><div><h3>{{ teacher.realName }}</h3><p>{{ teacher.district }} · {{ teacher.city?.name }}</p></div><p class="price"><strong>¥{{ teacher.basePrice }}</strong><span>/ lesson</span></p></div>
            <p class="bio">{{ teacher.bio }}</p>
            <div class="tags"><span v-for="tag in teacher.tags?.slice(0, 3)" :key="tag">{{ tag }}</span></div>
            <div class="next-slot"><span>Next opening</span><strong>{{ teacher.availability?.[0] || 'Ask for availability' }}</strong><b>View profile →</b></div>
          </div>
        </article>
      </div>
      <div v-else class="empty"><span>♪</span><h3>No exact match yet.</h3><p>Try another city or level—we're adding new teachers every week.</p></div>
    </section>

    <section class="promise">
      <p class="eyebrow">NO AWKWARD COMMITMENT</p><h2>One lesson is enough to know.</h2>
      <div class="promise-grid"><p><span>01</span><strong>Choose by fit</strong>Compare style, location, price, and real availability.</p><p><span>02</span><strong>Meet for one lesson</strong>Book a clear time and place. No package required.</p><p><span>03</span><strong>Keep the good ones</strong>Rebook when the chemistry—and the groove—feels right.</p></div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/client.js'
import { demoTeachers } from '../data/demoTeachers.js'

const keyword = ref(''); const city = ref(''); const timing = ref(''); const level = ref('')
const teachers = ref([]); const cities = ref([]); const loading = ref(true); const previewMode = ref(false)
const accents = ['#ca6b45', '#2f6c68', '#8b6944']

function normalizeTeacher(raw, index = 0) {
  return { ...raw, realName: raw.realName ?? raw.real_name ?? raw.name ?? 'NeoPick teacher', basePrice: raw.basePrice ?? raw.base_price ?? raw.hourlyRate ?? raw.hourly_rate, reviewCount: raw.reviewCount ?? raw.review_count ?? 0, bookingCount: raw.bookingCount ?? raw.booking_count ?? 0, avatarUrl: raw.avatarUrl ?? raw.avatar_url, accent: raw.accent || accents[index % accents.length] }
}
function initials(name = '') { return name.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase() }
async function loadCities() {
  try { const { data } = await api.get('/cities'); cities.value = Array.isArray(data) ? data : data?.content || [] }
  catch { cities.value = [{ code: 'SH', name: 'Shanghai' }, { code: 'NJ', name: 'Nanjing' }, { code: 'HZ', name: 'Hangzhou' }] }
}
async function search() {
  loading.value = true
  const params = { page: 0, size: 12 }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (city.value) params.city = city.value
  if (level.value) params.level = level.value
  try {
    const { data } = await api.get('/teachers', { params })
    const liveTeachers = (data?.content || (Array.isArray(data) ? data : [])).map(normalizeTeacher)
    if (liveTeachers.length) { teachers.value = liveTeachers; previewMode.value = false; return }
  } catch { /* Preview catalogue keeps the journey usable while onboarding starts. */ }
  finally { loading.value = false }
  previewMode.value = true
  teachers.value = demoTeachers.filter((teacher) => {
    const matchesCity = !city.value || teacher.city.code === city.value
    const matchesLevel = !level.value || teacher.level === level.value
    const haystack = [teacher.realName, teacher.bio, ...teacher.tags].join(' ').toLowerCase()
    return matchesCity && matchesLevel && (!keyword.value.trim() || haystack.includes(keyword.value.trim().toLowerCase()))
  })
}
onMounted(async () => { await Promise.all([loadCities(), search()]) })
</script>

<style scoped>
.hero{min-height:630px;padding:82px max(24px,calc((100% - 1180px)/2)) 58px;background:var(--ink);color:var(--paper);position:relative;overflow:hidden}.hero::before{content:'';position:absolute;width:560px;height:560px;border:1px solid rgba(242,193,78,.25);border-radius:50%;right:-170px;top:-215px;box-shadow:0 0 0 90px rgba(242,193,78,.025),0 0 0 180px rgba(242,193,78,.02)}.hero::after{content:'';position:absolute;width:42%;height:8px;left:58%;top:206px;background:repeating-linear-gradient(90deg,transparent 0 69px,rgba(247,242,232,.4) 70px 72px);border-top:1px solid rgba(247,242,232,.16);border-bottom:1px solid rgba(247,242,232,.16)}.hero-copy{position:relative;z-index:2;max-width:780px}.eyebrow{font:500 .68rem 'IBM Plex Mono';letter-spacing:.16em;color:var(--wood)}.hero .eyebrow{color:var(--pick)}h1,h2,h3{font-family:'Fraunces',serif;font-weight:500}h1{margin:18px 0;font-size:clamp(3.25rem,7vw,6.7rem);line-height:.92;letter-spacing:-.055em}h1 em{color:var(--pick);font-weight:500}.intro{width:min(570px,90%);color:#c7c7c0;font-size:1rem;line-height:1.7}.finder{position:relative;z-index:3;display:grid;grid-template-columns:1.15fr 1fr 1fr 1.15fr;margin-top:56px;border:1px solid rgba(247,242,232,.22);background:#202b35}.finder-label{position:absolute;top:-27px;left:0;color:#8d969e;font:.62rem 'IBM Plex Mono';letter-spacing:.14em}.finder label{display:flex;flex-direction:column;gap:8px;padding:18px 20px;border-right:1px solid rgba(247,242,232,.14)}.finder label span{color:#8d969e;font:.6rem 'IBM Plex Mono';letter-spacing:.1em;text-transform:uppercase}.finder select{color:var(--paper);background:transparent;border:0;outline:0;cursor:pointer}.finder select option{color:var(--ink)}.finder button{border:0;padding:20px;background:var(--pick);color:var(--ink);font-weight:700;cursor:pointer}.finder button span{margin-left:8px;transition:margin .2s}.finder button:hover span{margin-left:14px}.hero-note{position:relative;z-index:2;display:flex;align-items:center;gap:9px;margin-top:24px;color:#858f97;font-size:.7rem}.pulse{width:7px;height:7px;border-radius:50%;background:#71a487;box-shadow:0 0 0 5px rgba(113,164,135,.13)}
.results{padding:86px max(24px,calc((100% - 1180px)/2)) 105px}.results-heading{display:flex;justify-content:space-between;align-items:end;gap:30px;margin-bottom:30px}h2{margin-top:8px;font-size:clamp(2rem,4vw,3.35rem);letter-spacing:-.035em}.keyword{width:min(330px,100%)}.keyword span{display:block;color:#7d817f;font:.6rem 'IBM Plex Mono';text-transform:uppercase;letter-spacing:.12em}.keyword input{width:100%;padding:10px 0;border:0;border-bottom:1px solid var(--ink);background:transparent;outline:0}.preview-note{display:flex;gap:12px;align-items:baseline;margin:0 0 24px;padding:13px 16px;border-left:3px solid var(--pick);background:rgba(242,193,78,.12);font-size:.72rem}.preview-note strong{font-family:'IBM Plex Mono';white-space:nowrap}.preview-note span{color:#6a6f70}.teacher-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:22px}.teacher-card{border:1px solid var(--line);background:#fbf8f1;cursor:pointer;transition:transform .25s,box-shadow .25s}.teacher-card:hover{transform:translateY(-6px);box-shadow:0 18px 40px rgba(23,33,43,.12)}.portrait{height:210px;position:relative;display:grid;place-items:center;overflow:hidden;background:var(--accent)}.portrait::before{content:'';position:absolute;inset:18px;border:1px solid rgba(255,255,255,.45);border-radius:50% 50% 44% 56%;transform:rotate(-8deg)}.portrait-initials{color:rgba(255,255,255,.92);font:500 4.5rem 'Fraunces';letter-spacing:-.08em}.sample-label{position:absolute;top:13px;left:13px;padding:5px 7px;background:rgba(23,33,43,.82);color:white;font:.54rem 'IBM Plex Mono';letter-spacing:.12em}.rating{position:absolute;right:13px;bottom:13px;padding:6px 9px;background:var(--paper);font:.68rem 'IBM Plex Mono'}.teacher-body{padding:22px}.teacher-title{display:flex;justify-content:space-between;gap:12px}.teacher-title h3{font-size:1.55rem}.teacher-title p{color:#727779;font-size:.68rem;margin-top:4px}.price{text-align:right;white-space:nowrap}.price strong{display:block;color:var(--wood);font:500 1.05rem 'IBM Plex Mono'}.price span{font-size:.58rem}.bio{min-height:66px;margin:18px 0;color:#596064;font-size:.76rem;line-height:1.65}.tags{display:flex;gap:6px;flex-wrap:wrap}.tags span{padding:5px 8px;border:1px solid var(--line);font-size:.6rem}.next-slot{display:grid;grid-template-columns:1fr auto;gap:5px;margin:20px -22px -22px;padding:15px 22px;border-top:1px solid var(--line)}.next-slot span{color:#85898a;font:.55rem 'IBM Plex Mono';text-transform:uppercase}.next-slot strong{grid-row:2;font:500 .68rem 'IBM Plex Mono'}.next-slot b{grid-column:2;grid-row:1/3;align-self:center;font-size:.68rem;color:var(--green)}
.promise{padding:88px max(24px,calc((100% - 1180px)/2));background:#e8dfce}.promise-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:50px;margin-top:42px}.promise-grid p{color:#646967;font-size:.76rem;line-height:1.65}.promise-grid span,.promise-grid strong{display:block}.promise-grid span{color:var(--wood);font:.65rem 'IBM Plex Mono'}.promise-grid strong{margin:10px 0 8px;color:var(--ink);font-family:'Fraunces';font-size:1.2rem}.loading,.empty{padding:80px 20px;text-align:center;color:#747b7c}.empty span{color:var(--wood);font-size:2rem}.empty h3{margin:10px;color:var(--ink);font-size:1.5rem}
@media(max-width:850px){.finder{grid-template-columns:1fr 1fr}.finder label:nth-of-type(2){border-right:0}.finder label{border-bottom:1px solid rgba(247,242,232,.14)}.teacher-grid{grid-template-columns:1fr 1fr}}@media(max-width:600px){.hero{min-height:auto;padding:64px 18px 42px}.hero::after{display:none}h1{font-size:3.15rem}.finder{grid-template-columns:1fr;margin-top:50px}.finder label{border-right:0}.results{padding:62px 18px 75px}.results-heading{align-items:start;flex-direction:column}.keyword{width:100%}.preview-note{align-items:start;flex-direction:column}.teacher-grid,.promise-grid{grid-template-columns:1fr}.promise{padding:62px 18px}.promise-grid{gap:28px}}
</style>
