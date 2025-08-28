<template>
  <div
      class="dropzone card vstack"
      :class="{ active: isDragging }"
      role="button"
      tabindex="0"
      aria-label="Last opp JSON-filer (maks 3)"
      @click="openPicker"
      @keydown.enter.prevent="openPicker"
      @keydown.space.prevent="openPicker"
      @dragenter.prevent="isDragging = true"
      @dragover.prevent
      @dragleave.prevent="isDragging = false"
      @drop.prevent="onDrop"
  >
    <input
        ref="fileInput"
        type="file"
        accept="application/json,.json"
        multiple
        class="visually-hidden"
        @change="onSelect"
    />

    <div class="dz-header center vstack">


      <h3 class="dz-title">Dra & slipp JSON-filer</h3>
      <p class="dz-sub muted">…eller klikk for å velge. Maks 3 filer.</p>

      <div class="dz-meta small">
        Valgt: <strong>{{ files.length }}</strong> / 3
        <span v-if="note" class="note">{{ note }}</span>
      </div>
    </div>

    <!-- Liste over valgte filer -->
    <ul v-if="files.length" class="filelist">
      <li v-for="it in files" :key="it.name" class="fileitem">
        <div class="fileinfo">
          <div class="filebadge">{{ ext(it.name) }}</div>
          <div class="filetext">
            <div class="filename" :title="it.name">{{ it.name }}</div>
            <div class="filesub small muted">{{ pretty(it.size) }}</div>
          </div>
        </div>

        <div class="filestatus">
          <span v-if="it.error" class="chip chip-err" :title="it.error">Ugyldig</span>
          <span v-else-if="it.parsed" class="chip chip-ok">Klar</span>
          <span v-else class="chip">Leser…</span>
          <button class="btn ghost danger sm" @click.stop="remove(it.name)">Fjern</button>
        </div>
      </li>
    </ul>

    <div v-if="files.length" class="dz-actions">
      <button class="btn sm" @click.stop="openPicker">Legg til flere</button>
      <button class="btn ghost sm" @click.stop="clearAll">Tøm alt</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['files-parsed'])

const MAX_FILES = 3

const fileInput = ref(null)
const isDragging = ref(false)
const files = ref([]) // [{ file, name, size, parsed, error }]
const note = ref('')

// Åpne filvelger
function openPicker() {
  fileInput.value?.click()
}

function onSelect(e){
  if (e?.target?.files) pushFiles(e.target.files)
  // reset input så samme fil kan velges to ganger etter hverandre
  e.target.value = ''
}

function onDrop(e){
  isDragging.value = false
  if (e.dataTransfer?.files) pushFiles(e.dataTransfer.files)
}

// Legg til filer (dedupe på navn, cap = MAX_FILES)
function pushFiles(list){
  note.value = ''
  const incoming = Array.from(list)
  const merged = [...files.value]

  for (const f of incoming){
    if (merged.length >= MAX_FILES) {
      note.value = `Maks ${MAX_FILES} filer – overskytende ignorert.`
      break
    }
    // bytt ut eksisterende ved samme navn
    const ix = merged.findIndex(x => x.name === f.name)
    const item = { file: f, name: f.name, size: f.size, parsed: null, error: null }
    if (ix >= 0) merged.splice(ix, 1, item)
    else merged.push(item)
  }

  files.value = merged
  parseAll()
}

// Fjern én
function remove(name){
  files.value = files.value.filter(it => it.name !== name)
  parseAll()
}

// Tøm alt
function clearAll(){
  files.value = []
  emit('files-parsed', [])
}

// Parse alle (emit kun de som er gyldige)
async function parseAll(){
  const parsedOut = []
  for (const it of files.value){
    try{
      const text = await it.file.text()
      const obj = JSON.parse(text)
      it.parsed = obj
      it.error = null
      parsedOut.push(obj)
    }catch(err){
      it.parsed = null
      it.error = 'Kunne ikke lese gyldig JSON'
      // (lar den bli i lista, men emitter ikke den)
    }
  }
  emit('files-parsed', parsedOut)
}

// UI helpers
function ext(name){
  const m = name.toLowerCase().match(/\.([a-z0-9]+)$/)
  return m ? m[1] : 'fil'
}
function pretty(bytes){
  if (bytes < 1024) return bytes + ' B'
  const kb = bytes / 1024
  if (kb < 1024) return kb.toFixed(1) + ' kB'
  return (kb/1024).toFixed(2) + ' MB'
}
</script>

<style scoped>
/* Tilpasser seg dine eksisterende .card/.vstack/.btn-stiler, men gjør feltet mer “premium” */
.dropzone {
  background: transparent;
  padding: 16px;
  border-style: dashed;
  border-width: 2px;
  border-color: var(--border, rgba(255,255,255,.2));
  border-radius: 16px;
  transition: border-color .15s ease, background .15s ease, box-shadow .2s ease;
  cursor: pointer;
  outline: none;
}
.dropzone:hover,
.dropzone:focus-visible {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96,165,250,.2);
}
.dropzone.active {
  border-color: #34d399;
  background: linear-gradient(180deg, rgba(52,211,153,.08), rgba(52,211,153,.02));
}

.visually-hidden {
  position: absolute!important;
  height: 1px; width: 1px;
  overflow: hidden; clip: rect(1px,1px,1px,1px);
  white-space: nowrap;
}

.dz-header { gap: 6px; }
.dz-icon {
  width: 42px; height: 42px; opacity: .9;
  fill: currentColor;
}
.dz-title { margin: 6px 0 0; }
.dz-sub { margin: 0 0 6px; }
.dz-meta { margin-top: 2px; }
.note { margin-left: 8px; color: #facc15; }

.filelist {
  list-style: none; margin: 12px 0 0; padding: 0;
  display: grid; gap: 8px;
}
.fileitem {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--border, rgba(255,255,255,.15));
  border-radius: 12px;
  background: rgba(255,255,255,.03);
}
.fileinfo { display: flex; align-items: center; gap: 10px; min-width: 0; }
.filebadge {
  width: 50px; height: 36px; border-radius: 10px;
  display: grid; place-items: center;
  background: #0b1220; border: 1px solid var(--border, rgba(255,255,255,.15));
  font-weight: 700; text-transform: uppercase;
}
.filetext { min-width: 0; }
.filename {
  font-weight: 600; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; max-width: 52vw;
}
.filesub { margin-top: 2px; }

.filestatus { display: flex; align-items: center; gap: 8px; }
.chip {
  font-size: 12px; padding: 3px 8px; border-radius: 999px;
  background: rgba(255,255,255,.08); border: 1px solid rgba(255,255,255,.12);
}
.chip-ok { background: rgba(34,197,94,.18); border-color: rgba(34,197,94,.25); }
.chip-err { background: rgba(239,68,68,.18); border-color: rgba(239,68,68,.25); }

.dz-actions {
  margin-top: 10px; display: flex; gap: 8px; justify-content: flex-end;
}

.btn.sm { padding: 6px 10px; font-size: 13px; border-radius: 10px; }
.btn.ghost { background: transparent; border: 1px solid var(--border, rgba(255,255,255,.2)); }
</style>
