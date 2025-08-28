<template>
  <article class="vstack" style="gap:8px">
    <h3 style="margin:0">{{ index + 1 }}. {{ q.prompt }}</h3>
    <p class="small" style="margin:0;color:var(--muted)">Kilde: <em>{{ q.sourceTitle }}</em></p>
    <ul style="margin:4px 0 0 0; padding-left:18px">
      <li v-for="(opt, i) in q.options" :key="i" class="hstack" style="gap:8px;align-items:center">
        <label class="hstack" style="gap:8px;cursor:pointer">
          <input type="radio" :name="q.id" :value="i" :checked="modelValue[q.id] === i" @change="onSelect(i)" />
          <span>{{ opt }}</span>
        </label>
      </li>
    </ul>
    <div v-if="submitted" class="vstack" style="gap:6px;background:#0c1a11;border:1px solid #1b6b4f;padding:10px 12px;border-radius:10px">
      <p style="margin:0">
        Riktig svar: <strong>{{ q.options[q.answerIndex] }}</strong>
        <span v-if="q.explanation"> – {{ q.explanation }}</span>
      </p>
    </div>
    <hr />
  </article>
</template>


<script setup>
const props = defineProps({
  q: { type: Object, required: true },
  index: { type: Number, required: true },
  submitted: { type: Boolean, required: true },
  modelValue: { type: Object, required: true }
})
const emit = defineEmits(['update:modelValue'])


function onSelect(i){
  const next = { ...props.modelValue, [props.q.id]: i }
  emit('update:modelValue', next)
}
</script>