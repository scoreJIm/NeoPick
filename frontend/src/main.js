import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/Home.vue') },
  { path: '/teachers/:id', name: 'teacher', component: () => import('./views/TeacherDetail.vue') },
  { path: '/login', name: 'login', component: () => import('./views/Login.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

createApp(App).use(router).mount('#app')
