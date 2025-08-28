// src/lib/normalizeQuiz.js
export function normalizeQuiz(q) {
    if (!q || typeof q !== 'object') return null
    if (!Array.isArray(q.questions)) q.questions = []
    if (!q.title) q.title = 'Nyhetsquiz'
    if (!q.locale) q.locale = 'nb-NO'

    q.questions = q.questions
        .filter(x => x && x.prompt && String(x.prompt).trim())
        .map((x, i) => {
            const id = x.id || `q-${i+1}`
            let options = Array.isArray(x.options) ? x.options.filter(Boolean).map(s => String(s).trim()) : []
            while (options.length < 4) options.push('—')
            options = options.slice(0, 4)
            let answerIndex = Number.isInteger(x.answerIndex) ? x.answerIndex : 0
            if (answerIndex < 0 || answerIndex >= options.length) answerIndex = 0
            return { ...x, id, options, answerIndex }
        })

    return q
}
