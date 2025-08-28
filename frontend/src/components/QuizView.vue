<template>
  <div class="vstack" style="gap:12px">
    <QuestionBlock
        v-for="(q, idx) in quiz.questions"
        :key="q.id"
        :q="q" :index="idx" :submitted="submitted" v-model="answers"
    />


    <div class="spread" style="align-items:center">
      <button v-if="!submitted" class="btn primary" @click="submit">Lever</button>
      <div v-else class="hstack" style="gap:10px;align-items:center">
        <strong>Poeng: {{ score }} / {{ quiz.questions.length }}</strong>
        <button class="btn" @click="reset">Spill igjen</button>
      </div>
    </div>
  </div>
</template>


<script setup>
import { computed, reactive, ref } from 'vue'
import QuestionBlock from './QuestionBlock.vue'


const props = defineProps({ quiz: { type: Object, required: true } })
const answers = reactive({})
const submitted = ref(false)


const score = computed(() => {
  if (!submitted.value) return 0
  return props.quiz.questions.reduce((acc, q) => acc + (answers[q.id] === q.answerIndex ? 1 : 0), 0)
})


function submit(){ submitted.value = true }
function reset(){ submitted.value = false; for (const k of Object.keys(answers)) delete answers[k] }
</script>