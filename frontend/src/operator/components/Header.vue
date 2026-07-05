<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '../composables/useAuth'

const { user, signIn, signOut } = useAuth()
const isMenuOpen = ref(false)
</script>

<template>
  <header class="operator-header">
    <div class="brand">DocSynth Operator</div>
    <nav v-if="user">
      <router-link to="/specs">Specs</router-link>
      <router-link to="/docsets">DocSets</router-link>
      <router-link to="/drift">Drift</router-link>
      <router-link to="/tryit">Try It</router-link>
    </nav>
    <div class="user">
      <button v-if="!user" @click="signIn">Sign in</button>
      <button v-else @click="signOut">{{ user.email }} (sign out)</button>
    </div>
  </header>
</template>

<style scoped>
.operator-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}
.brand {
  font-weight: 700;
  font-size: 1.1rem;
}
nav a {
  margin: 0 0.75rem;
  text-decoration: none;
  color: #374151;
}
nav a.router-link-active {
  color: #2563eb;
  font-weight: 600;
}
</style>
