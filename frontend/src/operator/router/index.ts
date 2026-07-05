import { createRouter, createWebHistory } from 'vue-router'
import SpecList from '../pages/SpecList.vue'
import SpecSubmit from '../pages/SpecSubmit.vue'
import SpecDetail from '../pages/SpecDetail.vue'
import DocSetList from '../pages/DocSetList.vue'
import DriftList from '../pages/DriftList.vue'
import TryItSettings from '../pages/TryItSettings.vue'
import { authCallback } from '../pages/AuthCallback.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/specs' },
    { path: '/specs', component: SpecList },
    { path: '/specs/new', component: SpecSubmit },
    { path: '/specs/:id', component: SpecDetail, props: true },
    { path: '/docsets', component: DocSetList },
    { path: '/drift', component: DriftList },
    { path: '/tryit', component: TryItSettings },
    { path: '/auth/callback', component: authCallback },
  ],
})
