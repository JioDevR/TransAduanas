<template>
  <Dialog
    :visible="visible"
    modal
    header="Auditoría y Trazabilidad de Cuenta B2B"
    :style="{ width: '620px', maxWidth: '95vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="user" class="sga-dialog-body">
      <div class="aud-head-card">
        <div class="aud-avatar">{{ getInitials(user.contactoRepresentante || user.nombres) }}</div>
        <div class="aud-head-info">
          <div class="aud-title-row">
            <strong>{{ user.contactoRepresentante || user.nombres || 'Representante Legal' }}</strong>
            <Tag
              :value="user.estadoActivo ? 'Cuenta Activa' : 'Cuenta Suspendida'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </div>
          <span class="sga-text-muted">@{{ user.username }} · {{ user.razonSocialCliente }}</span>
        </div>
      </div>

      <div class="sga-card-meta-grid">
        <div class="sga-meta-item">
          <span class="sga-meta-label">EMPRESA CLIENTE</span>
          <span class="sga-meta-val">{{ user.razonSocialCliente || '—' }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">{{ user.tipoDocumentoCliente || 'RUC' }}</span>
          <span class="sga-meta-val">{{ user.rucCliente || '—' }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">CORREO ELECTRÓNICO</span>
          <span class="sga-meta-val">{{ user.correoElectronico || '—' }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">TELÉFONO</span>
          <span class="sga-meta-val">{{ user.telefonoContacto || 'Sin teléfono' }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">ESTADO CONTRASEÑA</span>
          <span class="sga-meta-val" :class="user.debeCambiarPassword ? 'sga-text-warn' : 'sga-text-success'">
            {{ user.debeCambiarPassword ? 'Cambio obligatorio' : 'Regularizada' }}
          </span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">ID TÉCNICO</span>
          <span class="sga-meta-val">#{{ user.idUsuarioCliente }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">FECHA DE ALTA</span>
          <span class="sga-meta-val">{{ formatDate(user.fechaCreacion) }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">ÚLTIMA MODIFICACIÓN</span>
          <span class="sga-meta-val">{{ formatDate(user.fechaActualizacion) }}</span>
        </div>
        <div class="sga-meta-item full-span">
          <span class="sga-meta-label">ÚLTIMO ACCESO AL PORTAL</span>
          <span class="sga-meta-val" :class="user.ultimoAcceso ? 'sga-text-success' : 'sga-text-muted'">
            {{ user.ultimoAcceso ? formatDate(user.ultimoAcceso) : 'Nunca ha iniciado sesión' }}
          </span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="sga-dialog-footer">
        <Button label="Cerrar" severity="secondary" outlined size="small" @click="$emit('update:visible', false)" />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'

defineProps({
  visible: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

defineEmits(['update:visible'])

const getInitials = (nombreCompleto) => {
  const parts = (nombreCompleto || '').trim().split(/\s+/)
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase()
  if (parts.length === 1 && parts[0]) return parts[0].substring(0, 2).toUpperCase()
  return 'B2B'
}

const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.aud-head-card {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md, 8px);
}

.aud-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-primary-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.aud-head-info {
  flex: 1;
  min-width: 0;
}

.aud-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
</style>
