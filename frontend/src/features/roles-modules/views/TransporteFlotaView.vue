<template>
  <div class="transporte-view">
    <!-- Hero Banner Card -->
    <HeroBannerCard
      kicker="TRANSPORTE Y FLOTA // CABINA OPERATIVA DE RUTA"
      :title="bannerTitle"
      :description="bannerDesc"
    />

    <!-- Pestañas de Navegación del Módulo de Transporte -->
    <div class="module-tabs-nav">
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'cabina' }"
        @click="switchTab('cabina')"
      >
        <i class="pi pi-compass"></i>
        <span>Mi Trabajo Actual</span>
        <span v-if="miDespachoActivo" class="tab-badge-pulse">1 ACTIVO</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'camion' }"
        @click="switchTab('camion')"
      >
        <i class="pi pi-truck"></i>
        <span>Camión Asignado</span>
      </button>

      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'viajes' }"
        @click="switchTab('viajes')"
      >
        <i class="pi pi-history"></i>
        <span>Historial de Mis Viajes</span>
        <span class="tab-counter">{{ misViajesList.length }}</span>
      </button>

      <!-- Pestaña de Monitoreo Global (Visible para todos o para Supervisores/Gerentes) -->
      <button
        type="button"
        class="tab-nav-btn"
        :class="{ active: activeTab === 'despachos' }"
        @click="switchTab('despachos')"
      >
        <i class="pi pi-chart-line"></i>
        <span>Monitoreo Global</span>
        <span class="tab-counter">{{ despachosGlobalList.length }}</span>
      </button>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 1: MI TRABAJO ACTUAL (CABINA DE RUTA DEL CHOFER)                  -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'cabina'" class="tab-container">
      <div v-if="loadingMiDespacho" class="loading-state-card">
        <i class="pi pi-spin pi-spinner spinner-lg"></i>
        <p>Sincronizando hoja de ruta y camión asignado...</p>
      </div>

      <!-- CASO A: TIENE UN TRABAJO ASIGNADO ACTIVO -->
      <div v-else-if="miDespachoActivo" class="cabin-active-wrapper">
        <!-- Banner de Orden Activa -->
        <div class="cabin-order-header" :class="miDespachoActivo.estadoTransporte">
          <div class="header-left">
            <span class="status-indicator">
              <span class="status-dot"></span>
              {{ formatEstado(miDespachoActivo.estadoTransporte) }}
            </span>
            <div class="tracking-title-wrap">
              <span class="tracking-code font-mono">{{ miDespachoActivo.codigoTracking }}</span>
              <span class="exp-code font-mono">EXP: {{ miDespachoActivo.codigoExpediente || 'N/A' }}</span>
              <span v-if="miDespachoActivo.numeroDam" class="dam-badge font-mono">DAM: {{ miDespachoActivo.numeroDam }}</span>
            </div>
          </div>

          <div class="header-right">
            <span v-if="miDespachoActivo.canalSunat" :class="['canal-tag', getCanalClass(miDespachoActivo.canalSunat)]">
              CANAL {{ miDespachoActivo.canalSunat }}
            </span>
            <button type="button" class="btn-refresh-cabin" @click="loadMiDespacho">
              <i class="pi pi-sync"></i>
              <span>Actualizar Ruta</span>
            </button>
          </div>
        </div>

        <!-- Cuadrícula 2 Columnas: Camión vs Hoja de Ruta -->
        <div class="cabin-grid-2">
          <!-- Columna 1: Camión y Equipo Asignado -->
          <div class="cabin-card">
            <div class="card-top-title">
              <i class="pi pi-truck card-icon"></i>
              <div>
                <span class="badge-kicker">EQUIPO DE TRANSPORTE</span>
                <h4 class="card-heading">1. Camión y Carga Asignada</h4>
              </div>
            </div>

            <div class="spec-list">
              <div class="spec-row">
                <span class="spec-label">Placa Tracto:</span>
                <span class="spec-value font-mono font-bold">{{ miDespachoActivo.unidadPlacaTracto || 'PENDIENTE ASIGNAR' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Placa Carreta / Plataforma:</span>
                <span class="spec-value font-mono">{{ miDespachoActivo.unidadPlacaCarreta || '— S/C —' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Marca y Modelo:</span>
                <span class="spec-value">{{ miDespachoActivo.unidadMarcaModelo || 'Unidad de flota' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Capacidad de Carga:</span>
                <span class="spec-value font-mono font-bold">{{ Number(miDespachoActivo.unidadCapacidadKg || 0).toLocaleString('es-PE') }} kg</span>
              </div>
              <div class="spec-divider"></div>
              <div class="spec-row">
                <span class="spec-label">Contenedor / Carga:</span>
                <span class="spec-value font-mono text-accent">{{ miDespachoActivo.numeroContenedor || 'Carga Suelta / Palletizada' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Precinto Aduanero:</span>
                <span class="spec-value font-mono">{{ miDespachoActivo.precintoAduanero || 'Sin precinto' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Mercancía Declarada:</span>
                <span class="spec-value">{{ miDespachoActivo.mercanciaNombre || 'Mercancía según DAM' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Guía GRE-T SUNAT:</span>
                <span class="spec-value font-mono font-bold">{{ miDespachoActivo.guiaRemisionTransporte || 'PENDIENTE DE EMISIÓN' }}</span>
              </div>
            </div>
          </div>

          <!-- Columna 2: Hoja de Ruta y Entrega -->
          <div class="cabin-card">
            <div class="card-top-title">
              <i class="pi pi-map-marker card-icon"></i>
              <div>
                <span class="badge-kicker">DESTINO Y CONTACTO</span>
                <h4 class="card-heading">2. Hoja de Ruta y Entrega</h4>
              </div>
            </div>

            <div class="spec-list">
              <div class="spec-row">
                <span class="spec-label">Punto de Partida (Origen):</span>
                <span class="spec-value">{{ miDespachoActivo.direccionPartida || 'Puerto / Almacén Aduanero Callao' }}</span>
              </div>
              <div class="spec-row destination-highlight">
                <span class="spec-label">Destino Final de Entrega:</span>
                <span class="spec-value font-bold">{{ miDespachoActivo.direccionLlegada || 'Dirección de planta cliente' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Distrito / Zona:</span>
                <span class="spec-value font-semibold">{{ miDespachoActivo.distritoLlegada || 'Lima / Callao' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Cliente Importador:</span>
                <span class="spec-value">{{ miDespachoActivo.clienteRazonSocial || 'Cliente SGA' }} (RUC: {{ miDespachoActivo.clienteNumeroDocumento || 'N/A' }})</span>
              </div>
              <div class="spec-divider"></div>
              <div class="spec-row">
                <span class="spec-label">Contacto / Receptor:</span>
                <span class="spec-value font-bold">{{ miDespachoActivo.contactoRecepcion || 'Encargado de almacén' }}</span>
              </div>
              <div class="spec-row">
                <span class="spec-label">Teléfono de Coordinación:</span>
                <div class="phone-call-wrap">
                  <span class="font-mono font-bold">{{ miDespachoActivo.clienteTelefono || '+51 987 112 233' }}</span>
                  <a
                    :href="'tel:' + (miDespachoActivo.clienteTelefono || '987112233')"
                    class="btn-call-direct"
                    title="Llamar directamente al contacto"
                  >
                    <i class="pi pi-phone"></i>
                    <span>Llamar</span>
                  </a>
                </div>
              </div>
              <div class="spec-row">
                <span class="spec-label">Fecha Programada:</span>
                <span class="spec-value font-mono">{{ formatDateTime(miDespachoActivo.fechaProgramada) }}</span>
              </div>
              <div v-if="miDespachoActivo.fechaSalidaRuta" class="spec-row">
                <span class="spec-label">Hora Salida a Ruta:</span>
                <span class="spec-value font-mono text-success">{{ formatDateTime(miDespachoActivo.fechaSalidaRuta) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Bitácora de Incidencias en Ruta si existen -->
        <div v-if="miDespachoActivo.detalleIncidencia" class="incidencias-box">
          <div class="incidencias-header">
            <i class="pi pi-info-circle"></i>
            <span>Historial de Incidencias Reportadas en Ruta</span>
          </div>
          <pre class="incidencias-text">{{ miDespachoActivo.detalleIncidencia }}</pre>
        </div>

        <!-- 3. Botones de Acción Rápida en Ruta -->
        <div class="cabin-actions-bar">
          <!-- Botón 1: Iniciar Salida a Ruta -->
          <button
            v-if="miDespachoActivo.estadoTransporte === 'PROGRAMADO' || miDespachoActivo.estadoTransporte === 'PENDIENTE_ASIGNACION'"
            type="button"
            class="btn-action-cabin btn-start-route"
            @click="handleIniciarRuta(miDespachoActivo)"
            :disabled="actionBusy"
          >
            <i class="pi pi-send"></i>
            <span>Iniciar Salida a Ruta</span>
          </button>

          <!-- Botón 2: Reportar Incidencia -->
          <button
            type="button"
            class="btn-action-cabin btn-incident"
            @click="openReportarIncidenciaModal"
            :disabled="actionBusy"
          >
            <i class="pi pi-exclamation-triangle"></i>
            <span>Reportar Incidencia</span>
          </button>

          <!-- Botón 3: Documentos GRE-T -->
          <button
            type="button"
            class="btn-action-cabin btn-docs"
            @click="openDocsModal(miDespachoActivo)"
          >
            <i class="pi pi-file-pdf"></i>
            <span>Ver Guía GRE-T / Docs</span>
          </button>

          <!-- Botón 4: Confirmar Entrega Conforme -->
          <button
            v-if="miDespachoActivo.estadoTransporte === 'EN_RUTA'"
            type="button"
            class="btn-action-cabin btn-confirm-delivery"
            @click="openConfirmarEntrega(miDespachoActivo)"
            :disabled="actionBusy"
          >
            <i class="pi pi-check-circle"></i>
            <span>Confirmar Entrega Conforme</span>
          </button>
        </div>
      </div>

      <!-- CASO B: NO TIENE TRABAJO ASIGNADO (ESTADO EN DESCANSO) -->
      <div v-else class="cabin-empty-state">
        <div class="empty-cabin-card">
          <div class="empty-icon-wrap">
            <i class="pi pi-check-circle"></i>
          </div>
          <h3 class="empty-title">Sin traslados asignados actualmente</h3>
          <p class="empty-desc">
            Su unidad se encuentra disponible. Cuando el área de despacho o gerencia programe una orden de traslado a su camión, aparecerá aquí su hoja de ruta, datos del cliente y botones de viaje.
          </p>
          <div class="empty-actions">
            <button type="button" class="btn-refresh-cabin" @click="loadMiDespacho">
              <i class="pi pi-sync"></i>
              <span>Comprobar Nuevas Asignaciones</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 2: CAMIÓN ASIGNADO (FICHA TÉCNICA DEL VEHÍCULO)                   -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'camion'" class="tab-container">
      <div v-if="miDespachoActivo && miDespachoActivo.unidadPlacaTracto" class="truck-detail-card">
        <div class="truck-header">
          <div class="truck-title-left">
            <i class="pi pi-truck truck-big-icon"></i>
            <div>
              <span class="badge-kicker">VEHÍCULO ASIGNADO A SU RUTA</span>
              <h3 class="truck-plate font-mono">{{ miDespachoActivo.unidadPlacaTracto }}</h3>
              <span class="truck-model">{{ miDespachoActivo.unidadMarcaModelo || 'Camión Pesado' }} ({{ formatTipo(miDespachoActivo.unidadTipoVehiculo) }})</span>
            </div>
          </div>
          <div class="truck-status-tag">
            <Tag severity="success" value="UNIDAD AUTORIZADA MTC" rounded />
          </div>
        </div>

        <div class="truck-specs-grid">
          <div class="spec-box">
            <span class="spec-label">Placa Tracto</span>
            <span class="spec-val font-mono">{{ miDespachoActivo.unidadPlacaTracto }}</span>
          </div>
          <div class="spec-box">
            <span class="spec-label">Placa Remolque / Carreta</span>
            <span class="spec-val font-mono">{{ miDespachoActivo.unidadPlacaCarreta || '— S/C —' }}</span>
          </div>
          <div class="spec-box">
            <span class="spec-label">Capacidad de Carga</span>
            <span class="spec-val font-mono">{{ Number(miDespachoActivo.unidadCapacidadKg || 0).toLocaleString('es-PE') }} kg</span>
          </div>
          <div class="spec-box">
            <span class="spec-label">Tipo de Unidad</span>
            <span class="spec-val">{{ formatTipo(miDespachoActivo.unidadTipoVehiculo) }}</span>
          </div>
          <div class="spec-box">
            <span class="spec-label">Conductor Asignado</span>
            <span class="spec-val">{{ miDespachoActivo.conductorNombre || 'Usted (Conductor Principal)' }}</span>
          </div>
          <div class="spec-box">
            <span class="spec-label">Licencia MTC</span>
            <span class="spec-val font-mono">{{ miDespachoActivo.conductorLicencia || 'A-IIIc' }}</span>
          </div>
        </div>

        <div class="truck-inspection-notice">
          <i class="pi pi-shield"></i>
          <div>
            <strong>Revisión de Seguridad Pre-Viaje:</strong>
            <span>Verifique el ajuste de pernos de carreta, presión de neumáticos a 110 PSI, extintor PQS vigente y sellos de seguridad antes de salir de garita.</span>
          </div>
        </div>
      </div>

      <div v-else class="cabin-empty-state">
        <div class="empty-cabin-card">
          <i class="pi pi-truck empty-icon-wrap"></i>
          <h3 class="empty-title">No tiene un camión asignado en este momento</h3>
          <p class="empty-desc">
            Cuando se le programe una orden de traslado terrestre, los datos técnicos de la unidad vehicular, carreta y revisiones se visualizarán aquí.
          </p>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 3: HISTORIAL DE MIS VIAJES REALIZADOS                             -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'viajes'" class="tab-container">
      <div class="flota-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">REGISTRO DE REPARTO</span>
            <h3 class="section-title">Historial de Entregas Realizadas</h3>
          </div>
          <button type="button" class="btn-refresh" @click="loadMisViajes" :disabled="loadingMisViajes">
            <i :class="['pi pi-sync', { 'pi-spin': loadingMisViajes }]"></i>
            <span>Actualizar</span>
          </button>
        </div>

        <div class="table-container">
          <DataTable
            :value="misViajesList"
            :loading="loadingMisViajes"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idDespachoTransporte"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-history empty-icon"></i>
                <p class="empty-title">Sin historial de entregas</p>
                <p class="empty-desc">Las órdenes de entrega concluidas se archivarán en este registro.</p>
              </div>
            </template>

            <Column header="N° Tracking">
              <template #body="{ data: d }">
                <span class="font-mono font-bold">{{ d.codigoTracking }}</span>
              </template>
            </Column>

            <Column header="Expediente / Cliente">
              <template #body="{ data: d }">
                <div class="client-cell">
                  <span class="font-bold text-xs font-mono">{{ d.codigoExpediente }}</span>
                  <span class="client-sub">{{ d.clienteRazonSocial }}</span>
                </div>
              </template>
            </Column>

            <Column header="Destino Entregado">
              <template #body="{ data: d }">
                <div class="client-cell">
                  <span class="text-xs">{{ d.direccionLlegada }}</span>
                  <span class="client-sub">{{ d.distritoLlegada }}</span>
                </div>
              </template>
            </Column>

            <Column header="Fecha Entrega">
              <template #body="{ data: d }">
                <span class="font-mono text-xs">{{ formatDateTime(d.fechaEntregaConforme || d.fechaProgramada) }}</span>
              </template>
            </Column>

            <Column header="Receptor Almacén">
              <template #body="{ data: d }">
                <div class="client-cell">
                  <span class="text-xs font-semibold">{{ d.receptorNombre || d.contactoRecepcion || 'Recepción' }}</span>
                  <span v-if="d.receptorDni" class="client-sub font-mono">DNI: {{ d.receptorDni }}</span>
                </div>
              </template>
            </Column>

            <Column header="Estado">
              <template #body="{ data: d }">
                <Tag
                  :severity="d.estadoTransporte === 'ENTREGADO' ? 'success' : 'secondary'"
                  :value="formatEstado(d.estadoTransporte)"
                  rounded
                  class="tag-xs"
                />
              </template>
            </Column>

            <Column header="Comprobante" headerStyle="width: 100px; text-align: center;" bodyStyle="text-align: center;">
              <template #body="{ data: d }">
                <button
                  type="button"
                  class="btn-row-action"
                  title="Ver Documentos y Guía GRE-T"
                  @click="openDocsModal(d)"
                >
                  <i class="pi pi-file-pdf"></i>
                </button>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- TAB 4: MONITOREO GLOBAL (DESPACHOS GENERALES)                         -->
    <!-- ===================================================================== -->
    <div v-if="activeTab === 'despachos'" class="tab-container">
      <!-- Métricas Rápidas Despachos -->
      <div class="metrics-grid">
        <MetricCard index="01" label="En Ruta / Tránsito" :value="despachosEnRutaCount" severity="pending" highlight />
        <MetricCard index="02" label="Programados" :value="despachosProgramadosCount" severity="warning" />
        <MetricCard index="03" label="Entregados Conforme" :value="despachosEntregadosCount" severity="success" />
        <MetricCard index="04" label="Total Operaciones" :value="despachosGlobalList.length" severity="accent" />
      </div>

      <!-- Barra de Filtros y Acciones -->
      <div class="flota-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">LOGÍSTICA TERRESTRE</span>
            <h3 class="section-title">Monitoreo Global de Despachos</h3>
          </div>

          <div class="header-actions">
            <!-- Filtro Estado -->
            <select v-model="filterEstado" class="filter-select">
              <option value="">Todos los Estados</option>
              <option value="PROGRAMADO">Programados</option>
              <option value="EN_RUTA">En Ruta</option>
              <option value="ENTREGADO">Entregados</option>
              <option value="CANCELADO">Cancelados</option>
            </select>

            <!-- Búsqueda -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por tracking, DAM o exp..."
              />
            </div>

            <button type="button" class="btn-refresh" @click="loadDespachosGlobal" :disabled="loadingGlobal">
              <i :class="['pi pi-sync', { 'pi-spin': loadingGlobal }]"></i>
              <span>Actualizar</span>
            </button>

            <button type="button" class="btn-primary-add" @click="showCrearDespachoModal = true">
              <i class="pi pi-plus"></i>
              <span>+ Programar Despacho</span>
            </button>
          </div>
        </div>

        <!-- Tabla Global -->
        <div class="table-container">
          <DataTable
            :value="filteredDespachosGlobal"
            :loading="loadingGlobal"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idDespachoTransporte"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-truck empty-icon"></i>
                <p class="empty-title">No hay despachos registrados</p>
                <p class="empty-desc">Programe una orden de traslado terrestre para iniciar el flujo de reparto.</p>
              </div>
            </template>

            <Column header="N° Tracking">
              <template #body="{ data: d }">
                <span class="font-mono font-bold">{{ d.codigoTracking }}</span>
              </template>
            </Column>

            <Column header="Expediente / Cliente">
              <template #body="{ data: d }">
                <div class="client-cell">
                  <span class="font-bold text-xs font-mono">{{ d.codigoExpediente }}</span>
                  <span class="client-sub">{{ d.clienteRazonSocial }}</span>
                </div>
              </template>
            </Column>

            <Column header="Guía GRE-T">
              <template #body="{ data: d }">
                <span class="font-mono text-xs">{{ d.guiaRemisionTransporte || 'PENDIENTE' }}</span>
              </template>
            </Column>

            <Column header="Unidad & Conductor">
              <template #body="{ data: d }">
                <div v-if="d.unidadPlacaTracto" class="client-cell">
                  <span class="font-bold text-xs font-mono">Tracto: {{ d.unidadPlacaTracto }}</span>
                  <span class="client-sub">{{ d.conductorNombre || 'Sin conductor' }}</span>
                </div>
                <div v-else>
                  <Tag severity="danger" value="Sin Asignación" rounded class="tag-xs" />
                </div>
              </template>
            </Column>

            <Column header="Destino">
              <template #body="{ data: d }">
                <span class="text-xs">{{ d.distritoLlegada || d.direccionLlegada }}</span>
              </template>
            </Column>

            <Column header="Estado">
              <template #body="{ data: d }">
                <Tag
                  :severity="getEstadoSeverity(d.estadoTransporte)"
                  :value="formatEstado(d.estadoTransporte)"
                  rounded
                  class="tag-xs"
                />
              </template>
            </Column>

            <Column header="Acciones" headerStyle="width: 140px; text-align: right;" bodyStyle="text-align: right;">
              <template #body="{ data: d }">
                <div class="action-buttons-cell">
                  <button
                    v-if="!d.idConductor || !d.idUnidad"
                    type="button"
                    class="btn-row-action"
                    title="Asignar Recursos"
                    @click="openAsignarRecursos(d)"
                  >
                    <i class="pi pi-user-plus"></i>
                  </button>

                  <button
                    v-if="d.estadoTransporte === 'PROGRAMADO'"
                    type="button"
                    class="btn-row-action text-info"
                    title="Iniciar Salida a Ruta"
                    @click="handleIniciarRuta(d)"
                  >
                    <i class="pi pi-send"></i>
                  </button>

                  <button
                    v-if="d.estadoTransporte === 'EN_RUTA'"
                    type="button"
                    class="btn-row-action text-success"
                    title="Confirmar Entrega Conforme"
                    @click="openConfirmarEntrega(d)"
                  >
                    <i class="pi pi-check"></i>
                  </button>

                  <button
                    type="button"
                    class="btn-row-action"
                    title="Ver Documentos"
                    @click="openDocsModal(d)"
                  >
                    <i class="pi pi-file-pdf"></i>
                  </button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ===================================================================== -->
    <!-- MODAL: REPORTAR INCIDENCIA EN RUTA                                    -->
    <!-- ===================================================================== -->
    <Dialog
      v-model:visible="showIncidenciaModal"
      modal
      header="REPORTAR INCIDENCIA // CABINA DE RUTA"
      :style="{ width: '500px' }"
      class="custom-modal-editorial"
    >
      <form @submit.prevent="submitIncidencia" class="modal-form">
        <div class="form-group">
          <label class="form-label">Detalle de la Novedad o Incidencia *</label>
          <textarea
            v-model="incidenciaTexto"
            class="form-textarea"
            rows="4"
            placeholder="Describa la eventualidad en ruta (ej. congestión en garita de terminal portuario, control aduanero SUNAT, falla mecánica menor)..."
            required
          ></textarea>
        </div>
        <div class="modal-footer-actions">
          <button type="button" class="btn-cancel" @click="showIncidenciaModal = false">
            Cancelar
          </button>
          <button type="submit" class="btn-save" :disabled="savingIncidencia">
            <i v-if="savingIncidencia" class="pi pi-spin pi-spinner"></i>
            <span>Asentar Reporte</span>
          </button>
        </div>
      </form>
    </Dialog>

    <!-- Modales de Negocio -->
    <CrearDespachoModal
      :visible="showCrearDespachoModal"
      @close="showCrearDespachoModal = false"
      @saved="onDespachoGlobalSaved"
    />

    <AsignarRecursosModal
      :visible="showAsignarRecursosModal"
      :despacho="selectedDespacho"
      @close="showAsignarRecursosModal = false"
      @saved="onDespachoGlobalSaved"
    />

    <ConfirmarEntregaModal
      :visible="showConfirmarEntregaModal"
      :despacho="selectedDespacho"
      @close="showConfirmarEntregaModal = false"
      @saved="onEntregaConfirmada"
    />

    <!-- Gestor Universal de Documentos para GRE-T -->
    <DocumentosManagerModal
      :visible="showDocsModal"
      :entidad-tipo="'EXPEDIENTE'"
      :id-entidad="selectedDocEntidadId"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigo"
      :titulo-entidad="selectedDocTitulo"
      @close="showDocsModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import HeroBannerCard from '@/shared/components/HeroBannerCard.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { ROLES } from '@/core/auth/roles'
import { transporteFlotaService } from '../services/transporteFlotaService'
import CrearDespachoModal from '../components/CrearDespachoModal.vue'
import AsignarRecursosModal from '../components/AsignarRecursosModal.vue'
import ConfirmarEntregaModal from '../components/ConfirmarEntregaModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import { useNotification } from '@/shared/composables/useNotification'

const { showSuccess, showError, showInfo } = useNotification()
const navigationStore = useNavigationStore()
const authStore = useAuthStore()

const activeTab = ref('cabina')

// Estados para Cabina Conductor
const miDespachoActivo = ref(null)
const loadingMiDespacho = ref(false)
const actionBusy = ref(false)

// Estados para Mis Viajes
const misViajesList = ref([])
const loadingMisViajes = ref(false)

// Estados para Monitoreo Global
const despachosGlobalList = ref([])
const loadingGlobal = ref(false)
const filterEstado = ref('')
const searchQuery = ref('')

// Modales
const showCrearDespachoModal = ref(false)
const showAsignarRecursosModal = ref(false)
const showConfirmarEntregaModal = ref(false)
const showIncidenciaModal = ref(false)
const incidenciaTexto = ref('')
const savingIncidencia = ref(false)
const selectedDespacho = ref(null)

// Gestor Documentos GRE-T
const showDocsModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigo = ref('')
const selectedDocTitulo = ref('')

// Textos dinámicos del banner
const bannerTitle = computed(() => {
  if (authStore.userRole === ROLES.SUPERVISOR_TRANSPORTE) {
    return `Cabina de Operaciones // Chofer: ${authStore.userFullName || authStore.userName}`
  }
  return 'Cabina Operativa de Ruta y Monitoreo de Transporte'
})

const bannerDesc = computed(() => {
  return 'Ejecución y seguimiento de traslados aduaneros terrestres, inspección de camión asignado, reporte de incidencias y entrega conforme con Guía GRE-T.'
})

// Métricas Globales
const despachosEnRutaCount = computed(() =>
  despachosGlobalList.value.filter(d => d.estadoTransporte === 'EN_RUTA').length
)
const despachosProgramadosCount = computed(() =>
  despachosGlobalList.value.filter(d => d.estadoTransporte === 'PROGRAMADO').length
)
const despachosEntregadosCount = computed(() =>
  despachosGlobalList.value.filter(d => d.estadoTransporte === 'ENTREGADO').length
)

const filteredDespachosGlobal = computed(() => {
  return despachosGlobalList.value.filter(d => {
    if (filterEstado.value && d.estadoTransporte !== filterEstado.value) return false
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase().trim()
      const trk = (d.codigoTracking || '').toLowerCase()
      const exp = (d.codigoExpediente || '').toLowerCase()
      const dam = (d.numeroDam || '').toLowerCase()
      const cli = (d.clienteRazonSocial || '').toLowerCase()
      if (!trk.includes(q) && !exp.includes(q) && !dam.includes(q) && !cli.includes(q)) return false
    }
    return true
  })
})

// Sincronización con navigationStore
watch(
  () => [navigationStore.transporteFlotaSubOption, navigationStore.navTick],
  ([sub]) => {
    if (sub && ['cabina', 'camion', 'viajes', 'despachos'].includes(sub)) {
      activeTab.value = sub
    }
  },
  { immediate: true }
)

const switchTab = (tab) => {
  activeTab.value = tab
  navigationStore.setTransporteFlotaSubOption(tab)
  if (tab === 'cabina' || tab === 'camion') {
    loadMiDespacho()
  } else if (tab === 'viajes') {
    loadMisViajes()
  } else if (tab === 'despachos') {
    loadDespachosGlobal()
  }
}

// Cargar Mi Despacho Activo
const loadMiDespacho = async () => {
  loadingMiDespacho.value = true
  try {
    const data = await transporteFlotaService.getMiDespachoActivo()
    miDespachoActivo.value = data || null
  } catch (err) {
    console.error('Error al cargar mi despacho activo:', err)
  } finally {
    loadingMiDespacho.value = false
  }
}

// Cargar Historial de Mis Viajes
const loadMisViajes = async () => {
  loadingMisViajes.value = true
  try {
    const data = await transporteFlotaService.getMisDespachos()
    misViajesList.value = data || []
  } catch (err) {
    console.error('Error al cargar historial de viajes:', err)
  } finally {
    loadingMisViajes.value = false
  }
}

// Cargar Despachos Globales
const loadDespachosGlobal = async () => {
  loadingGlobal.value = true
  try {
    const data = await transporteFlotaService.getDespachos({ soloActivos: false })
    despachosGlobalList.value = data || []
  } catch (err) {
    showError('Error al cargar monitoreo global de despachos')
  } finally {
    loadingGlobal.value = false
  }
}

// Iniciar Salida a Ruta
const handleIniciarRuta = async (despacho) => {
  const id = despacho.idDespachoTransporte || despacho.idDespacho
  if (!confirm(`¿Confirmar salida a ruta del despacho ${despacho.codigoTracking || id}?`)) return

  actionBusy.value = true
  try {
    await transporteFlotaService.iniciarRuta(id, {
      fechaSalidaRuta: new Date().toISOString()
    })
    showSuccess('Ruta de transporte iniciada exitosamente')
    await loadMiDespacho()
    if (activeTab.value === 'despachos') await loadDespachosGlobal()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al iniciar ruta de transporte')
  } finally {
    actionBusy.value = false
  }
}

// Reportar Incidencia
const openReportarIncidenciaModal = () => {
  incidenciaTexto.value = ''
  showIncidenciaModal.value = true
}

const submitIncidencia = async () => {
  if (!miDespachoActivo.value || !incidenciaTexto.value.trim()) return
  savingIncidencia.value = true
  try {
    const id = miDespachoActivo.value.idDespachoTransporte || miDespachoActivo.value.idDespacho
    await transporteFlotaService.reportarIncidencia(id, incidenciaTexto.value.trim())
    showInfo('Incidencia en ruta registrada en la bitácora')
    showIncidenciaModal.value = false
    await loadMiDespacho()
  } catch (err) {
    showError(err.response?.data?.message || 'Error al reportar incidencia')
  } finally {
    savingIncidencia.value = false
  }
}

// Confirmar Entrega
const openConfirmarEntrega = (despacho) => {
  selectedDespacho.value = despacho
  showConfirmarEntregaModal.value = true
}

const onEntregaConfirmada = async () => {
  showSuccess('Entrega conforme asentada y unidad liberada')
  await loadMiDespacho()
  await loadMisViajes()
  if (activeTab.value === 'despachos') await loadDespachosGlobal()
}

// Asignar Recursos
const openAsignarRecursos = (despacho) => {
  selectedDespacho.value = despacho
  showAsignarRecursosModal.value = true
}

const onDespachoGlobalSaved = async () => {
  await loadDespachosGlobal()
  await loadMiDespacho()
}

// Gestor de Documentos GRE-T
const openDocsModal = (despacho) => {
  selectedDocEntidadId.value = despacho.idExpediente || despacho.idDespachoTransporte
  selectedDocCodigo.value = despacho.codigoTracking || despacho.codigoExpediente || 'TRK'
  selectedDocTitulo.value = `Guía Remisión GRE-T y Docs: ${despacho.codigoTracking}`
  showDocsModal.value = true
}

// Formateos
const formatEstado = (estado) => {
  const map = {
    PENDIENTE_ASIGNACION: 'Pendiente de Recursos',
    PROGRAMADO: 'Programado',
    EN_RUTA: 'En Ruta',
    ENTREGADO: 'Entregado Conforme',
    CANCELADO: 'Cancelado'
  }
  return map[estado] || estado || 'Pendiente'
}

const getEstadoSeverity = (estado) => {
  switch (estado) {
    case 'EN_RUTA': return 'info'
    case 'PROGRAMADO': return 'warning'
    case 'ENTREGADO': return 'success'
    case 'CANCELADO': return 'danger'
    default: return 'secondary'
  }
}

const getCanalClass = (canal) => {
  switch (canal) {
    case 'VERDE': return 'canal-verde'
    case 'NARANJA': return 'canal-naranja'
    case 'ROJO': return 'canal-rojo'
    default: return ''
  }
}

const formatTipo = (tipo) => {
  const map = {
    TRACTO: 'Tracto Camión',
    PLATAFORMA: 'Plataforma / Carreta',
    FURGON: 'Furgón Cerrado',
    CAMION_BARANDA: 'Camión Baranda'
  }
  return map[tipo] || tipo || 'Tracto'
}

const formatDateTime = (val) => {
  if (!val) return '—'
  try {
    const d = new Date(val)
    return d.toLocaleString('es-PE', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return val
  }
}

onMounted(() => {
  loadMiDespacho()
  loadMisViajes()
  loadDespachosGlobal()
})
</script>

<style scoped>
.transporte-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Tabs */
.module-tabs-nav {
  display: flex;
  gap: 0.5rem;
  border-bottom: 2px solid var(--color-border, #e2d9cd);
  padding-bottom: 0.25rem;
  flex-wrap: wrap;
}
.tab-nav-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -0.35rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: #7a7065;
  cursor: pointer;
  transition: all 0.15s ease;
}
.tab-nav-btn:hover {
  color: var(--color-text-primary, #352f2a);
}
.tab-nav-btn.active {
  color: var(--color-accent, #9a7f60);
  border-bottom-color: var(--color-accent, #9a7f60);
}
.tab-counter {
  background: #ede6db;
  color: #352f2a;
  font-size: 0.7rem;
  padding: 0.1rem 0.45rem;
  border-radius: 12px;
  font-family: var(--font-mono, monospace);
}
.tab-badge-pulse {
  background: #b42318;
  color: #ffffff;
  font-size: 0.65rem;
  padding: 0.15rem 0.45rem;
  border-radius: 12px;
  font-family: var(--font-mono, monospace);
  font-weight: 800;
  animation: pulse-red 2s infinite;
}
@keyframes pulse-red {
  0% { opacity: 1; }
  50% { opacity: 0.6; }
  100% { opacity: 1; }
}

.tab-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* Cabina Activa */
.cabin-active-wrapper {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.cabin-order-header {
  background: #ffffff;
  border: 1px solid var(--color-border, #e2d9cd);
  border-left: 6px solid var(--color-accent, #9a7f60);
  border-radius: 6px;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.cabin-order-header.EN_RUTA {
  border-left-color: #027a48;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #027a48;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #027a48;
  box-shadow: 0 0 0 3px rgba(2, 122, 72, 0.2);
}

.tracking-title-wrap {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.tracking-code {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--color-text-primary, #352f2a);
}
.exp-code {
  font-size: 0.85rem;
  background: #f6f3ee;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #e2d9cd;
}
.dam-badge {
  font-size: 0.85rem;
  background: #fdf5ea;
  color: #8c5b1b;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #e8c697;
  font-weight: 700;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.canal-tag {
  font-size: 0.75rem;
  font-weight: 800;
  padding: 0.25rem 0.65rem;
  border-radius: 4px;
  font-family: var(--font-mono, monospace);
}
.canal-verde { background: #eef8f3; color: #027a48; border: 1px solid #a6dbbe; }
.canal-naranja { background: #fdf5ea; color: #b54708; border: 1px solid #e8c697; }
.canal-rojo { background: #faebeb; color: #b42318; border: 1px solid #e89797; }

.btn-refresh-cabin {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.75rem;
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-refresh-cabin:hover {
  background: #f6f3ee;
}

/* Grilla de Cabina */
.cabin-grid-2 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.25rem;
}

.cabin-card {
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.card-top-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid #e2d9cd;
  padding-bottom: 0.75rem;
}
.card-icon {
  font-size: 1.5rem;
  color: var(--color-accent, #9a7f60);
}
.card-heading {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}

.spec-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.spec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.825rem;
  padding: 0.2rem 0;
}
.spec-label {
  color: #6b635b;
}
.spec-value {
  color: var(--color-text-primary, #352f2a);
  text-align: right;
  max-width: 60%;
}
.spec-divider {
  height: 1px;
  background: #e2d9cd;
  margin: 0.25rem 0;
}
.destination-highlight {
  background: #ffffff;
  padding: 0.4rem 0.6rem;
  border-radius: 4px;
  border: 1px solid #e2d9cd;
}
.phone-call-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}
.btn-call-direct {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  background: #027a48;
  color: #ffffff;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  text-decoration: none;
}
.btn-call-direct:hover {
  background: #02633a;
}

.incidencias-box {
  background: #fdf5ea;
  border: 1px solid #e8c697;
  border-radius: 6px;
  padding: 0.75rem 1rem;
}
.incidencias-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-weight: 700;
  font-size: 0.8rem;
  color: #8c5b1b;
  margin-bottom: 0.35rem;
}
.incidencias-text {
  font-size: 0.775rem;
  color: #553b14;
  white-space: pre-wrap;
  margin: 0;
  font-family: inherit;
}

/* Botones de Cabina */
.cabin-actions-bar {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  padding: 1rem;
  background: #ffffff;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  justify-content: flex-end;
}
.btn-action-cabin {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  border: none;
  border-radius: 4px;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.btn-start-route {
  background: #175cd3;
  color: #ffffff;
}
.btn-start-route:hover {
  background: #154fb7;
}
.btn-incident {
  background: #b54708;
  color: #ffffff;
}
.btn-incident:hover {
  background: #93370d;
}
.btn-docs {
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  color: var(--color-text-primary, #352f2a);
}
.btn-docs:hover {
  background: #f6f3ee;
}
.btn-confirm-delivery {
  background: #027a48;
  color: #ffffff;
}
.btn-confirm-delivery:hover {
  background: #02633a;
}

/* Empty State Cabina */
.cabin-empty-state {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
}
.empty-cabin-card {
  max-width: 500px;
  text-align: center;
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 8px;
  padding: 2.5rem 1.5rem;
}
.empty-icon-wrap {
  font-size: 3rem;
  color: #027a48;
  margin-bottom: 1rem;
}

/* Ficha Camión Asignado */
.truck-detail-card {
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.truck-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e2d9cd;
  padding-bottom: 1rem;
}
.truck-title-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.truck-big-icon {
  font-size: 2.5rem;
  color: var(--color-accent, #9a7f60);
}
.truck-plate {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 800;
}
.truck-specs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}
.spec-box {
  background: #ffffff;
  padding: 0.75rem 1rem;
  border-radius: 4px;
  border: 1px solid #e2d9cd;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.spec-val {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}
.truck-inspection-notice {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  background: #eef8f3;
  border: 1px solid #a6dbbe;
  border-radius: 4px;
  padding: 0.75rem 1rem;
  font-size: 0.8rem;
  color: #02633a;
}

/* Tablas y métricas compartidas */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}
.flota-table-card {
  background: var(--color-bg-card, #ede6db);
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 6px;
  padding: 1.25rem;
}
.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}
.badge-kicker {
  font-family: var(--font-mono, monospace);
  font-size: 0.65rem;
  letter-spacing: 0.08em;
  color: var(--color-accent, #9a7f60);
  font-weight: 700;
  display: block;
}
.section-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.filter-select {
  padding: 0.45rem 0.65rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
}
.search-input-wrap {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 0.6rem;
  top: 50%;
  transform: translateY(-50%);
  color: #8c827a;
  font-size: 0.8rem;
}
.search-input {
  padding: 0.45rem 0.65rem 0.45rem 1.8rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
  width: 200px;
}
.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.75rem;
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-primary-add {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  background: var(--color-accent, #9a7f60);
  border: none;
  border-radius: 4px;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.table-container {
  background: #ffffff;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--color-border, #e2d9cd);
}
.client-cell {
  display: flex;
  flex-direction: column;
}
.client-sub {
  font-size: 0.7rem;
  color: #6b635b;
}
.action-buttons-cell {
  display: inline-flex;
  gap: 0.35rem;
}
.btn-row-action {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: 1px solid var(--color-border, #e2d9cd);
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
}
.btn-row-action:hover {
  background: #f6f3ee;
}
.tag-xs {
  font-size: 0.675rem;
  padding: 0.15rem 0.4rem;
}
.empty-state {
  text-align: center;
  padding: 2rem;
}
.empty-icon {
  font-size: 2rem;
  color: #8c827a;
  margin-bottom: 0.5rem;
}

/* Modales */
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.form-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-primary, #352f2a);
}
.form-textarea {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border, #e2d9cd);
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.8rem;
  font-family: inherit;
  resize: vertical;
}
.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  border-top: 1px solid var(--color-border, #e2d9cd);
  padding-top: 0.75rem;
}
.btn-cancel {
  padding: 0.45rem 0.85rem;
  background: transparent;
  border: 1px solid var(--color-border, #e2d9cd);
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-save {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 1rem;
  background: var(--color-accent, #9a7f60);
  border: none;
  border-radius: 4px;
  color: #ffffff;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
}
.loading-state-card {
  text-align: center;
  padding: 3rem 1rem;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e2d9cd;
  color: #7a7065;
}
.spinner-lg {
  font-size: 2rem;
  color: var(--color-accent, #9a7f60);
  margin-bottom: 0.5rem;
}
</style>
