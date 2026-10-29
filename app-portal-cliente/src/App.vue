<template>
  <Toast position="top-right" />
  <ConfirmDialog />
  <router-view />
</template>

<script setup>
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
</script>

<style>
/* Los estilos globales se administran en src/style.css */
</style>
