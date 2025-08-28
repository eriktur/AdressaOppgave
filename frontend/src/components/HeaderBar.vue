<template>
  <header style="position:sticky;top:0;z-index:10;background:rgba(15,23,42,.8);backdrop-filter: blur(10px);border-bottom:1px solid var(--border)">
    <div class="container spread" style="padding:14px 24px;align-items:center">
      <div class="hstack" style="gap:10px">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M4 6h16M4 12h16M4 18h16" stroke="#60a5fa" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <strong>Nyhetsquiz</strong>
        <span class="badge">Vue 3</span>
      </div>
      <div class="hstack small" style="gap:8px;color:var(--muted)">
        <span class="kbd">⌘</span><span>+</span><span class="kbd">K</span>
        <span class="small">for å fokusere søk (ikke aktivert)</span>
      </div>
    </div>
  </header>
</template>
<script setup></script>