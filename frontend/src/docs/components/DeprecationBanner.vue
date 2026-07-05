<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { apiClient } from '../api/client'

const props = defineProps<{ deprecatedAt?: string; replacementVersion?: string }>()

const isDeprecated = computed(() => !!props.deprecatedAt)
</script>

<template>
  <div v-if="isDeprecated" class="deprecation-banner" role="alert" aria-live="polite">
    <strong>This version of the documentation is deprecated.</strong>
    <span v-if="replacementVersion">
      See version <router-link :to="`/v${replacementVersion}/`">{{ replacementVersion }}</router-link>
      for the current API.
    </span>
  </div>
</template>

<style scoped>
.deprecation-banner {
  background: #fef3c7;
  border: 1px solid #f59e0b;
  border-radius: 6px;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
  color: #78350f;
}
.deprecation-banner a { color: #78350f; text-decoration: underline; }
</style>
