<template>
  <div class="gestion-aduanera-view">
    <!-- ============================================================== -->
    <!-- VISTA COMPLETA: AUDITORÍA TÉCNICA DEL EXPEDIENTE / COTIZACIÓN   -->
    <!-- ============================================================== -->
    <AuditoriaExpedienteModal
      v-if="showAuditoriaCompleta && (inspectedCotizacion || inspectedExpediente)"
      :cotizacion="inspectedCotizacion"
      :expediente="inspectedExpediente"
      :processing="processingAction"
      @back="closeAuditoriaCompleta"
      @approve="handleApproveFromAuditoria"
      @observe="handleObserveFromAuditoria"
      @reject="handleRejectFromAuditoria"
      @open-apertura="handleAperturaFromAuditoria"
      @doc-updated="loadData"
    />

    <!-- ============================================================== -->
    <!-- VISTA PRINCIPAL: BANDEJA Y GESTIÓN MAESTRA ADUANERA            -->
    <!-- ============================================================== -->
    <div v-else class="supervision-master-container">
      <!-- 1. Barra de Atajos Rápidos y Operatoria Aduanera (en pestañas legacy) -->
      <div v-if="activeTab !== 'cotizaciones'" class="supervision-action-toolbar">
        <div class="toolbar-left-info">
          <span class="badge-kicker">OPERATORIA & CONTROL ADUANERO</span>
          <h3 class="toolbar-title">Panel de Supervisión Técnica & Expedientes</h3>
        </div>
        <div class="toolbar-actions-group">
          <button
            type="button"
            :class="['btn-outline-warm', { active: activeTab === 'dashboard' }]"
            @click="switchTab('dashboard')"
          >
            <i class="pi pi-th-large"></i>
            <span>Dashboard Operativo</span>
          </button>
          <button
            type="button"
            :class="['btn-outline-warm', { active: activeTab === 'cotizaciones' }]"
            @click="switchTab('cotizaciones')"
          >
            <i class="pi pi-inbox"></i>
            <span>Bandeja Cotizaciones ({{ cotizaciones.length }})</span>
          </button>
          <button
            type="button"
            :class="['btn-outline-warm', { active: activeTab === 'apertura' }]"
            @click="switchTab('apertura')"
          >
            <i class="pi pi-folder-plus"></i>
            <span>Apertura de Expedientes ({{ cotizacionesClasificadas.listasApertura.length }})</span>
          </button>
          <button
            type="button"
            :class="['btn-outline-warm', { active: activeTab === 'expedientes' }]"
            @click="switchTab('expedientes')"
          >
            <i class="pi pi-folder-open"></i>
            <span>Expedientes Aduaneros ({{ expedientes.length }})</span>
          </button>
          <button
            type="button"
            class="btn-outline-warm"
            @click="loadData"
            :disabled="loading"
            title="Actualizar datos del servidor"
          >
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Refrescar</span>
          </button>
        </div>
      </div>

      <!-- 2. Grid de Métricas de Expedientes (Solo en Pestaña Expedientes) -->
      <div v-if="activeTab === 'expedientes'" class="metrics-supervision-grid">
        <!-- KPI 01: Total Expedientes -->
        <div class="supervision-kpi-card" @click="faseExpediente = 'TODOS'">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">{{ filterAgenteExpediente === 'MINE' ? 'Mis Expedientes' : 'Expedientes Registrados' }}</span>
            <i class="pi pi-folder kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value">{{ baseExpedientes.length }}</span>
            <span class="kpi-badge">{{ filterAgenteExpediente === 'MINE' ? 'Asignados a Mí' : 'Total Activos' }}</span>
          </div>
          <div class="kpi-footer-note">
            <span>{{ filterAgenteExpediente === 'MINE' ? 'Expedientes aduaneros a mi cargo' : 'Operaciones aduaneras en curso' }}</span>
          </div>
        </div>

        <!-- KPI 02: Canal Pendiente -->
        <div class="supervision-kpi-card pending-card" @click="faseExpediente = 'PENDIENTE_CANAL'">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Canal SUNAT Pendiente</span>
            <i class="pi pi-clock kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-warning">{{ expedientesPorCanalPendienteCount }}</span>
            <span class="kpi-badge warning">Por Asignar</span>
          </div>
          <div class="kpi-footer-note">
            <span>Esperando numeración de DAM</span>
          </div>
        </div>

        <!-- KPI 03: Con Canal Asignado -->
        <div class="supervision-kpi-card info-card" @click="faseExpediente = 'CONTROL_SUNAT'">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">Con Canal Asignado</span>
            <i class="pi pi-shield kpi-icon info"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-info">{{ expedientesEnControlCount }}</span>
            <span class="kpi-badge info">Verde / Naranja / Rojo</span>
          </div>
          <div class="kpi-footer-note">
            <span>En aforo o control documentario</span>
          </div>
        </div>

        <!-- KPI 04: Levante Autorizado -->
        <div class="supervision-kpi-card success-card" @click="faseExpediente = 'LEVANTE_AUTORIZADO'">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Levante Autorizado</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-success">{{ expedientesConLevanteCount }}</span>
            <span class="kpi-badge success">Mercancía Liberada</span>
          </div>
          <div class="kpi-footer-note">
            <span>Listos para retiro y entrega final</span>
          </div>
        </div>
      </div>

      <!-- ============================================================== -->
      <!-- PESTAÑA 01: DASHBOARD OPERATIVO ADUANERO                       -->
      <!-- ============================================================== -->
      <div v-if="activeTab === 'dashboard'" class="tab-content-section fade-in">
        <DashboardAduaneroPanel
          :cotizaciones="cotizaciones"
          :expedientes="expedientes"
          :loading="loading"
          @refresh="loadData"
          @navigate="handleDashboardNavigate"
        />
      </div>

      <!-- ============================================================== -->
      <!-- PESTAÑA 02: BANDEJA DE COTIZACIONES ADUANERAS                  -->
      <!-- ============================================================== -->
      <AduanasCotizacionesPanel
        v-else-if="activeTab === 'cotizaciones'"
        class="fade-in"
        :cotizaciones="filteredCotizaciones"
        :clasificadas="cotizacionesClasificadas"
        :total-count="cotizaciones.length"
        :loading="loading"
        v-model:sub-queue="subQueueCotizaciones"
        v-model:search-query="searchQuery"
        v-model:filter-regimen="filterRegimenCotizacion"
        v-model:sort-by="sortByCotizacion"
        :has-active-filters="hasActiveCotizacionFilters"
        @reset-filters="resetCotizacionFilters"
        @refresh="loadData"
        @inspect="openInspectModal"
        @open-docs="openDocumentosCotizacion"
        @approve="confirmApprove"
        @observe="openRevisionDialog($event, 'observe')"
        @reject="openRevisionDialog($event, 'reject')"
        @open-apertura="goToApertura"
      />

      <!-- ============================================================== -->
      <!-- PESTAÑA 03: APERTURA FORMAL DE EXPEDIENTES ADUANEROS           -->
      <!-- ============================================================== -->
      <div v-else-if="activeTab === 'apertura'" class="tab-content-section fade-in">
        <AperturaExpedienteView
          :cotizacion-preseleccionada="selectedCotizacionParaExpediente"
          @created="onExpedienteCreado"
          @cancel="switchTab('cotizaciones')"
          @clear-preselection="selectedCotizacionParaExpediente = null"
        />
      </div>

      <!-- ============================================================== -->
      <!-- PESTAÑA 04: GESTIÓN INTEGRAL DE EXPEDIENTES ADUANEROS          -->
      <!-- ============================================================== -->
      <div v-else-if="activeTab === 'expedientes'" class="tab-content-section fade-in">
        <!-- Alerta de Cotizaciones Aprobadas Pendientes de Apertura -->
        <div v-if="cotizacionesClasificadas.listasApertura.length > 0" class="exp-pending-alert-banner">
          <div class="alert-icon-box">
            <i class="pi pi-bell"></i>
          </div>
          <div class="alert-text-content">
            <strong>{{ cotizacionesClasificadas.listasApertura.length }} Cotización(es) Aprobada(s) con Pago Pendiente(s) de Apertura</strong>
            <p>Existen propuestas comerciales con pago verificado listas para generar su expediente aduanero operativo.</p>
          </div>
          <button
            type="button"
            class="btn-alert-action"
            @click="goToApertura(null)"
          >
            <i class="pi pi-plus-circle"></i>
            <span>Aperturar Expediente Ahora</span>
          </button>
        </div>

        <!-- Barra de Píldoras por Etapa Operativa de Despacho -->
        <div class="stage-pills-bar">
          <button
            type="button"
            :class="['stage-pill-btn', { active: faseExpediente === 'TODOS' }]"
            @click="faseExpediente = 'TODOS'"
          >
            <span>{{ filterAgenteExpediente === 'MINE' ? 'Todos Mis Expedientes' : 'Todos los Expedientes' }} ({{ baseExpedientes.length }})</span>
          </button>

          <button
            type="button"
            :class="['stage-pill-btn', { active: faseExpediente === 'PENDIENTE_CANAL' }]"
            @click="faseExpediente = 'PENDIENTE_CANAL'"
          >
            <span class="phase-idx">01</span>
            <span>Por Asignar Canal ({{ expedientesPorCanalPendienteCount }})</span>
          </button>

          <button
            type="button"
            :class="['stage-pill-btn', { active: faseExpediente === 'CONTROL_SUNAT' }]"
            @click="faseExpediente = 'CONTROL_SUNAT'"
          >
            <span class="phase-idx">02</span>
            <span>En Control SUNAT ({{ expedientesEnControlCount }})</span>
          </button>

          <button
            type="button"
            :class="['stage-pill-btn', { active: faseExpediente === 'LEVANTE_AUTORIZADO' }]"
            @click="faseExpediente = 'LEVANTE_AUTORIZADO'"
          >
            <span class="phase-idx">03</span>
            <span>Levante Autorizado ({{ expedientesConLevanteCount }})</span>
          </button>

          <button
            type="button"
            :class="['stage-pill-btn', { active: faseExpediente === 'CERRADOS' }]"
            @click="faseExpediente = 'CERRADOS'"
          >
            <span class="phase-idx">04</span>
            <span>Cerrados ({{ expedientesCerradosCount }})</span>
          </button>
        </div>

        <div class="section-header-row">
          <div>
            <span class="badge-kicker">
              {{ filterAgenteExpediente === 'MINE' ? 'BANDEJA PERSONAL // EXPEDIENTES ASIGNADOS' : 'OPERACIONES ADUANERAS // CONTROL DAM & LEVANTE' }}
            </span>
            <h3 class="section-title">
              {{ filterAgenteExpediente === 'MINE' ? 'Mis Expedientes Asignados' : getFaseExpedienteTitle(faseExpediente) }} ({{ filteredExpedientes.length }})
            </h3>
          </div>

          <div class="section-controls">
            <!-- Toggle Rápido: Mis Expedientes Asignados vs Todos -->
            <button
              v-if="authStore.user?.idUsuario"
              type="button"
              :class="['btn-toggle-mine', { active: filterAgenteExpediente === 'MINE' }]"
              @click="toggleFiltroMisExpedientes"
              :title="filterAgenteExpediente === 'MINE' ? 'Ver todos los expedientes de la empresa' : 'Ver solo mis expedientes asignados'"
            >
              <i class="pi pi-user-check"></i>
              <span>{{ filterAgenteExpediente === 'MINE' ? 'Ver Todos' : 'Mis Expedientes (' + misExpedientesCount + ')' }}</span>
            </button>

            <!-- Filtro Dropdown por Agente Aduanero Responsable (similar al Asesor Comercial) -->
            <select v-model="filterAgenteExpediente" class="filter-select" title="Filtrar por agente aduanero asignado">
              <option value="ALL">Todos los Agentes Aduaneros</option>
              <option v-if="authStore.user?.idUsuario" value="MINE">★ Mis Expedientes Asignados ({{ misExpedientesCount }})</option>
              <option value="UNASSIGNED">Sin Agente Asignado</option>
              <option v-for="a in agentesAduanerosDisponibles" :key="a.id" :value="String(a.id)">
                {{ a.nombre }}
              </option>
            </select>

            <!-- Filtro por Régimen Aduanero -->
            <select v-model="filterRegimenExpediente" class="filter-select">
              <option value="">Todos los Regímenes</option>
              <option value="IMPORTACION">Importación para el Consumo</option>
              <option value="EXPORTACION">Exportación Definitiva</option>
            </select>

            <!-- Filtro por Canal SUNAT -->
            <select v-model="filterCanalExpediente" class="filter-select">
              <option value="">Todos los Canales SUNAT</option>
              <option value="PENDIENTE">Canal Pendiente</option>
              <option value="VERDE">Canal VERDE (Levante Inmediato)</option>
              <option value="NARANJA">Canal NARANJA (Documentario)</option>
              <option value="ROJO">Canal ROJO (Reconocimiento Físico)</option>
            </select>

            <!-- Búsqueda Rápida de Expedientes -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchExpedienteQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por código, DAM, cliente, contenedor o agente..."
              />
            </div>

            <!-- Botón de Recarga -->
            <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading">
              <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
              <span>Actualizar</span>
            </button>
          </div>
        </div>

        <!-- Tabla de Expedientes Aduaneros -->
        <div class="table-container">
          <DataTable
            :value="filteredExpedientes"
            :loading="loading"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idExpediente"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-folder-open empty-icon"></i>
                <p class="empty-title">No hay expedientes aduaneros con este criterio</p>
                <p class="empty-desc">Los Expedientes se generan a partir de cotizaciones comerciales aprobadas con pago verificado.</p>
              </div>
            </template>

            <template #loading>
              <div class="loading-state">
                <i class="pi pi-spin pi-spinner"></i>
                <span>Cargando expedientes aduaneros...</span>
              </div>
            </template>

            <Column header="Código Expediente">
              <template #body="{ data: exp }">
                <div class="exp-code-badge" @click="openAuditarExpediente(exp)" title="Clic para ver auditoría integral">
                  <i class="pi pi-folder"></i>
                  <span class="font-bold font-mono">{{ exp.codigoExpediente }}</span>
                </div>
              </template>
            </Column>

            <Column header="Cotización / Régimen">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="doc-code font-mono">{{ exp.codigoCotizacion }}</span>
                  <span class="client-sub">{{ formatRegimen(exp.regimenAduanero) }}</span>
                </div>
              </template>
            </Column>

            <Column header="Cliente">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="client-name">{{ exp.clienteRazonSocial || '—' }}</span>
                  <span class="client-sub font-mono">Doc: {{ exp.clienteNumeroDocumento }}</span>
                </div>
              </template>
            </Column>

            <Column header="DAM & Canal SUNAT">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="font-mono text-xs font-bold">{{ exp.numeroDam || 'Sin DAM aún' }}</span>
                  <Tag
                    :value="exp.canalSunat || 'PENDIENTE'"
                    :severity="getCanalSeverity(exp.canalSunat)"
                    rounded
                  />
                </div>
              </template>
            </Column>

            <Column header="Contenedor / Nave">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="font-mono text-xs font-bold">{{ exp.numeroContenedor || '—' }}</span>
                  <span class="client-sub">{{ exp.naveViaje || 'Sin nave asignada' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Estado Trámite">
              <template #body="{ data: exp }">
                <Tag
                  :value="exp.estadoExpediente || 'ASIGNADO'"
                  :severity="getExpedienteSeverity(exp.estadoExpediente)"
                  rounded
                />
              </template>
            </Column>

            <Column header="Agente Aduanero">
              <template #body="{ data: exp }">
                <div class="agent-cell">
                  <span class="font-bold text-xs" :class="{ 'text-primary': exp.idAgenteAduanero === authStore.user?.idUsuario }">
                    <i class="pi pi-user-check text-success mr-1"></i>
                    {{ exp.agenteAduaneroNombre || 'Sin asignar' }}
                  </span>
                  <span v-if="exp.idAgenteAduanero && exp.idAgenteAduanero === authStore.user?.idUsuario" class="badge-asignado-mio">
                    Asignado a mí
                  </span>
                </div>
              </template>
            </Column>

            <Column header="Levante">
              <template #body="{ data: exp }">
                <span v-if="exp.fechaLevanteAutorizacion" class="text-success font-bold text-xs">
                  <i class="pi pi-check-circle"></i> Autorizado
                </span>
                <span v-else class="text-muted-dash">Pendiente</span>
              </template>
            </Column>

            <Column header="Acciones de Gestión" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: exp }">
                <div class="actions-wrapper">
                  <button
                    type="button"
                    class="action-btn-primary"
                    title="Ver Ficha y Auditoría Integral del Expediente"
                    @click="openAuditarExpediente(exp)"
                  >
                    <i class="pi pi-eye"></i>
                    <span>Auditar</span>
                  </button>

                  <button
                    type="button"
                    class="action-btn-outline"
                    title="Gestionar Documentos del Expediente (BL, Factura, DAM, etc.)"
                    @click="openDocumentosExpediente(exp)"
                  >
                    <i class="pi pi-paperclip"></i>
                    <span>Docs</span>
                  </button>

                  <!-- Tránsito (Actualizar Contenedor / Precinto diferidos) -->
                  <button
                    v-if="getAccionesExpediente(exp).puedeActualizarTransito"
                    type="button"
                    class="action-btn-outline"
                    title="Actualizar Datos de Tránsito, Contenedor y Precinto"
                    @click="openActualizarTransitoModal(exp)"
                  >
                    <i class="pi pi-globe"></i>
                    <span>Tránsito</span>
                  </button>

                  <!-- DAM y Canal SUNAT -->
                  <button
                    v-if="getAccionesExpediente(exp).puedeAsignarCanal"
                    type="button"
                    class="action-btn-outline"
                    title="Registrar Canal SUNAT y Declaración DAM"
                    @click="openCanalModal(exp)"
                  >
                    <i class="pi pi-compass"></i>
                    <span>DAM/Canal</span>
                  </button>
                  <button
                    v-else-if="getAccionesExpediente(exp).requiereArriboParaCanal"
                    type="button"
                    class="action-btn-outline disabled opacity-60 cursor-not-allowed"
                    disabled
                    title="Pendiente de Arribo: El agente aduanero debe confirmar la llegada a puerto de destino antes de asignar Canal SUNAT"
                  >
                    <i class="pi pi-clock"></i>
                    <span>Esperando Arribo</span>
                  </button>

                  <!-- Autorizar Levante -->
                  <button
                    v-if="getAccionesExpediente(exp).puedeLevante && exp.canalSunat && exp.canalSunat !== 'PENDIENTE'"
                    type="button"
                    class="action-btn-outline approve"
                    title="Autorizar Levante Aduanero"
                    @click="openLevanteModal(exp)"
                  >
                    <i class="pi pi-shield"></i>
                    <span>Levante</span>
                  </button>

                  <!-- Retiro Directo en Terminal (Si no contrató transporte TransAduanas) -->
                  <button
                    v-if="getAccionesExpediente(exp).puedeRetiro"
                    type="button"
                    class="action-btn-outline"
                    title="Registrar Acta de Retiro en Terminal por el Cliente"
                    @click="openRetiroDirectoModal(exp)"
                  >
                    <i class="pi pi-building"></i>
                    <span>Retiro</span>
                  </button>

                  <!-- Cierre de Expediente / Conformidad Final -->
                  <button
                    v-if="getAccionesExpediente(exp).puedeCerrar"
                    type="button"
                    class="action-btn-outline approve"
                    title="Revisión de Conformidad y Cierre Final del Expediente Aduanero"
                    @click="openCierreModal(exp)"
                  >
                    <i class="pi pi-lock"></i>
                    <span>Cerrar</span>
                  </button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ============================================================== -->
    <!-- MODALES OPERATIVOS AUXILIARES (APERTURA, DETALLE, CANAL, ETC.) -->
    <!-- ============================================================== -->
    <!-- Modal Unificado: Observar o Rechazar Cotización desde Bandeja -->
    <CotizacionRevisionDialog
      v-model:visible="showRevisionDialog"
      :mode="revisionDialogMode"
      :cotizacion="actionCotizacion"
      :loading="processingAction"
      @confirm="handleRevisionConfirm"
    />

    <!-- Modal: Asignación de Canal SUNAT / Levante Aduanero -->
    <CanalLevanteModal
      :visible="showCanalLevanteModal"
      :expediente="selectedExpedienteParaTramite"
      :action-type="canalLevanteActionType"
      @close="showCanalLevanteModal = false"
      @saved="onCanalLevanteSaved"
    />

    <!-- Modal: Gestión Integral de Documentos Adjuntos -->
    <DocumentosManagerModal
      :visible="showDocumentosModal"
      :entidad-tipo="selectedDocEntidadTipo"
      :id-entidad="selectedDocEntidadId"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigoEntidad"
      :titulo-entidad="selectedDocTituloEntidad"
      @close="showDocumentosModal = false"
    />

    <!-- Modal: Registro de Retiro Directo en Terminal -->
    <RetiroDirectoModal
      :visible="showRetiroDirectoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRetiroDirectoModal = false"
      @saved="onRetiroDirectoSaved"
    />


    <!-- Modal: Conformidad y Cierre Definitivo de Expediente -->
    <CierreExpedienteModal
      :visible="showCierreExpedienteModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showCierreExpedienteModal = false"
      @saved="onCierreExpedienteSaved"
    />

    <!-- Modal: Actualización de Tránsito, Contenedor y Precinto Diferidos -->
    <ActualizarTransitoModal
      :visible="showActualizarTransitoModal"
      :expediente="selectedExpedienteParaTransito"
      @close="showActualizarTransitoModal = false"
      @updated="onTransitoUpdated"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { useRoute, useRouter } from 'vue-router'
import { gestionAduaneraService } from '../services/gestionAduaneraService'
import { expedienteService } from '../services/expedienteService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import AperturaExpedienteView from '../components/AperturaExpedienteView.vue'
import CanalLevanteModal from '../components/CanalLevanteModal.vue'
import RetiroDirectoModal from '../components/RetiroDirectoModal.vue'
import CierreExpedienteModal from '../components/CierreExpedienteModal.vue'
import ActualizarTransitoModal from '../components/ActualizarTransitoModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import AuditoriaExpedienteModal from '../components/AuditoriaExpedienteModal.vue'
import DashboardAduaneroPanel from '../components/DashboardAduaneroPanel.vue'
import AduanasCotizacionesPanel from '../components/AduanasCotizacionesPanel.vue'
import CotizacionRevisionDialog from '../components/CotizacionRevisionDialog.vue'
import { useAduanasCotizaciones } from '../composables/useAduanasCotizaciones'
import { filtrarCotizacionesParaSupervision } from '../utils/supervisionCotizacionUtils'
import {
  filtrarExpedientesPorFase,
  obtenerAccionesPermitidasExpediente
} from '../utils/aduanasWorkflowUtils'


const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()

const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

const loading = ref(false)
const activeTab = ref('dashboard') // 'dashboard' | 'cotizaciones' | 'apertura' | 'expedientes'
const cotizaciones = ref([])
const expedientes = ref([])

// Fase de Expedientes y Filtros
const faseExpediente = ref('TODOS') // 'TODOS' | 'PENDIENTE_CANAL' | 'CONTROL_SUNAT' | 'LEVANTE_AUTORIZADO' | 'CERRADOS'

// Detección de Rol Operativo
const isAgenteAduanero = computed(() => {
  return authStore.userRole === 'AGENTE_ADUANERO' || authStore.user?.rol === 'AGENTE_ADUANERO'
})

// Filtros y Búsqueda de Expedientes
const filterRegimenExpediente = ref('')
const filterCanalExpediente = ref('')
const filterEstadoExpediente = ref('')
const filterAgenteExpediente = ref(isAgenteAduanero.value ? 'MINE' : 'ALL') // 'ALL' | 'MINE' | 'UNASSIGNED' | idUsuario
const searchExpedienteQuery = ref('')

// Vista Completa de Auditoría
const showAuditoriaCompleta = ref(false)
const inspectedCotizacion = ref(null)
const inspectedExpediente = ref(null)

// Modales Operativos Auxiliares
const selectedCotizacionParaExpediente = ref(null)

const showCanalLevanteModal = ref(false)
const selectedExpedienteParaTramite = ref(null)
const canalLevanteActionType = ref('canal')

// Modal de Actualización de Tránsito Diferido
const showActualizarTransitoModal = ref(false)
const selectedExpedienteParaTransito = ref(null)

const openActualizarTransitoModal = (exp) => {
  selectedExpedienteParaTransito.value = exp
  showActualizarTransitoModal.value = true
}

const onTransitoUpdated = () => {
  showActualizarTransitoModal.value = false
  showSuccess('Datos de embarque y tránsito actualizados correctamente.')
  loadData()
}

const getAccionesExpediente = (exp) => {
  return obtenerAccionesPermitidasExpediente(exp)
}

// Modal de Gestión de Documentos Adjuntos
const showDocumentosModal = ref(false)
const selectedDocEntidadTipo = ref('COTIZACION')
const selectedDocEntidadId = ref(null)
const selectedDocCodigoEntidad = ref('')
const selectedDocTituloEntidad = ref('')

const openDocumentosCotizacion = (cotizacion) => {
  if (!cotizacion) return
  selectedDocEntidadTipo.value = 'COTIZACION'
  selectedDocEntidadId.value = cotizacion.idCotizacion
  selectedDocCodigoEntidad.value = cotizacion.codigoCotizacion || `COT-${cotizacion.idCotizacion}`
  selectedDocTituloEntidad.value = `${cotizacion.codigoCotizacion || 'Cotización'} — ${cotizacion.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

const openDocumentosExpediente = (expediente) => {
  if (!expediente) return
  selectedDocEntidadTipo.value = 'EXPEDIENTE'
  selectedDocEntidadId.value = expediente.idExpediente
  selectedDocCodigoEntidad.value = expediente.codigoExpediente || `EXP-${expediente.idExpediente}`
  selectedDocTituloEntidad.value = `${expediente.codigoExpediente || 'Expediente'} — ${expediente.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

// Modal unificado de revisión (Observar / Rechazar)
const showRevisionDialog = ref(false)
const revisionDialogMode = ref('observe')
const actionCotizacion = ref(null)

// Sincronización reactiva con el menú lateral (Sidebar) y URL
const switchTab = (tabName) => {
  activeTab.value = tabName
  navigationStore.setGestionAduaneraSubOption(tabName)
  if (route.query.tab !== tabName) {
    router.replace({ query: { ...route.query, tab: tabName } })
  }
}

const handleDashboardNavigate = (tab, subFilter) => {
  switchTab(tab)
  if (tab === 'cotizaciones' && subFilter) {
    subQueueCotizaciones.value = subFilter
  } else if (tab === 'expedientes' && subFilter) {
    faseExpediente.value = subFilter
  }
}

const getFaseExpedienteTitle = (fase) => {
  const map = {
    TODOS: 'Directorio Completo de Expedientes Aduaneros',
    PENDIENTE_CANAL: 'Expedientes por Asignar Canal SUNAT / Numerar DAM',
    CONTROL_SUNAT: 'Expedientes en Control y Aforo SUNAT (Verde / Naranja / Rojo)',
    LEVANTE_AUTORIZADO: 'Expedientes con Levante Autorizado (Mercancía Liberada)',
    CERRADOS: 'Expedientes Concluidos y Cerrados'
  }
  return map[fase] || 'Expedientes Aduaneros'
}

watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && ['dashboard', 'cotizaciones', 'apertura', 'expedientes'].includes(newTab)) {
      activeTab.value = newTab
      if (navigationStore.gestionAduaneraSubOption !== newTab) {
        navigationStore.setGestionAduaneraSubOption(newTab)
      }
    }
  },
  { immediate: true }
)

watch(
  () => [navigationStore.gestionAduaneraSubOption, navigationStore.navTick],
  ([newSub]) => {
    if (newSub && ['dashboard', 'cotizaciones', 'apertura', 'expedientes'].includes(newSub)) {
      if (activeTab.value !== newSub) {
        activeTab.value = newSub
      }
    }
  }
)

// Lista de Agentes Aduaneros Disponibles en Expedientes para Filtro
const agentesAduanerosDisponibles = computed(() => {
  const map = new Map()
  expedientes.value.forEach((exp) => {
    if (exp.idAgenteAduanero && exp.agenteAduaneroNombre) {
      map.set(exp.idAgenteAduanero, exp.agenteAduaneroNombre)
    }
  })
  return Array.from(map.entries())
    .map(([id, nombre]) => ({ id, nombre }))
    .sort((a, b) => a.nombre.localeCompare(b.nombre))
})

// Expedientes asignados al usuario en sesión
const misExpedientesCount = computed(() => {
  const myId = authStore.user?.idUsuario
  if (!myId) return 0
  return expedientes.value.filter((e) => e.idAgenteAduanero === myId).length
})

const toggleFiltroMisExpedientes = () => {
  if (filterAgenteExpediente.value === 'MINE') {
    filterAgenteExpediente.value = 'ALL'
  } else {
    filterAgenteExpediente.value = 'MINE'
  }
}

// Base de Expedientes según Filtro de Asignación de Agente
const baseExpedientes = computed(() => {
  if (filterAgenteExpediente.value === 'MINE') {
    const myId = authStore.user?.idUsuario
    return expedientes.value.filter((e) => e.idAgenteAduanero === myId)
  } else if (filterAgenteExpediente.value === 'UNASSIGNED') {
    return expedientes.value.filter((e) => !e.idAgenteAduanero)
  } else if (filterAgenteExpediente.value !== 'ALL') {
    return expedientes.value.filter((e) => String(e.idAgenteAduanero) === String(filterAgenteExpediente.value))
  }
  return expedientes.value
})

// Métricas de Expedientes
const expedientesPorCanalPendienteCount = computed(() => {
  return filtrarExpedientesPorFase(baseExpedientes.value, 'PENDIENTE_CANAL').length
})

const expedientesEnControlCount = computed(() => {
  return filtrarExpedientesPorFase(baseExpedientes.value, 'CONTROL_SUNAT').length
})

const expedientesConLevanteCount = computed(() => {
  return filtrarExpedientesPorFase(baseExpedientes.value, 'LEVANTE_AUTORIZADO').length
})

const expedientesCerradosCount = computed(() => {
  return filtrarExpedientesPorFase(baseExpedientes.value, 'CERRADOS').length
})

// Filtrado de Expedientes Aduaneros por Etapa Operativa y Criterios
const filteredExpedientes = computed(() => {
  let list = filtrarExpedientesPorFase(baseExpedientes.value, faseExpediente.value)

  if (filterRegimenExpediente.value) {
    const regFilter = filterRegimenExpediente.value.toUpperCase()
    list = list.filter((e) => {
      const reg = (e.regimenAduanero || '').toUpperCase()
      return reg.includes(regFilter)
    })
  }

  if (filterCanalExpediente.value) {
    list = list.filter((e) => {
      if (filterCanalExpediente.value === 'PENDIENTE') {
        return !e.canalSunat || e.canalSunat === 'PENDIENTE'
      }
      return e.canalSunat === filterCanalExpediente.value
    })
  }

  if (filterEstadoExpediente.value) {
    list = list.filter((e) => e.estadoExpediente === filterEstadoExpediente.value)
  }

  if (searchExpedienteQuery.value) {
    const q = searchExpedienteQuery.value.toLowerCase().trim()
    list = list.filter((e) => {
      return (
        e.codigoExpediente?.toLowerCase().includes(q) ||
        e.codigoCotizacion?.toLowerCase().includes(q) ||
        e.clienteRazonSocial?.toLowerCase().includes(q) ||
        e.clienteNumeroDocumento?.toLowerCase().includes(q) ||
        e.numeroDam?.toLowerCase().includes(q) ||
        e.numeroContenedor?.toLowerCase().includes(q) ||
        e.naveViaje?.toLowerCase().includes(q) ||
        e.agenteAduaneroNombre?.toLowerCase().includes(q)
      )
    })
  }

  return list
})

const goToApertura = (cotizacion = null) => {
  selectedCotizacionParaExpediente.value = cotizacion
  switchTab('apertura')
}

const onExpedienteCreado = (expedienteCreado) => {
  showSuccess(`¡Expediente ${expedienteCreado.codigoExpediente} aperturado exitosamente!`)
  loadData()
  selectedCotizacionParaExpediente.value = null
  switchTab('expedientes')
}

const openCanalModal = (exp) => {
  selectedExpedienteParaTramite.value = exp
  canalLevanteActionType.value = 'canal'
  showCanalLevanteModal.value = true
}

const openLevanteModal = (exp) => {
  selectedExpedienteParaTramite.value = exp
  canalLevanteActionType.value = 'levante'
  showCanalLevanteModal.value = true
}

const onCanalLevanteSaved = (actualizado) => {
  showCanalLevanteModal.value = false
  showSuccess(`Expediente ${actualizado.codigoExpediente} actualizado conforme.`)
  loadData()
}

// Retiro Directo en Terminal y Cierre Final
const showRetiroDirectoModal = ref(false)
const showCierreExpedienteModal = ref(false)
const selectedExpedienteParaAccion = ref(null)

const openRetiroDirectoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRetiroDirectoModal.value = true
}

const onRetiroDirectoSaved = () => {
  showRetiroDirectoModal.value = false
  showSuccess('Acta de retiro directo en terminal registrada exitosamente.')
  loadData()
}

const openCierreModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showCierreExpedienteModal.value = true
}

const onCierreExpedienteSaved = () => {
  showCierreExpedienteModal.value = false
  showSuccess('Expediente aduanero cerrado formalmente con conformidad.')
  loadData()
}


// Cargar Datos
const loadData = async () => {
  loading.value = true
  try {
    const [cotsRes, expsRes] = await Promise.allSettled([
      gestionAduaneraService.getCotizaciones(),
      expedienteService.getExpedientes()
    ])
    if (cotsRes.status === 'fulfilled') cotizaciones.value = filtrarCotizacionesParaSupervision(cotsRes.value || [])
    if (expsRes.status === 'fulfilled') expedientes.value = expsRes.value || []

    const firstFailure = [cotsRes, expsRes].find((r) => r.status === 'rejected')
    if (firstFailure) {
      showError(firstFailure.reason?.response?.data?.message || 'Error al sincronizar datos de Supervisión.')
    }

    // Si había una cotización inspeccionada en vista completa, refrescar su dato
    if (inspectedCotizacion.value) {
      const updated = cotizaciones.value.find((c) => c.idCotizacion === inspectedCotizacion.value.idCotizacion)
      if (updated) inspectedCotizacion.value = updated
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

const {
  subQueueCotizaciones,
  searchQuery,
  filterRegimen: filterRegimenCotizacion,
  sortBy: sortByCotizacion,
  processingAction,
  cotizacionesClasificadas,
  filteredCotizaciones,
  hasActiveFilters: hasActiveCotizacionFilters,
  resetFilters: resetCotizacionFilters,
  ejecutarRevisionTecnica,
  aprobarCotizacion
} = useAduanasCotizaciones({
  cotizaciones,
  authStore,
  notify: { showSuccess, showError, showInfo },
  confirmAction,
  onReload: loadData
})

// ==========================================
// ACCIONES DE AUDITORÍA Y SUPERVISIÓN EN VISTA COMPLETA
// ==========================================
const openInspectModal = (cotizacion) => {
  inspectedCotizacion.value = cotizacion
  inspectedExpediente.value = null
  showAuditoriaCompleta.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openAuditarExpediente = (exp) => {
  inspectedExpediente.value = exp
  inspectedCotizacion.value = null
  showAuditoriaCompleta.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const closeAuditoriaCompleta = () => {
  showAuditoriaCompleta.value = false
  inspectedCotizacion.value = null
  inspectedExpediente.value = null
}

const confirmApprove = async (cotizacion) => {
  await aprobarCotizacion(cotizacion)
}

const handleApproveFromAuditoria = async (cotizacion) => {
  await confirmApprove(cotizacion)
  if (inspectedCotizacion.value) {
    const updated = cotizaciones.value.find((c) => c.idCotizacion === cotizacion.idCotizacion)
    if (updated) inspectedCotizacion.value = updated
  }
}

const handleObserveFromAuditoria = async ({ cotizacion, reason }) => {
  const ok = await ejecutarRevisionTecnica({
    cotizacion,
    estado: 'OBSERVADA',
    motivoObservacion: reason,
    observaciones: 'Observaciones emitidas en auditoría técnica aduanera.'
  })
  if (ok) closeAuditoriaCompleta()
}

const handleRejectFromAuditoria = async ({ cotizacion, reason }) => {
  const ok = await ejecutarRevisionTecnica({
    cotizacion,
    estado: 'RECHAZADA',
    motivoObservacion: reason,
    observaciones: 'Rechazo técnico formal emitido en auditoría aduanera.'
  })
  if (ok) closeAuditoriaCompleta()
}

const handleAperturaFromAuditoria = (cotizacion) => {
  closeAuditoriaCompleta()
  goToApertura(cotizacion)
}

// Acciones rápidas desde la bandeja (Observación / Rechazo)
const openRevisionDialog = (cotizacion, mode) => {
  actionCotizacion.value = cotizacion
  revisionDialogMode.value = mode
  showRevisionDialog.value = true
}

const handleRevisionConfirm = async ({ cotizacion, mode, reason }) => {
  const isObserve = mode === 'observe'
  const ok = await ejecutarRevisionTecnica({
    cotizacion,
    estado: isObserve ? 'OBSERVADA' : 'RECHAZADA',
    motivoObservacion: reason,
    observaciones: isObserve
      ? 'Observaciones emitidas por Supervisión Aduanera.'
      : 'Rechazo técnico emitido por Supervisión Aduanera.'
  })
  if (ok) {
    showRevisionDialog.value = false
  }
}

// Helpers de Estilo y Etiquetas para Expedientes
const getCanalSeverity = (canal) => {
  if (canal === 'VERDE') return 'success'
  if (canal === 'NARANJA') return 'warn'
  if (canal === 'ROJO') return 'danger'
  return 'contrast'
}

const getExpedienteSeverity = (estado) => {
  if (estado === 'LEVANTE_AUTORIZADO' || estado === 'CONCLUIDO') return 'success'
  if (estado === 'ANULADO') return 'danger'
  if (estado === 'CON_CANAL') return 'warn'
  return 'info'
}

const formatRegimen = (regimen) => {
  if (!regimen) return '—'
  return regimen.replace(/_/g, ' ')
}
</script>

<style scoped>
.gestion-aduanera-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
}

.supervision-master-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

/* 1. Barra de Atajos Rápidos y Operatoria */
.supervision-action-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
  flex-wrap: wrap;
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.toolbar-title {
  font-family: var(--font-sans);
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  flex-wrap: wrap;
}

/* 2. Grid de Métricas KPI Estandarizadas */
.metrics-supervision-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
}

@media (max-width: 1100px) {
  .metrics-supervision-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .metrics-supervision-grid {
    grid-template-columns: 1fr;
  }
}

.supervision-kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-3, 0.75rem);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.supervision-kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-border-strong);
}

.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-2, 0.5rem);
}

.kpi-num {
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-subtle);
}

.kpi-label {
  flex: 1;
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  color: var(--color-text-muted);
}

.kpi-icon {
  font-size: 1.1rem;
  color: var(--color-text-subtle);
}

.kpi-icon.warning { color: var(--color-warning-text); }
.kpi-icon.danger { color: var(--color-danger-text); }
.kpi-icon.success { color: var(--color-success-text); }
.kpi-icon.info { color: var(--color-info-text); }
.kpi-icon.primary { color: var(--color-primary); }

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2, 0.5rem);
}

.kpi-value {
  font-family: var(--font-sans);
  font-size: 2.15rem;
  font-weight: 800;
  line-height: 1;
  color: var(--color-text-main);
}

.kpi-badge {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-pill, 9999px);
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border-subtle);
}

.kpi-badge.warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
}

.kpi-badge.danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.kpi-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.kpi-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border-color: var(--color-info-border);
}

.kpi-badge.primary {
  background: var(--color-bg-subtle);
  color: var(--color-primary);
  border-color: var(--color-border-strong);
}

.kpi-footer-note {
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: var(--space-2, 0.5rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-subtle);
}

/* 4. Contenido de Pestaña & Secciones */
.tab-content-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}

/* Sub-colas de Cotizaciones (Pestaña 02) */
.sub-queues-bar {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.5rem 0;
}

.sub-queue-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.85rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  color: var(--color-text-main);
  font-family: var(--font-sans);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.sub-queue-btn:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-accent-copper, #c27847);
}

.sub-queue-btn.active {
  background: var(--color-accent-kicker, #8a4b26);
  color: #ffffff;
  border-color: var(--color-accent-kicker, #8a4b26);
  box-shadow: 0 1px 3px rgba(138, 75, 38, 0.25);
}

.sub-q-num {
  font-family: var(--font-mono);
  font-size: 0.6875rem;
  font-weight: 700;
  opacity: 0.8;
}

.sub-q-badge {
  font-size: 0.6875rem;
  font-weight: 800;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

.sub-queue-btn.active .sub-q-badge {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.sub-q-badge.warning { background: rgba(217, 119, 6, 0.12); color: #d97706; }
.sub-q-badge.danger { background: rgba(220, 38, 38, 0.12); color: #dc2626; }
.sub-q-badge.success { background: rgba(22, 163, 74, 0.12); color: #16a34a; }
.sub-q-badge.muted { background: var(--color-bg-subtle); color: var(--color-text-muted); }

/* Píldoras por Etapa de Despacho (Pestaña 04) */
.stage-pills-bar {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.5rem 0;
}

.stage-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 0.9rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  color: var(--color-text-main);
  font-family: var(--font-sans);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.stage-pill-btn:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-accent-copper, #c27847);
}

.stage-pill-btn.active {
  background: var(--color-accent-kicker, #8a4b26);
  color: #ffffff;
  border-color: var(--color-accent-kicker, #8a4b26);
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(138, 75, 38, 0.25);
}

.phase-idx {
  font-family: var(--font-mono);
  font-size: 0.6875rem;
  font-weight: 700;
  opacity: 0.8;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: var(--space-4, 1rem);
}

.section-title {
  font-family: var(--font-sans);
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.section-controls {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.btn-toggle-mine {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-toggle-mine:hover {
  background-color: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-toggle-mine.active {
  background-color: var(--color-espresso-primary, #352f2a);
  color: #ffffff;
  border-color: var(--color-espresso-primary, #352f2a);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.agent-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.badge-asignado-mio {
  display: inline-block;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  background-color: #d1fae5;
  color: #065f46;
  border: 1px solid #a7f3d0;
  width: fit-content;
}

.filter-select {
  padding: var(--space-2, 0.5rem) 2.25rem var(--space-2, 0.5rem) var(--space-3, 0.75rem);
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-size: var(--font-size-sm, 0.8125rem);
  outline: none;
  cursor: pointer;
}

.filter-select:focus {
  border-color: var(--color-border-strong);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-subtle);
  pointer-events: none;
  font-size: 0.85rem;
}

.search-input {
  padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem) var(--space-2, 0.5rem) 2.25rem;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-size: var(--font-size-sm, 0.8125rem);
  outline: none;
  min-width: 260px;
}

.search-input:focus {
  border-color: var(--color-border-strong);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);
}

/* Botones de Estilo Editorial */
.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: var(--space-2, 0.5rem) var(--space-4, 1rem);
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md, 8px);
  font-family: var(--font-sans);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
  white-space: nowrap;
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: var(--space-2, 0.5rem) var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-family: var(--font-sans);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
  white-space: nowrap;
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-outline-warm.active {
  background: var(--color-bg-subtle);
  border-color: var(--color-primary);
  color: var(--color-text-main);
  font-weight: 700;
}

/* 5. Tabla de Datos Custom SGA */
.table-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.code-column {
  display: flex;
  flex-direction: column;
}

.doc-code {
  font-family: var(--font-mono, monospace);
  font-weight: 700;
  font-size: 0.875rem;
  color: var(--color-text-main);
}

.date-sub {
  font-size: 0.725rem;
  color: var(--color-text-subtle);
}

.client-cell {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.client-name {
  font-weight: 600;
  color: var(--color-text-main);
  font-size: 0.85rem;
}

.client-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.comercial-name {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.regimen-tag {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm, 6px);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.total-amount {
  font-family: var(--font-mono, monospace);
  font-weight: 700;
  color: var(--color-text-main);
  font-size: 0.9375rem;
}

.exp-linked-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.55rem;
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  color: var(--color-success-text);
  border-radius: var(--radius-sm, 6px);
  font-size: 0.75rem;
  font-weight: 700;
  font-family: var(--font-mono, monospace);
}

.exp-code-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.6rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  font-weight: 700;
  font-family: var(--font-mono, monospace);
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast, 0.2s ease);
}

.exp-code-badge:hover {
  border-color: var(--color-primary);
  background: var(--color-bg-muted);
}

.exp-pending-alert-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.25rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1rem 1.5rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.alert-icon-box {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--radius-md, 8px);
  background: var(--color-warning-border);
  color: var(--color-warning-text);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.alert-text-content {
  flex: 1;
  min-width: 240px;
}

.alert-text-content strong {
  font-size: 0.9375rem;
  color: var(--color-warning-text);
  display: block;
}

.alert-text-content p {
  margin: 0.25rem 0 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.btn-alert-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-warning-text);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md, 8px);
  padding: 0.625rem 1.25rem;
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-alert-action:hover {
  filter: brightness(0.9);
  transform: translateY(-1px);
}

.text-muted-dash {
  color: var(--color-text-subtle);
  font-size: 0.8125rem;
}

/* Acciones en la Tabla */
.actions-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.375rem;
}

.action-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.65rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md, 8px);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.35rem 0.55rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.action-btn-outline.approve {
  color: var(--color-success-text);
  border-color: var(--color-success-border);
  background: var(--color-success-bg);
}

.action-btn-outline.approve:hover {
  filter: brightness(0.95);
}

.action-btn-outline.observe {
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
  background: var(--color-warning-bg);
}

.action-btn-outline.observe:hover {
  filter: brightness(0.95);
}

.action-btn-outline.reject {
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
  background: var(--color-danger-bg);
}

.action-btn-outline.reject:hover {
  filter: brightness(0.95);
}

/* Empty y Loading States */
.empty-state {
  padding: var(--space-10, 2.5rem);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  font-size: 2.5rem;
  color: var(--color-text-subtle);
  margin-bottom: var(--space-3, 0.75rem);
}

.empty-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0 0 0.25rem 0;
}

.empty-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0;
}

.loading-state {
  padding: var(--space-8, 2rem);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3, 0.75rem);
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

/* Modales Auxiliares de Confirmación y Acciones */
.modal-form-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
}

.modal-desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.custom-textarea {
  width: 100%;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-main);
  padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
  font-size: 0.875rem;
}

.custom-textarea:focus {
  border-color: var(--color-border-strong);
  box-shadow: 0 0 0 3px rgba(53, 47, 42, 0.08);
}

.dialog-actions-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.btn-warm-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: var(--space-2, 0.5rem) var(--space-4, 1rem);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md, 8px);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-warm-action:hover:not(:disabled) {
  filter: brightness(0.95);
}

.btn-danger-action {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: var(--space-2, 0.5rem) var(--space-4, 1rem);
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md, 8px);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-danger-action:hover:not(:disabled) {
  filter: brightness(0.95);
}

/* Animaciones */
.fade-in {
  animation: fadeIn 0.25s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
