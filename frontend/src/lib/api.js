import axios from 'axios'


const baseURL = import.meta.env.DEV ? '' : (import.meta.env.VITE_API_BASE || '')
export const api = axios.create({ baseURL })

// send a POST request to /api/quiz/generate with the given payload
export async function generateQuiz(payload) {
    const { data } = await api.post('/api/quiz/generate', payload)
    return data
}
