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
      <!-- 1. Barra de Atajos Rápidos y Operatoria Aduanera -->
      <div class="supervision-action-toolbar">
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

      <!-- 2. Grid de Métricas KPI Estandarizadas (Solo en Pestaña Cotizaciones) -->
      <div v-if="activeTab === 'cotizaciones'" class="metrics-supervision-grid">
        <!-- KPI 01: Por Auditar -->
        <div class="supervision-kpi-card pending-card" @click="subQueueCotizaciones = 'nuevas'">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Nuevas por Auditar</span>
            <i class="pi pi-clock kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-warning">{{ cotizacionesClasificadas.nuevas.length }}</span>
            <span class="kpi-badge warning">Revisión Inicial</span>
          </div>
          <div class="kpi-footer-note">
            <span>Propuestas comerciales entrantes</span>
          </div>
        </div>

        <!-- KPI 02: En Observación Técnica / Subsanadas -->
        <div class="supervision-kpi-card alert-card" @click="subQueueCotizaciones = 'subsanadas'">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Subsanadas / Reingresos</span>
            <i class="pi pi-file-edit kpi-icon danger"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-danger">{{ cotizacionesClasificadas.subsanadas.length }}</span>
            <span class="kpi-badge danger">Prioridad Alta</span>
          </div>
          <div class="kpi-footer-note">
            <span>Para subsanación o reingresadas</span>
          </div>
        </div>

        <!-- KPI 03: En Espera de Pago -->
        <div class="supervision-kpi-card wait-card" @click="subQueueCotizaciones = 'pendientesPago'">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">En Espera de Pago</span>
            <i class="pi pi-wallet kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value">{{ cotizacionesClasificadas.pendientesPago.length }}</span>
            <span class="kpi-badge">Pendiente Finanzas</span>
          </div>
          <div class="kpi-footer-note">
            <span>Aprobadas técnicamente sin pago</span>
          </div>
        </div>

        <!-- KPI 04: Listas para Aperturar Expediente -->
        <div class="supervision-kpi-card ready-card" @click="subQueueCotizaciones = 'listasApertura'">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Listas para Expediente</span>
            <i class="pi pi-folder-open kpi-icon primary"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-primary">{{ cotizacionesClasificadas.listasApertura.length }}</span>
            <span class="kpi-badge primary">Con Pago Confirmado</span>
          </div>
          <div class="kpi-footer-note">
            <span>Aprobadas con pago validado</span>
          </div>
        </div>
      </div>

      <!-- 2. Grid de Métricas de Expedientes (Solo en Pestaña Expedientes) -->
      <div v-else-if="activeTab === 'expedientes'" class="metrics-supervision-grid">
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
      <div v-else-if="activeTab === 'cotizaciones'" class="tab-content-section fade-in">
        <!-- Barra de Sub-Colas Operativas -->
        <div class="sub-queues-bar">
          <button
            type="button"
            :class="['sub-queue-btn', { active: subQueueCotizaciones === 'nuevas' }]"
            @click="subQueueCotizaciones = 'nuevas'"
          >
            <span class="sub-q-num">01</span>
            <span>Nuevas por Auditar</span>
            <span class="sub-q-badge warning" v-if="cotizacionesClasificadas.nuevas.length > 0">
              {{ cotizacionesClasificadas.nuevas.length }}
            </span>
          </button>

          <button
            type="button"
            :class="['sub-queue-btn', { active: subQueueCotizaciones === 'subsanadas' }]"
            @click="subQueueCotizaciones = 'subsanadas'"
          >
            <span class="sub-q-num">02</span>
            <span>Subsanadas / Reingresos</span>
            <span class="sub-q-badge danger" v-if="cotizacionesClasificadas.subsanadas.length > 0">
              {{ cotizacionesClasificadas.subsanadas.length }}
            </span>
          </button>

          <button
            type="button"
            :class="['sub-queue-btn', { active: subQueueCotizaciones === 'pendientesPago' }]"
            @click="subQueueCotizaciones = 'pendientesPago'"
          >
            <span class="sub-q-num">03</span>
            <span>En Espera de Pago</span>
            <span class="sub-q-badge muted" v-if="cotizacionesClasificadas.pendientesPago.length > 0">
              {{ cotizacionesClasificadas.pendientesPago.length }}
            </span>
          </button>

          <button
            type="button"
            :class="['sub-queue-btn', { active: subQueueCotizaciones === 'listasApertura' }]"
            @click="subQueueCotizaciones = 'listasApertura'"
          >
            <span class="sub-q-num">04</span>
            <span>Listas para Apertura</span>
            <span class="sub-q-badge success" v-if="cotizacionesClasificadas.listasApertura.length > 0">
              {{ cotizacionesClasificadas.listasApertura.length }}
            </span>
          </button>

          <button
            type="button"
            :class="['sub-queue-btn', { active: subQueueCotizaciones === 'historico' }]"
            @click="subQueueCotizaciones = 'historico'"
          >
            <span class="sub-q-num">05</span>
            <span>Histórico / Concluidas</span>
            <span class="sub-q-badge" v-if="cotizacionesClasificadas.historico.length > 0">
              {{ cotizacionesClasificadas.historico.length }}
            </span>
          </button>
        </div>

        <div class="section-header-row">
          <div>
            <span class="badge-kicker">CONTROL ADUANERO</span>
            <h3 class="section-title">
              {{ getSubQueueTitle(subQueueCotizaciones) }} ({{ filteredCotizaciones.length }})
            </h3>
          </div>

          <div class="section-controls">
            <!-- Búsqueda Rápida -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por código, cliente o RUC..."
              />
            </div>

            <!-- Botón de Recarga -->
            <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading">
              <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
              <span>Actualizar</span>
            </button>
          </div>
        </div>

        <!-- Tabla de Cotizaciones -->
        <div class="table-container">
          <DataTable
            :value="filteredCotizaciones"
            :loading="loading"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idCotizacion"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-check-circle empty-icon"></i>
                <p class="empty-title">No hay cotizaciones con este criterio</p>
                <p class="empty-desc">Las cotizaciones emitidas por los ejecutivos de ventas aparecerán aquí para su validación técnica.</p>
              </div>
            </template>

            <template #loading>
              <div class="loading-state">
                <i class="pi pi-spin pi-spinner"></i>
                <span>Cargando expedientes aduaneros...</span>
              </div>
            </template>

            <Column header="N° Cotización">
              <template #body="{ data: c }">
                <div class="code-column">
                  <span class="doc-code font-mono">{{ c.codigoCotizacion }}</span>
                  <span class="date-sub font-mono">{{ c.fechaEmision }}</span>
                </div>
              </template>
            </Column>

            <Column header="Cliente / RUC">
              <template #body="{ data: c }">
                <div class="client-cell">
                  <span class="client-name">{{ c.clienteRazonSocial || 'Cliente no asignado' }}</span>
                  <span class="client-sub font-mono">RUC/Doc: {{ c.clienteNumeroDocumento }}</span>
                </div>
              </template>
            </Column>

            <Column header="Comercial">
              <template #body="{ data: c }">
                <span class="comercial-name">{{ c.usuarioComercialNombre || 'Asesor Comercial' }}</span>
              </template>
            </Column>

            <Column header="Régimen Aduanero">
              <template #body="{ data: c }">
                <span class="regimen-tag">{{ formatRegimen(c.regimenAduanero) }}</span>
              </template>
            </Column>

            <Column header="Total Cotizado">
              <template #body="{ data: c }">
                <span class="total-amount font-mono">${{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
              </template>
            </Column>

            <Column header="Estado">
              <template #body="{ data: c }">
                <Tag
                  :value="getEstadoLabel(c.estado)"
                  :severity="getEstadoSeverity(c.estado)"
                  rounded
                />
              </template>
            </Column>

            <Column header="Pago / Anticipo">
              <template #body="{ data: c }">
                <div v-if="c.estado === 'APROBADA'" class="client-cell">
                  <Tag
                    :value="getEstadoPagoLabel(c.estadoPago)"
                    :severity="getEstadoPagoSeverity(c.estadoPago)"
                    rounded
                  />
                  <span v-if="c.totalAbonado > 0" class="client-sub font-mono font-bold" style="color: var(--color-success-text);">
                    Abono: ${{ Number(c.totalAbonado).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                  </span>
                  <span v-else class="client-sub text-muted">
                    Sin abono confirmado
                  </span>
                </div>
                <span v-else class="text-muted-dash">—</span>
              </template>
            </Column>

            <Column header="Expediente Aduanero">
              <template #body="{ data: c }">
                <div v-if="c.tieneExpediente" class="exp-linked-tag font-mono">
                  <i class="pi pi-folder"></i>
                  <span>{{ c.codigoExpediente }}</span>
                </div>
                <span v-else-if="c.estado === 'APROBADA' && c.tienePagoAprobado" class="text-muted-dash" style="color: var(--color-success-text); font-weight: 700;">Listo para Expediente</span>
                <span v-else-if="c.estado === 'APROBADA'" class="text-muted-dash" style="color: var(--color-warning-text); font-weight: 600;">Requiere Pago Finanzas</span>
                <span v-else class="text-muted-dash">—</span>
              </template>
            </Column>

            <Column header="Acciones de Supervisión" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: c }">
                <div class="actions-wrapper">
                  <!-- Botón Principal: Auditar en Vista Completa (Modificado según requerimiento) -->
                  <button
                    type="button"
                    class="action-btn-primary"
                    title="Auditar en vista completa con expediente resumido y documentos"
                    @click="openInspectModal(c)"
                  >
                    <i class="pi pi-eye"></i>
                    <span>Auditar</span>
                  </button>

                  <!-- Botón de Documentos Adjuntos Rápidos -->
                  <button
                    type="button"
                    class="action-btn-outline"
                    title="Gestionar Documentos y Anexos"
                    @click="openDocumentosCotizacion(c)"
                  >
                    <i class="pi pi-paperclip"></i>
                    <span>Docs</span>
                  </button>

                  <!-- Botón Rápido de Aprobación -->
                  <button
                    v-if="['PENDIENTE', 'EN_REVISION', 'ENVIADA', 'REVISION', 'OBSERVADA'].includes(c.estado)"
                    type="button"
                    class="action-btn-outline approve"
                    title="Aprobar técnicamente para apertura de Expediente Aduanero"
                    @click="confirmApprove(c)"
                  >
                    <i class="pi pi-check"></i>
                    <span>Aprobar</span>
                  </button>

                  <!-- Botón Rápido de Observación / Revisión -->
                  <button
                    v-if="['PENDIENTE', 'EN_REVISION', 'ENVIADA'].includes(c.estado)"
                    type="button"
                    class="action-btn-outline observe"
                    title="Devolver para Revisión a Ventas"
                    @click="openObservationModal(c)"
                  >
                    <i class="pi pi-file-edit"></i>
                    <span>Observar</span>
                  </button>

                  <!-- Botón Rápido de Rechazo -->
                  <button
                    v-if="['PENDIENTE', 'EN_REVISION', 'ENVIADA', 'REVISION', 'OBSERVADA'].includes(c.estado)"
                    type="button"
                    class="action-btn-outline reject"
                    title="Rechazar Cotización"
                    @click="openRejectModal(c)"
                  >
                    <i class="pi pi-times"></i>
                  </button>

                  <!-- Acciones de Expediente cuando está Aprobada -->
                  <template v-if="c.estado === 'APROBADA' && !c.tieneExpediente">
                    <button
                      v-if="c.tienePagoAprobado"
                      type="button"
                      class="action-btn-outline"
                      style="border-color: var(--color-accent-kicker); color: var(--color-accent-kicker); font-weight: 700;"
                      title="Aperturar Expediente Aduanero para esta cotización"
                      @click="goToApertura(c)"
                    >
                      <i class="pi pi-folder-open"></i>
                      <span>+ Expediente</span>
                    </button>

                    <button
                      v-else
                      type="button"
                      class="action-btn-outline"
                      style="border-color: var(--color-border); color: var(--color-text-subtle); cursor: not-allowed; background: var(--color-bg-subtle);"
                      disabled
                      title="Requiere que el área de Finanzas apruebe el pago o adelanto antes de generar el expediente"
                    >
                      <i class="pi pi-lock"></i>
                      <span>+ Expediente</span>
                    </button>
                  </template>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>

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
    <!-- Modal: Solicitar Revisión / Observación Rápida desde Tabla -->
    <Dialog
      v-model:visible="showObservationDialog"
      modal
      header="Solicitar Corrección a Ventas (Revisión)"
      :style="{ width: '540px', maxWidth: '95vw' }"
      class="custom-sga-dialog"
    >
      <div class="modal-form-content">
        <p class="modal-desc">
          Especifique las observaciones técnicas o financieras para que el asesor comercial corrija la cotización
          <strong>{{ actionCotizacion?.codigoCotizacion }}</strong>. Una vez corregida, volverá automáticamente a su bandeja para aprobación.
        </p>

        <div class="form-group">
          <label class="form-label">Detalle de Observación / Motivo de Revisión:</label>
          <Textarea
            v-model="observationReason"
            rows="4"
            class="custom-textarea"
            placeholder="Ejemplo: Rectificar partida arancelaria para régimen definitivo o adjuntar póliza de flete estimada..."
          />
        </div>
      </div>

      <template #footer>
        <div class="dialog-actions-row">
          <button type="button" class="btn-outline-warm" @click="showObservationDialog = false">
            <span>Cancelar</span>
          </button>
          <button
            type="button"
            class="btn-warm-action"
            :disabled="!observationReason.trim() || processingAction"
            @click="executeRevision"
          >
            <i v-if="processingAction" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-send"></i>
            <span>Enviar a Revisión</span>
          </button>
        </div>
      </template>
    </Dialog>

    <!-- Modal: Rechazar Cotización Rápido desde Tabla -->
    <Dialog
      v-model:visible="showRejectDialog"
      modal
      header="Rechazar Propuesta Comercial"
      :style="{ width: '540px', maxWidth: '95vw' }"
      class="custom-sga-dialog"
    >
      <div class="modal-form-content">
        <p class="modal-desc">
          Indique la razón formal por la cual la cotización <strong>{{ actionCotizacion?.codigoCotizacion }}</strong> no es viable técnica ni legalmente.
        </p>

        <div class="form-group">
          <label class="form-label">Motivo de Rechazo Aduanero:</label>
          <Textarea
            v-model="rejectReason"
            rows="4"
            class="custom-textarea"
            placeholder="Indique la causal de rechazo (incompatibilidad de régimen, restricciones de SUNAT, etc.)..."
          />
        </div>
      </div>

      <template #footer>
        <div class="dialog-actions-row">
          <button type="button" class="btn-outline-warm" @click="showRejectDialog = false">
            <span>Cancelar</span>
          </button>
          <button
            type="button"
            class="btn-danger-action"
            :disabled="!rejectReason.trim() || processingAction"
            @click="executeReject"
          >
            <i v-if="processingAction" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-times"></i>
            <span>Confirmar Rechazo</span>
          </button>
        </div>
      </template>
    </Dialog>

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
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
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
import { getCredencialesPostAprobacionNotification } from '@/features/aduanas/utils/aprobacionAduaneraUtils'
import { filtrarCotizacionesParaSupervision } from '../utils/supervisionCotizacionUtils'
import {
  clasificarCotizacionesAduaneras,
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

// Sub-cola de Cotizaciones y Fase de Expedientes
const subQueueCotizaciones = ref('nuevas') // 'nuevas' | 'subsanadas' | 'pendientesPago' | 'listasApertura' | 'historico' | 'todas'
const faseExpediente = ref('TODOS') // 'TODOS' | 'PENDIENTE_CANAL' | 'CONTROL_SUNAT' | 'LEVANTE_AUTORIZADO' | 'CERRADOS'

// Detección de Rol Operativo
const isAgenteAduanero = computed(() => {
  return authStore.userRole === 'AGENTE_ADUANERO' || authStore.user?.rol === 'AGENTE_ADUANERO'
})

// Filtros y Búsqueda
const searchQuery = ref('')
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

// Modales rápidos desde la tabla
const showObservationDialog = ref(false)
const observationReason = ref('')

const showRejectDialog = ref(false)
const rejectReason = ref('')

const actionCotizacion = ref(null)
const processingAction = ref(false)

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

const getSubQueueTitle = (queue) => {
  const map = {
    nuevas: 'Cotizaciones Nuevas para Revisión Técnica',
    subsanadas: 'Cotizaciones Subsanadas / Reingresos Prioritarios',
    pendientesPago: 'Cotizaciones Aprobadas en Espera de Pago (Finanzas)',
    listasApertura: 'Cotizaciones Listas para Aperturar Expediente',
    historico: 'Historial de Cotizaciones Concluidas y Rechazadas',
    todas: 'Todas las Cotizaciones Registradas'
  }
  return map[queue] || 'Bandeja de Cotizaciones'
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

// Clasificación reactiva de cotizaciones
const cotizacionesClasificadas = computed(() => {
  return clasificarCotizacionesAduaneras(cotizaciones.value)
})

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

// Filtrado de Cotizaciones por Sub-Cola
const filteredCotizaciones = computed(() => {
  let list = []
  const clasif = cotizacionesClasificadas.value
  switch (subQueueCotizaciones.value) {
    case 'nuevas':
      list = clasif.nuevas
      break
    case 'subsanadas':
      list = clasif.subsanadas
      break
    case 'pendientesPago':
      list = clasif.pendientesPago
      break
    case 'listasApertura':
      list = clasif.listasApertura
      break
    case 'historico':
      list = clasif.historico
      break
    default:
      list = cotizaciones.value
  }

  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase().trim()
    list = list.filter((c) => {
      return (
        c.codigoCotizacion?.toLowerCase().includes(q) ||
        c.clienteRazonSocial?.toLowerCase().includes(q) ||
        c.clienteNumeroDocumento?.toLowerCase().includes(q) ||
        c.usuarioComercialNombre?.toLowerCase().includes(q) ||
        c.codigoExpediente?.toLowerCase().includes(q) ||
        c.mercanciaCodigo?.toLowerCase().includes(q)
      )
    })
  }

  return list
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

const handleApproveFromAuditoria = async (cotizacion) => {
  await confirmApprove(cotizacion)
  if (inspectedCotizacion.value) {
    const updated = cotizaciones.value.find((c) => c.idCotizacion === cotizacion.idCotizacion)
    if (updated) inspectedCotizacion.value = updated
  }
}

const handleObserveFromAuditoria = async ({ cotizacion, reason }) => {
  processingAction.value = true
  try {
    await gestionAduaneraService.revisarCotizacion(cotizacion.idCotizacion, {
      idAgenteRevisor: authStore.user?.idUsuario || 1,
      estado: 'OBSERVADA',
      motivoObservacion: reason,
      observaciones: 'Observaciones emitidas en auditoría técnica aduanera.'
    })
    showInfo(`Cotización ${cotizacion.codigoCotizacion} devuelta como Observada para corrección por Ventas.`)
    closeAuditoriaCompleta()
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al solicitar corrección.')
  } finally {
    processingAction.value = false
  }
}

const handleRejectFromAuditoria = async ({ cotizacion, reason }) => {
  processingAction.value = true
  try {
    await gestionAduaneraService.revisarCotizacion(cotizacion.idCotizacion, {
      idAgenteRevisor: authStore.user?.idUsuario || 1,
      estado: 'RECHAZADA',
      motivoObservacion: reason,
      observaciones: 'Rechazo técnico formal emitido en auditoría aduanera.'
    })
    showInfo(`Cotización ${cotizacion.codigoCotizacion} marcada como Rechazada.`)
    closeAuditoriaCompleta()
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al rechazar cotización.')
  } finally {
    processingAction.value = false
  }
}

const handleAperturaFromAuditoria = (cotizacion) => {
  closeAuditoriaCompleta()
  goToApertura(cotizacion)
}

// Acciones rápidas desde la tabla
const confirmApprove = async (cotizacion) => {
  const confirmed = await confirmAction({
    message: `¿Confirma la aprobación de la Cotización ${cotizacion.codigoCotizacion}? Al aprobar, la propuesta queda formalizada técnicamente para apertura de Expediente Aduanero.`,
    header: 'Aprobación Técnica Aduanera',
    icon: 'pi pi-verified',
    acceptLabel: 'Aprobar Propuesta',
    rejectLabel: 'Cancelar',
    acceptClass: 'sga-confirm-accept-btn',
    rejectClass: 'sga-confirm-reject-btn'
  })
  if (!confirmed) return

  try {
    loading.value = true
    await gestionAduaneraService.revisarCotizacion(cotizacion.idCotizacion, {
      idAgenteRevisor: authStore.user?.idUsuario || 1,
      estado: 'APROBADA',
      motivoObservacion: null,
      observaciones: 'Aprobada formalmente por Supervisión Aduanera.'
    })
    showSuccess(`¡Cotización ${cotizacion.codigoCotizacion} Aprobada! Lista para apertura de Expediente Aduanero.`)

    const notifCred = getCredencialesPostAprobacionNotification(cotizacion)
    showInfo(notifCred.message, notifCred.title, notifCred.life)

    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'No se pudo aprobar la cotización.')
  } finally {
    loading.value = false
  }
}

const openObservationModal = (cotizacion) => {
  actionCotizacion.value = cotizacion
  observationReason.value = cotizacion.motivoObservacion || ''
  showObservationDialog.value = true
}

const executeRevision = async () => {
  if (!actionCotizacion.value || !observationReason.value.trim()) return

  processingAction.value = true
  try {
    await gestionAduaneraService.revisarCotizacion(actionCotizacion.value.idCotizacion, {
      idAgenteRevisor: authStore.user?.idUsuario || 1,
      estado: 'OBSERVADA',
      motivoObservacion: observationReason.value.trim(),
      observaciones: 'Observaciones emitidas por Supervisión Aduanera.'
    })
    showInfo(`Cotización ${actionCotizacion.value.codigoCotizacion} marcada como Observada para corrección por Ventas.`)
    showObservationDialog.value = false
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al cambiar estado a Revisión.')
  } finally {
    processingAction.value = false
  }
}

const openRejectModal = (cotizacion) => {
  actionCotizacion.value = cotizacion
  rejectReason.value = ''
  showRejectDialog.value = true
}

const executeReject = async () => {
  if (!actionCotizacion.value || !rejectReason.value.trim()) return

  processingAction.value = true
  try {
    await gestionAduaneraService.revisarCotizacion(actionCotizacion.value.idCotizacion, {
      idAgenteRevisor: authStore.user?.idUsuario || 1,
      estado: 'RECHAZADA',
      motivoObservacion: rejectReason.value.trim(),
      observaciones: 'Rechazo técnico emitido por Supervisión Aduanera.'
    })
    showInfo(`Cotización ${actionCotizacion.value.codigoCotizacion} marcada como Rechazada.`)
    showRejectDialog.value = false
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al rechazar cotización.')
  } finally {
    processingAction.value = false
  }
}

// Helpers de Estilo y Etiquetas
const getEstadoPagoLabel = (st) => {
  switch (st) {
    case 'ADELANTO_APROBADO': return 'Anticipo Aprobado'
    case 'PAGADO_TOTAL': return 'Pagado 100%'
    case 'PENDIENTE_REVISION': return 'En Revisión Finanzas'
    case 'OBSERVADO': return 'Pago Observado'
    default: return 'Pendiente de Pago'
  }
}

const getEstadoPagoSeverity = (st) => {
  switch (st) {
    case 'ADELANTO_APROBADO':
    case 'PAGADO_TOTAL':
      return 'success'
    case 'PENDIENTE_REVISION':
      return 'warn'
    case 'OBSERVADO':
      return 'danger'
    default:
      return 'info'
  }
}

const getEstadoLabel = (estado) => {
  const map = {
    ENVIADA: 'Por Revisar (Pendiente)',
    PENDIENTE: 'Por Revisar (Pendiente)',
    EN_REVISION: 'En Evaluación Técnica',
    REVISION: 'Observada (Para Corrección)',
    OBSERVADA: 'Observada (Para Corrección)',
    APROBADA: 'Aprobada',
    RECHAZADA: 'Rechazada',
    ANULADA: 'Anulada'
  }
  return map[estado] || estado
}

const getEstadoSeverity = (estado) => {
  if (estado === 'APROBADA') return 'success'
  if (estado === 'RECHAZADA' || estado === 'ANULADA') return 'danger'
  if (estado === 'REVISION' || estado === 'OBSERVADA') return 'warn'
  if (estado === 'EN_REVISION') return 'info'
  return 'contrast'
}

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
