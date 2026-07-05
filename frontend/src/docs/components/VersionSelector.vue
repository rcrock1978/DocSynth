<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { apiClient } from '../../operator/api/client'

export interface Manifest {
  versions: Array<{ displayVersion: string; state: 'active' | 'deprecated' | 'archived'; goneAt?: string }>
  current: string
  deprecatedPaths: string[]
  replacementVersion?: string
}

const manifest = ref<Manifest | null>(null)
export function useManifest() {
  return {
    deprecatedPaths: manifest.value?.deprecatedPaths ?? [],
    replacementVersion: manifest.value?.replacementVersion,
  }
}

onMounted(async () => {
  try {
    const res = await apiClient.get<Manifest>('/api/v1/manifest.json')
    manifest.value = res.data
  } catch {
    // Manifest is optional in SSG'd DocSets; failure to fetch is non-fatal.
  }
})
</script>

<template>
  <div class="version-selector">
    <label>
      Version
      <select v-if="manifest">
        <option v-for="v in manifest.versions" :key="v.displayVersion" :value="v.displayVersion">
          {{ v.displayVersion }} ({{ v.state }})
        </option>
      </select>
    </label>
  </div>
</template>

<style scoped>
.version-selector { margin: 0.5rem 0; }
label { display: inline-flex; align-items: center; gap: 0.5rem; }
select { padding: 0.25rem 0.5rem; }
</style>
