<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiClient } from '../api/client'
import type { DocSet } from '../../shared/types'

const docsets = ref<DocSet[]>([])
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const res = await apiClient.get<DocSet[]>('/api/v1/projects/current/docsets')
    docsets.value = res.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
})
</script>

<template>
  <div class="docset-list">
    <h1>DocSets</h1>
    <p v-if="error" class="error">{{ error }}</p>
    <table v-else>
      <thead>
        <tr>
          <th>Version</th>
          <th>State</th>
          <th>Try It</th>
          <th>Generated</th>
          <th>Published</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in docsets" :key="d.id">
          <td><code>{{ d.displayVersion }}</code></td>
          <td><span :class="`state state-${d.state}`">{{ d.state }}</span></td>
          <td>{{ d.tryItEnabled ? 'yes' : 'no' }}</td>
          <td>{{ new Date(d.generatedAt).toLocaleString() }}</td>
          <td>{{ d.publishedAt ? new Date(d.publishedAt).toLocaleString() : '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.docset-list { padding: 1.5rem; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th, td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
.state { padding: 0.15rem 0.5rem; border-radius: 4px; font-size: 0.85rem; }
.state-active { background: #d1fae5; color: #065f46; }
.state-deprecated { background: #fef3c7; color: #78350f; }
.state-archived { background: #e5e7eb; color: #374151; }
.error { color: #dc2626; }
</style>
