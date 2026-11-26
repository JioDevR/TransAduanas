<template>
  <div class="sga-audit-cards-stack">
    <AuditoriaSectionCard
      step="01"
      title="Identificación del Solicitante & Ficha Comercial"
      subtitle="Información tributaria y canales comerciales del cliente solicitante."
    >
      <div class="sga-audit-grid-3">
        <div class="sga-audit-field">
          <span class="sga-form-label">Cliente / Razón Social</span>
          <span class="sga-audit-val is-strong">{{ data.clienteRazonSocial || '—' }}</span>
        </div>

        <div class="sga-audit-field">
          <span class="sga-form-label">RUC / Documento</span>
          <span class="sga-audit-val font-mono">{{ data.clienteNumeroDocumento || '—' }}</span>
        </div>

        <div class="sga-audit-field">
          <span class="sga-form-label">Tipo de Cartera</span>
          <div>
            <Tag
              :value="data.clienteTipo || 'CLIENTE'"
              :severity="data.clienteTipo === 'CLIENTE' ? 'success' : 'warn'"
              rounded
            />
          </div>
        </div>

        <div class="sga-audit-field">
          <span class="sga-form-label">Asesor Comercial Emisor</span>
          <span class="sga-audit-val">{{ data.usuarioComercialNombre || 'Asesor Comercial Asignado' }}</span>
        </div>

        <div class="sga-audit-field">
          <span class="sga-form-label">Agente Aduanero Responsable</span>
          <span class="sga-audit-val is-strong sga-text-accent">
            {{ data.agenteAduaneroNombre || 'Agente Asignado' }}
          </span>
        </div>

        <div class="sga-audit-field">
          <span class="sga-form-label">Fecha de Apertura / Emisión</span>
          <span class="sga-audit-val font-mono">{{ formatIsoDate(data.fechaApertura || data.fechaEmision) }}</span>
        </div>
      </div>
    </AuditoriaSectionCard>

    <AuditoriaSectionCard
      step="02"
      title="Parámetros Técnicos, Logística & Mercancía"
      subtitle="Régimen aduanero, puertos origen/destino, partida NANDINA y especificación técnica de la carga."
    >
      <div class="sga-audit-grid-3">
        <div class="sga-audit-field sga-col-span-full">
          <span class="sga-form-label">Régimen Aduanero SUNAT</span>
          <div class="sga-audit-inline-group">
            <Tag :value="getRegimenCode(data.regimenAduanero)" severity="contrast" class="font-mono" />
            <span class="sga-audit-val is-strong">{{ formatRegimenFull(data.regimenAduanero) }}</span>
          </div>
        </div>

        <div class="sga-audit-field">
          <span class="sga-form-label">Modalidad de Transporte</span>
          <div class="sga-audit-inline-group">
            <i :class="data.isAirModal ? 'pi pi-send' : 'pi pi-compass'"></i>
            <span class="sga-audit-val is-strong">
              {{ data.isAirModal ? 'Transporte Aéreo' : 'Transporte Marítimo' }}
            </span>
          </div>
        </div>

        <div class="sga-audit-field">
          <span class="sga-form-label">Modalidad de Despacho</span>
          <span class="sga-audit-val">{{ formatTipoDespacho(data.tipoDespacho) }}</span>
        </div>

        <div class="sga-audit-field sga-col-span-full">
          <span class="sga-form-label">Ruta y Puertos / Terminales Internacionales</span>
          <div class="sga-ports-row">
            <div class="sga-port-box">
              <span class="sga-micro-label">ORIGEN / EMBARQUE</span>
              <strong class="sga-port-name">{{ data.puertoEnvioNombre || 'No especificado' }}</strong>
              <span class="sga-port-meta font-mono">
                {{ data.puertoEnvioCodigo || 'S/C' }} · {{ data.puertoEnvioPais || 'Exterior' }}
              </span>
            </div>

            <i class="pi pi-arrow-right sga-ports-arrow" aria-hidden="true"></i>

            <div class="sga-port-box is-highlight">
              <span class="sga-micro-label">DESTINO / ARRIBO</span>
              <strong class="sga-port-name">{{ data.puertoDestinoNombre || 'Sede Central Callao' }}</strong>
              <span class="sga-port-meta font-mono">
                {{ data.puertoDestinoCodigo || 'PE-CALLAO' }} · {{ data.puertoDestinoPais || 'Perú' }}
              </span>
            </div>
          </div>
        </div>

        <div class="sga-audit-field sga-col-span-full">
          <span class="sga-form-label">Mercancía & Clasificación Arancelaria NANDINA</span>
          <div class="sga-subpanel sga-audit-inline-group">
            <Tag
              :value="`PARTIDA: ${data.mercanciaCodigo || 'PENDIENTE'}`"
              icon="pi pi-tag"
              severity="secondary"
              class="font-mono"
            />
            <span class="sga-audit-val is-strong">{{ data.mercanciaNombre || 'Mercancía general' }}</span>
          </div>
        </div>

        <div class="sga-audit-field sga-col-span-full">
          <span class="sga-form-label">Tipo de Carga & Naturaleza</span>
          <div class="sga-audit-inline-group">
            <i class="pi pi-box"></i>
            <span class="sga-audit-val is-strong">{{ data.tipoCargaNombre || 'Carga General / Contenedorizada' }}</span>
          </div>

          <div v-if="data.tipoCargaDescripcion" class="sga-banner is-info sga-mt-xs">
            <i class="pi pi-info-circle"></i>
            <div>
              <span class="sga-micro-label">ESPECIFICACIÓN TÉCNICA DE LA CARGA</span>
              <p class="sga-banner-text">{{ data.tipoCargaDescripcion }}</p>
            </div>
          </div>
        </div>

        <div v-if="data.servicioTransporte || data.origenDepartamento" class="sga-audit-field sga-col-span-full">
          <span class="sga-form-label">Ruta Nacional de Transporte Local (SGA Flota)</span>
          <div class="sga-subpanel sga-audit-inline-group">
            <i class="pi pi-truck"></i>
            <span class="sga-audit-val">
              {{ data.origenDepartamento || 'Callao' }} ({{ data.origenProvincia || 'Callao' }})
            </span>
            <i class="pi pi-arrow-right sga-ports-arrow"></i>
            <span class="sga-audit-val">
              {{ data.destinoDepartamento || 'Lima' }} ({{ data.destinoProvincia || 'Lima' }})
            </span>
            <Tag
              v-if="Number(data.costoTransporteInterno || 0) > 0"
              :value="`Flete: $${formatUsd(data.costoTransporteInterno)}`"
              severity="secondary"
              class="font-mono"
            />
          </div>
        </div>
      </div>
    </AuditoriaSectionCard>
  </div>
</template>

<script setup>
import Tag from 'primevue/tag'
import AuditoriaSectionCard from './AuditoriaSectionCard.vue'
import {
  formatUsd,
  formatRegimenFull,
  getRegimenCode,
  formatTipoDespacho,
  formatIsoDate
} from '../../utils/aduanasFormatters'

defineProps({
  data: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.sga-audit-cards-stack {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sga-audit-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1.125rem 1.25rem;
}

.sga-audit-field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.sga-col-span-full {
  grid-column: 1 / -1;
}

.sga-audit-val {
  font-size: 0.875rem;
  color: var(--color-text-main);
  word-break: break-word;
}

.sga-audit-val.is-strong {
  font-weight: 600;
}

.sga-text-accent {
  color: var(--p-primary-color);
}

.sga-audit-inline-group {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex-wrap: wrap;
}

.sga-subpanel {
  padding: var(--sga-subpanel-padding, 1rem 1.15rem);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
}

.sga-ports-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 0.85rem;
  align-items: center;
}

.sga-port-box {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.9rem 1.05rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
}

.sga-port-box.is-highlight {
  border-color: var(--p-primary-color);
}

.sga-port-name {
  font-size: 0.9rem;
  color: var(--color-text-main);
}

.sga-port-meta {
  font-size: 0.78rem;
  color: var(--color-text-subtle);
}

.sga-ports-arrow {
  color: var(--color-text-subtle);
}

.sga-mt-xs {
  margin-top: 0.45rem;
}

.sga-banner-text {
  margin: 0.2rem 0 0;
}

@media (max-width: 900px) {
  .sga-audit-grid-3 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .sga-audit-grid-3,
  .sga-ports-row {
    grid-template-columns: 1fr;
  }

  .sga-ports-arrow {
    justify-self: center;
    transform: rotate(90deg);
  }
}
</style>
