import { createRouter, createWebHistory } from 'vue-router'
import { getCurrentUser, fetchAuthSession } from 'aws-amplify/auth'
import LandingPage from '../views/LandingPage.vue'

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
      path: '/confirm',
      name: 'confirm',
      component: () => import('../views/ConfirmSignUpPage.vue'),
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('../views/ResetPasswordPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('../views/DashboardPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/AdminPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

// ── Navigation guards ──
router.beforeEach(async (to) => {
  let isAuthenticated = false
  let isAdmin = false

  try {
    await getCurrentUser()
    isAuthenticated = true

    const session = await fetchAuthSession()
    const groups = session.tokens?.idToken?.payload?.['cognito:groups'] ?? []
    isAdmin = Array.isArray(groups) && groups.includes('ADMINS')
  } catch {
    // Not authenticated
  }

  // Redirect authenticated users away from guest-only pages
  if (to.meta.guestOnly && isAuthenticated) {
    return { name: 'dashboard' }
  }

  // Redirect unauthenticated users away from protected pages
  if (to.meta.requiresAuth && !isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // Redirect non-admins away from admin pages
  if (to.meta.requiresAdmin && !isAdmin) {
    return { name: 'dashboard' }
  }
})

export default router
