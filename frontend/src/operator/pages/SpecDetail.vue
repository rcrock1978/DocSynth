<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiClient } from '../api/client'
import type { ApiSpec, Endpoint } from '../../shared/types'

const props = defineProps<{ id: string }>()

const spec = ref<ApiSpec | null>(null)
const endpoints = ref<Endpoint[]>([])
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const res = await apiClient.get<ApiSpec>(`/api/v1/projects/current/specs/${props.id}`)
    spec.value = res.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
})
</script>

<template>
  <div class="spec-detail">
    <router-link to="/specs">← All specs</router-link>
    <h1 v-if="spec">{{ spec.title ?? '(untitled)' }}</h1>
    <dl v-if="spec">
      <dt>Version</dt><dd>{{ spec.specVersion ?? '—' }}</dd>
      <dt>OpenAPI</dt><dd>{{ spec.openapiVersion }}</dd>
      <dt>Endpoints</dt><dd>{{ spec.endpointCount }}</dd>
      <dt>Schemas</dt><dd>{{ spec.schemaCount }}</dd>
      <dt>Parsed</dt><dd>{{ new Date(spec.parsedAt).toLocaleString() }}</dd>
    </dl>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.spec-detail { padding: 1.5rem; max-width: 720px; }
dl { display: grid; grid-template-columns: max-content 1fr; gap: 0.5rem 1rem; }
dt { font-weight: 600; color: #4b5563; }
.error { color: #dc2626; }
</style>
