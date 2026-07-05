<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiClient } from '../api/client'

interface AllowlistEntry { id: string; hostPattern: string; source: string }
interface Secret { id: string; name: string; keyvaultSecretRef: string; lastRotatedAt?: string }

const allowlist = ref<AllowlistEntry[]>([])
const secrets = ref<Secret[]>([])
const newHost = ref('')
const newSecretName = ref('')
const newSecretRef = ref('')
const error = ref<string | null>(null)

async function load() {
  try {
    const [a, s] = await Promise.all([
      apiClient.get<AllowlistEntry[]>('/api/v1/projects/current/tryit/allowlist'),
      apiClient.get<Secret[]>('/api/v1/projects/current/tryit/secrets'),
    ])
    allowlist.value = a.data
    secrets.value = s.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

async function addHost() {
  if (!newHost.value) return
  try {
    await apiClient.post(`/api/v1/projects/current/tryit/allowlist?hostPattern=${encodeURIComponent(newHost.value)}`)
    newHost.value = ''
    await load()
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

async function addSecret() {
  if (!newSecretName.value || !newSecretRef.value) return
  try {
    await apiClient.post(`/api/v1/projects/current/tryit/secrets?name=${encodeURIComponent(newSecretName.value)}&keyvaultSecretRef=${encodeURIComponent(newSecretRef.value)}`)
    newSecretName.value = ''
    newSecretRef.value = ''
    await load()
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

onMounted(load)
</script>

<template>
  <div class="tryit-settings">
    <h1>Try It Settings</h1>
    <p v-if="error" class="error">{{ error }}</p>
    <section>
      <h2>Allowlist</h2>
      <form @submit.prevent="addHost">
        <input v-model="newHost" placeholder="api.example.com" />
        <button type="submit">Add host</button>
      </form>
      <ul>
        <li v-for="e in allowlist" :key="e.id">
          <code>{{ e.hostPattern }}</code> <span class="source">({{ e.source }})</span>
        </li>
      </ul>
    </section>
    <section>
      <h2>Secrets (Key Vault references)</h2>
      <p class="hint">Secret values are never stored or returned. Only the Key Vault path is referenced.</p>
      <form @submit.prevent="addSecret">
        <input v-model="newSecretName" placeholder="Production API key" />
        <input v-model="newSecretRef" placeholder="kv://..." />
        <button type="submit">Add secret</button>
      </form>
      <ul>
        <li v-for="s in secrets" :key="s.id">
          <code>{{ s.name }}</code> — <code>{{ s.keyvaultSecretRef }}</code>
          <span v-if="s.lastRotatedAt" class="rotated">(rotated {{ new Date(s.lastRotatedAt).toLocaleString() }})</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.tryit-settings { padding: 1.5rem; max-width: 800px; }
section { margin-bottom: 2rem; }
form { display: flex; gap: 0.5rem; margin-bottom: 0.5rem; }
input { flex: 1; padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 4px; }
button { padding: 0.4rem 0.8rem; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
ul { padding-left: 1.5rem; }
li { margin: 0.25rem 0; }
code { background: #f3f4f6; padding: 0.1rem 0.25rem; border-radius: 3px; }
.source { color: #6b7280; font-size: 0.85rem; }
.rotated { color: #059669; font-size: 0.85rem; }
.hint { color: #4b5563; font-size: 0.9rem; margin-bottom: 0.5rem; }
.error { color: #dc2626; }
</style>
