<template>
  <div class="informe-cards-stack">
    <!-- 1. Cliente & Operación Comercial -->
    <section class="sga-card-surface">
      <header class="informe-card-head">
        <span class="sga-kicker">01 · COMERCIAL Y CONSIGNATARIO</span>
        <h3 class="sga-chart-title">Cliente &amp; Operación Comercial</h3>
      </header>
      <div class="sga-grid-2 informe-kv-grid">
        <div class="informe-kv">
          <span class="sga-form-label">Razón Social</span>
          <span class="informe-val strong">{{ expediente.clienteRazonSocial || '—' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">RUC / Documento</span>
          <span class="informe-val font-mono">{{ expediente.clienteNumeroDocumento || '—' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Cotización Origen</span>
          <span class="informe-val font-mono">{{ expediente.codigoCotizacion || '—' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Régimen Aduanero</span>
          <span class="informe-val">{{ formatRegimenFull(expediente.regimenAduanero) }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Mercancía / Tipo de Carga</span>
          <span class="informe-val">
            {{ expediente.mercanciaNombre || 'Carga Comercial' }}
            <span v-if="expediente.tipoCargaNombre" class="sga-cell-sub">
              ({{ expediente.tipoCargaNombre }})
            </span>
          </span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Tramo Terrestre</span>
          <span class="informe-val">
            <Tag
              v-if="expediente.servicioTransporte"
              :value="expediente.transporteSolicitado ? 'Transporte SGA · Solicitud Externa Registrada' : 'Transporte SGA Contratado'"
              icon="pi pi-truck"
              severity="info"
              rounded
            />
            <Tag
              v-else
              value="Retiro / Ingreso por Cliente"
              severity="secondary"
              rounded
            />
          </span>
        </div>
      </div>
    </section>

    <!-- 2. Logística de Travesía & Embarque -->
    <section class="sga-card-surface">
      <header class="informe-card-head">
        <span class="sga-kicker">02 · TRAVESÍA INTERNACIONAL</span>
        <h3 class="sga-chart-title">Logística de Embarque &amp; Ruta</h3>
      </header>
      <div class="sga-grid-2 informe-kv-grid">
        <div class="informe-kv">
          <span class="sga-form-label">Puerto / Aeropuerto Origen</span>
          <span class="informe-val">
            {{ expediente.puertoEnvioNombre || 'Origen por definir' }}
            <span v-if="expediente.puertoEnvioPais" class="sga-cell-sub">({{ expediente.puertoEnvioPais }})</span>
          </span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Puerto / Aeropuerto Destino</span>
          <span class="informe-val">
            {{ expediente.puertoDestinoNombre || 'Destino por definir' }}
            <span v-if="expediente.puertoDestinoPais" class="sga-cell-sub">({{ expediente.puertoDestinoPais }})</span>
          </span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Nave / Buque / Vuelo</span>
          <span class="informe-val strong">{{ expediente.naveViaje || 'Sin nave asignada' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Contenedor / Guía</span>
          <span class="informe-val font-mono">{{ expediente.numeroContenedor || 'Pendiente' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Precinto Aduanero</span>
          <span class="informe-val font-mono">{{ expediente.precintoAduanero || 'Pendiente' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">ETD / ETA Programados</span>
          <span class="informe-val font-mono">
            {{ formatDateShort(expediente.fechaZarpeEtd) }} → {{ formatDateShort(expediente.fechaLlegadaEta) }}
          </span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Arribo / Ingreso Zona Primaria</span>
          <span class="informe-val strong">
            {{ expediente.fechaArriboAduana ? formatDateTime(expediente.fechaArriboAduana) : 'Pendiente' }}
          </span>
        </div>
        <div v-if="terminalBitacora" class="informe-kv">
          <span class="sga-form-label">Terminal / Depósito Registrado</span>
          <span class="informe-val">{{ terminalBitacora }}</span>
        </div>
      </div>
    </section>

    <!-- 3. Control Aduanero & SUNAT -->
    <section class="sga-card-surface">
      <header class="informe-card-head">
        <span class="sga-kicker">03 · FISCALIZACIÓN ADUANERA</span>
        <h3 class="sga-chart-title">Control SUNAT &amp; Diligencia</h3>
      </header>
      <div class="sga-grid-2 informe-kv-grid">
        <div class="informe-kv">
          <span class="sga-form-label">Declaración DAM</span>
          <span class="informe-val font-mono strong">{{ expediente.numeroDam || 'Sin numeración DAM' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Canal de Selección</span>
          <span class="informe-val">
            <Tag
              :value="getCanalLabel(expediente)"
              :severity="getCanalSeverity(expediente.canalSunat)"
              rounded
            />
          </span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Asignación de Canal</span>
          <span class="informe-val font-mono">
            {{ expediente.fechaAsignacionCanal ? formatDateTime(expediente.fechaAsignacionCanal) : 'Pendiente' }}
          </span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Autorización de Levante</span>
          <span class="informe-val font-mono strong">
            {{ expediente.fechaLevanteAutorizacion ? formatDateTime(expediente.fechaLevanteAutorizacion) : 'No otorgado' }}
          </span>
        </div>
        <div class="informe-kv informe-kv--full">
          <span class="sga-form-label">Acta / Detalle de Inspección SUNAT</span>
          <span class="informe-val">
            {{ expediente.detalleInspeccion || (expediente.canalSunat === 'VERDE' ? 'Levante automático sin aforo físico/documentario.' : 'Sin acta de diligencia registrada.') }}
          </span>
        </div>
      </div>
    </section>

    <!-- 4. Equipo Responsable & Contacto Exterior -->
    <section class="sga-card-surface">
      <header class="informe-card-head">
        <span class="sga-kicker">04 · EQUIPO Y CONTACTOS</span>
        <h3 class="sga-chart-title">Responsables &amp; Contacto Exterior</h3>
      </header>
      <div class="sga-grid-2 informe-kv-grid">
        <div class="informe-kv">
          <span class="sga-form-label">Agente Aduanero</span>
          <span class="informe-val strong">{{ expediente.agenteAduaneroNombre || 'Agente Asignado' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Ejecutivo Comercial</span>
          <span class="informe-val">{{ expediente.usuarioComercialNombre || '—' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Contacto Exterior</span>
          <span class="informe-val">{{ expediente.contactoExteriorNombre || 'No especificado' }}</span>
        </div>
        <div class="informe-kv">
          <span class="sga-form-label">Teléfono / Correo Exterior</span>
          <span class="informe-val font-mono">
            {{ expediente.contactoExteriorTelefono || '—' }}
            <span v-if="expediente.contactoExteriorCorreo" class="sga-cell-sub"> · {{ expediente.contactoExteriorCorreo }}</span>
          </span>
        </div>
      </div>
    </section>

    <!-- 5. Bitácora Operativa -->
    <section class="sga-card-surface">
      <header class="informe-card-head">
        <span class="sga-kicker">05 · TRAZABILIDAD DOCUMENTAL</span>
        <h3 class="sga-chart-title">Bitácora Operativa &amp; Observaciones</h3>
      </header>
      <p v-if="expediente.observaciones" class="informe-obs-text">
        {{ expediente.observaciones }}
      </p>
      <p v-else class="sga-cell-muted">
        No se han registrado observaciones técnicas en la bitácora operativa de este despacho.
      </p>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Tag from 'primevue/tag'
import { extractTerminalBitacora } from '../../utils/agenteWorkflowUtils'
import {
  formatRegimenFull,
  formatDateShort,
  formatDateTime,
  getCanalLabel,
  getCanalSeverity
} from '../../utils/agenteFormatters'

const props = defineProps({
  expediente: { type: Object, required: true }
})

const terminalBitacora = computed(() => extractTerminalBitacora(props.expediente?.observaciones))
</script>

<style scoped>
.informe-cards-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.informe-card-head {
  margin-bottom: 1rem;
  padding-bottom: 0.65rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.informe-kv-grid {
  gap: 0.9rem 1.25rem;
}

.informe-kv {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.informe-kv--full {
  grid-column: 1 / -1;
}

.informe-val {
  font-size: var(--sga-text-base, 0.875rem);
  color: var(--color-text-main);
  overflow-wrap: anywhere;
}

.informe-val.strong {
  font-weight: 700;
}

.informe-obs-text {
  margin: 0;
  font-size: var(--sga-text-base, 0.875rem);
  line-height: 1.6;
  color: var(--color-text-main);
  white-space: pre-line;
}
</style>
