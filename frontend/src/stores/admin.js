import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authClient } from '@/lib/auth-client'

const API = import.meta.env.VITE_API_URL

export const useAdminStore = defineStore('admin', () => {
  // --- State ---
  const stats = ref({ users: 0, birds: 0, sightings: 0 })
  const users = ref([])
  const birds = ref([])
  const sightings = ref([])
  const loading = ref(false)
  const error = ref(null)
  let controller = null

  function abortPrevious() {
    if (controller) controller.abort()
    controller = new AbortController()
    return controller.signal
  }

  function clearError() {
    error.value = null
  }

  // --- Stats ---
  async function loadStats() {
    const signal = abortPrevious()
    loading.value = true
    error.value = null
    try {
      const res = await fetch(`${API}/api/admin/stats`, {
        credentials: 'include',
        signal,
      })
      if (!res.ok) throw new Error('Failed to load stats')
      stats.value = await res.json()
    } catch (err) {
      if (err.name !== 'AbortError') error.value = err.message
    } finally {
      loading.value = false
    }
  }

  // --- Users (via better-auth admin client) ---
  async function loadUsers({ query = '', limit = 50, offset = 0 } = {}) {
    loading.value = true
    error.value = null
    try {
      const params = { limit, offset }
      if (query) params.searchField = 'email'
      if (query) params.searchValue = query
      if (query) params.searchOperator = 'contains'

      const result = await authClient.admin.listUsers({
        query: {
          limit,
          offset,
          ...(query
            ? { searchField: 'email', searchValue: query, searchOperator: 'contains' }
            : {}),
        },
      })
      if (result.error) throw new Error(result.error.message || 'Failed to load users')
      users.value = result.data?.users ?? []
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  async function setUserRole(userId, role) {
    error.value = null
    try {
      const result = await authClient.admin.setRole({ userId, role })
      if (result.error) throw new Error(result.error.message || 'Failed to set role')
      const user = users.value.find((u) => u.id === userId)
      if (user) user.role = role
      return true
    } catch (err) {
      error.value = err.message
      return false
    }
  }

  async function banUser(userId, banReason) {
    error.value = null
    try {
      const result = await authClient.admin.banUser({ userId, banReason })
      if (result.error) throw new Error(result.error.message || 'Failed to ban user')
      const user = users.value.find((u) => u.id === userId)
      if (user) {
        user.banned = true
        user.banReason = banReason
      }
      return true
    } catch (err) {
      error.value = err.message
      return false
    }
  }

  async function unbanUser(userId) {
    error.value = null
    try {
      const result = await authClient.admin.unbanUser({ userId })
      if (result.error) throw new Error(result.error.message || 'Failed to unban user')
      const user = users.value.find((u) => u.id === userId)
      if (user) {
        user.banned = false
        user.banReason = null
      }
      return true
    } catch (err) {
      error.value = err.message
      return false
    }
  }

  async function removeUser(userId) {
    error.value = null
    try {
      const result = await authClient.admin.removeUser({ userId })
      if (result.error) throw new Error(result.error.message || 'Failed to remove user')
      users.value = users.value.filter((u) => u.id !== userId)
      return true
    } catch (err) {
      error.value = err.message
      return false
    }
  }

  // --- Birds ---
  async function loadBirds({ query = '', limit = 50, offset = 0 } = {}) {
    const signal = abortPrevious()
    loading.value = true
    error.value = null
    try {
      const params = new URLSearchParams({ limit: String(limit), offset: String(offset) })
      if (query) params.set('q', query)
      const res = await fetch(`${API}/api/admin/birds?${params}`, {
        credentials: 'include',
        signal,
      })
      if (!res.ok) throw new Error('Failed to load birds')
      birds.value = await res.json()
    } catch (err) {
      if (err.name !== 'AbortError') error.value = err.message
    } finally {
      loading.value = false
    }
  }

  async function createBird(data) {
    error.value = null
    try {
      const res = await fetch(`${API}/api/admin/birds`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body.error || 'Failed to create bird')
      }
      const bird = await res.json()
      birds.value.push(bird)
      birds.value.sort((a, b) => a.name.localeCompare(b.name))
      return bird
    } catch (err) {
      error.value = err.message
      return null
    }
  }

  async function updateBird(id, data) {
    error.value = null
    try {
      const res = await fetch(`${API}/api/admin/birds/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(data),
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body.error || 'Failed to update bird')
      }
      const updated = await res.json()
      const idx = birds.value.findIndex((b) => b.id === id)
      if (idx !== -1) birds.value[idx] = updated
      return updated
    } catch (err) {
      error.value = err.message
      return null
    }
  }

  async function deleteBird(id) {
    error.value = null
    try {
      const res = await fetch(`${API}/api/admin/birds/${id}`, {
        method: 'DELETE',
        credentials: 'include',
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body.error || 'Failed to delete bird')
      }
      birds.value = birds.value.filter((b) => b.id !== id)
      return true
    } catch (err) {
      error.value = err.message
      return false
    }
  }

  // --- Sightings ---
  async function loadSightings({ userId = '', birdId = '', limit = 50, offset = 0 } = {}) {
    const signal = abortPrevious()
    loading.value = true
    error.value = null
    try {
      const params = new URLSearchParams({ limit: String(limit), offset: String(offset) })
      if (userId) params.set('userId', userId)
      if (birdId) params.set('birdId', String(birdId))
      const res = await fetch(`${API}/api/admin/sightings?${params}`, {
        credentials: 'include',
        signal,
      })
      if (!res.ok) throw new Error('Failed to load sightings')
      sightings.value = await res.json()
    } catch (err) {
      if (err.name !== 'AbortError') error.value = err.message
    } finally {
      loading.value = false
    }
  }

  async function deleteSighting(id) {
    error.value = null
    try {
      const res = await fetch(`${API}/api/admin/sightings/${id}`, {
        method: 'DELETE',
        credentials: 'include',
      })
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body.error || 'Failed to delete sighting')
      }
      sightings.value = sightings.value.filter((s) => s.id !== id)
      return true
    } catch (err) {
      error.value = err.message
      return false
    }
  }

  return {
    stats,
    users,
    birds,
    sightings,
    loading,
    error,
    clearError,
    loadStats,
    loadUsers,
    setUserRole,
    banUser,
    unbanUser,
    removeUser,
    loadBirds,
    createBird,
    updateBird,
    deleteBird,
    loadSightings,
    deleteSighting,
  }
})
