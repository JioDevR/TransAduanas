<template>
  <Tag :severity="roleSeverity" class="portal-role-tag" rounded>
    <template #icon>
      <i :class="roleIcon"></i>
    </template>
    <span>{{ displayRoleName }}</span>
  </Tag>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'

const props = defineProps({
  roleName: {
    type: String,
    default: 'ADMIN_EMPRESA'
  }
})

const roleConfig = {
  PORTAL_B2B: {
    label: 'PORTAL CLIENTE',
    icon: 'pi pi-building',
    severity: 'contrast'
  },
  ADMIN_EMPRESA: {
    label: 'PORTAL CLIENTE',
    icon: 'pi pi-building',
    severity: 'contrast'
  },
  OPERADOR_LOGISTICO: {
    label: 'OPERADOR LOGÍSTICO',
    icon: 'pi pi-compass',
    severity: 'info'
  },
  CONSULTOR_FINANCIERO: {
    label: 'CONSULTOR FINANCIERO',
    icon: 'pi pi-dollar',
    severity: 'success'
  },
  CONSULTA: {
    label: 'SOLO CONSULTA',
    icon: 'pi pi-eye',
    severity: 'secondary'
  }
}

const displayRoleName = computed(() => {
  return roleConfig[props.roleName]?.label || 'PORTAL CLIENTE'
})

const roleSeverity = computed(() => {
  return roleConfig[props.roleName]?.severity || 'contrast'
})

const roleIcon = computed(() => {
  return roleConfig[props.roleName]?.icon || 'pi pi-building'
})
</script>

<style scoped>
.portal-role-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.65rem;
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.portal-role-tag i {
  font-size: 0.7rem;
}
</style>
