import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/user/LoginPage.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/user/RegisterPage.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/chat/ChatLayout.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/user/profile',
      name: 'user-profile',
      component: () => import('@/views/user/ProfilePage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const user = localStorage.getItem('user')
  const isLoggedIn = !!user

  if (to.meta.requiresAuth && !isLoggedIn) {
    next({ name: 'login' })
  } else if (!to.meta.requiresAuth && isLoggedIn && (to.name === 'login' || to.name === 'register')) {
    next({ name: 'chat' })
  } else {
    next()
  }
})

export default router
