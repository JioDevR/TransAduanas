<template>
  <div class="tracking-ops-wrapper">
    <div class="sga-ops-grid">
      <!-- 1. Control Aduanero & Canal SUNAT -->
      <article class="sga-card-surface sga-tracking-card">
        <header class="sga-tracking-card-head">
          <div class="sga-tracking-card-title">
            <span class="sga-tracking-card-icon"><i class="pi pi-shield"></i></span>
            <div>
              <h3 class="sga-tracking-card-heading">Control Aduanero & SUNAT</h3>
              <span class="sga-tracking-card-sub">Numeración DAM, canal y diligencias</span>
            </div>
          </div>
          <Tag :value="canalMeta.label" :severity="canalMeta.severity" rounded />
        </header>

        <div class="sga-kv-list">
          <div class="sga-kv-row">
            <span class="sga-kv-label">N° DAM SUNAT</span>
            <span class="sga-kv-val sga-mono">
              {{ expediente.numeroDam || 'En trámite' }}
              <button
                v-if="expediente.numeroDam"
                type="button"
                class="sga-copy-btn"
                title="Copiar N° DAM"
                aria-label="Copiar N° DAM"
                @click="$emit('copy-code', expediente.numeroDam, 'N° DAM')"
              >
                <i class="pi pi-copy"></i>
              </button>
            </span>
          </div>

          <div class="sga-kv-row">
            <span class="sga-kv-label">Criterio de Canal</span>
            <span class="sga-kv-val">{{ canalMeta.description }}</span>
          </div>

          <div v-if="expediente.fechaAsignacionCanal" class="sga-kv-row">
            <span class="sga-kv-label">Asignación de Canal</span>
            <span class="sga-kv-val sga-mono">{{ formatDateTime(expediente.fechaAsignacionCanal) }}</span>
          </div>

          <div v-if="expediente.fechaLevanteAutorizacion" class="sga-kv-row">
            <span class="sga-kv-label">Levante Autorizado</span>
            <span class="sga-kv-val sga-mono sga-text-success">
              {{ formatDateTime(expediente.fechaLevanteAutorizacion) }}
            </span>
          </div>

          <div v-if="expediente.verificacionOrigenOk" class="sga-kv-row">
            <span class="sga-kv-label">Verificación en Origen</span>
            <span class="sga-kv-val">
              <Tag value="Verificado" severity="success" class="sga-mini-tag" rounded />
            </span>
          </div>

          <div v-if="expediente.cuadrillaSolicitada" class="sga-kv-row">
            <span class="sga-kv-label">Cuadrilla / Cita Canal Rojo</span>
            <span class="sga-kv-val">
              {{ expediente.codigoCitaInspeccion || 'Programada' }}
              <template v-if="expediente.cuadrillaDetalle"> — {{ expediente.cuadrillaDetalle }}</template>
            </span>
          </div>

          <div v-if="expediente.fechaCitaInspeccion" class="sga-kv-row">
            <span class="sga-kv-label">Fecha Cita Inspección</span>
            <span class="sga-kv-val sga-mono">{{ formatDateTime(expediente.fechaCitaInspeccion) }}</span>
          </div>

          <div v-if="expediente.envioSunatSimuladoOk" class="sga-kv-row">
            <span class="sga-kv-label">Transmisión SUNAT (Naranja)</span>
            <span class="sga-kv-val sga-mono">{{ expediente.codigoTransmisionSunat || 'Transmitido' }}</span>
          </div>

          <div v-if="expediente.entregadoAlmacenVerde" class="sga-kv-row">
            <span class="sga-kv-label">Almacén Canal Verde</span>
            <span class="sga-kv-val">
              <Tag value="Entregado en Almacén" severity="success" class="sga-mini-tag" rounded />
            </span>
          </div>

          <div v-if="expediente.detalleInspeccion" class="sga-kv-row">
            <span class="sga-kv-label">Detalle Diligencia</span>
            <span class="sga-kv-val">{{ expediente.detalleInspeccion }}</span>
          </div>
        </div>
      </article>

      <!-- 2. Depósito Temporal & Almacenaje -->
      <article class="sga-card-surface sga-tracking-card">
        <header class="sga-tracking-card-head">
          <div class="sga-tracking-card-title">
            <span class="sga-tracking-card-icon"><i class="pi pi-building"></i></span>
            <div>
              <h3 class="sga-tracking-card-heading">Depósito Temporal & Almacenaje</h3>
              <span class="sga-tracking-card-sub">{{ expediente.depositoTemporal || 'DP World / APM Terminals' }}</span>
            </div>
          </div>
          <Tag
            v-if="expediente.tieneAlmacen && expediente.estadoAlmacen"
            :value="expediente.estadoAlmacen"
            severity="info"
            class="sga-mini-tag"
            rounded
          />
        </header>

        <template v-if="expediente.tieneAlmacen">
          <div class="sga-kv-list">
            <div class="sga-kv-row">
              <span class="sga-kv-label">Ubicación / Bahía</span>
              <span class="sga-kv-val sga-mono">{{ expediente.ubicacionBahia || 'Bahía Primaria' }}</span>
            </div>
            <div v-if="expediente.cantidadBultos != null" class="sga-kv-row">
              <span class="sga-kv-label">Bultos / Embalaje</span>
              <span class="sga-kv-val sga-mono">{{ expediente.cantidadBultos }} bulto(s)</span>
            </div>
            <div v-if="expediente.pesoBrutoKg != null" class="sga-kv-row">
              <span class="sga-kv-label">Peso Bruto</span>
              <span class="sga-kv-val sga-mono">{{ Number(expediente.pesoBrutoKg).toLocaleString('en-US') }} kg</span>
            </div>
            <div class="sga-kv-row">
              <span class="sga-kv-label">Días Libres Pactados</span>
              <span class="sga-kv-val sga-mono">{{ expediente.diasLibresAlmacen || 15 }} días</span>
            </div>
          </div>

          <div
            v-if="expediente.estadoExpediente !== 'CERRADO'"
            :class="['sga-banner', getAlmacenAlertClass(expediente.diasRestantesAlmacen)]"
          >
            <i class="pi pi-clock"></i>
            <div class="almacen-banner-text">
              <strong>
                {{ expediente.diasRestantesAlmacen != null ? expediente.diasRestantesAlmacen : 15 }} días restantes de sobreestadía libre
              </strong>
              <span class="sga-mono">
                Límite de retiro: {{ expediente.fechaLimiteRetiro ? formatDate(expediente.fechaLimiteRetiro) : 'Consultar agente' }}
              </span>
            </div>
          </div>
        </template>

        <div v-else class="sga-inline-empty">
          <i class="pi pi-compass"></i>
          <span>
            Carga con despacho directo o actualmente en tránsito internacional sin ingreso registrado a depósito temporal.
          </span>
        </div>
      </article>

      <!-- 3. Transporte Local & Última Milla -->
      <article class="sga-card-surface sga-tracking-card">
        <header class="sga-tracking-card-head">
          <div class="sga-tracking-card-title">
            <span class="sga-tracking-card-icon"><i class="pi pi-truck"></i></span>
            <div>
              <h3 class="sga-tracking-card-heading">Transporte Local & Última Milla</h3>
              <span class="sga-tracking-card-sub">Solicitud de traslado terrestre externo</span>
            </div>
          </div>
          <Tag
            v-if="expediente.tieneTransporte"
            :value="expediente.estadoTransporte || 'SOLICITADO'"
            severity="info"
            class="sga-mini-tag"
            rounded
          />
        </header>

        <template v-if="expediente.tieneTransporte">
          <div class="sga-kv-list">
            <div v-if="expediente.tipoTraslado" class="sga-kv-row">
              <span class="sga-kv-label">Modalidad</span>
              <span class="sga-kv-val">
                {{ expediente.tipoTraslado === 'RECOJO_EXPORTACION' ? 'Recojo en Planta (Exportación)' : 'Entrega a Destino (Importación)' }}
              </span>
            </div>
            <div v-if="expediente.direccionPartida" class="sga-kv-row">
              <span class="sga-kv-label">Punto de Partida</span>
              <span class="sga-kv-val">{{ expediente.direccionPartida }}</span>
            </div>
            <div class="sga-kv-row">
              <span class="sga-kv-label">Dirección de Entrega</span>
              <span class="sga-kv-val">
                {{ expediente.direccionLlegada || 'Planta del cliente' }}
                <template v-if="expediente.distritoLlegada"> ({{ expediente.distritoLlegada }})</template>
              </span>
            </div>
            <div v-if="expediente.contactoRecepcionTransporte" class="sga-kv-row">
              <span class="sga-kv-label">Contacto Recepción</span>
              <span class="sga-kv-val">{{ expediente.contactoRecepcionTransporte }}</span>
            </div>
            <div v-if="expediente.fechaProgramadaTransporte" class="sga-kv-row">
              <span class="sga-kv-label">Fecha Programada</span>
              <span class="sga-kv-val sga-mono">{{ formatDateTime(expediente.fechaProgramadaTransporte) }}</span>
            </div>
            <div v-if="expediente.observacionesTransporte" class="sga-kv-row">
              <span class="sga-kv-label">Instrucciones</span>
              <span class="sga-kv-val">{{ expediente.observacionesTransporte }}</span>
            </div>
          </div>
          <p class="sga-inline-empty" style="margin-top: 0.75rem;">
            <i class="pi pi-info-circle"></i>
            <span>Coordinación operativa gestionada con operador de transporte externo.</span>
          </p>
        </template>

        <div v-else class="sga-inline-empty">
          <i class="pi pi-truck"></i>
          <span>
            {{
              expediente.servicioTransporte
                ? 'Servicio de transporte contratado; solicitud externa pendiente de registro tras el levante aduanero.'
                : 'Modalidad de retiro o ingreso directo por cuenta del cliente.'
            }}
          </span>
        </div>
      </article>

      <!-- 4. Manifiesto Internacional & Contacto Exterior -->
      <article class="sga-card-surface sga-tracking-card">
        <header class="sga-tracking-card-head">
          <div class="sga-tracking-card-title">
            <span class="sga-tracking-card-icon"><i class="pi pi-globe"></i></span>
            <div>
              <h3 class="sga-tracking-card-heading">Manifiesto & Contacto Exterior</h3>
              <span class="sga-tracking-card-sub">Contraparte, nave/vuelo y fechas</span>
            </div>
          </div>
          <Button
            v-if="expediente.estadoExpediente !== 'CERRADO' && expediente.estadoExpediente !== 'ANULADO'"
            type="button"
            label="Completar Datos"
            icon="pi pi-pencil"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('edit-client-data', expediente)"
          />
        </header>

        <div class="sga-kv-list">
          <div class="sga-kv-row">
            <span class="sga-kv-label">Contacto Exterior</span>
            <span class="sga-kv-val">{{ expediente.contactoExteriorNombre || 'Sin registrar' }}</span>
          </div>
          <div v-if="expediente.contactoExteriorTelefono" class="sga-kv-row">
            <span class="sga-kv-label">Teléfono</span>
            <span class="sga-kv-val sga-mono">{{ expediente.contactoExteriorTelefono }}</span>
          </div>
          <div v-if="expediente.contactoExteriorCorreo" class="sga-kv-row">
            <span class="sga-kv-label">Correo</span>
            <span class="sga-kv-val">{{ expediente.contactoExteriorCorreo }}</span>
          </div>
          <div v-if="expediente.naveViaje || expediente.numeroVuelo" class="sga-kv-row">
            <span class="sga-kv-label">Nave / Vuelo</span>
            <span class="sga-kv-val sga-mono">{{ expediente.numeroVuelo || expediente.naveViaje }}</span>
          </div>
          <div v-if="expediente.numeroContenedor" class="sga-kv-row">
            <span class="sga-kv-label">Contenedor / Precinto</span>
            <span class="sga-kv-val sga-mono">
              {{ expediente.numeroContenedor }}
              {{ expediente.precintoAduanero ? `(${expediente.precintoAduanero})` : '' }}
            </span>
          </div>
          <div v-if="expediente.fechaZarpeEtd" class="sga-kv-row">
            <span class="sga-kv-label">Zarpe Estimado (ETD)</span>
            <span class="sga-kv-val sga-mono">{{ formatDate(expediente.fechaZarpeEtd) }}</span>
          </div>
          <div v-if="expediente.fechaLlegadaEta" class="sga-kv-row">
            <span class="sga-kv-label">Arribo Estimado (ETA)</span>
            <span class="sga-kv-val sga-mono">{{ formatDate(expediente.fechaLlegadaEta) }}</span>
          </div>
          <div v-if="expediente.fechaArriboAduana" class="sga-kv-row">
            <span class="sga-kv-label">Arribo Confirmado</span>
            <span class="sga-kv-val sga-mono">{{ formatDateTime(expediente.fechaArriboAduana) }}</span>
          </div>
        </div>
      </article>
    </div>

    <!-- Informe de Conformidad de Entrega -->
    <section v-if="expediente.informeConformidadTexto" class="sga-card-surface sga-tracking-card">
      <header class="sga-tracking-card-head">
        <div class="sga-tracking-card-title">
          <span class="sga-tracking-card-icon"><i class="pi pi-file-check"></i></span>
          <div>
            <span class="sga-kicker">ACTA DE CONFORMIDAD DE ENTREGA</span>
            <h3 class="sga-tracking-card-heading">Informe de Conformidad al Cliente</h3>
            <span v-if="expediente.receptorConformidadNombre" class="sga-tracking-card-sub">
              Receptor: <strong>{{ expediente.receptorConformidadNombre }}</strong>
              <template v-if="expediente.receptorConformidadDni">
                (DNI: <span class="sga-mono">{{ expediente.receptorConformidadDni }}</span>)
              </template>
            </span>
          </div>
        </div>

        <div class="sga-form-actions">
          <Button
            type="button"
            label="Ver Informe PDF"
            icon="pi pi-eye"
            severity="secondary"
            outlined
            size="small"
            @click="$emit('preview-informe', expediente)"
          />
          <Button
            type="button"
            label="Descargar PDF"
            icon="pi pi-download"
            size="small"
            @click="$emit('download-informe', expediente)"
          />
        </div>
      </header>

      <p class="informe-body-text">{{ expediente.informeConformidadTexto }}</p>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { formatDate, formatDateTime } from '@/shared/utils/formatters'
import { getCanalMeta, getAlmacenAlertClass } from '../../utils/expedienteTrackingUtils'

const props = defineProps({
  expediente: { type: Object, required: true }
})

defineEmits(['edit-client-data', 'preview-informe', 'download-informe', 'copy-code'])

const canalMeta = computed(() => getCanalMeta(props.expediente?.canalSunat))
</script>

<style scoped>
.tracking-ops-wrapper {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.almacen-banner-text {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  font-size: 0.78rem;
}

.informe-body-text {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.55;
  color: var(--color-text-main);
}
</style>
