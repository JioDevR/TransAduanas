<template>
  <!-- Contenedores globales de PrimeVue (ToastService y ConfirmationService registrados en main.js) -->
  <Toast position="top-right" />
  <ConfirmDialog />
  <router-view />
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

// Carga diferida: Toast y ConfirmDialog (con sus dependencias de PrimeVue) no son
// necesarios para el primer render, así que se sacan del bundle inicial.
const Toast = defineAsyncComponent(() => import('primevue/toast'))
const ConfirmDialog = defineAsyncComponent(() => import('primevue/confirmdialog'))
</script>

<style>
/* Los estilos globales se administran en src/style.css */
</style>
