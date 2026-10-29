<template>
  <div class="ventas-view">

    <!-- Vista Completa: Corrección Unificada de Cotización Observada -->
    <CotizacionCorrectionView
      v-if="showCorrectionView"
      :cotizacion="selectedCotizacion"
      :clientes="clientes"
      @cancel="closeAllForms"
      @saved="handleCotizacionSaved"
    />

    <!-- Vista Completa: Formulario Maestro de Cotización Comercial (en Pasos) -->
    <CotizacionRegistrationForm
      v-else-if="showCotizacionForm"
      :cotizacion-to-edit="selectedCotizacion"
      :clientes="clientes"
      @cancel="closeAllForms"
      @saved="handleCotizacionSaved"
      @client-created="handleQuickClientCreated"
    />

    <!-- Vista Completa: Formulario Maestro de Cliente / Prospecto -->
    <ClienteRegistrationForm
      v-else-if="showClienteForm"
      :cliente-to-edit="selectedCliente"
      @cancel="closeAllForms"
      @saved="handleClienteSaved"
    />

    <!-- Vista Principal: Tablero Comercial y Listados -->
    <div v-else class="ventas-master-container">

      <!-- ======================================================== -->
      <!-- SUB-OPCIÓN 01: DASHBOARD COMERCIAL DE VENTAS             -->
      <!-- ======================================================== -->
      <section v-if="activeTab === 'dashboard'" class="dashboard-comercial-section fade-in">
        <!-- 1. Barra de Atajos Rápidos Comerciales -->
        <div class="dashboard-action-toolbar">
          <div class="toolbar-left-info">
            <span class="badge-kicker">OPERATORIA & ATAJOS RÁPIDOS</span>
            <h3 class="toolbar-title">Panel de Acciones Comerciales</h3>
          </div>
          <div class="toolbar-actions-group">
            <button type="button" class="btn-espresso" @click="openCreateCotizacion">
              <i class="pi pi-plus"></i>
              <span>+ Nueva Cotización</span>
            </button>
            <button type="button" class="btn-outline-warm" @click="openCreateCliente">
              <i class="pi pi-user-plus"></i>
              <span>+ Nuevo Cliente</span>
            </button>
            <button type="button" class="btn-outline-warm" @click="navigateTo('cotizaciones')">
              <i class="pi pi-file-edit"></i>
              <span>Bandeja Cotizaciones</span>
            </button>
            <button type="button" class="btn-outline-warm" @click="navigateTo('clientes')">
              <i class="pi pi-building"></i>
              <span>Directorio Clientes</span>
            </button>
            <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading" title="Actualizar datos">
              <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
              <span>Refrescar</span>
            </button>
          </div>
        </div>

        <!-- 2. Grid de Métricas Comerciales Avanzadas -->
        <div class="metrics-commercial-grid">
          <!-- Tarjeta 1: Total Cotizaciones -->
          <div class="commercial-kpi-card" @click="navigateTo('cotizaciones')">
            <div class="kpi-card-header">
              <span class="kpi-num">01</span>
              <span class="kpi-label">Cotizaciones Emitidas</span>
              <i class="pi pi-file-edit kpi-icon"></i>
            </div>
            <div class="kpi-main-stat">
              <span class="kpi-value">{{ cotizaciones.length }}</span>
              <span class="kpi-badge font-mono">${{ Number(totalMontoCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) }} USD</span>
            </div>
            <div class="kpi-footer-note">
              <span>Volumen acumulado cotizado</span>
            </div>
          </div>

          <!-- Tarjeta 2: Propuestas Aprobadas -->
          <div class="commercial-kpi-card success-card" @click="navigateTo('cotizaciones')">
            <div class="kpi-card-header">
              <span class="kpi-num">02</span>
              <span class="kpi-label">Propuestas Aprobadas</span>
              <i class="pi pi-check-circle kpi-icon success"></i>
            </div>
            <div class="kpi-main-stat">
              <span class="kpi-value text-success">{{ aprobadasCount }}</span>
              <span class="kpi-badge success">{{ conversionRate }}% Conversión</span>
            </div>
            <div class="kpi-footer-note">
              <span>Abonos: <strong class="font-mono text-success">${{ Number(totalAbonadoConfirmado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong></span>
            </div>
          </div>

          <!-- Tarjeta 3: En Negociación / Pendientes -->
          <div class="commercial-kpi-card pending-card" @click="navigateTo('cotizaciones')">
            <div class="kpi-card-header">
              <span class="kpi-num">03</span>
              <span class="kpi-label">Propuestas Pendientes</span>
              <i class="pi pi-clock kpi-icon warning"></i>
            </div>
            <div class="kpi-main-stat">
              <span class="kpi-value text-warning">{{ pendientesCount }}</span>
              <span class="kpi-badge warning">En Evaluación</span>
            </div>
            <div class="kpi-footer-note">
              <span>Pendientes de confirmación</span>
            </div>
          </div>

          <!-- Tarjeta 4: Observadas por Supervisión -->
          <div class="commercial-kpi-card alert-card" @click="navigateTo('cotizaciones')">
            <div class="kpi-card-header">
              <span class="kpi-num">04</span>
              <span class="kpi-label">Propuestas Observadas</span>
              <i class="pi pi-exclamation-triangle kpi-icon danger"></i>
            </div>
            <div class="kpi-main-stat">
              <span class="kpi-value text-danger">{{ observadasCount }}</span>
              <span class="kpi-badge danger">Requiere Ajuste</span>
            </div>
            <div class="kpi-footer-note">
              <span>Para corrección inmediata</span>
            </div>
          </div>

          <!-- Tarjeta 5: Clientes y Prospectos -->
          <div class="commercial-kpi-card" @click="navigateTo('clientes')">
            <div class="kpi-card-header">
              <span class="kpi-num">05</span>
              <span class="kpi-label">Cartera de Clientes</span>
              <i class="pi pi-users kpi-icon"></i>
            </div>
            <div class="kpi-main-stat">
              <span class="kpi-value">{{ clientes.length }}</span>
              <span class="kpi-badge">{{ clientesEfectivosCount }} Efectivos</span>
            </div>
            <div class="kpi-footer-note">
              <span>{{ prospectosCount }} prospectos calificados</span>
            </div>
          </div>

          <!-- Tarjeta 6: Expedientes Formalizados -->
          <div class="commercial-kpi-card" @click="navigateTo('ordenes')">
            <div class="kpi-card-header">
              <span class="kpi-num">06</span>
              <span class="kpi-label">Expedientes Operativos</span>
              <i class="pi pi-folder-open kpi-icon"></i>
            </div>
            <div class="kpi-main-stat">
              <span class="kpi-value">{{ expedientesList.length }}</span>
              <span class="kpi-badge info">En Seguimiento</span>
            </div>
            <div class="kpi-footer-note">
              <span>Trámites aduaneros activos</span>
            </div>
          </div>
        </div>

        <!-- 3. Paneles Analíticos en 2 Columnas -->
        <div class="dashboard-analytics-row">
          <!-- Columna A: Embudo y Desglose de Propuestas -->
          <div class="analytics-card">
            <div class="card-title-row">
              <div>
                <span class="badge-kicker">PIPELINE COMERCIAL</span>
                <h4 class="card-main-title">Embudo de Conversión de Cotizaciones</h4>
              </div>
              <span class="analytics-stat-pill">Ticket Promedio: <strong class="font-mono">${{ Number(ticketPromedio || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</strong></span>
            </div>

            <div class="pipeline-progress-bar">
              <div
                class="pipeline-segment approved"
                :style="{ width: (cotizaciones.length ? (aprobadasCount / cotizaciones.length) * 100 : 0) + '%' }"
                :title="`Aprobadas: ${aprobadasCount}`"
              ></div>
              <div
                class="pipeline-segment pending"
                :style="{ width: (cotizaciones.length ? (pendientesCount / cotizaciones.length) * 100 : 0) + '%' }"
                :title="`Pendientes: ${pendientesCount}`"
              ></div>
              <div
                class="pipeline-segment observed"
                :style="{ width: (cotizaciones.length ? (observadasCount / cotizaciones.length) * 100 : 0) + '%' }"
                :title="`Observadas: ${observadasCount}`"
              ></div>
              <div
                class="pipeline-segment rejected"
                :style="{ width: (cotizaciones.length ? (rechazadasCount / cotizaciones.length) * 100 : 0) + '%' }"
                :title="`Rechazadas / Anuladas: ${rechazadasCount}`"
              ></div>
            </div>

            <div class="pipeline-legend">
              <div class="legend-item">
                <span class="legend-dot approved"></span>
                <span class="legend-label">Aprobadas</span>
                <strong class="legend-count">{{ aprobadasCount }}</strong>
              </div>
              <div class="legend-item">
                <span class="legend-dot pending"></span>
                <span class="legend-label">Pendientes</span>
                <strong class="legend-count">{{ pendientesCount }}</strong>
              </div>
              <div class="legend-item">
                <span class="legend-dot observed"></span>
                <span class="legend-label">Observadas</span>
                <strong class="legend-count">{{ observadasCount }}</strong>
              </div>
              <div class="legend-item">
                <span class="legend-dot rejected"></span>
                <span class="legend-label">Rechazadas / Anuladas</span>
                <strong class="legend-count">{{ rechazadasCount }}</strong>
              </div>
            </div>
          </div>

          <!-- Columna B: Desglose por Régimen Aduanero -->
          <div class="analytics-card">
            <div class="card-title-row">
              <div>
                <span class="badge-kicker">DISTRIBUCIÓN OPERATIVA</span>
                <h4 class="card-main-title">Propuestas por Régimen Aduanero</h4>
              </div>
              <span class="analytics-stat-pill">{{ regimenBreakdown.length }} Regímenes</span>
            </div>

            <div class="regimen-list">
              <div v-for="r in regimenBreakdown" :key="r.regimen" class="regimen-row">
                <div class="regimen-info">
                  <span class="regimen-name">{{ r.label }}</span>
                  <span class="regimen-count">{{ r.count }} propuesta{{ r.count > 1 ? 's' : '' }}</span>
                </div>
                <div class="regimen-total font-mono">
                  ${{ Number(r.total || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                </div>
              </div>
              <div v-if="!regimenBreakdown.length" class="empty-mini">
                <span>Sin cotizaciones registradas para desglosar</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 4. Actividad Comercial Reciente (Últimas Cotizaciones) -->
        <div class="recent-commercial-card">
          <div class="card-title-row">
            <div>
              <span class="badge-kicker">HISTORIAL RECIENTE</span>
              <h4 class="card-main-title">Últimas Cotizaciones Comerciales Emitidas</h4>
            </div>
            <button type="button" class="btn-outline-warm btn-sm" @click="navigateTo('cotizaciones')">
              <span>Ver todas las cotizaciones ({{ cotizaciones.length }})</span>
              <i class="pi pi-arrow-right"></i>
            </button>
          </div>

          <div class="recent-table-wrapper">
            <table class="compact-commercial-table">
              <thead>
                <tr>
                  <th>N° Cotización</th>
                  <th>Cliente / Prospecto</th>
                  <th>Régimen Aduanero</th>
                  <th>Total Cotizado</th>
                  <th>Fecha Emisión</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in ultimasCotizaciones" :key="c.idCotizacion">
                  <td>
                    <span class="doc-code">{{ c.codigoCotizacion }}</span>
                  </td>
                  <td>
                    <div class="client-cell">
                      <span class="client-name">{{ c.clienteRazonSocial || 'Cliente N/A' }}</span>
                      <span class="client-sub">{{ c.clienteNumeroDocumento }}</span>
                    </div>
                  </td>
                  <td>
                    <span class="regimen-tag">{{ formatRegimen(c.regimenAduanero) }}</span>
                  </td>
                  <td>
                    <span class="total-amount font-mono">${{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
                  </td>
                  <td>
                    <span class="date-text">{{ c.fechaEmision }}</span>
                  </td>
                  <td>
                    <Tag
                      :value="getCotizacionLabel(c.estado)"
                      :severity="getCotizacionSeverity(c.estado)"
                      rounded
                    />
                  </td>
                  <td>
                    <div class="actions-wrapper">
                      <button
                        type="button"
                        class="action-btn-outline"
                        title="Ver / Adjuntar Documentos"
                        @click="openDocumentosCotizacion(c)"
                      >
                        <i class="pi pi-paperclip"></i>
                        <span>Docs</span>
                      </button>
                      <button
                        type="button"
                        class="action-btn-outline"
                        title="Editar cotización"
                        @click="openEditCotizacion(c)"
                      >
                        Editar
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!ultimasCotizaciones.length">
                  <td colspan="7" class="text-center text-muted" style="padding: 2rem;">
                    No hay cotizaciones registradas. Utilice el botón "+ Nueva Cotización" para emitir su primera oferta comercial.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- ======================================================== -->
      <!-- SUB-OPCIÓN 02: REGISTRO DE COTIZACIONES COMERCIALES      -->
      <!-- ======================================================== -->
      <!-- ======================================================== -->
      <!-- SUB-OPCIÓN 02: REGISTRO DE COTIZACIONES COMERCIALES      -->
      <!-- ======================================================== -->
      <div v-if="activeTab === 'cotizaciones'" class="tab-content-section fade-in">
        <div class="section-header-row">
          <div>
            <span class="badge-kicker">PROPUESTAS ADUANERAS</span>
            <h3 class="section-title">Registro de Cotizaciones Comerciales</h3>
          </div>

          <div class="section-controls">
            <button type="button" class="btn-espresso" @click="openCreateCotizacion">
              <i class="pi pi-plus"></i>
              <span>+ Nueva Cotización</span>
            </button>
          </div>
        </div>

        <!-- Controles de Tabla: Buscar, Filtros y Ordenar por -->
        <div class="table-controls-bar">
          <div class="controls-left">
            <!-- Campo de Búsqueda -->
            <div class="search-box">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model.trim="searchCotizacion"
                type="text"
                placeholder="Buscar por N° cotización, cliente o RUC..."
                class="search-input"
              />
              <button
                v-if="searchCotizacion"
                type="button"
                class="clear-search-btn"
                @click="searchCotizacion = ''"
                title="Limpiar búsqueda"
              >
                <i class="pi pi-times"></i>
              </button>
            </div>

            <!-- Filtro Régimen Aduanero -->
            <div class="filter-dropdown">
              <select v-model="filterRegimenCotizacion" class="filter-select" title="Filtrar por régimen aduanero">
                <option value="ALL">Todos los Regímenes</option>
                <option value="IMPORTACION_DEFINITIVA">Importación (REG-10)</option>
                <option value="EXPORTACION_DEFINITIVA">Exportación (REG-40)</option>
              </select>
            </div>

            <!-- Filtro Estado Cotización -->
            <div class="filter-dropdown">
              <select v-model="filterEstadoCotizacion" class="filter-select" title="Filtrar por estado">
                <option value="">Todos los Estados</option>
                <option value="PENDIENTE">Pendientes de Revisión</option>
                <option value="OBSERVADA">Observadas (Para Corrección)</option>
                <option value="APROBADA">Aprobadas</option>
                <option value="RECHAZADA">Rechazadas</option>
                <option value="ANULADA">Anuladas</option>
              </select>
            </div>

            <!-- Ordenar Por -->
            <div class="sort-dropdown">
              <select v-model="sortByCotizacion" class="filter-select sort-select" title="Ordenar resultados">
                <option value="fecha-desc">Ordenar por: Fecha (Más recientes)</option>
                <option value="fecha-asc">Ordenar por: Fecha (Más antiguas)</option>
                <option value="monto-desc">Ordenar por: Monto (Mayor a menor)</option>
                <option value="monto-asc">Ordenar por: Monto (Menor a mayor)</option>
                <option value="codigo-asc">Ordenar por: N° Cotización (A - Z)</option>
                <option value="cliente-asc">Ordenar por: Cliente (A - Z)</option>
              </select>
            </div>
          </div>

          <div class="controls-right">
            <button
              v-if="hasActiveCotizacionFilters"
              type="button"
              class="btn-clear-filters"
              @click="resetCotizacionFilters"
              title="Restablecer filtros"
            >
              <i class="pi pi-filter-slash"></i>
              <span>Limpiar Filtros</span>
            </button>

            <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading" title="Actualizar datos">
              <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
              <span>Actualizar</span>
            </button>
          </div>
        </div>

        <!-- Tabla de Cotizaciones con PrimeVue DataTable -->
        <div class="table-container">
          <DataTable
            :value="filteredCotizaciones"
            :loading="loading"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idCotizacion"
            paginator
            :rows="15"
            :rowsPerPageOptions="[15, 30, 50]"
            currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} cotizaciones"
            paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
          >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-file-edit empty-icon"></i>
              <p class="empty-title">No hay cotizaciones registradas</p>
              <p class="empty-desc">Haga clic en "+ Nueva Cotización" para emitir una nueva oferta comercial.</p>
            </div>
          </template>
          <template #loading>
            <div class="loading-state">
              <i class="pi pi-spin pi-spinner"></i>
              <span>Cargando cotizaciones comerciales...</span>
            </div>
          </template>

          <Column header="N° Cotización">
            <template #body="{ data: c }">
              <span class="doc-code">{{ c.codigoCotizacion }}</span>
            </template>
          </Column>

          <Column header="Cliente / Prospecto">
            <template #body="{ data: c }">
              <div class="client-cell">
                <span class="client-name">{{ c.clienteRazonSocial || 'Cliente N/A' }}</span>
                <div class="client-sub-meta">
                  <span class="client-sub">{{ c.clienteNumeroDocumento }}</span>
                  <span v-if="c.clienteTieneUsuario" class="portal-badge origin-badge origin-portal" title="Empresa formalizada con usuario activo en Portal B2B">
                    <i class="pi pi-id-card"></i> Portal B2B
                  </span>
                  <span v-else class="presencial-badge origin-badge origin-presencial" title="Nuevo prospecto / Sin cuenta B2B (Atención y subsanación presencial con el vendedor)">
                    <i class="pi pi-user"></i> Presencial
                  </span>
                </div>
              </div>
            </template>
          </Column>

          <Column header="Régimen Aduanero">
            <template #body="{ data: c }">
              <span class="regimen-tag">{{ c.regimenAduanero }}</span>
            </template>
          </Column>

          <Column header="Total Cotizado">
            <template #body="{ data: c }">
              <span class="total-amount">${{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
            </template>
          </Column>

          <Column header="Fecha Emisión">
            <template #body="{ data: c }">
              <span class="date-text">{{ c.fechaEmision }}</span>
            </template>
          </Column>

          <Column header="Estado">
            <template #body="{ data: c }">
              <div class="status-cell">
                <Tag
                  :value="getCotizacionLabel(c.estado)"
                  :severity="getCotizacionSeverity(c.estado)"
                  rounded
                />
                <button
                  v-if="(c.estado === 'REVISION' || c.estado === 'OBSERVADA') && c.motivoObservacion"
                  type="button"
                  class="obs-warning-pill"
                  @click="openViewObservation(c)"
                  title="Ver observación de Supervisión Aduanera"
                >
                  <i class="pi pi-exclamation-triangle"></i>
                  <span>Ver motivo</span>
                </button>
              </div>
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
                <span v-if="c.totalAbonado > 0" class="client-sub" style="color: #15803d; font-weight: 600;">
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
              <span v-if="c.tieneExpediente" class="os-linked-tag">
                <i class="pi pi-folder"></i> {{ c.codigoExpediente }}
              </span>
              <span v-else-if="c.estado === 'APROBADA' && c.tienePagoAprobado" class="text-muted-dash" style="color: #15803d; font-weight: 600;">
                Aprobada (Pago Validado)
              </span>
              <span v-else-if="c.estado === 'APROBADA'" class="text-muted-dash" style="color: #b45309;">
                Aprobada (Requiere Pago Finanzas)
              </span>
              <span v-else class="text-muted-dash">—</span>
            </template>
          </Column>

          <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: c }">
              <div class="actions-wrapper">
                <button
                  type="button"
                  class="action-btn-outline"
                  title="Ver / Adjuntar Documentos de la Cotización"
                  @click="openDocumentosCotizacion(c)"
                >
                  <i class="pi pi-paperclip"></i>
                  <span>Docs</span>
                </button>


                <button
                  v-if="(c.estado === 'REVISION' || c.estado === 'OBSERVADA') && !c.clienteTieneUsuario && (c.clienteTelefono || getClienteTelefono(c.idCliente))"
                  type="button"
                  class="action-btn-outline btn-whatsapp-action btn-whatsapp"
                  title="Notificar observaciones por WhatsApp (Atención Presencial/Directa)"
                  @click="notifyObservationViaWhatsApp(c)"
                >
                  <i class="pi pi-whatsapp"></i>
                </button>

                <button
                  v-if="c.estado === 'REVISION' || c.estado === 'OBSERVADA'"
                  type="button"
                  class="action-btn-outline observe-fix"
                  title="Corregir observaciones y enviar nuevamente a Supervisión"
                  @click="openCorrectionCotizacion(c)"
                >
                  <i class="pi pi-file-edit"></i>
                  <span>Corregir</span>
                </button>
                <button
                  v-else
                  type="button"
                  class="action-btn-outline"
                  title="Editar cotización"
                  @click="openEditCotizacion(c)"
                >
                  Editar
                </button>
                <button
                  v-if="!c.tieneExpediente && c.estado !== 'APROBADA'"
                  type="button"
                  class="action-btn-outline btn-danger-action"
                  title="Eliminar cotización"
                  @click="handleDeleteCotizacion(c)"
                >
                  Eliminar
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- TAB 2: CLIENTES & PROSPECTOS -->
    <div v-if="activeTab === 'clientes'" class="tab-content-section">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">CARTERA COMERCIAL</span>
          <h3 class="section-title">Directorio de Clientes & Prospectos</h3>
        </div>

        <div class="section-controls">
          <button type="button" class="btn-espresso" @click="openCreateCliente">
            <i class="pi pi-plus"></i>
            <span>+ Nuevo Cliente</span>
          </button>
        </div>
      </div>

      <!-- Controles de Tabla: Buscar, Filtros y Ordenar por -->
      <div class="table-controls-bar">
        <div class="controls-left">
          <!-- Campo de Búsqueda -->
          <div class="search-box">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model.trim="searchCliente"
              type="text"
              placeholder="Buscar por RUC/DNI, razón social o contacto..."
              class="search-input"
            />
            <button
              v-if="searchCliente"
              type="button"
              class="clear-search-btn"
              @click="searchCliente = ''"
              title="Limpiar búsqueda"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>

          <!-- Filtro Tipo de Cliente -->
          <div class="filter-dropdown">
            <select v-model="filterTipoCliente" class="filter-select" title="Filtrar por clasificación">
              <option value="ALL">Todos (Clientes y Prospectos)</option>
              <option value="PROSPECTO">Solo Prospectos</option>
              <option value="CLIENTE">Clientes Efectivos</option>
            </select>
          </div>

          <!-- Filtro Estado Cliente -->
          <div class="filter-dropdown">
            <select v-model="filterEstadoCliente" class="filter-select" title="Filtrar por estado">
              <option value="ALL">Todos los Estados</option>
              <option value="ACTIVO">Solo Activos</option>
              <option value="INACTIVO">Solo Inactivos</option>
            </select>
          </div>

          <!-- Ordenar Por -->
          <div class="sort-dropdown">
            <select v-model="sortByCliente" class="filter-select sort-select" title="Ordenar clientes">
              <option value="nombre-asc">Ordenar por: Razón Social (A - Z)</option>
              <option value="nombre-desc">Ordenar por: Razón Social (Z - A)</option>
              <option value="doc-asc">Ordenar por: Documento (A - Z)</option>
              <option value="tipo-asc">Ordenar por: Clasificación (Prospectos primero)</option>
            </select>
          </div>
        </div>

        <div class="controls-right">
          <button
            v-if="hasActiveClienteFilters"
            type="button"
            class="btn-clear-filters"
            @click="resetClienteFilters"
            title="Restablecer filtros"
          >
            <i class="pi pi-filter-slash"></i>
            <span>Limpiar Filtros</span>
          </button>

          <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading" title="Actualizar datos">
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>
        </div>
      </div>

      <!-- Tabla de Clientes con PrimeVue DataTable -->
      <div class="table-container">
        <DataTable
          :value="filteredClientes"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idCliente"
          paginator
          :rows="15"
          :rowsPerPageOptions="[15, 30, 50]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} clientes"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-building empty-icon"></i>
              <p class="empty-title">No hay clientes encontrados</p>
              <p class="empty-desc">Registre su primer cliente o prospecto para empezar a cotizar.</p>
            </div>
          </template>

          <Column header="Documento">
            <template #body="{ data: cl }">
              <div class="doc-badge">
                <span class="doc-type">{{ cl.tipoDocumento }}</span>
                <span class="doc-number">{{ cl.numeroDocumento }}</span>
              </div>
            </template>
          </Column>

          <Column header="Razón Social / Contacto">
            <template #body="{ data: cl }">
              <div class="client-cell">
                <span class="client-name">{{ cl.razonSocial }}</span>
                <div class="client-sub-meta">
                  <span class="client-sub" v-if="cl.contactoRepresentante">Rep: {{ cl.contactoRepresentante }}</span>
                  <span v-if="clienteTieneUsuarioPortal(cl)" class="portal-badge origin-badge origin-portal" title="Empresa formalizada con usuario activo en Portal B2B">
                    <i class="pi pi-id-card"></i> Portal B2B
                  </span>
                  <span v-else class="presencial-badge origin-badge origin-presencial" title="Nuevo prospecto / Sin cuenta B2B (Atención presencial con el vendedor)">
                    <i class="pi pi-user"></i> Presencial
                  </span>
                </div>
              </div>
            </template>
          </Column>

          <Column header="Correo Institucional">
            <template #body="{ data: cl }">
              <span class="contact-text">{{ cl.correoContacto }}</span>
            </template>
          </Column>

          <Column header="Teléfono">
            <template #body="{ data: cl }">
              <span class="contact-text">{{ cl.telefonoContacto || '—' }}</span>
            </template>
          </Column>

          <Column header="Dirección Fiscal">
            <template #body="{ data: cl }">
              <span class="address-text">{{ cl.direccionFiscal || '—' }}</span>
            </template>
          </Column>

          <Column header="Clasificación">
            <template #body="{ data: cl }">
              <Tag
                :value="cl.esProspecto ? 'Prospecto' : 'Cliente Efectivo'"
                :severity="cl.esProspecto ? 'warn' : 'success'"
                rounded
                :title="cl.esProspecto ? 'Se promoverá automáticamente a Cliente Efectivo al aprobarse una cotización' : 'Cuenta formalizada con cotizaciones aprobadas'"
              />
            </template>
          </Column>

          <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
            <template #body="{ data: cl }">
              <div class="actions-wrapper">
                <button type="button" class="action-btn-outline" @click="openEditCliente(cl)">
                  Editar
                </button>
                <button type="button" class="action-btn-outline btn-danger-action" @click="handleDeleteCliente(cl)">
                  Eliminar
                </button>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- TAB 3: EXPEDIENTES ADUANEROS (SOLO LECTURA) -->
    <div v-if="activeTab === 'ordenes'" class="tab-content-section">
      <div class="section-header-row">
        <div>
          <span class="badge-kicker">SEGUIMIENTO OPERATIVO // VISIBILIDAD GLOBAL</span>
          <h3 class="section-title">Expedientes Aduaneros (Modo Consulta)</h3>
        </div>
      </div>

      <!-- Controles de Tabla: Buscar, Filtros y Ordenar por -->
      <div class="table-controls-bar">
        <div class="controls-left">
          <!-- Campo de Búsqueda -->
          <div class="search-box">
            <i class="pi pi-search search-icon"></i>
            <input
              v-model.trim="searchExpediente"
              type="text"
              placeholder="Buscar por expediente, DAM, cotización o cliente..."
              class="search-input"
            />
            <button
              v-if="searchExpediente"
              type="button"
              class="clear-search-btn"
              @click="searchExpediente = ''"
              title="Limpiar búsqueda"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>

          <!-- Filtro Estado Expediente -->
          <div class="filter-dropdown">
            <select v-model="filterEstadoExpediente" class="filter-select" title="Filtrar por estado del expediente">
              <option value="">Todos los Estados</option>
              <option value="ASIGNADO">Asignado</option>
              <option value="NUMERADO">Numerado</option>
              <option value="EN_TRAMITE">En Trámite</option>
              <option value="CON_CANAL">Con Canal SUNAT</option>
              <option value="LEVANTE_AUTORIZADO">Levante Autorizado</option>
              <option value="CONCLUIDO">Concluido</option>
              <option value="ANULADO">Anulado</option>
            </select>
          </div>

          <!-- Filtro Canal SUNAT -->
          <div class="filter-dropdown">
            <select v-model="filterCanalExpediente" class="filter-select" title="Filtrar por canal SUNAT">
              <option value="ALL">Todos los Canales</option>
              <option value="VERDE">Canal Verde</option>
              <option value="NARANJA">Canal Naranja</option>
              <option value="ROJO">Canal Rojo</option>
              <option value="PENDIENTE">Pendiente Asignación</option>
            </select>
          </div>

          <!-- Ordenar Por -->
          <div class="sort-dropdown">
            <select v-model="sortByExpediente" class="filter-select sort-select" title="Ordenar expedientes">
              <option value="fecha-desc">Ordenar por: Fecha (Recientes primero)</option>
              <option value="fecha-asc">Ordenar por: Fecha (Antiguos primero)</option>
              <option value="codigo-asc">Ordenar por: N° Expediente (A - Z)</option>
              <option value="cliente-asc">Ordenar por: Cliente (A - Z)</option>
            </select>
          </div>
        </div>

        <div class="controls-right">
          <button
            v-if="hasActiveExpedienteFilters"
            type="button"
            class="btn-clear-filters"
            @click="resetExpedienteFilters"
            title="Restablecer filtros"
          >
            <i class="pi pi-filter-slash"></i>
            <span>Limpiar Filtros</span>
          </button>

          <button type="button" class="btn-outline-warm" @click="loadData" :disabled="loading" title="Actualizar datos">
            <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
            <span>Actualizar</span>
          </button>
        </div>
      </div>

      <!-- Tabla de Expedientes Aduaneros con PrimeVue DataTable (Solo Vista) -->
      <div class="table-container">
        <DataTable
          :value="filteredExpedientes"
          :loading="loading"
          responsiveLayout="scroll"
          class="custom-datatable"
          dataKey="idExpediente"
          paginator
          :rows="15"
          :rowsPerPageOptions="[15, 30, 50]"
          currentPageReportTemplate="Mostrando {first} a {last} de {totalRecords} expedientes"
          paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        >
          <template #empty>
            <div class="empty-state">
              <i class="pi pi-folder-open empty-icon"></i>
              <p class="empty-title">No hay expedientes aduaneros registrados</p>
              <p class="empty-desc">Los expedientes aduaneros son formalizados por Supervisión Aduanera tras aprobarse la cotización y confirmarse el pago en Finanzas.</p>
            </div>
          </template>

          <Column header="N° Expediente">
            <template #body="{ data: exp }">
              <span class="doc-code-os">{{ exp.codigoExpediente }}</span>
            </template>
          </Column>

          <Column header="Cotización Origen">
            <template #body="{ data: exp }">
              <span class="doc-code">{{ exp.codigoCotizacion || '—' }}</span>
            </template>
          </Column>

          <Column header="Cliente / Importador">
            <template #body="{ data: exp }">
              <div class="client-cell">
                <span class="client-name">{{ exp.clienteRazonSocial || '—' }}</span>
                <span v-if="exp.clienteNumeroDocumento" class="client-sub">{{ exp.clienteNumeroDocumento }}</span>
              </div>
            </template>
          </Column>

          <Column header="Régimen Aduanero">
            <template #body="{ data: exp }">
              <span class="regimen-tag">{{ formatRegimen(exp.regimenAduanero) }}</span>
            </template>
          </Column>

          <Column header="Canal SUNAT">
            <template #body="{ data: exp }">
              <Tag
                :value="exp.canalSunat || 'PENDIENTE'"
                :severity="getCanalSeverity(exp.canalSunat)"
                rounded
              />
            </template>
          </Column>

          <Column header="N° DAM">
            <template #body="{ data: exp }">
              <span class="font-mono text-xs font-bold">{{ exp.numeroDam || exp.numeroDeclaracion || '—' }}</span>
            </template>
          </Column>

          <Column header="Fecha Apertura">
            <template #body="{ data: exp }">
              <span class="date-text">{{ formatDateTime(exp.fechaApertura) }}</span>
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
        </DataTable>
      </div>
    </div>

    <!-- Modal: Detalle de Observación de Supervisión Aduanera -->
    <Dialog
      v-model:visible="showObservationDialog"
      modal
      header="Observaciones de Supervisión Aduanera"
      :style="{ width: '500px', maxWidth: '95vw' }"
      class="custom-sga-dialog"
    >
      <div v-if="selectedObservationCotizacion" class="observation-view-content">
        <div class="obs-box-header">
          <span class="badge-kicker">EXPEDIENTE N° {{ selectedObservationCotizacion.codigoCotizacion }}</span>
          <strong class="obs-box-title">{{ selectedObservationCotizacion.clienteRazonSocial || 'Cliente Asignado' }}</strong>
          <div class="obs-client-status">
            <span v-if="selectedObservationCotizacion.clienteTieneUsuario" class="portal-badge origin-badge origin-portal">
              <i class="pi pi-id-card"></i> Cuenta B2B Activa — Notificación enviada al portal del cliente
            </span>
            <span v-else class="presencial-badge origin-badge origin-presencial">
              <i class="pi pi-user"></i> Prospecto sin cuenta B2B — Coordinación presencial o directa con el vendedor
            </span>
          </div>
        </div>
        <div class="obs-box-body">
          <i class="pi pi-exclamation-triangle obs-box-icon"></i>
          <div>
            <span class="obs-box-label">Indicaciones de Supervisión Aduanera:</span>
            <p class="obs-box-text">{{ selectedObservationCotizacion.motivoObservacion || 'Se requieren ajustes en la cotización antes de la aprobación.' }}</p>
          </div>
        </div>
        <p class="obs-box-hint">
          Subsanar los puntos observados haciendo clic en "Corregir Cotización". Al guardar los cambios, la propuesta volverá automáticamente a la bandeja de Supervisión Aduanera para su reevaluación.
        </p>
      </div>
      <template #footer>
        <div class="dialog-footer-actions">
          <button
            v-if="selectedObservationCotizacion && !selectedObservationCotizacion.clienteTieneUsuario && (selectedObservationCotizacion.clienteTelefono || getClienteTelefono(selectedObservationCotizacion.idCliente))"
            type="button"
            class="btn-whatsapp-dialog btn-whatsapp-modal"
            @click="notifyObservationViaWhatsApp(selectedObservationCotizacion)"
          >
            <i class="pi pi-whatsapp"></i>
            <span>Notificar WhatsApp</span>
          </button>
          <button type="button" class="btn-outline-warm" @click="showObservationDialog = false">
            <span>Cerrar</span>
          </button>
          <button
            type="button"
            class="btn-espresso"
            @click="openEditFromObservation"
          >
            <i class="pi pi-pencil"></i>
            <span>Corregir Cotización</span>
          </button>
        </div>
      </template>
    </Dialog>

    <!-- Modal: Gestión Integral de Documentos Adjuntos -->
    <DocumentosManagerModal
      :visible="showDocumentosModal"
      :entidad-tipo="'COTIZACION'"
      :id-entidad="selectedDocCotizacionId"
      :entidad-id="selectedDocCotizacionId"
      :codigo-entidad="selectedDocCotizacionCodigo"
      :titulo-entidad="selectedDocCotizacionTitulo"
      @close="showDocumentosModal = false"
    />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import { comercialService } from '../services/comercialService'
import { expedienteService } from '../services/expedienteService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import ClienteRegistrationForm from '../components/ClienteRegistrationForm.vue'
import CotizacionRegistrationForm from '../components/CotizacionRegistrationForm.vue'
import CotizacionCorrectionView from '../components/CotizacionCorrectionView.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()
const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction, confirmDelete } = useConfirmation()

const activeTab = ref('dashboard')
const loading = ref(false)

const showClienteForm = ref(false)
const selectedCliente = ref(null)

const showCotizacionForm = ref(false)
const showCorrectionView = ref(false)
const selectedCotizacion = ref(null)

const navigateTo = (subOption) => {
  closeAllForms()
  activeTab.value = subOption
  navigationStore.setComercialSubOption(subOption)
  if (route.query.tab !== subOption) {
    router.replace({ query: { ...route.query, tab: subOption } })
  }
}

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

const showObservationDialog = ref(false)
const selectedObservationCotizacion = ref(null)

// Modal de Gestión de Documentos
const showDocumentosModal = ref(false)
const selectedDocCotizacionId = ref(null)
const selectedDocCotizacionCodigo = ref('')
const selectedDocCotizacionTitulo = ref('')

const openDocumentosCotizacion = (cotizacion) => {
  if (!cotizacion) return
  selectedDocCotizacionId.value = cotizacion.idCotizacion
  selectedDocCotizacionCodigo.value = cotizacion.codigoCotizacion || `COT-${cotizacion.idCotizacion}`
  selectedDocCotizacionTitulo.value = `${cotizacion.codigoCotizacion || 'Cotización'} — ${cotizacion.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

const openViewObservation = (cotizacion) => {
  selectedObservationCotizacion.value = cotizacion
  showObservationDialog.value = true
}

const openEditFromObservation = () => {
  if (selectedObservationCotizacion.value) {
    const c = selectedObservationCotizacion.value
    showObservationDialog.value = false
    openCorrectionCotizacion(c)
  }
}

const clienteTieneUsuarioPortal = (cl) => {
  if (!cl) return false
  if (typeof cl.tieneUsuario === 'boolean') return cl.tieneUsuario
  return cotizaciones.value.some(c => c.idCliente === cl.idCliente && c.clienteTieneUsuario)
}

const getClienteTelefono = (idCliente) => {
  if (!idCliente || !clientes.value) return ''
  const cli = clientes.value.find(item => item.idCliente === idCliente)
  return cli?.telefonoContacto || ''
}

const notifyObservationViaWhatsApp = (cotizacion) => {
  const tel = cotizacion.clienteTelefono || getClienteTelefono(cotizacion.idCliente) || ''
  const digitsOnly = tel.replace(/[^0-9]/g, '')
  if (!digitsOnly) {
    showToast('El cliente no tiene registrado número de teléfono/celular de contacto.', 'warn')
    return
  }
  const phone = digitsOnly.startsWith('51') ? digitsOnly : `51${digitsOnly}`
  const reason = cotizacion.motivoObservacion || 'Se requieren ajustes documentarios y técnicos en la propuesta comercial.'
  const msg = `Hola *${cotizacion.clienteRazonSocial || 'Estimado Cliente'}*, le saluda TransAduanas SGA.\n\nLe informamos que su cotización aduanera *${cotizacion.codigoCotizacion}* ha sido revisada por Supervisión Técnica y presenta la siguiente observación:\n\n📌 *Motivo:* ${reason}\n\nPor favor facilítenos los datos o documentos solicitados para subsanar la cotización y continuar con su trámite.\n\nAtentamente,\n*Área Comercial - TransAduanas*`

  const url = `https://wa.me/${phone}?text=${encodeURIComponent(msg)}`
  window.open(url, '_blank')
}

// Cerrar todos los formularios de vista completa y volver a los listados
const closeAllForms = () => {
  showCorrectionView.value = false
  showCotizacionForm.value = false
  showClienteForm.value = false
  selectedCotizacion.value = null
  selectedCliente.value = null
}

// Sincronizar activeTab y resetear formularios con query tab de la URL
watch(
  () => route.query.tab,
  (newTab) => {
    if (newTab && ['dashboard', 'cotizaciones', 'clientes', 'ordenes'].includes(newTab)) {
      activeTab.value = newTab
      if (navigationStore.comercialSubOption !== newTab) {
        navigationStore.setComercialSubOption(newTab)
      }
      closeAllForms()
    }
  },
  { immediate: true }
)

// Sincronizar activeTab y resetear formularios con sub-opción de navegación Comercial y navTick
watch(
  () => [navigationStore.comercialSubOption, navigationStore.navTick],
  ([newSub]) => {
    if (newSub && ['dashboard', 'cotizaciones', 'clientes', 'ordenes'].includes(newSub)) {
      if (activeTab.value !== newSub) {
        activeTab.value = newSub
        closeAllForms()
      }
    }
  }
)

// Escuchar disparadores de acciones desde el Sidebar
watch(
  () => [navigationStore.comercialActionTrigger, navigationStore.navTick],
  ([action]) => {
    if (action === 'nueva-cotizacion') {
      openCreateCotizacion()
      setTimeout(() => {
        if (navigationStore.comercialActionTrigger === 'nueva-cotizacion') {
          navigationStore.comercialActionTrigger = null
        }
      }, 50)
    } else if (action === 'nuevo-cliente') {
      openCreateCliente()
      setTimeout(() => {
        if (navigationStore.comercialActionTrigger === 'nuevo-cliente') {
          navigationStore.comercialActionTrigger = null
        }
      }, 50)
    }
  }
)

const cotizaciones = ref([])
const clientes = ref([])
const expedientesList = ref([])

// Normalizador de texto para búsquedas (ignora tildes, mayúsculas y espacios extras)
const normalizeSearchText = (text) => {
  if (!text) return ''
  return text
    .toString()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
}

// Filtros y ordenación para Cotizaciones (Sub-opción 02)
const searchCotizacion = ref('')
const filterRegimenCotizacion = ref('ALL')
const filterEstadoCotizacion = ref('')
const sortByCotizacion = ref('fecha-desc')

// Filtros y ordenación para Clientes (Sub-opción 03)
const searchCliente = ref('')
const filterTipoCliente = ref('ALL')
const filterEstadoCliente = ref('ALL')
const sortByCliente = ref('nombre-asc')

// Filtros y ordenación para Expedientes (Sub-opción 04)
const searchExpediente = ref('')
const filterEstadoExpediente = ref('')
const filterCanalExpediente = ref('ALL')
const sortByExpediente = ref('fecha-desc')

// Banderas de filtros activos
const hasActiveCotizacionFilters = computed(() => {
  return (
    !!searchCotizacion.value ||
    filterRegimenCotizacion.value !== 'ALL' ||
    !!filterEstadoCotizacion.value ||
    sortByCotizacion.value !== 'fecha-desc'
  )
})

const hasActiveClienteFilters = computed(() => {
  return (
    !!searchCliente.value ||
    filterTipoCliente.value !== 'ALL' ||
    filterEstadoCliente.value !== 'ALL' ||
    sortByCliente.value !== 'nombre-asc'
  )
})

const hasActiveExpedienteFilters = computed(() => {
  return (
    !!searchExpediente.value ||
    !!filterEstadoExpediente.value ||
    filterCanalExpediente.value !== 'ALL' ||
    sortByExpediente.value !== 'fecha-desc'
  )
})

// Acciones de restablecimiento de filtros
const resetCotizacionFilters = () => {
  searchCotizacion.value = ''
  filterRegimenCotizacion.value = 'ALL'
  filterEstadoCotizacion.value = ''
  sortByCotizacion.value = 'fecha-desc'
}

const resetClienteFilters = () => {
  searchCliente.value = ''
  filterTipoCliente.value = 'ALL'
  filterEstadoCliente.value = 'ALL'
  sortByCliente.value = 'nombre-asc'
}

const resetExpedienteFilters = () => {
  searchExpediente.value = ''
  filterEstadoExpediente.value = ''
  filterCanalExpediente.value = 'ALL'
  sortByExpediente.value = 'fecha-desc'
}

const showToast = (message, type = 'success') => {
  if (type === 'error') {
    showError(message)
  } else if (type === 'info') {
    showInfo(message)
  } else {
    showSuccess(message)
  }
}

// Métricas Comerciales Avanzadas para el Dashboard
const totalMontoCotizado = computed(() => {
  return cotizaciones.value.reduce((acc, c) => acc + (Number(c.totalCotizado) || 0), 0)
})

const aprobadasCount = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'APROBADA').length
})

const pendientesCount = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'PENDIENTE' || c.estado === 'ENVIADA').length
})

const observadasCount = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'REVISION' || c.estado === 'OBSERVADA').length
})

const rechazadasCount = computed(() => {
  return cotizaciones.value.filter((c) => c.estado === 'RECHAZADA' || c.estado === 'ANULADA').length
})

const conversionRate = computed(() => {
  if (!cotizaciones.value.length) return '0.0'
  return ((aprobadasCount.value / cotizaciones.value.length) * 100).toFixed(1)
})

const totalAbonadoConfirmado = computed(() => {
  return cotizaciones.value.reduce((acc, c) => acc + (Number(c.totalAbonado) || 0), 0)
})

const ticketPromedio = computed(() => {
  if (!cotizaciones.value.length) return '0.00'
  return (totalMontoCotizado.value / cotizaciones.value.length).toFixed(2)
})

const clientesEfectivosCount = computed(() => {
  return clientes.value.filter((c) => !c.esProspecto).length
})

const prospectosCount = computed(() => {
  return clientes.value.filter((c) => c.esProspecto).length
})

const ultimasCotizaciones = computed(() => {
  return [...cotizaciones.value]
    .sort((a, b) => (b.idCotizacion || 0) - (a.idCotizacion || 0))
    .slice(0, 5)
})

const regimenBreakdown = computed(() => {
  const map = {}
  cotizaciones.value.forEach((c) => {
    const reg = c.regimenAduanero || 'OTROS'
    if (!map[reg]) {
      map[reg] = { count: 0, total: 0 }
    }
    map[reg].count++
    map[reg].total += Number(c.totalCotizado) || 0
  })
  return Object.entries(map).map(([key, data]) => ({
    regimen: key,
    label: key.replace(/_/g, ' '),
    count: data.count,
    total: data.total
  }))
})

const filteredCotizaciones = computed(() => {
  let list = cotizaciones.value.filter((c) => {
    // 1. Filtro Estado
    if (filterEstadoCotizacion.value) {
      if (filterEstadoCotizacion.value === 'ENVIADA' || filterEstadoCotizacion.value === 'PENDIENTE') {
        if (c.estado !== 'ENVIADA' && c.estado !== 'PENDIENTE') return false
      } else if (filterEstadoCotizacion.value === 'REVISION' || filterEstadoCotizacion.value === 'OBSERVADA') {
        if (c.estado !== 'REVISION' && c.estado !== 'OBSERVADA') return false
      } else if (c.estado !== filterEstadoCotizacion.value) {
        return false
      }
    }

    // 2. Filtro Régimen
    if (filterRegimenCotizacion.value !== 'ALL') {
      if (c.regimenAduanero !== filterRegimenCotizacion.value) return false
    }

    // 3. Búsqueda inteligente (ignora tildes, tokens)
    const q = normalizeSearchText(searchCotizacion.value)
    if (q) {
      const tokens = q.split(/\s+/).filter(Boolean)
      const corpus = normalizeSearchText(
        `${c.codigoCotizacion || ''} ${c.clienteRazonSocial || ''} ${c.clienteNumeroDocumento || ''} ${c.regimenAduanero || ''} ${c.estado || ''}`
      )
      if (!tokens.every((t) => corpus.includes(t))) return false
    }

    return true
  })

  // 4. Ordenar
  return list.sort((a, b) => {
    switch (sortByCotizacion.value) {
      case 'fecha-asc':
        return new Date(a.fechaEmision || 0) - new Date(b.fechaEmision || 0)
      case 'monto-desc':
        return (Number(b.totalCotizado) || 0) - (Number(a.totalCotizado) || 0)
      case 'monto-asc':
        return (Number(a.totalCotizado) || 0) - (Number(b.totalCotizado) || 0)
      case 'codigo-asc':
        return (a.codigoCotizacion || '').localeCompare(b.codigoCotizacion || '')
      case 'cliente-asc':
        return (a.clienteRazonSocial || '').localeCompare(b.clienteRazonSocial || '')
      case 'fecha-desc':
      default:
        return new Date(b.fechaEmision || 0) - new Date(a.fechaEmision || 0) || (b.idCotizacion || 0) - (a.idCotizacion || 0)
    }
  })
})

const filteredClientes = computed(() => {
  let list = clientes.value.filter((cl) => {
    // 1. Tipo
    if (filterTipoCliente.value === 'PROSPECTO' && !cl.esProspecto) return false
    if (filterTipoCliente.value === 'CLIENTE' && cl.esProspecto) return false

    // 2. Estado
    if (filterEstadoCliente.value === 'ACTIVO' && cl.estado === 'INACTIVO') return false
    if (filterEstadoCliente.value === 'INACTIVO' && cl.estado !== 'INACTIVO') return false

    // 3. Búsqueda inteligente
    const q = normalizeSearchText(searchCliente.value)
    if (q) {
      const tokens = q.split(/\s+/).filter(Boolean)
      const corpus = normalizeSearchText(
        `${cl.numeroDocumento || ''} ${cl.razonSocial || ''} ${cl.contactoRepresentante || ''} ${cl.correoContacto || ''} ${cl.telefonoContacto || ''} ${cl.direccionFiscal || ''}`
      )
      if (!tokens.every((t) => corpus.includes(t))) return false
    }

    return true
  })

  // 4. Ordenar
  return list.sort((a, b) => {
    switch (sortByCliente.value) {
      case 'nombre-desc':
        return (b.razonSocial || '').localeCompare(a.razonSocial || '')
      case 'doc-asc':
        return (a.numeroDocumento || '').localeCompare(b.numeroDocumento || '')
      case 'tipo-asc':
        return (b.esProspecto ? 1 : 0) - (a.esProspecto ? 1 : 0)
      case 'nombre-asc':
      default:
        return (a.razonSocial || '').localeCompare(b.razonSocial || '')
    }
  })
})

const filteredExpedientes = computed(() => {
  let list = expedientesList.value.filter((exp) => {
    // 1. Estado
    if (filterEstadoExpediente.value && exp.estadoExpediente !== filterEstadoExpediente.value) return false

    // 2. Canal SUNAT
    if (filterCanalExpediente.value !== 'ALL') {
      const canal = (exp.canalSunat || 'PENDIENTE').toUpperCase()
      if (filterCanalExpediente.value === 'PENDIENTE') {
        if (canal !== 'PENDIENTE' && canal !== '') return false
      } else if (canal !== filterCanalExpediente.value) {
        return false
      }
    }

    // 3. Búsqueda inteligente
    const q = normalizeSearchText(searchExpediente.value)
    if (q) {
      const tokens = q.split(/\s+/).filter(Boolean)
      const corpus = normalizeSearchText(
        `${exp.codigoExpediente || ''} ${exp.codigoCotizacion || ''} ${exp.clienteRazonSocial || ''} ${exp.clienteNumeroDocumento || ''} ${exp.numeroDam || ''} ${exp.numeroDeclaracion || ''} ${exp.regimenAduanero || ''} ${exp.canalSunat || ''}`
      )
      if (!tokens.every((t) => corpus.includes(t))) return false
    }

    return true
  })

  // 4. Ordenar
  return list.sort((a, b) => {
    switch (sortByExpediente.value) {
      case 'fecha-asc':
        return new Date(a.fechaCreacion || a.fechaApertura || 0) - new Date(b.fechaCreacion || b.fechaApertura || 0)
      case 'codigo-asc':
        return (a.codigoExpediente || '').localeCompare(b.codigoExpediente || '')
      case 'cliente-asc':
        return (a.clienteRazonSocial || '').localeCompare(b.clienteRazonSocial || '')
      case 'fecha-desc':
      default:
        return new Date(b.fechaCreacion || b.fechaApertura || 0) - new Date(a.fechaCreacion || a.fechaApertura || 0) || (b.idExpediente || 0) - (a.idExpediente || 0)
    }
  })
})

const loadData = async () => {
  loading.value = true
  try {
    const [cots, cls, exps] = await Promise.all([
      comercialService.getCotizaciones(),
      comercialService.getClientes(),
      expedienteService.getExpedientes()
    ])
    cotizaciones.value = cots || []
    clientes.value = cls || []
    expedientesList.value = exps || []
  } catch (err) {
    showToast(err.response?.data?.message || 'Error al conectar con la API de ventas.', 'error')
  } finally {
    loading.value = false
  }
}

// ==========================================
// CLIENTES ACTIONS
// ==========================================
const openCreateCliente = () => {
  selectedCliente.value = null
  showClienteForm.value = true
}

const openEditCliente = (cliente) => {
  selectedCliente.value = { ...cliente }
  showClienteForm.value = true
}

const handleClienteSaved = async ({ isEdit, clienteId, payload, done }) => {
  try {
    if (isEdit) {
      await comercialService.updateCliente(clienteId, payload)
      showToast('Cliente actualizado correctamente.')
    } else {
      await comercialService.createCliente(payload)
      showToast('Nuevo cliente registrado exitosamente.')
    }
    showClienteForm.value = false
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'Error al guardar cliente.', 'error')
  } finally {
    done?.()
  }
}

const handleDeleteCliente = async (cliente) => {
  const confirmed = await confirmDelete(
    `¿Desea eliminar el cliente ${cliente.razonSocial}?`,
    'Confirmar Eliminación de Cliente'
  )
  if (!confirmed) return

  try {
    await comercialService.deleteCliente(cliente.idCliente)
    showToast('Cliente desactivado correctamente.', 'info')
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'No se pudo eliminar el cliente.', 'error')
  }
}

// ==========================================
// COTIZACIONES ACTIONS
// ==========================================
const openCreateCotizacion = () => {
  selectedCotizacion.value = null
  showCotizacionForm.value = true
}

const openCorrectionCotizacion = (cotizacion) => {
  selectedCotizacion.value = { ...cotizacion }
  showCorrectionView.value = true
  showCotizacionForm.value = false
  showClienteForm.value = false
  showObservationDialog.value = false
}

const openEditCotizacion = (cotizacion) => {
  if (cotizacion?.estado === 'REVISION' || cotizacion?.estado === 'OBSERVADA') {
    openCorrectionCotizacion(cotizacion)
    return
  }
  selectedCotizacion.value = { ...cotizacion }
  showCotizacionForm.value = true
}

const handleQuickClientCreated = async (newClient) => {
  await loadData()
}

const handleCotizacionSaved = async ({ isEdit, cotizacionId, payload, documentos, done }) => {
  try {
    if (isEdit) {
      await comercialService.updateCotizacion(cotizacionId, payload)
      if (cotizacionId && documentos && documentos.length > 0) {
        for (const doc of documentos) {
          try {
            await documentoService.upload([doc.file], {
              categoria: doc.categoria,
              descripcion: doc.descripcion || '',
              entidadTipo: 'COTIZACION',
              idEntidad: cotizacionId,
              idUsuario: authStore.user?.idUsuario || 1
            })
          } catch (docErr) {
            console.error('Error al subir documento sustentatorio en edición:', docErr)
          }
        }
      }
      showToast('Cotización actualizada y correcciones enviadas a Supervisión Aduanera.', 'success')
    } else {
      const created = await comercialService.createCotizacion(payload)
      const newId = created?.idCotizacion

      if (newId && documentos && documentos.length > 0) {
        let uploadedCount = 0
        for (const doc of documentos) {
          try {
            await documentoService.upload([doc.file], {
              categoria: doc.categoria,
              descripcion: doc.descripcion || '',
              entidadTipo: 'COTIZACION',
              idEntidad: newId,
              idUsuario: authStore.user?.idUsuario || 1
            })
            uploadedCount++
          } catch (docErr) {
            console.error('Error al subir documento sustentatorio:', docErr)
          }
        }
        showToast(`Cotización registrada exitosamente con ${uploadedCount} documento(s) adjunto(s).`)
      } else {
        showToast('Cotización registrada exitosamente.')
      }
    }
    closeAllForms()
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'Error al guardar cotización.', 'error')
  } finally {
    done?.()
  }
}

const handleCambiarEstadoCotizacion = async (cotizacion, nuevoEstado) => {
  let motivo = ''
  if (nuevoEstado === 'RECHAZADA') {
    const confirmed = await confirmAction(
      `¿Está seguro de marcar como RECHAZADA la cotización ${cotizacion.codigoCotizacion}?`,
      'Confirmar Rechazo de Cotización'
    )
    if (!confirmed) return
  }

  try {
    await comercialService.updateCotizacionEstado(cotizacion.idCotizacion, {
      estado: nuevoEstado,
      motivoObservacion: motivo
    })
    showToast(`Cotización marcada como ${nuevoEstado}.`)
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'Error al actualizar estado.', 'error')
  }
}

const handleDeleteCotizacion = async (cotizacion) => {
  const confirmed = await confirmDelete(
    `¿Desea eliminar la cotización ${cotizacion.codigoCotizacion}?`,
    'Confirmar Eliminación de Cotización'
  )
  if (!confirmed) return

  try {
    await comercialService.deleteCotizacion(cotizacion.idCotizacion)
    showToast('Cotización eliminada exitosamente.', 'info')
    await loadData()
  } catch (err) {
    showToast(err.response?.data?.message || 'No se puede eliminar la cotización.', 'error')
  }
}

const getCotizacionSeverity = (estado) => {
  if (estado === 'APROBADA') return 'success'
  if (estado === 'RECHAZADA' || estado === 'ANULADA') return 'danger'
  if (estado === 'REVISION' || estado === 'OBSERVADA') return 'warn'
  if (estado === 'EN_REVISION') return 'info'
  return 'contrast'
}

const getCotizacionLabel = (estado) => {
  const map = {
    ENVIADA: 'Pendiente',
    PENDIENTE: 'Pendiente',
    EN_REVISION: 'En Revisión',
    REVISION: 'Observada',
    OBSERVADA: 'Observada',
    APROBADA: 'Aprobada',
    RECHAZADA: 'Rechazada',
    ANULADA: 'Anulada'
  }
  return map[estado] || estado
}

const getExpedienteSeverity = (estado) => {
  if (estado === 'LEVANTE_AUTORIZADO' || estado === 'CONCLUIDO') return 'success'
  if (estado === 'ANULADO') return 'danger'
  if (estado === 'CON_CANAL') return 'warn'
  return 'info'
}

const getCanalSeverity = (canal) => {
  if (canal === 'VERDE') return 'success'
  if (canal === 'NARANJA') return 'warn'
  if (canal === 'ROJO') return 'danger'
  return 'info'
}

const formatRegimen = (regimen) => {
  if (!regimen) return '—'
  return regimen.replace(/_/g, ' ')
}

const formatDateTime = (dtStr) => {
  if (!dtStr) return '—'
  return dtStr.replace('T', ' ').substring(0, 16)
}

const getCotizacionBadgeClass = (estado) => {
  if (estado === 'APROBADA') return 'status-approved'
  if (estado === 'RECHAZADA') return 'status-rejected'
  return 'status-pending'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.ventas-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  width: 100%;
}

/* ==========================================================================
   ESTRUCTURA Y DASHBOARD COMERCIAL (LENGUAJE EDITORIAL CÁLIDO SGA)
   ========================================================================== */
.ventas-master-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.dashboard-comercial-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

/* 1. Barra de Atajos Rápidos Comerciales */
.dashboard-action-toolbar {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1rem 1.35rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.toolbar-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

/* 2. Grid de Métricas Comerciales Avanzadas */
.metrics-commercial-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.25rem;
}

@media (max-width: 1200px) {
  .metrics-commercial-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-commercial-grid {
    grid-template-columns: 1fr;
  }
}

.commercial-kpi-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.45rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  box-shadow: var(--shadow-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}

.commercial-kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
  border-color: var(--color-border-strong);
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-num {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: 4px;
  padding: 0.15rem 0.45rem;
  letter-spacing: 0.02em;
}

.kpi-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--color-text-muted);
  flex: 1;
}

.kpi-icon {
  font-size: 1.05rem;
  color: var(--color-text-subtle);
}

.kpi-icon.success {
  color: var(--color-success-text);
}

.kpi-icon.warning {
  color: var(--color-warning-text);
}

.kpi-icon.danger {
  color: var(--color-danger-text);
}

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.kpi-value {
  font-size: 2.15rem;
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1;
  letter-spacing: -0.02em;
}

.kpi-badge {
  font-size: 0.725rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
}

.kpi-badge.success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
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

.kpi-badge.info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border-color: var(--color-info-border);
}

.kpi-footer-note {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  border-top: 1px dashed var(--color-border-subtle);
  padding-top: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 3. Paneles Analíticos */
.dashboard-analytics-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

@media (max-width: 960px) {
  .dashboard-analytics-row {
    grid-template-columns: 1fr;
  }
}

.analytics-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.35rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.card-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.card-main-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0.15rem 0 0;
  letter-spacing: -0.01em;
}

.analytics-stat-pill {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
}

.pipeline-progress-bar {
  display: flex;
  height: 12px;
  border-radius: var(--radius-pill);
  overflow: hidden;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  margin: 0.35rem 0;
}

.pipeline-segment {
  height: 100%;
  transition: width 0.4s ease;
}

.pipeline-segment.approved {
  background: #15803d;
}

.pipeline-segment.pending {
  background: #b45309;
}

.pipeline-segment.observed {
  background: #eab308;
}

.pipeline-segment.rejected {
  background: #b91c1c;
}

.pipeline-legend {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.65rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-dot.approved {
  background: #15803d;
}

.legend-dot.pending {
  background: #b45309;
}

.legend-dot.observed {
  background: #eab308;
}

.legend-dot.rejected {
  background: #b91c1c;
}

.legend-count {
  margin-left: auto;
  color: var(--color-text-main);
  font-weight: 700;
}

.regimen-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.regimen-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-subtle);
}

.regimen-info {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.regimen-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.regimen-count {
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

.regimen-total {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.empty-mini {
  padding: 1rem;
  text-align: center;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

/* 4. Actividad Comercial Reciente */
.recent-commercial-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.35rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: var(--shadow-subtle);
}

.btn-sm {
  padding: 0.4rem 0.85rem;
  font-size: 0.785rem;
}

.recent-table-wrapper {
  overflow-x: auto;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
}

.compact-commercial-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.84rem;
  text-align: left;
}

.compact-commercial-table thead th {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 0.65rem 0.85rem;
  font-weight: 700;
  font-size: 0.725rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--color-border);
}

.compact-commercial-table tbody tr {
  border-bottom: 1px solid var(--color-border-subtle);
  transition: background var(--transition-fast);
}

.compact-commercial-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.compact-commercial-table tbody td {
  padding: 0.65rem 0.85rem;
  color: var(--color-text-main);
  vertical-align: middle;
}

.text-success {
  color: var(--color-success-text) !important;
}

.text-warning {
  color: var(--color-warning-text) !important;
}

.text-danger {
  color: var(--color-danger-text) !important;
}

/* Contenido de cada Tab */
.tab-content-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 1rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  letter-spacing: -0.01em;
}

.section-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.search-mini-input {
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.55rem 0.85rem;
  font-size: 0.825rem;
  color: var(--color-text-main);
  outline: none;
}

.search-mini-input:focus {
  border-color: var(--color-primary);
}

/* Tablas */
.table-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-subtle);
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.86rem;
}

.custom-table thead th {
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  font-weight: 700;
  font-size: 0.725rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0.95rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.custom-table tbody tr {
  border-bottom: 1px solid var(--color-border-subtle);
  transition: background var(--transition-fast);
  background: var(--color-bg-surface);
}

.custom-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.custom-table tbody td {
  padding: 0.95rem 1.25rem;
  vertical-align: middle;
}

.doc-code {
  font-weight: 700;
  color: var(--color-text-main);
  font-family: monospace;
  font-size: 0.85rem;
}

.doc-code-os {
  font-weight: 700;
  color: var(--color-success-text);
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.85rem;
}

.client-cell {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-weight: 600;
  color: var(--color-text-main);
}

.client-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.regimen-tag {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.total-amount {
  font-weight: 700;
  color: var(--color-text-main);
}

.date-text {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.status-pending {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.status-approved {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-rejected {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.status-progress {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.os-linked-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.785rem;
  font-weight: 600;
  color: var(--color-success-text);
}

.btn-generate-os {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.3rem 0.65rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  transition: all var(--transition-fast);
}

.btn-generate-os:hover {
  background: var(--color-bg-muted);
  color: var(--color-text-main);
}

.text-muted-dash {
  color: var(--color-text-subtle);
}

.text-right {
  text-align: right;
}

.actions-wrapper {
  display: inline-flex;
  gap: 0.45rem;
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.3rem 0.75rem;
  border-radius: 6px;
  font-size: 0.785rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
  color: var(--color-text-main);
}

.action-btn-outline.approve:hover {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.action-btn-outline.reject:hover {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.action-btn-outline.finish:hover {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.btn-danger-action:hover {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.doc-badge {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.doc-type {
  font-size: 0.65rem;
  font-weight: 700;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.doc-number {
  font-weight: 700;
  font-family: monospace;
}

.contact-text,
.address-text {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.client-tag {
  display: inline-flex;
  align-items: center;
  font-size: 0.725rem;
  font-weight: 700;
  padding: 0.15rem 0.55rem;
  border-radius: 4px;
}

.prospect-tag {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.client-effective-tag {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.obs-snippet {
  font-size: 0.785rem;
  color: var(--color-text-muted);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 3.5rem 1.5rem;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 2.25rem;
  color: var(--color-text-subtle);
  margin-bottom: 0.75rem;
}

.empty-title {
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.empty-desc {
  font-size: 0.8rem;
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
}

@media (max-width: 1024px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}

/* Estado Cell & Observation Pill */
.status-cell {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  align-items: flex-start;
}

.obs-warning-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.15rem 0.45rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  color: var(--color-warning-text);
  border-radius: var(--radius-sm, 6px);
  font-size: 0.6875rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.obs-warning-pill:hover {
  filter: brightness(0.95);
}

.action-btn-outline.observe-fix {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
  font-weight: 700;
}

.action-btn-outline.observe-fix:hover {
  filter: brightness(0.95);
}

/* Contenido Modal de Observación */
.observation-view-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
  padding: var(--space-2, 0.5rem) 0;
}

.obs-box-header {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding-bottom: var(--space-2, 0.5rem);
  border-bottom: 1px solid var(--color-border);
}

.obs-box-title {
  font-size: 1rem;
  color: var(--color-text-main);
}

.obs-box-body {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md, 8px);
  color: var(--color-warning-text);
}

.obs-box-icon {
  font-size: 1.5rem;
  margin-top: 0.15rem;
  flex-shrink: 0;
}

.obs-box-label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: block;
}

.obs-box-text {
  margin: 0.25rem 0 0 0;
  font-size: 0.875rem;
  line-height: 1.45;
  font-weight: 500;
}

.obs-box-hint {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.45;
  margin: 0;
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3, 0.75rem);
}

/* ==========================================================================
   Controles de Tablas (Filtros, Búsqueda, Ordenación - Estilo Warm Editorial)
   ========================================================================== */
.table-controls-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 0.85rem 1.25rem;
  box-shadow: var(--shadow-subtle);
}

.controls-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  flex: 1;
}

.controls-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 260px;
  flex: 1;
  max-width: 380px;
}

.search-icon {
  position: absolute;
  left: 0.85rem;
  color: var(--color-text-subtle);
  font-size: 0.85rem;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 0.55rem 2rem 0.55rem 2.25rem;
  background-color: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 0.825rem;
  color: var(--color-text-main);
  outline: none;
  transition: all var(--transition-fast);
}

.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(214, 180, 118, 0.2);
}

.clear-search-btn {
  position: absolute;
  right: 0.5rem;
  background: transparent;
  border: none;
  color: var(--color-text-subtle);
  cursor: pointer;
  padding: 0.2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-search-btn:hover {
  color: var(--color-text-main);
}

.filter-dropdown, .sort-dropdown {
  position: relative;
}

.filter-select {
  appearance: none !important;
  -webkit-appearance: none !important;
  -moz-appearance: none !important;
  font-family: var(--font-sans) !important;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main) !important;
  background-color: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md) !important;
  padding: 0.55rem 2.25rem 0.55rem 0.85rem !important;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%239a7f60' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e") !important;
  background-repeat: no-repeat !important;
  background-position: right 0.75rem center !important;
  background-size: 0.9rem 0.9rem !important;
  cursor: pointer;
  outline: none;
  transition: all var(--transition-fast);
}

.filter-select:focus {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 2px rgba(214, 180, 118, 0.2) !important;
}

.filter-select option {
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
}

.btn-clear-filters {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  padding: 0.5rem 0.8rem;
  border-radius: var(--radius-md);
  font-size: 0.775rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-clear-filters:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border-color: var(--color-danger-border);
}

.client-sub-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.15rem;
}

.portal-badge,
.origin-badge.origin-portal {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-sm, 3px);
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.presencial-badge,
.origin-badge.origin-presencial {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-sm, 3px);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.obs-client-status {
  margin-top: 0.4rem;
}

.btn-whatsapp-action,
.btn-whatsapp {
  color: var(--color-success-text) !important;
  border-color: var(--color-success-border) !important;
}

.btn-whatsapp-action:hover,
.btn-whatsapp:hover {
  background-color: #25d366 !important;
  color: #ffffff !important;
  border-color: #25d366 !important;
}

.btn-whatsapp-dialog,
.btn-whatsapp-modal {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background-color: #25d366;
  color: #ffffff;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md, 4px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-whatsapp-dialog:hover,
.btn-whatsapp-modal:hover {
  background-color: #1ebd56;
}
</style>
