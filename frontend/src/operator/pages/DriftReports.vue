<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiClient } from '../api/client'
import type { DriftReport } from '../../shared/types'

const reports = ref<DriftReport[]>([])
const error = ref<string | null>(null)
const compatibilityFilter = ref<string>('')

async function load() {
  try {
    const res = await apiClient.get<DriftReport[]>('/api/v1/projects/current/drift')
    reports.value = res.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

onMounted(load)
</script>

<template>
  <div class="drift-list">
    <h1>Drift Reports</h1>
    <label>
      Filter by compatibility:
      <select v-model="compatibilityFilter">
        <option value="">All</option>
        <option value="breaking">Breaking only</option>
        <option value="non_breaking">Non-breaking only</option>
        <option value="informational">Informational only</option>
      </select>
    </label>
    <p v-if="error" class="error">{{ error }}</p>
    <table v-else>
      <thead>
        <tr>
          <th>Generated</th>
          <th>Trigger</th>
          <th>Added</th>
          <th>Removed</th>
          <th>Changed</th>
          <th>Breaking</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in reports" :key="r.id">
          <td><router-link :to="`/drift/${r.id}`">{{ new Date(r.generatedAt).toLocaleString() }}</router-link></td>
          <td>{{ r.trigger }}</td>
          <td>{{ r.summary.added }}</td>
          <td>{{ r.summary.removed }}</td>
          <td>{{ r.summary.changed }}</td>
          <td :class="{ breaking: r.summary.breaking > 0 }">{{ r.summary.breaking }}</td>
          <td>{{ r.notificationStatus }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.drift-list { padding: 1.5rem; }
label { display: block; margin: 1rem 0; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
.breaking { color: #dc2626; font-weight: 600; }
.error { color: #dc2626; }
</style>
