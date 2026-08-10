<template>
  <div class="bookings">
    <h2>My Bookings</h2>
    <div v-if="bookings.length">
      <div v-for="b in bookings" :key="b.id" class="booking-card">
        <div class="booking-info">
          <strong>{{ b.teacherName }}</strong>
          <span class="status" :class="b.status">{{ b.status }}</span>
          <p>{{ b.date }} · ¥{{ b.totalAmount }}</p>
        </div>
        <div class="booking-actions" v-if="b.status === 'PENDING_CONFIRM'">
          <button class="btn-confirm" @click="manage(b.id, 'confirm')">Confirm</button>
          <button class="btn-reject" @click="manage(b.id, 'reject')">Reject</button>
        </div>
      </div>
    </div>
    <p v-else class="empty">No bookings yet.</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/client.js'

const bookings = ref([])

onMounted(async () => {
  try {
    const { data } = await api.get('/bookings/student')
    bookings.value = data.content || data
  } catch {}
})

async function manage(id, action) {
  await api.put(`/bookings/${id}/${action}`)
  const idx = bookings.value.findIndex(b => b.id === id)
  if (idx > -1) bookings.value[idx].status = action === 'confirm' ? 'PENDING_PAY' : 'REJECTED'
}
</script>

<style scoped>
.booking-card { display: flex; justify-content: space-between; align-items: center; padding: 1rem 0; border-bottom: 1px solid #eee; }
.status { font-size: 0.8rem; padding: 0.15rem 0.5rem; border-radius: 3px; margin-left: 0.5rem; }
.PENDING_CONFIRM { background: #fff3cd; color: #856404; }
.PENDING_PAY { background: #cce5ff; color: #004085; }
.COMPLETED { background: #d4edda; color: #155724; }
.booking-actions { display: flex; gap: 0.5rem; }
.btn-confirm { padding: 0.3rem 0.75rem; background: #333; color: #fff; border: none; border-radius: 3px; cursor: pointer; }
.btn-reject { padding: 0.3rem 0.75rem; background: #fff; border: 1px solid #ddd; border-radius: 3px; cursor: pointer; }
.empty { text-align: center; color: #999; margin-top: 3rem; }
</style>
