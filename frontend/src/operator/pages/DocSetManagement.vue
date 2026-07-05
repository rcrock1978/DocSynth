<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiClient } from '../api/client'
import type { DocSet } from '../../shared/types'

const docsets = ref<DocSet[]>([])
const error = ref<string | null>(null)
const projectId = ref<string>('current')

async function load() {
  try {
    const res = await apiClient.get<DocSet[]>(`/api/v1/projects/${projectId.value}/docsets`)
    docsets.value = res.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

async function transition(id: string, action: 'deprecate' | 'archive' | 'reactivate') {
  try {
    await apiClient.patch(`/api/v1/projects/${projectId.value}/docsets/${id}/state`, { action })
    await load()
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

onMounted(load)
</script>

<template>
  <div class="docset-management">
    <h1>DocSet Management</h1>
    <p v-if="error" class="error">{{ error }}</p>
    <table v-else>
      <thead>
        <tr>
          <th>Version</th>
          <th>State</th>
          <th>Try It</th>
          <th>Generated</th>
          <th>Deprecated</th>
          <th>Archived</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in docsets" :key="d.id">
          <td><code>{{ d.displayVersion }}</code></td>
          <td><span :class="`state state-${d.state}`">{{ d.state }}</span></td>
          <td>{{ d.tryItEnabled ? 'yes' : 'no' }}</td>
          <td>{{ new Date(d.generatedAt).toLocaleString() }}</td>
          <td>{{ d.deprecatedAt ? new Date(d.deprecatedAt).toLocaleString() : '—' }}</td>
          <td>{{ d.archivedAt ? new Date(d.archivedAt).toLocaleString() : '—' }}</td>
          <td class="actions">
            <button v-if="d.state === 'active'" @click="transition(d.id, 'deprecate')">Deprecate</button>
            <button v-if="d.state === 'deprecated' && !d.archivedAt" @click="transition(d.id, 'archive')">Archive</button>
            <button v-if="d.state === 'deprecated'" @click="transition(d.id, 'reactivate')">Reactivate</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p class="hint">Archived DocSets are terminal: they cannot be reactivated. After 90 days, the Gone worker sets <code>gone_at</code> and the URL serves a 410 page.</p>
  </div>
</template>

<style scoped>
.docset-management { padding: 1.5rem; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
.state { padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.85rem; }
.state-active { background: #d1fae5; color: #065f46; }
.state-deprecated { background: #fef3c7; color: #78350f; }
.state-archived { background: #e5e7eb; color: #374151; }
.actions button { margin-right: 0.5rem; padding: 0.25rem 0.5rem; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.hint { color: #4b5563; font-size: 0.9rem; margin-top: 1rem; }
.error { color: #dc2626; }
</style>
