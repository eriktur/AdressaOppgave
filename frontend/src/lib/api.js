import axios from 'axios'
// Dev: proxes til 8080, Prod: kan peke på VITE_API_BASE senere
const baseURL = import.meta.env.DEV ? '' : (import.meta.env.VITE_API_BASE || '')
export const api = axios.create({ baseURL })

export async function generateQuiz(payload) {
    const { data } = await api.post('/api/quiz/generate', payload)
    return data
}
