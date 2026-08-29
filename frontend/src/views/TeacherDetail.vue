<template>
  <div class="teacher-detail" v-if="teacher">
    <button class="back" @click="$router.back()">&larr; Back</button>
    <div class="profile">
      <div class="avatar">{{ teacher.name?.charAt(0) }}</div>
      <h1>{{ teacher.name }}</h1>
      <p class="level">{{ teacher.level }}</p>
      <p class="city">{{ teacher.city?.name }}</p>
      <p class="price" v-if="teacher.hourlyRate">¥{{ teacher.hourlyRate }} / hour</p>
      <p class="bio" v-if="teacher.bio">{{ teacher.bio }}</p>
    </div>

    <section class="actions" v-if="auth.isLoggedIn">
      <button class="btn-primary" @click="startBooking">Book a Lesson</button>
      <button class="btn-secondary" @click="startChat">Send Message</button>
      <button class="btn-fav" @click="toggleFavorite">
        {{ favorited ? 'Unfavorite' : 'Add to Favorites' }}
      </button>
    </section>

    <section class="reviews" v-if="teacher.reviews?.length">
      <h2>Reviews</h2>
      <div v-for="r in teacher.reviews" :key="r.id" class="review-card">
        <span class="stars">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5 - r.rating) }}</span>
        <p>{{ r.content }}</p>
      </div>
    </section>
  </div>
  <p v-else class="loading">Loading...</p>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import api from '../api/client.js'

const route = useRoute()
const auth = useAuthStore()
const teacher = ref(null)
const favorited = ref(false)

onMounted(async () => {
  const { data } = await api.get(`/teachers/${route.params.id}`)
  teacher.value = data
  if (auth.isLoggedIn) {
    try {
      const { data: favs } = await api.get('/favorites')
      favorited.value = favs.some(f => f.teacherId === data.id)
    } catch {}
  }
})

function startBooking() { /* TODO */ }
function startChat() { /* TODO */ }
async function toggleFavorite() {
  if (favorited.value) {
    await api.delete(`/favorites/${teacher.value.id}`)
    favorited.value = false
  } else {
    await api.post('/favorites', { teacherId: teacher.value.id })
    favorited.value = true
  }
}
</script>

<style scoped>
.back { background: none; border: none; color: #666; cursor: pointer; margin-bottom: 1rem; }
.profile { text-align: center; margin-bottom: 2rem; }
.avatar { width: 80px; height: 80px; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 2rem; margin: 0 auto 0.5rem; }
.level { color: #888; } .city { color: #666; font-size: 0.95rem; } .price { color: #e74c3c; font-size: 1.2rem; font-weight: 600; margin: 0.5rem 0; }
.bio { color: #444; max-width: 500px; margin: 1rem auto; }
.actions { display: flex; gap: 0.75rem; justify-content: center; margin: 2rem 0; }
.btn-primary { padding: 0.6rem 1.5rem; background: #333; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-secondary { padding: 0.6rem 1.5rem; background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px; cursor: pointer; }
.btn-fav { padding: 0.6rem 1.5rem; background: none; border: 1px solid #e74c3c; color: #e74c3c; border-radius: 4px; cursor: pointer; }
.reviews h2 { margin-bottom: 1rem; }
.review-card { border-bottom: 1px solid #eee; padding: 1rem 0; }
.stars { color: #f0ad4e; }
.loading { text-align: center; color: #999; margin-top: 3rem; }
</style>
