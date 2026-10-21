<template>
  <div class="finanzas-view">
    <!-- 1. Hero Banner Card Warm Editorial Reutilizable -->
    <HeroBannerCard
      kicker="FINANZAS // GESTIÓN DE LIQUIDACIONES Y COBRANZAS"
      title="Finanzas - Liquidación Aduanera, Facturación y Cobranzas"
      description="Emisión y consolidación de balances aduaneros: tributos liquidados ante SUNAT, gastos de depósito temporal, flete local terrestre y honorarios profesionales de despacho. Control de cobranzas bancarias y detracciones SPOT."
    />

    <!-- 2. Pestañas Principales del Módulo -->
    <div class="module-tabs-nav">
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'liquidaciones' }"
        @click="switchTab('liquidaciones')"
      >
        <i class="pi pi-receipt"></i>
        <span>Liquidaciones Emitidas</span>
        <span class="tab-counter">{{ liquidacionesList.length }}</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'por-liquidar' }"
        @click="switchTab('por-liquidar')"
      >
        <i class="pi pi-clock"></i>
        <span>Expedientes por Liquidar</span>
        <span class="tab-counter">{{ expedientesPorLiquidar.length }}</span>
      </button>

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

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'anticipos' }"
        @click="switchTab('anticipos')"
      >
        <i class="pi pi-shield"></i>
        <span>Anticipos Pre-Expediente</span>
        <span class="tab-counter" :style="anticiposPendientesCount > 0 ? 'background: #ea580c; color: #fff; font-weight: 700;' : ''">{{ anticiposList.length }}</span>
      </button>
    </div>

    <!-- 3. Cuadrícula de Métricas KPI en Warm Editorial -->
    <div class="metrics-grid">
      <MetricCard
        index="01"
        label="Total Cobrado (S/)"
        :value="formatCurrencyPen(metricas.totalCobradoPen)"
        severity="success"
        highlight
      />
      <MetricCard
        index="02"
        label="Cuentas por Cobrar (S/)"
        :value="formatCurrencyPen(metricas.cuentasPorCobrarPen)"
        severity="warning"
      />
      <MetricCard
        index="03"
        label="Tributos Pagados SUNAT"
        :value="formatCurrencyPen(metricas.totalTributosSunatPen)"
        severity="accent"
      />
      <MetricCard
        index="04"
        label="Detracciones SPOT (12%)"
        :value="formatCurrencyPen(metricas.totalDetraccionesPen)"
        severity="pending"
      />
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 1: LIQUIDACIONES EMITIDAS & CONTROL DE COBRANZA                   -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'liquidaciones'" class="tab-container">
      <div class="tab-content-section">
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
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 2: EXPEDIENTES POR LIQUIDAR                                       -->
    <!-- ===================================================================== -->
    <div v-else-if="activeTab === 'por-liquidar'" class="tab-container">
      <div class="tab-content-section">
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
    <div v-else-if="activeTab === 'conciliacion'" class="tab-container">
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
    <!-- TAB 4: ANTICIPOS Y PAGOS PRE-EXPEDIENTE                               -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'anticipos'" class="tab-container">
      <div class="tab-content-section">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">CONTROL DE INGRESOS PRE-OPERATIVOS</span>
            <h3 class="section-title">Bandeja de Anticipos y Pagos de Cotizaciones</h3>
          </div>

          <div class="section-controls">
            <!-- Filtro de Estado -->
            <select v-model="filterEstadoAnticipo" class="filter-select">
              <option value="">Todos los Estados</option>
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

            <button type="button" class="btn-outline-warm" @click="loadAnticipos" :disabled="loadingAnticipos">
              <i :class="['pi pi-sync', { 'pi-spin': loadingAnticipos }]"></i>
              <span>Actualizar</span>
            </button>

            <button type="button" class="btn-primary-action" @click="openRegistrarPagoModal">
              <i class="pi pi-plus"></i>
              <span>+ Registrar Anticipo / Pago</span>
            </button>
          </div>
        </div>

        <!-- Tabla de Anticipos -->
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

    <!-- Modal: Registro de Pago / Anticipo de Cotizaciones (Finanzas) -->
    <RegistrarPagoModal
      :visible="showRegistrarPagoModal"
      @close="showRegistrarPagoModal = false"
      @saved="onPagoRegistradoEnFinanzas"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'

import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import RegistrarPagoModal from '../components/RegistrarPagoModal.vue'

import finanzasService from '../services/finanzasService'
import { expedienteService } from '../services/expedienteService'
import { useNavigationStore } from '@/core/stores/navigationStore'

const navigationStore = useNavigationStore()

// Pestañas
const activeTab = ref('liquidaciones') // 'liquidaciones' | 'por-liquidar' | 'conciliacion'

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
const showRegistrarPagoModal = ref(false)

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
  await Promise.all([loadLiquidaciones(), loadMetricas(), loadExpedientes(), loadAnticipos()])
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

function switchTab(tab) {
  activeTab.value = tab
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
    alert('Error al emitir liquidación: ' + (err.response?.data?.message || err.message))
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
    alert('Error al registrar cobro: ' + (err.response?.data?.message || err.message))
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
  if (!confirm(`¿Está seguro de anular la liquidación ${liquidacion.codigoLiquidacion}?`)) return
  try {
    await finanzasService.anularLiquidacion(liquidacion.idLiquidacion)
    await loadAllData()
  } catch (err) {
    console.error('Error al anular liquidación:', err)
    alert('No se pudo anular la liquidación.')
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
    await loadAnticipos()
  } catch (err) {
    console.error('Error al validar anticipo:', err)
    alert(err.response?.data?.message || 'Error al validar el pago bancario.')
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

function openRegistrarPagoModal() {
  showRegistrarPagoModal.value = true
}

async function onPagoRegistradoEnFinanzas() {
  await loadAnticipos()
}

// Escuchar cambios de subopción desde el menú lateral si aplica
watch(() => navigationStore.finanzasSubOption, (val) => {
  if (val === 'facturacion') {
    activeTab.value = 'conciliacion'
  } else if (val === 'anticipos') {
    activeTab.value = 'anticipos'
  } else {
    activeTab.value = 'liquidaciones'
  }
})

// Escuchar disparadores de acciones desde el Sidebar
watch(
  () => [navigationStore.finanzasActionTrigger, navigationStore.navTick],
  ([act]) => {
    if (act === 'registrar-anticipo') {
      activeTab.value = 'anticipos'
      openRegistrarPagoModal()
      setTimeout(() => {
        if (navigationStore.finanzasActionTrigger === 'registrar-anticipo') {
          navigationStore.finanzasActionTrigger = null
        }
      }, 50)
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
  gap: 1.5rem;
  width: 100%;
}

/* Pestañas de Navegación */
.module-tabs-nav {
  display: flex;
  gap: 0.75rem;
  margin: 1.5rem 0 1rem;
  border-bottom: 2px solid var(--color-border-warm, #d8cdbf);
  padding-bottom: 0.5rem;
}

.tab-nav-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  border: none;
  background: transparent;
  color: var(--color-text-muted, #7c7267);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tab-nav-btn:hover {
  background: var(--color-bg-card, #ede6db);
  color: var(--color-primary, #352f2a);
}

.tab-nav-btn.active {
  background: var(--color-primary, #352f2a);
  color: #fff;
}

.tab-counter {
  font-size: 0.75rem;
  padding: 0.15rem 0.5rem;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.08);
}

.tab-nav-btn.active .tab-counter {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* Métricas Grid */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

/* Secciones de Contenido */
.tab-content-section {
  background: #fff;
  border: 1px solid var(--color-border-warm, #d8cdbf);
  border-radius: 8px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.section-header-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.25rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--color-border-warm, #ede6db);
}

.badge-kicker {
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-accent, #9a7f60);
  display: block;
}

.section-title {
  margin: 0.15rem 0 0;
  font-size: 1.25rem;
  color: var(--color-primary, #352f2a);
  font-weight: 700;
}

.section-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
}

.filter-select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border-warm, #d8cdbf);
  border-radius: 6px;
  background: #fff;
  font-size: 0.85rem;
  color: var(--color-primary, #352f2a);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: #8c827a;
  font-size: 0.85rem;
}

.search-input {
  padding: 0.5rem 0.75rem 0.5rem 2.2rem;
  border: 1px solid var(--color-border-warm, #d8cdbf);
  border-radius: 6px;
  font-size: 0.85rem;
  width: 240px;
}

/* Botones */
.btn-primary-action {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-primary, #352f2a);
  color: #fff;
  border: none;
  padding: 0.55rem 1rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary-action:hover {
  background: #1f1b18;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: transparent;
  color: var(--color-primary, #352f2a);
  border: 1px solid var(--color-border-warm, #d8cdbf);
  padding: 0.55rem 0.9rem;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-outline-warm:hover {
  background: var(--color-bg-card, #ede6db);
}

/* Tabla Celdas */
.ticket-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.25rem 0.5rem;
  background: var(--color-bg-card, #ede6db);
  border-radius: 4px;
  font-size: 0.85rem;
  color: var(--color-primary, #352f2a);
}

.client-cell {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--color-primary, #352f2a);
}

.client-sub {
  font-size: 0.75rem;
  color: #7c7267;
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
  color: var(--color-accent, #9a7f60);
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
  gap: 0.3rem;
  background: transparent;
  color: var(--color-primary, #352f2a);
  border: 1px solid var(--color-border-warm, #d8cdbf);
  padding: 0.35rem 0.6rem;
  border-radius: 4px;
  font-size: 0.78rem;
  cursor: pointer;
}

.btn-action-outline:hover {
  background: var(--color-bg-card, #ede6db);
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

.form-input, .form-select, .form-textarea {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border-warm, #d8cdbf);
  border-radius: 6px;
  font-size: 0.88rem;
  background: #fff;
  color: var(--color-primary, #352f2a);
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
</style>
