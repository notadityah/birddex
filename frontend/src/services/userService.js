import { doc, getDoc, setDoc, serverTimestamp } from 'firebase/firestore'
import { db } from '@/firebase'

/**
 * Create a new user profile in Firestore
 * @param {string} uid - Firebase Auth user ID
 * @param {{ name: string, email: string }} data
 */
export async function createUserProfile(uid, { name, email }) {
  const userRef = doc(db, 'users', uid)
  await setDoc(userRef, {
    name,
    email,
    role: 'user',
    createdAt: serverTimestamp(),
  })
}

/**
 * Fetch a user profile from Firestore
 * @param {string} uid
 * @returns {Promise<{ name: string, email: string, role: string, createdAt: any } | null>}
 */
export async function getUserProfile(uid) {
  const userRef = doc(db, 'users', uid)
  const snap = await getDoc(userRef)
  return snap.exists() ? snap.data() : null
}
