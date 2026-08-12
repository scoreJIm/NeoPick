import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import App from './App.vue'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/Home.vue') },
  { path: '/teachers/:id', name: 'teacher', component: () => import('./views/TeacherDetail.vue') },
  { path: '/login', name: 'login', component: () => import('./views/Login.vue') },
  { path: '/bookings', name: 'bookings', component: () => import('./views/Bookings.vue') },
  { path: '/chat', name: 'chat', component: () => import('./views/Chat.vue') },
]

const router = createRouter({ history: createWebHistory(), routes })
const pinia = createPinia()
pinia.use(({ store }) => { store.$router = router })

createApp(App).use(router).use(pinia).mount('#app')
