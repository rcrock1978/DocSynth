<script setup lang="ts">
import { computed } from 'vue'
import type { EndpointReferenceProps } from '../../shared/types'

const props = withDefaults(defineProps<{
  language: string
  code: string
  showLineNumbers?: boolean
}>(), { showLineNumbers: true })

const lines = computed(() => props.code.split('\n'))
</script>

<template>
  <div class="code-example-block">
    <header>
      <span class="language">{{ language }}</span>
      <button @click="navigator.clipboard.writeText(code)">Copy</button>
    </header>
    <pre><code><span
      v-for="(line, i) in lines"
      :key="i"
      class="line"
    ><span v-if="showLineNumbers" class="ln">{{ i + 1 }}</span>{{ line }}
</span></code></pre>
  </div>
</template>

<style scoped>
.code-example-block { margin: 0.5rem 0; border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }
header { display: flex; justify-content: space-between; align-items: center; padding: 0.25rem 0.75rem; background: #f9fafb; }
.language { font-family: monospace; font-size: 0.85rem; color: #4b5563; }
button { background: transparent; border: 1px solid #d1d5db; border-radius: 4px; padding: 0.1rem 0.5rem; cursor: pointer; }
pre { background: #0f172a; color: #e2e8f0; padding: 0.75rem 1rem; margin: 0; overflow-x: auto; }
.ln { display: inline-block; min-width: 2rem; padding-right: 0.5rem; color: #64748b; text-align: right; }
</style>
