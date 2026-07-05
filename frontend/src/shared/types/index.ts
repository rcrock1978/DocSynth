export interface ApiSpec {
  id: string
  projectId: string
  title?: string
  specVersion?: string
  openapiVersion: string
  endpointCount: number
  schemaCount: number
  parsedAt: string
}

export interface Endpoint {
  id: string
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS' | 'TRACE'
  path: string
  summary?: string
  description?: string
  tags: string[]
  deprecated: boolean
}

export interface DocSet {
  id: string
  projectId: string
  displayVersion: string
  state: 'active' | 'deprecated' | 'archived'
  tryItEnabled: boolean
  generatedAt: string
  deprecatedAt?: string
  archivedAt?: string
  goneAt?: string
}

export interface DriftReport {
  id: string
  projectId: string
  generatedAt: string
  summary: {
    added: number
    removed: number
    changed: number
    breaking: number
  }
  notificationStatus: 'pending' | 'sent' | 'failed' | 'skipped'
}

export interface DriftItem {
  id: string
  changeKind: 'added' | 'removed' | 'changed'
  compatibility: 'breaking' | 'non_breaking' | 'informational'
  targetKind: 'endpoint' | 'schema' | 'parameter' | 'response' | 'security'
  targetPath: string
  message: string
}
