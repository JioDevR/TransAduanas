<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { initTheme } from '@/composables/useTheme'

onMounted(() => {
  initTheme()
})
</script>
