export interface EndpointReferenceProps {
  method: string
  path: string
  summary?: string
  description?: string
  parameters?: Array<{ name: string; in: string; required: boolean; schema: any }>
  requestBody?: any
  responses?: Record<string, { description: string; content?: any }>
  codeExamples?: Record<string, string>
}
