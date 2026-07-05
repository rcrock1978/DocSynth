<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { apiClient } from '../api/client'
import type { DriftReport, DriftItem } from '../../shared/types'

const props = defineProps<{ id: string }>()

const report = ref<DriftReport | null>(null)
const items = ref<DriftItem[]>([])
const error = ref<string | null>(null)
const filter = ref<'all' | 'added' | 'removed' | 'changed' | 'breaking'>('all')

async function load() {
  try {
    const [r, i] = await Promise.all([
      apiClient.get<DriftReport>(`/api/v1/projects/current/drift/${props.id}`),
      apiClient.get<DriftItem[]>(`/api/v1/projects/current/drift/${props.id}/items`)
    ])
    report.value = r.data
    items.value = i.data
  } catch (e: any) {
    error.value = e.response?.data?.detail ?? e.message
  }
}

const filteredItems = (kind: string) =>
  items.value.filter(i => i.changeKind === kind)

const breakingItems = () => items.value.filter(i => i.compatibility === 'breaking')

onMounted(load)
</script>

<template>
  <div class="drift-detail">
    <router-link to="/drift">← All reports</router-link>
    <h1 v-if="report">Drift Report {{ new Date(report.generatedAt).toLocaleString() }}</h1>
    <dl v-if="report">
      <dt>Trigger</dt><dd>{{ report.trigger }}</dd>
      <dt>Added</dt><dd>{{ report.summary.added }}</dd>
      <dt>Removed</dt><dd>{{ report.summary.removed }}</dd>
      <dt>Changed</dt><dd>{{ report.summary.changed }}</dd>
      <dt>Breaking</dt><dd :class="{ breaking: report.summary.breaking > 0 }">{{ report.summary.breaking }}</dd>
      <dt>Notification</dt><dd>{{ report.notificationStatus }}</dd>
    </dl>

    <section v-if="breakingItems().length">
      <h2>Breaking changes</h2>
      <ul>
        <li v-for="i in breakingItems()" :key="i.id" class="breaking">
          <code>{{ i.targetPath }}</code> — {{ i.message }}
        </li>
      </ul>
    </section>

    <section v-if="filteredItems('added').length">
      <h2>Added</h2>
      <ul>
        <li v-for="i in filteredItems('added')" :key="i.id">
          <code>{{ i.targetPath }}</code> — {{ i.message }}
        </li>
      </ul>
    </section>

    <section v-if="filteredItems('removed').length">
      <h2>Removed</h2>
      <ul>
        <li v-for="i in filteredItems('removed')" :key="i.id">
          <code>{{ i.targetPath }}</code> — {{ i.message }}
        </li>
      </ul>
    </section>

    <section v-if="filteredItems('changed').length">
      <h2>Changed</h2>
      <ul>
        <li v-for="i in filteredItems('changed')" :key="i.id">
          <code>{{ i.targetPath }}</code> — <em>{{ i.compatibility }}</em> — {{ i.message }}
        </li>
      </ul>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<style scoped>
.drift-detail { padding: 1.5rem; max-width: 960px; }
dl { display: grid; grid-template-columns: max-content 1fr; gap: 0.5rem 1rem; margin: 1rem 0; }
dt { font-weight: 600; color: #4b5563; }
.breaking { color: #dc2626; }
ul { padding-left: 1.5rem; }
li { margin: 0.25rem 0; }
code { background: #f3f4f6; padding: 0.1rem 0.25rem; border-radius: 3px; }
.error { color: #dc2626; }
</style>
