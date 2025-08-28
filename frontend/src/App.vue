<template>
  <div id="app">
    <main class="container vstack" style="gap:16px;">

      <section class="card vstack" aria-labelledby="upload-title">
        <div class="spread">
          <h2 id="upload-title">Last opp artikler</h2>
          <span class="badge">Opptil 3 JSON-filer</span>
        </div>

        <!-- Dra/slipp eller velg JSON-filer -->
        <ArticleDropzone @files-parsed="onFilesParsed" />

        <!-- liten preview tittel -->
        <ul v-if="articles.length" class="small" style="margin:8px 0 0 0; padding-left:18px;">
          <li v-for="(a, i) in previewTitles" :key="i"> {{ a }}</li>
        </ul>

        <!-- Kontroller -->
        <div class="hstack" style="gap:16px;flex-wrap:wrap; margin-top:8px;">
          <div style="flex:1;min-width:220px">
            <label for="num">Antall spørsmål</label>
            <input id="num" class="input" type="number" min="2" max="12" v-model.number="num"/>
          </div>
          <div style="flex:1;min-width:220px">
            <label for="diff">Vanskelighet</label>
            <select id="diff" class="input" v-model="difficulty">
              <option value="mixed">Blandet</option>
              <option value="easy">Lett</option>
              <option value="medium">Middels</option>
              <option value="hard">Vanskelig</option>
            </select>
          </div>
          <div style="flex:1;min-width:220px">
            <label for="loc">Språk</label>
            <select id="loc" class="input" v-model="locale">
              <option value="nb-NO">Norsk (bokmål)</option>
              <option value="nn-NO">Norsk (nynorsk)</option>
            </select>
          </div>
        </div>

        <div class="hstack" style="gap:10px; margin-top:8px; align-items:center">
          <button class="btn primary" :disabled="articles.length===0 || loading" @click="generate">
            <Spinner v-if="loading"/>
            <span v-else>Generer quiz</span>
          </button>
          <span class="small" v-if="articles.length===0">Legg til minst én artikkel først.</span>
        </div>
      </section>

      <!-- Resultat -->
      <section v-if="hasQuiz" class="card vstack">
        <div class="spread">
          <h2>{{ q.title }}</h2>
          <span class="badge">{{ q.questions.length }} spørsmål</span>
        </div>
        <!-- key => remount ved ny quiz så Runner resettes -->
        <QuizRunner :key="quizKey" :quiz="q" @finish="onFinish"/>
      </section>

      <div v-if="t" class="toast" :class="{success: toastType==='success', error: toastType==='error'}">
        {{ toastMessage }}
      </div>
    </main>
  </div>
</template>

<script setup>
import {ref, computed} from 'vue'
import ArticleDropzone from './components/ArticleDropzone.vue'
import Spinner from './components/Spinner.vue'
import {generateQuiz} from './lib/api.js'
import QuizRunner from './components/QuizRunner.vue'

const articles = ref([])
const num = ref(8)
const difficulty = ref('mixed')
const locale = ref('nb-NO')
const loading = ref(false)
const quiz = ref(null)
const toast = ref(null) // { type: 'success'|'error', message: string }

// ——— Normalisering: gjør resultatet robust (fyller manglende felt/alternativer) ———
function normalizeQuiz(q) {
  if (!q || typeof q !== 'object') return null
  if (!Array.isArray(q.questions)) q.questions = []
  if (!q.title) q.title = 'Nyhetsquiz'
  if (!q.locale) q.locale = 'nb-NO'

  q.questions = q.questions
      .filter(x => x && x.prompt && String(x.prompt).trim())
      .map((x, i) => {
        const id = x.id || `q-${i + 1}`
        let options = Array.isArray(x.options) ? x.options.filter(Boolean).map(s => String(s).trim()) : []
        while (options.length < 4) options.push('—')
        options = options.slice(0, 4)
        let answerIndex = Number.isInteger(x.answerIndex) ? x.answerIndex : 0
        if (answerIndex < 0 || answerIndex >= options.length) answerIndex = 0
        const difficulty = x.difficulty || 'medium'
        return {...x, id, options, answerIndex, difficulty}
      })

  return q
}

// ——— UI helpers ———
const hasQuiz = computed(() => quiz.value !== null)
const q = computed(() => quiz.value ?? {title: '', locale: 'nb-NO', meta: {createdAt: ''}, questions: []})
const quizKey = computed(() =>
    q.value?.meta?.createdAt || (q.value?.title || 'quiz') + '-' + (q.value?.questions?.length || 0)
)
const t = computed(() => toast.value)
const toastType = computed(() => toast.value?.type || 'success')
const toastMessage = computed(() => toast.value?.message || '')
const previewTitles = computed(() =>
    articles.value.map(a => a?.title?.value || a?.title || a?.id || 'Uten tittel')
)

function showToast(type, message) {
  toast.value = {type, message}
  window.setTimeout(() => toast.value = null, 4000)
}

function onFilesParsed(payload) {
  articles.value = payload
}

async function generate() {
  try {
    loading.value = true
    quiz.value = null
    const payload = {
      articles: articles.value,
      numQuestions: num.value,
      difficulty: difficulty.value,
      locale: locale.value
    }
    const data = await generateQuiz(payload)
    quiz.value = normalizeQuiz(data)
    // scroll til quizen
    requestAnimationFrame(() => window.scrollTo({top: document.body.scrollHeight / 3, behavior: 'smooth'}))
    showToast('success', 'Quiz generert!')
  } catch (err) {
    console.error(err)
    const msg = err?.response?.data?.message || err?.message || 'Ukjent feil'
    showToast('error', 'Noe gikk galt ved generering: ' + msg)
  } finally {
    loading.value = false
  }
}

function onFinish({score, total}) {
  showToast('success', `Ferdig! Du fikk ${score}/${total} riktig.`)
  // Valgfritt: nullstill quizen etterpå:
  quiz.value = null
}
</script>
