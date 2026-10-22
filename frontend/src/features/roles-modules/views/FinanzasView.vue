<template>
  <div class="finanzas-view">
    <!-- Vista Completa: Registro de Pago / Anticipo de Cotización (Warm Editorial 2.0) -->
    <RegistroPagoFullView
      v-if="showRegistroPagoFullView"
      :cotizacion="selectedCotizacionForPago"
      :cotizaciones="cotizacionesAprobadas"
      @cancel="closeRegistroPagoFullView"
      @saved="handlePagoRegistradoDesdeFullView"
    />

    <!-- Vista Principal: Tablero Financiero, Dashboard y Listados -->
    <div v-else class="finanzas-main-layout">
      <!-- 1. Header Toolbar Editorial -->
      <header class="finanzas-action-toolbar">
      <div class="toolbar-identity">
        <span class="badge-kicker">FINANZAS // GESTIÓN DE LIQUIDACIONES Y COBRANZAS</span>
        <div class="title-with-badge">
          <h2 class="toolbar-title">{{ getToolbarTitle() }}</h2>
          <span class="count-badge font-mono">{{ totalOperacionesCount }} Operaciones</span>
        </div>
        <p class="toolbar-description">
          Consolidación ejecutiva y operativa: anticipos de cotización pre-expediente, balances de tributos SUNAT, gastos portuarios y cobranzas SPOT.
        </p>
      </div>

      <div class="toolbar-actions">
        <button
          type="button"
          class="btn-warm-outline"
          :disabled="loading"
          @click="loadAllData"
          title="Actualizar datos financieros"
        >
          <i class="pi pi-refresh" :class="{ 'pi-spin': loading }"></i>
          <span>Actualizar</span>
        </button>

        <button
          type="button"
          class="btn-warm-outline"
          @click="openRegistrarPagoModal(null)"
          title="Registrar nuevo abono para cotización"
        >
          <i class="pi pi-wallet"></i>
          <span>+ Registrar Anticipo</span>
        </button>

        <button
          type="button"
          class="btn-espresso"
          @click="openEmisionModal()"
        >
          <i class="pi pi-receipt"></i>
          <span>Nueva Liquidación</span>
        </button>
      </div>
    </header>

    <!-- 2. Pestañas Principales del Módulo (Sincronizadas con Sidebar) -->
    <div class="module-tabs-nav">
      <!-- Sub-opción 01: Dashboard Financiero -->
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'dashboard' }"
        @click="switchTab('dashboard')"
      >
        <i class="pi pi-chart-pie"></i>
        <span>Dashboard Financiero</span>
      </button>

      <!-- Sub-opción 02: Pagos Pre-Expedientes -->
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'pre-expedientes' || activeTab === 'anticipos' }"
        @click="switchTab('pre-expedientes')"
      >
        <i class="pi pi-shield"></i>
        <span>Pagos Pre-Expedientes</span>
        <span class="tab-counter" :class="{ 'counter-alert': anticiposPendientesCount > 0 }">
          {{ cotizacionesAprobadas.length }}
        </span>
      </button>

      <!-- Sub-opción 03: Liquidación de Cuentas -->
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'liquidaciones' || activeTab === 'por-liquidar' }"
        @click="switchTab('liquidaciones')"
      >
        <i class="pi pi-receipt"></i>
        <span>Liquidación de Cuentas</span>
        <span class="tab-counter">{{ liquidacionesList.length }}</span>
      </button>

      <!-- Sub-opción 04: Facturación & Conciliación -->
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'conciliacion' }"
        @click="switchTab('conciliacion')"
      >
        <i class="pi pi-wallet"></i>
        <span>Conciliación & Arqueo</span>
        <span class="tab-counter">{{ pagosRealizadosList.length }}</span>
      </button>
    </div>

    <!-- ===================================================================== -->
    <!-- SUB-OPCIÓN 01: DASHBOARD FINANCIERO EJECUTIVO                          -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'dashboard'" class="tab-container fade-in">
      <!-- Barra de Estado Operativo & Atajos -->
      <div class="dashboard-status-bar mb-4">
        <div class="status-indicator-wrap">
          <span class="pulse-dot"></span>
          <span class="status-label font-mono">FINANZAS & TESORERÍA ONLINE</span>
          <span class="separator">·</span>
          <span class="status-detail text-muted">Tipo de Cambio Ref: <strong class="font-mono">S/ {{ metricas.tipoCambioOficial || '3.75' }}</strong></span>
        </div>
        <div class="quick-shortcuts">
          <button type="button" class="btn-quick-tag" @click="switchTab('pre-expedientes')">
            <i class="pi pi-clock text-warning"></i>
            <span>{{ cotizacionesConSaldoPendienteCount }} Cotizaciones con Saldo</span>
          </button>
          <button type="button" class="btn-quick-tag" @click="switchTab('pre-expedientes', 'vouchers')">
            <i class="pi pi-shield text-info"></i>
            <span>{{ anticiposPendientesCount }} Abonos por Validar</span>
          </button>
          <button type="button" class="btn-quick-tag" @click="switchTab('liquidaciones')">
            <i class="pi pi-receipt text-primary"></i>
            <span>{{ expedientesPorLiquidar.length }} Por Liquidar</span>
          </button>
        </div>
      </div>

      <!-- Cuadrícula de Métricas KPI Ejecutivas en Warm Editorial 2.0 -->
      <div class="metrics-kpi-grid mb-4">
        <!-- KPI 01: Recaudación Conforme -->
        <div class="kpi-card highlight-card" @click="switchTab('conciliacion')">
          <div class="kpi-header">
            <span class="kpi-index">01</span>
            <span class="kpi-badge badge-success">Recaudación Conforme</span>
          </div>
          <div class="kpi-body">
            <span class="kpi-value font-mono">{{ formatCurrencyPen(metricas.totalCobradoPen) }}</span>
            <span class="kpi-label">Total Cobrado Conforme (S/)</span>
          </div>
          <div class="kpi-footer-note">
            <span>Equivalente aprox: <strong class="font-mono text-muted">$ {{ (metricas.totalCobradoPen / (metricas.tipoCambioOficial || 3.75)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }} USD</strong></span>
          </div>
        </div>

        <!-- KPI 02: Cuentas por Cobrar Pre-Expedientes -->
        <div class="kpi-card" @click="switchTab('pre-expedientes')">
          <div class="kpi-header">
            <span class="kpi-index">02</span>
            <span class="kpi-badge badge-warning">Pre-Expedientes</span>
          </div>
          <div class="kpi-body">
            <span class="kpi-value font-mono text-pending">$ {{ totalSaldoPendienteCotizacionesUsd.toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
            <span class="kpi-label">Saldos por Cobrar Cotizaciones ($)</span>
          </div>
          <div class="kpi-footer-note">
            <span><strong class="font-mono text-warning">{{ cotizacionesConSaldoPendienteCount }}</strong> cotizaciones en gestión de anticipo</span>
          </div>
        </div>

        <!-- KPI 03: Liquidaciones por Cobrar -->
        <div class="kpi-card" @click="switchTab('liquidaciones')">
          <div class="kpi-header">
            <span class="kpi-index">03</span>
            <span class="kpi-badge badge-info">Liquidaciones</span>
          </div>
          <div class="kpi-body">
            <span class="kpi-value font-mono">{{ formatCurrencyPen(metricas.cuentasPorCobrarPen) }}</span>
            <span class="kpi-label">Cuentas por Cobrar Aduanas (S/)</span>
          </div>
          <div class="kpi-footer-note">
            <span><strong class="font-mono">{{ metricas.liquidacionesPendientes || 0 }}</strong> liquidaciones pendientes de pago</span>
          </div>
        </div>

        <!-- KPI 04: Fiscal SUNAT & SPOT 12% -->
        <div class="kpi-card">
          <div class="kpi-header">
            <span class="kpi-index">04</span>
            <span class="kpi-badge badge-neutral">Fiscal SUNAT</span>
          </div>
          <div class="kpi-body">
            <span class="kpi-value font-mono">{{ formatCurrencyPen(metricas.totalTributosSunatPen) }}</span>
            <span class="kpi-label">Tributos Pagados SUNAT</span>
          </div>
          <div class="kpi-footer-note">
            <span>Fondo SPOT (12%): <strong class="font-mono text-muted">{{ formatCurrencyPen(metricas.totalDetraccionesPen) }}</strong></span>
          </div>
        </div>
      </div>

      <!-- Grilla Analítica Dual de Cobranzas -->
      <div class="dashboard-analytics-grid mb-4">
        <!-- Tarjeta 1: Embudo Financiero Pre-Expediente -->
        <div class="analytics-card">
          <div class="analytics-header">
            <div>
              <span class="badge-kicker">CONTROL DE FONDOS PRE-OPERATIVOS</span>
              <h4 class="card-main-title">Embudo de Cobranza de Cotizaciones</h4>
            </div>
            <button type="button" class="btn-text-link" @click="switchTab('pre-expedientes')">
              <span>Gestionar Cotizaciones</span>
              <i class="pi pi-arrow-right"></i>
            </button>
          </div>

          <p class="analytics-desc">
            Seguimiento de anticipos mínimos requeridos antes de aperturar el expediente aduanero:
          </p>

          <!-- Barra de Progreso del Embudo -->
          <div class="funnel-progress-wrap">
            <div
              class="funnel-segment seg-sin-pago"
              :style="{ width: (cotizacionesAprobadas.length ? (cotizacionesSinAbonoCount / cotizacionesAprobadas.length) * 100 : 0) + '%' }"
              :title="`Sin Abono: ${cotizacionesSinAbonoCount}`"
            ></div>
            <div
              class="funnel-segment seg-revision"
              :style="{ width: (cotizacionesAprobadas.length ? (cotizacionesEnRevisionCount / cotizacionesAprobadas.length) * 100 : 0) + '%' }"
              :title="`En Revisión Finanzas: ${cotizacionesEnRevisionCount}`"
            ></div>
            <div
              class="funnel-segment seg-adelanto"
              :style="{ width: (cotizacionesAprobadas.length ? (cotizacionesAdelantoConformeCount / cotizacionesAprobadas.length) * 100 : 0) + '%' }"
              :title="`Adelanto Conforme: ${cotizacionesAdelantoConformeCount}`"
            ></div>
            <div
              class="funnel-segment seg-total"
              :style="{ width: (cotizacionesAprobadas.length ? (cotizacionesPagadasTotalCount / cotizacionesAprobadas.length) * 100 : 0) + '%' }"
              :title="`100% Pagado: ${cotizacionesPagadasTotalCount}`"
            ></div>
          </div>

          <!-- Leyenda de Estados Pre-Expediente -->
          <div class="funnel-legend-grid">
            <div class="legend-item">
              <span class="legend-dot dot-sin-pago"></span>
              <span class="legend-label">Sin Abono ({{ cotizacionesSinAbonoCount }})</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot dot-revision"></span>
              <span class="legend-label">En Revisión ({{ cotizacionesEnRevisionCount }})</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot dot-adelanto"></span>
              <span class="legend-label">Adelanto Conforme ({{ cotizacionesAdelantoConformeCount }})</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot dot-total"></span>
              <span class="legend-label">100% Pagado ({{ cotizacionesPagadasTotalCount }})</span>
            </div>
          </div>

          <!-- Resumen de Cifras del Embudo -->
          <div class="funnel-financial-summary">
            <div class="financial-pill">
              <span class="lbl">Total en Cartera:</span>
              <strong class="font-mono">$ {{ totalCotizadoAprobadasUsd.toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong>
            </div>
            <div class="financial-pill">
              <span class="lbl">Recaudado (Abonos):</span>
              <strong class="font-mono text-success">$ {{ totalAbonadoCotizacionesUsd.toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong>
            </div>
            <div class="financial-pill">
              <span class="lbl">Saldo por Cobrar:</span>
              <strong class="font-mono text-pending">$ {{ totalSaldoPendienteCotizacionesUsd.toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong>
            </div>
          </div>
        </div>

        <!-- Tarjeta 2: Balance Operativo de Liquidaciones -->
        <div class="analytics-card">
          <div class="analytics-header">
            <div>
              <span class="badge-kicker">FLUJO POST-DESPACHO ADUANERO</span>
              <h4 class="card-main-title">Control de Liquidaciones & Tributos</h4>
            </div>
            <button type="button" class="btn-text-link" @click="switchTab('liquidaciones')">
              <span>Bandeja de Liquidación</span>
              <i class="pi pi-arrow-right"></i>
            </button>
          </div>

          <p class="analytics-desc">
            Balance general de cuentas, fletes internos, almacén temporal y liquidaciones ante SUNAT:
          </p>

          <div class="operations-kpi-rows">
            <div class="op-stat-row">
              <div class="op-stat-left">
                <i class="pi pi-clock icon-pending"></i>
                <div>
                  <strong>Expedientes por Liquidar</strong>
                  <span class="subtext">Aprobados por aduana en espera de liquidación formal</span>
                </div>
              </div>
              <span class="font-mono font-bold text-base">{{ expedientesPorLiquidar.length }} exp.</span>
            </div>

            <div class="op-stat-row">
              <div class="op-stat-left">
                <i class="pi pi-exclamation-circle icon-warning"></i>
                <div>
                  <strong>Liquidaciones Pendientes de Cobro</strong>
                  <span class="subtext">Facturadas al cliente con saldo de cobranza</span>
                </div>
              </div>
              <span class="font-mono font-bold text-base text-warning">{{ metricas.liquidacionesPendientes || 0 }} liq.</span>
            </div>

            <div class="op-stat-row">
              <div class="op-stat-left">
                <i class="pi pi-check-circle icon-success"></i>
                <div>
                  <strong>Liquidaciones Conformes Cobradas</strong>
                  <span class="subtext">Ingresos auditados y verificados en cuenta bancaria</span>
                </div>
              </div>
              <span class="font-mono font-bold text-base text-success">{{ pagosRealizadosList.length }} liq.</span>
            </div>
          </div>

          <div class="card-action-bar-end">
            <button type="button" class="btn-espresso btn-sm" @click="openEmisionModal(null)">
              <i class="pi pi-receipt"></i>
              <span>+ Emitir Nueva Liquidación</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 3. Alertas Financieras Prioritarias (Cotizaciones con Saldo Pendiente) -->
      <div class="tab-content-section mb-4">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">ATENCIÓN INMEDIATA // CARTERA PRE-OPERATIVA</span>
            <h3 class="section-title">Cotizaciones Aprobadas en Espera de Cobro o Validación</h3>
          </div>
          <button type="button" class="btn-outline-warm btn-sm" @click="switchTab('pre-expedientes')">
            <span>Ver todas las cotizaciones aprobadas ({{ cotizacionesAprobadas.length }})</span>
            <i class="pi pi-arrow-right"></i>
          </button>
        </div>

        <div class="table-container">
          <DataTable
            :value="cotizacionesPendientesCobroPreview"
            responsiveLayout="scroll"
            class="custom-datatable"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-check-circle empty-icon text-success"></i>
                <p class="empty-title">Cartera al día</p>
                <p class="empty-desc">No hay cotizaciones aprobadas con saldos pendientes de pago en este momento.</p>
              </div>
            </template>

            <Column header="Cotización">
              <template #body="{ data: c }">
                <div class="client-cell">
                  <span class="font-mono font-bold text-xs text-primary">{{ c.codigoCotizacion }}</span>
                  <span class="client-sub">{{ formatDate(c.fechaEmision) }}</span>
                </div>
              </template>
            </Column>

            <Column header="Cliente Importador">
              <template #body="{ data: c }">
                <div class="client-cell">
                  <span class="client-name">{{ c.clienteRazonSocial }}</span>
                  <span class="client-sub">Doc: {{ c.clienteNumeroDocumento }}</span>
                </div>
              </template>
            </Column>

            <Column header="Total Cotizado">
              <template #body="{ data: c }">
                <span class="font-mono font-bold text-xs">
                  $ {{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                </span>
              </template>
            </Column>

            <Column header="Saldo Pendiente">
              <template #body="{ data: c }">
                <span class="font-mono font-bold text-xs text-pending">
                  $ {{ Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                </span>
              </template>
            </Column>

            <Column header="Estado Finanzas">
              <template #body="{ data: c }">
                <Tag
                  :value="formatEstadoPagoCotizacionLabel(c.estadoPago)"
                  :severity="formatEstadoPagoCotizacionSeverity(c.estadoPago)"
                  rounded
                />
              </template>
            </Column>

            <Column header="Acción Rápida" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: c }">
                <div class="actions-wrapper">
                  <button
                    type="button"
                    class="btn-action-primary-sm"
                    title="Registrar pago o adelanto directo"
                    @click="openRegistrarPagoForCotizacion(c)"
                  >
                    <i class="pi pi-wallet"></i>
                    <span>+ Agregar Abono</span>
                  </button>
                  <button
                    type="button"
                    class="btn-action-outline"
                    title="Ver detalle y vouchers de esta cotización"
                    @click="openHistorialCotizacion(c)"
                  >
                    <i class="pi pi-list"></i>
                    <span>Ver Abonos</span>
                  </button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- SUB-OPCIÓN 03: LIQUIDACIÓN ADUANERA & EXPEDIENTES POR LIQUIDAR       -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'liquidaciones' || activeTab === 'por-liquidar'" class="tab-container fade-in">
      <!-- Selector Interno entre Liquidaciones Emitidas y Expedientes por Liquidar -->
      <div class="inner-subtab-switch mb-3">
        <button
          type="button"
          class="subtab-pill"
          :class="{ active: subTabLiquidacion === 'emitidas' }"
          @click="subTabLiquidacion = 'emitidas'"
        >
          <i class="pi pi-receipt"></i>
          <span>Liquidaciones Emitidas</span>
          <span class="subtab-badge">{{ liquidacionesList.length }}</span>
        </button>
        <button
          type="button"
          class="subtab-pill"
          :class="{ active: subTabLiquidacion === 'por-liquidar' }"
          @click="subTabLiquidacion = 'por-liquidar'"
        >
          <i class="pi pi-clock"></i>
          <span>Expedientes por Liquidar</span>
          <span class="subtab-badge">{{ expedientesPorLiquidar.length }}</span>
        </button>
      </div>

      <!-- VISTA 1: LIQUIDACIONES EMITIDAS -->
      <div v-if="subTabLiquidacion === 'emitidas'" class="tab-content-section">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">CUENTAS Y REGISTROS</span>
            <h3 class="section-title">Bandeja de Liquidaciones Aduaneras</h3>
          </div>

          <div class="section-controls">
            <!-- Filtro de Estado -->
            <select v-model="filterEstado" class="filter-select">
              <option value="">Todos los Estados</option>
              <option value="PENDIENTE">Pendientes de Pago</option>
              <option value="PAGADO">Cobrados / Pagados</option>
              <option value="ANULADO">Anulados</option>
            </select>

            <!-- Búsqueda rápida -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por código, DAM o cliente..."
              />
            </div>

            <button type="button" class="btn-outline-warm" @click="loadAllData" :disabled="loading">
              <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
              <span>Actualizar</span>
            </button>

            <button type="button" class="btn-primary-action" @click="openEmisionModal(null)">
              <i class="pi pi-plus"></i>
              <span>+ Emitir Liquidación</span>
            </button>
          </div>
        </div>

        <!-- Tabla Principal de Liquidaciones -->
        <div class="table-container">
          <DataTable
            :value="filteredLiquidaciones"
            :loading="loading"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idLiquidacion"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-receipt empty-icon"></i>
                <p class="empty-title">No hay liquidaciones registradas</p>
                <p class="empty-desc">Emita una liquidación aduanera vinculada a un expediente aprobado.</p>
              </div>
            </template>

            <Column header="N° Liquidación">
              <template #body="{ data: l }">
                <div class="ticket-cell">
                  <i class="pi pi-file-check"></i>
                  <span class="font-mono font-bold">{{ l.codigoLiquidacion }}</span>
                </div>
              </template>
            </Column>

            <Column header="Expediente / DAM">
              <template #body="{ data: l }">
                <div class="client-cell">
                  <span class="font-mono font-bold text-xs">{{ l.codigoExpediente || 'EXP-SGA' }}</span>
                  <div class="sub-row">
                    <span class="text-xs text-muted">DAM: {{ l.numeroDam || 'S/N' }}</span>
                    <Tag
                      v-if="l.canalSunat"
                      :value="l.canalSunat"
                      :severity="getCanalSeverity(l.canalSunat)"
                      class="tag-xs"
                      rounded
                    />
                  </div>
                </div>
              </template>
            </Column>

            <Column header="Cliente Importador">
              <template #body="{ data: l }">
                <div class="client-cell">
                  <span class="client-name">{{ l.clienteRazonSocial || 'Cliente no asignado' }}</span>
                  <span class="client-sub">RUC: {{ l.clienteNumeroDocumento || '—' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Tributos SUNAT">
              <template #body="{ data: l }">
                <span class="font-mono text-xs font-semibold">
                  {{ formatCurrencyPen(l.totalTributosSunat) }}
                </span>
              </template>
            </Column>

            <Column header="Servicios + IGV">
              <template #body="{ data: l }">
                <div class="amount-sub-cell">
                  <span class="font-mono text-xs">{{ formatCurrencyPen((Number(l.subtotalImponible || 0) + Number(l.igvMonto || 0))) }}</span>
                  <span v-if="l.aplicaDetraccion" class="detraccion-note">
                    SPOT (12%): {{ formatCurrencyPen(l.montoDetraccion) }}
                  </span>
                </div>
              </template>
            </Column>

            <Column header="Total Neto">
              <template #body="{ data: l }">
                <div class="amount-main-cell">
                  <span class="font-mono font-bold text-sm text-primary">
                    {{ formatCurrencyPen(l.totalGeneralPen) }}
                  </span>
                  <span class="font-mono text-xs text-muted">
                    $ {{ Number(l.totalGeneralUsd || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }} USD
                  </span>
                </div>
              </template>
            </Column>

            <Column header="Estado">
              <template #body="{ data: l }">
                <Tag
                  :value="formatEstadoPago(l.estadoPago)"
                  :severity="getEstadoSeverity(l.estadoPago)"
                  rounded
                  class="tag-status"
                />
              </template>
            </Column>

            <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: l }">
                <div class="table-actions">
                  <!-- Botón Registrar Cobro -->
                  <button
                    v-if="l.estadoPago === 'PENDIENTE'"
                    type="button"
                    class="btn-action-primary"
                    title="Registrar Pago / Cobranza"
                    @click="openPagoModal(l)"
                  >
                    <i class="pi pi-check-circle"></i>
                    <span>Cobrar</span>
                  </button>

                  <!-- Botón Ver Voucher / Detalle Editorial -->
                  <button
                    type="button"
                    class="btn-action-outline"
                    title="Ver Pre-Factura y Liquidación Detallada"
                    @click="openDetalleModal(l)"
                  >
                    <i class="pi pi-eye"></i>
                  </button>

                  <!-- Botón Gestión Documental -->
                  <button
                    type="button"
                    class="btn-action-outline"
                    title="Ver/Adjuntar Factura y Vouchers"
                    @click="openDocumentos(l)"
                  >
                    <i class="pi pi-folder-open"></i>
                  </button>

                  <!-- Botón Anular (solo si está PENDIENTE) -->
                  <button
                    v-if="l.estadoPago === 'PENDIENTE'"
                    type="button"
                    class="btn-action-danger"
                    title="Anular Liquidación"
                    @click="confirmAnular(l)"
                  >
                    <i class="pi pi-trash"></i>
                  </button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>

      <!-- VISTA 2: EXPEDIENTES POR LIQUIDAR -->
      <div v-else-if="subTabLiquidacion === 'por-liquidar'" class="tab-content-section">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">PENDIENTES DE FACTURACIÓN</span>
            <h3 class="section-title">Expedientes Aduaneros sin Liquidar</h3>
          </div>

          <div class="section-controls">
            <button type="button" class="btn-outline-warm" @click="loadExpedientes" :disabled="loadingExpedientes">
              <i :class="['pi pi-sync', { 'pi-spin': loadingExpedientes }]"></i>
              <span>Actualizar</span>
            </button>
          </div>
        </div>

        <div class="table-container">
          <DataTable
            :value="expedientesPorLiquidar"
            :loading="loadingExpedientes"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idExpediente"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-check-circle empty-icon text-success"></i>
                <p class="empty-title">¡Al día! No hay expedientes pendientes de liquidación</p>
                <p class="empty-desc">Todos los expedientes en trámite cuentan con su liquidación financiera emitida.</p>
              </div>
            </template>

            <Column header="Expediente">
              <template #body="{ data: e }">
                <div class="ticket-cell">
                  <i class="pi pi-folder"></i>
                  <span class="font-mono font-bold">{{ e.codigoExpediente }}</span>
                </div>
              </template>
            </Column>

            <Column header="Cliente">
              <template #body="{ data: e }">
                <div class="client-cell">
                  <span class="client-name">{{ e.clienteRazonSocial || 'Cliente Importador' }}</span>
                  <span class="client-sub">RUC: {{ e.clienteNumeroDocumento || '—' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Canal SUNAT">
              <template #body="{ data: e }">
                <Tag
                  :value="e.canalSunat || 'PENDIENTE'"
                  :severity="getCanalSeverity(e.canalSunat)"
                  rounded
                  class="tag-xs"
                />
              </template>
            </Column>

            <Column header="DAM / Levante">
              <template #body="{ data: e }">
                <div class="client-cell">
                  <span class="font-mono text-xs font-semibold">{{ e.numeroDam || 'Sin DAM Asignada' }}</span>
                  <span class="client-sub">{{ e.fechaLevanteAutorizacion ? 'Levante: ' + formatDate(e.fechaLevanteAutorizacion) : 'En trámite' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Mercancía">
              <template #body="{ data: e }">
                <span class="text-xs">{{ e.mercanciaNombre || 'Carga General' }}</span>
              </template>
            </Column>

            <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: e }">
                <button
                  type="button"
                  class="btn-primary-action"
                  @click="openEmisionModal(e)"
                >
                  <i class="pi pi-calculator"></i>
                  <span>+ Liquidar Expediente</span>
                </button>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 3: CONCILIACIÓN BANCARIA Y ARQUEO                                  -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'conciliacion'" class="tab-container fade-in">
      <div class="tab-content-section">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">CONCILIACIÓN Y TESORERÍA</span>
            <h3 class="section-title">Registro de Cobranzas y Operaciones Bancarias</h3>
          </div>

          <div class="section-controls">
            <button type="button" class="btn-outline-warm" @click="loadLiquidaciones" :disabled="loading">
              <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
              <span>Actualizar</span>
            </button>
          </div>
        </div>

        <div class="table-container">
          <DataTable
            :value="pagosRealizadosList"
            :loading="loading"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idLiquidacion"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-wallet empty-icon"></i>
                <p class="empty-title">No hay pagos registrados aún</p>
                <p class="empty-desc">Los cobros registrados desde las liquidaciones se consolidarán en esta bitácora.</p>
              </div>
            </template>

            <Column header="Fecha Pago">
              <template #body="{ data: p }">
                <span class="font-mono text-xs">{{ formatDateTime(p.fechaPago) }}</span>
              </template>
            </Column>

            <Column header="Liquidación / Factura">
              <template #body="{ data: p }">
                <div class="client-cell">
                  <span class="font-mono font-bold text-xs">{{ p.codigoLiquidacion }}</span>
                  <span class="client-sub">Factura: {{ p.numeroFactura || 'S/N' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Cliente">
              <template #body="{ data: p }">
                <span class="client-name">{{ p.clienteRazonSocial }}</span>
              </template>
            </Column>

            <Column header="Banco & Operación">
              <template #body="{ data: p }">
                <div class="client-cell">
                  <span class="font-bold text-xs text-primary">{{ p.bancoDestino || p.metodoPago || 'TRANSFERENCIA' }}</span>
                  <span class="font-mono text-xs text-muted">Op: {{ p.numeroOperacion || '—' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Monto Cobrado">
              <template #body="{ data: p }">
                <span class="font-mono font-bold text-sm text-success">
                  {{ formatCurrencyPen(p.totalGeneralPen) }}
                </span>
              </template>
            </Column>

            <Column header="Voucher / Docs" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: p }">
                <button
                  type="button"
                  class="btn-action-outline"
                  title="Ver Vouchers y Comprobantes"
                  @click="openDocumentos(p)"
                >
                  <i class="pi pi-file-pdf"></i>
                  <span>Ver Voucher</span>
                </button>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- SUB-OPCIÓN 02: GESTIÓN DE PAGOS DE PRE-EXPEDIENTES (COTIZACIONES)     -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'pre-expedientes' || activeTab === 'anticipos'" class="tab-container fade-in">
      <div class="tab-content-section">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">CONTROL DE INGRESOS PRE-OPERATIVOS // COTIZACIONES APROBADAS</span>
            <h3 class="section-title">Gestión de Pagos y Anticipos Pre-Expediente</h3>
            <p class="section-subtitle">
              Cobranza y verificación contable requerida para habilitar la apertura del Expediente Aduanero.
            </p>
          </div>

          <div class="section-controls">
            <!-- Selector de Vista: Cotizaciones (Principal) vs Arqueo de Vouchers -->
            <div class="view-mode-toggle">
              <button
                type="button"
                class="view-toggle-btn"
                :class="{ active: vistaPreExpedientes === 'cotizaciones' }"
                @click="vistaPreExpedientes = 'cotizaciones'"
                title="Bandeja de cotizaciones aprobadas con saldos de pago"
              >
                <i class="pi pi-table"></i>
                <span>Cotizaciones por Cobrar ({{ filteredCotizacionesAprobadas.length }})</span>
              </button>
              <button
                type="button"
                class="view-toggle-btn"
                :class="{ active: vistaPreExpedientes === 'vouchers' }"
                @click="vistaPreExpedientes = 'vouchers'"
                title="Bandeja global de comprobantes bancarios y arqueo"
              >
                <i class="pi pi-shield"></i>
                <span>Arqueo de Abonos ({{ filteredAnticipos.length }})</span>
                <span v-if="anticiposPendientesCount > 0" class="mini-alert-dot"></span>
              </button>
            </div>

            <!-- Botón Actualizar -->
            <button
              type="button"
              class="btn-outline-warm"
              @click="loadAllData"
              :disabled="loadingCotizaciones || loadingAnticipos"
            >
              <i :class="['pi pi-sync', { 'pi-spin': loadingCotizaciones || loadingAnticipos }]"></i>
              <span>Actualizar</span>
            </button>

            <!-- Botón Registrar Abono General -->
            <button
              type="button"
              class="btn-primary-action"
              @click="openRegistrarPagoModal(null)"
            >
              <i class="pi pi-plus"></i>
              <span>+ Registrar Abono</span>
            </button>
          </div>
        </div>

        <!-- 4 Mini KPIs de la Sección Pre-Expedientes -->
        <div class="sub-kpi-bar mb-4">
          <div class="sub-kpi-item">
            <span class="sub-kpi-lbl">Cotizaciones Aprobadas</span>
            <strong class="sub-kpi-val font-mono">{{ cotizacionesAprobadas.length }}</strong>
          </div>
          <div class="sub-kpi-item">
            <span class="sub-kpi-lbl">Saldo Total por Cobrar</span>
            <strong class="sub-kpi-val font-mono text-pending">$ {{ totalSaldoPendienteCotizacionesUsd.toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong>
          </div>
          <div class="sub-kpi-item">
            <span class="sub-kpi-lbl">Abonos en Revisión Finanzas</span>
            <strong class="sub-kpi-val font-mono" :class="anticiposPendientesCount > 0 ? 'text-warning font-bold' : ''">
              {{ anticiposPendientesCount }}
            </strong>
          </div>
          <div class="sub-kpi-item">
            <span class="sub-kpi-lbl">Listas para Apertura</span>
            <strong class="sub-kpi-val font-mono text-success">{{ cotizacionesHabilitadasCount }}</strong>
          </div>
        </div>

        <!-- ================================================================= -->
        <!-- VISTA 1: TABLA CENTRAL DE COTIZACIONES APROBADAS POR COBRAR       -->
        <!-- ================================================================= -->
        <template v-if="vistaPreExpedientes === 'cotizaciones'">
          <!-- Barra de Filtros para Cotizaciones -->
          <div class="table-filter-bar mb-3">
            <div class="filter-pills-wrap">
              <button
                type="button"
                class="filter-pill"
                :class="{ active: filterEstadoCotizacionPago === '' }"
                @click="filterEstadoCotizacionPago = ''"
              >
                Todas ({{ cotizacionesAprobadas.length }})
              </button>
              <button
                type="button"
                class="filter-pill"
                :class="{ active: filterEstadoCotizacionPago === 'PENDIENTES' }"
                @click="filterEstadoCotizacionPago = 'PENDIENTES'"
              >
                Con Saldo Pendiente ({{ cotizacionesConSaldoPendienteCount }})
              </button>
              <button
                type="button"
                class="filter-pill"
                :class="{ active: filterEstadoCotizacionPago === 'EN_REVISION' }"
                @click="filterEstadoCotizacionPago = 'EN_REVISION'"
              >
                En Revisión Finanzas ({{ cotizacionesEnRevisionCount }})
              </button>
              <button
                type="button"
                class="filter-pill"
                :class="{ active: filterEstadoCotizacionPago === 'HABILITADAS' }"
                @click="filterEstadoCotizacionPago = 'HABILITADAS'"
              >
                Adelanto Conforme ({{ cotizacionesAdelantoConformeCount }})
              </button>
              <button
                type="button"
                class="filter-pill"
                :class="{ active: filterEstadoCotizacionPago === 'PAGADAS' }"
                @click="filterEstadoCotizacionPago = 'PAGADAS'"
              >
                100% Canceladas ({{ cotizacionesPagadasTotalCount }})
              </button>
            </div>

            <!-- Búsqueda Rápida -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQueryCotizacion"
                type="text"
                placeholder="Buscar por código, RUC o cliente..."
                class="search-input"
              />
            </div>
          </div>

          <!-- Tabla de Cotizaciones Aprobadas -->
          <div class="table-container">
            <DataTable
              :value="filteredCotizacionesAprobadas"
              :loading="loadingCotizaciones"
              responsiveLayout="scroll"
              class="custom-datatable"
              dataKey="idCotizacion"
            >
              <template #empty>
                <div class="empty-state">
                  <i class="pi pi-folder-open empty-icon"></i>
                  <p class="empty-title">No se encontraron cotizaciones con este criterio</p>
                  <p class="empty-desc">Las cotizaciones comerciales con aprobación técnica aparecerán aquí para el control y cobranza de anticipos.</p>
                </div>
              </template>

              <!-- Código Cotización -->
              <Column header="Cotización">
                <template #body="{ data: c }">
                  <div class="client-cell">
                    <span class="font-mono font-bold text-xs text-primary">{{ c.codigoCotizacion }}</span>
                    <span class="client-sub">{{ formatDate(c.fechaEmision) }}</span>
                  </div>
                </template>
              </Column>

              <!-- Cliente -->
              <Column header="Cliente Importador">
                <template #body="{ data: c }">
                  <div class="client-cell">
                    <span class="client-name">{{ c.clienteRazonSocial || 'Cliente no asignado' }}</span>
                    <span class="client-sub">RUC/Doc: {{ c.clienteNumeroDocumento }}</span>
                  </div>
                </template>
              </Column>

              <!-- Régimen / Mercancía -->
              <Column header="Régimen & Carga">
                <template #body="{ data: c }">
                  <div class="client-cell">
                    <span class="text-xs font-semibold">{{ c.regimenAduanero || 'IMPORTACIÓN' }}</span>
                    <span class="client-sub">{{ c.mercanciaNombre || c.tipoCargaNombre || 'Carga General' }}</span>
                  </div>
                </template>
              </Column>

              <!-- Total Cotizado -->
              <Column header="Total Cotizado">
                <template #body="{ data: c }">
                  <span class="font-mono font-bold text-xs">
                    $ {{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                  </span>
                </template>
              </Column>

              <!-- Total Abonado & Progreso -->
              <Column header="Total Abonado">
                <template #body="{ data: c }">
                  <div class="progress-col">
                    <span class="font-mono font-bold text-xs text-success">
                      $ {{ Number(c.totalAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                    </span>
                    <div class="mini-progress-bar">
                      <div
                        class="mini-progress-fill"
                        :style="{ width: Math.min(100, Math.round(((Number(c.totalAbonado || 0)) / (Number(c.totalCotizado || 1))) * 100)) + '%' }"
                      ></div>
                    </div>
                    <span class="client-sub">
                      {{ Math.min(100, Math.round(((Number(c.totalAbonado || 0)) / (Number(c.totalCotizado || 1))) * 100)) }}% pagado
                    </span>
                  </div>
                </template>
              </Column>

              <!-- Saldo Pendiente -->
              <Column header="Saldo Pendiente">
                <template #body="{ data: c }">
                  <div class="client-cell">
                    <span
                      class="font-mono font-bold text-xs"
                      :class="Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0) > 0 ? 'text-pending' : 'text-success'"
                    >
                      $ {{ Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                    </span>
                    <span class="client-sub">
                      ≈ S/ {{ (Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0) * (metricas.tipoCambioOficial || 3.75)).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
                    </span>
                  </div>
                </template>
              </Column>

              <!-- Estado Finanzas -->
              <Column header="Estado Finanzas">
                <template #body="{ data: c }">
                  <Tag
                    :value="formatEstadoPagoCotizacionLabel(c.estadoPago)"
                    :severity="formatEstadoPagoCotizacionSeverity(c.estadoPago)"
                    rounded
                  />
                </template>
              </Column>

              <!-- Control de Apertura / Expediente -->
              <Column header="Operación / Expediente">
                <template #body="{ data: c }">
                  <div class="client-cell">
                    <template v-if="c.tieneExpediente">
                      <Tag :value="'Expediente: ' + c.codigoExpediente" severity="info" rounded />
                    </template>
                    <template v-else-if="c.habilitadoParaExpediente">
                      <Tag value="Listo para Apertura" severity="success" rounded />
                    </template>
                    <template v-else>
                      <Tag value="Requiere Anticipo" severity="warn" rounded />
                    </template>
                  </div>
                </template>
              </Column>

              <!-- Acciones de Cobranza -->
              <Column header="Acciones de Cobranza" headerClass="text-right" bodyClass="text-right">
                <template #body="{ data: c }">
                  <div class="actions-wrapper">
                    <!-- Botón Destacado: + Agregar Abono (Cotización Precargada) -->
                    <button
                      type="button"
                      class="btn-action-primary-sm"
                      title="Registrar pago o abono directo para esta cotización"
                      @click="openRegistrarPagoForCotizacion(c)"
                    >
                      <i class="pi pi-wallet"></i>
                      <span>+ Agregar Abono</span>
                    </button>

                    <!-- Botón: Ver Historial de Abonos / Vouchers -->
                    <button
                      type="button"
                      class="btn-action-outline"
                      title="Ver pagos registrados, vouchers y validaciones"
                      @click="openHistorialCotizacion(c)"
                    >
                      <i class="pi pi-list"></i>
                      <span>Ver Abonos</span>
                    </button>
                  </div>
                </template>
              </Column>
            </DataTable>
          </div>
        </template>

        <!-- ================================================================= -->
        <!-- VISTA 2: BANDEJA GLOBAL DE ABONOS BANCARIOS (ARQUEO / AUDITORÍA)  -->
        <!-- ================================================================= -->
        <template v-else>
          <div class="table-filter-bar mb-3">
            <!-- Filtro de Estado -->
            <select v-model="filterEstadoAnticipo" class="filter-select">
              <option value="">Todos los Estados de Abono</option>
              <option value="PENDIENTE_REVISION">Pendientes de Aprobación</option>
              <option value="APROBADO">Aprobados / Conformes</option>
              <option value="OBSERVADO">Observados</option>
              <option value="RECHAZADO">Rechazados</option>
            </select>

            <!-- Búsqueda rápida -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQueryAnticipo"
                type="text"
                placeholder="Buscar por cotización, cliente u operación..."
                class="search-input"
              />
            </div>
          </div>

          <!-- Tabla de Anticipos / Vouchers -->
          <div class="table-container">
            <DataTable
              :value="filteredAnticipos"
              :loading="loadingAnticipos"
              responsiveLayout="scroll"
              class="custom-datatable"
              dataKey="idPago"
            >
              <template #empty>
                <div class="empty-state">
                  <i class="pi pi-shield empty-icon"></i>
                  <p class="empty-title">No hay anticipos ni pagos registrados con este criterio</p>
                  <p class="empty-desc">Los abonos reportados en las cotizaciones comerciales aparecerán aquí para verificación y visto bueno de Finanzas.</p>
                </div>
              </template>

              <Column header="N° Pago">
                <template #body="{ data: a }">
                  <div class="client-cell">
                    <span class="font-mono font-bold text-xs">{{ a.codigoPago }}</span>
                    <span class="client-sub">{{ formatDateTime(a.fechaRegistro) }}</span>
                  </div>
                </template>
              </Column>

              <Column header="Cotización Origen">
                <template #body="{ data: a }">
                  <div class="client-cell">
                    <span class="font-mono font-bold text-xs text-primary">{{ a.codigoCotizacion }}</span>
                    <span class="client-sub">Total: ${{ Number(a.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
                  </div>
                </template>
              </Column>

              <Column header="Cliente Importador">
                <template #body="{ data: a }">
                  <div class="client-cell">
                    <span class="client-name">{{ a.clienteRazonSocial || 'Cliente no asignado' }}</span>
                    <span class="client-sub">Doc: {{ a.clienteNumeroDocumento }}</span>
                  </div>
                </template>
              </Column>

              <Column header="Monto Abonado">
                <template #body="{ data: a }">
                  <div class="client-cell">
                    <span class="font-mono font-bold text-sm text-success">
                      {{ a.moneda === 'PEN' ? 'S/' : '$' }} {{ Number(a.montoAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                    </span>
                    <span v-if="a.moneda === 'USD'" class="client-sub">
                      Equiv: S/ {{ Number(a.montoEquivalentePen || 0).toLocaleString('es-PE', { minimumFractionDigits: 2 }) }}
                    </span>
                  </div>
                </template>
              </Column>

              <Column header="Banco & N° Operación">
                <template #body="{ data: a }">
                  <div class="client-cell">
                    <span class="font-bold text-xs">{{ a.bancoDestino }}</span>
                    <span class="client-sub font-mono">Op: {{ a.numeroOperacion }} ({{ a.fechaOperacion }})</span>
                  </div>
                </template>
              </Column>

              <Column header="Modalidad">
                <template #body="{ data: a }">
                  <Tag
                    :value="formatTipoAbono(a.tipoAbono)"
                    severity="info"
                    rounded
                  />
                </template>
              </Column>

              <Column header="Estado Finanzas">
                <template #body="{ data: a }">
                  <Tag
                    :value="getEstadoAnticipoLabel(a.estadoPago)"
                    :severity="getEstadoAnticipoSeverity(a.estadoPago)"
                    rounded
                  />
                </template>
              </Column>

              <Column header="Acciones de Control" headerClass="text-right" bodyClass="text-right">
                <template #body="{ data: a }">
                  <div class="actions-wrapper">
                    <!-- Botón Ver Voucher -->
                    <button
                      v-if="a.idDocumentoComprobante"
                      type="button"
                      class="btn-action-outline"
                      title="Ver comprobante bancario"
                      @click="verVoucherAnticipo(a)"
                    >
                      <i class="pi pi-file-pdf"></i>
                      <span>Voucher</span>
                    </button>

                    <!-- Si está pendiente de revisión -->
                    <template v-if="a.estadoPago === 'PENDIENTE_REVISION'">
                      <button
                        type="button"
                        class="btn-action-approve"
                        title="Aprobar abono y habilitar cotización para Expediente Aduanero"
                        @click="openValidarAnticipo(a, 'APROBAR')"
                      >
                        <i class="pi pi-check"></i>
                        <span>Aprobar</span>
                      </button>
                      <button
                        type="button"
                        class="btn-action-observe"
                        title="Observar abono bancario"
                        @click="openValidarAnticipo(a, 'OBSERVAR')"
                      >
                        <i class="pi pi-eye"></i>
                        <span>Observar</span>
                      </button>
                    </template>
                    <span v-else class="text-xs text-muted">
                      {{ a.usuarioFinanzasNombre ? 'Validado por ' + a.usuarioFinanzasNombre : 'Conforme' }}
                    </span>
                  </div>
                </template>
              </Column>
            </DataTable>
          </div>
        </template>
      </div>
    </div>
    </div>

    <!-- ===================================================================== -->
    <!-- MODAL 1: EMISIÓN / CÁLCULO DE LIQUIDACIÓN ADUANERA                   -->
    <!-- ===================================================================== -->
    <Dialog
      v-model:visible="showEmisionModal"
      modal
      header="Emisión de Liquidación Aduanera & Balance de Gastos"
      :style="{ width: '850px', maxWidth: '95vw' }"
      class="custom-dialog-warm"
    >
      <div class="modal-body">
        <!-- Selector o Expediente de referencia -->
        <div class="form-group mb-4">
          <label class="form-label required">Expediente Aduanero Asociado</label>
          <select
            v-model="emisionForm.idExpediente"
            class="form-select"
            :disabled="loadingEstimado || isExpedienteFixed"
            @change="onExpedienteSelected"
          >
            <option :value="null">-- Seleccione un Expediente Aduanero --</option>
            <option
              v-for="exp in expedientesSelectableList"
              :key="exp.idExpediente"
              :value="exp.idExpediente"
            >
              {{ exp.codigoExpediente }} — {{ exp.clienteRazonSocial }} (DAM: {{ exp.numeroDam || 'S/N' }})
            </option>
          </select>
        </div>

        <!-- Banner de Datos del Expediente Seleccionado -->
        <div v-if="selectedExpedienteInfo" class="exp-summary-banner mb-4">
          <div class="exp-summary-item">
            <span class="label">Cliente Importador:</span>
            <span class="val">{{ selectedExpedienteInfo.clienteRazonSocial }} ({{ selectedExpedienteInfo.clienteNumeroDocumento }})</span>
          </div>
          <div class="exp-summary-item">
            <span class="label">Canal SUNAT / DAM:</span>
            <span class="val">{{ selectedExpedienteInfo.canalSunat || 'PENDIENTE' }} // DAM: {{ selectedExpedienteInfo.numeroDam || 'S/N' }}</span>
          </div>
        </div>

        <!-- Rubros Desglosados en Cuadrícula -->
        <div class="grid-2-cols mb-4">
          <div class="form-group">
            <label class="form-label required">Tributos Liquidados ante SUNAT (S/)</label>
            <div class="currency-input-wrap">
              <span class="currency-prefix">S/</span>
              <input
                v-model.number="emisionForm.totalTributosSunat"
                type="number"
                step="0.01"
                min="0"
                class="form-input"
                placeholder="0.00"
              />
            </div>
            <span class="field-hint">Ad-Valorem, IGV e IPM aduanero (no gravado internamente).</span>
          </div>

          <div class="form-group">
            <label class="form-label required">Gastos de Almacén & Terminal (S/)</label>
            <div class="currency-input-wrap">
              <span class="currency-prefix">S/</span>
              <input
                v-model.number="emisionForm.gastosAlmacenPuerto"
                type="number"
                step="0.01"
                min="0"
                class="form-input"
                placeholder="0.00"
              />
            </div>
            <span class="field-hint">Custodia, pesaje VGM y servicios de depósito temporal.</span>
          </div>

          <div class="form-group">
            <label class="form-label required">Flete de Transporte Local (S/)</label>
            <div class="currency-input-wrap">
              <span class="currency-prefix">S/</span>
              <input
                v-model.number="emisionForm.fleteTransporteLocal"
                type="number"
                step="0.01"
                min="0"
                class="form-input"
                placeholder="0.00"
              />
            </div>
            <span class="field-hint">Traslado en tracto y plataforma desde puerto a cliente.</span>
          </div>

          <div class="form-group">
            <label class="form-label required">Honorarios de Agencia Aduanera (S/)</label>
            <div class="currency-input-wrap">
              <span class="currency-prefix">S/</span>
              <input
                v-model.number="emisionForm.honorariosAgencia"
                type="number"
                step="0.01"
                min="0"
                class="form-input"
                placeholder="0.00"
              />
            </div>
            <span class="field-hint">Comisión profesional por agenciamiento y despacho.</span>
          </div>
        </div>

        <!-- Parámetros Tributarios y SUNAT -->
        <div class="grid-3-cols mb-4">
          <div class="form-group">
            <label class="form-label">Tipo de Cambio (S/ por USD)</label>
            <input
              v-model.number="emisionForm.tipoCambio"
              type="number"
              step="0.0001"
              min="1"
              class="form-input font-mono"
            />
          </div>

          <div class="form-group">
            <label class="form-label">N° Factura / Boleta Comercial</label>
            <input
              v-model="emisionForm.numeroFactura"
              type="text"
              class="form-input font-mono"
              placeholder="F001-0000421"
            />
          </div>

          <div class="form-group checkbox-group">
            <label class="checkbox-label">
              <input v-model="emisionForm.aplicaDetraccion" type="checkbox" />
              <span>Aplica Detracción SPOT SUNAT (12%)</span>
            </label>
            <span class="field-hint">Depósito obligatorio a cuenta de Banco de la Nación.</span>
          </div>
        </div>

        <!-- Resumen de Totales y Cálculos en Tiempo Real -->
        <div class="totals-breakdown-card mb-4">
          <div class="total-row">
            <span>Subtotal Servicios Gravados:</span>
            <span class="font-mono">{{ formatCurrencyPen(calculatedSubtotal) }}</span>
          </div>
          <div class="total-row">
            <span>IGV de Servicios (18%):</span>
            <span class="font-mono">{{ formatCurrencyPen(calculatedIgv) }}</span>
          </div>
          <div class="total-row">
            <span>Tributos SUNAT (Tasa Aduanera):</span>
            <span class="font-mono">{{ formatCurrencyPen(emisionForm.totalTributosSunat || 0) }}</span>
          </div>
          <div v-if="emisionForm.aplicaDetraccion" class="total-row text-accent">
            <span>Detracción SPOT a Banco de la Nación (12%):</span>
            <span class="font-mono font-bold">- {{ formatCurrencyPen(calculatedDetraccion) }}</span>
          </div>
          <div class="total-row total-highlight">
            <span class="font-bold">TOTAL GENERAL A COBRAR (PEN):</span>
            <span class="font-mono font-bold text-lg text-primary">{{ formatCurrencyPen(calculatedTotalPen) }}</span>
          </div>
          <div class="total-row text-muted text-xs">
            <span>Equivalente Consolidado en Dólares (USD):</span>
            <span class="font-mono font-bold">$ {{ Number(calculatedTotalUsd).toLocaleString('en-US', { minimumFractionDigits: 2 }) }} USD</span>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Observaciones y Términos de Liquidación</label>
          <textarea
            v-model="emisionForm.observaciones"
            rows="2"
            class="form-textarea"
            placeholder="Detalles complementarios, plazos de crédito o notas al cliente..."
          ></textarea>
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn-outline-warm" @click="showEmisionModal = false">
          Cancelar
        </button>
        <button
          type="button"
          class="btn-primary-action"
          :disabled="submittingEmision || !emisionForm.idExpediente"
          @click="submitEmision"
        >
          <i :class="['pi pi-check', { 'pi-spin': submittingEmision }]"></i>
          <span>Emitir Liquidación</span>
        </button>
      </template>
    </Dialog>

    <!-- ===================================================================== -->
    <!-- MODAL 2: REGISTRO DE COBRO BANCARIO                                   -->
    <!-- ===================================================================== -->
    <Dialog
      v-model:visible="showPagoModal"
      modal
      header="Registro de Cobranza & Operación Bancaria"
      :style="{ width: '600px', maxWidth: '95vw' }"
      class="custom-dialog-warm"
    >
      <div v-if="selectedLiquidacionPago" class="modal-body">
        <div class="pago-summary-banner mb-4">
          <div class="pago-banner-title font-mono font-bold">{{ selectedLiquidacionPago.codigoLiquidacion }}</div>
          <div class="pago-banner-client">{{ selectedLiquidacionPago.clienteRazonSocial }}</div>
          <div class="pago-banner-total">
            <span class="label">Monto Total a Cobrar:</span>
            <span class="val font-mono font-bold text-success">{{ formatCurrencyPen(selectedLiquidacionPago.totalGeneralPen) }}</span>
          </div>
          <div v-if="selectedLiquidacionPago.aplicaDetraccion" class="pago-banner-sub text-xs text-muted">
            Detracción SPOT (12%): {{ formatCurrencyPen(selectedLiquidacionPago.montoDetraccion) }} a Banco de la Nación
          </div>
        </div>

        <div class="form-group mb-3">
          <label class="form-label required">Medio de Pago</label>
          <select v-model="pagoForm.metodoPago" class="form-select">
            <option value="TRANSFERENCIA_BANCARIA">Transferencia Bancaria</option>
            <option value="DEPOSITO_EN_VENTANILLA">Depósito en Ventanilla</option>
            <option value="PAGO_ELECTRONICO">Pago Electrónico / Pasarela</option>
            <option value="CHEQUE">Cheque de Gerencia</option>
          </select>
        </div>

        <div class="grid-2-cols mb-3">
          <div class="form-group">
            <label class="form-label required">Banco de Destino</label>
            <select v-model="pagoForm.bancoDestino" class="form-select">
              <option value="BCP">Banco de Crédito (BCP)</option>
              <option value="BBVA">BBVA Perú</option>
              <option value="INTERBANK">Interbank</option>
              <option value="SCOTIABANK">Scotiabank</option>
              <option value="BANCO_NACION">Banco de la Nación (Detracciones)</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label required">N° de Operación Bancaria</label>
            <input
              v-model="pagoForm.numeroOperacion"
              type="text"
              class="form-input font-mono"
              placeholder="Ej. OP-90823412"
            />
          </div>
        </div>

        <div class="grid-2-cols mb-3">
          <div class="form-group">
            <label class="form-label">N° de Factura Emitida</label>
            <input
              v-model="pagoForm.numeroFactura"
              type="text"
              class="form-input font-mono"
              placeholder="F001-000421"
            />
          </div>

          <div class="form-group">
            <label class="form-label required">Fecha y Hora de Pago</label>
            <input
              v-model="pagoForm.fechaPago"
              type="datetime-local"
              class="form-input"
            />
          </div>
        </div>

        <div class="form-group mb-3">
          <label class="form-label">Observaciones de Cobranza</label>
          <textarea
            v-model="pagoForm.observaciones"
            rows="2"
            class="form-textarea"
            placeholder="Notas de abono, número de cuenta origen, etc..."
          ></textarea>
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn-outline-warm" @click="showPagoModal = false">
          Cancelar
        </button>
        <button
          type="button"
          class="btn-primary-action"
          :disabled="submittingPago || !pagoForm.numeroOperacion"
          @click="submitPago"
        >
          <i :class="['pi pi-check-circle', { 'pi-spin': submittingPago }]"></i>
          <span>Confirmar Cobro Conforme</span>
        </button>
      </template>
    </Dialog>

    <!-- ===================================================================== -->
    <!-- MODAL 3: DETALLE EDITORIAL / PRE-FACTURA DE LIQUIDACIÓN              -->
    <!-- ===================================================================== -->
    <Dialog
      v-model:visible="showDetalleModal"
      modal
      header="Liquidación Oficial de Despacho Aduanero"
      :style="{ width: '750px', maxWidth: '95vw' }"
      class="custom-dialog-warm"
    >
      <div v-if="selectedLiquidacionDetalle" class="invoice-container">
        <!-- Cabecera de la Liquidación -->
        <div class="invoice-header">
          <div>
            <div class="invoice-company">SISTEMA DE GESTIÓN ADUANERA (SGA)</div>
            <div class="invoice-sub text-xs text-muted">Agencia de Aduanas Autorizada SUNAT</div>
          </div>
          <div class="invoice-code-box">
            <div class="invoice-title">LIQUIDACIÓN ADUANERA</div>
            <div class="invoice-number font-mono">{{ selectedLiquidacionDetalle.codigoLiquidacion }}</div>
            <div class="invoice-date text-xs text-muted">Fecha: {{ selectedLiquidacionDetalle.fechaEmision }}</div>
          </div>
        </div>

        <!-- Ficha del Cliente y Expediente -->
        <div class="invoice-meta-grid">
          <div class="meta-item">
            <span class="meta-label">Cliente Importador:</span>
            <span class="meta-val">{{ selectedLiquidacionDetalle.clienteRazonSocial }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">RUC:</span>
            <span class="meta-val font-mono">{{ selectedLiquidacionDetalle.clienteNumeroDocumento }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Expediente:</span>
            <span class="meta-val font-mono">{{ selectedLiquidacionDetalle.codigoExpediente }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Declaración DAM:</span>
            <span class="meta-val font-mono">{{ selectedLiquidacionDetalle.numeroDam || 'S/N' }}</span>
          </div>
        </div>

        <!-- Tabla de Rubros -->
        <table class="invoice-table">
          <thead>
            <tr>
              <th>Rubro / Concepto Liquidado</th>
              <th class="text-right">Importe (S/)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <div class="font-bold">Tributos Liquidados ante SUNAT</div>
                <div class="text-xs text-muted">Ad-Valorem, IGV e IPM aduanero cancelados con DAM</div>
              </td>
              <td class="text-right font-mono">{{ formatCurrencyPen(selectedLiquidacionDetalle.totalTributosSunat) }}</td>
            </tr>
            <tr>
              <td>
                <div class="font-bold">Gastos Portuarios & Depósito Temporal</div>
                <div class="text-xs text-muted">Almacenaje, pesaje VGM y manipuleo de contenedor</div>
              </td>
              <td class="text-right font-mono">{{ formatCurrencyPen(selectedLiquidacionDetalle.gastosAlmacenPuerto) }}</td>
            </tr>
            <tr>
              <td>
                <div class="font-bold">Flete Terrestre de Distribución Local</div>
                <div class="text-xs text-muted">Traslado en tracto autorizado desde garita a planta</div>
              </td>
              <td class="text-right font-mono">{{ formatCurrencyPen(selectedLiquidacionDetalle.fleteTransporteLocal) }}</td>
            </tr>
            <tr>
              <td>
                <div class="font-bold">Honorarios Profesionales de Agenciamiento</div>
                <div class="text-xs text-muted">Gestión de trámite, despacho y liquidación aduanera</div>
              </td>
              <td class="text-right font-mono">{{ formatCurrencyPen(selectedLiquidacionDetalle.honorariosAgencia) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td>Subtotal de Servicios Gravados:</td>
              <td class="text-right font-mono">{{ formatCurrencyPen(selectedLiquidacionDetalle.subtotalImponible) }}</td>
            </tr>
            <tr>
              <td>Impuesto General a las Ventas (IGV 18%):</td>
              <td class="text-right font-mono">{{ formatCurrencyPen(selectedLiquidacionDetalle.igvMonto) }}</td>
            </tr>
            <tr v-if="selectedLiquidacionDetalle.aplicaDetraccion" class="row-detraccion">
              <td>Detracción SPOT a Banco de la Nación (12%):</td>
              <td class="text-right font-mono font-bold">- {{ formatCurrencyPen(selectedLiquidacionDetalle.montoDetraccion) }}</td>
            </tr>
            <tr class="row-total">
              <td class="font-bold">TOTAL GENERAL CONSOLIDADO (PEN):</td>
              <td class="text-right font-mono font-bold text-lg text-primary">{{ formatCurrencyPen(selectedLiquidacionDetalle.totalGeneralPen) }}</td>
            </tr>
            <tr class="row-usd text-xs text-muted">
              <td>Equivalente Dólares (Tipo de Cambio {{ selectedLiquidacionDetalle.tipoCambio }}):</td>
              <td class="text-right font-mono font-bold">$ {{ Number(selectedLiquidacionDetalle.totalGeneralUsd || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }} USD</td>
            </tr>
          </tfoot>
        </table>

        <!-- Información de Pago si ya fue cobrado -->
        <div v-if="selectedLiquidacionDetalle.estadoPago === 'PAGADO'" class="invoice-paid-banner mt-4">
          <div class="badge-paid">PAGADO / CONFORME</div>
          <div class="text-xs">
            Abonado mediante <strong>{{ selectedLiquidacionDetalle.bancoDestino || selectedLiquidacionDetalle.metodoPago }}</strong> el {{ formatDateTime(selectedLiquidacionDetalle.fechaPago) }} — Op: <strong>{{ selectedLiquidacionDetalle.numeroOperacion }}</strong>
          </div>
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn-outline-warm" @click="showDetalleModal = false">
          Cerrar
        </button>
        <button type="button" class="btn-primary-action" @click="imprimirLiquidacion">
          <i class="pi pi-print"></i>
          <span>Imprimir / Exportar</span>
        </button>
      </template>
    </Dialog>

    <!-- ===================================================================== -->
    <!-- MODAL 4: VALIDACIÓN FINANCIERA DE ANTICIPO / PAGO DE COTIZACIÓN       -->
    <!-- ===================================================================== -->
    <Dialog
      v-model:visible="showValidarAnticipoModal"
      modal
      :header="validarAccion === 'APROBAR' ? 'Aprobación de Pago / Anticipo' : 'Observación de Pago Bancario'"
      :style="{ width: '560px', maxWidth: '95vw' }"
      class="custom-dialog-warm"
    >
      <div v-if="selectedAnticipo" class="modal-body">
        <div class="anticipo-validation-summary mb-3">
          <div class="summary-line">
            <span class="label">Cotización:</span>
            <strong class="val">{{ selectedAnticipo.codigoCotizacion }} — {{ selectedAnticipo.clienteRazonSocial }}</strong>
          </div>
          <div class="summary-line">
            <span class="label">Operación Bancaria:</span>
            <span class="val font-mono">{{ selectedAnticipo.bancoDestino }} (N° {{ selectedAnticipo.numeroOperacion }})</span>
          </div>
          <div class="summary-line">
            <span class="label">Monto Abonado:</span>
            <strong class="val text-success">
              {{ selectedAnticipo.moneda === 'PEN' ? 'S/' : '$' }} {{ Number(selectedAnticipo.montoAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
              <span v-if="selectedAnticipo.moneda === 'USD'" class="text-xs text-muted">(Equiv: S/ {{ Number(selectedAnticipo.montoEquivalentePen || 0).toLocaleString('es-PE', { minimumFractionDigits: 2 }) }})</span>
            </strong>
          </div>
          <div class="summary-line">
            <span class="label">Modalidad:</span>
            <span class="val">{{ formatTipoAbono(selectedAnticipo.tipoAbono) }}</span>
          </div>
        </div>

        <div v-if="validarAccion === 'APROBAR'" class="alert-box-success mb-3">
          <i class="pi pi-check-circle mr-2"></i>
          <span>Al aprobar este pago, la cotización quedará <strong>habilitada financieramente</strong> y se desbloqueará la Apertura de Expediente Aduanero.</span>
        </div>
        <div v-else class="alert-box-warning mb-3">
          <i class="pi pi-exclamation-triangle mr-2"></i>
          <span>Al observar este abono, se notificará al área comercial para que rectifique el voucher o número de operación.</span>
        </div>

        <div class="form-group mb-3">
          <label class="form-label">Notas / Observaciones de Finanzas</label>
          <textarea
            v-model="observacionFinanzas"
            rows="3"
            class="form-textarea"
            :placeholder="validarAccion === 'APROBAR' ? 'Ej. Abono verificado conforme en extracto bancario BCP...' : 'Indique el motivo de discrepancia o por qué no se ubica la operación...'"
          ></textarea>
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn-outline-warm" @click="showValidarAnticipoModal = false">
          Cancelar
        </button>
        <button
          type="button"
          :class="validarAccion === 'APROBAR' ? 'btn-primary-action' : 'btn-danger-action'"
          :disabled="submittingValidacion"
          @click="submitValidarAnticipo"
        >
          <i :class="[validarAccion === 'APROBAR' ? 'pi pi-check' : 'pi pi-times', { 'pi-spin': submittingValidacion }]"></i>
          <span>{{ validarAccion === 'APROBAR' ? 'Confirmar Aprobación Bancaria' : 'Confirmar Observación' }}</span>
        </button>
      </template>
    </Dialog>

    <!-- ===================================================================== -->
    <!-- MODAL 5: GESTOR UNIVERSAL DE DOCUMENTOS ADJUNTOS                      -->
    <!-- ===================================================================== -->
    <DocumentosManagerModal
      :visible="showDocsModal"
      :entidad-tipo="'EXPEDIENTE'"
      :id-entidad="selectedDocEntidadId"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigo"
      :titulo-entidad="selectedDocTitulo"
      @close="showDocsModal = false"
    />

    <!-- ===================================================================== -->
    <!-- MODAL: HISTORIAL DE ABONOS Y VOUCHERS POR COTIZACIÓN                 -->
    <!-- ===================================================================== -->
    <Dialog
      v-model:visible="showHistorialCotizacionModal"
      modal
      :header="selectedCotizacionHistorial ? `Historial de Abonos // Cotización ${selectedCotizacionHistorial.codigoCotizacion}` : 'Historial de Abonos'"
      :style="{ width: '880px', maxWidth: '95vw' }"
      class="custom-dialog-warm"
    >
      <div v-if="selectedCotizacionHistorial" class="modal-body">
        <!-- Banner Resumen de la Cotización -->
        <div class="exp-summary-banner mb-4">
          <div class="exp-summary-item">
            <span class="label">Cliente Importador:</span>
            <span class="val font-semibold">{{ selectedCotizacionHistorial.clienteRazonSocial }} ({{ selectedCotizacionHistorial.clienteNumeroDocumento }})</span>
          </div>
          <div class="exp-summary-item">
            <span class="label">Total Cotizado:</span>
            <span class="val font-mono font-bold">$ {{ Number(selectedCotizacionHistorial.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="exp-summary-item">
            <span class="label">Total Abonado:</span>
            <span class="val font-mono font-bold text-success">$ {{ Number(selectedCotizacionHistorial.totalAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="exp-summary-item">
            <span class="label">Saldo Pendiente:</span>
            <span class="val font-mono font-bold text-pending">$ {{ Number(selectedCotizacionHistorial.saldoPendiente != null ? selectedCotizacionHistorial.saldoPendiente : selectedCotizacionHistorial.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
          </div>
        </div>

        <!-- Barra de Acciones del Modal -->
        <div class="modal-subheading-bar mb-3">
          <h4 class="subheading-title">Comprobantes y Depósitos Bancarios Registrados</h4>
          <button
            type="button"
            class="btn-primary-action btn-sm"
            @click="openRegistrarPagoForCotizacion(selectedCotizacionHistorial)"
          >
            <i class="pi pi-plus"></i>
            <span>+ Agregar Pago / Abono</span>
          </button>
        </div>

        <!-- Tabla de Pagos de la Cotización -->
        <div class="table-container">
          <DataTable
            :value="pagosCotizacionList"
            :loading="loadingPagosCotizacion"
            responsiveLayout="scroll"
            class="custom-datatable"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-wallet empty-icon"></i>
                <p class="empty-title">Sin pagos registrados</p>
                <p class="empty-desc">Aún no se han registrado abonos bancarios para esta cotización comercial.</p>
              </div>
            </template>

            <Column header="N° Pago">
              <template #body="{ data: p }">
                <div class="client-cell">
                  <span class="font-mono font-bold text-xs">{{ p.codigoPago }}</span>
                  <span class="client-sub">{{ formatDateTime(p.fechaRegistro) }}</span>
                </div>
              </template>
            </Column>

            <Column header="Monto Abonado">
              <template #body="{ data: p }">
                <div class="client-cell">
                  <span class="font-mono font-bold text-xs text-success">
                    {{ p.moneda === 'PEN' ? 'S/' : '$' }} {{ Number(p.montoAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                  </span>
                  <span v-if="p.moneda === 'USD'" class="client-sub font-mono">
                    ≈ S/ {{ Number(p.montoEquivalentePen || 0).toLocaleString('es-PE', { minimumFractionDigits: 2 }) }}
                  </span>
                </div>
              </template>
            </Column>

            <Column header="Banco & Operación">
              <template #body="{ data: p }">
                <div class="client-cell">
                  <span class="font-bold text-xs text-primary">{{ p.bancoDestino }}</span>
                  <span class="client-sub font-mono">Op: {{ p.numeroOperacion }} ({{ p.fechaOperacion }})</span>
                </div>
              </template>
            </Column>

            <Column header="Modalidad">
              <template #body="{ data: p }">
                <Tag :value="formatTipoAbono(p.tipoAbono)" severity="info" rounded />
              </template>
            </Column>

            <Column header="Estado Finanzas">
              <template #body="{ data: p }">
                <Tag :value="getEstadoAnticipoLabel(p.estadoPago)" :severity="getEstadoAnticipoSeverity(p.estadoPago)" rounded />
              </template>
            </Column>

            <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: p }">
                <div class="actions-wrapper">
                  <button
                    v-if="p.idDocumentoComprobante"
                    type="button"
                    class="btn-action-outline"
                    title="Ver comprobante bancario"
                    @click="verVoucherAnticipo(p)"
                  >
                    <i class="pi pi-file-pdf"></i>
                    <span>Voucher</span>
                  </button>

                  <template v-if="p.estadoPago === 'PENDIENTE_REVISION'">
                    <button
                      type="button"
                      class="btn-action-approve"
                      title="Aprobar abono y habilitar cotización para Expediente Aduanero"
                      @click="openValidarAnticipo(p, 'APROBAR')"
                    >
                      <i class="pi pi-check"></i>
                      <span>Aprobar</span>
                    </button>
                    <button
                      type="button"
                      class="btn-action-observe"
                      title="Observar abono bancario"
                      @click="openValidarAnticipo(p, 'OBSERVAR')"
                    >
                      <i class="pi pi-eye"></i>
                      <span>Observar</span>
                    </button>
                  </template>
                  <span v-else class="text-xs text-muted">
                    {{ p.usuarioFinanzasNombre ? 'Validado por ' + p.usuarioFinanzasNombre : 'Conforme' }}
                  </span>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
      <template #footer>
        <button type="button" class="btn-outline-warm" @click="showHistorialCotizacionModal = false">
          Cerrar
        </button>
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'

import { useRoute, useRouter } from 'vue-router'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import RegistroPagoFullView from '../components/RegistroPagoFullView.vue'

import finanzasService from '../services/finanzasService'
import { expedienteService } from '../services/expedienteService'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'

const route = useRoute()
const router = useRouter()
const { showSuccess, showError } = useNotification()
const { confirmAction } = useConfirmation()
const navigationStore = useNavigationStore()

// Pestañas principales sincronizadas con Sidebar ('dashboard' | 'pre-expedientes' | 'liquidaciones' | 'conciliacion')
const activeTab = ref('dashboard')
const subTabLiquidacion = ref('emitidas') // 'emitidas' | 'por-liquidar'
const vistaPreExpedientes = ref('cotizaciones') // 'cotizaciones' | 'vouchers'

// Cotizaciones Aprobadas (Control Pre-Expediente y Cobranza)
const cotizacionesAprobadas = ref([])
const loadingCotizaciones = ref(false)
const filterEstadoCotizacionPago = ref('')
const searchQueryCotizacion = ref('')
const selectedCotizacionForPago = ref(null)

// Historial y Control de Abonos por Cotización
const showHistorialCotizacionModal = ref(false)
const selectedCotizacionHistorial = ref(null)
const pagosCotizacionList = ref([])
const loadingPagosCotizacion = ref(false)

// Datos principales
const liquidacionesList = ref([])
const expedientesList = ref([])
const loading = ref(false)
const loadingExpedientes = ref(false)
const loadingEstimado = ref(false)

// Métricas
const metricas = ref({
  totalCobradoPen: 0,
  totalCobradoUsd: 0,
  cuentasPorCobrarPen: 0,
  totalTributosSunatPen: 0,
  totalDetraccionesPen: 0,
  totalLiquidaciones: 0,
  liquidacionesPendientes: 0,
  liquidacionesPagadas: 0,
  tipoCambioOficial: 3.75
})

// Filtros y búsquedas
const filterEstado = ref('')
const searchQuery = ref('')

// Modales
const showEmisionModal = ref(false)
const showPagoModal = ref(false)
const showDetalleModal = ref(false)
const showDocsModal = ref(false)
const showValidarAnticipoModal = ref(false)
const showRegistroPagoFullView = ref(false)

// Estado para Anticipos de Cotización
const anticiposList = ref([])
const loadingAnticipos = ref(false)
const filterEstadoAnticipo = ref('')
const searchQueryAnticipo = ref('')
const selectedAnticipo = ref(null)
const validarAccion = ref('APROBAR')
const observacionFinanzas = ref('')
const submittingValidacion = ref(false)

const submittingEmision = ref(false)
const submittingPago = ref(false)
const isExpedienteFixed = ref(false)

// Formulario de Emisión
const emisionForm = ref({
  idExpediente: null,
  totalTributosSunat: 0,
  gastosAlmacenPuerto: 0,
  fleteTransporteLocal: 0,
  honorariosAgencia: 0,
  tipoCambio: 3.75,
  aplicaDetraccion: true,
  porcentajeDetraccion: 12.0,
  numeroFactura: '',
  observaciones: ''
})

// Formulario de Pago
const selectedLiquidacionPago = ref(null)
const pagoForm = ref({
  metodoPago: 'TRANSFERENCIA_BANCARIA',
  bancoDestino: 'BCP',
  numeroOperacion: '',
  numeroFactura: '',
  fechaPago: new Date().toISOString().slice(0, 16),
  observaciones: ''
})

// Detalle / Impresión
const selectedLiquidacionDetalle = ref(null)

// Documentos
const selectedDocEntidadId = ref(null)
const selectedDocCodigo = ref('')
const selectedDocTitulo = ref('')

// =========================================================================
// COMPUTED PROPERTIES
// =========================================================================

const expedientesPorLiquidar = computed(() => {
  const idsConLiquidacion = new Set(liquidacionesList.value.map(l => l.idExpediente).filter(Boolean))
  return expedientesList.value.filter(e => !idsConLiquidacion.has(e.idExpediente))
})

const expedientesSelectableList = computed(() => {
  return expedientesList.value
})

const selectedExpedienteInfo = computed(() => {
  if (!emisionForm.value.idExpediente) return null
  return expedientesList.value.find(e => e.idExpediente === emisionForm.value.idExpediente)
})

const filteredLiquidaciones = computed(() => {
  return liquidacionesList.value.filter(l => {
    const matchEstado = !filterEstado.value || l.estadoPago === filterEstado.value
    const q = searchQuery.value.trim().toLowerCase()
    const matchQuery = !q ||
      (l.codigoLiquidacion && l.codigoLiquidacion.toLowerCase().includes(q)) ||
      (l.codigoExpediente && l.codigoExpediente.toLowerCase().includes(q)) ||
      (l.numeroDam && l.numeroDam.toLowerCase().includes(q)) ||
      (l.clienteRazonSocial && l.clienteRazonSocial.toLowerCase().includes(q)) ||
      (l.numeroFactura && l.numeroFactura.toLowerCase().includes(q))
    return matchEstado && matchQuery
  })
})

const pagosRealizadosList = computed(() => {
  return liquidacionesList.value.filter(l => l.estadoPago === 'PAGADO')
})

// Control y cálculo de Cotizaciones Aprobadas
const filteredCotizacionesAprobadas = computed(() => {
  return cotizacionesAprobadas.value.filter(c => {
    const saldo = Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0)
    if (filterEstadoCotizacionPago.value === 'PENDIENTES' && saldo <= 0.01) return false
    if (filterEstadoCotizacionPago.value === 'EN_REVISION' && c.estadoPago !== 'PENDIENTE_REVISION') return false
    if (filterEstadoCotizacionPago.value === 'HABILITADAS' && !(c.estadoPago === 'ADELANTO_APROBADO' || c.tienePagoAprobado)) return false
    if (filterEstadoCotizacionPago.value === 'PAGADAS' && c.estadoPago !== 'PAGADO_TOTAL') return false

    const q = searchQueryCotizacion.value.trim().toLowerCase()
    if (!q) return true
    return (
      (c.codigoCotizacion && c.codigoCotizacion.toLowerCase().includes(q)) ||
      (c.clienteRazonSocial && c.clienteRazonSocial.toLowerCase().includes(q)) ||
      (c.clienteNumeroDocumento && c.clienteNumeroDocumento.toLowerCase().includes(q)) ||
      (c.regimenAduanero && c.regimenAduanero.toLowerCase().includes(q)) ||
      (c.mercanciaNombre && c.mercanciaNombre.toLowerCase().includes(q))
    )
  })
})

const cotizacionesPendientesCobroPreview = computed(() => {
  return cotizacionesAprobadas.value
    .filter(c => {
      const saldo = Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0)
      return saldo > 0 || c.estadoPago === 'PENDIENTE_REVISION'
    })
    .slice(0, 5)
})

const totalSaldoPendienteCotizacionesUsd = computed(() => {
  return cotizacionesAprobadas.value.reduce((acc, c) => {
    const saldo = Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0)
    return acc + saldo
  }, 0)
})

const totalCotizadoAprobadasUsd = computed(() => {
  return cotizacionesAprobadas.value.reduce((acc, c) => acc + (Number(c.totalCotizado) || 0), 0)
})

const totalAbonadoCotizacionesUsd = computed(() => {
  return cotizacionesAprobadas.value.reduce((acc, c) => acc + (Number(c.totalAbonado) || 0), 0)
})

const cotizacionesConSaldoPendienteCount = computed(() => {
  return cotizacionesAprobadas.value.filter(c => {
    const saldo = Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0)
    return saldo > 0.01
  }).length
})

const cotizacionesSinAbonoCount = computed(() => {
  return cotizacionesAprobadas.value.filter(c => !c.totalAbonado || Number(c.totalAbonado) === 0).length
})

const cotizacionesEnRevisionCount = computed(() => {
  return cotizacionesAprobadas.value.filter(c => c.estadoPago === 'PENDIENTE_REVISION').length
})

const cotizacionesAdelantoConformeCount = computed(() => {
  return cotizacionesAprobadas.value.filter(c => c.estadoPago === 'ADELANTO_APROBADO' || c.tienePagoAprobado).length
})

const cotizacionesPagadasTotalCount = computed(() => {
  return cotizacionesAprobadas.value.filter(c => c.estadoPago === 'PAGADO_TOTAL').length
})

const cotizacionesHabilitadasCount = computed(() => {
  return cotizacionesAprobadas.value.filter(c => c.habilitadoParaExpediente).length
})

const totalOperacionesCount = computed(() => {
  return liquidacionesList.value.length + cotizacionesAprobadas.value.length + anticiposList.value.length
})

// Cálculos reactivos de emisión
const calculatedSubtotal = computed(() => {
  const alm = Number(emisionForm.value.gastosAlmacenPuerto || 0)
  const fle = Number(emisionForm.value.fleteTransporteLocal || 0)
  const hon = Number(emisionForm.value.honorariosAgencia || 0)
  return alm + fle + hon
})

const calculatedIgv = computed(() => {
  return Math.round(calculatedSubtotal.value * 0.18 * 100) / 100
})

const calculatedDetraccion = computed(() => {
  if (!emisionForm.value.aplicaDetraccion) return 0
  const base = calculatedSubtotal.value + calculatedIgv.value
  const pct = Number(emisionForm.value.porcentajeDetraccion || 12) / 100
  return Math.round(base * pct * 100) / 100
})

const calculatedTotalPen = computed(() => {
  const trib = Number(emisionForm.value.totalTributosSunat || 0)
  return Math.round((trib + calculatedSubtotal.value + calculatedIgv.value) * 100) / 100
})

const calculatedTotalUsd = computed(() => {
  const tc = Number(emisionForm.value.tipoCambio || 3.75)
  if (tc <= 0) return 0
  return Math.round((calculatedTotalPen.value / tc) * 100) / 100
})

// =========================================================================
// MÉTODOS DE CARGA Y SERVICIO
// =========================================================================

async function loadAllData() {
  await Promise.all([
    loadLiquidaciones(),
    loadMetricas(),
    loadExpedientes(),
    loadAnticipos(),
    loadCotizacionesAprobadas()
  ])
}

async function loadCotizacionesAprobadas() {
  loadingCotizaciones.value = true
  try {
    const data = await finanzasService.getCotizacionesAprobadas()
    cotizacionesAprobadas.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Error al cargar cotizaciones aprobadas:', err)
  } finally {
    loadingCotizaciones.value = false
  }
}

async function loadAnticipos() {
  loadingAnticipos.value = true
  try {
    const data = await finanzasService.getAnticipos()
    anticiposList.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Error al cargar anticipos de cotización:', err)
  } finally {
    loadingAnticipos.value = false
  }
}

async function loadLiquidaciones() {
  loading.value = true
  try {
    const data = await finanzasService.getLiquidaciones()
    liquidacionesList.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Error al cargar liquidaciones:', err)
  } finally {
    loading.value = false
  }
}

async function loadMetricas() {
  try {
    const data = await finanzasService.getMetricas()
    if (data) {
      metricas.value = data
    }
  } catch (err) {
    console.error('Error al cargar métricas financieras:', err)
  }
}

async function loadExpedientes() {
  loadingExpedientes.value = true
  try {
    const data = await expedienteService.getExpedientes()
    expedientesList.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Error al cargar expedientes:', err)
  } finally {
    loadingExpedientes.value = false
  }
}


// =========================================================================
// ACCIONES DE EMISIÓN
// =========================================================================

async function openEmisionModal(expediente = null) {
  if (expediente) {
    isExpedienteFixed.value = true
    emisionForm.value.idExpediente = expediente.idExpediente
    await fetchEstimado(expediente.idExpediente)
  } else {
    isExpedienteFixed.value = false
    emisionForm.value = {
      idExpediente: null,
      totalTributosSunat: 0,
      gastosAlmacenPuerto: 0,
      fleteTransporteLocal: 0,
      honorariosAgencia: 0,
      tipoCambio: 3.75,
      aplicaDetraccion: true,
      porcentajeDetraccion: 12.0,
      numeroFactura: '',
      observaciones: ''
    }
  }
  showEmisionModal.value = true
}

async function onExpedienteSelected() {
  if (!emisionForm.value.idExpediente) return
  await fetchEstimado(emisionForm.value.idExpediente)
}

async function fetchEstimado(idExpediente) {
  loadingEstimado.value = true
  try {
    const est = await finanzasService.calcularEstimado(idExpediente)
    if (est) {
      emisionForm.value.totalTributosSunat = Number(est.totalTributosSunatEstimado || 0)
      emisionForm.value.gastosAlmacenPuerto = Number(est.gastosAlmacenPuertoEstimado || 0)
      emisionForm.value.fleteTransporteLocal = Number(est.fleteTransporteLocalEstimado || 0)
      emisionForm.value.honorariosAgencia = Number(est.honorariosAgenciaEstimado || 0)
      emisionForm.value.tipoCambio = Number(est.tipoCambioSugerido || 3.75)
      emisionForm.value.aplicaDetraccion = est.aplicaDetraccion !== false
    }
  } catch (err) {
    console.error('Error al calcular estimado para expediente:', err)
  } finally {
    loadingEstimado.value = false
  }
}

async function submitEmision() {
  if (!emisionForm.value.idExpediente) return
  submittingEmision.value = true
  try {
    await finanzasService.emitirLiquidacion(emisionForm.value)
    showEmisionModal.value = false
    await loadAllData()
  } catch (err) {
    console.error('Error al emitir liquidación:', err)
    showError('Error al emitir liquidación: ' + (err.response?.data?.message || err.message))
  } finally {
    submittingEmision.value = false
  }
}

// =========================================================================
// ACCIONES DE COBRANZA Y PAGO
// =========================================================================

function openPagoModal(liquidacion) {
  selectedLiquidacionPago.value = liquidacion
  pagoForm.value = {
    metodoPago: 'TRANSFERENCIA_BANCARIA',
    bancoDestino: 'BCP',
    numeroOperacion: '',
    numeroFactura: liquidacion.numeroFactura || '',
    fechaPago: new Date().toISOString().slice(0, 16),
    observaciones: ''
  }
  showPagoModal.value = true
}

async function submitPago() {
  if (!selectedLiquidacionPago.value || !pagoForm.value.numeroOperacion) return
  submittingPago.value = true
  try {
    await finanzasService.registrarPago(selectedLiquidacionPago.value.idLiquidacion, pagoForm.value)
    showPagoModal.value = false
    await loadAllData()
  } catch (err) {
    console.error('Error al registrar cobro:', err)
    showError('Error al registrar cobro: ' + (err.response?.data?.message || err.message))
  } finally {
    submittingPago.value = false
  }
}

function openDetalleModal(liquidacion) {
  selectedLiquidacionDetalle.value = liquidacion
  showDetalleModal.value = true
}

function imprimirLiquidacion() {
  window.print()
}

function openDocumentos(item) {
  selectedDocEntidadId.value = item.idExpediente
  selectedDocCodigo.value = item.codigoLiquidacion || item.codigoExpediente
  selectedDocTitulo.value = `Documentación de Liquidación (${selectedDocCodigo.value})`
  showDocsModal.value = true
}

async function confirmAnular(liquidacion) {
  const confirmed = await confirmAction(
    `¿Está seguro de anular la liquidación ${liquidacion.codigoLiquidacion}? Esta acción no se puede deshacer.`,
    'Confirmar Anulación de Liquidación'
  )
  if (!confirmed) return
  try {
    await finanzasService.anularLiquidacion(liquidacion.idLiquidacion)
    showSuccess(`Liquidación ${liquidacion.codigoLiquidacion} anulada correctamente`)
    await loadAllData()
  } catch (err) {
    console.error('Error al anular liquidación:', err)
    showError('No se pudo anular la liquidación.')
  }
}

// =========================================================================
// FORMATTERS Y UTILS
// =========================================================================

function formatCurrencyPen(val) {
  const num = Number(val || 0)
  return 'S/ ' + num.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatEstadoPago(st) {
  if (!st) return 'DESCONOCIDO'
  switch (st.toUpperCase()) {
    case 'PENDIENTE': return 'Pendiente de Cobro'
    case 'PAGADO': return 'Cobrado / Conforme'
    case 'ANULADO': return 'Anulado'
    default: return st
  }
}

function getEstadoSeverity(st) {
  switch (st?.toUpperCase()) {
    case 'PAGADO': return 'success'
    case 'PENDIENTE': return 'warn'
    case 'ANULADO': return 'danger'
    default: return 'info'
  }
}

function getCanalSeverity(canal) {
  switch (canal?.toUpperCase()) {
    case 'VERDE': return 'success'
    case 'NARANJA': return 'warn'
    case 'ROJO': return 'danger'
    default: return 'info'
  }
}

function formatDate(dt) {
  if (!dt) return '—'
  return String(dt).split('T')[0]
}

function formatDateTime(dt) {
  if (!dt) return '—'
  const d = new Date(dt)
  return isNaN(d.getTime()) ? dt : d.toLocaleString('es-PE')
}

const anticiposPendientesCount = computed(() => {
  return anticiposList.value.filter((a) => a.estadoPago === 'PENDIENTE_REVISION').length
})

const filteredAnticipos = computed(() => {
  let list = anticiposList.value
  if (filterEstadoAnticipo.value) {
    list = list.filter((a) => a.estadoPago === filterEstadoAnticipo.value)
  }
  if (searchQueryAnticipo.value.trim()) {
    const q = searchQueryAnticipo.value.toLowerCase()
    list = list.filter(
      (a) =>
        (a.codigoCotizacion && a.codigoCotizacion.toLowerCase().includes(q)) ||
        (a.codigoPago && a.codigoPago.toLowerCase().includes(q)) ||
        (a.clienteRazonSocial && a.clienteRazonSocial.toLowerCase().includes(q)) ||
        (a.numeroOperacion && a.numeroOperacion.toLowerCase().includes(q)) ||
        (a.bancoDestino && a.bancoDestino.toLowerCase().includes(q))
    )
  }
  return list
})

function formatTipoAbono(tipo) {
  switch (tipo) {
    case 'ADELANTO': return 'Adelanto / Anticipo'
    case 'PAGO_TOTAL': return 'Pago Total (100%)'
    case 'GARANTIA_CREDITO': return 'Línea de Crédito'
    default: return tipo || 'Abono'
  }
}

function getEstadoAnticipoLabel(st) {
  switch (st) {
    case 'PENDIENTE_REVISION': return 'Pendiente Revisión'
    case 'APROBADO': return 'Aprobado / Conforme'
    case 'OBSERVADO': return 'Observado'
    case 'RECHAZADO': return 'Rechazado'
    default: return st || 'Pendiente'
  }
}

function getEstadoAnticipoSeverity(st) {
  switch (st) {
    case 'APROBADO': return 'success'
    case 'PENDIENTE_REVISION': return 'warn'
    case 'OBSERVADO': return 'danger'
    case 'RECHAZADO': return 'danger'
    default: return 'info'
  }
}

function openValidarAnticipo(anticipo, accion = 'APROBAR') {
  selectedAnticipo.value = anticipo
  validarAccion.value = accion
  observacionFinanzas.value = accion === 'APROBAR' ? 'Abono verificado y conforme en cuenta bancaria.' : ''
  showValidarAnticipoModal.value = true
}

async function submitValidarAnticipo() {
  if (!selectedAnticipo.value) return
  submittingValidacion.value = true
  try {
    await finanzasService.validarAnticipo(selectedAnticipo.value.idPago, {
      accion: validarAccion.value,
      observacionFinanzas: observacionFinanzas.value
    })
    showValidarAnticipoModal.value = false
    await Promise.all([loadAnticipos(), loadCotizacionesAprobadas(), loadMetricas()])
    if (selectedCotizacionHistorial.value) {
      const data = await finanzasService.getPagosCotizacion(selectedCotizacionHistorial.value.idCotizacion)
      pagosCotizacionList.value = Array.isArray(data) ? data : []
    }
  } catch (err) {
    console.error('Error al validar anticipo:', err)
    showError(err.response?.data?.message || 'Error al validar el pago bancario.')
  } finally {
    submittingValidacion.value = false
  }
}

function verVoucherAnticipo(anticipo) {
  if (anticipo.idCotizacion) {
    selectedDocEntidadId.value = anticipo.idCotizacion
    selectedDocCodigo.value = anticipo.codigoCotizacion || `COT-${anticipo.idCotizacion}`
    selectedDocTitulo.value = `Documentos y Voucher - ${anticipo.codigoCotizacion}`
    showDocsModal.value = true
  }
}

function openRegistrarPagoForCotizacion(cotizacion) {
  selectedCotizacionForPago.value = cotizacion
  showHistorialCotizacionModal.value = false
  showRegistroPagoFullView.value = true
}

function openRegistrarPagoModal(cotizacion = null) {
  selectedCotizacionForPago.value = cotizacion
  showRegistroPagoFullView.value = true
}

function closeRegistroPagoFullView() {
  showRegistroPagoFullView.value = false
  selectedCotizacionForPago.value = null
}
const closeRegistrarPagoFullView = closeRegistroPagoFullView

async function handlePagoRegistradoDesdeFullView(data) {
  closeRegistroPagoFullView()
  await onPagoRegistradoEnFinanzas()
}

function closeRegistrarPagoModal() {
  closeRegistroPagoFullView()
}

async function openHistorialCotizacion(cotizacion) {
  selectedCotizacionHistorial.value = cotizacion
  showHistorialCotizacionModal.value = true
  loadingPagosCotizacion.value = true
  try {
    const data = await finanzasService.getPagosCotizacion(cotizacion.idCotizacion)
    pagosCotizacionList.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Error al cargar pagos de cotización:', err)
  } finally {
    loadingPagosCotizacion.value = false
  }
}

function switchTab(tab, sub = null) {
  showRegistroPagoFullView.value = false
  selectedCotizacionForPago.value = null

  if (tab === 'anticipos') {
    activeTab.value = 'pre-expedientes'
    if (sub) vistaPreExpedientes.value = sub
    navigationStore.setFinanzasSubOption('pre-expedientes')
    if (route.query.tab !== 'pre-expedientes') {
      router.replace({ query: { ...route.query, tab: 'pre-expedientes' } })
    }
    return
  }
  if (tab === 'por-liquidar') {
    activeTab.value = 'liquidaciones'
    subTabLiquidacion.value = 'por-liquidar'
    navigationStore.setFinanzasSubOption('liquidaciones')
    if (route.query.tab !== 'liquidaciones') {
      router.replace({ query: { ...route.query, tab: 'liquidaciones' } })
    }
    return
  }
  activeTab.value = tab
  if (sub && tab === 'pre-expedientes') {
    vistaPreExpedientes.value = sub
  }
  navigationStore.setFinanzasSubOption(tab)
  if (route.query.tab !== tab) {
    router.replace({ query: { ...route.query, tab } })
  }
}

function formatEstadoPagoCotizacionLabel(st) {
  switch (st) {
    case 'PENDIENTE_REVISION': return 'En Revisión'
    case 'ADELANTO_APROBADO': return 'Adelanto Conforme'
    case 'PAGADO_TOTAL': return 'Cancelado 100%'
    case 'OBSERVADO': return 'Observado'
    case 'SIN_PAGO':
    default: return 'Sin Abono'
  }
}

function formatEstadoPagoCotizacionSeverity(st) {
  switch (st) {
    case 'PAGADO_TOTAL': return 'success'
    case 'ADELANTO_APROBADO': return 'success'
    case 'PENDIENTE_REVISION': return 'warn'
    case 'OBSERVADO': return 'danger'
    case 'SIN_PAGO':
    default: return 'secondary'
  }
}

function getToolbarTitle() {
  switch (activeTab.value) {
    case 'dashboard': return 'Dashboard Financiero & Monitoreo Ejecutivo'
    case 'pre-expedientes':
    case 'anticipos': return 'Gestión de Pagos Pre-Expedientes'
    case 'liquidaciones':
    case 'por-liquidar': return 'Liquidación Aduanera & Control de Cuentas'
    case 'conciliacion': return 'Facturación & Conciliación Bancaria'
    default: return 'Liquidación Aduanera y Cobranzas'
  }
}

async function onPagoRegistradoEnFinanzas() {
  await Promise.all([loadAnticipos(), loadCotizacionesAprobadas(), loadMetricas()])
  if (selectedCotizacionHistorial.value) {
    const data = await finanzasService.getPagosCotizacion(selectedCotizacionHistorial.value.idCotizacion)
    pagosCotizacionList.value = Array.isArray(data) ? data : []
  }
}

// Sincronizar activeTab con query param de la URL
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && ['dashboard', 'pre-expedientes', 'anticipos', 'liquidaciones', 'por-liquidar', 'conciliacion', 'facturacion'].includes(newTab)) {
      if (navigationStore.finanzasSubOption !== newTab) {
        navigationStore.setFinanzasSubOption(newTab)
      }
    }
  },
  { immediate: true }
)

// Escuchar cambios de subopción desde el menú lateral si aplica
watch(
  () => [navigationStore.finanzasSubOption, navigationStore.navTick],
  ([val]) => {
    // Si no es un disparador de acción directa, cerrar el formulario completo para ver la vista
    if (!navigationStore.finanzasActionTrigger) {
      showRegistroPagoFullView.value = false
      selectedCotizacionForPago.value = null
    }

    if (val === 'pre-expedientes' || val === 'anticipos') {
      activeTab.value = 'pre-expedientes'
      vistaPreExpedientes.value = 'cotizaciones'
    } else if (val === 'liquidaciones' || val === 'liquidacion' || val === 'por-liquidar') {
      activeTab.value = 'liquidaciones'
      if (val !== 'por-liquidar') {
        subTabLiquidacion.value = 'emitidas'
      }
    } else if (val === 'conciliacion' || val === 'facturacion') {
      activeTab.value = 'conciliacion'
    } else {
      activeTab.value = 'dashboard'
    }
  }
)

// Escuchar disparadores de acciones desde el Sidebar
watch(
  () => [navigationStore.finanzasActionTrigger, navigationStore.navTick],
  ([act]) => {
    if (act === 'registrar-anticipo') {
      activeTab.value = 'pre-expedientes'
      navigationStore.finanzasActionTrigger = null
      openRegistrarPagoModal(null)
    }
  }
)

onMounted(() => {
  loadAllData()
})
</script>

<style scoped>
.finanzas-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
  width: 100%;
}

.finanzas-main-layout {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
  width: 100%;
}

/* 1. Header Action Toolbar Editorial */
.finanzas-action-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.toolbar-identity {
  display: flex;
  flex-direction: column;
  gap: var(--space-1, 0.25rem);
}

.badge-kicker {
  font-family: var(--font-family-mono, monospace);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #8c6b3e);
  text-transform: uppercase;
}

.title-with-badge {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.toolbar-title {
  margin: 0;
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--color-text-main, #2b2520);
}

.count-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.6rem;
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-text-main, #2b2520);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-pill, 9999px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
}

.toolbar-description {
  margin: 0;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-muted, #6f6356);
  max-width: 820px;
  line-height: 1.45;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-shrink: 0;
}

.btn-warm-outline {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 0.95rem;
  border-radius: var(--radius-sm, 0.375rem);
  border: 1px solid var(--color-border, #e4dacb);
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-warm-outline:hover:not(:disabled) {
  background-color: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
}

.btn-warm-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 1rem;
  border-radius: var(--radius-sm, 0.375rem);
  border: 1px solid var(--color-primary, #352f2a);
  background-color: var(--color-primary, #352f2a);
  color: var(--color-primary-text, #ffffff);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-espresso:hover {
  filter: brightness(1.15);
}

/* Pestañas de Navegación */
.module-tabs-nav {
  display: flex;
  gap: 0.75rem;
  margin: 0.5rem 0;
  border-bottom: 2px solid var(--color-border, #e4dacb);
  padding-bottom: 0.5rem;
  overflow-x: auto;
}

.tab-nav-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  border: none;
  background: transparent;
  color: var(--color-text-muted, #6f6356);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-md, 0.5rem);
  transition: all 0.2s ease;
  white-space: nowrap;
}

.tab-nav-btn:hover {
  background: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-text-main, #2b2520);
}

.tab-nav-btn.active {
  background: var(--color-primary, #352f2a);
  color: #fff;
}

.tab-counter {
  font-size: 0.75rem;
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-pill, 9999px);
  background: rgba(0, 0, 0, 0.08);
}

.tab-counter.counter-alert {
  background: var(--color-warning-bg, #fef3c7);
  color: var(--color-warning-text, #92400e);
  border: 1px solid var(--color-warning-border, #fde68a);
  font-weight: 700;
}

.tab-nav-btn.active .tab-counter {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.tab-nav-btn.active .tab-counter.counter-alert {
  background: #ea580c;
  color: #ffffff;
}

/* Métricas KPI Grid Warm Editorial */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
  margin-bottom: var(--space-2, 0.5rem);
}

.kpi-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-md, 0.5rem);
  padding: var(--space-4, 1rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover, 0 4px 12px rgba(45, 35, 25, 0.08));
}

.kpi-card.highlight-card {
  border-left: 3px solid var(--color-accent-kicker, #8c6b3e);
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kpi-index {
  font-family: var(--font-family-mono, monospace);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-subtle, #998c7e);
}

.kpi-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-pill, 9999px);
}

.badge-success {
  background-color: var(--color-success-bg, #eaf6ed);
  color: var(--color-success-text, #1e6638);
  border: 1px solid var(--color-success-border, #bce5c6);
}

.badge-warning {
  background-color: var(--color-warning-bg, #fdf6ec);
  color: var(--color-warning-text, #945308);
  border: 1px solid var(--color-warning-border, #f6e0b5);
}

.badge-info {
  background-color: var(--color-info-bg, #eff6ff);
  color: var(--color-info-text, #1e40af);
  border: 1px solid var(--color-info-border, #bfdbfe);
}

.badge-neutral {
  background-color: var(--color-bg-subtle, #f2ebe1);
  color: var(--color-text-muted, #6f6356);
  border: 1px solid var(--color-border, #e4dacb);
}

.kpi-body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.kpi-value {
  font-size: 1.4rem;
  font-weight: 800;
  line-height: 1.1;
  color: var(--color-text-main, #2b2520);
}

.kpi-label {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #6f6356);
  font-weight: 600;
}

/* Secciones de Contenido */
.tab-content-section {
  background: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.25rem;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
  color: var(--color-text-main);
}

.section-header-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.badge-kicker {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  display: block;
}

.section-title {
  margin: 0.15rem 0 0;
  font-size: 1.25rem;
  color: var(--color-text-main);
  font-weight: 700;
}

.section-subtitle {
  margin: 0.25rem 0 0;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
}

.section-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
}

.filter-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background-color: var(--color-bg-surface);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236f6356' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 1rem 1rem !important;
  font-size: 0.85rem;
  color: var(--color-text-main);
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
  font-size: 0.85rem;
}

.search-input {
  padding: 0.5rem 0.75rem 0.5rem 2.2rem;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 0.85rem;
  width: 240px;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* Botones */
.btn-primary-action {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border: 1px solid var(--color-primary);
  padding: 0.55rem 1rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary-action:hover {
  filter: brightness(1.15);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.55rem 0.9rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline-warm:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

/* Tabla Celdas & Estilos Deep para PrimeVue DataTable */
:deep(.custom-datatable.p-datatable) {
  background: transparent !important;
  width: 100%;
}

:deep(.custom-datatable .p-datatable-table) {
  background: transparent !important;
  border-collapse: separate;
  border-spacing: 0;
  width: 100%;
}

:deep(.custom-datatable .p-datatable-thead > tr > th) {
  background: var(--color-bg-subtle) !important;
  color: var(--color-text-muted) !important;
  border-bottom: 1px solid var(--color-border) !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
  font-size: 0.6875rem !important;
  font-weight: 700 !important;
  letter-spacing: 0.06em !important;
  text-transform: uppercase !important;
  padding: 0.85rem 1rem !important;
}

:deep(.custom-datatable .p-datatable-tbody > tr) {
  background: var(--color-bg-surface) !important;
  color: var(--color-text-main) !important;
  border-bottom: 1px solid var(--color-border-subtle) !important;
  transition: background var(--transition-fast) !important;
}

:deep(.custom-datatable .p-datatable-tbody > tr:hover) {
  background: var(--color-bg-subtle) !important;
}

:deep(.custom-datatable .p-datatable-tbody > tr > td) {
  border-bottom: 1px solid var(--color-border-subtle) !important;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
  color: var(--color-text-main) !important;
  padding: 0.85rem 1rem !important;
}

:deep(.custom-datatable .p-datatable-empty-message td) {
  background: var(--color-bg-surface) !important;
  color: var(--color-text-muted) !important;
}

.ticket-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.25rem 0.5rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 4px;
  font-size: 0.85rem;
  color: var(--color-text-main);
}

.client-cell {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--color-text-main);
}

.client-sub {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.sub-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.2rem;
}

.amount-main-cell {
  display: flex;
  flex-direction: column;
}

.amount-sub-cell {
  display: flex;
  flex-direction: column;
}

.detraccion-note {
  font-size: 0.7rem;
  color: var(--color-accent-kicker, #9a7f60);
  font-weight: 600;
}

.table-actions {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
}

.btn-action-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #2e6047;
  color: #fff;
  border: none;
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-primary:hover {
  background: #234836;
}

.btn-action-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-action-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-action-danger {
  background: transparent;
  color: #a34e36;
  border: 1px solid #e0c2ba;
  padding: 0.35rem 0.55rem;
  border-radius: 4px;
  font-size: 0.78rem;
  cursor: pointer;
}

.btn-action-danger:hover {
  background: #fdf2f0;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
}

.empty-icon {
  font-size: 2.5rem;
  color: var(--color-accent, #9a7f60);
  margin-bottom: 0.5rem;
}

.empty-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
  margin: 0;
}

.empty-desc {
  font-size: 0.85rem;
  color: #7c7267;
  margin: 0.25rem 0 0;
}

/* Modales & Formularios */
.modal-body {
  padding: 0.5rem 0;
}

.grid-2-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.grid-3-cols {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.form-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-primary, #352f2a);
}

.form-label.required::after {
  content: ' *';
  color: #a34e36;
}

.form-input, .form-textarea {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border, #d8cdbf);
  border-radius: 6px;
  font-size: 0.88rem;
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #352f2a);
}

.form-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  border: 1px solid var(--color-border, #d8cdbf);
  border-radius: 6px;
  font-size: 0.88rem;
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #352f2a);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236f6356' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 1rem 1rem !important;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  outline: none;
  border-color: var(--color-primary, #352f2a);
}

.currency-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.currency-prefix {
  position: absolute;
  left: 0.75rem;
  font-weight: 700;
  color: #7c7267;
  font-size: 0.85rem;
}

.currency-input-wrap .form-input {
  padding-left: 2.2rem;
}

.field-hint {
  font-size: 0.7rem;
  color: #8c827a;
}

.checkbox-group {
  justify-content: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  color: var(--color-primary, #352f2a);
}

.exp-summary-banner {
  background: var(--color-bg-card, #ede6db);
  padding: 0.75rem 1rem;
  border-radius: 6px;
  border-left: 4px solid var(--color-accent, #9a7f60);
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.exp-summary-item .label {
  font-weight: 700;
  font-size: 0.78rem;
  color: #7c7267;
  margin-right: 0.4rem;
}

.exp-summary-item .val {
  font-size: 0.85rem;
  color: var(--color-primary, #352f2a);
  font-weight: 600;
}

.totals-breakdown-card {
  background: #fbf9f6;
  border: 1px solid var(--color-border-warm, #d8cdbf);
  border-radius: 6px;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.total-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  color: #4a4036;
}

.total-highlight {
  border-top: 1px dashed var(--color-border-warm, #d8cdbf);
  padding-top: 0.4rem;
  margin-top: 0.2rem;
  font-size: 0.95rem;
}

/* Pago Modal Banner */
.pago-summary-banner {
  background: var(--color-bg-card, #ede6db);
  padding: 1rem;
  border-radius: 6px;
  border-left: 4px solid #2e6047;
}

.pago-banner-title {
  font-size: 1.1rem;
  color: var(--color-primary, #352f2a);
}

.pago-banner-client {
  font-size: 0.85rem;
  color: #7c7267;
  margin-bottom: 0.5rem;
}

.pago-banner-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1rem;
}

/* Voucher de Liquidación Imprimible */
.invoice-container {
  padding: 1rem;
  background: #fff;
  border: 1px solid #d8cdbf;
  border-radius: 6px;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 2px solid #352f2a;
  padding-bottom: 1rem;
  margin-bottom: 1rem;
}

.invoice-company {
  font-size: 1.2rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  color: #352f2a;
}

.invoice-code-box {
  text-align: right;
}

.invoice-title {
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #9a7f60;
}

.invoice-number {
  font-size: 1.25rem;
  font-weight: 800;
  color: #352f2a;
}

.invoice-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  background: #fbf9f6;
  padding: 0.75rem 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.meta-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #7c7267;
  margin-right: 0.4rem;
}

.meta-val {
  font-size: 0.85rem;
  font-weight: 600;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

.invoice-table th {
  background: #ede6db;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #352f2a;
  border-bottom: 1px solid #d8cdbf;
}

.invoice-table td {
  padding: 0.65rem 0.75rem;
  border-bottom: 1px solid #eee;
  font-size: 0.85rem;
}

.invoice-table tfoot td {
  padding: 0.45rem 0.75rem;
  border-bottom: none;
}

.row-total td {
  border-top: 2px solid #352f2a;
  border-bottom: 1px solid #352f2a;
}

.invoice-paid-banner {
  background: #eaf4ed;
  border: 1px solid #b7dfc3;
  padding: 0.75rem 1rem;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.badge-paid {
  font-size: 0.75rem;
  font-weight: 800;
  background: #2e6047;
  color: #fff;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  letter-spacing: 0.05em;
}

.tag-xs {
  font-size: 0.7rem;
  padding: 0.1rem 0.4rem;
}

.tag-status {
  font-size: 0.75rem;
  font-weight: 700;
}

@media print {
  body * {
    visibility: hidden;
  }
  .invoice-container, .invoice-container * {
    visibility: visible;
  }
  .invoice-container {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    border: none;
  }
}

.btn-action-approve {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #15803d;
  color: #fff;
  border: none;
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-action-approve:hover {
  opacity: 0.9;
}

.btn-action-observe {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #b45309;
  color: #fff;
  border: none;
  padding: 0.35rem 0.65rem;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-action-observe:hover {
  opacity: 0.9;
}

.anticipo-validation-summary {
  background: var(--color-bg-card, #f8f6f0);
  border: 1px solid var(--color-border-warm, #d8cdbf);
  border-radius: 6px;
  padding: 0.875rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8125rem;
}

.summary-line .label {
  color: var(--color-text-muted, #7c7267);
  font-weight: 600;
}

.alert-box-success {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  color: #166534;
  font-size: 0.8125rem;
}

.alert-box-warning {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
  color: #92400e;
  font-size: 0.8125rem;
}

/* ========================================================================= */
/* ESTILOS DE DASHBOARD FINANCIERO & PRE-EXPEDIENTES (WARM EDITORIAL 2.0)    */
/* ========================================================================= */

.dashboard-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 1.15rem;
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-md, 0.5rem);
  gap: 1rem;
  flex-wrap: wrap;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.status-indicator-wrap {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.78rem;
}

.pulse-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background-color: #16a34a;
  box-shadow: 0 0 0 0 rgba(22, 163, 74, 0.7);
  animation: pulse-green 2s infinite;
}

@keyframes pulse-green {
  0% { box-shadow: 0 0 0 0 rgba(22, 163, 74, 0.5); }
  70% { box-shadow: 0 0 0 6px rgba(22, 163, 74, 0); }
  100% { box-shadow: 0 0 0 0 rgba(22, 163, 74, 0); }
}

.status-label {
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  letter-spacing: 0.05em;
  font-size: 0.75rem;
}

.separator {
  color: var(--color-border-strong, #c8b9a6);
}

.status-detail {
  font-size: 0.78rem;
  color: var(--color-text-muted, #6f6356);
}

.quick-shortcuts {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn-quick-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.7rem;
  background: var(--color-bg-subtle, #f5efe6);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-pill, 9999px);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-main, #2b2520);
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-quick-tag:hover {
  background: var(--color-border, #e4dacb);
  transform: translateY(-1px);
}

/* Grilla Analítica Dual de Cobranzas */
.dashboard-analytics-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: var(--space-4, 1rem);
}

.analytics-card {
  background: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.analytics-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.card-main-title {
  margin: 0.2rem 0 0;
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--color-primary, #352f2a);
}

.btn-text-link {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: transparent;
  border: none;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-accent-kicker, #8c6b3e);
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  transition: background 0.15s ease;
  white-space: nowrap;
}

.btn-text-link:hover {
  background: var(--color-bg-subtle, #f5efe6);
}

.analytics-desc {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted, #6f6356);
  line-height: 1.4;
}

/* Funnel Pre-Expedientes */
.funnel-progress-wrap {
  display: flex;
  width: 100%;
  height: 14px;
  background: #ede5d8;
  border-radius: 7px;
  overflow: hidden;
  margin-top: 0.25rem;
  gap: 2px;
}

.funnel-segment {
  height: 100%;
  transition: width 0.3s ease;
}

.seg-sin-pago {
  background-color: #94a3b8;
}

.seg-revision {
  background-color: #f59e0b;
}

.seg-adelanto {
  background-color: #3b82f6;
}

.seg-total {
  background-color: #10b981;
}

.funnel-legend-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.75rem;
  color: var(--color-text-main, #2b2520);
  font-weight: 600;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-sin-pago {
  background-color: #94a3b8;
}

.dot-revision {
  background-color: #f59e0b;
}

.dot-adelanto {
  background-color: #3b82f6;
}

.dot-total {
  background-color: #10b981;
}

.funnel-financial-summary {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px dashed var(--color-border, #e4dacb);
}

.financial-pill {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  background: var(--color-bg-subtle, #f7f3ee);
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  border: 1px solid var(--color-border, #e4dacb);
  flex: 1;
  min-width: 120px;
  font-size: 0.75rem;
}

.financial-pill .lbl {
  color: var(--color-text-muted, #7c7267);
  font-weight: 600;
  font-size: 0.7rem;
}

.financial-pill strong {
  font-size: 0.88rem;
}

/* Balance Operativo de Liquidaciones */
.operations-kpi-rows {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  margin-top: 0.25rem;
}

.op-stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.6rem 0.85rem;
  background: var(--color-bg-subtle, #f9f6f1);
  border: 1px solid var(--color-border, #ede5d8);
  border-radius: 6px;
}

.op-stat-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.8125rem;
}

.op-stat-left div {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.op-stat-left .subtext {
  font-size: 0.7rem;
  color: var(--color-text-muted, #7c7267);
}

.icon-pending {
  color: #64748b;
  font-size: 1.15rem;
}

.icon-warning {
  color: #d97706;
  font-size: 1.15rem;
}

.icon-success {
  color: #16a34a;
  font-size: 1.15rem;
}

.card-action-bar-end {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 0.75rem;
}

/* Pre-Expedientes Sub-opción 02 Controls */
.view-mode-toggle {
  display: inline-flex;
  background: var(--color-bg-subtle, #ede5d8);
  border-radius: var(--radius-sm, 0.375rem);
  padding: 0.2rem;
  gap: 0.2rem;
}

.view-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 0.8rem;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 600;
  border-radius: var(--radius-xs, 0.25rem);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.view-toggle-btn:hover {
  color: var(--color-text-main);
}

.view-toggle-btn.active {
  background: var(--color-bg-surface) !important;
  color: var(--color-text-main) !important;
  font-weight: 700;
  box-shadow: var(--shadow-subtle);
}

.mini-alert-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ea580c;
  position: absolute;
  top: 5px;
  right: 5px;
}

.sub-kpi-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
  background: var(--color-bg-subtle) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md, 0.5rem);
  padding: 0.75rem 1.15rem;
}

.sub-kpi-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.sub-kpi-lbl {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.sub-kpi-val {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.table-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.filter-pills-wrap {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.filter-pill {
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-muted);
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill, 9999px);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.filter-pill:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
}

.filter-pill.active {
  background: var(--color-primary) !important;
  color: var(--color-primary-text) !important;
  border-color: var(--color-primary) !important;
  font-weight: 700;
}

.progress-col {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.mini-progress-bar {
  width: 100%;
  height: 6px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 9999px;
  overflow: hidden;
}

.mini-progress-fill {
  height: 100%;
  background: var(--color-success-text, #10b981);
  border-radius: 9999px;
  transition: width 0.3s ease;
}

.inner-subtab-switch {
  display: flex;
  gap: 0.5rem;
}

.subtab-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 1rem;
  border: 1px solid var(--color-border, #d8cdbf);
  background: var(--color-bg-surface, #ffffff);
  color: var(--color-text-muted, #6f6356);
  font-size: 0.8125rem;
  font-weight: 600;
  border-radius: var(--radius-sm, 0.375rem);
  cursor: pointer;
  transition: all 0.2s ease;
}

.subtab-pill:hover {
  background: var(--color-bg-subtle, #f5efe6);
}

.subtab-pill.active {
  background: var(--color-primary, #352f2a);
  color: #ffffff;
  border-color: var(--color-primary, #352f2a);
  font-weight: 700;
}

.subtab-badge {
  font-size: 0.7rem;
  padding: 0.1rem 0.45rem;
  border-radius: 9999px;
  background: rgba(0, 0, 0, 0.08);
  font-weight: 700;
}

.subtab-pill.active .subtab-badge {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.btn-action-primary-sm {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: #166534;
  color: #ffffff;
  border: none;
  padding: 0.35rem 0.7rem;
  border-radius: 4px;
  font-size: 0.78rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-action-primary-sm:hover {
  background: #14532d;
}

.actions-wrapper {
  display: inline-flex;
  gap: 0.4rem;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.modal-subheading-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.subheading-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-primary, #352f2a);
}

/* Media Queries & Responsive */
@media (max-width: 1024px) {
  .metrics-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .finanzas-action-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
  .dashboard-analytics-grid {
    grid-template-columns: 1fr;
  }
  .sub-kpi-bar {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-kpi-grid {
    grid-template-columns: 1fr;
  }
  .sub-kpi-bar {
    grid-template-columns: 1fr;
  }
  .funnel-legend-grid {
    grid-template-columns: 1fr;
  }
}

/* Modo Oscuro (.dark-theme) */
:global(.dark-theme) .finanzas-view .filter-select,
:global(.dark-theme) .finanzas-view .form-select {
  background-color: var(--color-bg-surface, #24221c) !important;
  color: var(--color-text-main, #ede5d8) !important;
  border-color: var(--color-border, #444138) !important;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23d6b476' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") !important;
}

:global(.dark-theme) .finanzas-view .tab-content-section,
:global(.dark-theme) .tab-content-section,
:global(.dark-theme) .finanzas-view .anticipo-validation-summary {
  background: var(--color-bg-surface) !important;
  border-color: var(--color-border) !important;
  color: var(--color-text-main) !important;
}

:global(.dark-theme) .finanzas-view :deep(.custom-datatable.p-datatable) .p-datatable-thead > tr > th {
  background: var(--color-bg-subtle) !important;
  color: var(--color-text-muted) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

:global(.dark-theme) .finanzas-view :deep(.custom-datatable.p-datatable) .p-datatable-tbody > tr {
  background: var(--color-bg-surface) !important;
  color: var(--color-text-main) !important;
  border-bottom: 1px solid var(--color-border-subtle) !important;
}

:global(.dark-theme) .finanzas-view :deep(.custom-datatable.p-datatable) .p-datatable-tbody > tr:hover {
  background: var(--color-bg-subtle) !important;
}

:global(.dark-theme) .finanzas-view :deep(.custom-datatable.p-datatable) .p-datatable-tbody > tr > td {
  border-bottom: 1px solid var(--color-border-subtle) !important;
  color: var(--color-text-main) !important;
}

:global(.dark-theme) .finanzas-view .btn-espresso {
  background-color: var(--color-primary);
  color: var(--color-primary-text) !important;
  border-color: var(--color-primary);
}

:global(.dark-theme) .finanzas-view .btn-warm-outline {
  background-color: var(--color-bg-surface);
  border-color: var(--color-border);
  color: var(--color-text-main);
}

:global(.dark-theme) .finanzas-view .btn-warm-outline:hover:not(:disabled) {
  background-color: var(--color-bg-subtle);
}

:global(.dark-theme) .finanzas-view .dashboard-status-bar,
:global(.dark-theme) .finanzas-view .analytics-card {
  background: var(--color-bg-surface) !important;
  border-color: var(--color-border) !important;
}

:global(.dark-theme) .finanzas-view .sub-kpi-bar {
  background: var(--color-bg-subtle) !important;
  border-color: var(--color-border) !important;
}

:global(.dark-theme) .finanzas-view .card-main-title,
:global(.dark-theme) .finanzas-view .sub-kpi-val,
:global(.dark-theme) .finanzas-view .subheading-title {
  color: var(--color-text-main);
}

:global(.dark-theme) .finanzas-view .btn-quick-tag,
:global(.dark-theme) .finanzas-view .financial-pill,
:global(.dark-theme) .finanzas-view .op-stat-row {
  background-color: var(--color-bg-subtle);
  border-color: var(--color-border);
  color: var(--color-text-main);
}

:global(.dark-theme) .finanzas-view .view-mode-toggle {
  background-color: var(--color-bg-subtle);
  border-color: var(--color-border-subtle);
}

:global(.dark-theme) .finanzas-view .view-toggle-btn.active {
  background-color: var(--color-bg-surface) !important;
  color: var(--color-text-main) !important;
}

:global(.dark-theme) .finanzas-view .subtab-pill {
  background-color: var(--color-bg-surface);
  border-color: var(--color-border);
  color: var(--color-text-muted);
}

:global(.dark-theme) .finanzas-view .subtab-pill.active {
  background-color: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

:global(.dark-theme) .finanzas-view .filter-pill {
  background-color: var(--color-bg-surface);
  border-color: var(--color-border);
  color: var(--color-text-muted);
}

:global(.dark-theme) .finanzas-view .filter-pill.active {
  background-color: var(--color-primary) !important;
  color: var(--color-primary-text) !important;
  border-color: var(--color-primary) !important;
}

:global(.dark-theme) .finanzas-view .mini-progress-bar,
:global(.dark-theme) .finanzas-view .funnel-progress-wrap {
  background-color: var(--color-bg-muted);
}
</style>

