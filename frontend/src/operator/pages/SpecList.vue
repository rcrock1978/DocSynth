<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiClient } from '../api/client'
import type { ApiSpec } from '../../shared/types'

const specs = ref<ApiSpec[]>([])
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const res = await apiClient.get<ApiSpec[]>('/api/v1/projects/current/specs')
    specs.value = res.data
  } catch (e: any) {
    error.value = e.message
  }
})
</script>

<template>
  <div class="spec-list">
    <h1>API Specifications</h1>
    <router-link to="/specs/new">+ Submit new spec</router-link>
    <p v-if="error" class="error">{{ error }}</p>
    <table v-else>
      <thead>
        <tr>
          <th>Title</th>
          <th>Version</th>
          <th>OpenAPI</th>
          <th>Endpoints</th>
          <th>Schemas</th>
          <th>Parsed</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="s in specs" :key="s.id">
          <td><router-link :to="`/specs/${s.id}`">{{ s.title ?? '(untitled)' }}</router-link></td>
          <td>{{ s.specVersion ?? '—' }}</td>
          <td>{{ s.openapiVersion }}</td>
          <td>{{ s.endpointCount }}</td>
          <td>{{ s.schemaCount }}</td>
          <td>{{ new Date(s.parsedAt).toLocaleString() }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.spec-list { padding: 1.5rem; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th, td { padding: 0.5rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
.error { color: #dc2626; }
</style>
