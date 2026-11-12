<template>
  <div class="expedientes-view">
    <!-- Header de Sección -->
    <div class="view-header">
      <div class="header-titles">
        <span class="view-kicker">PORTAL EMPRESARIAL B2B — TRACKING LOGÍSTICO</span>
        <h1 class="view-title">Tablero de Expedientes Aduaneros</h1>
        <p class="view-subtitle">
          Supervise sus operaciones en curso y consulte el historial de expedientes finalizados con su bóveda documental oficial.
        </p>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="cargarExpedientes" :disabled="loading">
          <i class="pi pi-refresh" :class="{ 'pi-spin': loading }"></i>
          <span>Actualizar Tablero</span>
        </button>
      </div>
    </div>

    <!-- Métricas KPI Warm Editorial (01..04) -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">01</span>
          <div class="kpi-icon-box">
            <i class="pi pi-folder"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">TOTAL OPERACIONES</span>
          <span class="kpi-value font-mono">{{ expedientes.length }}</span>
        </div>
      </div>

      <div class="kpi-card highlighted">
        <div class="kpi-top">
          <span class="kpi-idx">02</span>
          <div class="kpi-icon-box accent">
            <i class="pi pi-sync"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">EXPEDIENTES ABIERTOS</span>
          <span class="kpi-value font-mono">{{ expedientesAbiertos.length }}</span>
        </div>
        <div class="kpi-note">Operaciones vivas en trámite</div>
      </div>

      <div class="kpi-card success-card">
        <div class="kpi-top">
          <span class="kpi-idx">03</span>
          <div class="kpi-icon-box success">
            <i class="pi pi-check-circle"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">HISTORIAL FINALIZADOS</span>
          <span class="kpi-value font-mono">{{ expedientesFinalizados.length }}</span>
        </div>
        <div class="kpi-note">Carpetas cerradas con conformidad</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-top">
          <span class="kpi-idx">04</span>
          <div class="kpi-icon-box warning">
            <i class="pi pi-shield"></i>
          </div>
        </div>
        <div class="kpi-content">
          <span class="kpi-label">LEVANTE CONCEDIDO</span>
          <span class="kpi-value font-mono">{{ totalConLevante }}</span>
        </div>
        <div class="kpi-note">Listos para retiro o entrega</div>
      </div>
    </div>

    <!-- Barra de Filtros y Búsqueda -->
    <div class="kanban-toolbar">
      <div class="search-box">
        <i class="pi pi-search"></i>
        <input
          v-model.trim="searchQuery"
          type="text"
          placeholder="Buscar por N° Expediente, DAM, contenedor, mercancía o nave..."
          class="search-input"
        />
        <button
          v-if="searchQuery"
          type="button"
          class="clear-search"
          @click="searchQuery = ''"
          title="Limpiar búsqueda"
        >
          <i class="pi pi-times"></i>
        </button>
      </div>

      <div class="filters-group">
        <!-- Filtro Régimen -->
        <div class="filter-select-wrap">
          <i class="pi pi-compass filter-icon"></i>
          <select v-model="filterRegimen" class="filter-select">
            <option value="TODOS">Todos los Regímenes</option>
            <option value="IMPORTACION">Importación</option>
            <option value="EXPORTACION">Exportación</option>
          </select>
        </div>

        <!-- Filtro Canal SUNAT -->
        <div class="filter-select-wrap">
          <i class="pi pi-tag filter-icon"></i>
          <select v-model="filterCanal" class="filter-select">
            <option value="TODOS">Todos los Canales</option>
            <option value="VERDE">Canal Verde</option>
            <option value="NARANJA">Canal Naranja</option>
            <option value="ROJO">Canal Rojo</option>
            <option value="PENDIENTE">Canal Pendiente</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Estado de Carga -->
    <div v-if="loading" class="loading-state">
      <i class="pi pi-spin pi-spinner"></i>
      <span>Cargando tablero kanban de despachos aduaneros...</span>
    </div>

    <!-- Estado Vacío General -->
    <div v-else-if="expedientes.length === 0" class="empty-state">
      <i class="pi pi-box"></i>
      <h3>No registra expedientes aduaneros</h3>
      <p>Sus cotizaciones aprobadas con pago validado pasarán a este tablero en cuanto se aperture la carpeta aduanera.</p>
    </div>

    <!-- TABLERO KANBAN DE 2 COLUMNAS: ABIERTOS vs FINALIZADOS -->
    <div v-else class="kanban-board">
      <!-- ============================================================== -->
      <!-- COLUMNA 1: EXPEDIENTES ABIERTOS (EN CURSO)                     -->
      <!-- ============================================================== -->
      <section class="kanban-column abiertos-col">
        <div class="column-header">
          <div class="header-left">
            <div class="col-dot dot-abierto"></div>
            <h2 class="col-title">Expedientes Abiertos</h2>
            <span class="count-badge font-mono">{{ filteredAbiertos.length }}</span>
          </div>
          <span class="col-hint">Despachos en curso</span>
        </div>

        <div v-if="filteredAbiertos.length === 0" class="column-empty">
          <i class="pi pi-check-circle text-muted"></i>
          <p>No hay expedientes abiertos que coincidan con los filtros.</p>
        </div>

        <div v-else class="cards-list">
          <article
            v-for="exp in filteredAbiertos"
            :key="exp.idExpediente"
            class="kanban-card card-abierto"
            tabindex="0"
            role="button"
            @click="abrirDetalle(exp)"
            @keydown.enter="abrirDetalle(exp)"
            @keydown.space.prevent="abrirDetalle(exp)"
          >
            <!-- Cabecera de la Tarjeta -->
            <div class="card-top-row">
              <span class="exp-code font-mono">{{ exp.codigoExpediente }}</span>
              <span class="canal-pill" :class="exp.canalSunat?.toLowerCase() || 'pendiente'">
                Canal {{ exp.canalSunat || 'PENDIENTE' }}
              </span>
            </div>

            <!-- Título de Carga y Régimen -->
            <div class="card-cargo-row">
              <h3 class="card-cargo-title">{{ exp.nombreMercancia || 'Carga General' }}</h3>
              <span class="regimen-tag" :class="isExport(exp) ? 'export' : 'import'">
                {{ isExport(exp) ? 'Exportación' : 'Importación' }}
              </span>
            </div>

            <!-- Fila DAM y Nave -->
            <div class="card-meta-grid">
              <div class="meta-item">
                <span class="meta-label">DAM SUNAT:</span>
                <span class="meta-val font-mono">{{ exp.numeroDam || 'En trámite' }}</span>
              </div>
              <div class="meta-item" v-if="exp.naveViaje">
                <span class="meta-label">Nave / Viaje:</span>
                <span class="meta-val truncate" :title="exp.naveViaje">{{ exp.naveViaje }}</span>
              </div>
              <div class="meta-item" v-if="exp.numeroContenedor">
                <span class="meta-label">Contenedor:</span>
                <span class="meta-val font-mono">{{ exp.numeroContenedor }}</span>
              </div>
            </div>

            <!-- Etapa Operativa Actual -->
            <div class="phase-indicator">
              <span class="phase-pill" :class="getPhaseClass(exp)">
                <i :class="getPhaseIcon(exp)"></i>
                <span>{{ getPhaseLabel(exp) }}</span>
              </span>
            </div>

            <!-- Alerta de Almacén si aplica -->
            <div
              v-if="exp.tieneAlmacen && exp.diasRestantesAlmacen != null"
              class="card-alert"
              :class="getDaysAlertClass(exp.diasRestantesAlmacen)"
            >
              <i class="pi pi-clock"></i>
              <span>{{ exp.diasRestantesAlmacen }} días libres de sobreestadía</span>
            </div>

            <!-- Footer con Fecha y Acción -->
            <div class="card-footer">
              <span class="card-date font-mono">
                <i class="pi pi-calendar"></i>
                {{ formatDate(exp.fechaApertura) }}
              </span>
              <button type="button" class="btn-card-action" @click.stop="abrirDetalle(exp)">
                <span>Ver Tracking</span>
                <i class="pi pi-arrow-right"></i>
              </button>
            </div>
          </article>
        </div>
      </section>

      <!-- ============================================================== -->
      <!-- COLUMNA 2: EXPEDIENTES FINALIZADOS (HISTORIAL CERRADO)          -->
      <!-- ============================================================== -->
      <section class="kanban-column finalizados-col">
        <div class="column-header">
          <div class="header-left">
            <div class="col-dot dot-finalizado"></div>
            <h2 class="col-title">Expedientes Finalizados</h2>
            <span class="count-badge font-mono completed">{{ filteredFinalizados.length }}</span>
          </div>
          <span class="col-hint">Historial culminado</span>
        </div>

        <div v-if="filteredFinalizados.length === 0" class="column-empty">
          <i class="pi pi-folder-open text-muted"></i>
          <p>No se registran expedientes finalizados en el archivo histórico.</p>
        </div>

        <div v-else class="cards-list">
          <article
            v-for="exp in filteredFinalizados"
            :key="exp.idExpediente"
            class="kanban-card card-finalizado"
            tabindex="0"
            role="button"
            @click="abrirDetalle(exp)"
            @keydown.enter="abrirDetalle(exp)"
            @keydown.space.prevent="abrirDetalle(exp)"
          >
            <!-- Cabecera de la Tarjeta -->
            <div class="card-top-row">
              <span class="exp-code font-mono">{{ exp.codigoExpediente }}</span>
              <span class="status-pill-closed">
                <i class="pi pi-check-circle"></i>
                <span>CERRADO</span>
              </span>
            </div>

            <!-- Título de Carga y Régimen -->
            <div class="card-cargo-row">
              <h3 class="card-cargo-title">{{ exp.nombreMercancia || 'Carga General' }}</h3>
              <span class="regimen-tag" :class="isExport(exp) ? 'export' : 'import'">
                {{ isExport(exp) ? 'Exportación' : 'Importación' }}
              </span>
            </div>

            <!-- DAM y Canal Definitivo -->
            <div class="card-meta-grid">
              <div class="meta-item">
                <span class="meta-label">DAM SUNAT:</span>
                <span class="meta-val font-mono font-bold">{{ exp.numeroDam || 'N/A' }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Canal Oficial:</span>
                <span class="canal-mini-pill" :class="exp.canalSunat?.toLowerCase() || 'verde'">
                  {{ exp.canalSunat || 'VERDE' }}
                </span>
              </div>
              <div class="meta-item full-width" v-if="exp.agenteAduaneroNombre">
                <span class="meta-label">Agente Responsable:</span>
                <span class="meta-val">{{ exp.agenteAduaneroNombre }}</span>
              </div>
            </div>

            <!-- Métricas de Cierre y Lead Time -->
            <div class="history-metrics">
              <div class="history-metric-box">
                <span class="h-label">Apertura</span>
                <span class="h-val font-mono">{{ formatDate(exp.fechaApertura) }}</span>
              </div>
              <div class="history-metric-divider">
                <i class="pi pi-arrow-right"></i>
              </div>
              <div class="history-metric-box">
                <span class="h-label">Cierre Formal</span>
                <span class="h-val font-mono">{{ formatDate(exp.fechaCierre) || 'Conforme' }}</span>
              </div>
              <div class="history-lead-time font-mono" v-if="calculateLeadTime(exp)">
                <span>{{ calculateLeadTime(exp) }}</span>
              </div>
            </div>

            <!-- Footer con Bóveda Digital -->
            <div class="card-footer">
              <span class="card-date font-mono text-muted">
                <i class="pi pi-lock"></i>
                Carpeta Archivada
              </span>
              <button type="button" class="btn-card-action history" @click.stop="abrirDetalle(exp)">
                <span>Ver Historial & Bóveda</span>
                <i class="pi pi-folder-open"></i>
              </button>
            </div>
          </article>
        </div>
      </section>
    </div>

    <!-- ============================================================== -->
    <!-- MODAL DE DETALLE INTEGRAL Y TRACKING DAM (BaseModal)           -->
    <!-- ============================================================== -->
    <BaseModal
      :visible="showDetailModal"
      :title="`Expediente: ${selectedExpediente?.codigoExpediente || ''}`"
      :subtitle="`DAM: ${selectedExpediente?.numeroDam || 'En trámite de numeración'} — ${selectedExpediente?.nombreMercancia || 'Carga General'}`"
      icon="pi pi-box"
      max-width="960px"
      @close="cerrarDetalle"
    >
      <div v-if="selectedExpediente" class="detail-modal-body">
        <!-- Cabecera de la Declaración -->
        <div class="modal-dam-header">
          <div class="dam-header-texts">
            <span class="kicker-exp">DECLARACIÓN ADUANERA DE MERCANCÍAS</span>
            <h2 class="dam-title font-mono">{{ selectedExpediente.numeroDam || 'En Proceso de Numeración' }}</h2>
            <div class="exp-codes-sub">
              <span class="code-badge font-mono">{{ selectedExpediente.codigoExpediente }}</span>
              <span>• Cotización Ref: <strong class="font-mono">{{ selectedExpediente.codigoCotizacion }}</strong></span>
              <span>• Agente SGA: <strong>{{ selectedExpediente.agenteAduaneroNombre || 'Asignado' }}</strong></span>
            </div>
          </div>

          <!-- Semáforo Oficial Canal SUNAT -->
          <div class="sunat-traffic-light" :class="selectedExpediente.canalSunat?.toLowerCase() || 'pendiente'">
            <div class="light-icon">
              <i v-if="selectedExpediente.canalSunat === 'VERDE'" class="pi pi-check-circle"></i>
              <i v-else-if="selectedExpediente.canalSunat === 'NARANJA'" class="pi pi-file"></i>
              <i v-else-if="selectedExpediente.canalSunat === 'ROJO'" class="pi pi-search"></i>
              <i v-else class="pi pi-clock"></i>
            </div>
            <div class="light-text">
              <span class="light-label">CANAL SUNAT</span>
              <span class="light-val">{{ selectedExpediente.canalSunat || 'PENDIENTE' }}</span>
              <span class="light-desc">{{ getCanalDescription(selectedExpediente.canalSunat) }}</span>
            </div>
          </div>
        </div>

        <!-- Línea de Tiempo Operativa (Stepper Dinámico) -->
        <div class="stepper-section">
          <h4 class="section-title">Línea de Tiempo Operativa</h4>
          <div class="stepper-container">
            <!-- Paso 1: Cotización -->
            <div class="step completed">
              <div class="step-circle"><i class="pi pi-check"></i></div>
              <div class="step-label">Cotización</div>
              <div class="step-date">Aprobada</div>
            </div>

            <div class="step-line active"></div>

            <!-- Paso 2: Salida / Tránsito -->
            <div class="step" :class="{ completed: isZarpeConfirmado(selectedExpediente), active: !isZarpeConfirmado(selectedExpediente) }">
              <div class="step-circle">
                <i v-if="isZarpeConfirmado(selectedExpediente)" class="pi pi-check"></i>
                <span v-else>2</span>
              </div>
              <div class="step-label">Salida Origen</div>
              <div class="step-date">{{ isZarpeConfirmado(selectedExpediente) ? (selectedExpediente.fechaZarpeEtd ? formatDate(selectedExpediente.fechaZarpeEtd) : 'Confirmado') : 'Por confirmar' }}</div>
            </div>

            <div class="step-line" :class="{ active: isZarpeConfirmado(selectedExpediente) }"></div>

            <!-- Paso 3: Arribo a Destino -->
            <div class="step" :class="{ completed: selectedExpediente.fechaArriboAduana, active: selectedExpediente.fechaZarpeEtd && !selectedExpediente.fechaArriboAduana }">
              <div class="step-circle">
                <i v-if="selectedExpediente.fechaArriboAduana" class="pi pi-check"></i>
                <span v-else>3</span>
              </div>
              <div class="step-label">Arribo Puerto</div>
              <div class="step-date">{{ selectedExpediente.fechaArriboAduana ? formatDateTime(selectedExpediente.fechaArriboAduana) : 'En navegación' }}</div>
            </div>

            <div class="step-line" :class="{ active: selectedExpediente.numeroDam }"></div>

            <!-- Paso 4: Numeración DAM y Canal -->
            <div class="step" :class="{ completed: selectedExpediente.numeroDam, active: selectedExpediente.fechaArriboAduana && !selectedExpediente.numeroDam }">
              <div class="step-circle">
                <i v-if="selectedExpediente.numeroDam" class="pi pi-check"></i>
                <span v-else>4</span>
              </div>
              <div class="step-label">Canal SUNAT</div>
              <div class="step-date">{{ selectedExpediente.canalSunat || 'Asignando' }}</div>
            </div>

            <div class="step-line" :class="{ active: selectedExpediente.fechaLevanteAutorizacion }"></div>

            <!-- Paso 5: Levante Aduanero -->
            <div class="step" :class="{ completed: selectedExpediente.fechaLevanteAutorizacion, active: selectedExpediente.numeroDam && !selectedExpediente.fechaLevanteAutorizacion }">
              <div class="step-circle">
                <i v-if="selectedExpediente.fechaLevanteAutorizacion" class="pi pi-check"></i>
                <span v-else>5</span>
              </div>
              <div class="step-label">Levante SUNAT</div>
              <div class="step-date">{{ selectedExpediente.fechaLevanteAutorizacion ? 'Autorizado' : 'En aforo' }}</div>
            </div>

            <div class="step-line" :class="{ active: selectedExpediente.estadoExpediente === 'CERRADO' }"></div>

            <!-- Paso 6: Conformidad y Cierre -->
            <div class="step" :class="{ completed: selectedExpediente.estadoExpediente === 'CERRADO' }">
              <div class="step-circle">
                <i v-if="selectedExpediente.estadoExpediente === 'CERRADO'" class="pi pi-check"></i>
                <span v-else>6</span>
              </div>
              <div class="step-label">Cierre Formal</div>
              <div class="step-date">{{ selectedExpediente.fechaCierre ? formatDate(selectedExpediente.fechaCierre) : 'En curso' }}</div>
            </div>
          </div>
        </div>

        <!-- Bloques Operativos: Almacén y Transporte -->
        <div class="operations-grid">
          <!-- Bloque 1: Almacén Temporal -->
          <div class="operation-card">
            <div class="op-card-header">
              <div class="op-icon warehouse">
                <i class="pi pi-building"></i>
              </div>
              <div>
                <h4 class="op-title">Depósito Temporal & Almacenaje</h4>
                <span class="op-sub">{{ selectedExpediente.depositoTemporal || 'DP World / APM Terminals' }}</span>
              </div>
            </div>

            <div class="op-card-body" v-if="selectedExpediente.tieneAlmacen">
              <div class="op-metric-row">
                <span class="op-lbl">Ubicación / Bahía:</span>
                <span class="op-val font-mono">{{ selectedExpediente.ubicacionBahia || 'Bahía Primaria' }}</span>
              </div>
              <div class="op-metric-row">
                <span class="op-lbl">Días Libres Pactados:</span>
                <span class="op-val font-mono font-bold">{{ selectedExpediente.diasLibresAlmacen || 15 }} días</span>
              </div>
              <div class="warehouse-countdown-alert" :class="getDaysAlertClass(selectedExpediente.diasRestantesAlmacen)">
                <div class="alert-icon">
                  <i class="pi pi-clock"></i>
                </div>
                <div class="alert-info">
                  <div class="days-remaining">
                    <strong>{{ selectedExpediente.diasRestantesAlmacen != null ? selectedExpediente.diasRestantesAlmacen : 15 }} días restantes</strong>
                    <span>de sobreestadía libre</span>
                  </div>
                  <div class="date-deadline font-mono">
                    Fecha límite de retiro: <strong>{{ selectedExpediente.fechaLimiteRetiro || 'Consultar agente' }}</strong>
                  </div>
                </div>
              </div>
            </div>

            <div class="op-card-empty" v-else>
              <i class="pi pi-info-circle"></i>
              <span>Carga con despacho directo o en tránsito internacional hacia puerto de destino.</span>
            </div>
          </div>

          <!-- Bloque 2: Transporte Terrestre -->
          <div class="operation-card">
            <div class="op-card-header">
              <div class="op-icon transport">
                <i class="pi pi-truck"></i>
              </div>
              <div>
                <h4 class="op-title">Transporte Local & Distribución</h4>
                <span class="op-sub">Despacho de última milla a planta</span>
              </div>
            </div>

            <div class="op-card-body" v-if="selectedExpediente.tieneTransporte">
              <div class="op-metric-row">
                <span class="op-lbl">Código de Traslado:</span>
                <span class="op-val font-mono font-bold">{{ selectedExpediente.codigoTracking || 'DSP-ACTIVO' }}</span>
              </div>
              <div class="op-metric-row">
                <span class="op-lbl">Dirección de Entrega:</span>
                <span class="op-val">{{ selectedExpediente.direccionLlegada || 'Dirección de planta del cliente' }}</span>
              </div>
              <div class="op-metric-row" v-if="selectedExpediente.fechaProgramadaTransporte">
                <span class="op-lbl">Fecha Programada:</span>
                <span class="op-val font-mono">{{ formatDateTime(selectedExpediente.fechaProgramadaTransporte) }}</span>
              </div>
              <div class="op-metric-row">
                <span class="op-lbl">Estado de Traslado:</span>
                <span class="status-pill-transporte">{{ selectedExpediente.estadoTransporte || 'PROGRAMADO' }}</span>
              </div>
            </div>

            <div class="op-card-empty" v-else>
              <i class="pi pi-info-circle"></i>
              <span>Coordinación de flota terrestre o retiro en terminal una vez validado el levante aduanero.</span>
            </div>
          </div>
        </div>

        <!-- Bóveda Documental Conectada -->
        <div class="documents-section">
          <div class="docs-section-header">
            <div>
              <h4 class="section-title">Bóveda Documental del Despacho</h4>
              <span class="section-sub">Documentación digital, timbrado DAM de SUNAT y actas de levante.</span>
            </div>
            <span class="docs-count font-mono" v-if="documentosExpediente.length > 0">
              {{ documentosExpediente.length }} archivo(s) digital(es)
            </span>
          </div>

          <div v-if="loadingDocs" class="docs-loading">
            <i class="pi pi-spin pi-spinner"></i>
            <span>Consultando bóveda de documentos digitales...</span>
          </div>

          <div v-else-if="documentosExpediente.length === 0" class="docs-empty-state">
            <i class="pi pi-folder-open"></i>
            <p>No se registran documentos adicionales adjuntos en el expediente.</p>
            <span class="sub-hint">Los documentos DAM timbrados por SUNAT aparecerán aquí en cuanto sean emitidos.</span>
          </div>

          <div v-else class="docs-list">
            <div
              v-for="doc in documentosExpediente"
              :key="doc.idDocumento"
              class="doc-item"
            >
              <div class="doc-icon">
                <i :class="getDocumentIcon(doc.formatoMime)"></i>
              </div>
              <div class="doc-meta">
                <span class="doc-name">{{ doc.nombreOriginal || doc.descripcion || 'Documento Oficial' }}</span>
                <span class="doc-sub font-mono">
                  {{ formatFileSize(doc.tamanoBytes) }} • {{ formatDate(doc.fechaSubida) }}
                </span>
              </div>
              <div class="doc-actions">
                <button
                  type="button"
                  class="btn-doc-action view"
                  title="Previsualizar en el Navegador"
                  @click="handlePrevisualizar(doc)"
                >
                  <i class="pi pi-eye"></i>
                  <span>Ver</span>
                </button>
                <button
                  type="button"
                  class="btn-doc-action download"
                  title="Descargar Archivo Oficial"
                  @click="handleDescargar(doc)"
                >
                  <i class="pi pi-download"></i>
                  <span>Descargar</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { portalExpedienteService } from '../services/portalExpedienteService'
import { useNotification } from '@/shared/composables/useNotification'
import { downloadBlob, openBlobInNewTab } from '@/shared/utils/download'
import {
  formatBytes as formatFileSize,
  formatDate,
  formatDateTime
} from '@/shared/utils/formatters'

const { showSuccess, showError } = useNotification()

const expedientes = ref([])
const loading = ref(true)

// Modal y selección
const selectedExpediente = ref(null)
const showDetailModal = ref(false)
const documentosExpediente = ref([])
const loadingDocs = ref(false)

// Filtros
const searchQuery = ref('')
const filterRegimen = ref('TODOS')
const filterCanal = ref('TODOS')

onMounted(() => {
  cargarExpedientes()
})

async function cargarExpedientes() {
  loading.value = true
  try {
    const data = await portalExpedienteService.listarExpedientes()
    expedientes.value = data || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cargar los expedientes aduaneros')
  } finally {
    loading.value = false
  }
}

// Clasificación de expedientes en Abiertos vs Finalizados
const expedientesAbiertos = computed(() => {
  return expedientes.value.filter(
    (e) => e.estadoExpediente !== 'CERRADO' && e.estadoExpediente !== 'ANULADO'
  )
})

const expedientesFinalizados = computed(() => {
  return expedientes.value.filter((e) => e.estadoExpediente === 'CERRADO')
})

const totalConLevante = computed(() => {
  return expedientes.value.filter((e) => Boolean(e.fechaLevanteAutorizacion) || e.estadoExpediente === 'LEVANTE_AUTORIZADO').length
})

// Filtro reactivo universal
function coincideFiltro(exp) {
  // Filtro de texto
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    const matchCode = (exp.codigoExpediente || '').toLowerCase().includes(q)
    const matchDam = (exp.numeroDam || '').toLowerCase().includes(q)
    const matchCargo = (exp.nombreMercancia || '').toLowerCase().includes(q)
    const matchCont = (exp.numeroContenedor || '').toLowerCase().includes(q)
    const matchNave = (exp.naveViaje || '').toLowerCase().includes(q)
    if (!matchCode && !matchDam && !matchCargo && !matchCont && !matchNave) {
      return false
    }
  }

  // Filtro de Régimen
  if (filterRegimen.value !== 'TODOS') {
    const isExp = isExport(exp)
    if (filterRegimen.value === 'IMPORTACION' && isExp) return false
    if (filterRegimen.value === 'EXPORTACION' && !isExp) return false
  }

  // Filtro de Canal
  if (filterCanal.value !== 'TODOS') {
    const canal = (exp.canalSunat || 'PENDIENTE').toUpperCase()
    if (canal !== filterCanal.value) return false
  }

  return true
}

const filteredAbiertos = computed(() => {
  return expedientesAbiertos.value.filter(coincideFiltro)
})

const filteredFinalizados = computed(() => {
  return expedientesFinalizados.value.filter(coincideFiltro)
})

function isExport(exp) {
  const reg = (exp?.regimenAduanero || exp?.nombreTipoCarga || '').toUpperCase()
  return reg.includes('EXPORT')
}

function isZarpeConfirmado(exp) {
  if (!exp) return false
  const obs = exp.observaciones || ''
  if (obs.includes('SALIDA ORIGEN CONFIRMADA') || obs.includes('EMBARQUE CONFIRMADO')) return true
  if (exp.fechaArriboAduana || (exp.canalSunat && exp.canalSunat !== 'PENDIENTE') || exp.fechaLevanteAutorizacion) return true
  if (exp.estadoExpediente === 'ASIGNADO') return false
  return exp.estadoExpediente !== 'ASIGNADO'
}

// Determinación de la fase operativa actual para la tarjeta de abiertos
function getPhaseLabel(exp) {
  if (exp.estadoExpediente === 'LEVANTE_AUTORIZADO' || exp.fechaLevanteAutorizacion) {
    return 'Levante Autorizado'
  }
  if (exp.estadoExpediente === 'INSPECCION' || exp.canalSunat === 'NARANJA' || exp.canalSunat === 'ROJO') {
    return exp.canalSunat === 'ROJO' ? 'Aforo Físico SUNAT' : 'Revisión Documentaria'
  }
  if (exp.fechaArriboAduana) {
    return 'Arribado en Puerto (Esperando Canal)'
  }
  if (isZarpeConfirmado(exp)) {
    return 'En Tránsito Internacional'
  }
  return 'Apertura / Pendiente Confirmación de Salida'
}

function getPhaseIcon(exp) {
  if (exp.estadoExpediente === 'LEVANTE_AUTORIZADO' || exp.fechaLevanteAutorizacion) {
    return 'pi pi-check-circle'
  }
  if (exp.estadoExpediente === 'INSPECCION' || exp.canalSunat === 'NARANJA' || exp.canalSunat === 'ROJO') {
    return 'pi pi-search'
  }
  if (exp.fechaArriboAduana) {
    return 'pi pi-compass'
  }
  if (isZarpeConfirmado(exp)) {
    return 'pi pi-send'
  }
  return 'pi pi-clock'
}

function getPhaseClass(exp) {
  if (exp.estadoExpediente === 'LEVANTE_AUTORIZADO' || exp.fechaLevanteAutorizacion) {
    return 'phase-levante'
  }
  if (exp.estadoExpediente === 'INSPECCION' || exp.canalSunat === 'NARANJA' || exp.canalSunat === 'ROJO') {
    return 'phase-inspeccion'
  }
  if (exp.fechaArriboAduana) {
    return 'phase-arribado'
  }
  if (isZarpeConfirmado(exp)) {
    return 'phase-transito'
  }
  return 'phase-apertura'
}

function calculateLeadTime(exp) {
  if (!exp.fechaApertura || !exp.fechaCierre) return null
  try {
    const inicio = new Date(exp.fechaApertura)
    const fin = new Date(exp.fechaCierre)
    const diffMs = fin - inicio
    const dias = Math.max(1, Math.round(diffMs / (1000 * 60 * 60 * 24)))
    return `${dias} días de ciclo`
  } catch {
    return null
  }
}

function abrirDetalle(exp) {
  selectedExpediente.value = exp
  showDetailModal.value = true
}

function cerrarDetalle() {
  showDetailModal.value = false
}

// Bóveda Documental
watch(
  () => selectedExpediente.value?.idExpediente,
  async (newId) => {
    if (newId) {
      await cargarDocumentos(newId)
    } else {
      documentosExpediente.value = []
    }
  }
)

async function cargarDocumentos(idExpediente) {
  loadingDocs.value = true
  try {
    const docs = await portalExpedienteService.listarDocumentosExpediente(idExpediente)
    documentosExpediente.value = docs || []
  } catch (err) {
    console.debug('Error al listar documentos del expediente:', err)
    documentosExpediente.value = []
  } finally {
    loadingDocs.value = false
  }
}

async function handleDescargar(doc) {
  try {
    const blob = await portalExpedienteService.descargarDocumento(doc.idDocumento)
    downloadBlob(blob, doc.nombreOriginal || `documento-${doc.idDocumento}.pdf`, doc.formatoMime || 'application/pdf')
    showSuccess(`Descargando ${doc.nombreOriginal || 'documento'}`, 'Bóveda Digital')
  } catch {
    showError('Error al descargar el documento del servidor')
  }
}

async function handlePrevisualizar(doc) {
  try {
    const blob = await portalExpedienteService.previsualizarDocumento(doc.idDocumento)
    openBlobInNewTab(blob, doc.formatoMime || 'application/pdf')
  } catch {
    showError('Error al previsualizar el documento en el navegador')
  }
}

function getDocumentIcon(mime) {
  if (!mime) return 'pi pi-file'
  if (mime.includes('pdf')) return 'pi pi-file-pdf'
  if (mime.includes('image')) return 'pi pi-image'
  if (mime.includes('sheet') || mime.includes('excel')) return 'pi pi-file-excel'
  return 'pi pi-file'
}

function getCanalDescription(canal) {
  switch (canal) {
    case 'VERDE':
      return 'Levante automático e inmediato sin revisión documental ni física'
    case 'NARANJA':
      return 'Revisión documentaria técnica obligatoria ante especialista SUNAT'
    case 'ROJO':
      return 'Reconocimiento físico obligatorio de bultos en depósito temporal'
    default:
      return 'Pendiente de asignación por el sistema SUNAT'
  }
}

function getDaysAlertClass(days) {
  if (days == null) return 'normal'
  if (days <= 3) return 'danger'
  if (days <= 7) return 'warning'
  return 'normal'
}
</script>

<style scoped>
.expedientes-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

/* Header */
.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.view-kicker {
  display: block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker, #c2410c);
  margin-bottom: 0.25rem;
}

.view-title {
  font-size: 1.625rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-main, #f6f2ea);
  margin: 0 0 0.25rem 0;
}

.view-subtitle {
  font-size: 0.875rem;
  color: var(--color-text-subtle, #9c9484);
  margin: 0;
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  font-size: 0.8125rem;
  font-weight: 600;
  background: var(--color-bg-surface, #282723);
  color: var(--color-text-main, #f6f2ea);
  border: 1px solid var(--color-border, #444138);
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: var(--color-bg-subtle, #2e2d27);
  border-color: var(--color-accent-kicker, #c2410c);
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

.kpi-card {
  padding: 1.125rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: var(--radius-lg, 10px);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  position: relative;
}

.kpi-card.highlighted {
  border-color: var(--color-accent-kicker, #c2410c);
}

.kpi-card.success-card {
  border-color: var(--color-success-border, #2d5738);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-idx {
  font-size: 0.6875rem;
  font-weight: 700;
  font-family: var(--font-mono, monospace);
  color: var(--color-accent-kicker, #c2410c);
}

.kpi-icon-box {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: var(--color-bg-subtle, #2e2d27);
  color: var(--color-text-subtle, #9c9484);
}

.kpi-icon-box.accent {
  background: rgba(194, 65, 12, 0.15);
  color: var(--color-accent-kicker, #c2410c);
}

.kpi-icon-box.success {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
}

.kpi-icon-box.warning {
  background: var(--color-warning-bg, #342914);
  color: var(--color-warning-text, #fcd34d);
}

.kpi-label {
  display: block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-subtle, #9c9484);
}

.kpi-value {
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
}

.kpi-note {
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
}

/* Barra de Filtros y Búsqueda */
.kanban-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding: 0.875rem 1rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: var(--radius-md, 8px);
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 280px;
  display: flex;
  align-items: center;
}

.search-box i.pi-search {
  position: absolute;
  left: 0.875rem;
  color: var(--color-text-subtle, #9c9484);
  font-size: 0.875rem;
}

.search-input {
  width: 100%;
  padding: 0.5625rem 2rem 0.5625rem 2.375rem;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  color: var(--color-text-main, #f6f2ea);
  font-size: 0.8125rem;
  outline: none;
  transition: border-color 0.2s ease;
}

.search-input:focus {
  border-color: var(--color-accent-kicker, #c2410c);
}

.clear-search {
  position: absolute;
  right: 0.75rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle, #9c9484);
  cursor: pointer;
}

.filters-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.filter-select-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.filter-icon {
  position: absolute;
  left: 0.75rem;
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
  pointer-events: none;
}

.filter-select {
  padding: 0.5625rem 1rem 0.5625rem 2rem;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  color: var(--color-text-main, #f6f2ea);
  font-size: 0.8125rem;
  outline: none;
  cursor: pointer;
}

.filter-select:focus {
  border-color: var(--color-accent-kicker, #c2410c);
}

/* Tablero Kanban (2 Columnas) */
.kanban-board {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  align-items: start;
}

.kanban-column {
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: var(--radius-lg, 10px);
  padding: 1.125rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.column-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border, #444138);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.col-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.col-dot.dot-abierto {
  background: var(--color-accent-kicker, #c2410c);
  box-shadow: 0 0 6px rgba(194, 65, 12, 0.4);
}

.col-dot.dot-finalizado {
  background: var(--color-success-text, #7ee79d);
  box-shadow: 0 0 6px rgba(126, 231, 157, 0.4);
}

.col-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
  margin: 0;
}

.count-badge {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  border-radius: 12px;
  background: rgba(194, 65, 12, 0.15);
  color: var(--color-accent-kicker, #c2410c);
  border: 1px solid rgba(194, 65, 12, 0.3);
}

.count-badge.completed {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border-color: var(--color-success-border, #2d5738);
}

.col-hint {
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
}

.column-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2.5rem 1rem;
  text-align: center;
  color: var(--color-text-subtle, #9c9484);
  font-size: 0.8125rem;
}

.cards-list {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

/* Tarjetas Kanban */
.kanban-card {
  padding: 1rem;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  border-radius: var(--radius-md, 8px);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.kanban-card:hover {
  border-color: var(--color-accent-kicker, #c2410c);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.kanban-card.card-finalizado:hover {
  border-color: var(--color-success-text, #7ee79d);
}

.card-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.exp-code {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
}

.canal-pill {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  text-transform: uppercase;
}

.canal-pill.verde {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border: 1px solid var(--color-success-border, #2d5738);
}

.canal-pill.naranja {
  background: var(--color-warning-bg, #342914);
  color: var(--color-warning-text, #fcd34d);
  border: 1px solid var(--color-warning-border, #695222);
}

.canal-pill.rojo {
  background: rgba(220, 38, 38, 0.15);
  color: #f87171;
  border: 1px solid rgba(220, 38, 38, 0.3);
}

.canal-pill.pendiente {
  background: var(--color-bg-subtle, #2e2d27);
  color: var(--color-text-subtle, #9c9484);
  border: 1px solid var(--color-border, #444138);
}

.status-pill-closed {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border: 1px solid var(--color-success-border, #2d5738);
}

.card-cargo-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.5rem;
}

.card-cargo-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text-main, #f6f2ea);
  margin: 0;
  line-height: 1.3;
}

.regimen-tag {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  white-space: nowrap;
}

.regimen-tag.import {
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.regimen-tag.export {
  background: rgba(168, 85, 247, 0.15);
  color: #d8b4fe;
  border: 1px solid rgba(168, 85, 247, 0.3);
}

.card-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.375rem 0.75rem;
  font-size: 0.75rem;
  background: var(--color-bg-surface, #282723);
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  border: 1px solid var(--color-border, #444138);
}

.meta-item {
  display: flex;
  flex-direction: column;
}

.meta-item.full-width {
  grid-column: 1 / -1;
}

.meta-label {
  color: var(--color-text-subtle, #9c9484);
  font-size: 0.6875rem;
}

.meta-val {
  color: var(--color-text-main, #f6f2ea);
  font-weight: 500;
}

.meta-val.truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.canal-mini-pill {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 700;
}

.canal-mini-pill.verde {
  color: var(--color-success-text, #7ee79d);
}

.canal-mini-pill.naranja {
  color: var(--color-warning-text, #fcd34d);
}

.canal-mini-pill.rojo {
  color: #f87171;
}

/* Indicador de fase operativa */
.phase-indicator {
  display: flex;
}

.phase-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
}

.phase-pill.phase-transito {
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.phase-pill.phase-arribado {
  background: rgba(245, 158, 11, 0.15);
  color: #fcd34d;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.phase-pill.phase-inspeccion {
  background: rgba(239, 68, 68, 0.15);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.phase-pill.phase-levante {
  background: var(--color-success-bg, #1c2b1f);
  color: var(--color-success-text, #7ee79d);
  border: 1px solid var(--color-success-border, #2d5738);
}

.phase-pill.phase-apertura {
  background: var(--color-bg-subtle, #2e2d27);
  color: var(--color-text-subtle, #9c9484);
  border: 1px solid var(--color-border, #444138);
}

.card-alert {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.3rem 0.5rem;
  border-radius: 4px;
}

.card-alert.normal {
  background: var(--color-bg-subtle, #2e2d27);
  color: var(--color-text-subtle, #9c9484);
}

.card-alert.warning {
  background: var(--color-warning-bg, #342914);
  color: var(--color-warning-text, #fcd34d);
}

.card-alert.danger {
  background: rgba(220, 38, 38, 0.2);
  color: #f87171;
}

/* Métricas de Historial */
.history-metrics {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.75rem;
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
  border-radius: 6px;
  gap: 0.5rem;
}

.history-metric-box {
  display: flex;
  flex-direction: column;
}

.h-label {
  font-size: 0.625rem;
  color: var(--color-text-subtle, #9c9484);
  text-transform: uppercase;
}

.h-val {
  font-size: 0.75rem;
  color: var(--color-text-main, #f6f2ea);
  font-weight: 600;
}

.history-metric-divider {
  color: var(--color-text-subtle, #9c9484);
  font-size: 0.6875rem;
}

.history-lead-time {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.2rem 0.4rem;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
  border-radius: 4px;
  color: var(--color-text-muted, #c4bcab);
}

/* Card Footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 0.5rem;
  border-top: 1px solid var(--color-border, #444138);
}

.card-date {
  font-size: 0.6875rem;
  color: var(--color-text-subtle, #9c9484);
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.btn-card-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.6rem;
  font-size: 0.6875rem;
  font-weight: 700;
  border: 1px solid var(--color-accent-kicker, #c2410c);
  background: rgba(194, 65, 12, 0.1);
  color: var(--color-accent-kicker, #c2410c);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-card-action:hover {
  background: var(--color-accent-kicker, #c2410c);
  color: #ffffff;
}

.btn-card-action.history {
  border-color: var(--color-border-strong, #5c564a);
  background: var(--color-bg-subtle, #2e2d27);
  color: var(--color-text-main, #f6f2ea);
}

.btn-card-action.history:hover {
  border-color: var(--color-success-text, #7ee79d);
  color: var(--color-success-text, #7ee79d);
  background: var(--color-success-bg, #1c2b1f);
}

/* MODAL DE DETALLE INTEGRAL */
.detail-modal-body {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.modal-dam-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border, #444138);
  flex-wrap: wrap;
}

.kicker-exp {
  display: block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-accent-kicker, #c2410c);
  margin-bottom: 0.25rem;
}

.dam-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
  margin: 0 0 0.375rem 0;
}

.exp-codes-sub {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-subtle, #9c9484);
  flex-wrap: wrap;
}

.code-badge {
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  background: var(--color-bg-subtle, #2e2d27);
  border: 1px solid var(--color-border, #444138);
  color: var(--color-text-main, #f6f2ea);
  font-weight: 700;
}

/* Semáforo SUNAT */
.sunat-traffic-light {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 1px solid var(--color-border, #444138);
  background: var(--color-bg-base, #1e1e1b);
  min-width: 260px;
}

.sunat-traffic-light.verde {
  border-color: var(--color-success-border, #2d5738);
  background: var(--color-success-bg, #1c2b1f);
}

.sunat-traffic-light.verde .light-icon {
  color: var(--color-success-text, #7ee79d);
}

.sunat-traffic-light.naranja {
  border-color: var(--color-warning-border, #695222);
  background: var(--color-warning-bg, #342914);
}

.sunat-traffic-light.naranja .light-icon {
  color: var(--color-warning-text, #fcd34d);
}

.sunat-traffic-light.rojo {
  border-color: rgba(220, 38, 38, 0.4);
  background: rgba(220, 38, 38, 0.1);
}

.sunat-traffic-light.rojo .light-icon {
  color: #f87171;
}

.light-icon {
  font-size: 1.5rem;
  color: var(--color-text-subtle, #9c9484);
}

.light-text {
  display: flex;
  flex-direction: column;
}

.light-label {
  font-size: 0.625rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-subtle, #9c9484);
}

.light-val {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
}

.light-desc {
  font-size: 0.6875rem;
  color: var(--color-text-subtle, #9c9484);
}

/* Stepper */
.stepper-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
  margin: 0;
}

.section-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
}

.stepper-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  overflow-x: auto;
  padding: 0.5rem 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.35rem;
  min-width: 80px;
  z-index: 1;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  background: var(--color-bg-base, #1e1e1b);
  border: 2px solid var(--color-border, #444138);
  color: var(--color-text-subtle, #9c9484);
}

.step.completed .step-circle {
  background: var(--color-success-bg, #1c2b1f);
  border-color: var(--color-success-text, #7ee79d);
  color: var(--color-success-text, #7ee79d);
}

.step.active .step-circle {
  background: rgba(194, 65, 12, 0.2);
  border-color: var(--color-accent-kicker, #c2410c);
  color: var(--color-accent-kicker, #c2410c);
}

.step-label {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-text-main, #f6f2ea);
}

.step-date {
  font-size: 0.625rem;
  color: var(--color-text-subtle, #9c9484);
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-border, #444138);
  margin: -1.25rem 0.25rem 0 0.25rem;
}

.step-line.active {
  background: var(--color-success-text, #7ee79d);
}

/* Operations Grid */
.operations-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.operation-card {
  padding: 1rem;
  border-radius: 8px;
  border: 1px solid var(--color-border, #444138);
  background: var(--color-bg-base, #1e1e1b);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.op-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.op-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

.op-icon.warehouse {
  background: rgba(194, 65, 12, 0.15);
  color: var(--color-accent-kicker, #c2410c);
}

.op-icon.transport {
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
}

.op-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
  margin: 0;
}

.op-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
}

.op-card-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.op-metric-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.75rem;
}

.op-lbl {
  color: var(--color-text-subtle, #9c9484);
}

.op-val {
  color: var(--color-text-main, #f6f2ea);
}

.status-pill-transporte {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
}

.warehouse-countdown-alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.75rem;
  border-radius: 6px;
  margin-top: 0.25rem;
}

.warehouse-countdown-alert.normal {
  background: var(--color-bg-surface, #282723);
  border: 1px solid var(--color-border, #444138);
}

.warehouse-countdown-alert.warning {
  background: var(--color-warning-bg, #342914);
  border: 1px solid var(--color-warning-border, #695222);
}

.warehouse-countdown-alert.danger {
  background: rgba(220, 38, 38, 0.15);
  border: 1px solid rgba(220, 38, 38, 0.3);
}

.alert-info {
  display: flex;
  flex-direction: column;
}

.days-remaining strong {
  color: var(--color-text-main, #f6f2ea);
}

.days-remaining span {
  font-size: 0.6875rem;
  color: var(--color-text-subtle, #9c9484);
  margin-left: 0.25rem;
}

.date-deadline {
  font-size: 0.6875rem;
  color: var(--color-text-subtle, #9c9484);
}

.op-card-empty {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
  padding: 1rem 0;
}

/* Documents Section */
.documents-section {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.docs-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.docs-count {
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
}

.docs-loading,
.docs-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem;
  text-align: center;
  color: var(--color-text-subtle, #9c9484);
  font-size: 0.8125rem;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px dashed var(--color-border, #444138);
  border-radius: 8px;
}

.sub-hint {
  font-size: 0.75rem;
  color: var(--color-text-subtle, #9c9484);
}

.docs-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.doc-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  background: var(--color-bg-base, #1e1e1b);
  border: 1px solid var(--color-border, #444138);
}

.doc-icon {
  font-size: 1.25rem;
  color: var(--color-accent-kicker, #c2410c);
}

.doc-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.doc-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main, #f6f2ea);
}

.doc-sub {
  font-size: 0.6875rem;
  color: var(--color-text-subtle, #9c9484);
}

.doc-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-doc-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.7rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid var(--color-border, #444138);
  background: var(--color-bg-surface, #282723);
  color: var(--color-text-main, #f6f2ea);
  transition: all 0.15s ease;
}

.btn-doc-action:hover {
  background: var(--color-bg-subtle, #2e2d27);
  border-color: var(--color-accent-kicker, #c2410c);
}

.btn-doc-action.download {
  border-color: var(--color-accent-kicker, #c2410c);
  background: rgba(194, 65, 12, 0.15);
  color: var(--color-accent-kicker, #c2410c);
}

.btn-doc-action.download:hover {
  background: var(--color-accent-kicker, #c2410c);
  color: #ffffff;
}

/* Empty State Global */
.empty-state,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 4rem 1.5rem;
  text-align: center;
  background: var(--color-bg-surface, #282723);
  border: 1px dashed var(--color-border, #444138);
  border-radius: var(--radius-lg, 10px);
  color: var(--color-text-subtle, #9c9484);
}

.empty-state i,
.loading-state i {
  font-size: 2.25rem;
  color: var(--color-accent-kicker, #c2410c);
}

.empty-state h3 {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-main, #f6f2ea);
  margin: 0;
}

.empty-state p {
  font-size: 0.875rem;
  max-width: 440px;
  margin: 0;
}

/* Responsividad */
@media (max-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .kanban-board {
    grid-template-columns: 1fr;
  }

  .operations-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .kanban-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .filters-group {
    flex-direction: column;
    width: 100%;
  }

  .filter-select {
    width: 100%;
  }
}
</style>
