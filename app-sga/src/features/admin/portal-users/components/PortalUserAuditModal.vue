<template>
  <Dialog
    :visible="visible"
    modal
    header="Auditoría y Trazabilidad de Cuenta B2B"
    :style="{ width: '640px', maxWidth: '95vw' }"
    class="portal-audit-dialog"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="user" class="audit-modal-body">
      <!-- Encabezado con Identidad y Estado -->
      <div class="audit-header-banner">
        <div class="user-main-avatar">
          {{ getInitials(user.contactoRepresentante || user.nombres) }}
        </div>
        <div class="user-header-text">
          <div class="user-title-row">
            <h4 class="user-full-name">{{ user.contactoRepresentante || user.nombres || 'Representante Legal' }}</h4>
            <Tag
              :value="user.estadoActivo ? 'Cuenta Activa' : 'Cuenta Suspendida'"
              :severity="user.estadoActivo ? 'success' : 'danger'"
              rounded
            />
          </div>
          <span class="user-handle font-mono">@{{ user.username }} &bull; {{ user.razonSocialCliente }}</span>
        </div>
      </div>

      <!-- Cuadrícula de Información Dividida en Secciones -->
      <div class="audit-details-grid">
        <!-- Sección 1: Empresa Cliente Vinculada -->
        <div class="audit-card-section">
          <div class="section-title-row">
            <i class="pi pi-building"></i>
            <span>Empresa Cliente Vinculada</span>
          </div>
          <div class="info-row">
            <span class="info-label">Razón Social:</span>
            <span class="info-value font-bold">{{ user.razonSocialCliente || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ user.tipoDocumentoCliente || 'RUC' }}:</span>
            <span class="info-value font-mono">{{ user.rucCliente || '—' }}</span>
          </div>
          <div v-if="user.direccionFiscalCliente" class="info-row">
            <span class="info-label">Dirección Fiscal:</span>
            <span class="info-value text-muted">{{ user.direccionFiscalCliente }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">ID Empresa en SGA:</span>
            <span class="info-value font-mono text-muted">#{{ user.idCliente }}</span>
          </div>
        </div>

        <!-- Sección 2: Perfil y Permisos en el Portal -->
        <div class="audit-card-section">
          <div class="section-title-row">
            <i class="pi pi-shield"></i>
            <span>Perfil y Acreditación B2B</span>
          </div>
          <div class="info-row">
            <span class="info-label">Tipo de Acceso:</span>
            <span class="info-value font-medium">Cuenta Corporativa B2B (General)</span>
          </div>
          <div class="info-row">
            <span class="info-label">Representante Oficial:</span>
            <span class="info-value">{{ user.contactoRepresentante || user.nombres || 'Representante Legal' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Cambio de Contraseña:</span>
            <span :class="['font-mono', user.debeCambiarPassword ? 'text-warning' : 'text-success']">
              {{ user.debeCambiarPassword ? 'Obligatorio en próximo login' : 'Al día / Regularizada' }}
            </span>
          </div>
        </div>

        <!-- Sección 3: Datos de Contacto y Notificaciones -->
        <div class="audit-card-section">
          <div class="section-title-row">
            <i class="pi pi-envelope"></i>
            <span>Contacto y Notificaciones</span>
          </div>
          <div class="info-row">
            <span class="info-label">Correo Electrónico:</span>
            <span class="info-value font-mono">{{ user.correoElectronico }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Teléfono de Contacto:</span>
            <span class="info-value">{{ user.telefonoContacto || 'Sin teléfono' }}</span>
          </div>
        </div>

        <!-- Sección 4: Marcas Temporales y Auditoría Técnica -->
        <div class="audit-card-section">
          <div class="section-title-row">
            <i class="pi pi-history"></i>
            <span>Marcas Temporales & Acceso</span>
          </div>
          <div class="info-row">
            <span class="info-label">ID Técnico:</span>
            <span class="info-value font-mono">#{{ user.idUsuarioCliente }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Fecha de Alta / Creación:</span>
            <span class="info-value font-mono">{{ formatDate(user.fechaCreacion) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Última Modificación:</span>
            <span class="info-value font-mono">{{ formatDate(user.fechaActualizacion) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Último Acceso al Portal:</span>
            <span :class="['info-value font-mono', user.ultimoAcceso ? 'text-success' : 'text-muted']">
              {{ user.ultimoAcceso ? formatDate(user.ultimoAcceso) : 'Nunca ha iniciado sesión' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer-actions">
        <button type="button" class="btn-outline-warm" @click="$emit('update:visible', false)">
          Cerrar
        </button>
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'

defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  user: {
    type: Object,
    default: null
  }
})

defineEmits(['update:visible'])

const getInitials = (nombreCompleto) => {
  const parts = (nombreCompleto || '').trim().split(/\s+/)
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase()
  } else if (parts.length === 1 && parts[0]) {
    return parts[0].substring(0, 2).toUpperCase()
  }
  return 'B2B'
}

const formatDate = (dateString) => {
  if (!dateString) return '—'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return dateString
  return date.toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.audit-modal-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0.5rem 0;
}

.audit-header-banner {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
}

.user-main-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-primary-text);
  font-weight: 700;
  font-size: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-header-text {
  flex: 1;
}

.user-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
}

.user-full-name {
  margin: 0;
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.user-handle {
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-accent-kicker);
}

.audit-details-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

.audit-card-section {
  padding: 1rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  margin-bottom: 0.25rem;
  padding-bottom: 0.35rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--font-size-sm, 0.8125rem);
  gap: 1rem;
}

.info-label {
  color: var(--color-text-muted);
  font-weight: 500;
}

.info-value {
  color: var(--color-text-main);
  text-align: right;
}

.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 0.5rem;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  font-size: 0.8125rem;
  font-weight: 600;
  padding: 0.5rem 0.95rem;
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms);
}

.btn-outline-warm:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.text-warning {
  color: var(--color-warning-text);
}

.text-success {
  color: var(--color-success-text);
}

.text-muted {
  color: var(--color-text-muted);
}

.font-bold {
  font-weight: 700;
}
</style>
