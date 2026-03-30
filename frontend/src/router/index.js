import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LandingPage from '../views/LandingPage.vue'

/**
 * Route meta fields control access:
 * - guestOnly:     redirect authenticated users to /mydex (login, register, etc.)
 * - requiresAuth:  redirect unauthenticated users to /login
 * - requiresAdmin: redirect non-admin users to /mydex (checked after requiresAuth)
 *
 * The beforeEach guard below enforces these. It awaits initAuthListener() first
 * so the session is fully resolved before making redirect decisions.
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior() {
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: LandingPage,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/mydex',
      name: 'mydex',
      component: () => import('../views/MyDexPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/detect',
      name: 'detect',
      component: () => import('../views/DetectPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/gallery',
      name: 'gallery',
      component: () => import('../views/GalleryPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/account',
      name: 'account',
      component: () => import('../views/AccountPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/privacy-policy',
      name: 'privacy-policy',
      component: () => import('../views/PrivacyPolicyPage.vue'),
    },
    {
      path: '/verify-email',
      name: 'verify-email',
      component: () => import('../views/VerifyEmailPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('../views/ResetPasswordPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundPage.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  // Wait for auth session to resolve before making guard decisions
  await authStore.initAuthListener()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' }
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'mydex' }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'mydex' }
  }
})

export default router
