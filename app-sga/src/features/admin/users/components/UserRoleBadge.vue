<template>
  <Tag :severity="roleSeverity" class="role-tag" rounded>
    <template #icon>
      <i :class="roleIcon"></i>
    </template>
    <span>{{ displayRoleName }}</span>
  </Tag>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { ROLES, ROLE_LABELS } from '@/core/auth/roles'

const props = defineProps({
  roleName: {
    type: String,
    default: ''
  }
})

const roleConfig = {
  [ROLES.ADMINISTRADOR_TI]: { icon: 'pi pi-shield', severity: 'contrast' },
  [ROLES.GERENTE_GENERAL]: { icon: 'pi pi-briefcase', severity: 'contrast' },
  [ROLES.AGENTE_COMERCIAL]: { icon: 'pi pi-shopping-bag', severity: 'success' },
  [ROLES.SUPERVISOR_ADUANERO]: { icon: 'pi pi-verified', severity: 'info' },
  [ROLES.AGENTE_ADUANERO]: { icon: 'pi pi-compass', severity: 'info' },
  [ROLES.SUPERVISOR_TRANSPORTE]: { icon: 'pi pi-truck', severity: 'warn' },
  [ROLES.CONTADOR]: { icon: 'pi pi-dollar', severity: 'success' },
  [ROLES.ASESOR_LEGAL]: { icon: 'pi pi-bookmark', severity: 'danger' }
}

const displayRoleName = computed(() => {
  return ROLE_LABELS[props.roleName] || props.roleName || 'SIN ROL'
})

const roleSeverity = computed(() => {
  return roleConfig[props.roleName]?.severity || 'secondary'
})

const roleIcon = computed(() => {
  return roleConfig[props.roleName]?.icon || 'pi pi-user'
})
</script>

<style scoped>
.role-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.65rem;
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.role-tag i {
  font-size: 0.7rem;
}
</style>
