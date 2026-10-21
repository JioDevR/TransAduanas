<template>
  <div class="gestion-aduanera-view">
    <!-- ============================================================== -->
    <!-- VISTA COMPLETA: AUDITORÍA TÉCNICA DEL EXPEDIENTE / COTIZACIÓN   -->
    <!-- ============================================================== -->
    <AuditoriaExpedienteModal
      v-if="showAuditoriaCompleta && inspectedCotizacion"
      :cotizacion="inspectedCotizacion"
      :processing="processingAction"
      @back="showAuditoriaCompleta = false"
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
            class="btn-espresso"
            @click="openAperturaModal(null)"
            title="Aperturar un nuevo expediente aduanero formal"
          >
            <i class="pi pi-plus"></i>
            <span>+ Aperturar Expediente</span>
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

      <!-- 2. Grid de Métricas KPI Estandarizadas (Sección 6.3 del manual SGA) -->
      <!-- A. Métricas de Cotizaciones (Pestaña 1) -->
      <div v-if="activeTab === 'cotizaciones'" class="metrics-supervision-grid">
        <!-- KPI 01: Por Auditar -->
        <div class="supervision-kpi-card pending-card" @click="filterEstado = 'PENDIENTE'">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Por Auditar (Pendientes)</span>
            <i class="pi pi-clock kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-warning">{{ enviadasCount }}</span>
            <span class="kpi-badge warning">En Cola</span>
          </div>
          <div class="kpi-footer-note">
            <span>Propuestas comerciales para revisión</span>
          </div>
        </div>

        <!-- KPI 02: En Observación Técnica -->
        <div class="supervision-kpi-card alert-card" @click="filterEstado = 'OBSERVADA'">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">En Observación Técnica</span>
            <i class="pi pi-file-edit kpi-icon danger"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-danger">{{ revisionCount }}</span>
            <span class="kpi-badge danger">Corrección Solicitada</span>
          </div>
          <div class="kpi-footer-note">
            <span>Para subsanación por Ventas</span>
          </div>
        </div>

        <!-- KPI 03: Cotizaciones Aprobadas -->
        <div class="supervision-kpi-card success-card" @click="filterEstado = 'APROBADA'">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">Cotizaciones Aprobadas</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-success">{{ aprobadasCount }}</span>
            <span class="kpi-badge success">Conforme</span>
          </div>
          <div class="kpi-footer-note">
            <span>Aprobadas técnicamente</span>
          </div>
        </div>

        <!-- KPI 04: Listas para Aperturar Expediente -->
        <div class="supervision-kpi-card ready-card" @click="filterEstado = 'APROBADA'">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Listas para Expediente</span>
            <i class="pi pi-folder-open kpi-icon primary"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-primary">{{ cotizacionesAprobadasSinExpediente.length }}</span>
            <span class="kpi-badge primary">Con Pago Confirmado</span>
          </div>
          <div class="kpi-footer-note">
            <span>Aprobadas con pago verificado en Finanzas</span>
          </div>
        </div>
      </div>

      <!-- B. Métricas de Expedientes (Pestaña 2) -->
      <div v-else class="metrics-supervision-grid">
        <!-- KPI 01: Total Expedientes -->
        <div class="supervision-kpi-card" @click="filterEstadoExpediente = ''">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Expedientes Registrados</span>
            <i class="pi pi-folder kpi-icon"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value">{{ expedientes.length }}</span>
            <span class="kpi-badge">Total Activos</span>
          </div>
          <div class="kpi-footer-note">
            <span>Operaciones aduaneras en curso</span>
          </div>
        </div>

        <!-- KPI 02: Canal Pendiente -->
        <div class="supervision-kpi-card pending-card" @click="filterCanalExpediente = 'PENDIENTE'">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Canal SUNAT Pendiente</span>
            <i class="pi pi-clock kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-warning">{{ expedientesPendienteCanalCount }}</span>
            <span class="kpi-badge warning">Por Asignar</span>
          </div>
          <div class="kpi-footer-note">
            <span>Esperando numeración de DAM</span>
          </div>
        </div>

        <!-- KPI 03: Con Canal Asignado -->
        <div class="supervision-kpi-card info-card" @click="filterCanalExpediente = 'VERDE'">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">Con Canal Asignado</span>
            <i class="pi pi-shield kpi-icon info"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value text-info">{{ expedientesConCanalCount }}</span>
            <span class="kpi-badge info">Verde / Naranja / Rojo</span>
          </div>
          <div class="kpi-footer-note">
            <span>En aforo o control documentario</span>
          </div>
        </div>

        <!-- KPI 04: Levante Autorizado -->
        <div class="supervision-kpi-card success-card" @click="filterEstadoExpediente = 'LEVANTE_AUTORIZADO'">
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

      <!-- 3. Barra de Pestañas Numeradas (01, 02) -->
      <div class="tabs-nav-bar">
        <button
          type="button"
          :class="['tab-btn', { active: activeTab === 'cotizaciones' }]"
          @click="switchTab('cotizaciones')"
        >
          <span class="tab-idx">01</span>
          <i class="pi pi-inbox"></i>
          <span>Bandeja de Cotizaciones Aduaneras ({{ cotizaciones.length }})</span>
        </button>

        <button
          type="button"
          :class="['tab-btn', { active: activeTab === 'expedientes' }]"
          @click="switchTab('expedientes')"
        >
          <span class="tab-idx">02</span>
          <i class="pi pi-folder-open"></i>
          <span>Gestión Integral de Expedientes ({{ expedientes.length }})</span>
        </button>
      </div>

      <!-- ============================================================== -->
      <!-- PESTAÑA 01: BANDEJA DE COTIZACIONES ADUANERAS                  -->
      <!-- ============================================================== -->
      <div v-if="activeTab === 'cotizaciones'" class="tab-content-section fade-in">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">CONTROL ADUANERO</span>
            <h3 class="section-title">Expedientes de Cotización para Revisión Técnica</h3>
          </div>

          <div class="section-controls">
            <!-- Filtro por Estado -->
            <select v-model="filterEstado" class="filter-select">
              <option value="">Todos los Estados</option>
              <option value="PENDIENTE">Por Revisar (Pendientes)</option>
              <option value="OBSERVADA">En Observación (Observadas)</option>
              <option value="APROBADA">Aprobadas</option>
              <option value="RECHAZADA">Rechazadas</option>
            </select>

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
                      @click="openAperturaModal(c)"
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
      <!-- PESTAÑA 02: GESTIÓN INTEGRAL DE EXPEDIENTES ADUANEROS          -->
      <!-- ============================================================== -->
      <div v-else-if="activeTab === 'expedientes'" class="tab-content-section fade-in">
        <!-- Alerta de Cotizaciones Aprobadas Pendientes de Apertura -->
        <div v-if="cotizacionesAprobadasSinExpediente.length > 0" class="exp-pending-alert-banner">
          <div class="alert-icon-box">
            <i class="pi pi-bell"></i>
          </div>
          <div class="alert-text-content">
            <strong>{{ cotizacionesAprobadasSinExpediente.length }} Cotización(es) Aprobada(s) Pendiente(s) de Apertura</strong>
            <p>Existen propuestas comerciales con pago verificado listas para generar su expediente aduanero operativo.</p>
          </div>
          <button
            type="button"
            class="btn-alert-action"
            @click="openAperturaModal(null)"
          >
            <i class="pi pi-plus-circle"></i>
            <span>Aperturar Expediente Ahora</span>
          </button>
        </div>

        <div class="section-header-row">
          <div>
            <span class="badge-kicker">OPERACIONES ADUANERAS // CONTROL DAM & LEVANTE</span>
            <h3 class="section-title">Directorio y Gestión Integral de Expedientes Aduaneros</h3>
          </div>

          <div class="section-controls">
            <!-- Filtro por Canal SUNAT -->
            <select v-model="filterCanalExpediente" class="filter-select">
              <option value="">Todos los Canales SUNAT</option>
              <option value="PENDIENTE">Canal Pendiente</option>
              <option value="VERDE">Canal VERDE (Levante Inmediato)</option>
              <option value="NARANJA">Canal NARANJA (Documentario)</option>
              <option value="ROJO">Canal ROJO (Reconocimiento Físico)</option>
            </select>

            <!-- Filtro por Estado de Expediente -->
            <select v-model="filterEstadoExpediente" class="filter-select">
              <option value="">Todos los Estados</option>
              <option value="ASIGNADO">Asignado</option>
              <option value="EN_TRAMITE">En Trámite</option>
              <option value="CON_CANAL">Con Canal Asignado</option>
              <option value="LEVANTE_AUTORIZADO">Levante Autorizado</option>
              <option value="CERRADO">Cerrado / Concluido</option>
              <option value="ANULADO">Anulado</option>
            </select>

            <!-- Búsqueda Rápida de Expedientes -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchExpedienteQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por código, DAM, cliente o contenedor..."
              />
            </div>

            <!-- Botón de Recarga -->
            <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading">
              <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
              <span>Actualizar</span>
            </button>

            <!-- Botón Principal: + Aperturar Expediente -->
            <button
              type="button"
              class="btn-espresso"
              @click="openAperturaModal(null)"
              title="Aperturar un nuevo expediente aduanero a partir de una cotización aprobada"
            >
              <i class="pi pi-plus"></i>
              <span>+ Aperturar Expediente</span>
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
                <p class="empty-title">No hay expedientes aduaneros registrados</p>
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
                <div class="exp-code-badge" @click="openDetalleExpediente(exp)" title="Clic para ver ficha completa">
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
                    title="Ver Ficha y Auditoría Completa del Expediente"
                    @click="openDetalleExpediente(exp)"
                  >
                    <i class="pi pi-eye"></i>
                    <span>Detalle</span>
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

                  <button
                    type="button"
                    class="action-btn-outline"
                    title="Registrar Canal SUNAT y Declaración DAM"
                    @click="openCanalModal(exp)"
                  >
                    <i class="pi pi-compass"></i>
                    <span>DAM/Canal</span>
                  </button>

                  <button
                    v-if="!exp.fechaLevanteAutorizacion"
                    type="button"
                    class="action-btn-outline approve"
                    title="Autorizar Levante Aduanero"
                    @click="openLevanteModal(exp)"
                  >
                    <i class="pi pi-shield"></i>
                    <span>Levante</span>
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

    <!-- Modal: Apertura de Expediente Aduanero -->
    <AperturaExpedienteModal
      :visible="showAperturaModal"
      :cotizacion="selectedCotizacionParaExpediente"
      :cotizaciones-disponibles="cotizacionesAprobadasSinExpediente"
      @close="showAperturaModal = false"
      @saved="onExpedienteAperturado"
    />

    <!-- Modal: Detalle Integral del Expediente Aduanero -->
    <DetalleExpedienteModal
      :visible="showDetalleModal"
      :expediente="selectedExpedienteParaDetalle"
      @close="showDetalleModal = false"
      @open-docs="openDocumentosExpediente"
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Textarea from 'primevue/textarea'
import { gestionAduaneraService } from '../services/gestionAduaneraService'
import { expedienteService } from '../services/expedienteService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import AperturaExpedienteModal from '../components/AperturaExpedienteModal.vue'
import DetalleExpedienteModal from '../components/DetalleExpedienteModal.vue'
import CanalLevanteModal from '../components/CanalLevanteModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import AuditoriaExpedienteModal from '../components/AuditoriaExpedienteModal.vue'

const authStore = useAuthStore()
const navigationStore = useNavigationStore()

const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction } = useConfirmation()

const loading = ref(false)
const activeTab = ref('cotizaciones') // 'cotizaciones' | 'expedientes'
const cotizaciones = ref([])
const expedientes = ref([])

// Filtros y Búsqueda
const filterEstado = ref('')
const searchQuery = ref('')
const filterCanalExpediente = ref('')
const filterEstadoExpediente = ref('')
const searchExpedienteQuery = ref('')

// Vista Completa de Auditoría
const showAuditoriaCompleta = ref(false)
const inspectedCotizacion = ref(null)

// Modales Operativos Auxiliares
const showAperturaModal = ref(false)
const selectedCotizacionParaExpediente = ref(null)

const showDetalleModal = ref(false)
const selectedExpedienteParaDetalle = ref(null)

const showCanalLevanteModal = ref(false)
const selectedExpedienteParaTramite = ref(null)
const canalLevanteActionType = ref('canal')

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

// Sincronización reactiva con el menú lateral (Sidebar)
const switchTab = (tabName) => {
  activeTab.value = tabName
  navigationStore.setGestionAduaneraSubOption(tabName)
}

watch(
  () => [navigationStore.gestionAduaneraSubOption, navigationStore.navTick],
  ([newSub]) => {
    if (newSub && ['cotizaciones', 'expedientes'].includes(newSub)) {
      activeTab.value = newSub
    }
  },
  { immediate: true }
)

// Métricas Computadas
const enviadasCount = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'ENVIADA' || c.estado === 'PENDIENTE').length
})

const revisionCount = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'REVISION' || c.estado === 'OBSERVADA').length
})

const aprobadasCount = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'APROBADA').length
})

const cotizacionesAprobadasSinExpediente = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'APROBADA' && c.tienePagoAprobado && !c.tieneExpediente)
})

// Métricas de Expedientes
const expedientesPendienteCanalCount = computed(() => {
  return expedientes.value.filter((e) => !e.canalSunat || e.canalSunat === 'PENDIENTE').length
})

const expedientesConCanalCount = computed(() => {
  return expedientes.value.filter((e) => e.canalSunat && e.canalSunat !== 'PENDIENTE').length
})

const expedientesConLevanteCount = computed(() => {
  return expedientes.value.filter((e) => !!e.fechaLevanteAutorizacion).length
})

// Filtrado de Cotizaciones
const filteredCotizaciones = computed(() => {
  return cotizaciones.value.filter((c) => {
    if (filterEstado.value) {
      if (filterEstado.value === 'ENVIADA' || filterEstado.value === 'PENDIENTE') {
        if (c.estado !== 'ENVIADA' && c.estado !== 'PENDIENTE') return false
      } else if (filterEstado.value === 'REVISION' || filterEstado.value === 'OBSERVADA') {
        if (c.estado !== 'REVISION' && c.estado !== 'OBSERVADA') return false
      } else if (c.estado !== filterEstado.value) {
        return false
      }
    }

    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase().trim()
      const match =
        c.codigoCotizacion?.toLowerCase().includes(q) ||
        c.clienteRazonSocial?.toLowerCase().includes(q) ||
        c.clienteNumeroDocumento?.toLowerCase().includes(q) ||
        c.usuarioComercialNombre?.toLowerCase().includes(q) ||
        c.codigoExpediente?.toLowerCase().includes(q)
      if (!match) return false
    }

    return true
  })
})

// Filtrado de Expedientes Aduaneros
const filteredExpedientes = computed(() => {
  return expedientes.value.filter((e) => {
    if (filterCanalExpediente.value) {
      if (filterCanalExpediente.value === 'PENDIENTE') {
        if (e.canalSunat && e.canalSunat !== 'PENDIENTE') return false
      } else if (e.canalSunat !== filterCanalExpediente.value) {
        return false
      }
    }

    if (filterEstadoExpediente.value) {
      if (e.estadoExpediente !== filterEstadoExpediente.value) return false
    }

    if (searchExpedienteQuery.value) {
      const q = searchExpedienteQuery.value.toLowerCase().trim()
      const match =
        e.codigoExpediente?.toLowerCase().includes(q) ||
        e.codigoCotizacion?.toLowerCase().includes(q) ||
        e.clienteRazonSocial?.toLowerCase().includes(q) ||
        e.clienteNumeroDocumento?.toLowerCase().includes(q) ||
        e.numeroDam?.toLowerCase().includes(q) ||
        e.numeroContenedor?.toLowerCase().includes(q) ||
        e.agenteAduaneroNombre?.toLowerCase().includes(q)
      if (!match) return false
    }

    return true
  })
})

const openAperturaModal = (cotizacion = null) => {
  selectedCotizacionParaExpediente.value = cotizacion
  showAperturaModal.value = true
}

const onExpedienteAperturado = (expedienteCreado) => {
  showAperturaModal.value = false
  showSuccess(`¡Expediente ${expedienteCreado.codigoExpediente} aperturado exitosamente!`)
  loadData()
}

const openDetalleExpediente = (exp) => {
  selectedExpedienteParaDetalle.value = exp
  showDetalleModal.value = true
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

// Cargar Datos
const loadData = async () => {
  loading.value = true
  try {
    const [cots, exps] = await Promise.all([
      gestionAduaneraService.getCotizaciones(),
      expedienteService.getExpedientes()
    ])
    cotizaciones.value = cots || []
    expedientes.value = exps || []

    // Si había una cotización inspeccionada en vista completa, refrescar su dato
    if (inspectedCotizacion.value) {
      const updated = cotizaciones.value.find((c) => c.idCotizacion === inspectedCotizacion.value.idCotizacion)
      if (updated) inspectedCotizacion.value = updated
    }

    // Si había un expediente en detalle, refrescar su dato
    if (selectedExpedienteParaDetalle.value) {
      const updatedExp = expedientes.value.find((e) => e.idExpediente === selectedExpedienteParaDetalle.value.idExpediente)
      if (updatedExp) selectedExpedienteParaDetalle.value = updatedExp
    }
  } catch (err) {
    showError(err.response?.data?.message || 'Error al sincronizar datos de Supervisión.')
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
  showAuditoriaCompleta.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
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
    showAuditoriaCompleta.value = false
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
    showAuditoriaCompleta.value = false
    await loadData()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al rechazar cotización.')
  } finally {
    processingAction.value = false
  }
}

const handleAperturaFromAuditoria = (cotizacion) => {
  showAuditoriaCompleta.value = false
  openAperturaModal(cotizacion)
}

// Acciones rápidas desde la tabla
const confirmApprove = async (cotizacion) => {
  const confirmed = await confirmAction(
    `¿Confirma la aprobación de la Cotización ${cotizacion.codigoCotizacion}? Al aprobar, la propuesta queda formalizada técnicamente para apertura de Expediente Aduanero.`,
    'Aprobación Técnica Aduanera'
  )
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

/* 3. Selector de Pestañas Numeradas */
.tabs-nav-bar {
  display: flex;
  gap: var(--space-2, 0.5rem);
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.15rem;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.75rem 1.25rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-text-muted);
  font-family: var(--font-sans);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.tab-btn:hover {
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  border-radius: var(--radius-sm, 6px) var(--radius-sm, 6px) 0 0;
}

.tab-btn.active {
  color: var(--color-text-main);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}

.tab-idx {
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-subtle);
}

.tab-btn.active .tab-idx {
  color: var(--color-primary);
}

/* 4. Contenido de Pestaña & Secciones */
.tab-content-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
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
