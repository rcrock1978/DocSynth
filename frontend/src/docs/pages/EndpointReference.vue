<script setup lang="ts">
import { computed } from 'vue'
import { useManifest } from '../composables/useManifest'
import VersionSelector from '../components/VersionSelector.vue'
import DeprecationBanner from '../components/DeprecationBanner.vue'

const props = defineProps<{
  method: string
  path: string
  summary?: string
  description?: string
  parameters?: Array<{ name: string; in: string; required: boolean; schema: any }>
  requestBody?: any
  responses?: Record<string, { description: string; content?: any }>
  codeExamples?: Record<string, string>
}>()

const manifest = useManifest()
const isDeprecated = computed(() => manifest.deprecatedPaths.includes(`${props.method} ${props.path}`))
</script>

<template>
  <article class="endpoint-reference">
    <DeprecationBanner v-if="isDeprecated" :replacement="manifest.replacementVersion" />
    <h1><span class="method">{{ method }}</span> {{ path }}</h1>
    <p v-if="summary" class="summary">{{ summary }}</p>
    <p v-if="description" class="description">{{ description }}</p>

    <section v-if="parameters && parameters.length">
      <h2>Parameters</h2>
      <table>
        <thead>
          <tr><th>Name</th><th>In</th><th>Required</th><th>Schema</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in parameters" :key="p.name">
            <td><code>{{ p.name }}</code></td>
            <td>{{ p.in }}</td>
            <td>{{ p.required ? 'yes' : 'no' }}</td>
            <td><code>{{ JSON.stringify(p.schema) }}</code></td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="requestBody">
      <h2>Request body</h2>
      <pre><code>{{ JSON.stringify(requestBody, null, 2) }}</code></pre>
    </section>

    <section v-if="responses">
      <h2>Responses</h2>
      <div v-for="(resp, status) in responses" :key="status" class="response">
        <h3>{{ status }} — {{ resp.description }}</h3>
        <pre v-if="resp.content"><code>{{ JSON.stringify(resp.content, null, 2) }}</code></pre>
      </div>
    </section>

    <section v-if="codeExamples">
      <h2>Code examples</h2>
      <VersionSelector />
      <div v-for="(code, lang) in codeExamples" :key="lang" class="code-example">
        <h3>{{ lang }}</h3>
        <pre><code>{{ code }}</code></pre>
      </div>
    </section>
  </article>
</template>

<style scoped>
.endpoint-reference { padding: 1.5rem; max-width: 960px; margin: 0 auto; }
.method {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  background: #2563eb;
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  margin-right: 0.5rem;
}
.summary { color: #4b5563; font-size: 1.1rem; }
.description { white-space: pre-wrap; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.4rem; border-bottom: 1px solid #e5e7eb; text-align: left; }
pre { background: #0f172a; color: #e2e8f0; padding: 1rem; border-radius: 6px; overflow-x: auto; }
</style>
