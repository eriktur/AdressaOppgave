<template>
  <div
      class="card vstack"
      :style="{background:'transparent', padding:'16px', borderStyle:'dashed'}"
      @dragover.prevent
      @drop.prevent="onDrop"
  >
    <div class="center vstack">
      <p class="small">Dra og slipp JSON-filer her, eller velg manuelt</p>
      <input ref="fileInput" type="file" accept="application/json" multiple class="input" @change="onSelect"/>
      <p class="small">Valgt: {{ files.length }} / 3</p>
    </div>
    <ul v-if="files.length" style="margin:0;padding-left:18px">
      <li v-for="f in files" :key="f.name" class="hstack spread">
        <span>{{ f.name }}</span>
        <button class="btn danger" @click="remove(f.name)">Fjern</button>
      </li>
    </ul>
  </div>
</template>


<script setup>
import { ref } from 'vue'


const emit = defineEmits(['files-parsed'])
const fileInput = ref(null)
const files = ref([])


function pushFiles(list){
  const arr = Array.from(list)
  const merged = [...files.value, ...arr].slice(0,3)
  files.value = merged
  parseAll()
}


function onSelect(e){
  const input = e.target
  if (input.files) pushFiles(input.files)
}


function onDrop(e){
  if (e.dataTransfer?.files) pushFiles(e.dataTransfer.files)
}


function remove(name){
  files.value = files.value.filter(f => f.name !== name)
  parseAll()
}


async function parseAll(){
  const parsed = []
  for (const f of files.value){
    try{
      const text = await f.text()
      parsed.push(JSON.parse(text))
    }catch(err){
      console.warn('Ugyldig JSON i fil:', f.name)
    }
  }
  emit('files-parsed', parsed)
}
</script>