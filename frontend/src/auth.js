import { reactive } from 'vue'
import { api } from './api'

export const auth = reactive({ user: null, ready: false })
let bootstrapPromise
export async function bootstrapAuth() {
  if (auth.ready) return
  if (!bootstrapPromise) bootstrapPromise = (async () => {
    try { auth.user = await api.me() }
    catch (error) { if (error.status !== 401) throw error }
    finally { auth.ready = true }
  })()
  return bootstrapPromise
}
export async function signIn(data) {
  auth.user = await api.login(data)
  auth.ready = true
}
