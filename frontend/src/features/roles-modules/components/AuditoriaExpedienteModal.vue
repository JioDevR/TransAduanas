<template>
  <div class="auditoria-expediente-view">
    <!-- 1. Header Bar de Auditoría y Acciones de Supervisión -->
    <div class="form-header-bar">
      <div class="header-titles">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('back')"
          title="Regresar a la bandeja de supervisión"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver a Supervisión Aduanera</span>
        </button>

        <div class="kicker-row">
          <span class="badge-kicker">SUPERVISIÓN TÉCNICA // AUDITORÍA ADUANERA</span>
          <span :class="['status-badge', getStatusBadgeClass(cotizacion.estado)]">
            <i :class="getStatusIcon(cotizacion.estado)"></i>
            <span>{{ getEstadoLabel(cotizacion.estado) }}</span>
          </span>
          <span v-if="cotizacion.tieneExpediente" class="exp-linked-badge">
            <i class="pi pi-folder"></i>
            <span>EXPEDIENTE: {{ cotizacion.codigoExpediente }}</span>
          </span>
        </div>

        <h1 class="form-main-title">
          Auditoría Técnica del Expediente: {{ cotizacion.codigoCotizacion || `COT-${cotizacion.idCotizacion}` }}
        </h1>
        <p class="form-main-desc">
          Inspección técnica integral, valoración aduanera y control documentario. Verifique los requisitos arancelarios y el sustento documental antes de autorizar la formalización o solicitar subsanación a ventas.
        </p>
      </div>

      <!-- Acciones de Cabecera (Principio de Única Fuente de Verdad) -->
      <div class="header-actions">
        <!-- Acciones cuando la cotización está en revisión o pendiente -->
        <template v-if="isAuditableState">
          <button
            type="button"
            class="btn-danger-action"
            @click="toggleRejectBox"
            :disabled="processing"
            title="Rechazar definitivamente la propuesta comercial"
          >
            <i class="pi pi-times"></i>
            <span>Rechazar</span>
          </button>

          <button
            type="button"
            class="btn-outline-warm"
            @click="toggleObservationBox"
            :disabled="processing"
            title="Devolver a Ventas con observaciones técnicas para corrección"
          >
            <i class="pi pi-file-edit"></i>
            <span>Solicitar Corrección</span>
          </button>

          <button
            type="button"
            class="btn-espresso"
            @click="$emit('approve', cotizacion)"
            :disabled="processing"
            title="Aprobar técnicamente para apertura de Expediente Aduanero"
          >
            <i v-if="processing" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-check"></i>
            <span>Aprobar Propuesta</span>
          </button>
        </template>

        <!-- Acciones cuando la cotización ya fue APROBADA -->
        <template v-else-if="cotizacion.estado === 'APROBADA'">
          <button
            v-if="!cotizacion.tieneExpediente && cotizacion.tienePagoAprobado"
            type="button"
            class="btn-espresso"
            @click="$emit('open-apertura', cotizacion)"
            title="Generar expediente aduanero formal para esta cotización"
          >
            <i class="pi pi-folder-open"></i>
            <span>+ Aperturar Expediente</span>
          </button>

          <span
            v-else-if="!cotizacion.tieneExpediente"
            class="status-lock-pill"
            title="Se requiere aprobación de pago en Finanzas antes de aperturar el expediente"
          >
            <i class="pi pi-lock"></i>
            <span>Requiere Pago Finanzas</span>
          </span>
        </template>

        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('back')"
        >
          <span>Cerrar</span>
        </button>
      </div>
    </div>

    <!-- 2. Panel Expansible para Solicitar Corrección / Observación -->
    <div v-if="showObservationBox" class="inline-action-panel observation-panel fade-in">
      <div class="panel-header">
        <div class="panel-title">
          <i class="pi pi-file-edit"></i>
          <span>Solicitar Corrección Técnica a Ventas (Marcar como Observada)</span>
        </div>
        <button type="button" class="btn-close-panel" @click="showObservationBox = false">
          <i class="pi pi-times"></i>
        </button>
      </div>
      <p class="panel-desc">
        Especifique con precisión las observaciones técnicas o arancelarias para que el asesor comercial rectifique la cotización <strong>{{ cotizacion.codigoCotizacion }}</strong>. Una vez corregida por Ventas, volverá a esta bandeja para su reevaluación.
      </p>
      <div class="panel-input-box">
        <label class="panel-label">Detalle de Observación / Motivo de Revisión *</label>
        <textarea
          v-model="observationReason"
          rows="3"
          class="form-textarea"
          placeholder="Ej. Rectificar partida arancelaria NANDINA para régimen definitivo, ajustar flete según BL o adjuntar factura comercial legalizada..."
        ></textarea>
      </div>
      <div class="panel-actions-row">
        <button type="button" class="btn-outline-warm" @click="showObservationBox = false">
          <span>Cancelar</span>
        </button>
        <button
          type="button"
          class="btn-warm-action"
          :disabled="!observationReason.trim() || processing"
          @click="submitObservation"
        >
          <i v-if="processing" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-send"></i>
          <span>Confirmar y Enviar a Ventas</span>
        </button>
      </div>
    </div>

    <!-- 3. Panel Expansible para Rechazo Definitivo -->
    <div v-if="showRejectBox" class="inline-action-panel reject-panel fade-in">
      <div class="panel-header">
        <div class="panel-title text-danger">
          <i class="pi pi-ban"></i>
          <span>Denegar / Rechazar Propuesta Comercial Definitivamente</span>
        </div>
        <button type="button" class="btn-close-panel" @click="showRejectBox = false">
          <i class="pi pi-times"></i>
        </button>
      </div>
      <p class="panel-desc">
        Al rechazar la propuesta <strong>{{ cotizacion.codigoCotizacion }}</strong>, se cerrará el trámite comercial sin opción a subsanación.
      </p>
      <div class="panel-input-box">
        <label class="panel-label">Motivo de Rechazo *</label>
        <textarea
          v-model="rejectReason"
          rows="2"
          class="form-textarea"
          placeholder="Indique la causa del rechazo técnico (ej. Operación inviable normativamente por mercancía prohibida)..."
        ></textarea>
      </div>
      <div class="panel-actions-row">
        <button type="button" class="btn-outline-warm" @click="showRejectBox = false">
          <span>Cancelar</span>
        </button>
        <button
          type="button"
          class="btn-danger-action"
          :disabled="!rejectReason.trim() || processing"
          @click="submitReject"
        >
          <i v-if="processing" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-times"></i>
          <span>Confirmar Rechazo Definitivo</span>
        </button>
      </div>
    </div>

    <!-- 4. Alerta de Observación previa si la cotización ya fue devuelta antes -->
    <div v-if="cotizacion.motivoObservacion && !showObservationBox" class="observation-history-card">
      <div class="obs-icon-col">
        <i class="pi pi-exclamation-circle"></i>
      </div>
      <div class="obs-body">
        <div class="obs-header-row">
          <span class="obs-kicker">OBSERVACIÓN TÉCNICA REGISTRADA PREVIAMENTE</span>
          <span class="obs-status">PENDIENTE DE SUBSANACIÓN</span>
        </div>
        <p class="obs-text">{{ cotizacion.motivoObservacion }}</p>
        <span class="obs-note">Revise si el asesor comercial ha corregido los importes o adjuntado los documentos solicitados.</span>
      </div>
    </div>

    <!-- 5. Grid Principal de Contenido (Resumen del Expediente + Panel Lateral) -->
    <div class="form-content-grid">
      <!-- Columna Principal: Ficha Técnica y Documentos -->
      <div class="form-blocks-column">
        <!-- Bloque 01: Identificación y Datos del Solicitante -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación del Solicitante & Ficha Comercial</h3>
              <p class="block-sub">Información tributaria y canales comerciales del cliente solicitante.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="details-grid-3">
              <div class="detail-item">
                <span class="detail-label">Cliente / Razón Social:</span>
                <span class="detail-val strong">{{ cotizacion.clienteRazonSocial || '—' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">RUC / Documento:</span>
                <span class="detail-val font-mono">{{ cotizacion.clienteNumeroDocumento || '—' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Tipo de Cartera:</span>
                <span class="detail-val">
                  <span :class="['badge-type', cotizacion.clienteTipo === 'CLIENTE' ? 'type-cliente' : 'type-prospecto']">
                    {{ cotizacion.clienteTipo || 'CLIENTE' }}
                  </span>
                </span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Asesor Comercial Emisor:</span>
                <span class="detail-val">{{ cotizacion.usuarioComercialNombre || 'Asesor Comercial Asignado' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Fecha de Emisión:</span>
                <span class="detail-val font-mono">{{ formatDate(cotizacion.fechaEmision) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Vigencia de la Oferta:</span>
                <span class="detail-val">{{ cotizacion.validezDias || 15 }} días calendario</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Bloque 02: Parámetros Técnicos Aduaneros & Logística -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Parámetros Técnicos Aduaneros & Transporte</h3>
              <p class="block-sub">Régimen aduanero declarado, puntos de ingreso y logística terrestre nacional.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="details-grid-3">
              <div class="detail-item full-width-sm">
                <span class="detail-label">Régimen Aduanero SUNAT:</span>
                <div class="regimen-tag-box">
                  <span class="regimen-code-pill font-mono">{{ getRegimenCode(cotizacion.regimenAduanero) }}</span>
                  <span class="regimen-name-text">{{ formatRegimen(cotizacion.regimenAduanero) }}</span>
                </div>
              </div>
              <div class="detail-item">
                <span class="detail-label">Modalidad de Despacho:</span>
                <span class="detail-val">{{ formatTipoDespacho(cotizacion.tipoDespacho) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Vía de Transporte:</span>
                <span class="detail-val">{{ formatTransporte(cotizacion.viaTransporte) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Aduana / Puerto de Ingreso:</span>
                <span class="detail-val">{{ cotizacion.puertoLlegada || 'Sede Central Callao (Marítimo)' }}</span>
              </div>
              <div class="detail-item full-width-sm">
                <span class="detail-label">Ruta Nacional de Transporte Local:</span>
                <div class="route-badge-box">
                  <i class="pi pi-map-marker"></i>
                  <span>{{ cotizacion.origenDepartamento || 'Callao' }} ({{ cotizacion.origenProvincia || 'Callao' }})</span>
                  <i class="pi pi-arrow-right route-arrow"></i>
                  <span>{{ cotizacion.destinoDepartamento || 'Lima' }} ({{ cotizacion.destinoProvincia || 'Lima' }})</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Bloque 03: Valoración Aduanera & Liquidación Arancelaria -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Liquidación Económica & Valoración Aduanera</h3>
              <p class="block-sub">Base imponible FOB/CIF y liquidación de tributos, aranceles y servicios aduaneros.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Tarjetas de Base Imponible (Valor CIF) -->
            <div class="cif-cards-row">
              <div class="cif-card">
                <span class="cif-card-label">VALOR FOB</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(cotizacion.valorFob || cotizacion.montoFobEstimado) }}</span>
                <span class="cif-card-sub">Mercancía declarada</span>
              </div>
              <div class="cif-card">
                <span class="cif-card-label">FLETE INTERNACIONAL</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(cotizacion.fleteInternacional || cotizacion.montoFleteEstimado) }}</span>
                <span class="cif-card-sub">Flete naviero / aéreo</span>
              </div>
              <div class="cif-card">
                <span class="cif-card-label">SEGURO ESTIMADO</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(cotizacion.seguroEstimado || cotizacion.montoSeguroEstimado) }}</span>
                <span class="cif-card-sub">Póliza o tabla SUNAT</span>
              </div>
              <div class="cif-card highlight">
                <span class="cif-card-label">VALOR CIF (BASE SUNAT)</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(cotizacion.valorCif) }}</span>
                <span class="cif-card-sub">Base imponible tributaria</span>
              </div>
            </div>

            <!-- Tabla de Tributos y Servicios a Liquidar -->
            <div class="financial-table-wrapper">
              <table class="financial-breakdown-table">
                <thead>
                  <tr>
                    <th>Concepto Arancelario / Operativo</th>
                    <th class="text-center">Tasa Aplicada</th>
                    <th class="text-right">Monto Liquidado ($ USD)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      <strong>Arancel Ad-Valórem</strong>
                      <span class="concept-sub">Tasa arancelaria según partida NANDINA</span>
                    </td>
                    <td class="text-center font-mono font-bold">{{ cotizacion.tasaAdValorem ?? 0 }}%</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(cotizacion.montoAdValorem) }}</td>
                  </tr>
                  <tr>
                    <td>
                      <strong>Impuesto General a las Ventas (IGV 16% + IPM 2%)</strong>
                      <span class="concept-sub">Tasa oficial SUNAT sobre base CIF + Ad-Valórem</span>
                    </td>
                    <td class="text-center font-mono">18.00%</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(cotizacion.montoIgv) }}</td>
                  </tr>
                  <tr v-if="Number(cotizacion.tasaPercepcion || 0) > 0 || Number(cotizacion.montoPercepcion || 0) > 0">
                    <td>
                      <strong>Percepción SUNAT</strong>
                      <span class="concept-sub">Régimen de percepción para importaciones</span>
                    </td>
                    <td class="text-center font-mono">{{ cotizacion.tasaPercepcion || 0 }}%</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(cotizacion.montoPercepcion) }}</td>
                  </tr>
                  <tr>
                    <td>
                      <strong>Honorarios de Agencia Aduanera (SGA)</strong>
                      <span class="concept-sub">Servicios integrales de agenciamiento aduanero</span>
                    </td>
                    <td class="text-center text-muted">—</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(cotizacion.montoHonorariosEstimado) }}</td>
                  </tr>
                  <tr>
                    <td>
                      <strong>Gastos Operativos & Administrativos</strong>
                      <span class="concept-sub">Trámites documentarios, cuadrilla y precintos</span>
                    </td>
                    <td class="text-center text-muted">—</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(cotizacion.gastosOperativos) }}</td>
                  </tr>
                  <tr v-if="Number(cotizacion.costoTransporteRuta || 0) > 0">
                    <td>
                      <strong>Flete Terrestre Nacional</strong>
                      <span class="concept-sub">Transporte local de carga (Ruta nacional)</span>
                    </td>
                    <td class="text-center text-muted">—</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(cotizacion.costoTransporteRuta) }}</td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr class="total-row">
                    <td colspan="2">
                      <span class="total-label">TOTAL ESTIMADO A LIQUIDAR (EXCLUYE FOB)</span>
                    </td>
                    <td class="text-right">
                      <span class="total-amount font-mono">${{ formatCurrency(cotizacion.totalCotizado) }} USD</span>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </section>

        <!-- Bloque 04: Observaciones Generales Registradas -->
        <section v-if="cotizacion.observaciones" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">04</div>
            <div class="block-header-info">
              <h3 class="block-title">Observaciones Comerciales & Instrucciones del Asesor</h3>
              <p class="block-sub">Anotaciones registradas por el ejecutivo de ventas para esta propuesta.</p>
            </div>
          </div>
          <div class="block-body">
            <p class="obs-full-text">{{ cotizacion.observaciones }}</p>
          </div>
        </section>

        <!-- Bloque 05: DOCUMENTOS SUSTENTATORIOS ADJUNTOS (VISTA COMPLETA) -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">05</div>
            <div class="block-header-info">
              <h3 class="block-title">Documentos Sustentatorios y Anexos Aduaneros</h3>
              <p class="block-sub">Facturas comerciales, listas de empaque (packing list), conocimientos de embarque y permisos sectoriales en tiempo real.</p>
            </div>
            <span class="panel-header-badge">GESTIÓN INTEGRADA</span>
          </div>

          <div class="block-body">
            <!-- Componente DocumentosUploadPanel Integrado Directamente -->
            <DocumentosUploadPanel
              mode="live"
              entidad-tipo="COTIZACION"
              :id-entidad="cotizacion.idCotizacion"
              :entidad-id="cotizacion.idCotizacion"
              :codigo-entidad="cotizacion.codigoCotizacion || `COT-${cotizacion.idCotizacion}`"
              :titulo-entidad="`${cotizacion.codigoCotizacion || 'Cotización'} — ${cotizacion.clienteRazonSocial || 'Cliente'}`"
              @updated="$emit('doc-updated')"
            />
          </div>
        </section>
      </div>

      <!-- Columna Lateral: Resumen Proforma & Checklist de Auditoría -->
      <aside class="form-summary-column">
        <!-- Tarjeta Proforma Oficial Resumida -->
        <div class="proforma-summary-card">
          <div class="card-kicker-row">
            <span class="proforma-badge-kicker">PROFORMA ADUANERA AUDITADA</span>
            <i class="pi pi-shield proforma-kicker-icon"></i>
          </div>

          <div class="proforma-code-box">
            <span class="proforma-code font-mono">{{ cotizacion.codigoCotizacion || 'COT-2026-BORRADOR' }}</span>
          </div>

          <div class="proforma-client-preview">
            <span class="p-preview-label">DESTINATARIO COMERCIAL:</span>
            <strong class="p-preview-name">{{ cotizacion.clienteRazonSocial || 'Cliente Solicitante' }}</strong>
            <span class="p-preview-doc font-mono">RUC: {{ cotizacion.clienteNumeroDocumento || '—' }}</span>
          </div>

          <div class="proforma-divider"></div>

          <div class="proforma-summary-lines">
            <div class="p-sum-line">
              <span class="p-line-label">Régimen:</span>
              <span class="p-line-val">{{ formatRegimen(cotizacion.regimenAduanero) }}</span>
            </div>
            <div class="p-sum-line">
              <span class="p-line-label">Vía:</span>
              <span class="p-line-val">{{ formatTransporte(cotizacion.viaTransporte) }}</span>
            </div>
            <div class="p-sum-line">
              <span class="p-line-label">Valor CIF Base:</span>
              <span class="p-line-val font-mono">${{ formatCurrency(cotizacion.valorCif) }}</span>
            </div>
            <div class="p-sum-line">
              <span class="p-line-label">Tributos SUNAT:</span>
              <span class="p-line-val font-mono">${{ formatCurrency(cotizacion.montoTributosEstimado) }}</span>
            </div>
            <div class="p-sum-line">
              <span class="p-line-label">Servicios SGA:</span>
              <span class="p-line-val font-mono">${{ formatCurrency((Number(cotizacion.montoHonorariosEstimado || 0) + Number(cotizacion.gastosOperativos || 0) + Number(cotizacion.costoTransporteRuta || 0))) }}</span>
            </div>
          </div>

          <div class="proforma-divider"></div>

          <div class="proforma-total-box">
            <span class="p-total-caption">TOTAL ESTIMADO (USD):</span>
            <span class="p-total-value font-mono">${{ formatCurrency(cotizacion.totalCotizado) }}</span>
            <span class="p-total-foot">Liquidación calculada para trámite aduanero</span>
          </div>
        </div>

        <!-- Tarjeta de Checklist de Validación Técnica Aduanera -->
        <div class="checklist-card">
          <div class="checklist-header">
            <i class="pi pi-check-square"></i>
            <span class="checklist-title">Checklist de Auditoría Técnica</span>
          </div>
          <p class="checklist-desc">
            Criterios de control antes de autorizar la propuesta o proceder a la apertura de expediente:
          </p>

          <div class="checklist-list">
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>RUC del solicitante activo y habido</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Régimen aduanero coherente con la mercancía</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Base imponible CIF sustentada técnicamente</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Tasa arancelaria Ad-Valórem validada</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Documentos comerciales mínimos adjuntos</span>
            </div>
          </div>

          <div class="checklist-audit-stamp">
            <i class="pi pi-verified"></i>
            <div>
              <strong>Supervisión Aduanera SGA</strong>
              <small>Validación de conformidad normativa</small>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import DocumentosUploadPanel from '@/shared/components/DocumentosUploadPanel.vue'

const props = defineProps({
  cotizacion: {
    type: Object,
    required: true
  },
  processing: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['back', 'approve', 'observe', 'reject', 'open-apertura', 'doc-updated'])

const showObservationBox = ref(false)
const observationReason = ref(props.cotizacion?.motivoObservacion || '')

const showRejectBox = ref(false)
const rejectReason = ref('')

const isAuditableState = computed(() => {
  const st = props.cotizacion?.estado
  return ['PENDIENTE', 'EN_REVISION', 'ENVIADA', 'REVISION', 'OBSERVADA'].includes(st)
})

const toggleObservationBox = () => {
  showRejectBox.value = false
  showObservationBox.value = !showObservationBox.value
}

const toggleRejectBox = () => {
  showObservationBox.value = false
  showRejectBox.value = !showRejectBox.value
}

const submitObservation = () => {
  if (!observationReason.value.trim()) return
  emit('observe', {
    cotizacion: props.cotizacion,
    reason: observationReason.value.trim()
  })
}

const submitReject = () => {
  if (!rejectReason.value.trim()) return
  emit('reject', {
    cotizacion: props.cotizacion,
    reason: rejectReason.value.trim()
  })
}

// Formatters
const formatCurrency = (val) => {
  return Number(val || 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  return String(dateStr).split('T')[0]
}

const formatRegimen = (regimen) => {
  if (!regimen) return 'Importación para el Consumo (REG-10)'
  return regimen.replace(/_/g, ' ')
}

const getRegimenCode = (regimen) => {
  if (!regimen) return 'REG-10'
  if (regimen.includes('10') || regimen.includes('CONSUMO')) return 'REG-10'
  if (regimen.includes('TEMPORAL')) return 'REG-20'
  if (regimen.includes('EXPORTACION')) return 'REG-40'
  if (regimen.includes('TRANSITO')) return 'REG-80'
  if (regimen.includes('DEPOSITO')) return 'REG-70'
  return 'REG-10'
}

const formatTipoDespacho = (despacho) => {
  if (!despacho) return 'Despacho Anticipado'
  return String(despacho).replace(/_/g, ' ')
}

const formatTransporte = (via) => {
  if (!via) return 'Marítimo'
  return String(via).replace(/_/g, ' ')
}

const getEstadoLabel = (estado) => {
  const map = {
    ENVIADA: 'Por Revisar',
    PENDIENTE: 'Por Revisar',
    EN_REVISION: 'En Evaluación Técnica',
    REVISION: 'Observada',
    OBSERVADA: 'Observada',
    APROBADA: 'Aprobada',
    RECHAZADA: 'Rechazada',
    ANULADA: 'Anulada'
  }
  return map[estado] || estado || 'Pendiente'
}

const getStatusBadgeClass = (estado) => {
  if (estado === 'APROBADA') return 'status-approved'
  if (estado === 'RECHAZADA' || estado === 'ANULADA') return 'status-rejected'
  if (estado === 'OBSERVADA' || estado === 'REVISION') return 'status-pending'
  return 'status-progress'
}

const getStatusIcon = (estado) => {
  if (estado === 'APROBADA') return 'pi pi-check-circle'
  if (estado === 'RECHAZADA' || estado === 'ANULADA') return 'pi pi-times-circle'
  if (estado === 'OBSERVADA' || estado === 'REVISION') return 'pi pi-exclamation-triangle'
  return 'pi pi-clock'
}
</script>

<style scoped>
.auditoria-expediente-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* 1. Header Bar */
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-5, 1.25rem);
  padding: var(--space-5, 1.25rem) var(--space-6, 1.5rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: var(--shadow-subtle);
  flex-wrap: wrap;
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 820px;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  padding: 0;
  margin-bottom: 0.15rem;
  transition: color var(--transition-fast);
}

.btn-back-link:hover {
  color: var(--color-primary);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.badge-kicker {
  font-size: 0.625rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-xs, 0.25rem);
  border: 1px solid var(--color-border-subtle);
}

.exp-linked-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--color-primary);
  background: var(--color-bg-muted);
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border-subtle);
}

.form-main-title {
  font-family: var(--font-sans);
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.015em;
  line-height: 1.25;
  margin: 0.15rem 0 0;
}

.form-main-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.45;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.status-lock-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  font-weight: 700;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
  padding: 0.45rem 0.85rem;
  border-radius: var(--radius-md);
}

/* 2. Inline Action Panels (Observar / Rechazar) */
.inline-action-panel {
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-5, 1.25rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
}

.observation-panel {
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
}

.reject-panel {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  font-size: 0.95rem;
  color: var(--color-text-main);
}

.btn-close-panel {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  padding: 0.25rem;
}

.panel-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.4;
}

.panel-input-box {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.panel-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.form-textarea {
  width: 100%;
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.85rem;
  outline: none;
  resize: vertical;
}

.form-textarea:focus {
  border-color: var(--color-primary);
}

.panel-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 0.65rem;
}

/* 3. Banner de Historial de Observaciones */
.observation-history-card {
  display: flex;
  gap: 1rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  align-items: flex-start;
}

.obs-icon-col {
  font-size: 1.35rem;
  color: var(--color-warning-text);
  margin-top: 0.15rem;
}

.obs-body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
}

.obs-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.obs-kicker {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-warning-text);
}

.obs-status {
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.45rem;
  background: var(--color-bg-surface);
  border-radius: var(--radius-pill);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.obs-text {
  font-size: 0.85rem;
  color: var(--color-text-main);
  margin: 0;
  font-weight: 600;
  line-height: 1.4;
}

.obs-note {
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

/* 4. Grid de Contenido */
.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: var(--space-6, 1.5rem);
  align-items: start;
}

@media (max-width: 1080px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}

.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  box-shadow: var(--shadow-subtle);
}

.block-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  margin-bottom: var(--space-6, 1.5rem);
  padding-bottom: var(--space-4, 1rem);
  border-bottom: 1px solid var(--color-border-subtle);
}

.block-idx {
  font-family: var(--font-mono);
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-subtle);
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md, 0.5rem);
  flex-shrink: 0;
}

.block-header-info {
  flex: 1;
}

.block-title {
  font-size: var(--font-size-md, 1rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 var(--space-1, 0.25rem) 0;
  letter-spacing: -0.01em;
}

.block-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
}

.panel-header-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  background: var(--color-bg-subtle);
  color: var(--color-accent-kicker);
  border: 1px solid var(--color-border);
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-sm, 0.25rem);
}

/* Grillas de Detalles */
.details-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4, 1rem);
}

@media (max-width: 768px) {
  .details-grid-3 {
    grid-template-columns: 1fr;
  }
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.detail-item.full-width-sm {
  grid-column: span 2;
}

@media (max-width: 768px) {
  .detail-item.full-width-sm {
    grid-column: span 1;
  }
}

.detail-label {
  font-size: 0.725rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.detail-val {
  font-size: 0.875rem;
  color: var(--color-text-main);
}

.detail-val.strong {
  font-weight: 700;
}

.badge-type {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.15rem 0.55rem;
  border-radius: var(--radius-pill);
}

.type-cliente {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.type-prospecto {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.regimen-tag-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 0.35rem 0.65rem;
}

.regimen-code-pill {
  font-size: 0.725rem;
  font-weight: 800;
  color: var(--color-primary);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.regimen-name-text {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.route-badge-box {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 0.4rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-main);
  font-weight: 600;
}

.route-arrow {
  color: var(--color-accent-kicker);
  font-size: 0.75rem;
}

/* CIF Cards Row */
.cif-cards-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

@media (max-width: 900px) {
  .cif-cards-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.cif-card {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.cif-card.highlight {
  background: var(--color-bg-muted);
  border-color: var(--color-border-strong);
}

.cif-card-label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
}

.cif-card-val {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.cif-card.highlight .cif-card-val {
  color: var(--color-primary);
}

.cif-card-sub {
  font-size: 0.6875rem;
  color: var(--color-text-subtle);
}

/* Tabla de Liquidación Financiera */
.financial-table-wrapper {
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.financial-breakdown-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.84rem;
}

.financial-breakdown-table th {
  background: var(--color-bg-subtle);
  padding: 0.65rem 0.85rem;
  color: var(--color-text-muted);
  font-size: 0.725rem;
  font-weight: 700;
  text-transform: uppercase;
  border-bottom: 1px solid var(--color-border);
}

.financial-breakdown-table td {
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid var(--color-border-subtle);
  color: var(--color-text-main);
}

.concept-sub {
  display: block;
  font-size: 0.725rem;
  color: var(--color-text-muted);
  font-weight: 400;
}

.total-row {
  background: var(--color-bg-subtle);
  font-weight: 800;
}

.total-label {
  font-size: 0.75rem;
  letter-spacing: 0.04em;
  color: var(--color-text-main);
}

.total-amount {
  font-size: 1.15rem;
  color: var(--color-primary);
}

.obs-full-text {
  font-size: 0.875rem;
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 0.85rem 1rem;
  margin: 0;
  line-height: 1.5;
}

/* Columna Lateral (Resumen y Checklist) */
.form-summary-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.proforma-summary-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem);
  box-shadow: var(--shadow-subtle);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.card-kicker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.proforma-badge-kicker {
  font-size: 0.625rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.proforma-kicker-icon {
  font-size: 0.9rem;
  color: var(--color-accent-kicker);
}

.proforma-code-box {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.45rem 0.75rem;
  text-align: center;
}

.proforma-code {
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--color-primary);
  letter-spacing: 0.04em;
}

.proforma-client-preview {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.p-preview-label {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.p-preview-name {
  font-size: 0.85rem;
  color: var(--color-text-main);
}

.p-preview-doc {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
}

.proforma-divider {
  height: 1px;
  background: var(--color-border-subtle);
}

.proforma-summary-lines {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.p-sum-line {
  display: flex;
  justify-content: space-between;
  font-size: 0.785rem;
}

.p-line-label {
  color: var(--color-text-muted);
}

.p-line-val {
  color: var(--color-text-main);
  font-weight: 600;
}

.proforma-total-box {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  text-align: center;
}

.p-total-caption {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.p-total-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-primary);
}

.p-total-foot {
  font-size: 0.65rem;
  color: var(--color-text-subtle);
}

/* Checklist Card */
.checklist-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.checklist-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.checklist-desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.35;
}

.checklist-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.checklist-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.785rem;
  color: var(--color-text-main);
}

.checklist-row.valid i {
  color: var(--color-success-text);
  font-size: 0.85rem;
}

.checklist-audit-stamp {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  border-top: 1px dashed var(--color-border);
  padding-top: 0.65rem;
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

.checklist-audit-stamp i {
  font-size: 1.25rem;
  color: var(--color-primary);
}
</style>
