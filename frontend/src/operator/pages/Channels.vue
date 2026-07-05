<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiClient } from '../api/client'
import { useAuth } from '../composables/useAuth'

interface Channel {
  id: string
  kind: 'slack' | 'email' | 'webhook' | 'ci_check'
  name: string
  configRef: string
  enabled: boolean
}

const channels = ref<Channel[]>([])
const kind = ref<Channel['kind']>('slack')
const name = ref('')
const configRef = ref('')
const error = ref<string | null>(null)
const { user } = useAuth()

async function load() {
  try {
    const res = await apiClient.get<Channel[]>('/api/v1/projects/current/channels')
    channels.value = res.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

async function create() {
  error.value = null
  try {
    await apiClient.post('/api/v1/projects/current/channels', {
      kind: kind.value, name: name.value, configRef: configRef.value
    })
    name.value = ''
    configRef.value = ''
    await load()
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

async function remove(id: string) {
  try {
    await apiClient.delete(`/api/v1/projects/current/channels/${id}`)
    await load()
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

onMounted(load)
</script>

<template>
  <div class="channels">
    <h1>Notification Channels</h1>
    <p v-if="!user">Sign in to manage channels.</p>
    <template v-else>
      <form @submit.prevent="create" class="new-channel">
        <h2>Add channel</h2>
        <label>Kind
          <select v-model="kind">
            <option value="slack">Slack</option>
            <option value="email">Email</option>
            <option value="webhook">Webhook</option>
            <option value="ci_check">CI check</option>
          </select>
        </label>
        <label>Name <input v-model="name" required /></label>
        <label>Config ref (Key Vault URI) <input v-model="configRef" required placeholder="kv://..." /></label>
        <button type="submit">Add</button>
      </form>
      <table>
        <thead>
          <tr><th>Kind</th><th>Name</th><th>Config ref</th><th>Enabled</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="c in channels" :key="c.id">
            <td>{{ c.kind }}</td>
            <td>{{ c.name }}</td>
            <td><code>{{ c.configRef }}</code></td>
            <td>{{ c.enabled ? 'yes' : 'no' }}</td>
            <td><button @click="remove(c.id)">Delete</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="error" class="error">{{ error }}</p>
    </template>
  </div>
</template>

<style scoped>
.channels { padding: 1.5rem; max-width: 800px; }
.new-channel { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 2rem; }
label { display: flex; flex-direction: column; gap: 0.25rem; }
input, select { padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 4px; }
button { padding: 0.4rem 0.8rem; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
code { background: #f3f4f6; padding: 0.1rem 0.25rem; border-radius: 3px; }
.error { color: #dc2626; }
</style>
