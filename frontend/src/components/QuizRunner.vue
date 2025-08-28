<template>
  <div class="runner">

    <section v-if="phase==='intro'" class="runner-card vstack center">
      <h1 class="runner-title">{{ quiz.title }}</h1>
      <p class="muted">{{ quiz.locale }} • {{ total }} spørsmål</p>
      <button class="btn primary big" @click="start">Start quiz</button>
    </section>


    <section v-else-if="phase==='playing'" class="runner-card vstack">
      <header class="runner-header spread">
        <div class="hstack" style="gap:10px;align-items:center">
          <div class="dots">
            <span v-for="i in total" :key="i" :class="['dot', {active: i-1===idx, done: i-1<idx}]"></span>
          </div>
          <span class="muted">Spørsmål {{ idx+1 }} / {{ total }}</span>
        </div>
        <div class="score muted">Riktig: {{ correct }}/{{ idx }}</div>
      </header>

      <h2 class="runner-prompt">{{ current.prompt }}</h2>
      <p class="runner-source small" v-if="current.sourceTitle">
        Kilde: <span class="muted">{{ current.sourceTitle }}</span>
      </p>

      <ul class="options" role="listbox" aria-label="Svaralternativer">
        <li v-for="(opt,i) in current.options" :key="i">
          <button
              :class="optionClass(i)"
              role="option"
              :aria-selected="selected===i"
              @click="select(i)"
              :disabled="locked"
          >
            <span class="badge kbd">{{ labels[i] }}</span>
            <span class="opt-text">{{ opt }}</span>
          </button>
        </li>
      </ul>

      <footer class="runner-footer spread">
        <span class="muted small" v-if="!locked">Tips: bruk tastene 1–4</span>
        <div class="hstack" style="gap:8px">
          <button class="btn" v-if="!locked" :disabled="selected===-1" @click="lockIn">Lås svaret</button>
          <button class="btn primary" v-else @click="next">{{ idx+1===total ? 'Se resultat' : 'Neste' }}</button>
        </div>
      </footer>

      <transition name="fade">
        <div v-if="locked" class="explain">
          <p v-if="isCorrect" class="ok">Riktig!</p>
          <p v-else class="err">Feil. Riktig svar var <strong>{{ labels[current.answerIndex] }}</strong>.</p>
          <p class="muted small" v-if="current.explanation">{{ current.explanation }}</p>
        </div>
      </transition>
    </section>


    <section v-else class="runner-card vstack center">
      <h2 class="runner-title">Resultat</h2>
      <p class="bigscore">{{ correct }} / {{ total }}</p>
      <p class="muted" v-if="correct===total">Perfekt!</p>
      <p class="muted" v-else-if="correct/total>=0.7">Sterkt levert</p>
      <p class="muted" v-else>Godt forsøk – prøv igjen </p>
      <div class="hstack center" style="gap:10px;margin-top:8px">
        <button class="btn" @click="restart">Spill igjen</button>
        <button class="btn primary" @click="finish">Ferdig</button>

      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'


const props = defineProps({ quiz: { type: Object, required: true } })
const emit = defineEmits(['finish'])
function finish(){
  emit('finish', { score: correct.value, total: total.value })
}

const phase = ref('intro')
const idx = ref(0)
const selected = ref(-1)
const locked = ref(false)
const correct = ref(0)
const labels = ['1','2','3','4']


const total = computed(() => (props.quiz?.questions?.length ?? 0))
const current = computed(() => props.quiz.questions[idx.value] || { options: [] })
const isCorrect = computed(() => selected.value === current.value.answerIndex)


function start(){ phase.value='playing'; idx.value=0; selected.value=-1; locked.value=false; correct.value=0 }
function select(i){ if (!locked.value) selected.value = i }
function lockIn(){
  if (selected.value === -1) return
  locked.value = true
  if (isCorrect.value) correct.value++
}
function next(){
  if (idx.value+1 < total.value){
    idx.value++
    selected.value = -1
    locked.value = false
  } else {
    phase.value = 'done'
  }
}
function restart(){ phase.value='intro'; idx.value=0; selected.value=-1; locked.value=false; correct.value=0 }

function onKey(e){
  if (phase.value !== 'playing') return
  if (e.key>='1' && e.key<='4'){ select(Number(e.key)-1) }
  if (e.key==='Enter'){ locked.value ? next() : lockIn() }
}
onMounted(()=> window.addEventListener('keydown', onKey))
onBeforeUnmount(()=> window.removeEventListener('keydown', onKey))

function optionClass(i){
  return [
    'opt',
    `opt-${i}`,
    { selected: selected.value===i, locked: locked.value,
      ok: locked.value && i===current.value.answerIndex,
      wrong: locked.value && selected.value===i && i!==current.value.answerIndex
    }
  ]
}
</script>

<style scoped>
.runner { width:100%; }
.runner-card{
  background: linear-gradient(180deg, rgba(255,255,255,.05), rgba(255,255,255,.02));
  border:1px solid var(--border);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,.25);
}
.center{ text-align:center; align-items:center; }
.runner-title{ margin:0 0 6px 0; }
.runner-header{ margin-bottom: 8px; }
.runner-prompt{ margin: 8px 0 6px 0; }
.runner-source{ margin: 0 0 12px 0; }
.big{ font-size:18px; padding:12px 18px; border-radius:14px; }
.bigscore{ font-size:48px; margin:6px 0 12px; }

.options { list-style:none; padding:0; margin: 8px 0 0; display:grid; gap:12px; }
@media (min-width: 720px){ .options { grid-template-columns: 1fr 1fr; } }

.opt{
  width:100%; text-align:left;
  padding: 14px; border-radius: 16px;
  border: 2px solid transparent;
  display:flex; gap:10px; align-items:center;
  font-weight:600; cursor:pointer;
  transition: transform .08s ease, box-shadow .2s ease, border-color .2s ease, background .2s ease;
}
.opt .opt-text{ flex:1; }
.opt:hover{ transform: translateY(-1px); box-shadow: 0 8px 16px rgba(0,0,0,.25); }
.opt.selected{ border-color: rgba(255,255,255,.6); }
.opt.locked{ cursor:default; }

.opt-0{ background:#2ecc71; }
.opt-1{ background:#3498db; }
.opt-2{ background:#f1c40f; }
.opt-3{ background:#e74c3c; }
.opt.ok{ outline: 3px solid #c7ffb3; }
.opt.wrong{ filter: grayscale(.25) brightness(.85); }

.dots{ display:flex; gap:6px; }
.dot{ width:10px; height:10px; border-radius:50%; background: #1f2a44; border:1px solid var(--border); }
.dot.active{ background:#60a5fa; }
.dot.done{ background:#34d399; }

.explain{ margin-top:12px; padding:12px; border:1px solid var(--border); border-radius:12px; background:#0b1220; }
.ok{ color:#9ae6b4; font-weight:700; margin:0 0 6px; }
.err{ color:#fecaca; font-weight:700; margin:0 0 6px; }

.fade-enter-active, .fade-leave-active { transition: opacity .15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
