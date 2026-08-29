<template>
  <div class="home">
    <section class="hero">
      <h1>Find Your Guitar Teacher</h1>
      <p>One-on-one lessons from professional musicians in your city.</p>
    </section>

    <div class="search-bar">
      <input v-model="keyword" placeholder="Search by name or instrument..." @keyup.enter="search" />
      <select v-model="city">
        <option value="">All cities</option>
        <option v-for="c in cities" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <button @click="search">Search</button>
    </div>

    <div class="teacher-grid" v-if="teachers.length">
      <div v-for="t in teachers" :key="t.id" class="teacher-card" @click="$router.push(`/teachers/${t.id}`)">
        <div class="avatar">{{ t.name?.charAt(0) }}</div>
        <h3>{{ t.name }}</h3>
        <p class="level">{{ t.level }}</p>
        <p class="price" v-if="t.hourlyRate">¥{{ t.hourlyRate }}/hr</p>
      </div>
    </div>
    <p v-else-if="searched" class="empty">No teachers found. Try a different search.</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/client.js'

const keyword = ref('')
const city = ref('')
const teachers = ref([])
const cities = ref([])
const searched = ref(false)

onMounted(async () => {
  try {
    const { data } = await api.get('/cities')
    cities.value = data
  } catch {}
})

async function search() {
  searched.value = true
  const params = {}
  if (keyword.value) params.keyword = keyword.value
  if (city.value) params.cityId = city.value
  try {
    const { data } = await api.get('/teachers', { params })
    teachers.value = data.content || data
  } catch {
    teachers.value = []
  }
}
</script>

<style scoped>
.hero { text-align: center; margin-bottom: 2rem; }
.hero h1 { font-size: 2rem; margin-bottom: 0.5rem; }
.hero p { color: #666; }
.search-bar { display: flex; gap: 0.5rem; margin-bottom: 2rem; }
.search-bar input { flex: 1; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; }
.search-bar select { padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; }
.search-bar button { padding: 0.6rem 1.5rem; background: #333; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.teacher-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 1.5rem; }
.teacher-card { border: 1px solid #eee; border-radius: 8px; padding: 1.5rem; text-align: center; cursor: pointer; }
.teacher-card:hover { border-color: #ccc; }
.avatar { width: 60px; height: 60px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; margin: 0 auto 0.75rem; }
.level { color: #888; font-size: 0.9rem; }
.price { color: #e74c3c; font-weight: 600; margin-top: 0.5rem; }
.empty { text-align: center; color: #999; margin-top: 3rem; }
</style>
