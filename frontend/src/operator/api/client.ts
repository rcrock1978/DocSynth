import axios, { type AxiosInstance } from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL as string

export const apiClient: AxiosInstance = axios.create({
  baseURL,
  withCredentials: false,
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Problem Details (RFC 9457) is the wire format. Surface traceId for
    // cross-service correlation (FR-013).
    if (error.response?.data?.traceId) {
      // eslint-disable-next-line no-console
      console.error('API error traceId:', error.response.data.traceId)
    }
    return Promise.reject(error)
  }
)
