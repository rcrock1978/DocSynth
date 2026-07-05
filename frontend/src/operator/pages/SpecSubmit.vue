<script setup lang="ts">
import { ref } from 'vue'
import { apiClient } from '../api/client'

const sourceKind = ref<'url' | 'file_upload' | 'github_repo'>('url')
const sourceRef = ref('')
const accessTokenRef = ref('')
const submitting = ref(false)
const error = ref<string | null>(null)
const createdId = ref<string | null>(null)

async function submit() {
  submitting.value = true
  error.value = null
  try {
    const res = await apiClient.post<{ specId: string }>('/api/v1/projects/current/specs', {
      sourceKind: sourceKind.value,
      sourceRef: sourceRef.value,
      accessTokenRef: accessTokenRef.value || null,
    })
    createdId.value = res.data.specId
    sourceRef.value = ''
    accessTokenRef.value = ''
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="spec-submit">
    <h1>Submit Spec</h1>
    <form @submit.prevent="submit">
      <label>
        Source
        <select v-model="sourceKind">
          <option value="url">Public URL</option>
          <option value="file_upload">File upload</option>
          <option value="github_repo">GitHub repository</option>
        </select>
      </label>
      <label>
        Source reference
        <input v-model="sourceRef" type="text" required
               :placeholder="sourceKind === 'github_repo' ? 'owner/repo@ref' : 'https://...'" />
      </label>
      <label v-if="sourceKind === 'github_repo'">
        Access token (Key Vault reference)
        <input v-model="accessTokenRef" type="text" placeholder="kv://..." />
      </label>
      <button type="submit" :disabled="submitting">{{ submitting ? 'Submitting…' : 'Submit' }}</button>
    </form>
    <p v-if="createdId" class="success">Spec created: <code>{{ createdId }}</code></p>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.spec-submit { padding: 1.5rem; max-width: 640px; }
form { display: flex; flex-direction: column; gap: 1rem; }
label { display: flex; flex-direction: column; gap: 0.25rem; }
input, select { padding: 0.5rem; border: 1px solid #d1d5db; border-radius: 4px; }
button { padding: 0.5rem 1rem; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.success { color: #059669; }
.error { color: #dc2626; }
</style>
