<script setup lang="ts">
import { ref } from 'vue'
import { apiClient } from '../../operator/api/client'

const props = defineProps<{
  specId: string
  method: string
  path: string
  targetHost: string
  targetPort?: number
}>()

const props_ = ref<Record<string, string>>({})
const headers = ref<Record<string, string>>({})
const body = ref('')
const executing = ref(false)
const response = ref<{ status: number; body: string; durationMs: number; requestId: string } | null>(null)
const error = ref<string | null>(null)

async function execute() {
  executing.value = true
  error.value = null
  response.value = null
  try {
    // In production, the proxyToken is fetched from a /api/v1/proxy/token
    // endpoint that mints a short-lived HMAC token bound to (user, tenant,
    // target host). For v1 we use a stub token.
    const tokenResp = await apiClient.post<{ token: string }>('/api/v1/proxy/token', {
      targetHost: props.targetHost
    })
    const res = await apiClient.post<{ status: number; body: string; durationMs: number; requestId: string }>(
      `/api/v1/proxy/try?projectId=current&specId=${props.specId}`,
      {
        proxyToken: tokenResp.data.token,
        targetHost: props.targetHost,
        targetPort: props.targetPort ?? 443,
        method: props.method,
        path: props.path,
        body: body.value || null,
      }
    )
    response.value = res.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  } finally {
    executing.value = false
  }
}
</script>

<template>
  <div class="try-it-console">
    <h3>Try it</h3>
    <p class="target">Target: <code>{{ targetHost }}</code></p>
    <details>
      <summary>Headers</summary>
      <textarea v-model="JSON.stringify(headers)" rows="3" placeholder='{"X-Tenant": "acme"}'></textarea>
    </details>
    <details v-if="['POST','PUT','PATCH','DELETE'].includes(method)">
      <summary>Body</summary>
      <textarea v-model="body" rows="6" placeholder='{"key": "value"}'></textarea>
    </details>
    <button @click="execute" :disabled="executing">{{ executing ? 'Sending…' : 'Send request' }}</button>
    <div v-if="response" class="response">
      <p><strong>Status:</strong> {{ response.status }} ({{ response.durationMs }} ms)</p>
      <p><strong>Request ID:</strong> <code>{{ response.requestId }}</code></p>
      <pre><code>{{ response.body }}</code></pre>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.try-it-console { margin: 1rem 0; padding: 1rem; border: 1px solid #e5e7eb; border-radius: 6px; }
.target { color: #4b5563; font-size: 0.9rem; }
textarea { width: 100%; font-family: monospace; }
button { margin-top: 0.5rem; padding: 0.5rem 1rem; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
pre { background: #0f172a; color: #e2e8f0; padding: 1rem; border-radius: 6px; overflow-x: auto; }
.error { color: #dc2626; }
</style>
